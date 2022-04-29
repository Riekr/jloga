package org.riekr.jloga.utils;

import java.io.Serializable;

public class TaggedObject<T, V> implements Serializable {
	private static final long serialVersionUID = -4879529322166442292L;

	public final T tag;
	public final V value;

	public TaggedObject(T tag, V value) {
		this.tag = tag;
		this.value = value;
	}
}
