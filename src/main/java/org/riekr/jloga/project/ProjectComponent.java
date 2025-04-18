package org.riekr.jloga.project;

import static org.riekr.jloga.utils.SpringLayoutUtils.makeCompactGrid;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchComponentWithExpandablePanel;

public abstract class ProjectComponent extends SearchComponentWithExpandablePanel implements Project {
	@Serial private static final long serialVersionUID = -1685626672776376188L;

	private final String _fileExt;
	private final String _fileDescr;

	protected Supplier<TextSource> _textSource;

	public ProjectComponent(String id, int level, String fileDescr) {
		this(id + '.' + level, "jloga", fileDescr);
	}

	public ProjectComponent(String prefsPrefix, String fileExt, String fileDescr) {
		super(prefsPrefix);
		_fileExt = fileExt;
		_fileDescr = fileDescr;
	}

	@Override
	protected void setupConfigPane(Container configPane) {
		List<ProjectCheckboxField<?>> checkboxFields = new ArrayList<>();
		List<ProjectField<?, ?>> otherFields = new ArrayList<>();
		fields().sequential().forEachOrdered((ProjectField<?, ?> f) -> {
			if (f instanceof ProjectCheckboxField)
				checkboxFields.add((ProjectCheckboxField<?>)f);
			else
				otherFields.add(f);
		});

		if (!otherFields.isEmpty()) {
			JPanel labeledComponents = new JPanel(new SpringLayout());
			configPane.add(labeledComponents);
			for (ProjectField<?, ?> f : otherFields) {
				String labelText = f.label.trim();
				if (!labelText.endsWith(":"))
					labelText += ':';
				JLabel label = new JLabel(labelText + ' ');
				Component comp = f.ui(this);
				label.setLabelFor(comp);
				labeledComponents.add(label);
				labeledComponents.add(comp);
			}
			makeCompactGrid(labeledComponents, otherFields.size(), 2, 0, 0, 0, 0);
		}

		if (!checkboxFields.isEmpty()) {
			Box checkboxPanel = Box.createHorizontalBox();
			checkboxFields.forEach((f) -> checkboxPanel.add(f.ui(this)));
			checkboxPanel.add(Box.createHorizontalGlue());
			configPane.add(checkboxPanel);
		}
	}

	@Override
	protected void setupConfigPaneButtons(Container configPaneButtons) {
		if (AutoDetect.Wizard.class.isAssignableFrom(getClass())) {
			configPaneButtons.add(newButton(AutoDetect.GLYPH, ((AutoDetect.Wizard)this)::onWizard));
			configPaneButtons.add(newButtonSpacer());
		}
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, _fileExt, _fileDescr)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, _fileExt, _fileDescr)));
		configPaneButtons.add(newButtonSpacer());
		configPaneButtons.add(newButton("Reset!", this::clear));
	}

	protected void calcTitle() {
		if (isReady())
			setText(getDescription());
		else if (Preferences.PRJCLICK.get())
			setText("Hey! Click here!");
		else
			setText("Hey! Hover here!");
	}

	@Override
	protected void search() {
		if (isReady())
			super.search();
	}

	public final void setTextSourceSupplier(Supplier<TextSource> textSource) {
		_textSource = textSource;
	}
}
