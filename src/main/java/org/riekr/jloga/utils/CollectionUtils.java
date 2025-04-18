package org.riekr.jloga.utils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CollectionUtils {

	public static <K, V> Optional<K> findKeyForValue(Map<K, V> values, V value) {
		return values.entrySet().stream()
				.filter((e) -> e.getValue().equals(value))
				.findAny()
				.map(Map.Entry::getKey);
	}

	public static <T> int indexOf(Iterable<? super T> it, T val) {
		int res = 0;
		for (Object candidate : it) {
			if (Objects.equals(candidate, val))
				return res;
			res++;
		}
		return -1;
	}

	@SuppressWarnings("unchecked") public static <K, V> boolean swapRows(Map<K, V> map, K key1, K key2) {
		Set<K> keys = map.keySet();
		int index1 = indexOf(keys, key1);
		if (index1 == -1)
			return false;
		int index2 = indexOf(keys, key2);
		if (index2 == -1)
			return false;
		if (index1 == index2)
			return false;
		if (index1 > index2) {
			int i = index1;
			index1 = index2;
			index2 = i;
			K k = key1;
			key1 = key2;
			key2 = k;
		}
		int size = map.size();
		Map<K, V> temp;
		try {
			temp = (Map<K, V>)map.getClass().getConstructor(Integer.TYPE).newInstance(size);
		} catch (Throwable e) {
			throw new RuntimeException("Unable to copy map", e);
		}
		int i = 0;
		for (Map.Entry<K, V> e : map.entrySet()) {
			if (i == index1)
				temp.put(key2, map.get(key2));
			else if (i == index2)
				temp.put(key1, map.get(key1));
			else
				temp.put(e.getKey(), e.getValue());
			i++;
		}
		map.clear();
		map.putAll(temp);
		return true;
	}

	private CollectionUtils() {}
}
