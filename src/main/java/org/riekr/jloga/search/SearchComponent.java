package org.riekr.jloga.search;

import java.util.function.Consumer;

public interface SearchComponent {

	void onSearch(Consumer<SearchPredicate> consumer);

	String getID();

	default String getSearchIconLabel() {
		String res = getClass().getSimpleName();
		return res.replaceAll("([A-Z])", " $1").trim();
	}

}
