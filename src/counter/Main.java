package counter;

import java.io.File;
import java.util.Iterator;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class Main {

	public static final String CLOC_DIR = "E:\\CLOC";
	public static final String PROJ_DIR = "E:\\CLOC\\hbase";
	public static final String PROJ_GITHUB_URL = "https://github.com/apache/hbase.git";
	public static final String PROJ_NAME = "apache/hbase";

	public static void main(String[] args) throws Exception {

		GitService git = new GitService();
		Repository repo = git.cloneIfNotExists(
				PROJ_DIR,
				PROJ_GITHUB_URL);
		new KLocCounter(new File(PROJ_DIR)).calculateKLocPerCommit();
	}
}
