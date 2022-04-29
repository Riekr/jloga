package org.riekr.jloga.search;

import static javax.swing.BorderFactory.createEmptyBorder;
import static org.riekr.jloga.react.Observer.async;
import static org.riekr.jloga.react.Observer.uniq;
import static org.riekr.jloga.utils.KeyUtils.ESC;
import static org.riekr.jloga.utils.KeyUtils.addKeyStrokeAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.prefs.PrefsUtils;
import org.riekr.jloga.react.BoolBehaviourSubject;
import org.riekr.jloga.ui.FitOnScreenComponentListener;
import org.riekr.jloga.ui.HoverMonitor;
import org.riekr.jloga.ui.MRUComboWithLabels;
import org.riekr.jloga.utils.SpringUtils;
import org.riekr.jloga.utils.UIUtils;

public abstract class SearchComponentWithExpandablePanel extends JComponent implements SearchComponent {
	private static final long serialVersionUID = 7751803744482675483L;

	private final String               _prefsPrefix;
	private final BoolBehaviourSubject _configVisible = new BoolBehaviourSubject();

	private JDialog                   _configFrame;
	private Consumer<SearchPredicate> _onSearchConsumer;

	private final JLabel       _collapsedLabel = new JLabel();
	private final HoverMonitor _mouseListener  = new HoverMonitor(_configVisible::next);

	public SearchComponentWithExpandablePanel(String prefsPrefix) {
		setLayout(new BorderLayout());
		add(_collapsedLabel, BorderLayout.CENTER);
		add(UIUtils.newBorderlessButton("Run", this::search, "Run analysis with current parameters"), BorderLayout.LINE_END);
		_prefsPrefix = prefsPrefix;
	}

	public void setText(String text) {
		_collapsedLabel.setText(text);
	}

	protected void buildUI() {
		if (_configFrame == null) {
			_configFrame = new JDialog(Main.getMain(), false);
			_configFrame.setVisible(false);
			_configFrame.setUndecorated(true);
			_configFrame.addComponentListener(FitOnScreenComponentListener.INSTANCE);
			addKeyStrokeAction(_configFrame, ESC, () -> _configVisible.next(false));

			JPanel configPane = new JPanel(new SpringLayout());
			configPane.setBorder(createEmptyBorder(8, 8, 8, 8));
			setupConfigPane(configPane);
			_configFrame.setContentPane(configPane);

			Box configPaneButtons = Box.createHorizontalBox();
			setupConfigPaneButtons(configPaneButtons);
			configPaneButtons.add(newButtonSpacer(2, 1));
			configPaneButtons.add(newButton("Start analysis", this::search));
			configPaneButtons.add(Box.createGlue());
			configPaneButtons.setBorder(createEmptyBorder(8, 0, 0, 0));
			configPane.add(configPaneButtons);

			SpringUtils.makeCompactGrid(configPane, configPane.getComponentCount(), 1, 0, 0, 0, 0);

			_collapsedLabel.addMouseListener(_mouseListener);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					_configVisible.toggle();
				}
			});
			_configVisible.subscribe(async(uniq(this::setExpanded)));

			calcTitle();
		}
	}

	protected abstract void setupConfigPane(Container configPane);

	protected abstract void setupConfigPaneButtons(Container configPaneButtons);

	public MRUComboWithLabels<String> newEditableComponent(String key, String label, Consumer<String> onResult) {
		MRUComboWithLabels<String> editableField = MRUComboWithLabels.forString(_prefsPrefix + '.' + key, label, onResult);
		editableField.combo.addMouseListener(_mouseListener);
		editableField.combo.addPopupMenuListener(_mouseListener);
		// TODO: ugly!
		EventQueue.invokeLater(() -> _configVisible.subscribe((visible) -> {
			if (!visible)
				editableField.combo.resend();
		}));
		return editableField;
	}

	public JCheckBox newCheckbox(String key, String label, Consumer<String> onResult) {
		JCheckBox checkBox = new JCheckBox(label);
		checkBox.addActionListener((e) -> {
			boolean val = checkBox.isSelected();
			onResult.accept(Boolean.toString(val));
			PrefsUtils.save(_prefsPrefix + '.' + key, val);
		});
		checkBox.addMouseListener(_mouseListener);
		// TODO: ugly!
		EventQueue.invokeLater(() -> _configVisible.subscribe((visible) -> {
			if (!visible)
				onResult.accept(Boolean.toString(PrefsUtils.load(_prefsPrefix + '.' + key)));
		}));

		return checkBox;
	}

	protected abstract void calcTitle();

	protected JComponent newButton(String label, Runnable action) {
		JButton button = new JButton(label);
		button.addMouseListener(_mouseListener);
		button.addActionListener((e) -> action.run());
		return button;
	}

	protected Component newButtonSpacer() {
		return newButtonSpacer(1, 1);
	}

	protected Component newButtonSpacer(int hmult, int vmult) {
		return Box.createRigidArea(new Dimension(16 * hmult, 16 * vmult));
	}

	public void setExpanded(boolean expanded) {
		if (expanded) {
			if (!isEnabled()) {
				_configVisible.next(false);
				return;
			}
			_configFrame.setMinimumSize(getSize());
			_configFrame.pack();
			_configFrame.setLocation(getLocationOnScreen());
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

	@Override
	public @NotNull JComponent getUIComponent() {
		return this;
	}

}
