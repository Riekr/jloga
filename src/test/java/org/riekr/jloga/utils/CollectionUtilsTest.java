package org.riekr.jloga.utils;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

public class CollectionUtilsTest {

	@Test
	public void indexOf() {
		List<Integer> list = IntStream.range(0, 10).boxed().collect(toList());
		assertEquals(3, CollectionUtils.indexOf(list, 3));
	}

	@Test
	public void swapRows() {
		LinkedHashMap<Integer, Integer> map = IntStream.range(0, 10).boxed().collect(toMap(
				identity(), identity(), (m1, m2) -> m1, LinkedHashMap::new
		));
		map.entrySet().forEach(System.out::println);
		int prevSize = map.size();
		boolean res = CollectionUtils.swapRows(map, 3, 6);
		map.entrySet().forEach(System.out::println);
		assertTrue(res);
		assertEquals(prevSize, map.size());
	}
}
