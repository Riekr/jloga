package org.riekr.jloga.ext;

import static java.util.stream.Collectors.joining;
import static org.riekr.jloga.utils.PopupUtils.popupError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.OSUtils;
import org.riekr.jloga.utils.TextUtils;

class ExtEnv {

	public static void read(File workingDir, Map<String, String> dest) {
		readFile(new File(workingDir, "env.jloga.properties"), dest, Function.identity());
		if (OSUtils.isWindows()) {
			Pattern pat = Pattern.compile("%([\\w_-]+)%");
			readFile(new File(workingDir, "env-windows.jloga.properties"), dest, (v) -> replaceFromEnv(v, pat));
		} else {
			Pattern pat = Pattern.compile("\\$\\{([\\w_-]+)}");
			readFile(new File(workingDir, "env-unix.jloga.properties"), dest, (v) -> replaceFromEnv(v, pat), ExtEnv::fixHomePath);
		}
		Preferences.EXT_ENV.tap(bag ->
				bag.stream().map(Map.Entry::getKey).filter(Objects::nonNull).map(Object::toString).distinct().forEach(envName -> {
					if (!envName.isEmpty()) {
						String envValue = bag.stream().map(Map.Entry::getValue).filter(Objects::nonNull).map(Object::toString).collect(joining(File.pathSeparator));
						if (!envValue.isEmpty())
							dest.put(envName, envValue);
					}
				}));
	}

	private static void readFile(File envFile, Map<String, String> dest, Function<String, String> mapper1, Function<String, String> mapper2) {
		readFile(envFile, dest, mapper1.andThen(mapper2));
	}

	private static void readFile(File envFile, Map<String, String> dest, Function<String, String> mapper) {
		if (envFile.isFile() && envFile.canRead()) {
			Pattern selfPropPattern = Pattern.compile("%\\{([\\w._-]+)}");
			try (Reader reader = new BufferedReader(new FileReader(envFile))) {
				Properties props = new Properties();
				props.load(reader);
				BiFunction<String, String, String> f = Boolean.getBoolean("jloga.env.override") ? dest::putIfAbsent : dest::put;
				props.forEach((k, v) -> f.apply(String.valueOf(k), mapper.apply(TextUtils.replaceRegex(String.valueOf(v), selfPropPattern, dest))));
			} catch (IOException e) {
				popupError("Unable to read " + envFile, e);
			}
		}
	}

	private static String fixHomePath(@NotNull String val) {
		if (val.startsWith("~" + File.separatorChar))
			return System.getProperty("user.home") + val.substring(1);
		return val;
	}

	private static String replaceFromEnv(@NotNull String val, Pattern pat) {
		return TextUtils.replaceRegex(val, pat, System.getenv());
	}

	private ExtEnv() {}
}
