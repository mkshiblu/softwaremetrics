package counter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class KLocCounter {

	// private Iterator<RevCommit> commits;
	private static final String cloc = Main.CLOC_DIR + "\\cloc.exe";
	public int MaxCommitCount = Integer.MAX_VALUE;
	private String includedLangs = "Java";
	private File projDir;
	private final String outputFile = Main.CLOC_DIR + "\\output.csv";
	private int processedCount;
	private static final String csvDelimiter = ",";
	Git git;

	public KLocCounter(File projDir) {
		this.projDir = projDir;
	}

	public void calculateKLocPerCommit() {

		BufferedWriter writer = null;
		processedCount = 0;
		try {
			git = Git.open(projDir);
			writer = new BufferedWriter(new FileWriter(outputFile));
			RevCommit commit;
			RevCommit parent;
			RevCommit root = getRootCommit();

			Iterable<RevCommit> commits = getAllCommits();
			Iterator<RevCommit> it = commits.iterator();

			List<RevCommit> comList = new ArrayList<RevCommit>();
			commits.forEach(comList::add);

			String out;

			while (it.hasNext() && processedCount < MaxCommitCount) {
				commit = it.next();
				checkout(commit);
				int kloc = getRepoCloc();
				out = toCsv(Main.PROJ_NAME, commit, kloc);

				writer.write(out);
				writer.newLine();
				writer.flush();
				processedCount++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public int getRepoCloc() {
		int kloc = 0;
		try {
			String out = ExternalProcess.execute(projDir, cloc, "--include-lang=" + includedLangs,
					projDir.getAbsolutePath().toString());
			// System.out.println(out);
			kloc = klocFromOutput(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return kloc;
	}

	String toCsv(String projName, RevCommit commit, int kloc) {

		RevCommit parent;
		try {
			parent = commit.getParent(0);
		} catch (Exception ex) {
			parent = null;
		}

		return projName + csvDelimiter + commit.getName()
				+ csvDelimiter + parent.getName()
				+ csvDelimiter
				+ commit.getCommitTime()
				+ csvDelimiter + kloc;
	}

	int klocFromOutput(String output) {

		final String lines[] = output.split("\\r?\\n");
		String[] words = null;

		for (final String line : lines) {
			if (line.startsWith("Java")) {
				words = line.split("\\s+");
				return tryParse(words[words.length - 1], 0);
			}
		}
		return 0;
	}

	public int tryParse(String value, int defaultVal) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private RevCommit getRootCommit() throws Exception {

		git.checkout().setStartPoint(Constants.HEAD);
		Repository repo = git.getRepository();
		ObjectId lastCommitId = repo.resolve(Constants.HEAD);

		try (RevWalk revWalk = new RevWalk(repo)) {
			return revWalk.parseCommit(lastCommitId);
		}
	}

	private Iterable<RevCommit> getAllCommits() throws Exception {
		Repository repo = git.getRepository();
		Iterable<RevCommit> commits = git.log().add(repo.resolve(Constants.HEAD)).call();

		return commits;
	}

	private void checkout(RevCommit commit) throws Exception {
		//
		GitService.checkout2(git.getRepository(), commit.getId().getName());
	}
}
