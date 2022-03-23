package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.concurrent.Future;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchException;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.utils.UIUtils;

public class SearchPanelBottomArea extends JPanel {
	private static final long serialVersionUID = 8509725982053259245L;

	private final String _title;
	private final int    _level;

	private @Nullable SearchPanel _resultTextArea;

	private final SearchPanel    _parent;
	private final JobProgressBar _progressBar;

	private final SearchSelector _searchUI;
	private       Future<?>      _searching;

	public SearchPanelBottomArea(String title, SearchPanel parent, JobProgressBar progressBar, int level) {
		_title = title;
		_parent = parent;
		_level = level;
		setLayout(new BorderLayout());
		_progressBar = progressBar;
		JPanel searchHeader = new JPanel();
		searchHeader.setLayout(new BorderLayout());
		_searchUI = new SearchSelector(level, this::search, parent::getTextSource, Map.of(
				"Title", title,
				"RootTitle", parent.getRootTitle()
		));
		searchHeader.add(_searchUI, BorderLayout.CENTER);

		JToolBar searchToolbar = new JToolBar();
		searchToolbar.add(UIUtils.newBorderlessButton("\u274C", () -> {
			if (_searching != null && !_searching.isDone()) {
				_searching.cancel(true);
			} else if (!this.removeResultTextArea()) {
				_parent.removeBottomArea(this);
			}
		}));
		searchToolbar.add(UIUtils.newBorderlessButton("\uD83D\uDDAB", this::saveResults));
		searchHeader.add(searchToolbar, BorderLayout.LINE_END);
		add(searchHeader, BorderLayout.NORTH);
	}

	private void saveResults() {
		if (_resultTextArea == null)
			return;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(Preferences.LAST_SAVE_PATH.get());
		fileChooser.setDialogTitle("Specify a file to save");
		int userSelection = fileChooser.showSaveDialog(SearchPanelBottomArea.this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			System.out.println("Save as file: " + fileToSave.getAbsolutePath());
			_resultTextArea.getTextSource().requestSave(fileToSave, _progressBar.addJob("Saving"));
			Preferences.LAST_SAVE_PATH.set(fileToSave.getParentFile());
		}
	}

	private synchronized void search(SearchPredicate predicate) {
		if (predicate != null && _searchUI.isEnabled()) {
			_searchUI.setEnabled(false);
			if (_searching != null && !_searching.isDone())
				_searching.cancel(true);
			_searching = _parent.getTextSource().requestSearch(
					predicate,
					_progressBar.addJob("Searching").afterAll(() -> _searchUI.setEnabled(true)),
					(res) -> {
						getResultTextArea().setTextSource(res);
						_parent.expandBottomArea();
					},
					(err) -> EventQueue.invokeLater(() -> {
						if (err instanceof SearchException) {
							JOptionPane.showMessageDialog(this,
									err.getCause() == null ? err.getLocalizedMessage() : err.getCause().getLocalizedMessage(),
									err.getLocalizedMessage(),
									JOptionPane.ERROR_MESSAGE
							);
						} else
							JOptionPane.showMessageDialog(this, err.getLocalizedMessage(), "Search error", JOptionPane.ERROR_MESSAGE);
					})
			);
		}
	}

	@NotNull
	private SearchPanel getResultTextArea() {
		if (_resultTextArea == null) {
			_resultTextArea = new SearchPanel(_title, _progressBar, _level + 1, _parent.getBottomTabsNavigation(), _parent);
			_resultTextArea.setFont(getFont());
			_resultTextArea.setMinimumSize(new Dimension(0, 0));
			_resultTextArea.getTextArea().setLineClickListener((line) -> {
				Integer srcLine = _resultTextArea.getTextSource().getSrcLine(line);
				if (srcLine != null) {
					_parent.getTextArea().centerOn(srcLine);
					_resultTextArea.getTextArea().setHighlightedLine(line);
				}
			});
			add(_resultTextArea, BorderLayout.CENTER);
		}
		return _resultTextArea;
	}

	public boolean removeResultTextArea() {
		if (_resultTextArea != null) {
			_resultTextArea.getTextArea().setLineClickListener(null);
			remove(_resultTextArea);
			_resultTextArea = null;
			_searchUI.requestFocus();
			_parent.collapseBottomArea();
			return true;
		}
		return false;
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		if (_resultTextArea != null)
			_resultTextArea.setFont(f);
	}

	public void onClose() {
		if (_resultTextArea != null)
			_resultTextArea.onClose();
		if (_searching != null)
			_searching.cancel(true);
	}

	public String getTitle() {
		return _title;
	}

	public SearchSelector getSearchUI() {
		return _searchUI;
	}
}
