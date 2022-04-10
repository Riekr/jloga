package org.riekr.jloga.react;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

public interface Observable<T> {

	@NotNull
	Unsubscribable subscribe(Observer<? super T> observer);

	@NotNull
	default Unsubscribable subscribe(Component component, Observer<? super T> observer) {
		AtomicReference<Unsubscribable> unsubscribable = new AtomicReference<>(subscribe(observer));
		HierarchyListener hierarchyListener = (e) -> {
			if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
				if (component.getParent() == null) {
					Unsubscribable u = unsubscribable.getAndSet(null);
					if (u != null)
						u.unsubscribe();
				} else {
					if (unsubscribable.get() == null)
						unsubscribable.set(subscribe(observer));
				}
			}
		};
		component.addHierarchyListener(hierarchyListener);
		return () -> {
			Unsubscribable u = unsubscribable.get();
			if (u != null)
				u.unsubscribe();
			component.removeHierarchyListener(hierarchyListener);
		};
	}

	@NotNull
	default Unsubscribable subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError) {
		return subscribe(Observer.from(onNext, onError));
	}

	default Future<T> once(Observer<? super T> observer) {
		return new ObserveOnce<>(observer, this);
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
