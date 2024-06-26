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
import java.util.function.Supplier;

import org.riekr.jloga.utils.TableReorderMouseHandler;

public class FileMapComponent extends JScrollPane {
	private static final long serialVersionUID = -114942181064191852L;

	private static final String REMOVE = "\u274C";
	private static final String CHANGE = "\uD83D\uDCC2";

	private final JTable _table = new JTable() {
		private static final long serialVersionUID = -2420185066877683979L;

		@Override public boolean isCellEditable(int row, int column) {return isDataColumn(column);}

		@Override public boolean isCellSelected(int row, int column) {return isDataColumn(column);}
	};

	private final Vector<String>         _columns;
	private final Vector<Vector<Object>> _data = new Vector<>();
	private final DefaultTableModel      _model;

	private final Supplier<Iterable<Map.Entry<Object, Object>>> _listSupplier;
	private final BiConsumer<Object, Object>                    _removeAction;
	private final BiConsumer<Object, Object>                    _addAction;
	private final BiConsumer<Object, Object>                    _swapAction;

	private Runnable _onEditingStopAction;

	public FileMapComponent(
			String keyTitle, String valueTitle,
			Supplier<Iterable<Map.Entry<Object, Object>>> listSupplier,
			BiConsumer<Object, Object> removeAction,
			BiConsumer<Object, Object> addAction,
			BiConsumer<Object, Object> swapAction
	) {
		_columns = new Vector<>(asList(keyTitle, valueTitle, "", ""));
		_model = new DefaultTableModel(_data, _columns);
		_listSupplier = listSupplier;
		_removeAction = removeAction;
		_addAction = addAction;
		_swapAction = swapAction;
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
		if (_swapAction != null) {
			TableReorderMouseHandler tr = new TableReorderMouseHandler(_table, (from, to) -> {
				// System.out.println("FROM " + from + " TO " + to);
				_swapAction.accept(
						_data.get(from).get(0),
						_data.get(to).get(0)
				);
				_model.moveRow(from, from, to);
			}, (row) -> row < _data.size() - 1);
			_table.addMouseListener(tr);
			_table.addMouseMotionListener(tr);
		}
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
		_model.setDataVector(_data, _columns);
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
					Vector<Object> r = _data.get(row);
					_removeAction.accept(r.get(0), r.get(1));
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
		Vector<Object> r = _data.get(editingRow);
		Object editingKey = r.get(0);
		Object editingVal = r.get(1);
		switch (editingCol) {
			case 0: {
				// System.out.println("KEY START " + editingKey + " " + editingRow + "," + editingCol);
				_onEditingStopAction = () -> {
					Object newKey = _data.get(editingRow).get(0);
					if (!("".equals(editingVal) || "".equals(newKey)) && !editingKey.equals(newKey)) {
						Object value = _data.get(editingRow).get(1);
						// System.out.println("KEY CHANGE " + newKey + " " + editingRow + "," + editingCol);
						_removeAction.accept(editingKey, editingVal);
						_addAction.accept(newKey, value);
						reload();
					}
				};
			}
			break;
			case 1: {
				// System.out.println("VALUE START " + editingKey + " " + editingRow + "," + editingCol);
				_onEditingStopAction = () -> {
					Object newValue = _data.get(editingRow).get(1);
					if (!("".equals(newValue) || "".equals(editingKey)) && !editingVal.equals(newValue)) {
						File file = new File(newValue.toString());
						// System.out.println("VALUE CHANGE " + file + " " + editingRow + "," + editingCol);
						_addAction.accept(editingKey, file);
						reload();
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
