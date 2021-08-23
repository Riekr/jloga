package org.riekr.jloga;

import org.drjekyll.fontchooser.FontDialog;
import org.riekr.jloga.help.AboutPane;
import org.riekr.jloga.help.MainDesktopHelp;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.riekr.jloga.io.Preferences.FONT;
import static org.riekr.jloga.io.Preferences.LAST_OPEN_PATH;
import static org.riekr.jloga.ui.UIUtils.newButton;
import static org.riekr.jloga.ui.UIUtils.newTabHeader;

public class Main extends JFrame {

	private final CharsetCombo _charsetCombo;
	private final JTabbedPane _tabs;
	private final JobProgressBar _progressBar;
	private final Map<Object, TextSource> _openFiles = new LinkedHashMap<>();

	private Font _font;
	private MainDesktopHelp _help;

	public Main() {
		_font = Preferences.load(FONT, () -> new Font("monospaced", Font.PLAIN, 12));
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		_tabs = new JTabbedPane();
		_progressBar = new JobProgressBar();
		JToolBar toolBar = new JToolBar();
		_charsetCombo = new CharsetCombo();
		_charsetCombo.setMaximumSize(_charsetCombo.getPreferredSize());
		_charsetCombo.setToolTipText("Select next file charset");

		toolBar.add(newButton("\uD83D\uDCC1", this::openFileDialog, "Open file in new tab"));
		toolBar.addSeparator();
		toolBar.add(newButton("\uD83D\uDDDA", this::selectFont, "Select text font"));
		toolBar.add(newButton("\u21C5", this::selectPagingSize, "Select page scroll size"));
		toolBar.addSeparator();
		toolBar.add(newButton("\u292D", this::openMixDialog, "Pick'n'mix opened log files"));
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(_charsetCombo);
		toolBar.add(newButton("\uD83D\uDEC8", () -> new AboutPane().createDialog("About").setVisible(true)));

		// layout
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(_progressBar, BorderLayout.SOUTH);

		add(_help = new MainDesktopHelp(toolBar));
	}

	private void openMixDialog() {
		String title = "Pick'n'mix (EXPERIMENTAL)"; // TODO
		if (_openFiles.size() < 2) {
			JOptionPane.showMessageDialog(this, "Please open more than 1 log file first", title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		HashMap<File, PickNMixDialogEntry> files = new HashMap<>();
		JOptionPane optionPane = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = optionPane.createDialog(this, title);
		optionPane.setMessage(_openFiles.keySet().stream()
				.filter((f) -> f instanceof File).map(f -> (File) f)
				.map((f) -> new PickNMixDialogEntry(f, (selected, entry) -> {
					if (selected)
						files.put(f, entry);
					else
						files.remove(f);
					EventQueue.invokeLater(dialog::pack);
				}))
				.toArray(JComponent[]::new));
		EventQueue.invokeLater(dialog::pack);
		dialog.setMinimumSize(new Dimension(480, 0));
//		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();
		if (optionPane.getValue() == (Integer) JOptionPane.OK_OPTION) {
			if (files.size() < 2) {
				JOptionPane.showMessageDialog(this, "Please select more than 1 log file", title, JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			Set<File> fileSet = files.keySet();
			String tabTitle = fileSet.stream()
					.map(File::getName)
					.collect(Collectors.joining("+"));
			AtomicInteger idx = new AtomicInteger();
			String tabDescr = fileSet.stream()
					.map((f) -> idx.getAndIncrement() + " = " + f.getAbsolutePath())
					.collect(Collectors.joining("<br>"));
			open(fileSet, tabTitle, "<html>" + tabDescr + "</html>", () -> {
				Map<TextSource, MixFileSource.Config> sources = new LinkedHashMap<>();
				for (File f : fileSet)
					sources.put(_openFiles.get(f), files.get(f).getConfig());
				return new MixFileSource(sources);
			});
		}

	}

	private void selectFont() {
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
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setCurrentDirectory(Preferences.loadFile(LAST_OPEN_PATH, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to open");
		int userSelection = fileChooser.showOpenDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File lastFile = null;
			for (File selectedFile : fileChooser.getSelectedFiles()) {
				lastFile = selectedFile;
				openFile(selectedFile);
			}
			if (lastFile != null)
				Preferences.save(LAST_OPEN_PATH, lastFile.getParentFile());
		}
	}

	public void openFile(File file) {
		if (file.canRead())
			open(file, file.getName(), file.getAbsolutePath(), () -> new TextFileSource(file.toPath(), _charsetCombo.charset));
	}

	public void open(Object key, String title, String description, Supplier<TextSource> src) {
		if (_openFiles.containsKey(key)) {
			System.out.println("Already open: " + key);
			return;
		}
		TextSource textSource = src.get();
		_openFiles.put(key, textSource);
		try {
			System.out.println("Opening: " + description);
			if (_help != null) {
				remove(_help);
				_help = null;
				add(_tabs);
			}
			SearchPanel searchPanel = new SearchPanel(title, description, textSource, _progressBar);
			_tabs.addTab(searchPanel.toString(), searchPanel);
			searchPanel.setFont(_font);
			searchPanel.addHierarchyListener(e -> {
				if (e.getID() == HierarchyEvent.PARENT_CHANGED && searchPanel.getParent() == null)
					_openFiles.remove(key);
			});
			int idx = _tabs.getTabCount() - 1;
			_tabs.setSelectedIndex(idx);
			_tabs.setTabComponentAt(idx, newTabHeader(searchPanel.toString(), () -> {
				searchPanel.onClose();
				_tabs.remove(searchPanel);
				_openFiles.remove(key);
			}, () -> _tabs.setSelectedIndex(_tabs.indexOfComponent(searchPanel))));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			_openFiles.remove(key);
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
			if (args != null) {
				Stream.of(args)
						.map(File::new)
						.filter(File::canRead)
						.forEach(main::openFile);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

}
