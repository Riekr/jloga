package org.riekr.jloga.react;

public interface Publisher<T> {

	void next(T item);

}
