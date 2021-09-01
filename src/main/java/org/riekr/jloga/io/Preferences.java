package org.riekr.jloga.io;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;

import org.riekr.jloga.Main;

public class Preferences {

	public static final String LAST_OPEN_PATH = "LastOpen";
	public static final String LAST_SAVE_PATH = "LastSave";
	public static final String CHARSET        = "CharsetCombo";
	public static final String FONT           = "Font";
	public static final String SEARCH_TYPE    = "SearchType";
	public static final String PAGE_DIVIDER   = "PageDivider";

	private static final java.util.prefs.Preferences _PREFS = java.util.prefs.Preferences.userNodeForPackage(Main.class);

	static {
		_PREFS.addPreferenceChangeListener(evt -> {
			if (PAGE_DIVIDER.equals(evt.getKey())) {
				_PAGE_DIVIDER = null;
			}
		});
	}

	private static Integer _PAGE_DIVIDER;

	public static void save(String key, DefaultComboBoxModel<?> o) {
		Object[] data = new Object[o.getSize()];
		for (int i = 0; i < Math.min(20, data.length); i++)
			data[i] = o.getElementAt(i);
		save(key, data);
	}

	public static void save(String key, Class<?> o) {
		save(key, o.getCanonicalName());
	}

	public static void save(String key, File o) {
		save(key, o.getAbsolutePath());
	}

	public static void save(String key, Serializable o) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream dos = new ObjectOutputStream(baos)) {
			dos.writeObject(o);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return;
		}
		_PREFS.putByteArray(key, baos.toByteArray());
		try {
			_PREFS.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> DefaultComboBoxModel<T> loadDefaultComboBoxModel(String key) {
		Object[] data = load(key, null);
		DefaultComboBoxModel<T> res = new DefaultComboBoxModel<>();
		if (data != null) {
			for (Object o : data)
				res.addElement((T)o);
		}
		return res;
	}

	public static File loadFile(String key, Supplier<File> deflt) {
		String path = load(key, null);
		if (path == null)
			return deflt == null ? null : deflt.get();
		return new File(path);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> loadClass(String key, Supplier<Class<T>> deflt) {
		try {
			String name = load(key, null);
			if (name != null) {
				return (Class<T>)Class.forName(name);
			}
		} catch (ClassNotFoundException | ClassCastException ignored) {
		}
		return deflt == null ? null : deflt.get();
	}

	@SuppressWarnings("unchecked")
	public static <T> T load(String key, Supplier<T> deflt) {
		if (!Boolean.getBoolean("jloga.prefs.ignore")) {
			byte[] buf = _PREFS.getByteArray(key, null);
			if (buf != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(buf);
				try (ObjectInputStream ois = new ObjectInputStream(bais)) {
					return (T)ois.readObject();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return deflt == null ? null : deflt.get();
	}

	@SuppressWarnings("ConstantConditions")
	public static int getPageDivider() {
		if (_PAGE_DIVIDER == null)
			_PAGE_DIVIDER = Math.max(1, load(PAGE_DIVIDER, () -> 3));
		return _PAGE_DIVIDER;
	}

	public static void setPageDivider(int divider) {
		_PAGE_DIVIDER = Math.max(1, divider);
		save(PAGE_DIVIDER, _PAGE_DIVIDER);
	}
}
