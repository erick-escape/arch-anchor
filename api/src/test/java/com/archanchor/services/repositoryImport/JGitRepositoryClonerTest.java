package com.archanchor.services.repositoryImport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Clones from an origin repository built in a temporary directory, so the suite needs no
 * network: JGit serves file:// URIs with the same upload-pack used over HTTPS.
 */
class JGitRepositoryClonerTest {

	private static final PersonIdent AUTHOR = new PersonIdent("Arch Anchor Test", "test@archanchor.invalid");

	@TempDir
	Path workspace;

	private URI originUri;

	private String firstCommit;

	private String secondCommit;

	private final JGitRepositoryCloner cloner = new JGitRepositoryCloner(10);

	@BeforeEach
	void createOrigin() throws IOException, GitAPIException {
		Path origin = workspace.resolve("origin");
		originUri = origin.toUri();
		try (Git git = Git.init().setDirectory(origin.toFile()).setInitialBranch("main").call()) {
			firstCommit = commitFile(git, origin, "src/main/java/shop/Order.java");
			git.tag().setName("v1").setMessage("first release").setTagger(AUTHOR).setSigned(false).call();
			git.branchCreate().setName("feature").call();
			secondCommit = commitFile(git, origin, "src/main/java/shop/Invoice.java");
		}
	}

	@Test
	void checksOutTheDefaultBranchAndReturnsItsCommit() {
		Path target = workspace.resolve("clone");

		String commit = cloner.cloneShallow(new CloneRequest(originUri, null, null), target);

		assertThat(commit).isEqualTo(secondCommit);
		assertThat(target.resolve("src/main/java/shop/Invoice.java")).exists();
	}

	@Test
	void fetchesOnlyTheCheckedOutCommit() throws IOException, GitAPIException {
		Path target = workspace.resolve("clone");

		cloner.cloneShallow(new CloneRequest(originUri, null, null), target);

		try (Git clone = Git.open(target.toFile())) {
			assertThat(clone.log().call()).hasSize(1);
		}
	}

	@Test
	void checksOutABranchByName() {
		Path target = workspace.resolve("clone");

		String commit = cloner.cloneShallow(new CloneRequest(originUri, "feature", null), target);

		assertThat(commit).isEqualTo(firstCommit);
		assertThat(target.resolve("src/main/java/shop/Order.java")).exists();
		assertThat(target.resolve("src/main/java/shop/Invoice.java")).doesNotExist();
	}

	@Test
	void checksOutAnAnnotatedTagByName() {
		Path target = workspace.resolve("clone");

		String commit = cloner.cloneShallow(new CloneRequest(originUri, "v1", null), target);

		assertThat(commit).isEqualTo(firstCommit);
	}

	@Test
	void refusesARefTheRemoteDoesNotHave() {
		CloneRequest request = new CloneRequest(originUri, "no-such-branch", null);

		assertThatThrownBy(() -> cloner.cloneShallow(request, workspace.resolve("clone")))
			.isInstanceOf(RepositoryRefNotFoundException.class)
			.hasMessageContaining("'no-such-branch'");
	}

	@Test
	void reportsAMissingRepositoryAsNotAccessible() {
		CloneRequest request = new CloneRequest(workspace.resolve("missing").toUri(), null, null);

		assertThatThrownBy(() -> cloner.cloneShallow(request, workspace.resolve("clone")))
			.isInstanceOf(RepositoryNotAccessibleException.class);
	}

	@Test
	void reportsAnEmptyRepositoryAsHavingNothingToCheckOut() throws GitAPIException {
		Path empty = workspace.resolve("empty");
		Git.init().setDirectory(empty.toFile()).call().close();

		assertThatThrownBy(
				() -> cloner.cloneShallow(new CloneRequest(empty.toUri(), null, null), workspace.resolve("clone")))
			.isInstanceOf(RepositoryRefNotFoundException.class)
			.hasMessageContaining("no commits");
	}

	@Test
	void reportsAnUnreachableHostAsACloneFailure() {
		// Port 1 on the loopback interface refuses connections immediately; no traffic
		// leaves the machine.
		CloneRequest request = new CloneRequest(URI.create("https://127.0.0.1:1/owner/repo.git"), null, null);

		assertThatThrownBy(() -> cloner.cloneShallow(request, workspace.resolve("clone")))
			.isInstanceOf(RepositoryCloneFailedException.class);
	}

	private String commitFile(Git git, Path worktree, String relativePath) throws IOException, GitAPIException {
		Path file = worktree.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, "class " + file.getFileName().toString().replace(".java", "") + " {}");
		git.add().addFilepattern(".").call();
		return git.commit()
			.setMessage("add " + relativePath)
			.setAuthor(AUTHOR)
			.setCommitter(AUTHOR)
			.setSign(false)
			.call()
			.getName();
	}

}
