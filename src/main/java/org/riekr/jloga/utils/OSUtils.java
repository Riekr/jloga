package org.riekr.jloga.utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class OSUtils {

	private OSUtils() {}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name", "unknown").trim().toLowerCase();
		// https://www.mindprod.com/jgloss/properties.html#OSNAME
		return osName.startsWith("windows");
	}

	public static void openInFileManager(File file) {
		if (file.isFile())
			file = file.getParentFile();
		try {
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
