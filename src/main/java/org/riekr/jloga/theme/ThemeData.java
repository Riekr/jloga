package org.riekr.jloga.theme;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static java.util.Collections.singletonMap;

class ThemeData {

	static final Map<Object, Object> _FLATLAF_DEFAULTS = singletonMap("ScrollBar.minimumThumbSize", new Dimension(8, 20));

	private static Map<Object, Object> _APPLIED_DEFAULTS;

	static boolean apply(Theme theme) {
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

}
