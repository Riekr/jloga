package org.riekr.jloga.utils;

import static java.util.Collections.emptyList;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileUtils {

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

	public static List<File> selectFilesDialog(Component parent, File initialDir) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setCurrentDirectory(initialDir == null || !initialDir.isDirectory() ? new File(".") : initialDir);
		chooser.setDialogTitle("Open files");
		int userSelection = chooser.showOpenDialog(parent);
		if (userSelection == JFileChooser.APPROVE_OPTION)
			return Arrays.asList(chooser.getSelectedFiles());
		return emptyList();
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
