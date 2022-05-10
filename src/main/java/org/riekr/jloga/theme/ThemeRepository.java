package org.riekr.jloga.theme;

import javax.swing.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

class ThemeRepository {

	private static Map<String, Theme> _AVAILABLE;

	public static Map<String, Theme> availableThemes() {
		if (_AVAILABLE == null) {
			// https://www.formdev.com/flatlaf/themes/
			tryAdd("com.formdev.flatlaf.FlatDarculaLaf");
			tryAdd("com.formdev.flatlaf.FlatIntelliJLaf");

			// flatlaf extras
			try {
				Class<?> cl = Class.forName("com.formdev.flatlaf.intellijthemes.FlatAllIJThemes");
				Field f = cl.getField("INFOS");
				Object arr = f.get(null);
				for (int i = 0, len = Array.getLength(arr); i < len; i++) {
					UIManager.LookAndFeelInfo info = (UIManager.LookAndFeelInfo)Array.get(arr, i);
					tryAdd(info.getClassName());
				}
			} catch (ClassNotFoundException ignored) {
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}

			// Built-ins
			tryAdd(UIManager.getSystemLookAndFeelClassName());
			UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
			for (UIManager.LookAndFeelInfo laf : installed)
				tryAdd(laf.getClassName());
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
