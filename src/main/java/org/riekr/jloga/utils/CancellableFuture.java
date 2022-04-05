package org.riekr.jloga.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;

public final class CancellableFuture implements Future<Void> {

	private final AtomicBoolean _cancelled = new AtomicBoolean();
	private final Runnable      _cancelTask;

	public CancellableFuture(Runnable cancelTask) {
		_cancelTask = cancelTask;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (_cancelled.compareAndSet(false, true)) {
			_cancelTask.run();
			return true;
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		return _cancelled.get();
	}

	@Override
	public boolean isDone() {
		return isCancelled();
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public Void get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}
}
