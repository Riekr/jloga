package org.riekr.jloga.search;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;
import java.util.function.Consumer;

public interface SearchComponent {

	void onSearch(Consumer<SearchPredicate> consumer);

	String getID();

	default String getSearchIconLabel() {
		String res = getClass().getSimpleName();
		return res.replaceAll("([A-Z])", " $1").trim();
	}

	default void setVariables(Map<String, String> vars) {}

	@NotNull JComponent getUIComponent();

	default void prefill(String text) {
		// implementations supporting prefill should override this method
	}

}
