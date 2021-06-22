package org.riekr.jloga.io;

import org.riekr.jloga.Main;

import javax.swing.*;
import java.io.*;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;

public class Preferences {

	public static final String LAST_OPEN_PATH = "LastOpen";
	public static final String LAST_SAVE_PATH = "LastSave";
	public static final String CHARSET = "CharsetCombo";

	private static final java.util.prefs.Preferences _PREFS = java.util.prefs.Preferences.userNodeForPackage(Main.class);

	public static void save(String key, DefaultComboBoxModel<?> o) {
		Object[] data = new Object[o.getSize()];
		for (int i = 0; i < data.length; i++)
			data[i] = o.getElementAt(i);
		save(key, data);
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

	public static <T> DefaultComboBoxModel<T> loadDefaultComboBoxModel(String key) {
		Object[] data = load(key, null);
		DefaultComboBoxModel<T> res = new DefaultComboBoxModel<>();
		if (data != null) {
			for (Object o : data)
				//noinspection unchecked
				res.addElement((T) o);
		}
		return res;
	}

	public static File loadFile(String key, Supplier<File> deflt) {
		String path = load(key, null);
		if (path == null)
			return deflt == null ? null : deflt.get();
		return new File(path);
	}

	public static <T> T load(String key, Supplier<T> deflt) {
		byte[] buf = _PREFS.getByteArray(key, null);
		if (buf == null)
			return deflt == null ? null : deflt.get();
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		try (ObjectInputStream ois = new ObjectInputStream(bais)) {
			//noinspection unchecked
			return (T) ois.readObject();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return deflt == null ? null : deflt.get();
		}
	}
}
