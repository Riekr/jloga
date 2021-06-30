package org.riekr.jloga.search;

import java.util.function.Consumer;

public interface SearchComponent {

	void onSearch(Consumer<SearchPredicate> consumer);

}
