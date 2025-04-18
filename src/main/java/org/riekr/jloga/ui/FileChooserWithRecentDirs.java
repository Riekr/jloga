package org.riekr.jloga.ui;

import static org.riekr.jloga.utils.FileUtils.getDisplayName;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.Serial;

import org.riekr.jloga.prefs.LimitedList;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.utils.SpringLayoutUtils;

public class FileChooserWithRecentDirs extends JFileChooser {
	@Serial private static final long serialVersionUID = -4426233513317201106L;

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);
		LimitedList<File> recentDirs = Preferences.RECENT_DIRS.get();
		if (recentDirs != null && !recentDirs.isEmpty()) {
			JPanel vbox = new JPanel(new SpringLayout());
			vbox.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(5, 12, 10, 0),
					BorderFactory.createTitledBorder("Last folders")
			));
			for (File f : recentDirs) {
				JButton btn = newBorderlessButton(
						getDisplayName(f.toPath()),
						() -> setCurrentDirectory(f),
						f.getAbsolutePath()
				);
				btn.setHorizontalAlignment(SwingConstants.LEADING);
				ContextMenu.addAction(btn, "Remove", () -> {
					recentDirs.remove(f);
					Preferences.RECENT_DIRS.set(recentDirs);
					if (recentDirs.isEmpty())
						dialog.remove(vbox);
					else
						vbox.remove(btn);
					dialog.validate();
				});
				vbox.add(btn);
			}
			SpringLayoutUtils.makeCompactGrid(vbox, recentDirs.size(), 1, 0, 0, 0, 0);
			ContextMenu.addAction(vbox, "Clear", () -> {
				recentDirs.clear();
				Preferences.RECENT_DIRS.set(recentDirs);
				dialog.remove(vbox);
				dialog.validate();
			});
			vbox.add(Box.createVerticalGlue());
			dialog.add(vbox, BorderLayout.WEST);
			dialog.pack();
			dialog.setLocationRelativeTo(parent);
		}
		return dialog;
	}

}
