package org.riekr.jloga.io;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.READ;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class FullReadMismatchTest extends WithTestFile {

	@Test
	public void start() throws IOException, ExecutionException, InterruptedException {
		Charset cs = ISO_8859_1;
		TextFileSource f = new TextFileSource(testFilePath, cs, (a, b) -> {}, () -> {});
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
		Assert.assertEquals("Line count mismatch", lineNumber, lineCount);
	}
}
