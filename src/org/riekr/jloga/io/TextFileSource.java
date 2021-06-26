package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.charset.*;
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

import static java.nio.file.StandardOpenOption.READ;

public class TextFileSource implements TextSource {

	private static final int PAGE_SIZE = 1024 * 1024 * 2; // 2MB

	private final Path _file;
	private final Charset _charset;

	private Future<?> _indexing;
	private NavigableMap<Integer, IndexData> _index;

	private int _lineCount;
	private final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();

	private @NotNull ProgressListener _indexingListener = ProgressListener.NOP;
	private final Set<Runnable> _indexChangeListeners = new CopyOnWriteArraySet<>();

	private int _fromLine = Integer.MAX_VALUE;
	private String[] _lines;

	public TextFileSource(Path file, Charset charset) {
		_file = file;
		_charset = charset;
		reindex();
	}

	public synchronized void reindex() {
		if (_indexing != null && !_indexing.isDone())
			_indexing.cancel(true);
		_indexing = EXECUTOR.submit(this::getIndex);
	}

	private void getIndex() {
		System.out.println("Reindexing " + _file);
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
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		System.out.println("Indexed " + _file + ' ' + _lineCount + " lines in " + (System.currentTimeMillis() - start) + "ms");
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
					} catch (ExecutionException | InterruptedException ignored) {
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

	@Override
	public synchronized String getText(int line) throws ExecutionException, InterruptedException {
		if (_lines == null || line < _fromLine || line >= (_fromLine + _lines.length)) {
			Map.Entry<Integer, IndexData> fromLineE = _index.floorEntry(line);
			IndexData indexData = fromLineE.getValue();
			int fromLine = fromLineE.getKey();
			if (indexData.data == null || (_lines = indexData.data.get()) == null) {
//				System.out.println("MISS");
				Integer toLine = _index.ceilingKey(line + 1);
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
					return "";
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
		return _lineCountSubject.subscribe(consumer);
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
}
