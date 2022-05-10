package org.riekr.jloga.theme;

import static java.util.Collections.singletonMap;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

public class Theme {

	private static final Map<Object, Object> _FLATLAF_DEFAULTS = singletonMap("ScrollBar.minimumThumbSize", new Dimension(8, 20));
	private static       Map<Object, Object> _APPLIED_DEFAULTS;

	public final String                       name;
	public final String                       className;
	public final Class<? extends LookAndFeel> clazz;
	public final boolean                      dark;

	@SuppressWarnings("unchecked")
	public Theme(String className) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		this.clazz = (Class<? extends LookAndFeel>)Class.forName(className);
		LookAndFeel laf = clazz.getConstructor().newInstance();
		this.name = laf.getName();
		this.className = className;
		boolean dark;
		try {
			dark = (boolean)clazz.getMethod("isDark").invoke(laf);
		} catch (Throwable e) {
			dark = false;
		}
		this.dark = dark;
	}

	public boolean apply() {
		UIDefaults defaults = UIManager.getDefaults();
		if (_APPLIED_DEFAULTS != null) {
			_APPLIED_DEFAULTS.keySet().forEach(defaults::remove);
			_APPLIED_DEFAULTS = null;
		}
		try {
			UIManager.setLookAndFeel(this.className);
			if (className.startsWith("com.formdev.flatlaf.")) {
				defaults.putAll(_FLATLAF_DEFAULTS);
				_APPLIED_DEFAULTS = _FLATLAF_DEFAULTS;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final Theme theme = (Theme)o;
		return Objects.equals(name, theme.name);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return className;
	}
}
