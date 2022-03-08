package org.riekr.jloga.transform;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class FastSplitOperationTest {

	@Test
	public void cols() {
		// assertArrayEquals(new String[]{"1", "2", "3"}, new FastSplitOperation().apply("1\t2\t3"));
		// assertArrayEquals(new String[]{"1", "2", ""}, new FastSplitOperation().apply("1\t2\t"));
		// assertArrayEquals(new String[]{"1", "2"}, new FastSplitOperation().apply("1\t2"));
		assertArrayEquals(new String[]{"1", "2\t3"}, new FastSplitOperation().apply("1\t2\\\t3"));
	}

}