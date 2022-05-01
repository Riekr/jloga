package org.riekr.jloga.io;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.READ;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class FullReadMismatchTest extends WithTestFile {

	@Test
	public void singleLine() throws IOException, ExecutionException, InterruptedException {
		Charset cs = ISO_8859_1;
		try (TextFileSource f = new TextFileSource(testFilePath, cs, (a, b) -> {}, () -> {})) {
			int lineCount = f.getLineCount();
			int lineNumber = 0;
			try (FileChannel fileChannel = FileChannel.open(testFilePath, READ)) {
				try (BufferedReader br = new BufferedReader(Channels.newReader(fileChannel, cs))) {
					String refLine;
					while ((refLine = br.readLine()) != null) {
						String line = f.getText(lineNumber);
						if (!refLine.equals(line)) {
							System.out.println();
							System.out.println("EXPECTED: " + refLine);
							System.out.println("ACTUAL  : " + line);
							throw new AssertionError("Mismatch at line " + lineNumber);
						}
						lineNumber++;
					}
				}
			}
			assertEquals("Line count mismatch", lineNumber, lineCount);
		}
	}

	@Test
	public void multiLine() throws IOException, ExecutionException, InterruptedException {
		Charset cs = ISO_8859_1;
		try (TextFileSource f = new TextFileSource(testFilePath, cs, (a, b) -> {}, () -> {})) {
			int lineCount = f.getLineCount();
			int lineNumber = 0;
			int refBufferLength = 30;
			ArrayList<String> refBuffer = new ArrayList<>(refBufferLength);
			try (FileChannel fileChannel = FileChannel.open(testFilePath, READ)) {
				try (BufferedReader br = new BufferedReader(Channels.newReader(fileChannel, cs))) {
					String refLine;
					while ((refLine = br.readLine()) != null) {
						refBuffer.clear();
						do {
							refBuffer.add(refLine);
						} while (refBuffer.size() < refBufferLength && (refLine = br.readLine()) != null);
						String[] actualBuffer = f.getText(lineNumber, refBufferLength);
						assertEquals(refBuffer.size(), actualBuffer.length);
						for (int i = 0, refBufferSize = refBuffer.size(); i < refBufferSize; i++) {
							final String expectedLine = refBuffer.get(i);
							final String actualLine = actualBuffer[i];
							if (!expectedLine.equals(actualLine)) {
								System.out.println();
								System.out.println("EXPECTED: " + expectedLine);
								System.out.println("ACTUAL  : " + actualLine);
								throw new AssertionError("Mismatch at line " + (lineNumber + i));
							}
						}
						lineNumber += refBuffer.size();
					}
				}
			}
			assertEquals("Line count mismatch", lineNumber, lineCount);
		}
	}
}
