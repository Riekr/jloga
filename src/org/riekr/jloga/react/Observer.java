package org.riekr.jloga.react;

import java.util.function.Consumer;

public interface Observer<T> {

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

	void onNext(T item);

	default void onError(Throwable t) {
		t.printStackTrace(System.err);
	}
}
