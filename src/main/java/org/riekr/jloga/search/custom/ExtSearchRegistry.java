package org.riekr.jloga.search.custom;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.riekr.jloga.search.SearchRegistry.Entry;

import com.google.gson.Gson;

public class ExtSearchRegistry {

	private static List<Entry<?>> _ENTRIES = Collections.emptyList();

	static {
		// load external searches modules
		String extPath = System.getProperty("jloga.ext.dir");
		if (extPath != null) {
			try {
				_ENTRIES = scanDir(extPath);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public static List<Entry<?>> scanDir(String extPath) throws IOException {
		Gson gson = new Gson();
		ArrayList<Entry<?>> res = new ArrayList<>();
		for (Path f : Files.list(Path.of(extPath)).collect(toList())) {
			if (Files.isRegularFile(f) && f.toString().toLowerCase().endsWith(".jloga.json")) {
				try (BufferedReader reader = Files.newBufferedReader(f)) {
					ExtProcessConfig config = gson.fromJson(reader, ExtProcessConfig.class);
					System.out.println("LOADING EXT: " + config);
					String id = f.toAbsolutePath().toString();
					res.add(new Entry<>(id, (level) -> new ExtProcessComponent(id, config.icon, config.label, config.command), config.description));
				}
			}
		}
		return res;
	}

	public static void forEach(Consumer<Entry<?>> consumer) {
		_ENTRIES.forEach(consumer);
	}

	private ExtSearchRegistry() {}
}
