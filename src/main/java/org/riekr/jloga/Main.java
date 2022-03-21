package org.riekr.jloga;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newTabHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.help.AboutPane;
import org.riekr.jloga.help.MainDesktopHelp;
import org.riekr.jloga.httpd.FinosPerspectiveServer;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.prefs.LimitedList;
import org.riekr.jloga.prefs.PrefPanel;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.ui.CharsetCombo;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.ui.JobProgressBar;
import org.riekr.jloga.ui.PickNMixOptionPane;
import org.riekr.jloga.ui.SearchPanel;
import org.riekr.jloga.ui.TabNavigation;
import org.riekr.jloga.utils.FileUtils;
import org.riekr.jloga.utils.UIUtils;

public class Main extends JFrame implements FileDropListener {
	private static final long serialVersionUID = -5418006859279219934L;

	private static Main _INSTANCE;

	@Nullable
	public static Main getMain() {return _INSTANCE;}

	private final CharsetCombo            _charsetCombo;
	private final JTabbedPane             _tabs;
	private final JobProgressBar          _progressBar;
	private final Map<Object, TextSource> _openFiles = new LinkedHashMap<>();

	private       Font            _font;
	private final MainDesktopHelp _help;

	private Main() {
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		_tabs = new JTabbedPane();
		_progressBar = new JobProgressBar();
		JToolBar toolBar = new JToolBar();
		_charsetCombo = new CharsetCombo();
		_charsetCombo.setMaximumSize(_charsetCombo.getPreferredSize());
		_charsetCombo.setToolTipText("Select next file charset");

		toolBar.add(newBorderlessButton("\uD83D\uDCC1 Open", this::openFileDialog, "Open file in new tab"));
		toolBar.addSeparator();
		toolBar.add(newBorderlessButton("\u292D Mix", this::openMixDialog, "Pick'n'mix open log files"));
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(newBorderlessButton("\u2699 Settings", this::openPreferences, "Change preferences"));
		toolBar.add(_charsetCombo);
		toolBar.add(UIUtils.newBorderlessButton("\uD83D\uDEC8 About", () -> new AboutPane().createDialog("About").setVisible(true)));

		// layout
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(_progressBar, BorderLayout.SOUTH);

		add(_help = new MainDesktopHelp(toolBar, this::openFile));

		setFileDropListener(this::openFiles);

		// preferences
		Preferences.FONT.subscribe((selectedFont) -> {
			if (!selectedFont.equals(_font)) {
				System.out.println("Selected font is: " + selectedFont);
				_font = selectedFont;
				for (int i = 0; i < _tabs.getTabCount(); i++) {
					SearchPanel analyzer = (SearchPanel)_tabs.getComponentAt(i);
					analyzer.setFont(selectedFont);
				}
			}
		});
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

	private void openPreferences() {
		new PrefPanel(this).setVisible(true);
	}

	public void openFileDialog() {
		List<File> files = FileUtils.selectFilesDialog(this, Preferences.LAST_OPEN_PATH.get());
		File lastFile = null;
		for (File selectedFile : files) {
			lastFile = selectedFile;
			openFile(selectedFile);
		}
		if (lastFile != null)
			Preferences.LAST_OPEN_PATH.set(lastFile.getParentFile());
	}

	public void openFiles(@NotNull List<File> files) {
		files.forEach(this::openFile);
	}

	public void openFile(File file) {
		if (file.canRead()) {
			LimitedList<File> files = Preferences.RECENT_FILES.get();
			files.remove(file);
			try {
				open(file, file.getName(), file.getAbsolutePath(), () -> new TextFileSource(file.toPath(), _charsetCombo.charset));
			} finally {
				files.add(0, file);
				Preferences.RECENT_FILES.set(files);
			}
		}
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
			_help.setVisible(false);
			if (_tabs.getParent() == null)
				add(_tabs);
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
			JComponent tabHeader = newTabHeader(searchPanel.toString(), () -> {
				searchPanel.onClose();
				_tabs.remove(searchPanel);
				_openFiles.remove(key);
				if (_tabs.getTabCount() == 0)
					_help.setVisible(true);
			}, () -> _tabs.setSelectedIndex(_tabs.indexOfComponent(searchPanel)));
			ContextMenu.addActionCopy(tabHeader, key);
			_tabs.setTabComponentAt(idx, tabHeader);
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
			UIManager.put("ScrollBar.minimumThumbSize", new Dimension(8, 20));
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

	public static void main(String[] vargs) {
		try {
			// check args
			ArrayList<String> args = new ArrayList<>();
			if (vargs != null)
				Collections.addAll(args, vargs);
			for (Iterator<String> i = args.iterator(); i.hasNext(); ) {
				final String arg = i.next();
				if (arg.startsWith("#")) {
					i.remove();
					continue;
				}
				if (arg.equals("-perspective")) {
					i.remove();
					FinosPerspectiveServer.main(args.toArray(EMPTY_STRINGS));
					return;
				}
			}

			// init themes
			boolean dark = loadLAF();

			// init ui
			if (_INSTANCE == null) {
				_INSTANCE = new Main();
				_INSTANCE.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				_INSTANCE.setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
				_INSTANCE.setExtendedState(_INSTANCE.getExtendedState() | JFrame.MAXIMIZED_BOTH);
				_INSTANCE.setTitle("JLogA");
				UIUtils.setIcon(_INSTANCE, "icon.png", dark);
			}
			_INSTANCE.setVisible(true);

			// load files
			args.stream().sequential()
					.map(File::new)
					.filter(File::canRead)
					.forEach(_INSTANCE::openFile);
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
