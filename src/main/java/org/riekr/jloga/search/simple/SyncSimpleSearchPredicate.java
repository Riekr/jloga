package org.riekr.jloga.search.simple;

import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;

import java.util.function.Predicate;

class SyncSimpleSearchPredicate implements SearchPredicate {

	private         ChildTextSource   _childTextSource;
	protected final Predicate<String> _predicate;

	public SyncSimpleSearchPredicate(Predicate<String> predicate) {
		_predicate = predicate;
	}

	@Override
	public final FilteredTextSource start(TextSource master) {
		_childTextSource = new ChildTextSource(master);
		return _childTextSource;
	}

	public final void verify(int line, String text) {
		if (_predicate.test(text))
			_childTextSource.addLine(line);
	}

	@Override
	public final void end() {
		_childTextSource = null;
	}
}
