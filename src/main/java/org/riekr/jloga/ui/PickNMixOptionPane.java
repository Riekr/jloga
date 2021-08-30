package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PickNMixOptionPane {

	private static final String _TITLE = "Pick'n'mix (EXPERIMENTAL)"; // TODO

	@Nullable
	public static MixFileSource.Config show(@NotNull Map<File, TextSource> inputFiles, @Nullable Component parentComponent) {
		if (inputFiles.size() < 2) {
			JOptionPane.showMessageDialog(parentComponent, "Please open more than 1 log file first", _TITLE, JOptionPane.INFORMATION_MESSAGE);
			return null;
		}

		HashMap<File, PickNMixDialogEntry> selectedFiles = new HashMap<>();
		JOptionPane optionPane = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = optionPane.createDialog(parentComponent, _TITLE);
		optionPane.setMessage(inputFiles.keySet().stream().map((f) -> new PickNMixDialogEntry(f, (selected, entry) -> {
					if (selected)
						selectedFiles.put(f, entry);
					else
						selectedFiles.remove(f);
					EventQueue.invokeLater(dialog::pack);
				}))
				.toArray(JComponent[]::new));
		EventQueue.invokeLater(dialog::pack);
		dialog.setMinimumSize(new Dimension(480, 0));
//		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();

		if (optionPane.getValue() == (Integer) JOptionPane.OK_OPTION) {
			if (selectedFiles.size() >= 2) {
				Map<TextSource, MixFileSource.SourceConfig> res = new HashMap<>();
				selectedFiles.forEach((k, v) -> res.put(inputFiles.get(k), v.getConfig(k)));
				return new MixFileSource.Config(res, null, null);
			}
			JOptionPane.showMessageDialog(parentComponent, "Please select more than 1 log file", _TITLE, JOptionPane.INFORMATION_MESSAGE);
		}

		return null;
	}

}
