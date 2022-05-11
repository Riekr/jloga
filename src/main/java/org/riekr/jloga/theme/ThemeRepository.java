package org.riekr.jloga.theme;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

class ThemeRepository {

	private static Map<String, Theme> _AVAILABLE;

	@SuppressWarnings("SpellCheckingInspection")
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
				for (int i = 0, len = Array.getLength(arr); i < len; i++)
					tryAdd((LookAndFeelInfo)Array.get(arr, i));
			} catch (ClassNotFoundException ignored) {
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}

			// Built-ins
			Map<String, LookAndFeelInfo> builtIns = Arrays.stream(UIManager.getInstalledLookAndFeels())
					.collect(toMap(LookAndFeelInfo::getClassName, identity(), (a, b) -> b, LinkedHashMap::new));
			LookAndFeelInfo sys = builtIns.remove(UIManager.getSystemLookAndFeelClassName());
			if (sys != null)
				tryAdd(sys);
			builtIns.values().forEach(ThemeRepository::tryAdd);
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

	public synchronized static void tryAdd(LookAndFeelInfo lookAndFeelInfo) {
		try {
			Theme theme = new Theme(lookAndFeelInfo);
			if (theme.name != null && !theme.name.isBlank()) {
				if (_AVAILABLE == null)
					_AVAILABLE = new LinkedHashMap<>();
				_AVAILABLE.putIfAbsent(theme.name, theme);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	private ThemeRepository() {}

}
