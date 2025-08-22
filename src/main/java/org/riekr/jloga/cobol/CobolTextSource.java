package org.riekr.jloga.cobol;

import static java.util.stream.Collectors.joining;
import static org.riekr.jloga.utils.ContextMenu.addActionCopy;
import static org.riekr.jloga.utils.ContextMenu.addActionOpenInFileManager;
import static org.riekr.jloga.utils.FileUtils.sizeToString;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.FileProgressInputStream;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.IndexingException;
import org.riekr.jloga.io.ProgressListener;
import org.riekr.jloga.io.VolatileTextSource;
import org.riekr.jloga.pmem.PagedJavaList;
import org.riekr.jloga.search.SearchPredicate;

import net.sf.JRecord.Common.FieldDetail;
import net.sf.JRecord.Common.IFieldDetail;
import net.sf.JRecord.Common.IFileStructureConstants;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.Details.LayoutDetail;
import net.sf.JRecord.Details.RecordDecider;
import net.sf.JRecord.Details.RecordDetail;
import net.sf.JRecord.Details.fieldValue.IFieldValue;
import net.sf.JRecord.External.CopybookLoader;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.JRecordInterface1;
import net.sf.JRecord.Numeric.ICopybookDialects;
import net.sf.JRecord.def.IO.builders.ICobolIOBuilder;
import net.sf.cb2xml.def.Cb2xmlConstants;

public class CobolTextSource extends VolatileTextSource {

	private static final String _DELIM = "|";

	private final @NotNull String  _copybook;
	private final @NotNull String  _datafile;
	private final @NotNull Integer _copybookFileFormat;
	private final @NotNull String  _font;
	private final @NotNull Integer _fileOrganization;
	private final @NotNull Integer _splitCopybook;
	private final @NotNull Integer _dialect;

	private Future<?> _indexing;

	public CobolTextSource(
			@NotNull String copybook,
			@NotNull String datafile,
			@Nullable Integer copybookFileFormat,
			@Nullable String font,
			@Nullable Integer fileOrganization,
			@Nullable Integer splitCopybook,
			@Nullable Integer dialect,
			@Nullable Supplier<ProgressListener> progressListenerSupplier
	) {
		super(PagedJavaList.ofStrings());
		_copybook = copybook;
		_datafile = datafile;
		_copybookFileFormat = copybookFileFormat == null ? Cb2xmlConstants.USE_STANDARD_COLUMNS : copybookFileFormat;
		_font = font == null || font.isBlank() ? "CP037" : font;
		_fileOrganization = fileOrganization == null ? IFileStructureConstants.IO_FIXED_LENGTH : fileOrganization;
		_splitCopybook = splitCopybook == null ? CopybookLoader.SPLIT_NONE : splitCopybook;
		_dialect = dialect == null ? ICopybookDialects.FMT_MAINFRAME : dialect;
		_indexing = defaultAsyncIO(() -> reload(progressListenerSupplier == null ? () -> ProgressListener.NOP : progressListenerSupplier));
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

	@Override
	public void close() {
		_indexing.cancel(true);
	}

	@Override
	public boolean isIndexing() {
		return !_indexing.isDone();
	}

	@Override
	public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		_indexing.get();
		super.search(predicate, out, progressListener, running);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_indexing.get();
		return super.getLineCount();
	}

	@Override
	public void reload(Supplier<ProgressListener> progressListenerSupplier) {
		try {
			clear();

			final ICobolIOBuilder builder = JRecordInterface1.COBOL.newIOBuilder(_copybook)
					.setFileOrganization(_fileOrganization)
					.setSplitCopybook(_splitCopybook)
					.setDialect(_dialect)
					.setCopybookFileFormat(_copybookFileFormat)
					.setFont(_font);

			// final Map<Integer, RecordDetail> records = new LinkedHashMap<>();
			final Map<String, IFieldDetail> header = new LinkedHashMap<>();
			final LayoutDetail layout = builder.getLayout();
			for (final RecordDetail recordDetail : layout.getRecordsAsList()) {
				System.out.println("Record: " + recordDetail.getRecordName() + '/' + recordDetail.getParentRecordIndex());
				// records.put(recordDetail.getSourceIndex(), recordDetail);
				for (final FieldDetail field : recordDetail.getFields()) {
					if (header.put(field.getName(), field) != null)
						System.err.println("Duplicate field: " + field.getName());
				}
			}
			RecordDecider decider = layout.getDecider();
			if (decider == null)
				add(String.join(_DELIM, header.keySet()));
			else
				add("R" + _DELIM + String.join(_DELIM, header.keySet()));

			AbstractLineReader reader = null;
			try (FileProgressInputStream fis = new FileProgressInputStream(progressListenerSupplier.get(), _datafile)) {
				reader = builder.newReader(fis);

				AbstractLine line;
				while ((line = reader.read()) != null) {
					final Stream<String> values = header.values().stream()
							.map(line::getFieldValue)
							.map(IFieldValue::asString);
					if (decider == null)
						add(values.collect(joining(_DELIM)));
					else {
						add(Stream.concat(
								IntStream.of(decider.getPreferedIndex(line)).mapToObj(Integer::toString),
								values
						).collect(joining(_DELIM)));
					}
				}
			} finally {
				if (reader != null)
					reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new IndexingException("Error reloading " + _datafile, e);
		}
	}

	@Override
	public List<JLabel> describe() {

		final JLabel copybookLabel = new JLabel("Copybook: " + _copybook);
		final File copybookFile = new File(_copybook);
		addActionOpenInFileManager(copybookLabel, copybookFile);
		addActionCopy(copybookLabel, "Copy \"" + copybookFile.getName() + "\" absolute path", copybookFile::getAbsolutePath);

		final JLabel datafileLabel = new JLabel("Datafile: " + _datafile);
		final File datafileFile = new File(_datafile);
		addActionOpenInFileManager(datafileLabel, datafileFile);
		addActionCopy(datafileLabel, "Copy \"" + datafileFile.getName() + "\" absolute path", datafileFile::getAbsolutePath);
		datafileLabel.setToolTipText(sizeToString(datafileFile));

		return List.of(copybookLabel, datafileLabel);
	}

	@Override
	public boolean mayHaveTabularData() {
		return true;
	}
}
