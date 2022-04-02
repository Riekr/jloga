package org.riekr.jloga.utils;

import java.util.Map;
import java.util.Optional;

public class Utils {

	public static <K, V> Optional<K> findKeyForValue(Map<K, V> values, V value) {
		return values.entrySet().stream()
				.filter((e) -> e.getValue().equals(value))
				.findAny()
				.map(Map.Entry::getKey);
	}


	private Utils() {}
}
