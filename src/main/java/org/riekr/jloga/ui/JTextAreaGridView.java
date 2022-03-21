package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.transform.FastSplitOperation;
import org.riekr.jloga.utils.ContextMenu;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS_MATRIX;

public class JTextAreaGridView extends JTable {
	private static final long serialVersionUID = -4187398533175075732L;

	private final          FastSplitOperation _splitter = new FastSplitOperation();
	private final          String[]           _header;
	private final @NotNull JTextArea          _text;

	private String[][] _data = EMPTY_STRINGS_MATRIX;

	public JTextAreaGridView(@NotNull JTextArea text, String header) {
		_text = text;
		_header = _splitter.apply(header);
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
		_data = Pattern.compile("[\r\n]+").splitAsStream(_text.getText())
				.skip(1)
				.map(_splitter)
				.toArray(String[][]::new);
		this.tableChanged(null);
	}
}
