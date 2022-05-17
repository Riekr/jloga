package org.riekr.jloga.io;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNullElse;
import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS;
import static org.riekr.jloga.utils.AsyncOperations.asyncTask;
import static org.riekr.jloga.utils.AsyncOperations.monitorProgress;
import static org.riekr.jloga.utils.FileUtils.getFileCreationTime;
import static org.riekr.jloga.utils.PopupUtils.popupError;
import static org.riekr.jloga.utils.PopupUtils.popupWarning;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.misc.MutableInt;
import org.riekr.jloga.misc.MutableLong;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.utils.AsyncOperations;
import org.riekr.jloga.utils.CancellableFuture;
import org.riekr.jloga.utils.TextUtils;

public class TextFileSource implements TextSource {

	interface IndexTask {
		boolean run();
	}

	static final class IndexData {
		final long startPos;
		WeakReference<String[]> data;

		public IndexData(long startPos) {
			this.startPos = startPos;
		}
	}

	private final int            _pageSize = Preferences.PAGE_SIZE.get();
	private final Path           _file;
	private       Charset        _charset;
	private       CharsetDecoder _charsetDecoder; // for loadPage only

	private Future<?>                   _indexing;
	private TreeMap<Integer, IndexData> _index;

	private       int                 _lineCount;
	private final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();
	private final MutableLong         _lastPos          = new MutableLong();

	private final Set<IndexTask> _indexChangeListeners = newSetFromMap(new ConcurrentHashMap<>());

	private int      _fromLine = Integer.MAX_VALUE;
	private String[] _lines;

	private final ByteBuffer _fingerPrint = ByteBuffer.allocate(100);
	private       FileTime   _fileCreationTime;

	public TextFileSource(@NotNull Path file, @NotNull Charset charset, @NotNull ProgressListener indexingListener, @NotNull Runnable closer) {
		_file = file;
		setCharset(charset);
		_indexing = AsyncOperations.INDEX.submit(file, () -> {
			try {
				reindex(indexingListener);
			} catch (IndexingException e) {
				closer.run();
				throw e;
			}
		});
	}

	public void setCharset(@NotNull Charset charset) {
		_charset = charset;
		_charsetDecoder = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	private void reindex(@NotNull ProgressListener indexingListener) {
		System.out.println("Indexing " + _file);
		final long start = currentTimeMillis();
		_lineCount = 0;
		_lineCountSubject.next(0);
		_index = new TreeMap<>();
		_index.put(0, new IndexData(0));
		_lastPos.value = 0;
		_lines = null;
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			final long totalSize = fileChannel.size();
			ScheduledFuture<?> updateTask = monitorProgress(_lastPos, totalSize, indexingListener.andThen(() -> _lineCountSubject.next(_lineCount)));
			try {
				scanFile(fileChannel);
			} finally {
				finishIndexing(updateTask, indexingListener);
			}
			System.out.println("Indexed " + _file + ' ' + _lineCount + " lines in " + (currentTimeMillis() - start) + "ms (" + _index.size() + " pages)");
		} catch (ClosedByInterruptException ignored) {
			System.out.println("Indexing cancelled");
		} catch (IOException | InvalidMarkException e) {
			popupError("Wrong charset?", "Indexing error", e);
			throw new IndexingException("Error indexing " + _file, e);
		}
	}

	private void scanFile(FileChannel fileChannel) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(_pageSize);
		long read = fileChannel.read(byteBuffer);
		if (read > 0) {
			CharBuffer charBuffer = CharBuffer.allocate(_pageSize);
			CharsetDecoder decoder = _charset.newDecoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			CharsetEncoder encoder = _charset.newEncoder();
			char lastChar = 0;
			long lastPos = read;
			for (; ; ) {
				decoder.decode(byteBuffer.flip(), charBuffer, false);
				charBuffer.flip();
				if (charBuffer.hasRemaining()) {
					int lineCount = _lineCount;
					do {
						final char curr = charBuffer.get();
						switch (curr) {
							// from java.io.BufferedReader.readLine():
							// "A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'), a carriage return followed immediately by a line feed"
							case '\n':
								if (lastChar == '\r')
									break;
								//noinspection fallthrough
							case '\r':
								lineCount++;
								charBuffer.mark();
								break;
						}
						lastChar = curr;
					} while (charBuffer.hasRemaining());
					_lineCount = lineCount;
				}
				// finalize indexed page
				_lastPos.value = lastPos;
				_index.put(_lineCount, new IndexData(lastPos - encoder.encode(charBuffer.reset()).limit()));
				byteBuffer.flip();
				read = fileChannel.read(byteBuffer);
				lastPos += read;
				if (read <= 0)
					break;
				_indexChangeListeners.removeIf(IndexTask::run);
				charBuffer.flip();
			}
			// if the last char is not a new-line adjust the line count
			if (lastChar != '\n') {
				IndexData t = _index.remove(_lineCount);
				_index.put(_lineCount + 1, t);
			}
		}
	}

	private void finishIndexing(Future<?> updateTask, ProgressListener progressListener) {
		fingerprint();
		updateTask.cancel(false);
		_lineCountSubject.last(_lineCount);
		progressListener.onProgressChanged(_lastPos.value, _lastPos.value);
		_indexChangeListeners.forEach(IndexTask::run);
		_indexChangeListeners.clear();
	}

	@Override
	public boolean supportsReload() {
		return true;
	}

	public Future<?> requestReload(Supplier<ProgressListener> progressListenerSupplier) {
		if (_indexing == null || _indexing.isDone())
			_indexing = defaultAsyncIO(() -> reload(progressListenerSupplier));
		return _indexing;
	}

	private void fingerprint() {
		_fingerPrint.clear();
		try {
			_fileCreationTime = getFileCreationTime(_file);
			try (FileChannel fc = FileChannel.open(_file, READ)) {
				fc.read(_fingerPrint);
				_fingerPrint.flip();
			}
		} catch (Throwable e) {
			System.err.println("Unable to fingerprint " + _file);
			e.printStackTrace(System.err);
			_fingerPrint.clear();
			_fingerPrint.limit(0);
		}
	}

	private boolean isSameFile() {
		try {
			if (_fileCreationTime != null) {
				FileTime fileCreationTime = getFileCreationTime(_file);
				// some file system may not update or support creation, or contents simply may have changed,
				// so I will check fingerprint if creation time did not change
				if (!_fileCreationTime.equals(fileCreationTime))
					return false;
			}
			ByteBuffer buf = ByteBuffer.allocate(_fingerPrint.capacity());
			try (FileChannel fc = FileChannel.open(_file, READ)) {
				fc.read(buf);
				buf.flip();
			}
			return _fingerPrint.equals(buf);
		} catch (Throwable e) {
			System.err.println("Unable to check fingerprint of " + _file);
			e.printStackTrace(System.err);
			return false;
		}
	}

	@Override
	public void reload(Supplier<ProgressListener> progressListenerSupplier) {
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			final long totalSize = fileChannel.size();
			if (_lastPos.value > totalSize || !isSameFile()) {
				System.out.println("Reindex of " + _file);
				reindex(progressListenerSupplier.get());
			} else if (_lastPos.value < totalSize) {
				System.out.println("Updating " + _file);
				final long start = currentTimeMillis();
				final int initialLineCount = _lineCount;
				final int initialPageCount = _index.size();
				final int lastLine = _index.lastKey();
				IndexData lastIndexPart = _index.remove(lastLine);
				fileChannel.position(lastIndexPart.startPos);
				_lastPos.value = lastIndexPart.startPos;
				ProgressListener progressListener = progressListenerSupplier.get();
				ScheduledFuture<?> updateTask = monitorProgress(_lastPos, totalSize, progressListener.andThen(() -> {
					_indexChangeListeners.removeIf(IndexTask::run);
					_lineCountSubject.next(_lineCount);
				}));
				try {
					scanFile(fileChannel);
					// user may have hit reload due to a bug so all caches must be invalidated
					// even a change in the file before the previous indexing will be detected this way
					synchronized (this) {
						_index.values().forEach((id) -> id.data = null);
						_lines = null;
					}
				} finally {
					finishIndexing(updateTask, progressListener);
				}
				System.out.println("Reloaded " + _file + " +" + (_lineCount - initialLineCount) + " lines in " + (currentTimeMillis() - start) + "ms (+" + (_index.size() + initialPageCount) + " pages)");
			} else {
				System.out.println("Skipped reloaded of " + _file + " (file did not change)");
			}
		} catch (ClosedByInterruptException ignored) {
			System.out.println("Reloading cancelled");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new IndexingException("Error reloading " + _file, e);
		}
	}

	@Override
	public Future<?> defaultAsyncIO(Runnable task) {
		return AsyncOperations.IO.submit(_file, task);
	}

	@Override
	public Future<?> requestText(int fromLine, int count, Consumer<String> consumer) {
		if (_index != null && _index.ceilingKey(fromLine + count) != null) {
			Map.Entry<Integer, IndexData> entry = _index.floorEntry(fromLine);
			if (entry != null) {
				IndexData indexData = entry.getValue();
				String[] lines;
				if (indexData.data != null && (lines = indexData.data.get()) != null) {
					return asyncTask(() -> {
						String text = TextUtils.toString(fromLine - entry.getKey(), Math.min(count, _lineCount), lines, count);
						EventQueue.invokeLater(() -> consumer.accept(text));
					});
				}
			}
		}

		if (_indexing.isDone()) {
			return defaultAsyncIO(() -> {
				String text = TextUtils.toString(getText(fromLine, Math.min(_lineCount - fromLine, count)), count);
				EventQueue.invokeLater(() -> consumer.accept(text));
			});
		}

		IndexTask task = () -> {
			try {
				if (_index.floorKey(fromLine) != null && _index.ceilingKey(fromLine + count) != null) {
					String text = TextUtils.toString(getText(fromLine, Math.min(_lineCount - fromLine, count)), count);
					EventQueue.invokeLater(() -> consumer.accept(text));
					return true;
				}
				return false;
			} catch (Throwable e) {
				consumer.accept(e.getLocalizedMessage());
				if (e instanceof RuntimeException)
					throw (RuntimeException)e;
				throw new RuntimeException(e);
			}
		};
		_indexChangeListeners.add(task);
		return new CancellableFuture(() -> _indexChangeListeners.remove(task));
	}

	@Override
	public Iterator<String> iterator(int fromInclusive, int toExclusive) {
		return new RangeIterator<>(fromInclusive, Math.min(_lineCount, toExclusive), this::getText);
	}

	private void loadPage(long pos, String[] lines) throws IOException {
		_charsetDecoder.reset();
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			fileChannel.position(pos);
			BufferedReader br = new BufferedReader(Channels.newReader(fileChannel, _charsetDecoder, _pageSize), 2048);
			for (int i = 0; i < lines.length; i++)
				lines[i] = br.readLine();
		}
	}

	private String[] loadPage(int fromLine, int toLine, long pos) throws IOException {
		String[] lines = new String[toLine - fromLine];
		try {
			loadPage(pos, lines);
			return lines;
		} catch (MalformedInputException e) {
			if (Preferences.CHARSET_DETECT.get()) {
				// cycle all but one (the first)
				Charset orig = _charset;
				for (Charset charset : Charsets.nextOf(orig)) {
					try {
						setCharset(charset);
						loadPage(pos, lines);
						System.out.println("Changed charset from " + orig + " to " + _charset);
						Preferences.CHARSET.set(_charset);
						popupWarning("Charset has been automatically changed to " + _charset + ",\n"
										+ "if text is corrupted please select correct charset and reopen the file.\n"
										+ "You can disable charset auto detection in preferences.",
								"Charset changed");
						return lines;
					} catch (MalformedInputException ignored) {}
				}
				setCharset(orig);
			}
			throw e;
		}
	}

	@Override
	public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		long start = currentTimeMillis();
		final MutableInt lineNumber = new MutableInt();
		ScheduledFuture<?> updateTask = monitorProgress(lineNumber, _lineCount, progressListener.andThen(out::dispatchLineCount));
		try {
			Semaphore semaphore = new Semaphore(0);
			_indexChangeListeners.add(() -> {
				semaphore.release(1);
				return false;
			});
			BooleanSupplier continueCond = () -> {
				if (!_indexing.isDone()) {
					// wait for at least 1 page to be loaded
					semaphore.acquireUninterruptibly();
					return running.getAsBoolean();
				}
				return false;
			};
			int fromLine = 0;
			do {
				if (_lineCount > 0) {
					// System.out.println(_lineCount);
					for (lineNumber.value = fromLine; lineNumber.value < _lineCount && running.getAsBoolean(); lineNumber.value++)
						predicate.verify(lineNumber.value, getText(lineNumber.value));
					fromLine = lineNumber.value;
				}
			} while (continueCond.getAsBoolean());
		} finally {
			predicate.end(!running.getAsBoolean());
			updateTask.cancel(false);
			progressListener.onProgressChanged(_lineCount, _lineCount);
		}
		System.out.println("Search finished in " + (currentTimeMillis() - start) + "ms");
	}

	/**
	 * Read a single line changing loaded page if needed
	 */
	@Override
	public synchronized String getText(int line) {
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
	public String[] getText(int fromLine, int count) {
		// This method exists to make sure all data is read from disk without interruptions.
		// Reading line-by-line may cause loaded page to be changed multiple times.
		final int currentLineCount = _lineCount;
		if ((count = Math.min(count, currentLineCount - fromLine)) <= 0)
			return EMPTY_STRINGS;
		final String[] res = new String[count];
		final int endLineExcl = fromLine + count;
		Set<Map.Entry<Integer, IndexData>> list = _index.subMap(_index.floorKey(fromLine), true, endLineExcl, false).entrySet();
		int resStart = 0;
		synchronized (this) {
			for (Map.Entry<Integer, IndexData> entry : list) {
				int pageStart = entry.getKey();
				if (_lines == null || pageStart < _fromLine || pageStart >= (_fromLine + _lines.length)) {
					IndexData indexData = entry.getValue();
					int pageEnd = requireNonNullElse(_index.higherKey(pageStart), _lineCount);
					if (indexData.data == null || (_lines = indexData.data.get()) == null) {
						try {
							_lines = loadPage(pageStart, pageEnd, indexData.startPos);
							indexData.data = new WeakReference<>(_lines);
						} catch (ClosedByInterruptException ignored) {
							return EMPTY_STRINGS;
						} catch (IOException e) {
							e.printStackTrace(System.err);
							throw new UncheckedIOException(e);
						}
					}
					_fromLine = pageStart;
				}
				int from = fromLine - pageStart;
				int len = Math.min(res.length - resStart, _lines.length - from);
				System.arraycopy(_lines, from, res, resStart, len);
				resStart += len;
				fromLine += len;
			}
		}
		return res;
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return _lineCountSubject.once(Observer.async(consumer::accept));
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_indexing.get();
		return _lineCount;
	}

	@Override
	public String toString() {
		return _file.toString();
	}

	@Override
	public void close() {
		_indexing.cancel(true);
	}

	@Override
	public boolean isIndexing() {
		return !_indexing.isDone();
	}

	public Path getFile() {
		return _file;
	}
}
