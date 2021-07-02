package org.riekr.jloga.search;

import org.riekr.jloga.misc.TaggedHolder;

import javax.swing.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.riekr.jloga.ui.UIUtils.*;

public class DurationAnalysisProject implements Supplier<DurationAnalysis> {

	private final JComponent _owner;

	public Function<String, Pattern> patDateExtract = (pat) -> toPattern(getOwner(), pat, 1);
	public Function<String, TaggedHolder<DateTimeFormatter>> patDate = (pat) -> toDateTimeFormatter(getOwner(), pat);
	public Function<String, Pattern> patFunc = (pat) -> toPattern(getOwner(), pat, 1);
	public Function<String, Pattern> patStart = (pat) -> toPattern(getOwner(), pat, 0);
	public Function<String, Pattern> patEnd = (pat) -> toPattern(getOwner(), pat, 0);
	public Function<String, Pattern> patRestart = (pat) -> toPattern(getOwner(), pat, 0);
	public Function<String, Duration> minDuration = (pat) -> toDuration(getOwner(), pat);

	private Pattern _patDateExtract;
	private TaggedHolder<DateTimeFormatter> _patDate;
	private Pattern _patFunc;
	private Pattern _patStart;
	private Pattern _patEnd;
	private Pattern _patRestart;
	private Duration _minDuration;

	public DurationAnalysisProject(JComponent owner) {
		_owner = owner;
	}

	private JComponent getOwner() {
		return _owner;
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

	public void setMinDuration(Duration minDuration) {
		_minDuration = minDuration;
	}

	public void setPatDateExtract(String patDateExtract) {
		_patDateExtract = this.patDateExtract.apply(patDateExtract);
	}

	public void setPatDate(String patDate) {
		_patDate = this.patDate.apply(patDate);
	}

	public void setPatStart(String patStart) {
		_patStart = this.patStart.apply(patStart);
	}

	public void setPatEnd(String patEnd) {
		_patEnd = this.patEnd.apply(patEnd);
	}

	public void setPatFunc(String patFunc) {
		_patFunc = this.patFunc.apply(patFunc);
	}

	public void setPatRestart(String patRestart) {
		_patRestart = this.patRestart.apply(patRestart);
	}

	public void setMinDuration(String minDuration) {
		_minDuration = this.minDuration.apply(minDuration);
	}

	public String getPatDateExtract() {
		return _patDateExtract == null ? null : _patDateExtract.pattern();
	}

	public String getPatDate() {
		return _patDate == null ? null : _patDate.tag;
	}

	public String getPatFunc() {
		return _patFunc == null ? null : _patFunc.pattern();
	}

	public String getPatStart() {
		return _patStart == null ? null : _patStart.pattern();
	}

	public String getPatEnd() {
		return _patEnd == null ? null : _patEnd.pattern();
	}

	public String getPatRestart() {
		return _patRestart == null ? null : _patRestart.pattern();
	}

	public String getMinDuration() {
		return _minDuration == null ? null : _minDuration.toString();
	}

	public boolean isReady() {
		return _patDateExtract != null
				&& _patDate != null && _patDate.value != null
				&& _patStart == null
				&& _patEnd == null
				&& _patFunc == null;
	}

	@Override
	public String toString() {
		return "Date: " + (_patDate == null ? "-" : _patDate.toString())
				+ " | Start: " + (_patStart == null ? "-" : _patStart.pattern())
				+ " | End: " + (_patEnd == null ? "-" : _patEnd.pattern())
				+ " | Func: " + (_patFunc == null ? "-" : _patFunc.pattern())
				+ " | Restart: " + (_patRestart == null ? "-" : _patRestart.pattern())
				+ " | Minimum duration: " + (_minDuration == null ? "-" : _minDuration.toString());
	}

	@Override
	public DurationAnalysis get() {
		return new DurationAnalysis(_patDateExtract, _patDate.value, _patFunc, _patStart, _patEnd, _patRestart, _minDuration);
	}

}
