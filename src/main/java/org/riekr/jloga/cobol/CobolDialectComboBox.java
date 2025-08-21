package org.riekr.jloga.cobol;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import net.sf.JRecord.Numeric.ICopybookDialects;

record Dialect(int val, String name) {
	@Override
	public @NotNull String toString() {return name;}
}

class CobolDialectComboBox extends JComboBox<Dialect> {

	private static final Dialect   _DEFAULT;
	private static final Dialect[] _VALUES = new Dialect[]{
			new Dialect(ICopybookDialects.FMT_INTEL, ICopybookDialects.FMT_INTEL_NAME),
			_DEFAULT = new Dialect(ICopybookDialects.FMT_MAINFRAME, ICopybookDialects.FMT_MAINFRAME_NAME),
			new Dialect(ICopybookDialects.FMT_FUJITSU, ICopybookDialects.FMT_FUJITSU_NAME),
			new Dialect(ICopybookDialects.FMT_BIG_ENDIAN, ICopybookDialects.FMT_BIG_ENDIAN_NAME),
			new Dialect(ICopybookDialects.FMT_GNU_COBOL, ICopybookDialects.FMT_GNU_COBOL_NAME),
			new Dialect(ICopybookDialects.FMT_FS2000, ICopybookDialects.FMT_FS2000_NAME),
			new Dialect(ICopybookDialects.FMT_GNU_COBOL_MVS, ICopybookDialects.FMT_GNU_COBOL_MVS_NAME),
			new Dialect(ICopybookDialects.FMT_GNU_COBOL_MF, ICopybookDialects.FMT_GNU_COBOL_MF_NAME),
			new Dialect(ICopybookDialects.FMT_GNU_COBOL_BE, ICopybookDialects.FMT_GNU_COBOL_BE_NAME),
			new Dialect(ICopybookDialects.FMT_FS2000_BE, ICopybookDialects.FMT_FS2000_BE_NAME),
			new Dialect(ICopybookDialects.FMT_GNU_COBOL_MVS_BE, ICopybookDialects.FMT_GNU_COBOL_MVS_BE_NAME),
			new Dialect(ICopybookDialects.FMT_OC_MICRO_FOCUS_BE, ICopybookDialects.FMT_OC_MICRO_FOCUS_BE_NAME),
			new Dialect(ICopybookDialects.FMT_MICRO_FOCUS, "Micro Focus"),
			new Dialect(ICopybookDialects.FMT_MAINFRAME_COMMA_DECIMAL, ICopybookDialects.FMT_MAINFRAME_COMMA_DECIMAL_NAME),
			new Dialect(ICopybookDialects.FMT_FUJITSU_COMMA_DECIMAL, ICopybookDialects.FMT_FUJITSU_COMMA_DECIMAL_NAME)
	};

	CobolDialectComboBox() {
		super(_VALUES);
		setSelectedItem(_DEFAULT);
	}

	public Integer getValue() {
		final int i = super.getSelectedIndex();
		return i < 0 ? null : _VALUES[i].val();
	}

}
