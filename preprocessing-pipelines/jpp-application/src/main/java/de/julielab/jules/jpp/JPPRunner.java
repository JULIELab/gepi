package de.julielab.jules.jpp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPPRunner {

	private static final Logger log = LoggerFactory.getLogger(JPPRunner.class);

	public static final String SERVER_LIST = "serverList";
	public static final String JPP_RUN_SCRIPT_LIST = "jppRunScriptList";
	public static final String SCRIPT_RUN_TEMPLATE = "ssh %s \"screen -dmS %s bash -c \\\"%s\\\"\"";

	private List<String> serverList;
	private List<String> scriptList;

	public JPPRunner() {
		// readFiles();
	}

	public void runPipelines(String pipelineRunArguments) {
		String sh = "/bin/sh";
		String cop = "-c";
		String cmd = "../runPipelinesWithSLURM.sh 2 4 " + pipelineRunArguments;
		// Script parameters: Number of jobs, number of threads per job, further
		// parameters (here: update (-u) or empty).
		String[] exe = new String[] { sh, cop, cmd };
		System.out.println("Running command: " + cmd);
		try {
			Process exec = Runtime.getRuntime().exec(exe);
			int exitValue = exec.waitFor();
			if (exitValue != 0) {
				List<String> errLines = IOUtils.readLines(exec.getErrorStream());
				log.error(
						"The command \"{}\" has terminated with exit value {}. Error: \"{}\"",
						new Object[] { cmd, exitValue, StringUtils.join(errLines, "\n") });
			}
			else
				log.info("The command \"{}\" was started successfully.", cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// for (String stage : scriptList) {
		// String stageName = stage.trim().replaceAll(".sh$", "");
		// for (String server : serverList) {
		// String command = String.format(SCRIPT_RUN_TEMPLATE, server,
		// stageName, stage + pipelineRunArguments);
		// log.debug("Starting script \"{}\" on \"{}\".", stage, server);
		// }
		// }
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readFiles() {
		try (InputStream serverlistIS = new FileInputStream(SERVER_LIST);
				InputStream runscriptIS = new FileInputStream(
						JPP_RUN_SCRIPT_LIST)) {
			List<String> serverList = IOUtils.readLines(serverlistIS);
			List<String> scriptList = IOUtils.readLines(runscriptIS);
			this.serverList = new ArrayList<>();
			this.scriptList = new ArrayList<>();
			for (String line : serverList) {
				if (!line.trim().equals("#"))
					this.serverList.add(line);
			}
			for (String line : scriptList) {
				if (!line.trim().equals("#"))
					this.scriptList.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
