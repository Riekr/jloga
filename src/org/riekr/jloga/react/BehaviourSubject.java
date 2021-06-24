package org.riekr.jloga.react;

import java.util.function.Supplier;

public class BehaviourSubject<T> extends Subject<T> implements Supplier<T> {

	private T _value;

	public BehaviourSubject(T initialValue) {
		_value = initialValue;
	}

	@Override
	public T get() {
		return _value;
	}

	@Override
	protected void onSubscribe(Observer<? super T> observer) {
		super.onSubscribe(observer);
		observer.onNext(_value);
	}

	@Override
	public void next(T item) {
		_value = item;
		super.next(item);
	}

	@Override
	public void first(T item) {
		_value = item;
		super.first(item);
	}

	@Override
	public void last(T item) {
		_value = item;
		super.last(item);
	}

	public void last() {
		super.last(_value);
	}

	@Override
	public String toString() {
		return String.valueOf(_value);
	}
}
