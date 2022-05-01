package org.riekr.jloga.io;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.riekr.jloga.utils.TempFiles;

public class TempTextSourceTest {

	@Test
	public void main() throws ExecutionException, InterruptedException {
		try (TempTextSource tts = new TempTextSource()) {
			int lines = 200000;
			for (int i = 0; i < lines; i++)
				tts.addLine(i, i + " XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			tts.complete();
			System.out.println("Wrote " + tts.pages() + " pages");
			Assert.assertNotEquals(0, tts.pages());
			Random random = new Random();
			ArrayList<Future<?>> futures = new ArrayList<>();
			for (int i = 0; i < 1000; i++) {
				int count = random.nextInt(20) + 10;
				int from = random.nextInt(lines - count);
				futures.add(tts.requestText(from, count, (text) -> {
					int id = from;
					for (String line : text.split("\n")) {
						Assert.assertTrue(line.startsWith(id + " "));
						id++;
						// TODO: Assert.assertEquals(count, id - from);
					}
				}));
			}
			for (Future<?> future : futures)
				future.get();
		}
	}

	@Test
	public void cleanup() {
		TempFiles.cleanup();
	}
}
