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
			.describe(Type.Font, "Text font", "Font used in text viewers, using a monospace font is recommended.");

	GUIPreference<Integer> PAGE_SCROLL_DIVIDER = of("PageDivider", 3, 1, Integer.MAX_VALUE)
			.describe(Type.Combo, "Page scroll size:", "Select how many of the visible lines should be scrolled when paging text.")
			.add("Full page", 1)
			.add("\u00BD page", 2)
			.add("\u2153 of page", 3)
			.add("\u00BC of page", 4)
			.add("\u2155 of page", 5);

	GUIPreference<Charset> CHARSET = of("CharsetCombo", UTF_8)
			.describe(Type.Combo, "Charset", "Select the charset used for opening the next files. Using UTF-8 or ISO-8859-1 is recommended.")
			.add(Charset::availableCharsets);

	GUIPreference<ThreadModel> MT_MODEL = of("Multithreading.model", () -> getRuntime().availableProcessors() > 1 ? ThreadModel.STREAM : ThreadModel.SYNC, ThreadModel.class)
			.describe(Type.Combo, "Thread model", "Select which threading model should be used for simple searches (eg: plain text and regex).")
			.add(SimpleSearchPredicate::getThreadModels);

	GUIPreference<Boolean> AUTO_GRID = of("Grid.auto", () -> true).describe(Type.Toggle, "Automatic grid",
			"When checked files with extensions '.tsv' and '.csv' will be automatically opened in grid view");
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
