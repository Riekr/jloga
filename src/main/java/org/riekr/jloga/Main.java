package org.riekr.jloga;

import static org.riekr.jloga.io.Preferences.FONT;
import static org.riekr.jloga.io.Preferences.LAST_OPEN_PATH;
import static org.riekr.jloga.ui.utils.UIUtils.newButton;
import static org.riekr.jloga.ui.utils.UIUtils.newRadioButton;
import static org.riekr.jloga.ui.utils.UIUtils.newTabHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.drjekyll.fontchooser.FontDialog;
import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.help.AboutPane;
import org.riekr.jloga.help.MainDesktopHelp;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.ui.CharsetCombo;
import org.riekr.jloga.ui.JobProgressBar;
import org.riekr.jloga.ui.PickNMixOptionPane;
import org.riekr.jloga.ui.SearchPanel;
import org.riekr.jloga.ui.TabNavigation;
import org.riekr.jloga.ui.utils.UIUtils;

public class Main extends JFrame implements FileDropListener {

	private final CharsetCombo            _charsetCombo;
	private final JTabbedPane             _tabs;
	private final JobProgressBar          _progressBar;
	private final Map<Object, TextSource> _openFiles = new LinkedHashMap<>();

	private Font            _font;
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
		toolBar.add(newButton("\uD83D\uDD0B", this::openSystemConfigDialog, "System configuration"));
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

		setFileDropListener(this::openFiles);
	}

	private void openMixDialog() {
		Map<File, TextSource> inputFiles = new HashMap<>();
		_openFiles.forEach((k, v) -> {
			if (k instanceof File)
				inputFiles.put((File)k, v);
		});
		MixFileSource.Config config = PickNMixOptionPane.show(inputFiles, this);
		if (config != null) {
			String tabTitle = config.sources.values().stream()
					.map((sc) -> sc.file.getName())
					.collect(Collectors.joining("+"));
			AtomicInteger idx = new AtomicInteger();
			String tabDescr = config.sources.values().stream()
					.map((sc) -> idx.getAndIncrement() + " = " + sc.file.getAbsolutePath())
					.collect(Collectors.joining("<br>"));
			open(config, tabTitle, "<html>" + tabDescr + "</html>", () -> new MixFileSource(config));
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
					SearchPanel analyzer = (SearchPanel)_tabs.getComponentAt(i);
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

	private void openSystemConfigDialog() {
		JOptionPane optionPane = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
		JDialog dialog = optionPane.createDialog(this, "System configuration");
		ArrayList<Object> options = new ArrayList<>();
		ButtonGroup threadingOptions = new ButtonGroup();
		for (Map.Entry<String, String> e : SimpleSearchPredicate.getModels().entrySet()) {
			options.add(newRadioButton(
					threadingOptions,
					e.getValue(),
					e.getKey(),
					() -> SimpleSearchPredicate.setModel(e.getKey()),
					e.getKey().equals(SimpleSearchPredicate.getModel())
			));
		}
		optionPane.setMessage(options.toArray());
		EventQueue.invokeLater(dialog::pack);
		dialog.setMinimumSize(new Dimension(480, 0));
		dialog.setVisible(true);
		dialog.dispose();
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

	public void openFiles(@NotNull List<File> files) {
		files.forEach(this::openFile);
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
			SearchPanel searchPanel = new SearchPanel(title, description, textSource, _progressBar, TabNavigation.createFor(_tabs));
			searchPanel.setFileDropListener(this::openFiles);
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
	private static boolean loadLAF() {
		try {
			// https://www.formdev.com/flatlaf/themes/
			UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
			return true;
		} catch (Throwable ignored) {
		}
		try {
			UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
			return true;
		} catch (Throwable ignored) {
		}
		// https://stackoverflow.com/a/65805346/1326326
		return false;
	}

	public static void main(String... args) {
		try {
			// init themes
			boolean dark = loadLAF();

			// init ui
			Main main = new Main();
			main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			main.setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
			main.setExtendedState(main.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			main.setTitle("JLogA");
			UIUtils.setIcon(main, "icon.png", dark);
			main.setVisible(true);

			// load files
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

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		UIUtils.setFileDropListener(this, this::openFiles);
		UIUtils.setFileDropListener(_help, this::openFiles);
	}
}
