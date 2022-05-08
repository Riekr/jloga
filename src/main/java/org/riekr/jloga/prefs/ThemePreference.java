package org.riekr.jloga.prefs;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ThemePreference extends GUIPreference<ThemePreference.Theme> {

	private static final Map<Object, Object> _FLATLAF_DEFAULTS = singletonMap("ScrollBar.minimumThumbSize", new Dimension(8, 20));
	private static       Map<Object, Object> _APPLIED_DEFAULTS;

	public ThemePreference(String title) {
		super(Preference.of("Theme", ThemePreference::getDefault, Theme.class), Type.Combo, title);
		availableThemes().forEach((theme) -> add(theme.description, theme));
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

	public static boolean apply(Theme theme) {
		UIDefaults defaults = UIManager.getDefaults();
		if (_APPLIED_DEFAULTS != null) {
			_APPLIED_DEFAULTS.keySet().forEach(defaults::remove);
			_APPLIED_DEFAULTS = null;
		}
		try {
			UIManager.setLookAndFeel(theme.clazz);
			if (theme.params != null)
				defaults.putAll(theme.params);
			_APPLIED_DEFAULTS = theme.params;
			return true;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

	@Override
	public boolean available() {
		return availableThemes().count() > 1;
	}

	@SuppressWarnings("SpellCheckingInspection")
	public enum Theme {

		// https://www.formdev.com/flatlaf/themes/
		// FlatDarkLaf("com.formdev.flatlaf.FlatDarkLaf", _FLATLAF_DEFAULTS, "FlatLaf Dark"),
		FlatDarculaLaf("com.formdev.flatlaf.FlatDarculaLaf", _FLATLAF_DEFAULTS, "FlatLaf Dark (Darcula)"),
		// FlatLightLaf("com.formdev.flatlaf.FlatLightLaf", _FLATLAF_DEFAULTS, "FlatLaf Light"),
		FlatIntelliJLaf("com.formdev.flatlaf.FlatIntelliJLaf", _FLATLAF_DEFAULTS, "FlatLaf Light (IntelliJ)"),

		// Built-ins
		MetalLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel", null, "Metal"),
		NimbusLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel", null, "Nimbus"),
		GTKLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel", null, "GTK"),
		WindowsLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel", null, "Windows"),
		WindowsClassicLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel", null, "Windows Classic"),
		MotifLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel", null, "CDE/Motif");

		public final String              clazz;
		public final Map<Object, Object> params;
		public final String              description;

		Theme(String clazz, Map<Object, Object> params, String description) {
			this.clazz = clazz;
			this.params = params;
			this.description = description;
		}

		public boolean available() {
			try {
				Class.forName(clazz);
				return true;
			} catch (Throwable ignored) {}
			return false;
		}
	}

}
