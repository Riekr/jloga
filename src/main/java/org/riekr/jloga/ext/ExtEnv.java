package org.riekr.jloga.ext;

import static javax.swing.JOptionPane.showMessageDialog;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.utils.OSUtils;

class ExtEnv {

	public static void read(File workingDir, Map<String, String> dest) {
		readFile(new File(workingDir, "env.jloga.properties"), dest);
		if (OSUtils.isWindows())
			readFile(new File(workingDir, "env-windows.jloga.properties"), dest);
		else
			readFile(new File(workingDir, "env-unix.jloga.properties"), dest, ExtEnv::fixHomePath);
	}

	private static void readFile(File envFile, Map<String, String> dest) {
		readFile(envFile, dest, Function.identity());
	}

	private static void readFile(File envFile, Map<String, String> dest, Function<String, String> mapper) {
		if (envFile.isFile() && envFile.canRead()) {
			try (Reader reader = new BufferedReader(new FileReader(envFile))) {
				Properties props = new Properties();
				props.load(reader);
				props.forEach((k, v) -> dest.put(String.valueOf(k), mapper.apply(String.valueOf(v))));
			} catch (IOException e) {
				showMessageDialog(Main.getMain(), e.getLocalizedMessage(), "Unable to read " + envFile, JOptionPane.WARNING_MESSAGE);
				e.printStackTrace(System.err);
			}
		}
	}

	private static String fixHomePath(@NotNull String val) {
		if (val.startsWith("~" + File.separatorChar))
			return System.getProperty("user.home") + val.substring(1);
		return val;
	}

	private ExtEnv() {}
}
