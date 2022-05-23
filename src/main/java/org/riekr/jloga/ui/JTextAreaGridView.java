package org.riekr.jloga.ui;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS_MATRIX;
import static org.riekr.jloga.utils.ContextMenu.addActionCopy;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.transform.FastSplitOperation;

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
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (rowIndex < _data.length && columnIndex < _data[rowIndex].length)
					return _data[rowIndex][columnIndex];
				return null;
			}
		});
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		refresh();
		addActionCopy(this, this::getSelectedText);
	}

	private final Map<Integer, Integer> _colsMax = new HashMap<>();

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		// https://stackoverflow.com/a/25570812/1326326
		Component component = super.prepareRenderer(renderer, row, column);
		TableColumn tableColumn = getColumnModel().getColumn(column);
		int colWidth = Math.max(tableColumn.getPreferredWidth(), component.getPreferredSize().width + getIntercellSpacing().width);
		int maxWidth = _colsMax.compute(column, (c, prev) -> prev == null ? colWidth : Math.max(colWidth, prev));
		tableColumn.setPreferredWidth(maxWidth);
		return component;
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
