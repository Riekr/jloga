package org.riekr.jloga.httpd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import org.riekr.jloga.transform.ArrowConversion;
import org.riekr.jloga.transform.FastSplitOperation;

public class FinosPerspectiveServer extends ResourcesServer {

	public FinosPerspectiveServer() {
		this(false);
	}

	public FinosPerspectiveServer(boolean autoClose) {
		super("org/riekr/jloga/http/perspective", autoClose);
	}

	public void load(String title, String... data) {
		load(title, Arrays.stream(data));
	}

	public void load(String title, Stream<String> lines) {
		load(title, lines.filter((line) -> line != null && !line.isBlank())
				.map(new FastSplitOperation())
				.sequential().iterator()
		);
	}

	public void load(String title, String[][] data) {
		load(title, Arrays.stream(data).sequential().iterator());
	}

	public void load(String title, Iterator<String[]> data) {
		if (data.hasNext()) {
			final class LoadOperation extends ArrowConversion {
				LoadOperation() {
					super(data.next());
				}

				void set() {
					sendJS("s('" + (title == null ? "null" : title.replace("'", "\\'")) + "'," + toArrowChunk(data) + ')', this::update);
				}

				void update(String res) {
					if (!res.equals("OK"))
						throw new IllegalArgumentException("JS failed: " + res);
					if (data.hasNext())
						sendJS("u(" + toArrowChunk(data) + ')', this::update);
				}
			}
			LoadOperation loadOperation = new LoadOperation();
			if (data.hasNext())
				loadOperation.set();
		}
	}

	public static void main(String[] args) {
		if (args == null || args.length != 1) {
			System.err.println("Please specify a file to load");
			return;
		}
		Path file = Paths.get(args[0]);
		try {
			Stream<String> stream = Files.lines(file);
			FinosPerspectiveServer server = new FinosPerspectiveServer(true);
			server.start(false);
			server.load(file.getFileName().toString(), stream);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

}
