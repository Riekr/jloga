package org.riekr.jloga.react;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;

class ObserveOnce<T> implements Future<T>, Observer<T> {

	private final Observer<? super T> observer;
	private final AtomicBoolean       done  = new AtomicBoolean(false);
	private final CountDownLatch      latch = new CountDownLatch(1);

	private volatile T              _value;
	private volatile Throwable      _err;
	private volatile Unsubscribable _unsubscribable;
	private volatile boolean        _cancelled;

	public ObserveOnce(Observer<? super T> observer, Observable<T> observable) {
		this.observer = observer;
		synchronized (this) {
			_unsubscribable = null;
			_unsubscribable = observable.subscribe(this);
		}
		if (done.get())
			_unsubscribable.unsubscribe();
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (done.compareAndSet(false, true)) {
			complete();
			_cancelled = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		return _cancelled;
	}

	@Override
	public boolean isDone() {
		return _cancelled || done.get();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		latch.await();
		if (_err != null)
			throw new ExecutionException(_err);
		return _value;
	}

	@Override
	public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (latch.await(timeout, unit)) {
			if (_err != null)
				throw new ExecutionException(_err);
			return _value;
		}
		throw new TimeoutException();
	}

	@Override
	public void onNext(T item) {
		if (!done.getAndSet(true)) {
			_value = item;
			observer.onNext(item);
			complete();
		}
	}

	@Override
	public void onError(Throwable t) {
		if (!done.getAndSet(true)) {
			_err = t;
			observer.onError(t);
			complete();
		}
	}

	private void complete() {
		synchronized (done) {
			if (_unsubscribable != null)
				_unsubscribable.unsubscribe();
		}
	}
}
