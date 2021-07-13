package org.riekr.jloga.search;

import org.riekr.jloga.react.BoolBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.ui.FitOnScreenComponentListener;
import org.riekr.jloga.ui.MRUComboWithLabels;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public abstract class SearchComponentWithExpandablePanel extends JLabel implements SearchComponent {

	private final String _prefsPrefix;
	private final BoolBehaviourSubject _configVisible = new BoolBehaviourSubject();
	private JFrame _configFrame;

	private Consumer<SearchPredicate> _onSearchConsumer;

	private boolean _mouseListenerEnabled = true;
	private final MouseListener _mouseListener = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			if (_mouseListenerEnabled)
				_configVisible.next(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (_mouseListenerEnabled)
				_configVisible.next(false);
		}
	};

	public SearchComponentWithExpandablePanel(String prefsPrefix) {
		_prefsPrefix = prefsPrefix;
	}

	protected void buildUI() {
		if (_configFrame == null) {
			_configFrame = new JFrame();
			_configFrame.setUndecorated(true);
			_configFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			_configFrame.setVisible(false);
			_configFrame.addComponentListener(FitOnScreenComponentListener.INSTANCE);

			Container configPane = _configFrame.getContentPane();
			configPane.setLayout(new BoxLayout(configPane, BoxLayout.Y_AXIS));
			setupConfigPane(configPane);

			Box configPaneButtons = new Box(BoxLayout.X_AXIS);
			configPane.add(configPaneButtons);
			setupConfigPaneButtons(configPaneButtons);
			configPaneButtons.add(Box.createGlue());
			configPaneButtons.add(newButton("Start analysis", this::search));

			addMouseListener(_mouseListener);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					_configVisible.toggle();
				}
			});
			_configVisible.subscribe(Observer.async(Observer.uniq(this::setExpanded)));

			calcTitle();
		}
	}

	protected abstract void setupConfigPane(Container configPane);

	protected abstract void setupConfigPaneButtons(Container configPaneButtons);

	protected MRUComboWithLabels<String> newEditableField(String key, String label, Consumer<String> onResult) {
		MRUComboWithLabels<String> editableField = MRUComboWithLabels.forString(_prefsPrefix + '.' + key, label, onResult);
		editableField.combo.addMouseListener(_mouseListener);
		editableField.combo.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				_mouseListenerEnabled = false;
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				_mouseListenerEnabled = true;
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				_mouseListenerEnabled = true;
			}
		});
		return editableField;
	}

	protected abstract void calcTitle();

	protected JComponent newButton(String label, Runnable action) {
		JButton button = new JButton(label);
		button.addMouseListener(_mouseListener);
		button.addActionListener((e) -> action.run());
		return button;
	}

	protected Component newButtonSpacer() {
		return Box.createRigidArea(new Dimension(16, 16));
	}

	public void setExpanded(boolean expanded) {
		if (expanded) {
			if (!isEnabled()) {
				_configVisible.next(false);
				return;
			}
			Point loc = getLocationOnScreen();
			Dimension size = getSize();
			_configFrame.setLocation(loc);
			_configFrame.setMinimumSize(size);
			_configFrame.pack();
			removeMouseListener(_mouseListener);
			_configFrame.addMouseListener(_mouseListener);
		} else {
			_configFrame.removeMouseListener(_mouseListener);
			addMouseListener(_mouseListener);
			EventQueue.invokeLater(this::calcTitle);
		}
		_configFrame.setVisible(expanded);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		_onSearchConsumer = consumer;
	}

	protected abstract SearchPredicate getSearchPredicate();

	protected void search() {
		SearchPredicate predicate = getSearchPredicate();
		if (predicate != null && _onSearchConsumer != null) {
			_configVisible.next(false);
			_onSearchConsumer.accept(predicate);
		}
	}

}
