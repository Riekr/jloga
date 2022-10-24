package org.riekr.jloga.io;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

public class LinkedProperties extends Properties {
	private final LinkedHashSet<Object> _keyOrder = new LinkedHashSet<>();

	public Iterable<Map.Entry<Object, Object>> linkedEntrySet() {
		return () -> {
			Iterator<Object> order = _keyOrder.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {return order.hasNext();}

				@Override
				public Map.Entry<Object, Object> next() {
					Object key = order.next();
					return new Map.Entry<>() {
						@Override
						public Object getKey() {return key;}

						@Override
						public Object getValue() {return get(key);}

						@Override
						public Object setValue(Object value) {return LinkedProperties.super.put(key, value);}
					};
				}
			};
		};
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		_keyOrder.add(key);
		return super.put(key, value);
	}
}
