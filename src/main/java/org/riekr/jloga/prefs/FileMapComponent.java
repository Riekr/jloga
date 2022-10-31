package org.riekr.jloga.prefs;

import static java.util.Arrays.asList;
import static javax.swing.SwingUtilities.invokeLater;
import static org.riekr.jloga.utils.FileUtils.selectDirectoryDialog;
import static org.riekr.jloga.utils.MouseListenerBuilder.mouse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FileMapComponent extends JScrollPane {

	private static final String REMOVE = "\u274C";
	private static final String CHANGE = "\uD83D\uDCC2";

	private static final Vector<String> _COLUMNS = new Vector<>(asList("Nick", "Folder", "", ""));

	private final JTable _table = new JTable() {
		@Override public boolean isCellEditable(int row, int column) {return isDataColumn(column);}

		@Override public boolean isCellSelected(int row, int column) {return isDataColumn(column);}
	};

	private final Vector<Vector<Object>> _data  = new Vector<>();
	private final DefaultTableModel      _model = new DefaultTableModel(_data, _COLUMNS);

	private final Supplier<Iterable<Map.Entry<Object, Object>>> _listSupplier;
	private final Consumer<Object>                              _removeAction;
	private final BiConsumer<Object, Object>                    _addAction;

	private Runnable _onEditingStopAction;

	public FileMapComponent(
			Supplier<Iterable<Map.Entry<Object, Object>>> listSupplier,
			Consumer<Object> removeAction,
			BiConsumer<Object, Object> addAction
	) {
		_listSupplier = listSupplier;
		_removeAction = removeAction;
		_addAction = addAction;
		_table.setModel(_model);
		_table.setShowGrid(false);
		_table.addMouseListener(mouse().onClick(this::onClick));
		setViewportView(_table);
		reload();
		_table.addPropertyChangeListener(evt -> {
			if ("tableCellEditor".equals(evt.getPropertyName())) {
				if (_table.isEditing())
					invokeLater(this::onEditingStart);
				else
					invokeLater(this::onEditingStop);
			}
		});
	}

	public void reload() {
		_data.clear();
		_listSupplier.get().forEach(entry -> {
			Vector<Object> row = new Vector<>(2);
			row.add(entry.getKey()); // 0
			row.add(entry.getValue()); // 1
			row.add(REMOVE); // 2
			row.add(CHANGE); // 3
			_data.add(row);
		});
		_data.add(new Vector<>(asList("", "", "", CHANGE)));
		_model.setDataVector(_data, _COLUMNS);
		_table.tableChanged(null);
		invokeLater(this::fixColumnWidths);
	}

	private void fixColumnWidths() {
		TableColumnModel columnModel = _table.getColumnModel();
		int h = _table.getRowHeight();
		for (int i = 0, len = columnModel.getColumnCount(); i < len; i++) {
			if (!isDataColumn(i))
				columnModel.getColumn(i).setMaxWidth(h);
		}
	}

	private boolean isDataColumn(int column) {
		switch (column) {
			case 0:
			case 1:
				return true;
		}
		return false;
	}

	private void onClick(MouseEvent evt) {
		Point point = evt.getPoint();
		int row = _table.rowAtPoint(point);
		if (row >= 0) {
			int col = _table.columnAtPoint(point);
			switch (col) {
				case 2:
					_removeAction.accept(_data.get(row).get(0));
					break;
				case 3:
					Object oldVal = _data.get(row).get(1);
					Object newVal = selectDirectoryDialog(this, oldVal instanceof File ? (File)oldVal : new File("."));
					if (!Objects.equals(oldVal, newVal))
						_addAction.accept(_data.get(row).get(0), newVal);
					break;
			}
		}
	}

	private void onEditingStart() {
		int editingRow = _table.getEditingRow();
		int editingCol = _table.getEditingColumn();
		Object editingKey = _data.get(editingRow).get(0);
		Object editingVal = _data.get(editingRow).get(1);
		if ("".equals(editingVal) || "".equals(editingKey))
			return;
		switch (editingCol) {
			case 0: {
				// System.out.println("KEY START " + editingKey + " " + editingRow + "," + editingCol);
				_onEditingStopAction = () -> {
					Object newKey = _data.get(editingRow).get(0);
					if (!editingKey.equals(newKey)) {
						Object value = _data.get(editingRow).get(1);
						// System.out.println("KEY CHANGE " + newKey + " " + editingRow + "," + editingCol);
						_removeAction.accept(editingKey);
						_addAction.accept(newKey, value);
					}
				};
			}
			break;
			case 1: {
				// System.out.println("VALUE START " + editingKey + " " + editingRow + "," + editingCol);
				_onEditingStopAction = () -> {
					Object newValue = _data.get(editingRow).get(1);
					if (!editingVal.equals(newValue)) {
						File file = new File(newValue.toString());
						// System.out.println("VALUE CHANGE " + file + " " + editingRow + "," + editingCol);
						_addAction.accept(editingKey, file);
					}
				};
			}
		}
	}

	private void onEditingStop() {
		if (_onEditingStopAction != null) {
			_onEditingStopAction.run();
			_onEditingStopAction = null;
		}
	}

}
