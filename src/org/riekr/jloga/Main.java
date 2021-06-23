package org.riekr.jloga;

import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.ui.CharsetCombo;
import org.riekr.jloga.ui.SearchPanel;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.riekr.jloga.io.Preferences.LAST_OPEN_PATH;
import static org.riekr.jloga.ui.UIUtils.newButton;
import static org.riekr.jloga.ui.UIUtils.newTabHeader;

public class Main extends JFrame {

	private final CharsetCombo _charsetCombo;
	private final JTabbedPane _tabs;
	private final JProgressBar _progressBar;
	private final Font _font = new Font("monospaced", Font.PLAIN, 12);
	private final Set<File> _openFiles = new HashSet<>();

	public Main() {
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		_tabs = new JTabbedPane();
		_progressBar = new JProgressBar();
		_progressBar.setMinimum(0);
		_progressBar.setMaximum(0);
		_progressBar.setStringPainted(true);
		JToolBar toolBar = new JToolBar();
		_charsetCombo = new CharsetCombo();
		_charsetCombo.setMaximumSize(_charsetCombo.getPreferredSize());

		toolBar.add(newButton("\uD83D\uDCC1", this::openFileDialog));
		toolBar.add(_charsetCombo);

		// layout
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(_tabs);
		add(_progressBar, BorderLayout.SOUTH);
	}

	public void openFileDialog() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(Preferences.loadFile(LAST_OPEN_PATH, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to open");
		int userSelection = fileChooser.showOpenDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			openFile(selectedFile);
			Preferences.save(LAST_OPEN_PATH, selectedFile.getParentFile());
		}
	}

	public void openFile(File file) {
		try {
			if (file.canRead() && _openFiles.add(file)) {
				System.out.println("Load file: " + file.getAbsolutePath());
				SearchPanel analyzer = new SearchPanel(file, _charsetCombo.charset, _progressBar);
				_tabs.addTab(analyzer.toString(), analyzer);
				analyzer.setFont(_font);
				analyzer.addHierarchyListener(e -> {
					if (e.getID() == HierarchyEvent.PARENT_CHANGED && analyzer.getParent() == null)
						_openFiles.remove(file);
				});
				int idx = _tabs.getTabCount() - 1;
				_tabs.setSelectedIndex(idx);
				_tabs.setTabComponentAt(idx, newTabHeader(analyzer.toString(), () -> {
					_tabs.remove(analyzer);
					_openFiles.remove(file);
				}, () -> _tabs.setSelectedIndex(_tabs.indexOfComponent(analyzer))));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			_openFiles.remove(file);
		}
	}


	@SuppressWarnings("SpellCheckingInspection")
	private static void loadLAF() {
		try {
			// https://www.formdev.com/flatlaf/themes/
			UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
			return;
		} catch (Throwable ignored) {
		}
		try {
			UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
		} catch (Throwable ignored) {
		}
	}

	public static void main(String... args) {
		try {
			loadLAF();
			Main main = new Main();
			main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			main.setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
			main.setExtendedState(main.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			main.setTitle("JLogA");
			main.setVisible(true);
			if (args.length > 0)
				main.openFile(new File(args[0]));
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

}
