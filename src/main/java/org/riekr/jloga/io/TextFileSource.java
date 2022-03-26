package org.riekr.jloga.io;

import static java.nio.file.StandardOpenOption.READ;
import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.misc.MutableInt;
import org.riekr.jloga.misc.MutableLong;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.SearchPredicate;

import javax.swing.*;

public class TextFileSource implements TextSource {

	static final class IndexData {
		final long startPos;
		WeakReference<String[]> data;

		public IndexData(long startPos) {
			this.startPos = startPos;
		}
	}

	private final int     _pageSize = Preferences.PAGE_SIZE.get();
	private final Path    _file;
	private       Charset _charset;

	private final Future<?>                   _indexing;
	private       TreeMap<Integer, IndexData> _index;

	private       int                 _lineCount;
	private final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();

	private final Set<Runnable> _indexChangeListeners = new CopyOnWriteArraySet<>();

	private int      _fromLine = Integer.MAX_VALUE;
	private String[] _lines;

	private final AtomicReference<Future<?>> _textRequest = new AtomicReference<>();

	public TextFileSource(@NotNull Path file, @NotNull Charset charset, @NotNull ProgressListener indexingListener, @NotNull Runnable closer) {
		_file = file;
		_charset = charset;
		_indexing = IO_EXECUTOR.submit(() -> {
			System.out.println("Indexing " + _file);
			final long start = System.currentTimeMillis();
			_lineCount = 0;
			_lineCountSubject.next(0);
			_index = new TreeMap<>();
			_index.put(0, new IndexData(0));
			ByteBuffer byteBuffer = ByteBuffer.allocate(_pageSize);
			CharBuffer charBuffer = CharBuffer.allocate(_pageSize);
			CharsetDecoder decoder = _charset.newDecoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			CharsetEncoder encoder = _charset.newEncoder();
			try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
				final long totalSize = fileChannel.size();
				final MutableLong pos = new MutableLong();
				ScheduledFuture<?> updateTask = MONITOR_EXECUTOR.scheduleWithFixedDelay(() -> {
					indexingListener.onProgressChanged(pos.value, totalSize);
					_indexChangeListeners.forEach(Runnable::run);
					_lineCountSubject.next(_lineCount);
				}, 0, 200, TimeUnit.MILLISECONDS);
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
						pos.value = fileChannel.position();
						_index.put(_lineCount, new IndexData(pos.value - encoder.encode(charBuffer.reset()).limit()));
						charBuffer.flip();
						byteBuffer.flip();
					}
				} finally {
					updateTask.cancel(false);
					_lineCountSubject.last(_lineCount);
					indexingListener.onProgressChanged(totalSize, totalSize);
					_indexChangeListeners.forEach(Runnable::run);
					_indexChangeListeners.clear();
				}
				System.out.println("Indexed " + _file + ' ' + _lineCount + " lines in " + (System.currentTimeMillis() - start) + "ms (" + _index.size() + " pages)");
			} catch (ClosedByInterruptException ignored) {
				System.out.println("Indexing cancelled");
			} catch (IOException | InvalidMarkException e) {
				EventQueue.invokeLater(() -> {
					String msg = e.getLocalizedMessage();
					if (msg == null)
						msg = "";
					else if (!(msg = msg.trim()).isEmpty())
						msg += '\n';
					JOptionPane.showMessageDialog(Main.getMain(), msg + "\nWrong charset?", "Indexing error", JOptionPane.ERROR_MESSAGE);
					closer.run();
				});
				e.printStackTrace(System.err);
				throw new IndexingException("Error indexing " + file, e);
			}
		});
	}

	@Override
	public void enqueueTextRequest(Runnable task) {
		Future<?> oldFuture = _textRequest.getAndSet(AUX_EXECUTOR.submit(task));
		if (oldFuture != null)
			oldFuture.cancel(false);
	}

	@Override
	public void requestText(int fromLine, int count, Consumer<Reader> consumer) {
		if (_indexing.isDone()) {
			enqueueTextRequest(() -> {
				StringsReader reader = new StringsReader(getText(fromLine, Math.min(_lineCount - fromLine, count)));
				EventQueue.invokeLater(() -> consumer.accept(reader));
			});
		} else {
			_indexChangeListeners.add(new Runnable() {
				@Override
				public void run() {
					try {
						if (_index.floorKey(fromLine) != null && _index.ceilingKey(fromLine + count) != null) {
							_indexChangeListeners.remove(this);
							StringsReader reader = new StringsReader(getText(fromLine, Math.min(_lineCount - fromLine, count)));
							EventQueue.invokeLater(() -> consumer.accept(reader));
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

	@Override
	public Iterator<String> iterator(int fromInclusive, int toExclusive) {
		return new RangeIterator<>(fromInclusive, Math.min(_lineCount, toExclusive), this::getText);
	}

	private void loadPage(long pos, String[] lines) throws IOException {
		try (FileChannel fileChannel = FileChannel.open(_file, READ)) {
			fileChannel.position(pos);
			try (BufferedReader br = new BufferedReader(Channels.newReader(fileChannel, _charset), _pageSize)) {
				for (int i = 0; i < lines.length; i++)
					lines[i] = br.readLine();
			}
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
						_charset = charset;
						loadPage(pos, lines);
						System.out.println("Changed charset from " + orig + " to " + _charset);
						Preferences.CHARSET.set(_charset);
						EventQueue.invokeLater(this::showCharsetWarning);
						return lines;
					} catch (MalformedInputException ignored) {}
				}
				_charset = orig;
			}
			throw e;
		}
	}

	private void showCharsetWarning() {
		JOptionPane.showMessageDialog(Main.getMain(), "Charset has been automatically changed to " + _charset + ",\n"
						+ "if text is corrupted please select another charset and reopen the file.\n"
						+ "You can disable charset auto detection in preferences.",
				"Charset changed",
				JOptionPane.WARNING_MESSAGE
		);
	}

	@Override
	public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		long start = System.currentTimeMillis();
		final MutableInt lineNumber = new MutableInt();
		ScheduledFuture<?> updateTask = MONITOR_EXECUTOR.scheduleWithFixedDelay(() -> {
			progressListener.onProgressChanged(lineNumber.value, _lineCount);
			out.dispatchLineCount();
		}, 0, 200, TimeUnit.MILLISECONDS);
		try {
			Semaphore semaphore = new Semaphore(0);
			_indexChangeListeners.add(() -> semaphore.release(1));
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
			predicate.end();
			updateTask.cancel(false);
			progressListener.onProgressChanged(_lineCount, _lineCount);
		}
		System.out.println("Search finished in " + (System.currentTimeMillis() - start) + "ms");
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
		switch (count) {
			case 0:
				return EMPTY_STRINGS;
			case 1:
				return new String[]{getText(fromLine)};
		}
		String[] res;
		synchronized (this) {
			count = Math.min(count, _lineCount - fromLine);
			int endLine = fromLine + count;
			res = new String[count + 1];
			if (_lines == null || fromLine < _fromLine || res.length > _lines.length || fromLine >= (_fromLine + _lines.length)) {
				Map.Entry<Integer, IndexData> entry = _index.floorEntry(fromLine);
				do {
					IndexData indexData = entry.getValue();
					int pageFromLine = entry.getKey();
					if (indexData.data == null || (_lines = indexData.data.get()) == null) {
						//				System.out.println("MISS");
						entry = _index.higherEntry(entry.getKey());
						int toLine;
						if (entry == null)
							toLine = _lineCount;
						else {
							toLine = entry.getKey();
							if (toLine >= endLine)
								entry = null;
						}
						try {
							_lines = loadPage(pageFromLine, toLine, indexData.startPos);
							_fromLine = pageFromLine;
							indexData.data = new WeakReference<>(_lines);
						} catch (ClosedByInterruptException ignored) {
							return EMPTY_STRINGS;
						} catch (IOException e) {
							e.printStackTrace(System.err);
							throw new UncheckedIOException(e);
						}
					} else {
						//				System.out.println("HIT");
						_fromLine = pageFromLine;
						if (_fromLine + _lines.length >= endLine)
							entry = null;
					}
					int from = fromLine - _fromLine;
					int len = Math.min(res.length, _lines.length - from);
					System.arraycopy(_lines, from, res, 0, len);
					fromLine += len;
				} while (entry != null);
			} else {
				int from = fromLine - _fromLine;
				System.arraycopy(_lines, from, res, 0, Math.min(res.length, _lines.length - from));
			}
		}
		if (res[count] == null)
			res[count] = "";
		return res;
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return _lineCountSubject.once(Observer.async(consumer::accept));
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(Observer.async(consumer::accept));
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
	public void onClose() {
		_indexing.cancel(true);
	}

	@Override
	public boolean isIndexing() {
		return !_indexing.isDone();
	}
}
