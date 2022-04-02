package org.riekr.jloga.ext;

import static java.util.Collections.emptyMap;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.ext.ExtProcessConfig.Param;
import org.riekr.jloga.project.ProjectComponent;
import org.riekr.jloga.project.ProjectField;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessComponentProject extends ProjectComponent {
	private static final long serialVersionUID = 2400305540460475435L;

	private final @NotNull String             _id;
	private final          String             _icon;
	private final          Map<String, Param> _params;
	private final          ExtProcessManager  _manager;

	private Map<String, String> _vars = emptyMap();

	@SuppressWarnings("unchecked")
	public ExtProcessComponentProject(@NotNull String id, String icon, int level, File workingDir, String[] command, Map<String, Param> params) {
		super(id, level, id);
		_id = id;
		_icon = icon;
		_params = params;
		_manager = new ExtProcessManager(command, workingDir);
		_params.forEach((key, param) -> {
			switch (param.type) {
				case STRING:
					param._field = newStringField(key, param.description);
					break;
				case PATTERN:
					param._field = newPatternField(key, param.description, param.min);
					break;
				case DURATION:
					param._field = newDurationField(key, param.description, param.deflt == null ? null : Duration.parse(param.deflt));
					break;
				case COMBO:
					if (param.values == null)
						throw new IllegalArgumentException("No values specified for combo type");
					if (param.values instanceof List)
						param._field = newSelectField(key, param.description, (List<String>)param.values);
					else if (param.values instanceof Map)
						param._field = newSelectField(key, param.description, (Map<String, String>)param.values);
					else
						throw new IllegalArgumentException("Invalid values specified for combo type");
			}
		});
		buildUI();
	}

	@Override
	public Stream<? extends ProjectField<?, ?>> fields() {
		return _params.values().stream().map((param) -> param._field);
	}

	@Override
	public boolean isReady() {
		for (Param param : _params.values()) {
			if (param.mandatory && (param._field == null || !param._field.hasValue()))
				return false;
		}
		return true;
	}

	@Override
	public String getID() {
		return _id;
	}

	@Override
	public String getSearchIconLabel() {
		return _icon;
	}

	@Override
	public void setVariables(Map<String, String> vars) {
		_vars = vars == null ? emptyMap() : vars;
	}

	@Override
	protected SearchPredicate getSearchPredicate() {
		Map<String, String> vars = new HashMap<>(_vars);
		_params.forEach((key, val) -> {
			Object fieldValue = val._field.get();
			vars.put(key, fieldValue == null ? "" : fieldValue.toString());
		});
		_manager.setSearchVars(vars);
		return _manager.newSearchPredicate();
	}
}
