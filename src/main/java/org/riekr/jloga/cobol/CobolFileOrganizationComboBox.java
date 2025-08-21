package org.riekr.jloga.cobol;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import net.sf.JRecord.Common.IFileStructureConstants;

record FileOrganization(int val, String name) {
	@Override
	public @NotNull String toString() {return name;}
}

class CobolFileOrganizationComboBox extends JComboBox<FileOrganization> {

	private static final FileOrganization   _DEFAULT;
	private static final FileOrganization[] _VALUES = new FileOrganization[]{
			new FileOrganization(IFileStructureConstants.IO_STANDARD_TEXT_FILE, "Standard text file"),
			_DEFAULT = new FileOrganization(IFileStructureConstants.IO_FIXED_LENGTH_RECORDS, "Fixed bytes length records"),
			new FileOrganization(IFileStructureConstants.IO_FIXED_LENGTH_CHAR, "Fixed chars length records"),
			new FileOrganization(IFileStructureConstants.IO_BINARY_IBM_4680, "Binary IBM/4680"),
			new FileOrganization(IFileStructureConstants.IO_VB, "RECFM=VB"),
			new FileOrganization(IFileStructureConstants.IO_VB_DUMP, "RECFM=VB Dump with disk-block data"),
			new FileOrganization(IFileStructureConstants.IO_VB_FUJITSU, "Fujistsu RECFM=VB (line length at start and end of line)"),
			new FileOrganization(IFileStructureConstants.IO_VB_GNU_COBOL, "Gnu Cobol (line length at start of line)"),
			new FileOrganization(IFileStructureConstants.IO_BIN_TEXT, "Binary text"),
			new FileOrganization(IFileStructureConstants.IO_MICROFOCUS, "Microfocus")
	};

	CobolFileOrganizationComboBox() {
		super(_VALUES);
		setSelectedItem(_DEFAULT);
	}

	public Integer getValue() {
		final int i = super.getSelectedIndex();
		return i < 0 ? null : _VALUES[i].val();
	}

}
