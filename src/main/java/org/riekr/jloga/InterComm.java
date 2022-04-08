package org.riekr.jloga;

import static java.io.File.pathSeparator;
import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class InterComm extends ServerSocket implements Runnable {

	private static final Preferences PREFS = Preferences.userNodeForPackage(InterComm.class);

	private static final String PREF_PID  = "pid";
	private static final String PREF_PORT = "port";

	private static final String CMD_OPEN = "open";
	private static final String CMD_INFO = "info";

	private static boolean sync() {
		try {
			PREFS.sync();
			return true;
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

	static boolean isAlive() {
		long otherPid = PREFS.getLong(PREF_PID, 0);
		return otherPid != 0 && ProcessHandle.of(otherPid).isPresent();
	}

	static void sendFileOpenCommand(List<File> files) {
		if (files != null && !files.isEmpty())
			sendCommand(CMD_OPEN, files.stream().map(File::getAbsolutePath).collect(joining(pathSeparator)));
	}

	static void sendInfoCommand() {
		sendCommand(CMD_INFO);
	}

	static void sendCommand(String cmd, String... args) {
		int port = PREFS.getInt(PREF_PORT, 0);
		if (port != 0) {
			try (Socket socket = new Socket(InetAddress.getLocalHost(), port)) {
				try (
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
				) {
					// write
					out.writeLong(ProcessHandle.current().pid());
					out.writeUTF(cmd);
					for (final String arg : args)
						out.writeUTF(arg);
					// response?
					String line;
					while ((line = reader.readLine()) != null)
						System.out.println(line);
				}
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		}
	}

	static void start(Consumer<List<File>> openFile) {
		InterComm interComm;
		try {
			interComm = new InterComm(openFile);
			interComm.bind(interComm.getLocalSocketAddress());
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return;
		}
		PREFS.putLong(PREF_PID, ProcessHandle.current().pid());
		PREFS.putInt(PREF_PORT, interComm.getLocalPort());
		if (sync()) {
			Thread th = new Thread(interComm, "InterComm");
			th.setDaemon(true);
			th.start();
		} else {
			try {
				interComm.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private final Consumer<List<File>> _openFile;

	private InterComm(Consumer<List<File>> openFile) throws IOException {
		_openFile = openFile;
	}

	public void run() {
		try {
			while (!isClosed()) {
				try (Socket socket = accept()) {
					try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
						if (ProcessHandle.of(in.readLong()).isEmpty()) {
							// not a local process, may not be another instance of jloga
							continue;
						}
						String command = in.readUTF();
						switch (command) {

							case CMD_OPEN: {
								String files = in.readUTF();
								_openFile.accept(
										list(new StringTokenizer(files, pathSeparator)).stream()
												.map((fileName) -> new File((String)fileName))
												.filter(File::canRead)
												.collect(toList())
								);
								break;
							}

							case CMD_INFO: {
								PrintStream out = new PrintStream(socket.getOutputStream(), false);
								Runtime rt = Runtime.getRuntime();
								out.println("Available Processors = " + rt.availableProcessors());
								out.println("Heap Memory = " + rt.totalMemory());
								out.println("Free Memory = " + rt.freeMemory());
								out.println("Max Memory = " + rt.maxMemory());
								out.flush();
								break;
							}

							default:
								System.err.println("Unknown InterComm command: " + command);
						}
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

}
