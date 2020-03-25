package git;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

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

	public static final String REPO_DIR = "F:\\eclipse_workplace\\Cassandra";
	public static final String START_COMMIT = "c7a8730447d38698e6b21cf5a3226cd059a543b6";
	public static final String END_COMMIT = "9eab2633cc194af7a6b04977c0f148a65a735189";
	public static HashSet<String> visited = new HashSet<>();

	public static void main(String[] args) throws IOException {
		System.out.println("AncestorChecker");

		Repository repo = openRepository(REPO_DIR);

		visited.clear();
		List<RevCommit> res = findCommitsIfReachable(repo, START_COMMIT, END_COMMIT);

		if (res != null) {
			System.out.println("They are in same branch!! Path:");
			res.forEach(c -> System.out.println(c.getId()));
		} else {
			System.out.println("No path");
		}

		System.out.println("-------------Program Ends--------------");
	}

	/**
	 * Returns commits including the start and end commit (parent) if there is a path from
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
						System.out.println(commit.getId()
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

	public static List<RevCommit> findCommitsIfReachable(Repository repo, String commitId, String ancestorId) {
		if (commitId.equals(ancestorId)) {
			System.out.println("Start and end commit id must be different");
			return null;
		}

		RevCommit startCommit, endCommit, commit, parent;
		Stack<RevCommit> commitStack = new Stack<>();
		boolean isAncestor = false;
		List<RevCommit> result = new ArrayList<>();

		try {
			startCommit = getCommit(repo, ObjectId.fromString(commitId));
			endCommit = getCommit(repo, ObjectId.fromString(ancestorId));

			if (validate(startCommit, endCommit)) {
				commitStack.push(startCommit);
				while (!commitStack.isEmpty()) {
					commit = commitStack.peek();

					if (commit.getParents() != null) {
						for (int i = 0; i < commit.getParents().length; i++) {
							parent = getCommit(repo, commit.getParent(i));
							if (parent.getId().equals(endCommit.getId())) {

								result.add(parent);
								isAncestor = true;

								RevCommit currLevel = parent;
								while (!commitStack.isEmpty()) {
									currLevel = commitStack.pop();

									if (commitStack.isEmpty() || isDirectParent(commitStack.peek(), currLevel)) {
										result.add(currLevel);
									}
								}
								break;
							} else if (parent.getCommitTime() > 0
									&& parent.getCommitTime() < endCommit.getCommitTime()) {
								// gone too far
							} else {
								if (!visited.contains(parent.getName())) {
									commitStack.push(parent);
								}
							}
						}
					}

					visited.add(commit.getName());
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Collections.reverse(result);
		return isAncestor ? result : null;
	}

	public static boolean isDirectParent(RevCommit commit, RevCommit parent) {
		if (commit.getParents() != null) {
			for (int i = 0; i < commit.getParents().length; i++) {
				if (parent.getId().equals(commit.getParent(i).getId()))
					return true;
			}
		}
		return false;
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
