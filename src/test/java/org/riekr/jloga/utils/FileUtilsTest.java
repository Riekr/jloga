package org.riekr.jloga.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Test;

public class FileUtilsTest {

	@Test
	public void emptyReaderTest() throws IOException {
		try (BufferedReader reader = FileUtils.emptyReader()) {
			assertNull(reader.readLine());
		}
		try (BufferedReader reader = FileUtils.emptyReader()) {
			assertEquals(0, reader.lines().count());
		}
	}
}
