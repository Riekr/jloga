package org.riekr.jloga.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;

// from: https://stackoverflow.com/questions/58043707/java-how-to-drag-jtable-rows-without-usage-of-transferhandler
public class TableReorderMouseHandler implements MouseListener, MouseMotionListener {

	private Integer row = null;

	private final JTable                       _table;
	private final BiConsumer<Integer, Integer> _swapAction;
	private final IntPredicate                 _acceptor;

	public TableReorderMouseHandler(JTable table, BiConsumer<Integer, Integer> swapAction, IntPredicate acceptor) {
		_table = table;
		_swapAction = swapAction;
		_acceptor = acceptor;
	}

	@Override
	public void mouseClicked(MouseEvent event) {}

	@Override
	public void mousePressed(MouseEvent event) {
		int viewRowIndex = _table.rowAtPoint(event.getPoint());
		int row = _table.convertRowIndexToModel(viewRowIndex);
		if (_acceptor.test(row)) {
			this.row = row;
			_table.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else
			this.row = null;
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		row = null;
		_table.setCursor(Cursor.getDefaultCursor());
	}

	@Override
	public void mouseEntered(MouseEvent event) {}

	@Override
	public void mouseExited(MouseEvent event) {}

	@Override
	public void mouseDragged(MouseEvent event) {
		if (row == null)
			return;

		int viewRowIndex = _table.rowAtPoint(event.getPoint());
		int currentRow = _table.convertRowIndexToModel(viewRowIndex);

		if (currentRow == row || !_acceptor.test(currentRow))
			return;

		_swapAction.accept(row, currentRow);
		row = currentRow;
		_table.setRowSelectionInterval(viewRowIndex, viewRowIndex);
	}

	@Override
	public void mouseMoved(MouseEvent event) {}

}