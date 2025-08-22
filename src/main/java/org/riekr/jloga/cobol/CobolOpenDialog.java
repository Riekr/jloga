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

import javax.swing.*;
import java.awt.*;
import java.io.File;

import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.MainPanel;
import org.riekr.jloga.utils.FileUtils;
import org.riekr.jloga.utils.UIUtils;

public class CobolOpenDialog extends JDialog {

	private final MainPanel                       _main;
	private final MRUTextCombo<String>            _copybook           = MRUTextCombo.newMRUTextCombo("COBOL_COPYBOOK", "");
	private final MRUTextCombo<String>            _datafile           = MRUTextCombo.newMRUTextCombo("COBOL_DATAFILE", "");
	private final CobolCopybookFileFormatComboBox _copybookFileFormat = new CobolCopybookFileFormatComboBox();
	private final CobolFontComboBox               _font               = new CobolFontComboBox();
	private final CobolFileOrganizationComboBox   _fileOrganization   = new CobolFileOrganizationComboBox();
	private final CobolCopybookSplitComboBox      _splitting          = new CobolCopybookSplitComboBox();
	private final CobolDialectComboBox            _dialect            = new CobolDialectComboBox();

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
		verticalBox.add(horizontalBox(new JLabel("Copybook format:"), _copybookFileFormat));

		verticalBox.add(Box.createVerticalStrut(borer));

		verticalBox.add(horizontalBox(new JLabel("Datafile:"), _datafile, newButton(ICO_OPEN, this::selectDatafile)));
		verticalBox.add(horizontalBox(new JLabel("Datafile code page:"), _font));
		verticalBox.add(horizontalBox(new JLabel("File organization:"), _fileOrganization));
		verticalBox.add(horizontalBox(new JLabel("Copybook splitting:"), _splitting));
		verticalBox.add(horizontalBox(new JLabel("Cobol dialect:"), _dialect));

		verticalBox.add(Box.createVerticalGlue());
		verticalBox.add(Box.createVerticalStrut(borer));
		verticalBox.add(atStart(new JLabel("<html><b>WARNING:</b> opening cobol copybook is experimental and whole datafile is stored in memory.</html>")));
		verticalBox.add(Box.createVerticalStrut(borer));

		final Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(newButton(ICO_CANCEL + " Cancel", this::doCancel));
		buttons.add(newButton(ICO_OPEN + " Open", this::doOpen));
		verticalBox.add(buttons);

		pack();
	}

	private void selectCopybook() {
		FileUtils.fileDialog(OPEN, new File(_copybook.getValue()), "Select copybook:", "cbl", "Cobol file")
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
			@SuppressWarnings("resource") CobolTextSource textSource = new CobolTextSource(
					_copybook.getValue(),
					datafile,
					_copybookFileFormat.getValue(),
					_font.getValue(),
					_fileOrganization.getValue(),
					_splitting.getValue(),
					_dialect.getValue(),
					() -> _main.getProgressBar().addJob("Opening cobol datafile...")
			);

			_main.open(datafile, datafile, closer -> textSource);

			setVisible(false);
		} catch (Throwable e) {
			popupError("Unable to open cobol datafile", e);
		}
	}

	private void doCancel() {
		setVisible(false);
	}

}
