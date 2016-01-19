package javax.shell.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;

import org.junit.Test;

public class TestProcess {

	public void setup() {

	}

	@Test
	public void testExpansion() throws URISyntaxException {
		// FIXME this only works if not packaged
		URL resource = getClass().getResource("/dir1/file1.txt");
		assertTrue("Resources file not found?!?", resource != null);
		File dir = new File(resource.toURI()).getParentFile().getParentFile();
		assertTrue("Resources folder " + dir + " not found?!?", dir.exists());

		System.setProperty("user.dir", dir.getAbsolutePath());
		System.out.println("DEBUG resources=" + dir.getAbsolutePath());
		
		List<String> args, exp;

		args = new ArrayList<String>();
		args.add("./dir1/file*");
		exp = Process.expand(args);
		assertEquals("There are 3 files", 3, exp.size());

		args = new ArrayList<String>();
		args.add("dir1/file*");
		exp = Process.expand(args);
		assertEquals("There are 3 files", 3, exp.size());

		args = new ArrayList<String>();
		args.add("dir1/file?.txt");
		exp = Process.expand(args);
		assertEquals("There are 2 files .txt here", 2, exp.size());

		args = new ArrayList<String>();
		args.add(dir.getAbsolutePath() + "/dir1/file?.txt");
		exp = Process.expand(args);
		assertEquals("There are 2 files .txt here", 2, exp.size());

		args = new ArrayList<String>();
		args.add("/file?.txt");
		exp = Process.expand(args);
		assertTrue("No file should be there", exp.isEmpty());

		args = new ArrayList<String>();
		args.add("*/file*.txt");
		exp = Process.expand(args);
		assertEquals("There are 2+1=3 files .txt", 3, exp.size());

	}

	@Test
	public void testPipelines() {
		fail("Not yet implemented");
	}

	@Test
	public void testAnd() {
		fail("Not yet implemented");
	}

	@Test
	public void testOr() {
		fail("Not yet implemented");
	}

}
