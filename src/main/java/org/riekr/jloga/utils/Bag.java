package org.riekr.jloga.utils;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class Bag<K, V> extends ArrayList<Map.Entry<K, V>> implements Serializable {

	public Bag() {}

	@SuppressWarnings("unused")
	public Bag(int size) {
		super(size);
	}

	public Bag(Bag<K, V> o) {
		super(o);
	}

	public boolean remove(K k, V v) {
		return removeIf(e -> Objects.equals(e.getKey(), k) && Objects.equals(e.getValue(), v));
	}

	public boolean add(K k, V v) {
		return add(new AbstractMap.SimpleEntry<>(k, v));
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean swap(K k1, K k2) {
		Map.Entry<K, V> e1 = stream().filter(e -> Objects.equals(e.getKey(), k1)).findFirst().orElse(null);
		if (e1 == null)
			return false;
		Map.Entry<K, V> e2 = stream().filter(e -> e1 != e && Objects.equals(e.getKey(), k2)).findFirst().orElse(null);
		if (e2 == null)
			return false;
		V v = e1.getValue();
		e1.setValue(e2.getValue());
		e2.setValue(v);
		return true;
	}
}
