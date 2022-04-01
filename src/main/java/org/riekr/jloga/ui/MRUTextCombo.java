package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.BiFunction;

import org.riekr.jloga.prefs.PrefsUtils;
import org.riekr.jloga.react.Subject;

public class MRUTextCombo<T> extends JComboBox<T> {
	private static final long serialVersionUID = -3289527560220826466L;

	public static MRUTextCombo<String> newMRUTextCombo(String key, String deflt) {
		return new MRUTextCombo<>(key, (newValue, oldValue) -> newValue == null ? deflt : newValue);
	}

	public static MRUTextCombo<String> newMRUTextCombo(String key) {
		return new MRUTextCombo<>(key, (newValue, oldValue) -> newValue);
	}

	private final String                   _key;
	private final DefaultComboBoxModel<T>  _model;
	private final BiFunction<String, T, T> _mapper;

	private T       _value;
	private int     _valueIndex;
	private boolean _propagateMouseListener = true;

	public final Subject<T> subject = new Subject<>();

	public MRUTextCombo(String key, BiFunction<String, T, T> mapper) {
		_key = key;
		_value = mapper.apply(null, null);
		subject.subscribe((value) -> {
			_value = value;
			_valueIndex = getSelectedIndex();
		});
		_model = PrefsUtils.loadDefaultComboBoxModel(_key);
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
					//noinspection fallthrough
				case "comboBoxChanged":
					requestFocusInWindow();
					break;
				default:
					System.err.println(e);
			}
		});
		getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					resend();
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

	public void resend() {
		subject.next(convert(getSelectedItem()));
	}

	public void save() {
		PrefsUtils.save(_key, _model);
	}

	@SuppressWarnings("unchecked")
	private T convert(Object elem) {
		if (elem instanceof String)
			return _mapper.apply((String)elem, _valueIndex == getSelectedIndex() ? _value : null);
		return (T)elem;
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
