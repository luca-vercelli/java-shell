package javax.shell.test;

import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;

/**
 * A {@see Process} that re-emits given input. Received lines are stored in an
 * internal List so that they can be examined later.
 * 
 * @author vercelli 2016
 *
 */
public class TesterProcess extends Process {

	List<Integer> processesRan = null;
	Integer id = null;
	List<String> linesReceived = null;

	public TesterProcess(int id, List<Integer> processesRan, List<String> linesReceived) {
		this.processesRan = processesRan;
		this.id = id;
		this.linesReceived = linesReceived;
	}

	public TesterProcess(int id, List<Integer> processesRan) {
		this.processesRan = processesRan;
		this.id = id;
		this.linesReceived = new ArrayList<String>();
	}

	@Override
	public void runme() throws Exception {

		// DEBUG CODE
		System.out.println("HERE TesterProcess " + id);

		processesRan.add(id);

		String line;
		// while (stdinReader.ready()) {
		// String line = stdinReader.readLine();
		while ((line = stdinReader.readLine()) != null) {
			linesReceived.add(line);
			stdout.println(line);
		}
	}

	public List<String> getLinesReceived() {
		return linesReceived;
	}

}
