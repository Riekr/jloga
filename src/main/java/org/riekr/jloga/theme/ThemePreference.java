package org.riekr.jloga.theme;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.GUIPreference;
import org.riekr.jloga.prefs.Preference;

public class ThemePreference extends GUIPreference<Theme> {

	private static Theme getTheme(Preference<String> pref, String className) {
		try {
			return new Theme(className);
		} catch (ClassNotFoundException ignored) {
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
		Theme def = getDefault();
		pref.set(def.toString());
		return def;
	}

	@NotNull
	public static Theme getDefault() {
		return ThemeRepository.availableThemes().values().iterator().next();
	}

	public ThemePreference(String title) {
		super(Preference.of("Theme", () -> getDefault().toString())
				.withConversion(ThemePreference::getTheme, (p, t) -> t.className), Type.Combo, title);
		add(ThemeRepository::availableThemes);
	}

	@Override
	public boolean available() {
		return !ThemeRepository.availableThemes().isEmpty();
	}

}
