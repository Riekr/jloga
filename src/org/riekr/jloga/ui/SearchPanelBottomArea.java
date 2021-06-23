package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.Preferences;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.riekr.jloga.io.Preferences.LAST_SAVE_PATH;
import static org.riekr.jloga.io.ProgressListener.newProgressListenerFor;

public class SearchPanelBottomArea extends JPanel {

	private final int _level;

	private @Nullable SearchPanel _resultTextArea;

	private final MRUTextCombo _regex;
	private final AtomicBoolean _searching = new AtomicBoolean(false);
	private final SearchPanel _parent;
	private final JProgressBar _progressBar;

	public SearchPanelBottomArea(SearchPanel parent, JProgressBar progressBar, int level) {
		_parent = parent;
		_level = level;
		setLayout(new BorderLayout());
		_progressBar = progressBar;
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
		searchHeader.add(_regex, BorderLayout.CENTER);
		searchHeader.add(searchToolbar, BorderLayout.LINE_END);
		add(searchHeader, BorderLayout.NORTH);
	}

	private void saveResults() {
		if (_resultTextArea == null)
			return;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(Preferences.loadFile(LAST_SAVE_PATH, () -> new File(".")));
		fileChooser.setDialogTitle("Specify a file to save");
		int userSelection = fileChooser.showSaveDialog(SearchPanelBottomArea.this);
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
				_parent.getTextSource().requestSearch(
						searchPattern,
						newProgressListenerFor(_progressBar, "Searching").andThen(() -> _regex.setEnabled(true)),
						_searching,
						(res) -> {
							getResultTextArea().setTextSource(res);
							_parent.expandBottomArea();
							_searching.compareAndSet(true, false);
						}
				);
			} catch (PatternSyntaxException pse) {
				JOptionPane.showMessageDialog(this, pse.getLocalizedMessage(), "RegEx syntax error", JOptionPane.ERROR_MESSAGE);
				_regex.setEnabled(true);
			}
		}
	}

	@NotNull
	private SearchPanel getResultTextArea() {
		if (_resultTextArea == null) {
			_resultTextArea = new SearchPanel(_progressBar, _level + 1);
			_resultTextArea.setFont(getFont());
			_resultTextArea.setMinimumSize(new Dimension(0, 0));
			_resultTextArea.getTextArea().setLineListener((line) -> {
				Integer srcLine = _resultTextArea.getTextSource().getSrcLine(line);
				if (srcLine != null)
					_parent.getTextArea().centerOn(srcLine);
			});
			add(_resultTextArea, BorderLayout.CENTER);
		}
		return _resultTextArea;
	}

	public void removeResultTextArea() {
		if (_resultTextArea != null) {
			_resultTextArea.getTextArea().setLineListener(null);
			_resultTextArea.removeResultTextArea();
			remove(_resultTextArea);
			_resultTextArea = null;
			_regex.requestFocus();
			_parent.collapseBottomArea();
		}
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		if (_resultTextArea != null)
			_resultTextArea.setFont(f);
	}

}
