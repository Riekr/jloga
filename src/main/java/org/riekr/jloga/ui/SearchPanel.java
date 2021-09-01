package org.riekr.jloga.ui;

import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SearchPanel extends JComponent {

	private static final String _TAB_ADD    = " + ";
	private static final String _TAB_PREFIX = "Search ";

	private final VirtualTextArea _textArea;
	private final JSplitPane      _splitPane;
	private final JTabbedPane     _bottomTabs;
	private final TabNavigation   _bottomTabsNavigation;

	private final JobProgressBar _progressBar;
	private       String         _title;
	private       int            _searchId = 0;

	public SearchPanel(String title, String description, TextSource src, JobProgressBar progressBar, @Nullable TabNavigation tabNavigation) {
		this(progressBar, 0, tabNavigation);
		_title = title;
		JLabel descriptionLabel = ContextMenu.addActionCopy(new JLabel(description));
		add(descriptionLabel, BorderLayout.NORTH);
		setTextSource(src);
	}


	public SearchPanel(JobProgressBar progressBar, int level, @Nullable TabNavigation tabNavigation) {
		setLayout(new BorderLayout());
		_textArea = new VirtualTextArea(tabNavigation);
		_textArea.setMinimumSize(new Dimension(0, 0));
		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setResizeWeight(1);
		_progressBar = progressBar;

		// layout
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.add(_textArea);
		_bottomTabs = new JTabbedPane();
		Supplier<String> titleSupplier = () -> _TAB_PREFIX + (char)('A' + level) + (++_searchId);
		SearchPanelBottomArea tabContent = new SearchPanelBottomArea(this, progressBar, level);
		_bottomTabs.addTab(titleSupplier.get(), tabContent);
		_bottomTabs.setTabComponentAt(0, newTabHeader(titleSupplier.get(), tabContent));
		_bottomTabs.addTab(_TAB_ADD, null);
		_bottomTabs.setTabComponentAt(1, UIUtils.newTabHeader(_TAB_ADD, null, () -> {
			SearchPanelBottomArea body = new SearchPanelBottomArea(SearchPanel.this, progressBar, level);
			body.setFont(getFont());
			int idx = _bottomTabs.indexOfTab(_TAB_ADD);
			_bottomTabs.insertTab(null, null, body, null, idx);
			_bottomTabs.setTabComponentAt(idx, newTabHeader(titleSupplier.get(), body));
			_bottomTabs.setSelectedIndex(idx);
			invalidate();
			if (_splitPane.getDividerLocation() >= _splitPane.getMaximumDividerLocation())
				collapseBottomArea();
		}));
		_bottomTabsNavigation = TabNavigation.createFor(_bottomTabs);
		_splitPane.add(_bottomTabs);
	}

	private Component newTabHeader(String title, SearchPanelBottomArea tabContent) {
		return UIUtils.newTabHeader(title,
				() -> removeBottomArea(tabContent),
				() -> _bottomTabs.setSelectedComponent(tabContent));
	}

	public void setTextSource(TextSource src) {
		_textArea.setTextSource(src);
		if (src.isIndexing())
			src.setIndexingListener(_progressBar.addJob(src instanceof MixFileSource ? "Mixing" : "Indexing"));
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

	public void removeBottomArea(SearchPanelBottomArea bottomArea) {
		int idx = _bottomTabs.indexOfComponent(bottomArea);
		_bottomTabs.setSelectedIndex(Math.max(0, idx - 1));
		_bottomTabs.removeTabAt(idx);
		if (_bottomTabs.getTabCount() == 1) // + button is a tab!
			collapseBottomArea();
	}

	public void onClose() {
		_textArea.onClose();
		searchPanelBottomAreaStream().forEach(SearchPanelBottomArea::onClose);
	}

	public TabNavigation getBottomTabsNavigation() {
		return _bottomTabsNavigation;
	}
}
