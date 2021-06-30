package org.riekr.jloga.ui;

import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.search.RegExComponent;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.search.SearchRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.riekr.jloga.io.Preferences.SEARCH_TYPE;

public class SearchSelector extends JPanel {

	private final int _level;
	private final JButton _selectBtn;
	private final Consumer<SearchPredicate> _onSearchConsumer;

	private SearchComponent _searchComponent;
	private JComponent _searchUI;

	public SearchSelector(int level, Consumer<SearchPredicate> onSearchConsumer) {
		_level = level;
		_onSearchConsumer = onSearchConsumer;

		setLayout(new BorderLayout());

		_selectBtn = UIUtils.newButton("\u26A7", this::openSelection);
		add(_selectBtn, BorderLayout.LINE_START);

		SearchRegistry.get(
				Preferences.load(SEARCH_TYPE, () -> RegExComponent.class),
				level,
				(searchComp) -> {
					_searchComponent = searchComp;
					_searchUI = searchComp;
					add(_searchUI, BorderLayout.CENTER);
					_selectBtn.setText(_searchComponent.getLabel());
					_searchComponent.onSearch(_onSearchConsumer);
				}
		);
	}

	private void openSelection() {
		SearchRegistry.Entry<?>[] choices = SearchRegistry.getChoices();
		SearchRegistry.Entry<?> initialSelectionValue = Arrays.stream(choices)
				.filter((e) -> e.comp == _searchComponent.getClass())
				.findFirst()
				.orElseGet(() -> choices[0]);
		SearchRegistry.Entry<?> input = (SearchRegistry.Entry<?>) JOptionPane.showInputDialog(
				_selectBtn,
				"Select a search type between those available:",
				"Choose search type",
				JOptionPane.QUESTION_MESSAGE,
				null,
				choices,
				initialSelectionValue
		);
		if (input != null)
			setSearchUI(input.newInstance(_level));
	}

	private <T extends JComponent & SearchComponent> void setSearchUI(T comp) {
		Preferences.save(SEARCH_TYPE, comp.getClass());
		_searchComponent.onSearch(null);
		remove(_searchUI);
		_searchComponent = comp;
		_searchUI = comp;
		comp.onSearch(_onSearchConsumer);
		add(comp, BorderLayout.CENTER);
		_selectBtn.setText(_searchComponent.getLabel());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_selectBtn.setEnabled(enabled);
		_searchUI.setEnabled(enabled);
	}
}
