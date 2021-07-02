package org.riekr.jloga.ui;

import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.riekr.jloga.io.ProgressListener.newProgressListenerFor;

public class SearchPanel extends JComponent {

	private static final String _TAB_ADD = " + ";
	private static final String _TAB_PREFIX = "Search ";

	private final VirtualTextArea _textArea;
	private final JSplitPane _splitPane;
	private final JTabbedPane _bottomTabs;

	private final JProgressBar _progressBar;
	private String _tag;
	private int _searchId = 0;

	public SearchPanel(File file, Charset charset, JProgressBar progressBar) {
		this(progressBar, 0);
		_tag = file.getName();
		add(new JLabel(file.getAbsolutePath()), BorderLayout.NORTH);
		TextSource src = new TextFileSource(file.toPath(), charset);
		setTextSource(src);
	}

	public SearchPanel(JProgressBar progressBar, int level) {
		setLayout(new BorderLayout());
		_textArea = new VirtualTextArea();
		_textArea.setMinimumSize(new Dimension(0, 0));
		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setResizeWeight(1);
		_progressBar = progressBar;

		// layout
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.add(_textArea);
		_bottomTabs = new JTabbedPane();
		Supplier<String> titleSupplier = () -> _TAB_PREFIX + (char) ('A' + level) + (++_searchId);
		_bottomTabs.addTab(titleSupplier.get(), new SearchPanelBottomArea(this, progressBar, level));
		_bottomTabs.addTab(_TAB_ADD, null);
		_bottomTabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				_bottomTabs.removeChangeListener(this);
				int idx = _bottomTabs.indexOfTab(_TAB_ADD);
				if (_bottomTabs.getSelectedIndex() == idx) {
					SearchPanelBottomArea body = new SearchPanelBottomArea(SearchPanel.this, progressBar, level);
					body.setFont(getFont());
					_bottomTabs.insertTab(titleSupplier.get(), null, body, null, idx);
					_bottomTabs.setSelectedIndex(idx);
				}
				EventQueue.invokeLater(() -> _bottomTabs.addChangeListener(this));
			}
		});
		_splitPane.add(_bottomTabs);
	}

	public void setTextSource(TextSource src) {
		_textArea.setTextSource(src);
		src.setIndexingListener(newProgressListenerFor(_progressBar, "Indexing"));
	}

	public TextSource getTextSource() {
		return _textArea.getTextSource();
	}

	private Stream<SearchPanelBottomArea> searchPanelBottomAreaStream() {
		return IntStream.range(0, _bottomTabs.getTabCount() - 1)
				.mapToObj(_bottomTabs::getComponentAt)
				.filter((c) -> c instanceof SearchPanelBottomArea)
				.map((c) -> (SearchPanelBottomArea) c);
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
		return _tag == null ? super.toString() : _tag;
	}

	public void expandBottomArea() {
		if (_splitPane.getResizeWeight() == 1.0) {
			_splitPane.setResizeWeight(.5);
			_splitPane.setDividerLocation(.5);
		}
	}

	public void collapseBottomArea() {
		if (_splitPane.getResizeWeight() != 1.0) {
			_splitPane.setResizeWeight(1.0);
			_splitPane.setDividerLocation(-1);
		}
	}

	public VirtualTextArea getTextArea() {
		return _textArea;
	}

	public void removeBottomArea(SearchPanelBottomArea bottomArea) {
		int idx = _bottomTabs.indexOfComponent(bottomArea);
		_bottomTabs.setSelectedIndex(Math.max(0, idx - 1));
		_bottomTabs.removeTabAt(idx);
	}
}
