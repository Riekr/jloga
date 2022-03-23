package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.search.SearchRegistry;
import org.riekr.jloga.utils.UIUtils;

public class SearchSelector extends JPanel {
	private static final long serialVersionUID = 1562652212113703845L;

	private final int                       _level;
	private final JButton                   _selectBtn;
	private final Consumer<SearchPredicate> _onSearchConsumer;
	private final Map<String, String>       _vars;

	private                 SearchComponent      _searchComponent;
	private                 JComponent           _searchUI;
	private final @Nullable Supplier<TextSource> _textSource;

	public SearchSelector(int level, Consumer<SearchPredicate> onSearchConsumer, @Nullable Supplier<TextSource> textSource, Map<String, String> vars) {
		_level = level;
		_onSearchConsumer = onSearchConsumer;
		_textSource = textSource;
		_vars = vars;

		setLayout(new BorderLayout());

		_selectBtn = UIUtils.newBorderlessButton("\u26A7", this::openSelection);
		add(_selectBtn, BorderLayout.LINE_START);

		setSearchUI(Preferences.LAST_SEARCH_TYPE.get(level));
	}

	public void openSelection() {
		SearchRegistry.Entry<?>[] choices = SearchRegistry.getChoices();
		SearchRegistry.Entry<?> initialSelectionValue = Arrays.stream(choices)
				.filter((e) -> e.id.equals(_searchComponent.getID()))
				.findFirst()
				.orElseGet(() -> choices[0]);
		SearchRegistry.Entry<?> input = (SearchRegistry.Entry<?>)JOptionPane.showInputDialog(
				this.getRootPane(),
				"Select a search type between those available:",
				"Choose search type",
				JOptionPane.QUESTION_MESSAGE,
				null,
				choices,
				initialSelectionValue
		);
		if (input != null)
			Preferences.LAST_SEARCH_TYPE.set(setSearchUI(input.newInstance(_level)), _level);
	}

	public void setSearchUI(String id) {
		SearchRegistry.get(id, _level, this::setSearchUI);
	}

	private <T extends JComponent & SearchComponent> String setSearchUI(T comp) {
		boolean focus = _searchComponent != null;
		if (_searchComponent != null)
			_searchComponent.onSearch(null);
		if (_searchUI != null)
			remove(_searchUI);
		_searchComponent = comp;
		_searchUI = comp;
		if (comp instanceof AutoDetect.Wizard && _textSource != null)
			((AutoDetect.Wizard)comp).setTextSourceSupplier(_textSource);
		comp.onSearch(_onSearchConsumer);
		comp.setVariables(_vars);
		add(comp, BorderLayout.CENTER);
		_selectBtn.setText(_searchComponent.getSearchIconLabel());
		if (focus)
			comp.requestFocusInWindow();
		return comp.getID();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_selectBtn.setEnabled(enabled);
		_searchUI.setEnabled(enabled);
	}
}
