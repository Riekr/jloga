package org.riekr.jloga.project;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.riekr.jloga.prefs.PrefsUtils;

class PropsIO {

	private static final String PATH_PREFS_PREFIX = "PropsIO.";

	private static class GetterAndSetter {
		private final Method getter, setter;

		private GetterAndSetter(Method getter, Method setter) {
			this.getter = getter;
			this.setter = setter;
		}
	}

	private static Map<String, GetterAndSetter> map(Object obj) {
		TreeMap<String, GetterAndSetter> res = new TreeMap<>();
		Class<?> cl = obj.getClass();
		for (Method getter : cl.getMethods()) {
			if (getter.getName().startsWith("get")
					&& getter.getParameterCount() == 0
					&& getter.getReturnType() == String.class) {
				try {
					String key = getter.getName().substring(3);
					Method setter = cl.getMethod("set" + key, String.class);
					if (setter.getReturnType() == Void.TYPE)
						res.put(key, new GetterAndSetter(getter, setter));
				} catch (NoSuchMethodException ignored) {
				}
			}
		}
		return res;
	}

	public static void save(File dest, Object pojo) throws InvocationTargetException, IllegalAccessException, IOException {
		if (pojo instanceof Project) {
			save(dest, (Project)pojo);
			return;
		}
		Properties props = new Properties();
		for (Map.Entry<String, GetterAndSetter> entry : map(pojo).entrySet()) {
			String val = (String)entry.getValue().getter.invoke(pojo);
			if (val != null)
				props.setProperty(entry.getKey(), val);
		}
		try (Writer writer = new FileWriter(dest, false)) {
			props.store(writer, pojo.getClass().getSimpleName());
		}
	}

	public static void load(File src, Object dest) throws IOException, InvocationTargetException, IllegalAccessException {
		if (dest instanceof Project) {
			load(src, (Project)dest);
			return;
		}
		Properties props = new Properties();
		try (Reader reader = new FileReader(src)) {
			props.load(reader);
		}
		for (Map.Entry<String, GetterAndSetter> entry : map(dest).entrySet())
			entry.getValue().setter.invoke(dest, props.getProperty(entry.getKey()));

	}

	public static void save(File dest, Project project) throws IOException {
		Properties props = new Properties();
		project.fields()
				.filter(ProjectField::hasValue)
				.forEach((f) -> props.setProperty(f.key, f.toString()));
		try (Writer writer = new FileWriter(dest, false)) {
			props.store(writer, project.getClass().getSimpleName());
		}
	}

	public static void load(File src, Project project) throws IOException {
		Properties props = new Properties();
		try (Reader reader = new FileReader(src)) {
			props.load(reader);
		}
		project.fields()
				.forEach((f) -> f.accept(props.getProperty(f.key)));
	}

	public static void requestSave(Component owner, Object pojo, String ext, String extDescription, Runnable... onSuccess) {
		JFileChooser fileChooser = new JFileChooser();
		if (ext != null && !ext.isBlank())
			fileChooser.setFileFilter(new FileNameExtensionFilter(extDescription, ext));
		fileChooser.setCurrentDirectory(PrefsUtils.loadFile(PATH_PREFS_PREFIX + ext, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to open");
		int userSelection = fileChooser.showSaveDialog(owner);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				save(selectedFile, pojo);
			} catch (Throwable e) {
				JOptionPane.showMessageDialog(owner, e.getLocalizedMessage(), "Error saving file", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace(System.err);
				return;
			}
			PrefsUtils.save(PATH_PREFS_PREFIX + ext, selectedFile.getParentFile());
			if (onSuccess != null)
				for (Runnable task : onSuccess)
					task.run();
		}
	}

	public static void requestLoad(Component owner, Object pojo, String ext, String extDescription, Runnable... onSuccess) {
		JFileChooser fileChooser = new JFileChooser();
		if (ext != null && !ext.isBlank())
			fileChooser.setFileFilter(new FileNameExtensionFilter(extDescription, ext));
		fileChooser.setCurrentDirectory(PrefsUtils.loadFile(PATH_PREFS_PREFIX + ext, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to open");
		int userSelection = fileChooser.showOpenDialog(owner);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				load(selectedFile, pojo);
			} catch (Throwable e) {
				JOptionPane.showMessageDialog(owner, e.getLocalizedMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace(System.err);
				return;
			}
			PrefsUtils.save(PATH_PREFS_PREFIX + ext, selectedFile.getParentFile());
			if (onSuccess != null)
				for (Runnable task : onSuccess)
					task.run();
		}
	}

}
