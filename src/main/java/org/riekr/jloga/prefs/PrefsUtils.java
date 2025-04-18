package org.riekr.jloga.prefs;

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
import org.riekr.jloga.utils.TaggedObject;

public class PrefsUtils {

	private static final java.util.prefs.Preferences _PREFS = java.util.prefs.Preferences.userNodeForPackage(Main.class);

	public static void save(String key, DefaultComboBoxModel<?> o) {
		Object[] data = new Object[o.getSize()];
		for (int i = 0; i < Math.min(20, data.length); i++)
			data[i] = o.getElementAt(i);
		save(key, new TaggedObject<>(o.getSelectedItem(), data));
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

	public static void save(String key, boolean bool) {
		_PREFS.putBoolean(key, bool);
		try {
			_PREFS.sync();
		} catch (BackingStoreException e) {
			e.printStackTrace(System.err);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> DefaultComboBoxModel<T> loadDefaultComboBoxModel(String key) {
		try {
			TaggedObject<T, Object[]> data = load(key, null);
			DefaultComboBoxModel<T> res = new DefaultComboBoxModel<>();
			if (data != null) {
				for (Object o : data.value())
					res.addElement((T)o);
				res.setSelectedItem(data.tag());
			}
			return res;
		} catch (ClassCastException e) {
			return loadOldDefaultComboBoxModel(key);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> DefaultComboBoxModel<T> loadOldDefaultComboBoxModel(String key) {
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
	public static <T> T load(String key, Supplier<T> deflt) {
		T res = null;
		if (!Boolean.getBoolean("jloga.prefs.ignore")) {
			byte[] buf = _PREFS.getByteArray(key, null);
			if (buf != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(buf);
				try (ObjectInputStream ois = new ObjectInputStream(bais)) {
					res = (T)ois.readObject();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		if (res != null)
			return res;
		return deflt == null ? null : deflt.get();
	}

	public static boolean load(String key) {
		return load(key, false);
	}

	public static boolean load(String key, boolean deflt) {
		if (!Boolean.getBoolean("jloga.prefs.ignore"))
			return _PREFS.getBoolean(key, deflt);
		return deflt;
	}
}
