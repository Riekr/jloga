package org.riekr.jloga.react;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface Observable<T> {

	@NotNull
	Unsubscribable subscribe(Observer<? super T> observer);

	@NotNull
	default Unsubscribable subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError) {
		return subscribe(Observer.from(onNext, onError));
	}

	@NotNull
	default Unsubscribable subscribe(Consumer<? super T> onNext) {
		return subscribe((Observer<T>) onNext::accept);
	}

}
