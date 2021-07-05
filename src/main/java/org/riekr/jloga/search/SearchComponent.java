package org.riekr.jloga.search;

import java.util.function.Consumer;

public interface SearchComponent {

	void onSearch(Consumer<SearchPredicate> consumer);

	default String getLabel() {
		String res = getClass().getSimpleName();
		return res.replaceAll("([A-Z])", " $1").trim();
	}

}
