package org.riekr.jloga.search.simple;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;

class StreamSearchPredicate implements SearchPredicate {

	private static final int _QLEN = 50000;
	private static final int _QLIM = _QLEN - 1;

	protected final Predicate<Entry> _predicate;
	private         ChildTextSource  _childTextSource;
	private final   Entry[]          _pool = new Entry[_QLEN];
	private         int              _pos  = 0;

	public StreamSearchPredicate(Predicate<String> predicate) {
		_predicate = (entry) -> predicate.test(entry.text);
		for (int i = 0; i < _QLEN; i++)
			_pool[i] = new Entry();
	}

	public StreamSearchPredicate(Supplier<Predicate<String>> predicateSupplier) {
		ThreadLocal<Predicate<String>> tl = ThreadLocal.withInitial(predicateSupplier);
		_predicate = (entry) -> tl.get().test(entry.text);
		for (int i = 0; i < _QLEN; i++)
			_pool[i] = new Entry();
	}

	@Override
	public final FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("Not ended");
		_childTextSource = new ChildTextSource(master);
		return _childTextSource;
	}

	@Override
	public final void end(boolean interrupted) {
		if (_childTextSource == null)
			throw new IllegalStateException("Not started");
		if (_pos != 0)
			spool();
		_childTextSource = null;
	}

	public final void verify(int line, String text) {
		final Entry entry = _pool[_pos];
		assert entry != null;
		entry.line = line;
		entry.text = text;
		if (_pos == _QLIM)
			spool();
		else
			_pos++;
	}

	private void spool() {
		Arrays.stream(_pool, 0, _pos).parallel()
				.filter(_predicate)
				.mapToInt((entry) -> entry.line)
				.forEachOrdered(_childTextSource::addLine);
		_pos = 0;
	}

	static class Entry {
		int    line;
		String text;
	}

}
