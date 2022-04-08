package org.riekr.jloga.utils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.riekr.jloga.Main;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.ui.FileChooserWithRecentDirs;

public class FileUtils {

	public enum DialogType {
		OPEN_MULTI(true, "Open files", (jfc) -> jfc.showOpenDialog(Main.getMain())),
		OPEN(false, "Open file", (jfc) -> jfc.showOpenDialog(Main.getMain())),
		SAVE(false, "Save file", (jfc) -> jfc.showSaveDialog(Main.getMain()));

		final boolean                     multi;
		final String                      defaultTitle;
		final ToIntFunction<JFileChooser> action;

		DialogType(boolean multi, String defaultTitle, ToIntFunction<JFileChooser> action) {
			this.multi = multi;
			this.defaultTitle = defaultTitle;
			this.action = action;
		}
	}

	public static String getDisplayName(File f) {
		return getDisplayName(f.toPath());
	}

	public static String getDisplayName(Path p) {
		int count = p.getNameCount();
		if (count <= 3)
			return p.toString();
		return "..." + File.separatorChar + p.getName(count - 2) + File.separatorChar + p.getName(count - 1);
	}

	public static FileFilter directoryFileFilter(boolean hidden) {
		return new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() && (hidden || !(f.isHidden() || f.getName().startsWith(".")));
			}

			@Override
			public String getDescription() {return hidden ? "All directories" : "Directories";}
		};
	}

	public static FileFilter executableFilter() {
		String description = "Executables";
		if (OSUtils.isWindows())
			return new FileNameExtensionFilter(description, "exe", "com", "cmd", "bat");
		return new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.canExecute();
			}

			@Override
			public String getDescription() {return description;}
		};
	}

	public static File selectDirectoryDialog(Component parent, File initialDir) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(initialDir == null || !initialDir.isDirectory() ? new File(".") : initialDir);
		chooser.setDialogTitle("Select directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(directoryFileFilter(false));
		chooser.addChoosableFileFilter(directoryFileFilter(true));
		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File res = chooser.getSelectedFile();
			return res == null || res.isDirectory() ? res : res.getParentFile();
		}
		return null;
	}

	public static Stream<File> fileDialog(DialogType type, File initialDir) {
		return fileDialog(type, initialDir, type.defaultTitle);
	}

	public static Stream<File> fileDialog(DialogType type, File initialDir, String title) {
		return fileDialog(type, initialDir, title, null, null);
	}

	public static Stream<File> fileDialog(DialogType type, File initialDir, String title, String ext, String extDescription) {
		if (initialDir == null || !initialDir.isDirectory())
			initialDir = Preferences.RECENT_DIRS.get().stream().findFirst().orElseGet(() -> new File("."));
		JFileChooser chooser = new FileChooserWithRecentDirs();
		if (ext != null && !ext.isBlank())
			chooser.setFileFilter(new FileNameExtensionFilter(extDescription, ext));
		chooser.setMultiSelectionEnabled(type.multi);
		chooser.setCurrentDirectory(initialDir);
		chooser.setDialogTitle(title);
		int userSelection = type.action.applyAsInt(chooser);
		if (userSelection == JFileChooser.APPROVE_OPTION)
			return type.multi ? Arrays.stream(chooser.getSelectedFiles()) : Stream.of(chooser.getSelectedFile());
		return Stream.empty();
	}

	public static File selectExecutableDialog(Component parent, File initialFile) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setAcceptAllFileFilterUsed(true);
		FileFilter filter = executableFilter();
		chooser.addChoosableFileFilter(filter);
		chooser.setFileFilter(filter);
		File initialDir;
		if (initialFile == null)
			initialDir = null;
		else if (initialFile.isDirectory())
			initialDir = initialFile;
		else {
			initialDir = initialFile.getParentFile();
			chooser.setSelectedFile(initialFile);
		}
		chooser.setCurrentDirectory(initialDir == null || !initialDir.isDirectory() ? new File(".") : initialDir);
		chooser.setDialogTitle("Select executable");
		int userSelection = chooser.showOpenDialog(parent);
		if (userSelection == JFileChooser.APPROVE_OPTION)
			return chooser.getSelectedFile();
		return null;
	}

	private FileUtils() {}
}
