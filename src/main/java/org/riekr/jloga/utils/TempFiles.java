package org.riekr.jloga.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.InterComm;

public class TempFiles {

	public static File createTempDirectory(@NotNull String name) {
		try {
			File tempDir = Files.createTempDirectory("jloga-" + name + '-').toFile();
			System.out.println("Created temp directory " + tempDir.getAbsolutePath());
			tempDir.deleteOnExit();
			return tempDir;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static File createTempFile(@NotNull String name) {
		return createTempFile(name, null);
	}

	public static File createTempFile(@NotNull String name, @Nullable File tempDir) {
		try {
			File temp = File.createTempFile("jloga-" + name + '-', null, tempDir);
			System.out.println("Created " + temp.getAbsolutePath());
			temp.deleteOnExit();
			return temp;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean isTemp(Path path) {
		// \AppData\Local\Temp\jloga-PagedList-9049821355325028396\jloga-Page-810638474214795088.tmp
		// \AppData\Local\Temp\jloga97252669508416147431c7d68ff
		String fn = path.getFileName().toString();
		return fn.matches("jloga[\\w-]+");
	}

	private static void deleteRecurs(Path path) {
		System.out.println("Removing " + path);
		try {
			Files.walk(path)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(file -> {
						if (!file.delete())
							System.err.println("Unable to delete " + file.getAbsolutePath());
					});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Invoke if there is only 1 instance!
	 * @see InterComm
	 */
	public static void cleanup() {
		//noinspection SpellCheckingInspection
		String tmp = System.getProperty("java.io.tmpdir");
		if (tmp != null && tmp.length() > 2) {
			try {
				Files.list(Path.of(tmp))
						.filter(TempFiles::isTemp)
						.forEach(TempFiles::deleteRecurs);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private TempFiles() {}
}
