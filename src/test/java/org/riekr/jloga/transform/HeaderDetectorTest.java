package org.riekr.jloga.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.riekr.jloga.io.TempTextSource;

public class HeaderDetectorTest {

	@Test
	public void test() throws InterruptedException {
		Semaphore semaphore = new Semaphore(0);
		try (TempTextSource t = new TempTextSource()) {
			t.addLine(0, "A|B|C|D");
			t.addLine(0, "1|2|3|4");
			t.addLine(0, "1|2|3|");
			t.complete();
			HeaderDetector detector = new HeaderDetector(null);
			detector.detect(t, () -> {
				System.out.println(detector);
				semaphore.release();
			});
			semaphore.acquire();
			assertEquals(4, detector.getColCount());
			assertTrue(detector.isOwnHeader());
		}
	}

}