package org.riekr.jloga.search;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.function.IntConsumer;

public interface SearchPredicate {

	abstract class Simple implements SearchPredicate {
		protected abstract boolean accept(int line, String text);

		public final void verify(int line, String text, IntConsumer accumulator) {
			if (accept(line, text))
				accumulator.accept(line);
		}
	}

	default void start() {
	}

	void verify(int line, String text, IntConsumer accumulator);

	default NavigableSet<Integer> end() {
		return Collections.emptyNavigableSet();
	}

}
