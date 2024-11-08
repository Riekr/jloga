package org.riekr.jloga.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchRegistry;
import org.riekr.jloga.search.SearchRegistry.Entry;
import org.riekr.jloga.utils.FileUtils;

import com.google.gson.Gson;

public class ExtSearchRegistry {

	private static final Map<Object, List<Entry>> _ENTRIES     = new HashMap<>();
	private static final String                   _KEY_SYSPROP = "jloga.ext.dir";
	private static final String                   _KEY_RES     = "res://jloga.scripts";

	public static void init() {
		// load external searches modules
		String extPath = System.getProperty(_KEY_SYSPROP);
		if (extPath != null)
			scanDir(_KEY_SYSPROP, new File(extPath));

		try (BufferedReader br = FileUtils.readFrom(_KEY_RES, false)) {
			load("RES", _KEY_RES, br.lines().flatMap(definition -> {
				try (BufferedReader src = FileUtils.readFrom("res://" + definition, true)) {
					System.err.println("Loading resource " + definition);
					return fromReader(definition, src);
				} catch (Throwable e) {
					System.err.println("Unable to load jloga script " + definition);
					e.printStackTrace(System.err);
					return Stream.empty();
				}
			}));
		} catch (IOException e) {
			System.err.println("Unable to load jloga scripts resource");
		}

		Preferences.EXT_DIR.subscribe(key -> scanDir(Preferences.EXT_DIR, key));
	}

	public static void scanDir(Object key, File extPath) {
		if (extPath != null) {
			try (Stream<Path> files = Files.list(extPath.toPath())) {
				load("EXT", key, files
						.filter((f) -> Files.isRegularFile(f) && f.toString().toLowerCase().endsWith(".jloga.json"))
						.flatMap(ExtSearchRegistry::fromFile));
			} catch (Throwable e) {
				System.err.println("Unable to scanDir");
				e.printStackTrace(System.err);
			}
		}
	}

	private static synchronized void load(@NotNull String prefix, Object key, @NotNull Stream<ExtProcessConfig> configStream) {
		try {
			List<Entry> list = _ENTRIES.computeIfAbsent(key, k -> new ArrayList<>());
			list.forEach(SearchRegistry::remove);
			list.clear();
			configStream.sorted(Comparator.<ExtProcessConfig>comparingInt(c -> c.order).thenComparing(c -> c._id))
					.map((config) -> new Entry(key, config._id, (level) -> config.toComponent(config._id, level), config.description) {
						@Override
						public String toString() {
							return prefix + ": " + super.toString();
						}
					})
					.peek(SearchRegistry::register)
					.forEach(list::add);
		} catch (Throwable e) {
			System.err.println("Unable to load");
			e.printStackTrace(System.err);
		}
	}

	private static @NotNull Stream<ExtProcessConfig> fromFile(Path f) {
		// System.out.println("LOADING EXT: " + config);
		try (BufferedReader reader = Files.newBufferedReader(f)) {
			return fromReader(f.getFileName().toString(), reader)
					.peek(e -> e.guessWorkingDir(f));
		} catch (Throwable e) {
			System.err.println("Unable to read " + f);
			e.printStackTrace(System.err);
			return Stream.empty();
		}
	}

	private static @NotNull Stream<ExtProcessConfig> fromReader(String fn, BufferedReader reader) {
		try {
			final ExtProcessConfig extProcessConfig = new Gson().fromJson(reader, ExtProcessConfig.class);
			if (extProcessConfig.enabled) {
				extProcessConfig.guessOrdering(fn);
				return Stream.of(extProcessConfig);
			}
		} catch (Throwable e) {
			System.err.println("Unable to parse");
			e.printStackTrace(System.err);
		}
		return Stream.empty();
	}

	private ExtSearchRegistry() {}
}
