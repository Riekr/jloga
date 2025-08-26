package org.riekr.jloga.io;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.pmem.PagedJavaList;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.utils.CancellableFuture;
import org.riekr.jloga.utils.TextUtils;

public abstract class ReloadableVolatileTextSource extends VolatileTextSource {

	private volatile Future<?> _loadingFuture;
	private volatile boolean   _loading = true;

	public ReloadableVolatileTextSource() {
		super(PagedJavaList.ofStrings());
	}

	protected final void doFirstLoad(@Nullable Supplier<ProgressListener> progressListenerSupplier) {
		_loadingFuture = defaultAsyncIO(() -> reload(progressListenerSupplier == null ? () -> ProgressListener.NOP : progressListenerSupplier));
	}

	@Override
	public final boolean supportsReload() {
		return true;
	}

	public final Future<?> requestReload(Supplier<ProgressListener> progressListenerSupplier) {
		if (_loadingFuture == null || _loadingFuture.isDone()) {
			_loadingFuture = defaultAsyncIO(() -> reload(progressListenerSupplier));
			_loading = true;
		}
		return _loadingFuture;
	}

	@Override
	public void close() {
		super.close();
		_loadingFuture.cancel(true);
		_loading = false;
	}

	@Override
	public final boolean isIndexing() {
		return _loading;
	}

	@Override
	public void add(String line) {
		if (_loading)
			super.add(line);
		else
			throw new IllegalStateException("Not loading");
	}

	@Override
	public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		_loadingFuture.get();
		super.search(predicate, out, progressListener, running);
	}

	@Override
	public int getLineCount() {
		final Future<?> loading = _loadingFuture;
		if (loading != null) {
			try {
				loading.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}
		return super.getLineCount();
	}

	@Override
	public Future<?> requestText(int fromLine, int count, Consumer<String> consumer) {
		if (_loading) {
			AtomicReference<Runnable> unsubscribe = new AtomicReference<>(() -> {});
			unsubscribe.set(_lineCountSubject.subscribe(lastLine -> {
				int toLine = Math.min(fromLine + count, super.getLineCount());
				if (fromLine < lastLine) {
					String text = TextUtils.toString(getText(fromLine, toLine - fromLine), count);
					EventQueue.invokeLater(() -> consumer.accept(text));
					if (toLine >= lastLine) {
						unsubscribe.get().run();
						return;
					}
				}
				if (!_loading)
					unsubscribe.get().run();
			})::unsubscribe);
			return new CancellableFuture(unsubscribe.get());
		}
		return defaultAsyncIO(() -> {
			String text = TextUtils.toString(getText(fromLine, Math.min(super.getLineCount() - fromLine, count)), count);
			EventQueue.invokeLater(() -> consumer.accept(text));
		});
	}

	protected abstract void doReload(Supplier<ProgressListener> progressListenerSupplier) throws IOException;

	@Override
	public final void reload(Supplier<ProgressListener> progressListenerSupplier) {
		try {
			clear();
			doReload(progressListenerSupplier);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new IndexingException("Error reloading", e);
		} finally {
			_loading = false;
		}
	}

}
