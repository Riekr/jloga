package org.riekr.jloga.search;

import org.riekr.jloga.io.PropsIO;
import org.riekr.jloga.misc.TaggedHolder;
import org.riekr.jloga.react.BoolBehaviourSubject;
import org.riekr.jloga.react.BoolConsumer;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.ui.FitOnScreenComponentListener;
import org.riekr.jloga.ui.MRUTextComboWithLabel;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class DurationAnalysisComponent extends JLabel implements SearchComponent {

	private static final String _F_EXT = "jloga-dap";
	private static final String _F_DESCR = "Duration analysis project";

	private final BoolBehaviourSubject _configVisible = new BoolBehaviourSubject();
	private final JFrame _configFrame;

	private final MRUTextComboWithLabel<Pattern> fDateExtractor;
	private final MRUTextComboWithLabel<TaggedHolder<DateTimeFormatter>> fDate;
	private final MRUTextComboWithLabel<Pattern> fFunc;
	private final MRUTextComboWithLabel<Pattern> fStart;
	private final MRUTextComboWithLabel<Pattern> fEnd;
	private final MRUTextComboWithLabel<Pattern> fRestart;
	private final MRUTextComboWithLabel<Duration> fMinDuration;


	private final DurationAnalysisProject _project = new DurationAnalysisProject(this);
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

	public DurationAnalysisComponent(int level) {

		_configFrame = new JFrame();
		_configFrame.setUndecorated(true);
		_configFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		_configFrame.setVisible(false);
		_configFrame.addComponentListener(FitOnScreenComponentListener.INSTANCE);

		Container configPane = _configFrame.getContentPane();
		configPane.setLayout(new BoxLayout(configPane, BoxLayout.Y_AXIS));
		configPane.add(fDateExtractor = newEditableField(level + ".DateExtractor", "Date extractor pattern:", _project::setPatDateExtract, _project.patDateExtract));
		configPane.add(fDate = newEditableField(level + ".Date", "Date pattern:", _project::setPatDate, _project.patDate));
		configPane.add(fFunc = newEditableField(level + ".Func", "Function pattern:", _project::setPatFunc, _project.patFunc));
		configPane.add(fStart = newEditableField(level + ".Start", "Start pattern:", _project::setPatStart, _project.patStart));
		configPane.add(fEnd = newEditableField(level + ".End", "End pattern:", _project::setPatEnd, _project.patEnd));
		configPane.add(fRestart = newEditableField(level + ".Restart", "Restart pattern:", _project::setPatRestart, _project.patRestart));
		configPane.add(fMinDuration = newEditableField(level + ".MinDuration", "Minimum duration:", _project::setMinDuration, _project.minDuration));

		Box configPaneButtons = new Box(BoxLayout.X_AXIS);
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, _project, _F_EXT, _F_DESCR, this::updateConfigPanel)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, _project, _F_EXT, _F_DESCR)));
		configPaneButtons.add(Box.createRigidArea(new Dimension(16, 16)));
		configPaneButtons.add(newButton("Reset!", () -> PropsIO.reset(_project, this::updateConfigPanel)));
		configPaneButtons.add(Box.createGlue());
		configPaneButtons.add(newButton("Start analysis", this::search));
		configPane.add(configPaneButtons);

		calcTitle();

		addMouseListener(_mouseListener);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				_configVisible.toggle();
			}
		});
		_configVisible.subscribe(Observer.async(Observer.uniq(this::setExpanded)));
	}

	private <T> MRUTextComboWithLabel<T> newEditableField(String key, String label, Consumer<T> onResult, Function<String, T> mapper) {
		MRUTextComboWithLabel<T> editableField = new MRUTextComboWithLabel<>("DurationAnalysisComponent." + key, label, onResult, mapper);
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
		_configVisible.subscribe((BoolConsumer) (visible) -> {
			if (!visible)
				editableField.combo.getListener().accept((String) editableField.combo.getSelectedItem());
		});
		return editableField;
	}

	private JComponent newButton(String label, Runnable action) {
		JButton button = new JButton(label);
		button.addMouseListener(_mouseListener);
		button.addActionListener((e) -> action.run());
		return button;
	}

	private void updateConfigPanel() {
		fDateExtractor.combo.set(_project.getPatDateExtract());
		fDate.combo.set(_project.getPatDate());
		fFunc.combo.set(_project.getPatFunc());
		fStart.combo.set(_project.getPatStart());
		fEnd.combo.set(_project.getPatEnd());
		fRestart.combo.set(_project.getPatRestart());
		fMinDuration.combo.set(_project.getMinDuration());
	}

	private void calcTitle() {
		if (_project.isReady())
			setText(_project.toString());
		else
			setText("Hey! Hover here!");
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

	private void search() {
		if (_onSearchConsumer != null && _project.isReady()) {
			_configVisible.next(false);
			_onSearchConsumer.accept(_project.get());
		}
	}

	@Override
	public String getLabel() {
		return "\u0394";
	}

}
