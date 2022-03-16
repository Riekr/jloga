package org.riekr.jloga.react;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

public interface Observable<T> {

	@NotNull
	Unsubscribable subscribe(Observer<? super T> observer);

	@NotNull
	default Unsubscribable subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError) {
		return subscribe(Observer.from(onNext, onError));
	}

	default <R> Observable<R> map(Function<T, R> mapper) {
		return observer -> this.subscribe(new Observer<>() {
			@Override
			public void onNext(T item) {
				observer.onNext(item == null ? null : mapper.apply(item));
			}

			@Override
			public void onError(Throwable t) {
				observer.onError(t);
			}
		});
	}

}
