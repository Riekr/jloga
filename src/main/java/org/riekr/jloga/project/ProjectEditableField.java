package org.riekr.jloga.project;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.riekr.jloga.ui.MRUComboWithLabels;

public class ProjectEditableField<T> extends ProjectField<T, MRUComboWithLabels<?>> {
	public ProjectEditableField(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode) {
		super(key, label, mapper, encode);
	}

	public ProjectEditableField(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode, T deflt) {
		super(key, label, mapper, encode, deflt);
	}

	@Override
	public void set(T value) {
		super.set(value);
		_ui.combo.setValue(_src);
	}

	@Override
	public void accept(String s) {
		super.accept(s);
		_ui.combo.setValue(_src);
	}

	@Override
	protected MRUComboWithLabels<?> newUI(ProjectComponent panel) {
		return panel.newEditableComponent(key, null, this);
	}
}
