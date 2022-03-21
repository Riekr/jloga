package org.riekr.jloga.utils;

public class OSUtils {

	private OSUtils() {}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name", "unknown").trim().toLowerCase();
		// https://www.mindprod.com/jgloss/properties.html#OSNAME
		return osName.startsWith("windows");
	}
}
