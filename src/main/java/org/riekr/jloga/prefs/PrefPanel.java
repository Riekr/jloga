package org.riekr.jloga.prefs;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.drjekyll.fontchooser.FontDialog;
import org.riekr.jloga.ui.ComboEntryWrapper;

public class PrefPanel extends JDialog {
	private static final long serialVersionUID = 3940084083723252336L;

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
		Container cp = getContentPane();
		cp.setLayout(new GridBagLayout());
		AtomicInteger cpY = new AtomicInteger();
		List<GUIPreference<?>> allPrefs = Preferences.getGUIPreferences();
		for (GUIPreference<?> p : allPrefs) {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder(p.title()));
			panel.setLayout(new GridLayout());

			AtomicInteger panelY = new AtomicInteger();

			String descr = p.description();
			if (descr != null && !descr.isEmpty())
				panel.add(new JLabel(descr), constraints(panelY));

			switch (p.type()) {
				case Font:
					GUIPreference<Font> fontPref = (GUIPreference<Font>)p;
					JButton button = new JButton(fontPref.get().getFontName());
					button.addActionListener((e) -> {
						Font selectedFont = selectFont(fontPref.get());
						if (selectedFont != null) {
							fontPref.set(selectedFont);
							button.setText(selectedFont.getFontName());
						}
					});
					panel.add(button, constraints(panelY));
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
					panel.add(combo, constraints(panelY));
					break;

				default:
					System.err.println("PREFERENCE TYPE " + p.type() + " NOT IMPLEMENTED YET!");
					continue;
			}
			cp.add(panel, constraints(cpY));
		}
		pack();
		setLocationRelativeTo(parent);
		EventQueue.invokeLater(() -> setMinimumSize(getSize()));
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
