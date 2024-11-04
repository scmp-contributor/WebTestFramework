package com.scmp.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * CommandPrompt - Utility class for executing system commands.
 */
public class CommandPrompt {
	private Process process;
	private static final Logger frameworkLogger = LoggerFactory.getLogger(CommandPrompt.class);

	private static final String[] WIN_RUNTIME = {"cmd.exe", "/C"};
	private static final String[] OS_LINUX_RUNTIME = {"/bin/bash", "-l", "-c"};

	public CommandPrompt() {
	}

	/**
	 * Concatenates two arrays.
	 *
	 * @param first  the first array
	 * @param second the second array
	 * @param <T>    the type of the arrays
	 * @return the concatenated array
	 */
	private static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * Executes a system command.
	 *
	 * @param command the command to execute
	 * @return the process running the command
	 * @throws InterruptedException if the thread is interrupted
	 * @throws IOException          if an I/O error occurs
	 */
	public static Process executeCommand(String command) throws InterruptedException, IOException {
		Process tempProcess;
		ProcessBuilder tempBuilder;

		String os = System.getProperty("os.name");
		frameworkLogger.info("Running Command on [" + os + "]: " + command);

		String[] allCommand;
		// Build command process according to OS
		if (os.contains("Windows")) {
			allCommand = concat(WIN_RUNTIME, new String[]{command});
		} else {
			allCommand = concat(OS_LINUX_RUNTIME, new String[]{command});
		}

		tempBuilder = new ProcessBuilder(allCommand);
		tempBuilder.redirectErrorStream(true);
		Thread.sleep(1000);
		tempProcess = tempBuilder.start();

		return tempProcess;
	}

	/**
	 * Runs a command and returns the output.
	 *
	 * @param command the command to run
	 * @return a list of output lines
	 * @throws InterruptedException if the thread is interrupted
	 * @throws IOException          if an I/O error occurs
	 */
	public ArrayList<String> runCommand(String command) throws InterruptedException, IOException {
		process = executeCommand(command);

		// Get standard output
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		ArrayList<String> allLine = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty()) continue;
			allLine.add(line);
		}

		return allLine;
	}

	/**
	 * Destroys the running process.
	 */
	public void destroy() {
		process.destroy();
	}

	/**
	 * StreamDrainer - Runnable class for draining input streams.
	 */
	static class StreamDrainer implements Runnable {
		private final BufferedReader reader;

		public StreamDrainer(BufferedReader ins) {
			this.reader = ins;
		}

		public void run() {
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					// Uncomment the following lines if needed
					// if (!"ON".equalsIgnoreCase(TestContext.getInstance().getVariable("CMD_Mode"))) {
					//     System.out.println(line);
					// }
				}
			} catch (IOException e) {
				frameworkLogger.error("Ops!", e);
			}
		}
	}
}