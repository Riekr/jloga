package org.riekr.jloga.theme;

import static java.util.Collections.singletonMap;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import com.formdev.flatlaf.FlatLaf;

class ThemeData {

	static final Map<Object, Object> _FLATLAF_DEFAULTS = singletonMap("ScrollBar.minimumThumbSize", new Dimension(8, 20));

	private static Map<Object, Object> _APPLIED_DEFAULTS;

	public final String              clazz;
	public final Map<Object, Object> params;
	public final String              name;
	public final boolean             dark;

	ThemeData(String clazz, Map<Object, Object> params) {
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

	boolean apply() {
		UIDefaults defaults = UIManager.getDefaults();
		if (_APPLIED_DEFAULTS != null) {
			_APPLIED_DEFAULTS.keySet().forEach(defaults::remove);
			_APPLIED_DEFAULTS = null;
		}
		try {
			boolean applied = false;
			try {
				Class<?> cl = Class.forName(this.clazz);
				if (FlatLaf.class.isAssignableFrom(cl)) {
					cl.getMethod("setup").invoke(null);
					applied = true;
				}
			} catch (Throwable ignored) {}
			if (!applied)
				UIManager.setLookAndFeel(this.clazz);
			if (this.params != null)
				defaults.putAll(this.params);
			_APPLIED_DEFAULTS = this.params;
			return true;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

}
