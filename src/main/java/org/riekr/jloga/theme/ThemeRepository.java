package org.riekr.jloga.theme;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes.FlatIJLookAndFeelInfo;

class ThemeRepository {

	private static Map<String, Theme> _AVAILABLE;

	public static Map<String, Theme> availableThemes() {
		if (_AVAILABLE == null) {
			// https://www.formdev.com/flatlaf/themes/
			tryAdd("com.formdev.flatlaf.FlatDarculaLaf");
			tryAdd("com.formdev.flatlaf.FlatIntelliJLaf");

			// extras
			for (FlatIJLookAndFeelInfo e : FlatAllIJThemes.INFOS)
				tryAdd(e.getClassName());

			// Built-ins
			tryAdd("javax.swing.plaf.metal.MetalLookAndFeel");
			tryAdd("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			tryAdd("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			tryAdd("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			tryAdd("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
			tryAdd("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
			LookAndFeel[] aux = UIManager.getAuxiliaryLookAndFeels();
			if (aux != null) {
				for (LookAndFeel laf : aux)
					tryAdd(laf.getClass().getName());
			}

		}
		return _AVAILABLE;
	}

	public synchronized static void tryAdd(String className) {
		try {
			Theme theme = new Theme(className);
			if (theme.name != null && !theme.name.isBlank()) {
				if (_AVAILABLE == null)
					_AVAILABLE = new LinkedHashMap<>();
				_AVAILABLE.putIfAbsent(theme.name, theme);
			}
		} catch (ClassNotFoundException ignored) {
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	private ThemeRepository() {}

}
