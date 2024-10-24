package com.scmp.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class CommandPrompt {
	Process process;
	private static final Logger frameworkLogger = LoggerFactory.getLogger(CommandPrompt.class);

	private static final String[] WIN_RUNTIME = {"cmd.exe", "/C"};
	private static final String[] OS_LINUX_RUNTIME = {"/bin/bash", "-l", "-c"};

	public CommandPrompt() {
	}

	private static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static Process executeCommand(String command) throws InterruptedException, IOException {
		Process tempProcess;
		ProcessBuilder tempBuilder;

		String os = System.getProperty("os.name");
		frameworkLogger.info("Running Command on [" + os + "]: " + command);

		String[] allCommand;
		// build cmd proccess according to os
		if (os.contains("Windows")) // if windows
		{
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
	 * This method run command on windows and mac
	 *
	 * @param command to run
	 */
	public ArrayList<String> runCommand(String command) throws InterruptedException, IOException {
		process = executeCommand(command);

		// get std output
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = "";
		ArrayList<String> allLine = new ArrayList<>();
		while ((line = reader.readLine()) != null) {

			if (line.isEmpty()) continue;

			allLine.add(line);
		}

		return allLine;
	}

	public void destory() {
		process.destroy();
	}

	class StreamDrainer implements Runnable {
		private BufferedReader reader;

		public StreamDrainer(BufferedReader ins) {
			this.reader = ins;
		}

		public void run() {
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
//					if(!"ON".equalsIgnoreCase(TestContext.getInstance().getVariable("CMD_Mode")))
//					{
//						System.out.println(line);
//					}
				}

			} catch (IOException e) {
				frameworkLogger.error("Ops!", e);
			}
		}
	}

}
