package org.riekr.jloga.ui;

import static org.riekr.jloga.utils.KeyUtils.addKeyStrokeAction;
import static org.riekr.jloga.utils.TextUtils.TAB_ADD;
import static org.riekr.jloga.utils.UIUtils.onClickListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.prefs.KeyBindings;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.PlainTextComponent;
import org.riekr.jloga.search.RegExComponent;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.utils.KeyUtils;
import org.riekr.jloga.utils.UIUtils;

public class SearchPanel extends JComponent implements FileDropListener {
	private static final long serialVersionUID = -6368198678080747740L;

	private static final String _TAB_PREFIX = "Search ";

	private final VirtualTextArea _textArea;
	private final JSplitPane      _splitPane;
	private final JTabbedPane     _bottomTabs;
	private final TabNavigation   _bottomTabsNavigation;
	private final JobProgressBar  _progressBar;
	private final SearchPanel     _parent;

	private final int _level;

	private final String _title;
	private       int    _searchId = 0;

	/** Main panel */
	public SearchPanel(String title, String description, TextSource src, JobProgressBar progressBar, @Nullable TabNavigation tabNavigation) {
		this(title, progressBar, 0, tabNavigation, null);
		JLabel descriptionLabel = ContextMenu.addActionCopy(new JLabel(description));
		add(descriptionLabel, BorderLayout.NORTH);
		setTextSource(src);
	}

	/** Child panel */
	public SearchPanel(String title, JobProgressBar progressBar, int level, @Nullable TabNavigation tabNavigation, @Nullable SearchPanel parent) {
		_title = title;
		_progressBar = progressBar;
		_level = level;
		_parent = parent;

		setLayout(new BorderLayout());
		_textArea = new VirtualTextArea(tabNavigation, title, parent == null ? null : parent.getTextArea());
		_textArea.setMinimumSize(new Dimension(0, 0));
		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setOneTouchExpandable(true);
		_splitPane.setResizeWeight(1);

		// layout
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.add(_textArea);
		_bottomTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		// add first empty tab
		String tabTitle = newTabTempTitle();
		SearchPanelBottomArea tabContent = new SearchPanelBottomArea(getChildTitle(tabTitle), this, _progressBar, _level);
		_bottomTabs.addTab(null, tabContent);
		_bottomTabs.setTabComponentAt(0, newTabHeader(tabTitle, tabContent));

		// add new tab code: I can't use a change listener to avoid loops
		// the "+" tab should never be selected and must stick as last tab
		_bottomTabs.addTab(TAB_ADD, null);
		_bottomTabsNavigation = new TabNavigation(_bottomTabs);
		_splitPane.add(_bottomTabs);
		// ugly but working (otherwise you have to hit "+" text)
		_bottomTabs.addMouseListener(onClickListener(this::addNewTab));
		setupKeyBindings();
	}

	/**
	 * In order to let keybindings work in {@link VirtualTextArea} you should allow keystrokes in {@link ROKeyListener#discard(KeyEvent)}
	 * after having checked that the key combinations does not have side effect on the editor.
	 */
	private void setupKeyBindings() {
		addKeyStrokeAction(this, KeyBindings.KB_FINDTEXT, () -> selectSearchInFocusedTab(PlainTextComponent.ID), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		addKeyStrokeAction(this, KeyBindings.KB_FINDREGEX, () -> selectSearchInFocusedTab(RegExComponent.ID), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		addKeyStrokeAction(this, KeyBindings.KB_FINDSELECT, () -> selectSearchInFocusedTab(null), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		addKeyStrokeAction(this, KeyBindings.KB_CLOSETAB, this::removeCurrentBottomArea, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		addKeyStrokeAction(this, KeyUtils.F5, this::reloadTextSource);
		addKeyStrokeAction(this, KeyUtils.CTRL_T, this::addNewTab, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	public void reloadTextSource() {
		_textArea.reload(() -> _progressBar.addJob("Reloading"));
	}

	private void selectSearchInFocusedTab(String id) {
		SearchPanelBottomArea tab;
		if (Preferences.FIND_NEWTAB.get() || (tab = getSelectedSearchPanelBottomArea()) == null)
			tab = addNewTab();
		SearchSelector searchSelector = tab.getSearchUI();
		if (searchSelector != null) {
			if (id == null)
				searchSelector.openSelection();
			else
				searchSelector.setSearchUI(id);
		}
	}

	private String getChildTitle(String tabTitle) {
		if (tabTitle == null || tabTitle.isBlank())
			return _title;
		return tabTitle + " \uD83E\uDC06 " + _title;
	}

	private String newTabTempTitle() {
		return _TAB_PREFIX + (char)('A' + _level) + (++_searchId);
	}

	private SearchPanelBottomArea addNewTab() {
		String tabTitle = newTabTempTitle();
		SearchPanelBottomArea body = new SearchPanelBottomArea(getChildTitle(tabTitle), SearchPanel.this, _progressBar, _level);
		body.setFont(getFont());
		int idx = _bottomTabs.indexOfTab(TAB_ADD);
		_bottomTabs.insertTab(null, null, body, null, idx);
		_bottomTabs.setTabComponentAt(idx, newTabHeader(this.newTabTempTitle(), body));
		_bottomTabs.setSelectedIndex(idx);
		invalidate();
		if (_splitPane.getDividerLocation() >= _splitPane.getMaximumDividerLocation())
			collapseBottomArea();
		return body;
	}

	private JComponent newTabHeader(String title, SearchPanelBottomArea tabContent) {
		JComponent res = UIUtils.newTabHeader(title,
				() -> removeBottomArea(tabContent),
				() -> _bottomTabs.setSelectedComponent(tabContent));
		ContextMenu.addActionCopy(res, title);
		return res;
	}

	public void setTextSource(TextSource src) {
		_textArea.setTextSource(src);
	}

	public TextSource getTextSource() {
		return _textArea.getTextSource();
	}

	private Stream<SearchPanelBottomArea> searchPanelBottomAreaStream() {
		return IntStream.range(0, _bottomTabs.getTabCount() - 1)
				.mapToObj(_bottomTabs::getComponentAt)
				.filter((c) -> c instanceof SearchPanelBottomArea)
				.map((c) -> (SearchPanelBottomArea)c);
	}

	@Nullable
	private SearchPanelBottomArea getSelectedSearchPanelBottomArea() {
		return (SearchPanelBottomArea)_bottomTabs.getSelectedComponent();
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		_textArea.setFont(f);
		searchPanelBottomAreaStream()
				.forEach((ba) -> ba.setFont(f));
	}

	@Override
	public String toString() {
		return _title == null ? super.toString() : _title;
	}

	public void expandBottomArea() {
		if (_splitPane.getResizeWeight() == 1.0) {
			_splitPane.setResizeWeight(.5);
			_splitPane.setDividerLocation(.5);
		}
	}

	public void collapseBottomArea() {
		_splitPane.setResizeWeight(1.0);
		_splitPane.setDividerLocation(-1);
	}

	public VirtualTextArea getTextArea() {
		return _textArea;
	}

	private void removeCurrentBottomArea() {
		SearchPanelBottomArea tab = getSelectedSearchPanelBottomArea();
		if (tab != null)
			removeBottomArea(tab);
	}

	public void removeBottomArea(SearchPanelBottomArea bottomArea) {
		int idx = _bottomTabs.indexOfComponent(bottomArea);
		_bottomTabs.setSelectedIndex(Math.max(0, idx - 1));
		_bottomTabs.removeTabAt(idx);
		if (_bottomTabs.getTabCount() == 1) // + button is a tab!
			collapseBottomArea();
		_textArea.requestFocusInWindow();
	}

	public void onClose() {
		_textArea.onClose();
		searchPanelBottomAreaStream().forEach(SearchPanelBottomArea::onClose);
	}

	public TabNavigation getBottomTabsNavigation() {
		return _bottomTabsNavigation;
	}

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		_textArea.setFileDropListener(consumer);
	}

	public String getTitle() {
		return _title;
	}

	public String getRootTitle() {
		return _parent == null ? _title : _parent.getRootTitle();
	}
}
