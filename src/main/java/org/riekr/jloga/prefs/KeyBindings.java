package org.riekr.jloga.prefs;

import static org.riekr.jloga.prefs.GUIPreference.Type.KeyBinding;
import static org.riekr.jloga.prefs.Preference.of;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.riekr.jloga.utils.KeyUtils;

public interface KeyBindings {

	String KEYS = "Key bindings";

	private static GUIPreference<KeyStroke> keyPref(String key, KeyStroke deflt, String title) {
		return of(key, deflt::toString).withConversion(KeyStroke::getKeyStroke, KeyStroke::toString)
				.describe(KeyBinding, title).group(KEYS);
	}

	GUIPreference<KeyStroke> KB_OPENFILE = keyPref("kb.openFile", KeyUtils.CTRL_O, "Open file");
	GUIPreference<KeyStroke> KB_SETTINGS = keyPref("kb.settings", KeyUtils.CTRL_COMMA, "Open settings");

	GUIPreference<KeyStroke> KB_FINDTEXT   = keyPref("kb.findText", KeyUtils.CTRL_F, "Find plain text");
	GUIPreference<KeyStroke> KB_FINDREGEX  = keyPref("kb.findRegex", KeyUtils.CTRL_R, "Find regex text");
	GUIPreference<KeyStroke> KB_FINDSELECT = keyPref("kb.findSelect", KeyUtils.CTRL_DOT, "Open search type selector");


	static List<GUIPreference<?>> getGUIKeyBindings() {
		ArrayList<GUIPreference<?>> res = new ArrayList<>();
		for (Field f : KeyBindings.class.getFields()) {
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
