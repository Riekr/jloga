package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
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
		_index = new TreeMap<>();
		_index.put(0, new IndexData(0));
		ByteBuffer byteBuffer = ByteBuffer.allocate(PAGE_SIZE);
		CharBuffer charBuffer = CharBuffer.allocate(PAGE_SIZE);
		CharsetDecoder decoder = _charset.newDecoder();
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			long totalSize = fileChannel.size();
			try {
				while (fileChannel.read(byteBuffer) > 0) {
					CoderResult res = decoder.decode(byteBuffer.flip(), charBuffer, false);
					if (res != null && res.isError())
						System.err.println(res);
					charBuffer.flip();
					while (charBuffer.hasRemaining()) {
						char ch = charBuffer.get();
						if (ch == '\n') {
							_lineCount++;
							charBuffer.mark();
						}
					}
					long pos = fileChannel.position();
					_index.put(_lineCount, new IndexData(pos - _charset.encode(charBuffer.reset()).limit()));
					_indexingListener.onProgressChanged(pos, totalSize);
					_indexChangeListeners.forEach(Runnable::run);
					charBuffer.flip();
					byteBuffer.flip();
				}
			} finally {
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
	public void requestText(int fromLine, int count, Consumer<String> consumer) {
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
							StringBuilder buf = new StringBuilder(32768);
							for (int line = fromLine; line < toLinePlus1; line++)
								buf.append(getText(line)).append('\n');
							EventQueue.invokeLater(() -> consumer.accept(buf.toString()));
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
	public String getText(int line) throws ExecutionException, InterruptedException {
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
		return i < _lines.length ? _lines[i] : "";
	}

	@Override
	public void requestLineCount(IntConsumer consumer) {
		if (_indexing.isDone())
			TextSource.super.requestLineCount(consumer);
		else
			_indexChangeListeners.add(() -> EventQueue.invokeLater(() -> consumer.accept(_lineCount)));
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