package git;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Checks if the commits are ancestors of each others
 * 
 * @author K
 *
 */
public class AncestorChecker {

	public static final String REPO_DIR = "F:\\New folder\\toy_proj";

	public static void main(String[] args) throws IOException {
		System.out.println("AncestorChecker");

		Repository repo = openRepository(REPO_DIR);

		List<RevCommit> res = findCommits(repo, "4287d757eed9ba75e7261ea622f239f3d91dd86f",
				"17875c60cd303f072bcfdb3aa59e9d612500c586");

		if (res != null) {
			System.out.println("They are in same branch!! Path:");
			res.forEach(c -> System.out.println(c.getId()));
		} else {
			System.out.println("No path");
		}

		System.out.println("-------------Program Ends--------------");
	}

	/**
	 * Returns commits including the end commit (parent) if there is a path from
	 * commit id to its potential ancestor otherwise null;
	 */
	public static List<RevCommit> findCommits(Repository repo, String commitId, String ancestorId) {

		List<RevCommit> result = new ArrayList<>();
		RevCommit startCommit;
		RevCommit endCommit;
		RevCommit commit, parent;
		boolean isAncestor = false;

		if (commitId.equals(ancestorId)) {
			System.out.println("Start and end commit id must be different");
			return null;
		}

		try {
			startCommit = getCommit(repo, ObjectId.fromString(commitId));
			endCommit = getCommit(repo, ObjectId.fromString(ancestorId));

			if (validate(startCommit, endCommit)) {
				commit = startCommit;

				while (commit.getParents() != null) {
					if (commit.getParentCount() > 1) {
						System.out.println("Commit: " + commit.getId()
								+ " More than 1 parent found. Checking the first parent only");
					}

					parent = getCommit(repo, commit.getParent(0).getId());
					if (parent.getId().equals(endCommit.getId())) {
						result.add(parent);
						isAncestor = true;
						break;
					} else if (parent.getCommitTime() > 0 && parent.getCommitTime() < endCommit.getCommitTime()) {
						break;
					}

					result.add(parent);
					commit = parent;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return isAncestor ? result : null;
	}

	/**
	 * Returns full RevCommit details
	 */
	public static RevCommit getCommit(Repository repository, ObjectId commitId)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		try (RevWalk revWalk = new RevWalk(repository)) {
			RevCommit commit = revWalk.parseCommit(commitId);
			return commit;
		}
	}

	public static boolean validate(RevCommit commit, RevCommit parentCommit) {
		boolean hasTime = hasValidCommitTimes(commit, parentCommit);
		if (!hasTime) {
			System.out.println("Times must be non zero to determine the order."
					+ " commit time: " + commit.getCommitTime() + " parent: " + parentCommit.getCommitTime());
			System.out.println("This will result in checking every previous commit");
		}

		if (hasTime && !isInCorrectOrder(commit, parentCommit)) {
			System.out.println(
					"Child (" + commit.getId() + ") cannot have timestamp " + commit.getCommitTime()
							+ " less than or equal to the parent (" + parentCommit.getId()
							+ ") timestamp " + parentCommit.getCommitTime()
							+ ". Try swapping the start and end commit");
			return false;
		}

		return true;
	}

	public static boolean isInCorrectOrder(RevCommit commit, RevCommit parentCommit) {
		return commit.getCommitTime() >= parentCommit.getCommitTime();
	}

	public static boolean hasValidCommitTimes(RevCommit commit, RevCommit parentCommit) {
		return commit.getCommitTime() > 0 && parentCommit.getCommitTime() > 0;
	}

	public static Repository openRepository(String repositoryPath) throws IOException {
		File folder = new File(repositoryPath);
		Repository repository;
		if (folder.exists()) {
			RepositoryBuilder builder = new RepositoryBuilder();
			repository = builder
					.setGitDir(new File(folder, ".git"))
					.readEnvironment()
					.findGitDir()
					.build();
		} else {
			throw new FileNotFoundException(repositoryPath);
		}
		return repository;
	}
}
