package javax.shell.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.shell.Program;

import org.junit.Test;

public class TestProgram {

	public void setup(){
		
	}
	
	@Test
	public void testExpansion() {
		getClass().getResource("/javax/shell/test/resources/file1.txt");
		File dir = new File("./javax/shell/test/resources");
		assertTrue("Resources folder not fould", dir.exists());

		List<String> args,exp;
		
		args = new ArrayList<String>();
		args.add("./javax/shell/test/resources/file*");
		exp = Program.expand(args);
		assertTrue("There are 3 files", 3 == exp.size());

		args = new ArrayList<String>();
		args.add("javax/shell/test/resources/file*");
		exp = Program.expand(args);
		assertTrue("There are 3 files", 3 == exp.size());

		args = new ArrayList<String>();
		args.add("javax/shell/test/resources/file?.p*");
		exp = Program.expand(args);
		assertTrue("There is 1 file here", 1 == exp.size());

		args = new ArrayList<String>();
		args.add("/javax/shell/test/resources/file?.p*");
		exp = Program.expand(args);
		assertTrue("No file should be there", exp.isEmpty());

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
