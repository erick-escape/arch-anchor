package com.archanchor.services.repositoryImport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for the git library: instead of cloning, it writes a fixed repository layout
 * into the target, then either returns {@link #COMMIT_SHA} or throws the failure it was
 * given. It records every request it receives.
 *
 * <pre>
 * FakeRepositoryCloner cloner = new FakeRepositoryCloner();
 * FakeRepositoryCloner broken = new FakeRepositoryCloner().failingWith(new RepositoryCloneFailedException("down", null));
 * </pre>
 *
 * The layout has sources at the root and in {@code examples/demo}, a {@code .git/}
 * directory and a symbolic link, so callers can check what survives the import.
 */
public class FakeRepositoryCloner implements RepositoryCloner {

	public static final String COMMIT_SHA = "4f2c9e1d7b3a5c8e0f6d2b4a9c1e7f3d5b8a0c2e";

	public final List<CloneRequest> requests = new ArrayList<>();

	private RuntimeException failure;

	public FakeRepositoryCloner failingWith(RuntimeException failure) {
		this.failure = failure;
		return this;
	}

	@Override
	public String cloneShallow(CloneRequest request, Path target) {
		requests.add(request);
		writeRepository(target);
		if (failure != null) {
			throw failure;
		}
		return COMMIT_SHA;
	}

	private void writeRepository(Path target) {
		try {
			write(target.resolve(".git/config"), "[core]");
			write(target.resolve("pom.xml"), "<project/>");
			write(target.resolve("src/main/java/shop/Order.java"), "class Order {}");
			write(target.resolve("examples/demo/src/main/java/demo/Demo.java"), "class Demo {}");
			Files.createSymbolicLink(target.resolve("src/main/java/shop/Linked.java"), target.resolve("pom.xml"));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void write(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

}
