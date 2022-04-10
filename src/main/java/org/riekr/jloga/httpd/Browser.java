package org.riekr.jloga.httpd;

import static org.riekr.jloga.utils.PopupUtils.popupError;
import static org.riekr.jloga.utils.PopupUtils.popupWarning;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.OSUtils;

class Browser {
	private Browser() {}

	private static void addIfExists(String file, List<String> dest) {
		if (file != null && !file.isEmpty() && new File(file).exists())
			dest.add(file);
	}

	private static String[] findWindowsExecutable(String url) {
		List<String> programFiles = new ArrayList<>();
		addIfExists(System.getenv("ProgramFiles(x86)"), programFiles);
		addIfExists(System.getenv("ProgramFiles"), programFiles);
		addIfExists(System.getenv("LocalAppData"), programFiles);
		for (String programFilesDir : programFiles) {
			File f;
			// "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --profile-directory="Personale"
			if ((f = new File(programFilesDir, "Microsoft\\Edge\\Application\\msedge.exe")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			// https://stackoverflow.com/questions/40674914/google-chrome-path-in-windows-10
			if ((f = new File(programFilesDir, "Google\\Chrome\\Application\\chrome.exe")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			// TODO: add other chromium browsers, firefox unfortunately does not work with finos perspective
		}
		return null;
	}

	private static String[] findUnixExecutable(String url) {
		// TODO: to be checked
		List<String> programFiles = new ArrayList<>();
		addIfExists("/usr/bin", programFiles);
		addIfExists("/usr/local/bin", programFiles);
		addIfExists("/opt/bin", programFiles);
		for (String programFilesDir : programFiles) {
			File f;
			if ((f = new File(programFilesDir, "chromium-freeworld")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			if ((f = new File(programFilesDir, "chromium")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			if ((f = new File(programFilesDir, "google-chrome")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			if ((f = new File(programFilesDir, "microsoft-edge")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			// TODO: add other chromium browsers, firefox unfortunately does not work with finos perspective
		}
		return null;
	}

	private static void exec(String[] command) {
		System.out.println("Executing " + Arrays.toString(command));
		try {
			new ProcessBuilder().inheritIO().command(command).start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void open(String url) {
		if (url == null || (url = url.trim()).isEmpty())
			throw new IllegalArgumentException("No url provided");
		String[] command = null;
		File custom = Preferences.BROWSER_CUSTOM.get();
		if (custom != null) {
			if (custom.canExecute())
				command = new String[]{custom.getAbsolutePath(), url};
			else {
				popupError('\'' + custom.getAbsolutePath() + "'\nis not executable, check you preferences.", "Invalid executable");
				Preferences.BROWSER_CUSTOM.reset();
			}
		}
		if (!Preferences.BROWSER_SYSTEM.get()) {
			if (command == null) {
				if (OSUtils.isWindows())
					command = findWindowsExecutable(url);
				else {
					// if not windows just assume os it is unix like
					command = findUnixExecutable(url);
				}
			}
			if (command != null) {
				exec(command);
				return;
			}
			// if no supported chromium based browser has been found try the standard/uglier/maybe unsupported one
			if (Preferences.BROWSER_WARN.get()) {
				popupWarning("No supported browser found, you may select one in preferences.", "Invalid browser");
				Preferences.BROWSER_WARN.set(false);
			}
		}
		Desktop desktop = Desktop.getDesktop();
		try {
			URI uri = new URI(url);
			desktop.browse(uri);
		} catch (Exception e) {
			throw new UnsupportedOperationException(e.getLocalizedMessage(), e);
		}
	}

}
