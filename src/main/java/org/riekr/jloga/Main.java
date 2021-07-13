package org.riekr.jloga;

import org.drjekyll.fontchooser.FontDialog;
import org.riekr.jloga.help.AboutPane;
import org.riekr.jloga.help.MainDesktopHelp;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.ui.CharsetCombo;
import org.riekr.jloga.ui.SearchPanel;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.*;

import static org.riekr.jloga.io.Preferences.FONT;
import static org.riekr.jloga.io.Preferences.LAST_OPEN_PATH;
import static org.riekr.jloga.ui.UIUtils.newButton;
import static org.riekr.jloga.ui.UIUtils.newTabHeader;

public class Main extends JFrame {

	private final CharsetCombo _charsetCombo;
	private final JTabbedPane _tabs;
	private final JProgressBar _progressBar;
	private final Set<File> _openFiles = new HashSet<>();

	private Font _font;
	private MainDesktopHelp _help;

	public Main() {
		_font = Preferences.load(FONT, () -> new Font("monospaced", Font.PLAIN, 12));
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		_tabs = new JTabbedPane();
		_progressBar = new JProgressBar();
		_progressBar.setMinimum(0);
		_progressBar.setMaximum(0);
		_progressBar.setStringPainted(true);
		_progressBar.setVisible(false);
		JToolBar toolBar = new JToolBar();
		_charsetCombo = new CharsetCombo();
		_charsetCombo.setMaximumSize(_charsetCombo.getPreferredSize());
		_charsetCombo.setToolTipText("Select next file charset");

		toolBar.add(newButton("\uD83D\uDCC1", this::openFileDialog, "Open file in new tab"));
		toolBar.addSeparator();
		toolBar.add(newButton("\uD83D\uDDDA", this::selectFont, "Select text font"));
		toolBar.add(newButton("\u21C5", this::selectPagingSize, "Select page scroll size"));
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(_charsetCombo);
		toolBar.add(newButton("\uD83D\uDEC8", () -> new AboutPane().createDialog("About").setVisible(true)));

		// layout
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(_progressBar, BorderLayout.SOUTH);

		add(_help = new MainDesktopHelp(toolBar));
	}

	public void selectFont() {
		FontDialog dialog = new FontDialog(this, "Select Font", true);
		dialog.setSelectedFont(_font);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		if (!dialog.isCancelSelected()) {
			Font selectedFont = dialog.getSelectedFont();
			System.out.println("Selected font is: " + selectedFont);
			if (!selectedFont.equals(_font)) {
				_font = selectedFont;
				for (int i = 0; i < _tabs.getTabCount(); i++) {
					SearchPanel analyzer = (SearchPanel) _tabs.getComponentAt(i);
					analyzer.setFont(selectedFont);
				}
				Preferences.save(FONT, selectedFont);
			}
		}
	}

	private void selectPagingSize() {
		String[] options = {"Full page", "\u00BD page", "\u2153 of page", "\u00BC of page", "\u2155 of page"};
		Object x = JOptionPane.showInputDialog(this,
				"<html>Select how many of the visible lines<br>" +
						"should be scrolled when paging text:<br>&nbsp;</html>",
				"Page scrolling size",
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[Math.min(options.length, Preferences.getPageDivider() - 1)]
		);
		if (x != null) {
			int idx = Arrays.binarySearch(options, x);
			if (idx != -1)
				Preferences.setPageDivider(idx + 1);
		}
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
				if (_help != null) {
					remove(_help);
					_help = null;
					add(_tabs);
				}
				System.out.println("Load file: " + file.getAbsolutePath());
				SearchPanel searchPanel = new SearchPanel(file, _charsetCombo.charset, _progressBar);
				_tabs.addTab(searchPanel.toString(), searchPanel);
				searchPanel.setFont(_font);
				searchPanel.addHierarchyListener(e -> {
					if (e.getID() == HierarchyEvent.PARENT_CHANGED && searchPanel.getParent() == null)
						_openFiles.remove(file);
				});
				int idx = _tabs.getTabCount() - 1;
				_tabs.setSelectedIndex(idx);
				_tabs.setTabComponentAt(idx, newTabHeader(searchPanel.toString(), () -> {
					searchPanel.onClose();
					_tabs.remove(searchPanel);
					_openFiles.remove(file);
				}, () -> _tabs.setSelectedIndex(_tabs.indexOfComponent(searchPanel))));
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
