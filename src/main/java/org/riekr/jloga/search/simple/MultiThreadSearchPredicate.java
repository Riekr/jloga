package org.riekr.jloga.search.simple;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;

/**
 * @deprecated Not working as expected, miss matches in the tail
 */
@SuppressWarnings("unused") @Deprecated
class MultiThreadSearchPredicate implements SearchPredicate {

	private static final int _PSIZE = Integer.max(1, (Runtime.getRuntime().availableProcessors() / 2) - 1);
	private static final int _QLEN  = _PSIZE * 10000;
	private static final int _QLIM  = _QLEN - 1;

	private final    ArrayDeque<Searcher>      _workers        = new ArrayDeque<>(_PSIZE);
	private final    ArrayBlockingQueue<Queue> _queues         = new ArrayBlockingQueue<>(_PSIZE);
	private          ExecutorService           _searchExecutor = null;
	private          ExecutorService           _spoolExecutor  = null;
	private @NotNull Queue                     _qActive;
	private          ChildTextSource           _childTextSource;

	public MultiThreadSearchPredicate(Predicate<String> predicate) {
		for (int i = 0; i < _PSIZE; i++) {
			_workers.add(new Searcher(predicate));
			_queues.add(new Queue());
		}
		_qActive = _queues.remove();
	}

	public MultiThreadSearchPredicate(Supplier<Predicate<String>> predicateSupplier) {
		for (int i = 0; i < _PSIZE; i++) {
			_workers.add(new Searcher(predicateSupplier.get()));
			_queues.add(new Queue());
		}
		_qActive = _queues.remove();
	}

	@Override
	public final FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("Not ended");
		_childTextSource = new ChildTextSource(master);
		_searchExecutor = Executors.newFixedThreadPool(_PSIZE);
		_spoolExecutor = Executors.newSingleThreadExecutor();
		return _childTextSource;
	}

	@Override
	public final void end(boolean interrupted) {
		if (_childTextSource == null)
			throw new IllegalStateException("Not started");
		try {
			if (interrupted)
				_searchExecutor.shutdownNow();
			else
				_searchExecutor.shutdown();
			if (!_searchExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS))
				throw new CompletionException("Search timed out", null);
			List<Runnable> remainingSpools = _spoolExecutor.shutdownNow();
			if (!_spoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS))
				throw new CompletionException("Spool timed out", null);
			System.out.println("Remaining spooling tasks: " + remainingSpools.size());
			for (Runnable spool : remainingSpools)
				spool.run();
			_childTextSource = null;
			_searchExecutor = null;
			_spoolExecutor = null;
		} catch (InterruptedException e) {
			throw new CompletionException(e);
		}
	}

	public final void verify(int line, String text) {
		final Entry entry = _qActive.entries[_qActive.pos];
		entry.line = line;
		entry.text = text;
		if (_qActive.pos == _QLIM)
			spool();
		else
			_qActive.pos++;
	}

	private void spool() {
		try {
			_workers.removeFirst().start(_qActive);
			_qActive = _queues.take();
		} catch (InterruptedException e) {
			throw new CompletionException(e);
		}
	}

	static class Entry {
		int     line;
		String  text;
		boolean accepted;
	}

	class Queue implements Runnable {
		final Entry[] entries = new Entry[_QLEN];
		int pos, from;
		private CompletableFuture<?> _future;

		Queue() {
			for (int i = 0; i < _QLEN; i++)
				entries[i] = new Entry();
		}

		public void start(@NotNull CompletableFuture<?> search) {
			_future = search;
			_spoolExecutor.execute(this);
		}

		@Override
		public void run() {
			_future.join();
			for (int i = from; i < pos; i++) {
				Entry entry = entries[i];
				if (entry.accepted)
					_childTextSource.addLine(entry.line);
			}
			pos = 0;
			_queues.offer(this);
		}
	}

	class Searcher implements Runnable {
		private final Predicate<String> _predicate;
		private       Queue             _queue;

		Searcher(Predicate<String> predicate) {
			_predicate = predicate;
		}

		public void start(@NotNull Queue queue) {
			_queue = queue;
			queue.start(CompletableFuture.runAsync(this, _searchExecutor));
		}

		@Override
		public void run() {
			int e = 0;
			for (; e < _queue.pos; e++) {
				Entry entry = _queue.entries[e];
				if (_predicate.test(entry.text)) {
					entry.accepted = true;
					break;
				} else
					entry.accepted = false;
			}
			_queue.from = e;
			for (; e < _queue.pos; e++) {
				Entry entry = _queue.entries[e];
				entry.accepted = _predicate.test(entry.text);
			}
			_workers.add(this);
		}
	}
}
