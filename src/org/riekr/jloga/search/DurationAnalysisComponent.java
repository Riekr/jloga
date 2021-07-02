package org.riekr.jloga.search;

import org.riekr.jloga.misc.TaggedHolder;
import org.riekr.jloga.react.BoolBehaviourSubject;
import org.riekr.jloga.react.BoolConsumer;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.ui.FitOnScreenComponentListener;
import org.riekr.jloga.ui.MRUTextCombo;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.riekr.jloga.ui.UIUtils.toDateTimeFormatter;
import static org.riekr.jloga.ui.UIUtils.toPattern;

public class DurationAnalysisComponent extends JLabel implements SearchComponent {

	private final BoolBehaviourSubject _configVisible = new BoolBehaviourSubject();
	private final JFrame _configFrame;

	private Pattern _patDateExtract;
	private TaggedHolder<DateTimeFormatter> _patDate;
	private Pattern _patFunc;
	private Pattern _patStart;
	private Pattern _patEnd;
	private Pattern _patRestart;
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
		configPane.add(newEditableField(level + ".DateExtractor", "Date extractor pattern:", this::setPatDateExtract, (pat) -> toPattern(this, pat, 1)));
		configPane.add(newEditableField(level + ".Date", "Date pattern:", this::setPatDate, (pat) -> toDateTimeFormatter(this, pat)));
		configPane.add(newEditableField(level + ".Func", "Function pattern:", this::setPatFunc, (pat) -> toPattern(this, pat, 1)));
		configPane.add(newEditableField(level + ".Start", "Start pattern:", this::setPatStart, (pat) -> toPattern(this, pat, 0)));
		configPane.add(newEditableField(level + ".End", "End pattern:", this::setPatEnd, (pat) -> toPattern(this, pat, 0)));
		configPane.add(newEditableField(level + ".Restart", "Restart pattern:", this::setPatRestart, (pat) -> toPattern(this, pat, 0)));

		JButton start = new JButton("Start analysis");
		start.addMouseListener(_mouseListener);
		start.addActionListener((e) -> this.search());
		configPane.add(start);

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

	private <T> JComponent newEditableField(String key, String label, Consumer<T> onResult, Function<String, T> mapper) {
		JPanel container = new JPanel();
		container.setLayout(new BorderLayout());
		container.add(new JLabel(label + " "), BorderLayout.LINE_START);
		MRUTextCombo combo = new MRUTextCombo("DurationAnalysisComponent." + key);
		combo.addMouseListener(_mouseListener);
		combo.addPopupMenuListener(new PopupMenuListener() {
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
		Consumer<String> onText = (text) -> {
			T res = mapper.apply(text);
			if (res == null)
				combo.setSelectedIndex(-1);
			onResult.accept(res);
		};
		_configVisible.subscribe((BoolConsumer) (visible) -> {
			if (!visible)
				onText.accept((String) combo.getSelectedItem());
		});
		combo.setListener(onText);
		container.add(combo, BorderLayout.CENTER);
		return container;
	}

	private void calcTitle() {
		if (_patDate == null && _patStart == null && _patEnd == null && _patFunc == null)
			setText("Hey! Hover here!");
		else {
			setText("Date: " + (_patDate == null ? "-" : _patDate.toString())
					+ " | Start: " + (_patStart == null ? "-" : _patStart.pattern())
					+ " | End: " + (_patEnd == null ? "-" : _patEnd.pattern())
					+ " | Func: " + (_patFunc == null ? "-" : _patFunc.pattern())
			);
		}
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
		if (_onSearchConsumer != null &&
				// is it ready?
				_patDateExtract != null && _patDate != null && _patDate.value != null && _patStart != null && _patEnd != null && _patFunc != null
		) {
			_configVisible.next(false);
			_onSearchConsumer.accept(new DurationAnalysis(_patDateExtract, _patDate.value, _patFunc, _patStart, _patEnd, _patRestart));
		}
	}

	@Override
	public String getLabel() {
		return "\u0394";
	}

	public void setPatDateExtract(Pattern patDateExtract) {
		_patDateExtract = patDateExtract;
	}

	public void setPatDate(TaggedHolder<DateTimeFormatter> patDate) {
		_patDate = patDate;
	}

	public void setPatStart(Pattern patStart) {
		_patStart = patStart;
	}

	public void setPatEnd(Pattern patEnd) {
		_patEnd = patEnd;
	}

	public void setPatFunc(Pattern patFunc) {
		_patFunc = patFunc;
	}

	public void setPatRestart(Pattern patRestart) {
		_patRestart = patRestart;
	}

}
