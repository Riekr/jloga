package org.riekr.jloga.project;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static org.riekr.jloga.utils.UIUtils.toDateTimeFormatter;
import static org.riekr.jloga.utils.UIUtils.toPattern;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.riekr.jloga.misc.DateTimeFormatterRef;
import org.riekr.jloga.utils.UIUtils;

public interface Project {

	default ProjectEditableField<String> newStringField(String key, String label) {
		return new ProjectEditableField<>(key, label, (text, ui) -> text, identity());
	}

	default ProjectEditableField<Pattern> newPatternField(String key, String label) {
		return newPatternField(key, label, 0);
	}

	default ProjectEditableField<Pattern> newPatternField(String key, String label, int minGroups) {
		return new ProjectEditableField<>(key, label, (pattern, ui) -> toPattern(ui, pattern, minGroups), Pattern::pattern);
	}

	default ProjectEditableField<Duration> newDurationField(String key, String label) {
		return new ProjectEditableField<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern), Duration::toString);
	}

	default ProjectEditableField<Duration> newDurationField(String key, String label, Duration deflt) {
		return new ProjectEditableField<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern), Duration::toString, deflt);
	}

	default ProjectEditableField<DateTimeFormatterRef> newDateTimeFormatterField(String key, String label) {
		return new ProjectEditableField<>(key, label, (pattern, ui) -> toDateTimeFormatter(ui, pattern), DateTimeFormatterRef::pattern);
	}

	default ProjectComboField<String> newSelectField(String key, String label, Map<String, String> values) {
		return new ProjectComboField<>(key, label, values);
	}

	default ProjectCheckboxField<String> newCheckboxField(String key, String label, Map<Boolean, String> values) {
		return new ProjectCheckboxField<>(key, label, values);
	}

	boolean isReady();

	default Stream<? extends ProjectField<?, ?>> fields() {
		return Arrays.stream(getClass().getFields())
				.filter((f) -> ProjectField.class.isAssignableFrom(f.getType()))
				.map((f) -> {
					try {
						return (ProjectField<?, ?>)f.get(this);
					} catch (IllegalAccessException e) {
						e.printStackTrace(System.err);
						return null;
					}
				})
				.filter(Objects::nonNull);
	}

	default void clear() {
		fields().forEach((f) -> f.accept(null));
	}

	default String getDescription() {
		return fields()
				.map(ProjectField::getDescription)
				.collect(joining(" | "));
	}

}
