package org.riekr.jloga.search;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

public interface SearchPredicate {

	FilteredTextSource start(TextSource master);

	void verify(int line, String text);

	default void end() {}

}
