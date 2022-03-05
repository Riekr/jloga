package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.ui.utils.UIUtils;

public class SearchPanel extends JComponent implements FileDropListener {

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
		// add new tab code: I can't use a change listener to avoid loops
		// the "+" tab should never be selected and must stick as last tab
		Runnable addNewTab = () -> {
			SearchPanelBottomArea body = new SearchPanelBottomArea(SearchPanel.this, progressBar, level);
			body.setFont(getFont());
			int idx = _bottomTabs.indexOfTab(_TAB_ADD);
			_bottomTabs.insertTab(null, null, body, null, idx);
			_bottomTabs.setTabComponentAt(idx, newTabHeader(titleSupplier.get(), body));
			_bottomTabs.setSelectedIndex(idx);
			invalidate();
			if (_splitPane.getDividerLocation() >= _splitPane.getMaximumDividerLocation())
				collapseBottomArea();
		};
		_bottomTabs.setTabComponentAt(1, UIUtils.newTabHeader(_TAB_ADD, null, addNewTab));
		_bottomTabsNavigation = TabNavigation.createFor(_bottomTabs);
		_splitPane.add(_bottomTabs);
		// ugly but working (otherwise you have to hit "+" text)
		_bottomTabs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 1)
					addNewTab.run();
			}
		});
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

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		_textArea.setFileDropListener(consumer);
	}
}
