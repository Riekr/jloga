package org.riekr.jloga.ui;

import static org.riekr.jloga.io.TextSource.closeTextSource;
import static org.riekr.jloga.utils.KeyUtils.addKeyStrokeAction;
import static org.riekr.jloga.utils.TextUtils.TAB_ADD;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newTabHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.help.AboutPane;
import org.riekr.jloga.help.MainDesktopHelp;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.prefs.Favorites;
import org.riekr.jloga.prefs.KeyBindings;
import org.riekr.jloga.prefs.PrefPanel;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.utils.FileUtils;
import org.riekr.jloga.utils.UIUtils;

public class MainPanel extends JFrame implements FileDropListener {
	private static final long serialVersionUID = -5418006859279219934L;

	private final JTabbedPane             _tabs;
	private final JobProgressBar          _progressBar;
	private final Map<Object, TextSource> _openFiles = new LinkedHashMap<>();

	private       Font            _font;
	private final MainDesktopHelp _help;
	private final JButton         _refreshBtn;

	public MainPanel() {
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		_tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		_progressBar = new JobProgressBar();
		JToolBar toolBar = new JToolBar();

		CharsetCombo charsetCombo = new CharsetCombo();
		charsetCombo.setMaximumSize(charsetCombo.getPreferredSize());
		charsetCombo.setToolTipText("Select next file charset");
		Preferences.CHARSET.subscribe(charsetCombo::setSelectedItem);

		toolBar.add(newBorderlessButton("\uD83D\uDCC1 Open", this::openFileDialog, "Open file in new tab"));
		addKeyStrokeAction(this, KeyBindings.KB_OPENFILE, this::openFileDialog);
		toolBar.addSeparator();
		toolBar.add(newBorderlessButton("\u292D Mix", this::openMixDialog, "Pick'n'mix open log files"));
		toolBar.addSeparator();
		_refreshBtn = newBorderlessButton("\uD83D\uDDD8 Refresh", this::refreshCurrentTab, "Refresh current tab");
		_refreshBtn.setEnabled(false);
		toolBar.add(_refreshBtn);
		toolBar.addSeparator();
		JButton favoritesBtn = newBorderlessButton("\u2605 Favorites", null, "Open favorites popup");
		Favorites.setup(favoritesBtn);
		favoritesBtn.addActionListener((e) -> UIUtils.showComponentMenu(favoritesBtn));
		toolBar.add(favoritesBtn);
		Component glue = Box.createGlue();
		FrameDragListener.associate(this, glue);
		toolBar.add(glue);
		toolBar.add(newBorderlessButton("\u2699 Settings", this::openPreferences, "Change preferences"));
		addKeyStrokeAction(this, KeyBindings.KB_SETTINGS, this::openPreferences);
		toolBar.add(charsetCombo);
		toolBar.add(UIUtils.newBorderlessButton("\uD83D\uDEC8 About", () -> new AboutPane().createDialog("About").setVisible(true)));

		// help
		_help = new MainDesktopHelp(toolBar, this::openFile);

		// layout
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(_progressBar, BorderLayout.SOUTH);
		add(_help, BorderLayout.CENTER);

		setFileDropListener(this::openFiles);

		// preferences
		Preferences.FONT.subscribe((selectedFont) -> {
			if (!selectedFont.equals(_font)) {
				// System.out.println("Selected font is: " + selectedFont);
				_font = selectedFont;
				for (int i = 0; i < _tabs.getTabCount(); i++) {
					SearchPanel analyzer = (SearchPanel)_tabs.getComponentAt(i);
					analyzer.setFont(selectedFont);
				}
			}
		});
		Preferences.THEME.subscribe((theme) -> {
			try {
				UIUtils.setIcon(MainPanel.this, "icon.png", theme.dark);
			} catch (IOException e) {
				System.err.println("Unable to set window icon!");
				e.printStackTrace(System.err);
			}
		});
	}

	private void refreshCurrentTab() {
		Object comp = _tabs.getSelectedComponent();
		if (comp instanceof SearchPanel)
			((SearchPanel)comp).reloadTextSource();
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
			open(config, tabTitle, "<html>" + tabDescr + "</html>",
					(closer) -> new MixFileSource(config, _progressBar.addJob("Mixing"), closer)
			);
		}
	}

	private void openPreferences() {
		new PrefPanel(this).setVisible(true);
	}

	public void openFileDialog() {
		openFiles(FileUtils.fileDialog(
				FileUtils.DialogType.OPEN_MULTI,
				Preferences.RECENT_FILES.get().stream().findFirst().map(File::getParentFile).orElse(null)
		).iterator());
	}

	public void openFiles(@NotNull List<File> files) {
		openFiles(files.iterator());
	}

	public void openFiles(@NotNull Iterator<File> files) {
		if (files.hasNext()) {
			EventQueue.invokeLater(() -> {
				openFile(files.next());
				int firstOpenedTab = _tabs.getSelectedIndex();
				// "invokeLater" to avoid ArrayIndexOOB in laf
				EventQueue.invokeLater(() -> {
					while (files.hasNext()) {
						openFile(files.next());
						_tabs.setSelectedIndex(firstOpenedTab);
					}
				});
			});
		}
	}

	public void openFile(File file) {
		if (file.canRead()) {
			Preferences.RECENT_FILES.tap((p) -> p.roll(file));
			Preferences.RECENT_DIRS.tap((p) -> p.roll(file.getParentFile()));
			open(file, file.getName(), file.getAbsolutePath(),
					(closer) -> new TextFileSource(file.toPath(), Preferences.CHARSET.get(), _progressBar.addJob("Indexing"), closer)
			);
			Preferences.LAST_OPEN_PATH.set(file.getParentFile());
		}
	}

	private void onAddFirstTab() {
		remove(_help);
		_help.setArrowsHidden(true);
		_tabs.addTab(TAB_ADD, _help);
		_refreshBtn.setEnabled(true);
	}

	private void onRemoveLastTab() {
		int helpIdx = _tabs.indexOfComponent(_help);
		if (helpIdx != -1)
			_tabs.removeTabAt(helpIdx);
		remove(_tabs);
		_help.setArrowsHidden(false);
		add(_help, BorderLayout.CENTER);
		_refreshBtn.setEnabled(false);
	}

	public void open(Object key, String title, String description, Function<Runnable, TextSource> src) {
		if (_openFiles.containsKey(key)) {
			System.out.println("Already open: " + key);
			return;
		}
		AtomicReference<SearchPanel> searchPanelReference = new AtomicReference<>();

		// used for closing the tab, passet to the text source to handle indexing errors
		Runnable closer = () -> {
			SearchPanel searchPanel = searchPanelReference.get();
			if (searchPanel != null) {
				searchPanel.onClose();
				_tabs.remove(searchPanel);
			}
			closeTextSource(_openFiles.remove(key));
			if (_tabs.getTabCount() == 1)
				onRemoveLastTab();
			else {
				int idx = _tabs.indexOfTab(TAB_ADD);
				if (idx > 0)
					_tabs.setSelectedIndex(idx - 1);
			}
		};

		TextSource textSource = src.apply(closer);
		_openFiles.put(key, textSource);
		try {
			System.out.println("Opening: " + description);
			if (_tabs.getParent() == null) {
				onAddFirstTab();
				add(_tabs, BorderLayout.CENTER);
			}

			SearchPanel searchPanel = new SearchPanel(title, description, textSource, _progressBar, new TabNavigation(_tabs));
			searchPanel.setFileDropListener(this::openFiles);
			searchPanel.setFont(_font);

			int idx = _tabs.indexOfTab(TAB_ADD);
			_tabs.insertTab(searchPanel.toString(), null, searchPanel, null, idx);
			_tabs.setSelectedIndex(idx);

			searchPanelReference.set(searchPanel);
			searchPanel.addHierarchyListener(e -> {
				if (e.getID() == HierarchyEvent.PARENT_CHANGED && searchPanel.getParent() == null)
					closeTextSource(_openFiles.remove(key));
			});

			JComponent tabHeader = newTabHeader(searchPanel.toString(), closer, () -> _tabs.setSelectedIndex(_tabs.indexOfComponent(searchPanel)));
			ContextMenu.addActionCopy(tabHeader, key);
			_tabs.setTabComponentAt(idx, tabHeader);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			closeTextSource(_openFiles.remove(key));
		}
	}

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		UIUtils.setFileDropListener(this, this::openFiles);
		UIUtils.setFileDropListener(_help, this::openFiles);
	}

	public void bringToFront() {
		int state = getExtendedState();
		if ((state & Frame.ICONIFIED) == Frame.ICONIFIED)
			setExtendedState(state ^ Frame.ICONIFIED);
		toFront();
		requestFocus();
		// https://stackoverflow.com/a/643000/1326326
		EventQueue.invokeLater(() -> {
			setAlwaysOnTop(true);
			EventQueue.invokeLater(() -> setAlwaysOnTop(false));
		});
	}
}
