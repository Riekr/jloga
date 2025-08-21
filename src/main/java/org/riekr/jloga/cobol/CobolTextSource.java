package org.riekr.jloga.cobol;

import static java.util.stream.Collectors.joining;

import javax.swing.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.VolatileTextSource;

import net.sf.JRecord.Common.FieldDetail;
import net.sf.JRecord.Common.IFieldDetail;
import net.sf.JRecord.Common.IFileStructureConstants;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.Details.fieldValue.IFieldValue;
import net.sf.JRecord.External.CopybookLoader;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.JRecordInterface1;
import net.sf.JRecord.Numeric.ICopybookDialects;
import net.sf.JRecord.def.IO.builders.ICobolIOBuilder;
import net.sf.cb2xml.def.Cb2xmlConstants;

public class CobolTextSource extends VolatileTextSource {

	private static final String _DELIM = "|";

	private final String _copybook;
	private final String _datafile;

	public CobolTextSource(
			@NotNull String copybook,
			@NotNull String datafile,
			@Nullable Integer copybookFileFormat,
			@Nullable String font,
			@Nullable Integer fileOrganization,
			@Nullable Integer splitCopybook,
			@Nullable Integer dialect
	) throws IOException {
		super(new LinkedList<>());
		_copybook = copybook;
		_datafile = datafile;

		if (copybookFileFormat == null)
			copybookFileFormat = Cb2xmlConstants.USE_STANDARD_COLUMNS;
		if (font == null || font.isBlank())
			font = "CP037";
		if (fileOrganization == null)
			fileOrganization = IFileStructureConstants.IO_FIXED_LENGTH;
		if (splitCopybook == null)
			splitCopybook = CopybookLoader.SPLIT_NONE;
		if (dialect == null)
			dialect = ICopybookDialects.FMT_MAINFRAME;

		final ICobolIOBuilder builder = JRecordInterface1.COBOL.newIOBuilder(copybook)
				.setFileOrganization(fileOrganization)
				.setSplitCopybook(splitCopybook)
				.setDialect(dialect)
				.setCopybookFileFormat(copybookFileFormat)
				.setFont(font);

		final List<FieldDetail> header = builder.getLayout().getRecordsAsList().stream()
				.flatMap(l -> l.getFields().stream())
				.toList();
		_data.add(header.stream().map(IFieldDetail::getName).collect(joining(_DELIM)));

		AbstractLineReader reader = null;
		try {
			reader = builder.newReader(datafile);

			AbstractLine aline;
			while ((aline = reader.read()) != null) {
				_data.add(header.stream()
						.map(aline::getFieldValue)
						.map(IFieldValue::asString)
						.collect(joining(_DELIM)));
			}
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	@Override
	public List<JLabel> describe() {
		return List.of(
				new JLabel("Copybook: " + _copybook),
				new JLabel("Datafile: " + _datafile)
		);
	}

	@Override
	public boolean mayHaveTabularData() {
		return true;
	}
}
