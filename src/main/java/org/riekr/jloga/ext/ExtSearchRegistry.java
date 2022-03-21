package org.riekr.jloga.ext;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchRegistry;
import org.riekr.jloga.search.SearchRegistry.Entry;

import com.google.gson.Gson;

public class ExtSearchRegistry {

	private static List<Entry<?>> _ENTRIES = emptyList();

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
				ArrayList<Map.Entry<Integer, Entry<?>>> res = new ArrayList<>();
				for (Path f : Files.list(extPath.toPath()).collect(toList())) {
					try {
						if (Files.isRegularFile(f) && f.toString().toLowerCase().endsWith(".jloga.json")) {
							try (BufferedReader reader = Files.newBufferedReader(f)) {
								ExtProcessConfig config = gson.fromJson(reader, ExtProcessConfig.class);
								System.out.println("LOADING EXT: " + config);
								if (config.workingDirectory == null)
									config.workingDirectory = extPath;
								String id = f.toAbsolutePath().toString();
								res.add(new AbstractMap.SimpleEntry<>(
										config.order,
										new Entry<>(id, (level) -> new ExtProcessComponent(id, config.icon, config.label, config.workingDirectory, config.command), config.description))
								);
							}
						}
					} catch (Throwable e) {
						System.err.println("Unable to parse " + f + " as external script");
						e.printStackTrace(System.err);
					}
				}
				res.sort(comparingInt(Map.Entry::getKey));
				_ENTRIES = res.stream().map(Map.Entry::getValue).collect(toList());
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
