package counter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

public class GitService {

	public Repository cloneIfNotExists(String projectPath, String cloneUrl/* , String branch */) throws Exception {
		File folder = new File(projectPath);
		Repository repository;
		if (folder.exists()) {
			RepositoryBuilder builder = new RepositoryBuilder();
			repository = builder
					.setGitDir(new File(folder, ".git"))
					.readEnvironment()
					.findGitDir()
					.build();

			System.out.println(String.format("Project {)} is already cloned, current branch is {1}",
					cloneUrl, repository.getBranch()));

		} else {
			System.out.println(String.format("Cloning {} ...", cloneUrl));
			Git git = Git.cloneRepository()
					.setDirectory(folder)
					.setURI(cloneUrl)

					// .setCloneAllBranches(true)
					.call();
			repository = git.getRepository();
			System.out.println(
					String.format("Done cloning {0}, current branch is {1}", cloneUrl, repository.getBranch()));
		}
		return repository;
	}

	public static Iterator<RevCommit> getAllCommits(Repository repo) {
		try (RevWalk walk = new RevWalk(repo)) {
			walk.setRevFilter(RevFilter.NO_MERGES);
			return walk.iterator();
		}
	}
	public static void checkoutRemoteHead(Repository repository) throws Exception {
		// logger.info("Checking out {} {} ...",
		// repository.getDirectory().getParent().toString(), commitId);
		File workingDir = repository.getDirectory().getParentFile();
		String output = ExternalProcess.execute(workingDir, "git", "fetch", "origin");
		//output += = ExternalProcess.execute(workingDir, "git", "reset", "--hard");
	}
	
	public static void checkout(Repository repository, String commitId) throws Exception {
		// logger.info("Checking out {} {} ...",
		// repository.getDirectory().getParent().toString(), commitId);
		try (Git git = new Git(repository)) {
			CheckoutCommand checkout = git.checkout().setName(commitId);
			checkout.call();
		}
//		File workingDir = repository.getDirectory().getParentFile();
//		ExternalProcess.execute(workingDir, "git", "checkout", commitId);
	}

	public static void checkout2(Repository repository, String commitId) throws Exception {
		// logger.info("Checking out {} {} ...",
		// repository.getDirectory().getParent().toString(), commitId);
		File workingDir = repository.getDirectory().getParentFile();
		String output = ExternalProcess.execute(workingDir, "git", "checkout", commitId);
		if (output.startsWith("fatal")) {
			throw new RuntimeException("git error " + output);
		}
	}
}
