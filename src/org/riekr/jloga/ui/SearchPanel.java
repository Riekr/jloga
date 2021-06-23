package org.riekr.jloga.ui;

import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;

import static org.riekr.jloga.io.ProgressListener.newProgressListenerFor;

public class SearchPanel extends JComponent {

	private final VirtualTextArea _textArea;
	private final JSplitPane _splitPane;
	private final SearchPanelBottomArea _bottomArea;

	private final JProgressBar _progressBar;
	private String _tag;

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
		_bottomArea = new SearchPanelBottomArea(this, progressBar, level);

		// layout
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.add(_textArea);
		_splitPane.add(_bottomArea);
	}

	public void setTextSource(TextSource src) {
		_textArea.setTextSource(src);
		src.setIndexingListener(newProgressListenerFor(_progressBar, "Indexing"));
	}

	public TextSource getTextSource() {
		return _textArea.getTextSource();
	}

	public void removeResultTextArea() {
		_bottomArea.removeResultTextArea();
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		_textArea.setFont(f);
		_bottomArea.setFont(f);
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
}
