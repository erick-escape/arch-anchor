#!/usr/bin/env bash
#
# Runs the Docker image and analyses the example projects in it over HTTP, as a user would (#85).
#
# It sees what the unit tests cannot: that the packaged tool, as the image's unprivileged user,
# on the image's C library and CPU, reads every language it claims to. The PHP grammar is native
# code, one library per platform, so an image can be built from a tree that passes every test and
# still be unable to read a PHP project.
#
#   .github/scripts/image-smoke-test.sh arch-anchor                # on this machine's platform
#   .github/scripts/image-smoke-test.sh arch-anchor linux/arm64    # emulated, where QEMU is set up
#
# Needs docker, curl and jq.
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
	echo "Usage: $0 <image> [platform], for example: $0 arch-anchor linux/arm64" >&2
	exit 64
fi

image="$1"
platform="${2:-}"
port="${ARCH_ANCHOR_SMOKE_PORT:-18080}"
api="http://localhost:${port}/api"
examples="$(cd "$(dirname "$0")/../../examples" && pwd)"
# An emulated JVM takes minutes to start what a native one starts in seconds.
seconds_to_start=300

container="$(docker run --detach ${platform:+--platform "$platform"} --publish "${port}:8080" "$image")"

# The container is kept until here, not run with --rm, so that its log outlives a crash.
report_and_remove() {
	status=$?
	if [ "$status" -ne 0 ]; then
		echo "--- the last lines of the container's log" >&2
		docker logs "$container" 2>&1 | tail -n 60 >&2
	fi
	docker rm --force "$container" > /dev/null
	exit "$status"
}
trap report_and_remove EXIT

wait_for_the_tool() {
	for _ in $(seq "$seconds_to_start"); do
		if curl --silent --fail --output /dev/null "${api}/projects"; then
			return 0
		fi
		sleep 1
	done
	echo "Expected ${api}/projects to answer within ${seconds_to_start} s, it did not" >&2
	return 1
}

# Sends an example as the browser sends a folder: every file, under the folder's name.
upload() {
	local project="$1"
	local files=()
	while IFS= read -r file; do
		files+=(--form "files=@${examples}/${project}/${file};filename=${project}/${file}")
	done < <(cd "${examples}/${project}" && find . -type f | sed 's|^\./||' | sort)
	curl --silent --show-error --fail --output /dev/null "${files[@]}" "${api}/upload"
}

# Every example is the same bookstore, so each must come back as the four modules and the
# similarity its README quotes.
analyse() {
	local project="$1"
	local analysis
	upload "$project"
	analysis="$(curl --silent --show-error --fail --request POST "${api}/analyze?projectName=${project}")"
	local as_quoted='(.modulesList | length) == 4 and .projectSimilarity == 0.9375'
	local as_returned='{modules: (.modulesList | length), projectSimilarity}'
	if ! jq --exit-status "$as_quoted" <<< "$analysis" > /dev/null; then
		echo "Expected 4 modules at similarity 0.9375 for ${project}, got $(jq --compact-output "$as_returned" <<< "$analysis")" >&2
		return 1
	fi
	echo "${project}: 4 modules at similarity 0.9375"
}

wait_for_the_tool
analyse bookstore
analyse bookstore-php
