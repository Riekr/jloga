package org.riekr.jloga.react;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Subject<T> implements Observable<T>, Publisher<T>, Closeable {

	private final ConcurrentLinkedQueue<Observer<? super T>> _observers = new ConcurrentLinkedQueue<>();

	@Override
	public @NotNull Unsubscribable subscribe(Observer<? super T> observer) {
		_observers.add(observer);
		onSubscribe(observer);
		return () -> _observers.remove(observer);
	}

	protected void onSubscribe(Observer<? super T> observer) {
	}

	private long _next = 0L;

	@Override
	public void updateUI(T item) {
		long now = System.currentTimeMillis();
		if (_next < now) {
			_next = now + 200L;
			EventQueue.invokeLater(() -> {
				for (Observer<? super T> observer : _observers)
					observer.onNext(item);
			});
		}
	}

	public void next(T item) {
		_next = System.currentTimeMillis() + 200L;
		EventQueue.invokeLater(() -> {
			for (Observer<? super T> observer : _observers)
				observer.onNext(item);
		});
	}

	public void last(T item) {
		_next = Long.MAX_VALUE;
		EventQueue.invokeLater(() -> {
			Iterator<Observer<? super T>> i = _observers.iterator();
			while (i.hasNext()) {
				i.next().onNext(item);
				i.remove();
			}
		});
	}

	@Override
	public void close() {
		_observers.clear();
	}
}
