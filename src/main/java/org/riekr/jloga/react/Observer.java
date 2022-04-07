package org.riekr.jloga.react;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.riekr.jloga.utils.UIUtils;

public interface Observer<T> {

	static <T> Observer<T> uniq(Observer<T> observer) {
		return new Observer<>() {
			private T _lastValue;

			@Override
			public void onNext(T item) {
				if (!Objects.equals(_lastValue, item)) {
					observer.onNext(item);
					_lastValue = item;
				}
			}

			@Override
			public void onError(Throwable t) {
				observer.onError(t);
			}
		};
	}

	static <T> Observer<T> async(Observer<T> observer) {
		return new Observer<>() {
			private final AtomicBoolean _enqueued = new AtomicBoolean();
			private T _value;

			@Override
			public void onNext(T item) {
				_value = item;
				if (_enqueued.compareAndSet(false, true))
					UIUtils.invokeAfter(() -> {
						if (_enqueued.compareAndSet(true, false))
							observer.onNext(_value);
					}, 200);
			}

			@Override
			public void onError(Throwable t) {
				observer.onError(t);
			}
		};
	}

	static <T> Observer<T> from(Consumer<T> onNext, Consumer<Throwable> onError) {
		return new Observer<>() {
			@Override
			public void onNext(T item) {
				onNext.accept(item);
			}

			@Override
			public void onError(Throwable t) {
				onError.accept(t);
			}
		};
	}

	static <T> Observer<T> skip(int count, Observer<T> observer) {
		if (count <= 0)
			return observer;
		AtomicReference<Observer<T>> latch = new AtomicReference<>();
		latch.set(new Observer<>() {
			final AtomicInteger countDown = new AtomicInteger(count);

			@Override
			public void onNext(T item) {
				if (countDown.decrementAndGet() == 0)
					latch.set(observer);
			}

			@Override
			public void onError(Throwable t) {
				observer.onError(t);
			}
		});
		return new Observer<>() {
			@Override
			public void onNext(T item) {
				latch.get().onNext(item);
			}

			@Override
			public void onError(Throwable t) {
				latch.get().onError(t);
			}
		};
	}

	void onNext(T item);

	default void onError(Throwable t) {
		t.printStackTrace(System.err);
	}

}
