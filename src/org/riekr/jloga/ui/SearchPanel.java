package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.riekr.jloga.io.Preferences.LAST_SAVE_PATH;
import static org.riekr.jloga.io.ProgressListener.newProgressListenerFor;

public class SearchPanel extends JComponent {

	private final int _level;
	private final VirtualTextArea _textArea;
	private @Nullable SearchPanel _resultTextArea;

	private final JSplitPane _splitPane;
	private final JPanel _bottomArea;
	private final MRUTextCombo _regex;
	private final AtomicBoolean _searching = new AtomicBoolean(false);

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
		_level = level;
		setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
		setLayout(new BorderLayout());
		_textArea = new VirtualTextArea();
		_textArea.setMinimumSize(new Dimension(0, 0));
		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setResizeWeight(1);
		_progressBar = progressBar;
		_bottomArea = new JPanel();
		_bottomArea.setLayout(new BorderLayout());
		JPanel searchHeader = new JPanel();
		searchHeader.setLayout(new BorderLayout());
		_regex = new MRUTextCombo("regex." + level);
		_regex.setListener(this::search);
		JToolBar searchToolbar = new JToolBar();
		searchToolbar.add(UIUtils.newButton("\u274C", () -> {
			if (!_searching.compareAndExchange(true, false))
				this.removeResultTextArea();
		}));
		searchToolbar.add(UIUtils.newButton("\uD83D\uDDAB", this::saveResults));

		// layout
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.add(_textArea);
		_splitPane.add(_bottomArea);
		searchHeader.add(_regex, BorderLayout.CENTER);
		searchHeader.add(searchToolbar, BorderLayout.LINE_END);
		_bottomArea.add(searchHeader, BorderLayout.NORTH);
	}

	private void saveResults() {
		if (_resultTextArea == null)
			return;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(Preferences.loadFile(LAST_SAVE_PATH, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to save");
		int userSelection = fileChooser.showSaveDialog(SearchPanel.this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			System.out.println("Save as file: " + fileToSave.getAbsolutePath());
			_resultTextArea.getTextSource().requestSave(fileToSave, newProgressListenerFor(_progressBar, "Saving"));
			Preferences.save(LAST_SAVE_PATH, fileToSave.getParentFile());
		}
	}

	private synchronized void search(String regex) {
		if (regex != null && !regex.isEmpty() && _regex.isEnabled()) {
			_regex.setEnabled(false);
			try {
				Pattern searchPattern = Pattern.compile(regex);
				_searching.set(true);
				_textArea.getTextSource().requestSearch(
						searchPattern,
						newProgressListenerFor(_progressBar, "Searching").andThen(() -> _regex.setEnabled(true)),
						_searching,
						(res) -> {
							getResultTextArea().setTextSource(res);
							if (_splitPane.getResizeWeight() == 1.0) {
								_splitPane.setResizeWeight(.5);
								_splitPane.setDividerLocation(.5);
							}
						}
				);
			} catch (PatternSyntaxException pse) {
				JOptionPane.showMessageDialog(this, pse.getLocalizedMessage(), "RegEx syntax error", JOptionPane.ERROR_MESSAGE);
				_regex.setEnabled(true);
			}
		}
	}

	public void setTextSource(TextSource src) {
		_textArea.setTextSource(src);
		src.setIndexingListener(newProgressListenerFor(_progressBar, "Indexing"));
	}

	public TextSource getTextSource() {
		return _textArea.getTextSource();
	}

	@NotNull
	private SearchPanel getResultTextArea() {
		if (_resultTextArea == null) {
			_resultTextArea = new SearchPanel(_progressBar, _level + 1);
			_resultTextArea.setFont(getFont());
			_resultTextArea.setMinimumSize(new Dimension(0, 0));
			_resultTextArea._textArea.setLineListener((line) -> {
				Integer srcLine = _resultTextArea.getTextSource().getSrcLine(line);
				if (srcLine != null)
					_textArea.centerOn(srcLine);
			});
			_bottomArea.add(_resultTextArea, BorderLayout.CENTER);
		}
		return _resultTextArea;
	}

	private void removeResultTextArea() {
		if (_resultTextArea != null) {
			_resultTextArea._textArea.setLineListener(null);
			_resultTextArea.removeResultTextArea();
			_bottomArea.remove(_resultTextArea);
			_bottomArea.repaint();
			_resultTextArea = null;
			_regex.requestFocus();
			_splitPane.setResizeWeight(1.0);
			_splitPane.setDividerLocation(-1);
		}
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		_textArea.setFont(f);
		if (_resultTextArea != null)
			_resultTextArea.setFont(f);
	}

	@Override
	public String toString() {
		return _tag == null ? super.toString() : _tag;
	}
}
