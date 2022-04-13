package org.riekr.jloga.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

public class TempTextSourceTest {

	@Test
	public void main() throws ExecutionException, InterruptedException {
		TempTextSource tts = new TempTextSource();
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
			futures.add(tts.requestText(from, count, (reader) -> {
				try (BufferedReader bufferedReader = new BufferedReader(reader)) {
					String line;
					int id = from;
					while ((line = bufferedReader.readLine()) != null) {
						Assert.assertTrue(line.startsWith(id + " "));
						id++;
					}
					// TODO: Assert.assertEquals(count, id - from);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
		}
		for (Future<?> future : futures)
			future.get();

	}
}
