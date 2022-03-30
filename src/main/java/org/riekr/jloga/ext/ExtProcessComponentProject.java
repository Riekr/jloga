package org.riekr.jloga.ext;

import static java.util.Collections.emptyMap;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.ext.ExtProcessConfig.Param;
import org.riekr.jloga.project.ProjectComponent;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessComponentProject extends ProjectComponent {
	private static final long serialVersionUID = 2400305540460475435L;

	private final @NotNull String             _id;
	private final          String             _icon;
	private final          Map<String, Param> _params;
	private final          ExtProcessManager  _manager;

	private Map<String, String> _vars = emptyMap();

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
			}
		});
		buildUI();
	}

	@Override
	public Stream<? extends Field<?>> fields() {
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
		_params.forEach((key, val) -> vars.put(key, val._field.toString()));
		_manager.setSearchVars(vars);
		return _manager.newSearchPredicate();
	}
}