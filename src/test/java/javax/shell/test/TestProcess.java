package javax.shell.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestProcess {

	static File resourcesFolder = null;

	@BeforeClass
	public static void setup() throws URISyntaxException {
		// FIXME this only works if not packaged
		URL resource = TestUnix.class.getResource("/dir1/file1.txt");
		assertTrue("Resources file not found?!?", resource != null);
		resourcesFolder = new File(resource.toURI()).getParentFile()
				.getParentFile();
		assertTrue("Resources folder " + resourcesFolder + " not found?!?",
				resourcesFolder.exists());
	}

	@Test
	public void testExpansion() {

		Process p = new Process() {

			@Override
			public void runme() throws Exception {
			}
		};

		p.setCurrentFolder(resourcesFolder.getAbsolutePath());

		List<String> args, exp;

		args = new ArrayList<String>();
		args.add("./dir1/file*");
		exp = p.expand(args);
		assertEquals("There are 3 files", 3, exp.size());

		args = new ArrayList<String>();
		args.add("dir1/file*");
		exp = p.expand(args);
		assertEquals("There are 3 files", 3, exp.size());

		args = new ArrayList<String>();
		args.add("dir1/file?.txt");
		exp = p.expand(args);
		assertEquals("There are 2 files .txt here", 2, exp.size());

		args = new ArrayList<String>();
		args.add(resourcesFolder.getAbsolutePath() + "/dir1/file?.txt");
		exp = p.expand(args);
		assertEquals("There are 2 files .txt here", 2, exp.size());

		args = new ArrayList<String>();
		args.add("/file?.txt");
		exp = p.expand(args);
		assertTrue("No file should be there", exp.isEmpty());

		args = new ArrayList<String>();
		args.add("*/file*.txt");
		exp = p.expand(args);
		assertEquals("There are 2+1=3 files .txt", 3, exp.size());

	}

	@Test
	public void testPipelines() throws IOException {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() {
				processesRan.add(1);
				stdout.println("ehlo");
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		TesterProcess p3 = new TesterProcess(3, processesRan);

		p1.pipe(p2).pipe(p3).sh();

		assertEquals("3 processes ran, not " + processesRan, 3,
				processesRan.size());
		assertEquals("3 lines should be elaborated", 3, p2.getLinesReceived()
				.size());
		assertEquals("3 lines should be elaborated", 3, p3.getLinesReceived()
				.size());
	}

	@Test
	public void testPipelines2() throws IOException {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() {
				processesRan.add(1);
				stdout.println("ehlo");
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.pipe(p2).sh();

		assertEquals("2 processes ran, not " + processesRan, 2,
				processesRan.size());
		assertEquals("3 lines should be elaborated", 3, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testPipelines3() throws IOException {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() throws InterruptedException {
				processesRan.add(1);
				stdout.println("ehlo");
				Thread.sleep(500);
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.pipe(p2).sh();

		assertEquals("2 processes ran, not " + processesRan, 2,
				processesRan.size());
		assertEquals("3 lines should be elaborated", 3, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testPipelines4() throws IOException {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() {
				processesRan.add(1);
				stdout.println("ehlo");
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan) {
			@Override
			public void runme() throws Exception {
				Thread.sleep(500);
				super.runme();
			}
		};

		p1.pipe(p2).sh();

		assertEquals("2 processes ran, not " + processesRan, 2,
				processesRan.size());
		assertEquals("3 lines should be elaborated", 3, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testAnd() {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() throws Exception {
				processesRan.add(1);
				stdout.println("ehlo");
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.and(p2).sh();

		assertEquals("Two processes ran", 2, processesRan.size());
		assertEquals("0 lines should be elaborated", 0, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testAnd2() {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() throws Exception {
				processesRan.add(1);
				throw new IllegalStateException();
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.and(p2).sh();

		assertEquals("1 processes ran", 1, processesRan.size());
		assertEquals("0 lines should be elaborated", 0, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testOr() {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() throws Exception {
				processesRan.add(1);
				stdout.println("ehlo");
				stdout.println("mydarling");
				stdout.println("seeyou");
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.or(p2).sh();

		assertEquals("1 process ran", 1, processesRan.size());
		assertEquals("0 lines should be elaborated", 0, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testOr2() {
		final List<Integer> processesRan = new ArrayList<Integer>();

		Process p1 = new Process() {
			@Override
			public void runme() throws Exception {
				processesRan.add(1);
				throw new IllegalStateException();
			}
		};

		TesterProcess p2 = new TesterProcess(2, processesRan);

		p1.or(p2).sh();

		assertEquals("2 processes ran", 2, processesRan.size());
		assertEquals("0 lines should be elaborated", 0, p2.getLinesReceived()
				.size());
	}

	@Test
	public void testAndStdout() throws IOException {

		File temp = File.createTempFile("output", ".tmp");
		temp.deleteOnExit();

		final List<Integer> processesRan = new ArrayList<Integer>();

		TesterProcess p1 = new TesterProcess(2, processesRan);

		Process p2 = new Process() {
			@Override
			public void runme() throws Exception {
				processesRan.add(1);
				Thread.sleep(5000);
			}
		};

		p1.and(p2).redirect(temp.getAbsolutePath()).sh();
		// p2 is far slower than p1
		// I want to be sure that stdout is not closed in the middle...

		assertEquals("2 processes ran", 2, processesRan.size());
	}

}
