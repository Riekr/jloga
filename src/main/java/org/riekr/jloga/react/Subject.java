package org.riekr.jloga.react;

import java.awt.*;
import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.jetbrains.annotations.NotNull;

public class Subject<T> implements Observable<T>, Publisher<T>, Closeable {

	protected static <T> void dispatch(T value, Observer<T> observer) {
		try {
			observer.onNext(value);
		} catch (Throwable t) {
			try {
				observer.onError(t);
			} catch (Throwable ignored) {
			}
		}
	}

	private final ConcurrentLinkedQueue<Observer<? super T>> _observers = new ConcurrentLinkedQueue<>();

	@Override
	public @NotNull Unsubscribable subscribe(Observer<? super T> observer) {
		_observers.add(observer);
		onSubscribe(observer);
		return () -> _observers.remove(observer);
	}

	protected void onSubscribe(Observer<? super T> observer) {}

	private long _next = 0L;

	@Override
	public void updateUI(T item) {
		long now = System.currentTimeMillis();
		if (_next < now) {
			_next = now + 200L;
			EventQueue.invokeLater(() -> {
				for (Observer<? super T> observer : _observers)
					dispatch(item, observer);
			});
		}
	}

	public void next(T item) {
		_next = System.currentTimeMillis() + 200L;
		EventQueue.invokeLater(() -> {
			for (Observer<? super T> observer : _observers)
				dispatch(item, observer);
		});
	}

	public void last(T item) {
		_next = Long.MAX_VALUE;
		EventQueue.invokeLater(() -> {
			Iterator<Observer<? super T>> i = _observers.iterator();
			while (i.hasNext()) {
				dispatch(item, i.next());
				i.remove();
			}
		});
	}

	@Override
	public void close() {
		_observers.clear();
	}
}
