package org.riekr.jloga.theme;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.riekr.jloga.prefs.GUIPreference;
import org.riekr.jloga.prefs.Preference;

public class ThemePreference extends GUIPreference<Theme> {

	public ThemePreference(String title) {
		super(Preference.of("Theme", ThemePreference::getDefault, Theme.class), Type.Combo, title);
		add(() -> availableThemes()
				.collect(toMap(Theme::description, identity(), (a, b) -> a, LinkedHashMap::new)));
	}

	@Override
	protected Map<String, Theme> newValuesMap() {
		return new LinkedHashMap<>();
	}

	public static Stream<Theme> availableThemes() {
		return stream(Theme.values())
				.filter(Theme::available)
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
