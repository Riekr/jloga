package org.riekr.jloga.ui;

import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HalfeningAdjustmentListener implements AdjustmentListener, KeyEventDispatcher {

	private final @NotNull IntConsumer      _consumer;
	private                boolean          _listening  = false;
	private                int              _value;
	private                int              _startValue = -1;
	private                int              _factor     = 1;
	private @Nullable      IntUnaryOperator _operator;

	public HalfeningAdjustmentListener(@NotNull IntConsumer consumer) {
		_consumer = consumer;
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		_value = e.getValue();
		if (e.getValueIsAdjusting()) {
			if (!_listening) {
				_listening = true;
				getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
			}
		} else if (_listening) {
			getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
			_startValue = -1;
		}
		// if (!e.getValueIsAdjusting()) // <- uncomment to avoid scroll while dragging
		_consumer.accept(_operator == null ? _value : _operator.applyAsInt(_value));
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		_factor = 1;
		if (e.isShiftDown())
			_factor *= 2;
		if (e.isControlDown())
			_factor *= 4;
		if (e.isAltDown() || e.isAltGraphDown())
			_factor *= 8;
		if (_factor > 1) {
			if (_operator == null) {
				_startValue = _value;
				_operator = (newValue) -> _startValue + ((newValue - _startValue) / _factor);
			}
		} else {
			_startValue = -1;
			_operator = null;
		}
		return true;
	}
}
