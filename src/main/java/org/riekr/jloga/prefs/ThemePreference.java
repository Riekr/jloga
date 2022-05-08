package org.riekr.jloga.prefs;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.formdev.flatlaf.FlatLaf;

public class ThemePreference extends GUIPreference<ThemePreference.Theme> {

	private static final Map<Object, Object> _FLATLAF_DEFAULTS = singletonMap("ScrollBar.minimumThumbSize", new Dimension(8, 20));
	private static       Map<Object, Object> _APPLIED_DEFAULTS;

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

	public static boolean apply(Theme theme) {
		UIDefaults defaults = UIManager.getDefaults();
		if (_APPLIED_DEFAULTS != null) {
			_APPLIED_DEFAULTS.keySet().forEach(defaults::remove);
			_APPLIED_DEFAULTS = null;
		}
		try {
			boolean applied = false;
			try {
				Class<?> cl = Class.forName(theme.clazz);
				if (FlatLaf.class.isAssignableFrom(cl)) {
					cl.getMethod("setup").invoke(null);
					applied = true;
				}
			} catch (Throwable ignored) {}
			if (!applied)
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
		FlatDarculaLaf("com.formdev.flatlaf.FlatDarculaLaf", _FLATLAF_DEFAULTS),
		// FlatLightLaf("com.formdev.flatlaf.FlatLightLaf", _FLATLAF_DEFAULTS, "FlatLaf Light"),
		FlatIntelliJLaf("com.formdev.flatlaf.FlatIntelliJLaf", _FLATLAF_DEFAULTS),

		// Extras
		FlatArcDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme", _FLATLAF_DEFAULTS),
		FlatArcDarkOrangeIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme", _FLATLAF_DEFAULTS),
		FlatArcIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcIJTheme", _FLATLAF_DEFAULTS),
		FlatArcOrangeIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme", _FLATLAF_DEFAULTS),
		FlatCarbonIJTheme("com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme", _FLATLAF_DEFAULTS),
		FlatCobalt2IJTheme("com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme", _FLATLAF_DEFAULTS),
		FlatCyanLightIJTheme("com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme", _FLATLAF_DEFAULTS),
		FlatDarkFlatIJTheme("com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme", _FLATLAF_DEFAULTS),
		FlatDarkPurpleIJTheme("com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme", _FLATLAF_DEFAULTS),
		FlatDraculaIJTheme("com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme", _FLATLAF_DEFAULTS),
		FlatGradiantoDarkFuchsiaIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme", _FLATLAF_DEFAULTS),
		FlatGradiantoDeepOceanIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme", _FLATLAF_DEFAULTS),
		FlatGradiantoMidnightBlueIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme", _FLATLAF_DEFAULTS),
		FlatGradiantoNatureGreenIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme", _FLATLAF_DEFAULTS),
		FlatGrayIJTheme("com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme", _FLATLAF_DEFAULTS),
		FlatGruvboxDarkHardIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme", _FLATLAF_DEFAULTS),
		FlatGruvboxDarkMediumIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme", _FLATLAF_DEFAULTS),
		FlatGruvboxDarkSoftIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme", _FLATLAF_DEFAULTS),
		FlatHiberbeeDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme", _FLATLAF_DEFAULTS),
		FlatHighContrastIJTheme("com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme", _FLATLAF_DEFAULTS),
		FlatLightFlatIJTheme("com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme", _FLATLAF_DEFAULTS),
		FlatMaterialDesignDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme", _FLATLAF_DEFAULTS),
		FlatMonocaiIJTheme("com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme", _FLATLAF_DEFAULTS),
		FlatMonokaiProIJTheme("com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme", _FLATLAF_DEFAULTS),
		FlatNordIJTheme("com.formdev.flatlaf.intellijthemes.FlatNordIJTheme", _FLATLAF_DEFAULTS),
		FlatOneDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme", _FLATLAF_DEFAULTS),
		FlatSolarizedDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme", _FLATLAF_DEFAULTS),
		FlatSolarizedLightIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", _FLATLAF_DEFAULTS),
		FlatSpacegrayIJTheme("com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme", _FLATLAF_DEFAULTS),
		FlatVuesionIJTheme("com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme", _FLATLAF_DEFAULTS),
		FlatXcodeDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme", _FLATLAF_DEFAULTS),

		// Built-ins
		MetalLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel", null),
		NimbusLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel", null),
		GTKLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel", null),
		WindowsLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel", null),
		WindowsClassicLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel", null),
		MotifLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel", null);

		public final String              clazz;
		public final Map<Object, Object> params;
		public final String              name;
		public final boolean             dark;

		Theme(String clazz, Map<Object, Object> params) {
			String name = null;
			boolean dark = false;
			try {
				Class<?> cl = Class.forName(clazz);
				LookAndFeel laf = (LookAndFeel)cl.getConstructor().newInstance();
				name = laf.getName();
				dark = (boolean)cl.getMethod("isDark").invoke(laf);
			} catch (Throwable ignored) {}
			this.name = name;
			this.dark = dark;
			if (name == null) {
				this.clazz = null;
				this.params = null;
			} else {
				this.clazz = clazz;
				this.params = params;
			}
		}
	}

}
