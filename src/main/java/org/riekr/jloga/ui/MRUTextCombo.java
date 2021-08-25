package org.riekr.jloga.ui;

import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.react.Subject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.BiFunction;

public class MRUTextCombo<T> extends JComboBox<T> {

	public static MRUTextCombo<String> newMRUTextCombo(String key) {
		return new MRUTextCombo<>(key, (newValue, oldValue) -> newValue);
	}

	private final String _key;
	private final DefaultComboBoxModel<T> _model;
	private final BiFunction<String, T, T> _mapper;

	private T _value;
	private int _valueIndex;
	private boolean _propagateMouseListener = true;

	public final Subject<T> subject = new Subject<>();

	public MRUTextCombo(String key, BiFunction<String, T, T> mapper) {
		_key = key;
		_value = mapper.apply(null, null);
		subject.subscribe((value) -> {
			_value = value;
			_valueIndex = getSelectedIndex();
		});
		_model = Preferences.loadDefaultComboBoxModel(_key);
		_mapper = mapper;
		setModel(_model);
		setEditable(true);
		addActionListener(e -> {
			Object elem = getSelectedItem();
			switch (e.getActionCommand()) {
				case "comboBoxEdited":
					_model.removeElement(elem);
					_model.insertElementAt(convert(elem), 0);
					save();
					setSelectedIndex(0);
				case "comboBoxChanged":
					subject.next(convert(elem));
					break;
				default:
					System.err.println(e);
			}
		});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentHidden(ComponentEvent e) {
				subject.next(convert(getSelectedItem()));
			}
		});
		addHierarchyListener(e -> {
			if (e.getID() == HierarchyEvent.PARENT_CHANGED && getParent() == null)
				subject.close();
		});
		getEditor().getEditorComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					showPopup();
			}
		});
	}

	public void save() {
		Preferences.save(_key, _model);
	}

	@SuppressWarnings("unchecked")
	private T convert(Object elem) {
		if (elem instanceof String)
			return _mapper.apply((String) elem, _valueIndex == getSelectedIndex() ? _value : null);
		return (T) elem;
	}

	@Override
	public void updateUI() {
		try {
			_propagateMouseListener = false;
			super.updateUI();
		} finally {
			_propagateMouseListener = true;
		}
	}

	@Override
	public synchronized void addMouseListener(MouseListener l) {
		super.addMouseListener(l);
		if (_propagateMouseListener) {
			getEditor().getEditorComponent().addMouseListener(l);
			for (Component child : getComponents()) {
				if (child instanceof JButton) {
					child.addMouseListener(l);
					break;
				}
			}
		}
	}

	public T getValue() {
		return _value;
	}

	public void setValue(T str) {
		setSelectedItem(str);
		subject.next(str);
		save();
	}
}
