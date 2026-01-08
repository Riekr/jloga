package org.riekr.jloga.utils;

import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ExecUtils {

	public static Optional<String[]> validateCommand(Collection<String> binDirs, String command, String... args) {
		for (final String binDir : binDirs) {
			final File f = new File(binDir, command);
			if (f.canExecute()) {
				final String[] res = new String[args.length + 1];
				res[0] = f.getAbsolutePath();
				System.arraycopy(args, 0, res, 1, args.length);
				return Optional.of(res);
			}
		}
		return Optional.empty();
	}

	public static Optional<String[]> validateFlatpak(String pkg, String... args) {
		if (OSUtils.isWindows())
			return Optional.empty();
		final Set<String> flatpaks = ExecUtils.streamCommandOutput("flatpak", "list", "--columns=application", "--app")
				.skip(1)
				.collect(toSet());
		if (flatpaks.contains(pkg)) {
			final String[] res = new String[args.length + 3];
			res[0] = "flatpak";
			res[1] = "run";
			res[2] = pkg;
			System.arraycopy(args, 0, res, 3, args.length);
			return Optional.of(res);
		}
		return Optional.empty();
	}

	public static Stream<String> streamCommandOutput(String... command) {
		try {
			Process proc = new ProcessBuilder()
					.inheritIO()
					.redirectOutput(ProcessBuilder.Redirect.PIPE)
					.command(command)
					.start();
			return new BufferedReader(new InputStreamReader(proc.getInputStream())).lines();
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			return Stream.empty();
		}
	}

	private ExecUtils() {}


}
