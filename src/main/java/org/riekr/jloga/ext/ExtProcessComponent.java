package org.riekr.jloga.ext;

import javax.swing.*;
import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.utils.UIUtils;

public class ExtProcessComponent extends JButton implements SearchComponent {
	private static final long serialVersionUID = 8599529986240844558L;

	private final String _id;
	private final String _icon;

	private       Consumer<SearchPredicate> _searchPredicateConsumer;
	private final ExtProcessManager         _manager;

	public ExtProcessComponent(String id, String icon, String label, File workingDirectory, String[] command) {
		_id = id;
		_icon = icon;
		_manager = new ExtProcessManager(command, workingDirectory);
		UIUtils.makeBorderless(this);
		setText(label);
		addActionListener((e) -> {
			if (_searchPredicateConsumer != null) {
				SearchPredicate searchPredicate = _manager.newSearchPredicate();
				if (searchPredicate != null)
					_searchPredicateConsumer.accept(searchPredicate);
			}
		});
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		_searchPredicateConsumer = consumer;
	}

	@Override
	public String getID() {
		return _id;
	}

	@Override
	public String getSearchIconLabel() {
		return _icon;
	}

	@Override
	public void setVariables(Map<String, String> vars) {
		_manager.setSearchVars(vars);
	}

	@Override
	public @NotNull JComponent getUIComponent() {
		return this;
	}

}
