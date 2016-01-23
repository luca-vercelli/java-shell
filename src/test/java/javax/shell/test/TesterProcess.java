package javax.shell.test;

import java.util.ArrayList;
import java.util.List;

import javax.shell.Process;

public class TesterProcess extends Process {

	List<Integer> processesRan = null;
	Integer id = null;
	List<String> linesReceived = null;

	public TesterProcess(int id, List<Integer> processesRan,
			List<String> linesReceived) {
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

		while (stdinReader.ready()) {
			String line = stdinReader.readLine();
			linesReceived.add(line);
			stdout.println(line);
		}
		processesRan.add(id);
	}

	public List<String> getLinesReceived() {
		return linesReceived;
	}

}
