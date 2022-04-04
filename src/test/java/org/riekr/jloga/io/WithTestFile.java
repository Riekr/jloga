package org.riekr.jloga.io;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.BeforeClass;

public class WithTestFile {

	protected static Path testFilePath;

	@BeforeClass
	public static void getTestFile() {
		String name = System.getenv("jlogaTestFile");
		assumeNotNull("Env variable 'jlogaTestFile' not specified", name);
		File f = new File(name);
		assumeTrue("Can't read " + name, f.isFile() && f.canRead());
		testFilePath = f.toPath();
	}
}
