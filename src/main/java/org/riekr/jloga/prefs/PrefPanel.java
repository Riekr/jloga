package org.riekr.jloga.prefs;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.riekr.jloga.prefs.GUIPreference.Type.KeyBinding;
import static org.riekr.jloga.utils.CollectionUtils.swapRows;
import static org.riekr.jloga.utils.FileUtils.selectDirectoryDialog;
import static org.riekr.jloga.utils.FileUtils.selectExecutableDialog;
import static org.riekr.jloga.utils.FontUtils.describeFont;
import static org.riekr.jloga.utils.FontUtils.selectFontDialog;
import static org.riekr.jloga.utils.KeyUtils.closeOnEscape;
import static org.riekr.jloga.utils.UIUtils.allComponents;
import static org.riekr.jloga.utils.UIUtils.newButton;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.theme.ThemePreview;
import org.riekr.jloga.ui.ComboEntryWrapper;
import org.riekr.jloga.ui.KeyStrokeToggleButton;
import org.riekr.jloga.utils.ContextMenu;

public class PrefPanel extends JDialog {
	private static final long serialVersionUID = 3940084083723252336L;

	private static class Tab extends JPanel {
		private static final long serialVersionUID = 7427857846628153672L;

		final List<GUIPreference<?>> prefs;
		final Box                    contents = Box.createVerticalBox();

		public Tab(List<GUIPreference<?>> prefs) {
			super(new BorderLayout());
			this.prefs = prefs;
			this.contents.setBorder(BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING));
			add(contents, BorderLayout.NORTH);
		}
	}

	private static final int    _SPACING = 4;
	private static final String _RESET   = "\u21BA";

	private static int _LAST_SELECTED_TAB = 0;

	private final JTabbedPane               _tabs;
	private final ArrayList<Unsubscribable> _subscriptions = new ArrayList<>();
	private final ArrayList<AbstractButton> _allButtons    = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public PrefPanel(Frame parent) {
		super(parent, "Preferences:", true);
		setResizable(false);
		closeOnEscape(this);

		_subscriptions.add(Preferences.THEME.subscribe((t) -> SwingUtilities.updateComponentTreeUI(this)));

		JPanel cp = new JPanel(new BorderLayout());
		getContentPane().add(cp);
		cp.setBorder(BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING));
		_tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		cp.add(_tabs, BorderLayout.NORTH);
		Map<String, List<GUIPreference<?>>> prefsByGroup = Preferences.getGUIPreferences().stream().sequential()
				.filter((p) -> {
					if (p.group() == null) {
						System.err.println("Preference '" + p.title() + "' does not belong to a group");
						return false;
					}
					return true;
				})
				.collect(groupingBy(GUIPreference::group, LinkedHashMap::new, toList()));
		for (Map.Entry<String, List<GUIPreference<?>>> e : prefsByGroup.entrySet()) {
			String group = e.getKey();
			final Tab tab = new Tab(e.getValue());
			_tabs.addTab(group, tab);
			Box themePanel = null;
			for (GUIPreference<?> p : tab.prefs) {
				Box panel = Box.createVerticalBox();
				ContextMenu.addAction(panel, "Reset", p::reset);
				String descr = p.description();
				if (descr != null && !descr.isEmpty()) {
					if (p == Preferences.THEME) {
						themePanel = panel;
					} else if (p.type() != KeyBinding) {
						panel.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createTitledBorder(p.title()),
								BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING)
						));
					}
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
					case Executable:
						panel.add(newExecComponent((GUIPreference<File>)p));
						break;
					case KeyBinding:
						panel.add(newKeyBindingComponent((GUIPreference<KeyStroke>)p));
						break;
					case FileMap:
						panel.add(newFileMapComponent((GUIPreference<LinkedHashMap<Object, Object>>)p));
						break;

					default:
						System.err.println("PREFERENCE TYPE " + p.type() + " NOT IMPLEMENTED YET!");
						continue;
				}
				tab.contents.add(panel);
				_subscriptions.add(p.enabled.subscribe((enabled) -> allComponents(panel).forEach((c) -> c.setEnabled(enabled))));
			}
			if (themePanel != null) {
				ThemePreview preview = new ThemePreview();
				preview.setAlignmentX(0);
				preview.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder("Preview:"),
						BorderFactory.createEmptyBorder(_SPACING, _SPACING, _SPACING, _SPACING)
				));
				themePanel.add(Box.createVerticalStrut(_SPACING));
				themePanel.add(preview);
			}
		}
		_tabs.setSelectedIndex(_LAST_SELECTED_TAB);
		cp.add(Box.createVerticalStrut(_SPACING), BorderLayout.CENTER);
		Box footer = Box.createHorizontalBox();
		footer.add(newRegisteredButton("Reset page", () -> {
			Component comp = _tabs.getSelectedComponent();
			if (comp instanceof Tab)
				((Tab)comp).prefs.forEach(Preference::reset);
		}));
		footer.add(newRegisteredButton("Reset all", () -> {
			int input = JOptionPane.showConfirmDialog(this, "Do you really want to reset all preferences?", "Reset", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION)
				prefsByGroup.values().stream().flatMap(Collection::stream).forEach(Preference::reset);
		}));
		footer.add(Box.createHorizontalGlue());
		footer.add(newButton("Close", this::dispose));
		cp.add(footer, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);
		EventQueue.invokeLater(() -> setMinimumSize(getSize()));
	}

	public PrefPanel setSelectedTab(@NotNull String name) {
		for (int i = 0, len = _tabs.getTabCount(); i < len; i++) {
			if (name.equals(_tabs.getTitleAt(i))) {
				_tabs.setSelectedIndex(i);
				return this;
			}
		}
		System.err.println("No PrefPanel tab named '" + name + '\'');
		return this;
	}

	private JButton newRegisteredButton(String text, Runnable action) {
		return register(newButton(text, action));
	}

	private <T extends AbstractButton> T register(T button) {
		_allButtons.add(button);
		return button;
	}

	private Component newFontComponent(GUIPreference<Font> fontPref) {
		JPanel res = new JPanel(new BorderLayout());
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
		JPanel res = new JPanel(new BorderLayout());
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

	private Component newExecComponent(GUIPreference<File> filePref) {
		JPanel res = new JPanel(new BorderLayout());
		final JButton selectButton = new JButton();
		_subscriptions.add(filePref.subscribe((f) -> selectButton.setText(f == null ? "" : f.getAbsolutePath())));
		selectButton.addActionListener((e) -> {
			File newFile = selectExecutableDialog(this, filePref.get());
			if (newFile != null)
				filePref.set(newFile);
		});
		res.add(selectButton, BorderLayout.CENTER);
		final JButton resetButton = new JButton(_RESET);
		resetButton.addActionListener((e) -> filePref.reset());
		res.add(resetButton, BorderLayout.LINE_END);
		res.setAlignmentX(0);
		return res;
	}

	private Component newKeyBindingComponent(GUIPreference<KeyStroke> keyPref) {
		KeyStrokeToggleButton btn = register(new KeyStrokeToggleButton(_allButtons, keyPref) {
			private static final long serialVersionUID = 1481165132273129303L;

			@Override
			protected void disableOthers() {
				super.disableOthers();
				_tabs.setEnabled(false);
			}

			@Override
			protected void enableOthers() {
				super.enableOthers();
				_tabs.setEnabled(true);
			}
		});
		JButton resetButton = register(new JButton(_RESET));
		resetButton.addActionListener((e) -> keyPref.reset());
		_subscriptions.add(keyPref.subscribe((ks) -> btn.refresh()));
		JPanel res = new JPanel(new BorderLayout());
		res.add(btn, BorderLayout.CENTER);
		res.add(resetButton, BorderLayout.LINE_END);
		return res;
	}

	private Component newFileMapComponent(GUIPreference<LinkedHashMap<Object, Object>> pref) {
		FileMapComponent comp = new FileMapComponent(
				() -> pref.get().entrySet(),
				(key) -> pref.tap(p -> p.remove(key)),
				(k, v) -> pref.tap(p -> p.put(k, v)),
				(k1, k2) -> pref.tap(p -> swapRows(p, k1, k2))
		);
		pref.subscribe(comp, (data) -> comp.reload());
		return comp;
	}

	@Override
	public void dispose() {
		_LAST_SELECTED_TAB = _tabs.getSelectedIndex();
		super.dispose();
		_subscriptions.forEach(Unsubscribable::unsubscribe);
	}
}
