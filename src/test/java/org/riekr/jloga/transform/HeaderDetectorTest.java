package org.riekr.jloga.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.riekr.jloga.io.VolatileTextSource;

public class HeaderDetectorTest {

	@Test
	public void test() throws InterruptedException {
		Semaphore semaphore = new Semaphore(0);
		try (VolatileTextSource t = new VolatileTextSource(new ArrayList<>())) {
			t.add("A|B|C|D");
			t.add("1|2|3|4");
			t.add("1|2|3|");
			HeaderDetector detector = new HeaderDetector(t, semaphore::release, null);
			detector.detect();
			semaphore.acquire();
			assertEquals(4, detector.getColCount());
			assertTrue(detector.isOwnHeader());
		}
	}

}