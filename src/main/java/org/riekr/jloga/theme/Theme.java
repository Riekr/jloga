package org.riekr.jloga.theme;

import javax.swing.*;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public enum Theme {

	// https://www.formdev.com/flatlaf/themes/
	// FlatDarkLaf("com.formdev.flatlaf.FlatDarkLaf", _FLATLAF_DEFAULTS, "FlatLaf Dark"),
	FlatDarculaLaf("com.formdev.flatlaf.FlatDarculaLaf", ThemeData._FLATLAF_DEFAULTS),
	// FlatLightLaf("com.formdev.flatlaf.FlatLightLaf", _FLATLAF_DEFAULTS, "FlatLaf Light"),
	FlatIntelliJLaf("com.formdev.flatlaf.FlatIntelliJLaf", ThemeData._FLATLAF_DEFAULTS),

	// Extras
	FlatArcDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatArcDarkOrangeIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatArcIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatArcOrangeIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatCarbonIJTheme("com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatCobalt2IJTheme("com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatCyanLightIJTheme("com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatDarkFlatIJTheme("com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatDarkPurpleIJTheme("com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatDraculaIJTheme("com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGradiantoDarkFuchsiaIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGradiantoDeepOceanIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGradiantoMidnightBlueIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGradiantoNatureGreenIJTheme("com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGrayIJTheme("com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGruvboxDarkHardIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGruvboxDarkMediumIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatGruvboxDarkSoftIJTheme("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatHiberbeeDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatHighContrastIJTheme("com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatLightFlatIJTheme("com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatMaterialDesignDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatMonocaiIJTheme("com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatMonokaiProIJTheme("com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatNordIJTheme("com.formdev.flatlaf.intellijthemes.FlatNordIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatOneDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatSolarizedDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatSolarizedLightIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatSpacegrayIJTheme("com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatVuesionIJTheme("com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme", ThemeData._FLATLAF_DEFAULTS),
	FlatXcodeDarkIJTheme("com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme", ThemeData._FLATLAF_DEFAULTS),

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

	public boolean apply() {
		return ThemeData.apply(this);
	}
}
