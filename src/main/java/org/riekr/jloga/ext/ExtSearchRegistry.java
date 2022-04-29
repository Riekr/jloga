package org.riekr.jloga.ext;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchRegistry;
import org.riekr.jloga.search.SearchRegistry.Entry;

import com.google.gson.Gson;

public class ExtSearchRegistry {

	private static List<Entry> _ENTRIES = emptyList();

	public static void init() {
		// load external searches modules
		String extPath = System.getProperty("jloga.ext.dir");
		if (extPath != null)
			scanDir(new File(extPath));
		if (_ENTRIES.isEmpty())
			Preferences.EXT_DIR.subscribe(ExtSearchRegistry::scanDir);
	}

	public static void scanDir(File extPath) {
		try {
			_ENTRIES.forEach(SearchRegistry::remove);
			if (extPath != null) {
				Gson gson = new Gson();
				_ENTRIES = Files.list(extPath.toPath())
						.filter((f) -> Files.isRegularFile(f) && f.toString().toLowerCase().endsWith(".jloga.json"))
						.map((f) -> {
							// System.out.println("LOADING EXT: " + config);
							try (BufferedReader reader = Files.newBufferedReader(f)) {
								ExtProcessConfig config = gson.fromJson(reader, ExtProcessConfig.class);
								if (config.workingDirectory == null)
									config.workingDirectory = extPath.getAbsolutePath();
								config._id = f.getFileName().toString();
								return config;
							} catch (Throwable e) {
								System.err.println("Unable to read " + f);
								e.printStackTrace(System.err);
								return null;
							}
						})
						.filter((config) -> config != null && config.enabled)
						.sorted(comparingInt(c -> c.order))
						.map((config) -> new Entry(config._id, (level) -> config.toComponent(config._id, level), config.description) {
							@Override
							public String toString() {
								if (Preferences.EXT_PREFIX_SKIP.get())
									return super.toString();
								return "EXT: " + super.toString();
							}
						})
						.collect(toList());
				_ENTRIES.forEach(SearchRegistry::register);
				return;
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
		_ENTRIES = emptyList();
	}

	private ExtSearchRegistry() {}
}
