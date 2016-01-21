package javax.shell.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;
import static javax.shell.Unix.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestUnix {

	static File resourcesFolder = null;

	@BeforeClass
	public static void setup() throws URISyntaxException {
		// FIXME this only works if not packaged
		URL resource = TestUnix.class.getResource("/dir1/file1.txt");
		assertTrue("Resources file not found?!?", resource != null);
		resourcesFolder = new File(resource.toURI()).getParentFile().getParentFile();
		assertTrue("Resources folder " + resourcesFolder + " not found?!?", resourcesFolder.exists());
	}

	@Test
	public void testEcho() {
		Process.setCurrentFolder(resourcesFolder.getAbsolutePath());

		List<Integer> processesRan = new ArrayList<Integer>();
		TesterProcess p2 = new TesterProcess(1, processesRan);

		echo("dir1/*").pipe(p2).sh();

		assertEquals("Echo should emit 3 files on just one line", 1, p2.getLinesReceived().size());
	}

	@Test
	public void testCatGrep() {
		Process.setCurrentFolder(resourcesFolder.getAbsolutePath());

		List<Integer> processesRan = new ArrayList<Integer>();
		TesterProcess p2 = new TesterProcess(2, processesRan);
		TesterProcess p3 = new TesterProcess(3, processesRan);

		cat("dir1/file*.txt").pipe(p2).pipe(grep("A")).pipe(p3).sh();

		assertEquals("There are 5 lines in two files", 5, p2.getLinesReceived().size());
		assertEquals("Only two lines contain 'A'", 2, p3.getLinesReceived().size());
	}

	@Test
	public void testLs() throws InterruptedException {

		Process.setCurrentFolder(resourcesFolder.getAbsolutePath());

		List<Integer> processesRan = new ArrayList<Integer>();
		TesterProcess p2 = new TesterProcess(1, processesRan);

		ls("dir1").pipe(p2).sh();

		assertEquals("There are 3 files inside folder 'dir1'", 3, p2.getLinesReceived().size());
	}

}
