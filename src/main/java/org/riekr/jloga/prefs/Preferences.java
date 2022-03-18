package org.riekr.jloga.prefs;

import static java.lang.Runtime.getRuntime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.riekr.jloga.prefs.Preference.of;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.riekr.jloga.prefs.GUIPreference.Type;
import org.riekr.jloga.search.RegExComponent;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.search.simple.SimpleSearchPredicate.ThreadModel;

public interface Preferences {

	//region GUI editable preferences
	GUIPreference<Font> FONT = of("Font", () -> new Font("monospaced", Font.PLAIN, 12))
			.describe(Type.Font, "Text font")
			.addDescription("Font used in text viewers, using a monospace font is recommended.");

	GUIPreference<Integer> PAGE_SCROLL_DIVIDER = of("PageDivider", 3, 1, Integer.MAX_VALUE)
			.describe(Type.Combo, "Page scroll size:")
			.addDescription("Select how many of the visible lines should be scrolled when paging text.")
			.add("Full page", 1)
			.add("\u00BD page", 2)
			.add("\u2153 of page", 3)
			.add("\u00BC of page", 4)
			.add("\u2155 of page", 5);

	GUIPreference<Charset> CHARSET = of("CharsetCombo", UTF_8)
			.describe(Type.Combo, "Charset")
			.addDescription("Select the charset used for opening the next files. Using UTF-8 or ISO-8859-1 is recommended.")
			.add(Charset::availableCharsets);

	GUIPreference<ThreadModel> MT_MODEL = of("Multithreading.model", () -> getRuntime().availableProcessors() > 1 ? ThreadModel.STREAM : ThreadModel.SYNC, ThreadModel.class)
			.describe(Type.Combo, "Thread model")
			.addDescription("Select which threading model should be used for simple searches (eg: plain text and regex).")
			.add(SimpleSearchPredicate::getThreadModels);

	GUIPreference<Boolean> AUTO_GRID = of("Grid.auto", () -> true).describe(Type.Toggle, "Automatic grid")
			.addDescription("When checked files with extensions '.tsv' and '.csv' will be automatically opened in grid view");

	GUIPreference<File> EXT_DIR = of("ext.dir", () -> (String)null).withConversion(File::new, File::getAbsolutePath)
			.describe(Type.Directory, "Extension scripts folder")
			.addDescription("A folder that contains a set of '.jloga.json' files describing external scripts to use as search implementations.");

	GUIPreference<Integer> PAGE_SIZE = of("page_size", () -> 1024 * 1024)
			.describe(Type.Combo, "Size of disk pages")
			.addDescription("Text files will be read in blocks of this size, a lower size will reduce disk i/o but increase memory usage and vice-versa.")
			.addDescription("1MB is generally recommended.")
			.add("256kB", 256 * 1024)
			.add("512kB", 512 * 1024)
			.add("1MB", 1024 * 1024)
			.add("2MB", 1024 * 1024 * 2)
			.add("4MB", 1024 * 1024 * 4);
	//endregion

	//region Hidden (state) preferences
	Preference<File>        LAST_OPEN_PATH   = of("LastOpen", () -> ".").withConversion(File::new, File::getAbsolutePath);
	Preference<File>        LAST_SAVE_PATH   = of("LastSave", () -> ".").withConversion(File::new, File::getAbsolutePath);
	KeyedPreference<String> LAST_SEARCH_TYPE = of("SearchType", () -> RegExComponent.ID);
	//endregion

	static List<GUIPreference<?>> getGUIPreferences() {
		ArrayList<GUIPreference<?>> res = new ArrayList<>();
		for (Field f : Preferences.class.getFields()) {
			if (GUIPreference.class.isAssignableFrom(f.getType())) {
				try {
					res.add((GUIPreference<?>)f.get(null));
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return res;
	}

}
