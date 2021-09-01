package org.riekr.jloga.ui;

import static org.riekr.jloga.io.Preferences.SEARCH_TYPE;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.search.RegExComponent;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.search.SearchRegistry;

public class SearchSelector extends JPanel {

	private final int                       _level;
	private final JButton                   _selectBtn;
	private final Consumer<SearchPredicate> _onSearchConsumer;

	private                 SearchComponent      _searchComponent;
	private                 JComponent           _searchUI;
	private final @Nullable Supplier<TextSource> _textSource;

	public SearchSelector(int level, Consumer<SearchPredicate> onSearchConsumer, @Nullable Supplier<TextSource> textSource) {
		_level = level;
		_onSearchConsumer = onSearchConsumer;
		_textSource = textSource;

		setLayout(new BorderLayout());

		_selectBtn = UIUtils.newButton("\u26A7", this::openSelection);
		add(_selectBtn, BorderLayout.LINE_START);

		SearchRegistry.get(
				Preferences.loadClass(SEARCH_TYPE, () -> RegExComponent.class),
				level,
				this::setSearchUI
		);
	}

	private void openSelection() {
		SearchRegistry.Entry<?>[] choices = SearchRegistry.getChoices();
		SearchRegistry.Entry<?> initialSelectionValue = Arrays.stream(choices)
				.filter((e) -> e.comp == _searchComponent.getClass())
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
			Preferences.save(SEARCH_TYPE, setSearchUI(input.newInstance(_level)));
	}

	private <T extends JComponent & SearchComponent> Class<?> setSearchUI(T comp) {
		if (_searchComponent != null)
			_searchComponent.onSearch(null);
		if (_searchUI != null)
			remove(_searchUI);
		_searchComponent = comp;
		_searchUI = comp;
		if (comp instanceof AutoDetect.Wizard && _textSource != null)
			((AutoDetect.Wizard)comp).setTextSourceSupplier(_textSource);
		comp.onSearch(_onSearchConsumer);
		add(comp, BorderLayout.CENTER);
		_selectBtn.setText(_searchComponent.getLabel());
		return comp.getClass();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_selectBtn.setEnabled(enabled);
		_searchUI.setEnabled(enabled);
	}
}
