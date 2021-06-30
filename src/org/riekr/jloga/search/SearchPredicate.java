package org.riekr.jloga.search;

import java.util.Collections;
import java.util.NavigableSet;

public interface SearchPredicate {

	default void start() {
	}

	boolean accept(int line, String text);

	default NavigableSet<Integer> end() {
		return Collections.emptyNavigableSet();
	}

}
