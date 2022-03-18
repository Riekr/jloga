package org.riekr.jloga.prefs;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.riekr.jloga.ui.utils.FileUtils.selectDirectoryDialog;
import static org.riekr.jloga.ui.utils.FontUtils.describeFont;
import static org.riekr.jloga.ui.utils.FontUtils.selectFontDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.ui.ComboEntryWrapper;
import org.riekr.jloga.ui.utils.KeyUtils;
import org.riekr.jloga.ui.utils.UIUtils;

public class PrefPanel extends JDialog {
	private static final long serialVersionUID = 3940084083723252336L;

	private static final int    _SPACING = 4;
	private static final String _RESET   = "\u21BA";

	private final ArrayList<Unsubscribable> _subscriptions = new ArrayList<>();

	private GridBagConstraints constraints(AtomicInteger y) {
		GridBagConstraints res = new GridBagConstraints();
		res.fill = GridBagConstraints.HORIZONTAL;
		res.weightx = 1;
		res.weighty = 1;
		res.gridx = 0;
		res.gridy = y.getAndIncrement();
		res.anchor = GridBagConstraints.NORTH;
		return res;
	}

	@SuppressWarnings("unchecked")
	public PrefPanel(Frame parent) {
		super(parent, "Preferences:", true);
		setResizable(false);
		KeyUtils.closeOnEscape(this);

		JPanel cp = new JPanel();
		getContentPane().add(cp);
		cp.setLayout(new GridBagLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING));
		AtomicInteger cpY = new AtomicInteger();
		JTabbedPane tabs = new JTabbedPane();
		cp.add(tabs, constraints(cpY));
		Map<String, List<GUIPreference<?>>> prefsByGroup = Preferences.getGUIPreferences().stream().sequential()
				.collect(groupingBy(GUIPreference::group, LinkedHashMap::new, toList()));
		for (Map.Entry<String, List<GUIPreference<?>>> e : prefsByGroup.entrySet()) {
			String group = e.getKey();
			List<GUIPreference<?>> allPrefs = e.getValue();
			JPanel tabContents = new JPanel();
			tabContents.setBorder(BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING));
			tabContents.setLayout(new GridBagLayout());
			AtomicInteger tabY = new AtomicInteger();
			tabs.addTab(group, tabContents);
			for (GUIPreference<?> p : allPrefs) {
				Box panel = Box.createVerticalBox();
				panel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(p.title()),
						BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING)
				));
				String descr = p.description();
				if (descr != null && !descr.isEmpty()) {
					JLabel label = new JLabel(descr);
					label.setAlignmentX(0);
					panel.add(label);
					panel.add(Box.createVerticalStrut(_SPACING));
				}
				switch (p.type()) {
					case Font:
						panel.add(newFontComponent((GUIPreference<Font>)p));
						break;
					case Combo:
						panel.add(newComboComponent((GUIPreference<Object>)p));
						break;
					case Toggle:
						panel.add(newToggleComponent((GUIPreference<Boolean>)p));
						break;
					case Directory:
						panel.add(newDirectoryComponent((GUIPreference<File>)p));
						break;

					default:
						System.err.println("PREFERENCE TYPE " + p.type() + " NOT IMPLEMENTED YET!");
						continue;
				}
				tabContents.add(panel, constraints(tabY));
			}
		}
		cp.add(Box.createVerticalStrut(_SPACING), constraints(cpY));
		Box footer = Box.createHorizontalBox();
		footer.add(UIUtils.newButton("Reset all", () -> {
			int input = JOptionPane.showConfirmDialog(this, "Do you really want to reset all preferences?", "Reset", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION)
				prefsByGroup.values().stream().flatMap(Collection::stream).forEach(Preference::reset);
		}));
		footer.add(Box.createHorizontalGlue());
		footer.add(UIUtils.newButton("Close", this::dispose));
		cp.add(footer, constraints(cpY));
		pack();
		setLocationRelativeTo(parent);
		EventQueue.invokeLater(() -> setMinimumSize(getSize()));
	}

	private Component newFontComponent(GUIPreference<Font> fontPref) {
		JPanel res = new JPanel();
		res.setLayout(new BorderLayout());
		final JButton selectButton = new JButton();
		_subscriptions.add(fontPref.subscribe((font) -> selectButton.setText(describeFont(fontPref.get()))));
		res.add(selectButton, BorderLayout.CENTER);
		selectButton.addActionListener((e) -> {
			Font selectedFont = selectFontDialog(this, fontPref.get());
			if (selectedFont != null) {
				fontPref.set(selectedFont);
				selectButton.setText(describeFont(selectedFont));
			}
		});
		final JButton resetButton = new JButton(_RESET);
		resetButton.addActionListener((e) -> fontPref.reset());
		res.add(resetButton, BorderLayout.LINE_END);
		res.setAlignmentX(-1);
		return res;
	}

	private Component newComboComponent(GUIPreference<Object> comboPref) {
		ComboEntryWrapper<?>[] values = comboPref.values().stream().map(ComboEntryWrapper::new).toArray(ComboEntryWrapper[]::new);
		JComboBox<ComboEntryWrapper<?>> combo = new JComboBox<>(values);
		_subscriptions.add(comboPref.subscribe((selectedItem) -> combo.setSelectedIndex(ComboEntryWrapper.indexOf(selectedItem, values))));
		combo.addItemListener(e -> {
			Object selectedItem = combo.getSelectedItem();
			if (selectedItem instanceof ComboEntryWrapper)
				comboPref.set(((ComboEntryWrapper<?>)selectedItem).value);
		});
		combo.setAlignmentX(0);
		return combo;
	}

	private Component newToggleComponent(GUIPreference<Boolean> togglePref) {
		JCheckBox toggle = new JCheckBox(togglePref.title());
		toggle.addChangeListener(e -> togglePref.set(toggle.isSelected()));
		toggle.setSelected(togglePref.get());
		toggle.setAlignmentX(0);
		return toggle;
	}

	private Component newDirectoryComponent(GUIPreference<File> dirPref) {
		JPanel res = new JPanel();
		res.setLayout(new BorderLayout());
		final JButton selectButton = new JButton();
		_subscriptions.add(dirPref.subscribe((dir) -> selectButton.setText(dir == null ? "" : dir.getAbsolutePath())));
		selectButton.addActionListener((e) -> {
			File newDir = selectDirectoryDialog(this, dirPref.get());
			if (newDir != null)
				dirPref.set(newDir);
		});
		res.add(selectButton, BorderLayout.CENTER);
		final JButton resetButton = new JButton(_RESET);
		resetButton.addActionListener((e) -> dirPref.reset());
		res.add(resetButton, BorderLayout.LINE_END);
		res.setAlignmentX(0);
		return res;
	}

	@Override
	public void dispose() {
		super.dispose();
		_subscriptions.forEach(Unsubscribable::unsubscribe);
	}
}
