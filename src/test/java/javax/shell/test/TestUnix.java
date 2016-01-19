package javax.shell.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;
import static javax.shell.Unix.*;

import org.junit.Test;

public class TestUnix {

	@Test
	public void testEcho() {
		fail("Not yet implemented");
	}

	@Test
	public void testCat() {
		fail("Not yet implemented");
	}

	@Test
	public void testLs() throws URISyntaxException {
		// FIXME this only works if not packaged
		URL resource = getClass().getResource("/dir1/file1.txt");
		assertTrue("Resources file not found?!?", resource != null);
		File dir = new File(resource.toURI()).getParentFile().getParentFile();
		assertTrue("Resources folder " + dir + " not found?!?", dir.exists());

		Process.setCurrentFolder(dir.getAbsolutePath());

		List<Integer> processesRan = new ArrayList<Integer>();
		List<String> linesReceived = new ArrayList<String>();
		Process p = new SimpleProcess(1, processesRan, linesReceived);
	}

}
