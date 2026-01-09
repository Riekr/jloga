package org.riekr.jloga.httpd;

import static org.riekr.jloga.utils.ExecUtils.validateCommand;
import static org.riekr.jloga.utils.ExecUtils.validateFlatpak;
import static org.riekr.jloga.utils.PopupUtils.popupError;
import static org.riekr.jloga.utils.PopupUtils.popupWarning;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.OSUtils;

class Browser {
	private Browser() {}

	private static void addIfExists(String file, Collection<String> dest) {
		if (file != null && !file.isEmpty() && new File(file).exists())
			dest.add(file);
	}

	private static String[] findWindowsExecutable(String url) {
		final Set<String> binDirs = new LinkedHashSet<>();
		addIfExists(System.getenv("ProgramFiles(x86)"), binDirs);
		addIfExists(System.getenv("ProgramFiles"), binDirs);
		addIfExists(System.getenv("LocalAppData"), binDirs);
		for (String binDir : binDirs) {
			File f;
			// "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --profile-directory="Personale"
			if ((f = new File(binDir, "Microsoft\\Edge\\Application\\msedge.exe")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			// https://stackoverflow.com/questions/40674914/google-chrome-path-in-windows-10
			if ((f = new File(binDir, "Google\\Chrome\\Application\\chrome.exe")).canExecute())
				return new String[]{f.getAbsolutePath(), "--new-window", "--app=" + url};
			if ((f = new File(binDir, "Mozilla Firefox\\firefox.exe")).canExecute())
				return new String[]{f.getAbsolutePath(), "--private-window=" + url};
			// TODO: add other chromium browsers
			// TODO: add firefox
		}
		return null;
	}

	private static String[] findUnixExecutable(String url) {
		final Set<String> binDirs = new LinkedHashSet<>();
		addIfExists("/usr/bin", binDirs);
		addIfExists("/usr/local/bin", binDirs);
		addIfExists("/opt/bin", binDirs);
		Optional.of(System.getenv("PATH")).stream()
				.flatMap(s -> Arrays.stream(s.split(":")))
				.forEach(binDirs::add);
		return validateCommand(binDirs, "chromium-freeworld", "--new-window", "--app=" + url)
				.or(() -> validateFlatpak("io.github.ungoogled_software.ungoogled_chromium", "--new-window", "--app=" + url))
				.or(() -> validateCommand(binDirs, "chromium", "--new-window", "--app=" + url))
				.or(() -> validateFlatpak("org.chromium.Chromium", "--new-window", "--app=" + url))
				// google chrome
				.or(() -> validateCommand(binDirs, "google-chrome", "--new-window", "--app=" + url))
				.or(() -> validateFlatpak("com.google.Chrome", "--new-window", "--app=" + url))
				// microsoft edge
				.or(() -> validateCommand(binDirs, "microsoft-edge", "--new-window", "--app=" + url))
				.or(() -> validateFlatpak("com.microsoft.Edge", "--new-window", "--app=" + url))
				// firefox (does not support "app" look and feel)
				.or(() -> validateCommand(binDirs, "firefox", "--private-window=" + url))
				.or(() -> validateFlatpak("org.mozilla.firefox", "--private-window=" + url))
				.or(() -> validateFlatpak("org.mozilla.Firefox", "--private-window=" + url))
				.orElse(null);
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
