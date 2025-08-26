package org.riekr.jloga.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;

/**
 * Like {@link TempFilteredTextSource} but faster and only for root panes.
 */
public class VolatileTextSource implements TextSource {

	protected final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();
	private final   List<String>        _data;
	private final   ReadWriteLock       _readWriteLock    = new ReentrantReadWriteLock();

	public VolatileTextSource(@NotNull List<String> backend) {
		_data = backend;
	}

	public VolatileTextSource(String firstLine, String... lines) {
		_data = new ArrayList<>(lines.length + 1);
		_data.add(firstLine);
		Collections.addAll(_data, lines);
	}

	public final void clear() {
		final Lock lock = _readWriteLock.writeLock();
		lock.lock();
		try {
			_data.clear();
		} finally {
			lock.unlock();
		}
		_lineCountSubject.next(0);
	}

	public void add(String line) {
		final Lock lock = _readWriteLock.writeLock();
		lock.lock();
		try {
			_data.add(line);
		} finally {
			lock.unlock();
		}
		_lineCountSubject.next(_data.size());
	}

	@Override
	public final String getText(int line) {
		final Lock lock = _readWriteLock.readLock();
		lock.lock();
		try {
			if (line < 0 || line >= _data.size())
				return "";
			return _data.get(line);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public final String[] getText(int fromLine, int count) {
		int toLinePlus1 = Math.min(fromLine + count, Math.toIntExact(_data.size()));
		final String[] lines = new String[toLinePlus1 - fromLine + 1];
		final Lock lock = _readWriteLock.readLock();
		lock.lock();
		try {
			for (int i = fromLine; i <= toLinePlus1; i++) {
				String line;
				if (i < 0 || i >= _data.size())
					line = "";
				else
					line = _data.get(i);
				lines[i - fromLine] = line;
			}
		} finally {
			lock.unlock();
		}
		return lines;
	}

	@Override
	public final Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return _lineCountSubject.once(Observer.async(consumer::accept));
	}

	@Override
	public final Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public int getLineCount() {
		return _data.size();
	}
}
