package org.riekr.jloga.search;

import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

import java.util.function.IntConsumer;

public interface SearchPredicate {

	abstract class Simple implements SearchPredicate {
		private IntConsumer _accumulator;

		@Override
		public FilteredTextSource start(TextSource master) {
			ChildTextSource res = new ChildTextSource(master);
			_accumulator = res::addLine;
			return res;
		}

		protected abstract boolean accept(int line, String text);

		public final void verify(int line, String text) {
			if (accept(line, text))
				_accumulator.accept(line);
		}

		@Override
		public void end() {
			_accumulator = null;
		}
	}

	FilteredTextSource start(TextSource master);

	void verify(int line, String text);

	default void end() {
	}

}
