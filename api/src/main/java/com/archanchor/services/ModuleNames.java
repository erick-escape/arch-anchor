package com.archanchor.services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The names modules are shown and exported under (#89). A module is named after its
 * directory and that directory's parent, {@code bookstore/model}, unless two modules
 * would share a name: each of those then takes as many more leading directories as it
 * needs to differ, {@code Requests/Admin/Sale} and {@code Services/Admin/Sale}. A module
 * at the project root is named after the project. Only names that collide change, so a
 * project without repeated directory names keeps the names it always had.
 *
 * <pre>
 * ModuleNames.of(Path.of("uploads/shop"), List.of(Path.of("uploads/shop/app/Services/Admin/Sale")));
 * // {uploads/shop/app/Services/Admin/Sale=Admin/Sale}
 * </pre>
 */
final class ModuleNames {

	private static final int USUAL_DIRECTORIES = 2;

	private ModuleNames() {
	}

	/** Each module directory's name, in the order the directories are given. */
	static Map<Path, String> of(Path projectDirectory, Collection<Path> moduleDirectories) {
		List<NamedDirectory> named = moduleDirectories.stream()
			.map(directory -> new NamedDirectory(directory, segmentsOf(projectDirectory, directory)))
			.toList();
		// Each pass lengthens at least one name, and a name stops at the project, so this
		// ends.
		boolean lengthened = true;
		while (lengthened) {
			lengthened = lengthenSharedNames(named);
		}
		Map<Path, String> names = new LinkedHashMap<>();
		named.forEach(directory -> names.put(directory.directory, directory.name()));
		return names;
	}

	private static boolean lengthenSharedNames(List<NamedDirectory> named) {
		Map<String, List<NamedDirectory>> byName = named.stream()
			.collect(Collectors.groupingBy(NamedDirectory::name, LinkedHashMap::new, Collectors.toList()));
		boolean lengthened = false;
		for (List<NamedDirectory> sharing : byName.values()) {
			lengthened |= sharing.size() > 1 && lengthenEach(sharing);
		}
		return lengthened;
	}

	private static boolean lengthenEach(List<NamedDirectory> sharing) {
		boolean lengthened = false;
		for (NamedDirectory directory : sharing) {
			lengthened |= directory.lengthen();
		}
		return lengthened;
	}

	// The project's own name first, then the directories below it: a name can grow up to
	// the project, never into the server's directories above it. Both paths are made
	// absolute first, so that how each is written does not matter.
	private static List<String> segmentsOf(Path projectDirectory, Path moduleDirectory) {
		Path project = projectDirectory.toAbsolutePath().normalize();
		Path below = project.relativize(moduleDirectory.toAbsolutePath().normalize());
		List<String> segments = new ArrayList<>();
		segments.add(project.getFileName() == null ? project.toString() : project.getFileName().toString());
		if (!below.toString().isEmpty()) {
			below.forEach(segment -> segments.add(segment.toString()));
		}
		return segments;
	}

	private static final class NamedDirectory {

		private final Path directory;

		private final List<String> segments;

		private int length;

		NamedDirectory(Path directory, List<String> segments) {
			this.directory = directory;
			this.segments = segments;
			this.length = Math.min(USUAL_DIRECTORIES, segments.size());
		}

		String name() {
			return String.join("/", segments.subList(segments.size() - length, segments.size()));
		}

		// False when the name already holds every directory up to the project.
		boolean lengthen() {
			if (length == segments.size()) {
				return false;
			}
			length++;
			return true;
		}

	}

}
