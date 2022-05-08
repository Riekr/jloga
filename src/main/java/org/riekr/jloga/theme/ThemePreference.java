package org.riekr.jloga.theme;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.riekr.jloga.prefs.GUIPreference;
import org.riekr.jloga.prefs.Preference;

public class ThemePreference extends GUIPreference<Theme> {

	public ThemePreference(String title) {
		super(Preference.of("Theme", ThemePreference::getDefault, Theme.class), Type.Combo, title);
		availableThemes().forEach((theme) -> add(theme.name, theme));
	}

	@Override
	protected Map<String, Theme> newValuesMap() {
		return new LinkedHashMap<>();
	}

	public static Stream<Theme> availableThemes() {
		return stream(Theme.values())
				.filter((theme) -> theme.name != null)
				.sorted(comparing(Theme::ordinal));
	}

	public static Theme getDefault() {
		return availableThemes().findFirst()
				.orElse(null);
	}

	@Override
	public boolean available() {
		return availableThemes().count() > 1;
	}

}
