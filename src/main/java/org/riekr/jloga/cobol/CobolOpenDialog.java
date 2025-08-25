package org.riekr.jloga.cobol;

import static org.riekr.jloga.utils.FileUtils.DialogType.OPEN;
import static org.riekr.jloga.utils.KeyUtils.ESC;
import static org.riekr.jloga.utils.KeyUtils.addKeyStrokeAction;
import static org.riekr.jloga.utils.PopupUtils.popupError;
import static org.riekr.jloga.utils.UIUtils.ICO_CANCEL;
import static org.riekr.jloga.utils.UIUtils.ICO_OPEN;
import static org.riekr.jloga.utils.UIUtils.atStart;
import static org.riekr.jloga.utils.UIUtils.horizontalBox;
import static org.riekr.jloga.utils.UIUtils.newButton;
import static org.riekr.jloga.utils.UIUtils.setSingleFileDropListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

import org.riekr.jloga.ui.ComboBoxWithLabelAndTooltips;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.MainPanel;
import org.riekr.jloga.utils.FileUtils;
import org.riekr.jloga.utils.UIUtils;

import net.sf.JRecord.Common.IFileStructureConstants;
import net.sf.JRecord.Numeric.ICopybookDialects;
import net.sf.JRecord.Option.ICobolSplitOptions;
import net.sf.cb2xml.def.Cb2xmlConstants;

public class CobolOpenDialog extends JDialog {

	private final MainPanel            _main;
	private final MRUTextCombo<String> _copybook = MRUTextCombo.newMRUTextCombo("COBOL_COPYBOOK", "");
	private final MRUTextCombo<String> _datafile = MRUTextCombo.newMRUTextCombo("COBOL_DATAFILE", "");
	private final CobolFontComboBox    _font     = new CobolFontComboBox();


	private final ComboBoxWithLabelAndTooltips<Integer> _copybookFileFormat = new ComboBoxWithLabelAndTooltips<Integer>()
			.setLabel("Copybook format:")
			.setGlobalTooltip("""
					Cobol is a column-sensitive language; Traditionally columns 1-5 are used for line-numbers (or version comment)
					and ignore everything after column 72. This parameter controls which part of the line to use.""")
			.addDefaultValue(Cb2xmlConstants.USE_STANDARD_COLUMNS, "Use standard columns", "Use columns 6-72 (normal format for mainframe copybooks), this is the default")
			.addValue(Cb2xmlConstants.USE_COLS_6_TO_80, "Use columns from 6 to 80")
			.addValue(Cb2xmlConstants.USE_LONG_LINE, "Use long lines", "Use columns 6-10000");


	private final ComboBoxWithLabelAndTooltips<Integer> _fileOrganization = new ComboBoxWithLabelAndTooltips<Integer>()
			.setLabel("File organization:")
			.setGlobalTooltip("Specify the organization and format of a data file, such as fixed bytes length records text files and VB for mainframe-style variable-length records.")
			.addDefaultValue(IFileStructureConstants.IO_FIXED_LENGTH_RECORDS, "Bytes fixed length records",
					"Each line is a Fixed length. There are no line ending characters. The file is read as bytes so supports binary fields but not unicode. For unicode files use chars fixed length")
			.addValue(IFileStructureConstants.IO_FIXED_LENGTH_CHAR, "Chars fixed length records",
					"Each line (or record) is a fixed number of characters. There are no line ending characters. It will support Unicode files but not binary files.")
			.addValue(IFileStructureConstants.IO_BINARY_IBM_4680, "Binary IBM/4680")
			.addValue(IFileStructureConstants.IO_VB, "RECFM=VB",
					"Mainframe recfm=VB file. Each line consists of a line length followed by the lines data. There is no Block information, just the file data.")
			.addValue(IFileStructureConstants.IO_VB_DUMP, "RECFM=VB Dump with disk-block data",
					"Mainframe Recfm=VB including, It includes both disk-block data + the file data. IO_VB contains file data but no block data.")
			.addValue(IFileStructureConstants.IO_VB_FUJITSU, "Fujistsu RECFM=VB (line length at start and end of line)",
					"Fujitsu Cobols VB files. They contain line-lengths at both the start and end of the line.")
			.addValue(IFileStructureConstants.IO_VB_GNU_COBOL, "Gnu Cobol (line length at start of line)",
					"GNU Cobols VB files. Each line consists of a line-length followed by the Line Data.")
			.addValue(IFileStructureConstants.IO_BIN_TEXT, "Binary text",
					"Text file, it is read as byte (java stream) instead of as Characters. It does not handle Unicode.")
			.addValue(IFileStructureConstants.IO_MICROFOCUS, "Microfocus");


	// https://sourceforge.net/p/jrecord/wiki/Multi%20Record%20Files/
	private final ComboBoxWithLabelAndTooltips<Integer> _splitting = new ComboBoxWithLabelAndTooltips<Integer>()
			.setLabel("Copybook splitting:")
			.setGlobalTooltip("Options to split a Cobol Copybook into seperate records")
			.addDefaultValue(ICobolSplitOptions.SPLIT_NONE, "None", "Standard Single record type copybook")
			.addValue(ICobolSplitOptions.SPLIT_REDEFINE, "Redefine", """
					Multi-Record Copybook with each record is in a redefines
					      01  Trans-File.
					          .....
					         03 Record-Type                  Pic X.
					         ....
					         03  Header-Record.
					             .....\s
					         03  Detail-Record redefines Header-Record.""")
			.addValue(ICobolSplitOptions.SPLIT_01_LEVEL, "1st level", """
					Multi-Record Copybook with each record on a 01 Group level
					      01  Header-Record.
					          .....
					      01  Detail-Record.""")
			.addValue(ICobolSplitOptions.SPLIT_TOP_LEVEL, "Top level", """
					Multi-Record Copybook with each record in a Group level under 01 i.e. 05 in the following
					         05  Header-Record.
					             .....
					         05  Detail-Record.""")
			.addValue(ICobolSplitOptions.SPLIT_HIGHEST_REPEATING, "Highest repeating", """
					Multi-Record Copybook with each record in a Group level under 01 i.e. 05 in the following
					         01  TOP-LEVEL
					             05  Header-Record.
					                 .....
					             05  Detail-Record.""");


	private final ComboBoxWithLabelAndTooltips<Integer> _dialect = new ComboBoxWithLabelAndTooltips<Integer>()
			.setLabel("Cobol dialect:")
			.setGlobalTooltip("Set the Cobol dialect (is it Mainframe, GNU-Cobol etc).")
			.addValue(ICopybookDialects.FMT_INTEL, ICopybookDialects.FMT_INTEL_NAME)
			.addDefaultValue(ICopybookDialects.FMT_MAINFRAME, ICopybookDialects.FMT_MAINFRAME_NAME, "Mainframe Cobol")
			.addValue(ICopybookDialects.FMT_FUJITSU, ICopybookDialects.FMT_FUJITSU_NAME, "Written for the old Fujitsu Cobol 3 compiler")
			.addValue(ICopybookDialects.FMT_BIG_ENDIAN, ICopybookDialects.FMT_BIG_ENDIAN_NAME)
			.addValue(ICopybookDialects.FMT_GNU_COBOL, ICopybookDialects.FMT_GNU_COBOL_NAME, "GNU Cobol (formerly Open Cobol) on a Little Endian machine (e.g Intel)")
			.addValue(ICopybookDialects.FMT_FS2000, ICopybookDialects.FMT_FS2000_NAME)
			.addValue(ICopybookDialects.FMT_GNU_COBOL_MVS, ICopybookDialects.FMT_GNU_COBOL_MVS_NAME)
			.addValue(ICopybookDialects.FMT_GNU_COBOL_MF, ICopybookDialects.FMT_GNU_COBOL_MF_NAME)
			.addValue(ICopybookDialects.FMT_GNU_COBOL_BE, ICopybookDialects.FMT_GNU_COBOL_BE_NAME)
			.addValue(ICopybookDialects.FMT_FS2000_BE, ICopybookDialects.FMT_FS2000_BE_NAME)
			.addValue(ICopybookDialects.FMT_GNU_COBOL_MVS_BE, ICopybookDialects.FMT_GNU_COBOL_MVS_BE_NAME)
			.addValue(ICopybookDialects.FMT_OC_MICRO_FOCUS_BE, ICopybookDialects.FMT_OC_MICRO_FOCUS_BE_NAME, "GNU Cobol running in Microfocus compatibility mode on a Big Endian machine")
			.addValue(ICopybookDialects.FMT_MICRO_FOCUS, "Micro Focus")
			.addValue(ICopybookDialects.FMT_MAINFRAME_COMMA_DECIMAL, ICopybookDialects.FMT_MAINFRAME_COMMA_DECIMAL_NAME)
			.addValue(ICopybookDialects.FMT_FUJITSU_COMMA_DECIMAL, ICopybookDialects.FMT_FUJITSU_COMMA_DECIMAL_NAME);


	public CobolOpenDialog(MainPanel owner) {
		super(owner, "Open cobol copybook:", true);
		_main = owner;
		setResizable(false);
		setMinimumSize(new Dimension(640, 100));
		setLocationRelativeTo(owner);
		addKeyStrokeAction(this, ESC, this::doCancel);
		final int borer = 8;

		final Box verticalBox = Box.createVerticalBox();
		verticalBox.setBorder(UIUtils.createEmptyBorder(borer));
		setContentPane(verticalBox);

		verticalBox.add(horizontalBox(new JLabel("Copybook:"), _copybook, newButton(ICO_OPEN, this::selectCopybook)));
		setSingleFileDropListener(_copybook, _copybook::setValue);
		verticalBox.add(_copybookFileFormat);

		verticalBox.add(Box.createVerticalStrut(borer));

		verticalBox.add(horizontalBox(new JLabel("Datafile:"), _datafile, newButton(ICO_OPEN, this::selectDatafile)));
		setSingleFileDropListener(_datafile, _datafile::setValue);
		verticalBox.add(_font);
		verticalBox.add(_fileOrganization);
		verticalBox.add(_splitting);
		verticalBox.add(_dialect);

		verticalBox.add(Box.createVerticalGlue());
		verticalBox.add(Box.createVerticalStrut(borer));
		verticalBox.add(atStart(new JLabel("<html><b>WARNING:</b> opening cobol copybook is still experimental.</html>")));
		verticalBox.add(atStart(new JLabel("<html><b>Hint:</b> Leave the mouse pointer over an item to get help.</html>")));
		verticalBox.add(Box.createVerticalStrut(borer));

		final Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(newButton(ICO_CANCEL + " Cancel", this::doCancel));
		buttons.add(newButton(ICO_OPEN + " Open", this::doOpen));
		verticalBox.add(buttons);

		pack();
	}

	private void selectCopybook() {
		FileUtils.fileDialog(OPEN, new File(_copybook.getValue()), "Select copybook:",
						Map.of(List.of("cbl", "cob"), "Cobol file")
				)
				.findFirst()
				.ifPresent(f -> _copybook.setValue(f.getAbsolutePath()));
	}

	private void selectDatafile() {
		FileUtils.fileDialog(OPEN, new File(_datafile.getValue()), "Select datafile:")
				.findFirst()
				.ifPresent(f -> _datafile.setValue(f.getAbsolutePath()));
	}

	private void doOpen() {
		try {
			final String datafile = _datafile.getValue();
			if (datafile == null || datafile.isBlank()) {
				popupError("No input datafile");
				return;
			}
			final File datafileFile = new File(datafile);
			if (!datafileFile.canRead()) {
				popupError("Can't read " + datafileFile.getAbsolutePath());
				return;
			}
			@SuppressWarnings("resource") CobolTextSource textSource = new CobolTextSource(
					_copybook.getValue(),
					datafile,
					_copybookFileFormat.getValue(),
					_font.combo.getValue(),
					_fileOrganization.getValue(),
					_splitting.getValue(),
					_dialect.getValue(),
					() -> _main.getProgressBar().addJob("Opening cobol datafile...")
			);

			_main.open(datafile, datafileFile.getName(), closer -> textSource);

			setVisible(false);
		} catch (Throwable e) {
			popupError("Unable to open cobol datafile", e);
		}
	}

	private void doCancel() {
		setVisible(false);
	}

}
