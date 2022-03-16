package org.riekr.jloga.prefs;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.drjekyll.fontchooser.FontDialog;
import org.riekr.jloga.ui.ComboEntryWrapper;
import org.riekr.jloga.ui.utils.KeyUtils;
import org.riekr.jloga.ui.utils.UIUtils;

public class PrefPanel extends JDialog {
	private static final long serialVersionUID = 3940084083723252336L;

	private static final int _SPACING = 4;

	private GridBagConstraints constraints(AtomicInteger y) {
		GridBagConstraints res = new GridBagConstraints();
		res.fill = GridBagConstraints.HORIZONTAL;
		res.weightx = 1;
		res.gridx = 0;
		res.gridy = y.getAndIncrement();
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
		List<GUIPreference<?>> allPrefs = Preferences.getGUIPreferences();
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
					GUIPreference<Font> fontPref = (GUIPreference<Font>)p;
					JButton button = new JButton(describeFont(fontPref.get()));
					button.addActionListener((e) -> {
						Font selectedFont = selectFont(fontPref.get());
						if (selectedFont != null) {
							fontPref.set(selectedFont);
							button.setText(describeFont(selectedFont));
						}
					});
					button.setAlignmentX(-1);
					panel.add(button);
					break;

				case Combo:
					GUIPreference<Object> comboPref = (GUIPreference<Object>)p;
					ComboEntryWrapper<?>[] values = comboPref.values().stream().map(ComboEntryWrapper::new).toArray(ComboEntryWrapper[]::new);
					JComboBox<ComboEntryWrapper<?>> combo = new JComboBox<>(values);
					combo.setSelectedIndex(ComboEntryWrapper.indexOf(comboPref.get(), values));
					combo.addItemListener(e -> {
						Object selectedItem = combo.getSelectedItem();
						if (selectedItem instanceof ComboEntryWrapper)
							comboPref.set(((ComboEntryWrapper<?>)selectedItem).value);
					});
					combo.setAlignmentX(0);
					panel.add(combo);
					break;

				default:
					System.err.println("PREFERENCE TYPE " + p.type() + " NOT IMPLEMENTED YET!");
					continue;
			}
			cp.add(panel, constraints(cpY));
		}
		cp.add(Box.createVerticalStrut(_SPACING), constraints(cpY));
		Box footer = Box.createHorizontalBox();
		footer.add(Box.createHorizontalGlue());
		footer.add(UIUtils.newButton("Close", this::dispose));
		cp.add(footer, constraints(cpY));
		pack();
		setLocationRelativeTo(parent);
		EventQueue.invokeLater(() -> setMinimumSize(getSize()));
	}

	private String describeFont(Font font) {
		if (font == null)
			return null;
		String family = font.getFamily();
		String name = font.getName().replace('.', ' ');
		if (name.toUpperCase().startsWith(family.toUpperCase()))
			name = name.substring(family.length()).trim();
		return family + ' ' + name + ' ' + font.getSize();
	}

	private Font selectFont(Font initialFont) {
		FontDialog dialog = new FontDialog(this, "Select Font", true);
		dialog.setSelectedFont(initialFont);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		if (!dialog.isCancelSelected())
			return dialog.getSelectedFont();
		return null;
	}
}
