package org.riekr.jloga.cobol;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import net.sf.cb2xml.def.Cb2xmlConstants;

record CopybookFileFormat(int val, String name) {
	@Override
	public @NotNull String toString() {return name;}
}

class CobolCopybookFileFormatComboBox extends JComboBox<CopybookFileFormat> {

	private static final CopybookFileFormat   _DEFAULT;
	private static final CopybookFileFormat[] _VALUES = new CopybookFileFormat[]{
			_DEFAULT = new CopybookFileFormat(Cb2xmlConstants.USE_STANDARD_COLUMNS, "Use standard columns"),
			new CopybookFileFormat(Cb2xmlConstants.USE_COLS_6_TO_80, "Use columns from 6 to 80"),
			new CopybookFileFormat(Cb2xmlConstants.USE_LONG_LINE, "Use long lines"),
			new CopybookFileFormat(Cb2xmlConstants.FREE_FORMAT, "Free format")
	};

	CobolCopybookFileFormatComboBox() {
		super(_VALUES);
		setSelectedItem(_DEFAULT);
	}

	public Integer getValue() {
		final int i = super.getSelectedIndex();
		return i < 0 ? null : _VALUES[i].val();
	}

}
