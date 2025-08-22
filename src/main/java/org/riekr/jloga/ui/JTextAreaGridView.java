package org.riekr.jloga.ui;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS_MATRIX;
import static org.riekr.jloga.utils.ContextMenu.addActionCopy;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.Serial;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.transform.FastSplitOperation;

public class JTextAreaGridView extends JTable {
	@Serial private static final long serialVersionUID = -4187398533175075732L;

	private final          FastSplitOperation _splitter = new FastSplitOperation();
	private final @NotNull VirtualTextArea    _text;
	private final          boolean            _headerIsEmbedded;

	private String[]   _header;
	private String[][] _data = EMPTY_STRINGS_MATRIX;

	public JTextAreaGridView(@NotNull VirtualTextArea text, String header, boolean headerIsEmbedded) {
		_text = text;
		_header = _splitter.apply(header);
		_headerIsEmbedded = headerIsEmbedded;
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setCellSelectionEnabled(true);
		setModel(new AbstractTableModel() {
			@Serial private static final long serialVersionUID = -937458072437359755L;

			@Override
			public int getRowCount() {return _data.length;}

			@Override
			public int getColumnCount() {return _header.length;}

			@Nls
			@Override
			public String getColumnName(int columnIndex) {return _header[columnIndex];}

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

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		// https://stackoverflow.com/a/25570812/1326326
		Component component = super.prepareRenderer(renderer, row, column);
		int rendererWidth = component.getPreferredSize().width;
		TableColumn tableColumn = getColumnModel().getColumn(column);
		tableColumn.setMinWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
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
		repaint();
	}

	public void setDelim(char delim) {
		_splitter.setDelim(delim);
	}

	public void setHeader(String header) {
		_header = _splitter.apply(header);
	}
}
