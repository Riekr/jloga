package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.transform.FastSplitOperation;
import org.riekr.jloga.utils.ContextMenu;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS_MATRIX;

public class JTextAreaGridView extends JTable {
	private static final long serialVersionUID = -4187398533175075732L;

	private final          FastSplitOperation _splitter = new FastSplitOperation();
	private final          String[]           _header;
	private final @NotNull VirtualTextArea    _text;
	private final          boolean            _headerIsEmbedded;

	private String[][] _data = EMPTY_STRINGS_MATRIX;

	public JTextAreaGridView(@NotNull VirtualTextArea text, String header, boolean headerIsEmbedded) {
		_text = text;
		_header = _splitter.apply(header);
		_headerIsEmbedded = headerIsEmbedded;
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setCellSelectionEnabled(true);
		setModel(new AbstractTableModel() {
			private static final long serialVersionUID = -937458072437359755L;

			@Override
			public int getRowCount() {return _data.length;}

			@Override
			public int getColumnCount() {return _header.length;}

			@Nls
			@Override
			public String getColumnName(int columnIndex) {return _header[columnIndex];}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {return false;}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {return _data[rowIndex][columnIndex];}
		});
		refresh();
		ContextMenu.addActionCopy(this, this::getSelectedText);
	}

	public CharSequence getSelectedText() {
		StringBuilder res = new StringBuilder();
		String delim = Character.toString(_splitter.getDelim());
		String escaped = "\\" + delim;
		for (int row : getSelectedRows()) {
			for (int col : getSelectedColumns()) {
				if (isCellSelected(row, col))
					res.append(_data[row][col].replace(delim, escaped));
				res.append(delim);
			}
			res.append('\n');
		}
		return res;
	}

	public void refresh() {
		Stream<String> stream = Pattern.compile("[\r\n]+").splitAsStream(_text.getDisplayedText());
		if (_text.getFromLine() == 0 && _headerIsEmbedded)
			stream = stream.skip(1);
		_data = stream.map(_splitter).toArray(String[][]::new);
		this.tableChanged(null);
	}
}
