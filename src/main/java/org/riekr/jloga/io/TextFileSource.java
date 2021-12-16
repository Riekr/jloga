package org.riekr.jloga.io;

import static java.nio.file.StandardOpenOption.READ;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

public class TextFileSource implements TextSource {

	// private static final int PAGE_SIZE = 1024 * 1024 * 2; // 2MB
	private static final int PAGE_SIZE = 1024 * 1024; // 1MB

	static final class IndexData {
		final long startPos;
		WeakReference<String[]> data;

		public IndexData(long startPos) {
			this.startPos = startPos;
		}
	}


	private final Path    _file;
	private final Charset _charset;

	private final Future<?>                        _indexing;
	private       NavigableMap<Integer, IndexData> _index;

	private       int                 _lineCount;
	private final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();

	private @NotNull ProgressListener _indexingListener     = ProgressListener.NOP;
	private final    Set<Runnable>    _indexChangeListeners = new CopyOnWriteArraySet<>();

	private int      _fromLine = Integer.MAX_VALUE;
	private String[] _lines;

	public TextFileSource(Path file, Charset charset) {
		_file = file;
		_charset = charset;
		_indexing = EXECUTOR.submit(() -> {
			System.out.println("Indexing " + _file);
			final long start = System.currentTimeMillis();
			_lineCount = 0;
			_lineCountSubject.next(0);
			_index = new TreeMap<>();
			_index.put(0, new IndexData(0));
			ByteBuffer byteBuffer = ByteBuffer.allocate(PAGE_SIZE);
			CharBuffer charBuffer = CharBuffer.allocate(PAGE_SIZE);
			CharsetDecoder decoder = _charset.newDecoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			CharsetEncoder encoder = _charset.newEncoder();
			try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
				long totalSize = fileChannel.size();
				try {
					while (fileChannel.read(byteBuffer) > 0) {
						decoder.decode(byteBuffer.flip(), charBuffer, false);
						charBuffer.flip();
						while (charBuffer.hasRemaining()) {
							if (charBuffer.get() == '\n') {
								_lineCount++;
								charBuffer.mark();
							}
						}
						long pos = fileChannel.position();
						_index.put(_lineCount, new IndexData(pos - encoder.encode(charBuffer.reset()).limit()));
						_indexingListener.onProgressChanged(pos, totalSize);
						_indexChangeListeners.forEach(Runnable::run);
						charBuffer.flip();
						byteBuffer.flip();
						_lineCountSubject.next(_lineCount);
					}
				} finally {
					_lineCountSubject.last();
					_indexingListener.onProgressChanged(totalSize, totalSize);
					_indexChangeListeners.forEach(Runnable::run);
					_indexChangeListeners.clear();
				}
				System.out.println("Indexed " + _file + ' ' + _lineCount + " lines in " + (System.currentTimeMillis() - start) + "ms (" + _index.size() + " pages)");
			} catch (ClosedByInterruptException ignored) {
				System.out.println("Indexing cancelled");
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		});
	}

	@Override
	public void requestText(int fromLine, int count, Consumer<Reader> consumer) {
		if (_indexing.isDone())
			TextSource.super.requestText(fromLine, count, consumer);
		else {
			_indexChangeListeners.add(new Runnable() {
				@Override
				public void run() {
					try {
						int toLinePlus1 = fromLine + count;
						if (_index.floorKey(fromLine) != null && _index.ceilingKey(toLinePlus1) != null) {
							_indexChangeListeners.remove(this);
							String[] lines = new String[toLinePlus1 - fromLine + 1];
							for (int line = fromLine; line <= toLinePlus1; line++)
								lines[line - fromLine] = getText(line);
							EventQueue.invokeLater(() -> consumer.accept(new StringsReader(lines)));
						}
					} catch (Throwable e) {
						_indexChangeListeners.remove(this);
						consumer.accept(new StringsReader.ErrorReader(e));
						if (e instanceof RuntimeException)
							throw (RuntimeException)e;
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

	private String[] loadPage(int fromLine, int toLine, long pos) throws IOException {
		String[] lines = new String[toLine - fromLine];
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			fileChannel.position(pos);
			try (BufferedReader br = new BufferedReader(Channels.newReader(fileChannel, _charset), PAGE_SIZE)) {
				for (int i = 0; i < lines.length; i++)
					lines[i] = br.readLine();
				return lines;
			}
		}
	}

	// TODO: should fill search results while indexing but is causing search line corruptions, should investigate why
	// @Override
	// public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException {
	// 	// dispatchLineCount called here to take advantage of 200ms scheduling of global progressbar update
	// 	progressListener = progressListener.andThen((pos, of) -> out.dispatchLineCount());
	// 	long start = System.currentTimeMillis();
	// 	try (BufferedReader reader = Files.newBufferedReader(_file, _charset)) {
	// 		int lineNumber = 0;
	// 		String line;
	// 		while (running.getAsBoolean() && (line = reader.readLine()) != null) {
	// 			predicate.verify(lineNumber, line);
	// 			progressListener.onProgressChanged(lineNumber++, _lineCount);
	// 		}
	// 	} catch (IOException e) {
	// 		throw new ExecutionException(e);
	// 	} finally {
	// 		predicate.end();
	// 		progressListener.onProgressChanged(_lineCount, _lineCount);
	// 		System.out.println("Search finished in " + (System.currentTimeMillis() - start) + "ms");
	// 	}
	// }

	@Override
	public synchronized String getText(int line) throws ExecutionException, InterruptedException {
		if (_lines == null || line < _fromLine || line >= (_fromLine + _lines.length)) {
			Map.Entry<Integer, IndexData> fromLineE = _index.floorEntry(line);
			IndexData indexData = fromLineE.getValue();
			int fromLine = fromLineE.getKey();
			if (indexData.data == null || (_lines = indexData.data.get()) == null) {
				//				System.out.println("MISS");
				Integer toLine = _index.higherKey(line);
				if (toLine == null)
					toLine = _lineCount;
				try {
					_lines = loadPage(fromLine, toLine, indexData.startPos);
					_fromLine = fromLine;
					indexData.data = new WeakReference<>(_lines);
				} catch (ClosedByInterruptException ignored) {
					return "";
				} catch (IOException e) {
					e.printStackTrace(System.err);
					throw new UncheckedIOException(e);
				}
			} else {
				//				System.out.println("HIT");
				_fromLine = fromLine;
			}
		}
		int i = line - _fromLine;
		return i >= 0 && i < _lines.length ? _lines[i] : "";
	}

	@Override
	public Unsubscribable requestLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_indexing.get();
		return _lineCount;
	}

	public void setIndexingListener(@Nullable ProgressListener indexingListener) {
		_indexingListener = indexingListener == null ? ProgressListener.NOP : indexingListener;
		if (_indexing.isDone())
			_indexingListener.onProgressChanged(100, 100);
	}

	@Override
	public String toString() {
		return _file.toString();
	}

	@Override
	public void onClose() {
		_indexing.cancel(true);
	}

	@Override
	public boolean isIndexing() {
		return !_indexing.isDone();
	}
}
