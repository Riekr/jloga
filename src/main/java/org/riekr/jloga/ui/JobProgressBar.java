package org.riekr.jloga.ui;

import org.riekr.jloga.io.ProgressListener;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.IntBehaviourSubject;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.stream.Collectors.joining;

public class JobProgressBar extends JProgressBar {

	private static final class Job {
		public final String descr;
		public long pos, max;

		public Job(String descr) {
			this.descr = descr;
		}
	}

	private final ConcurrentLinkedQueue<Job> _jobs = new ConcurrentLinkedQueue<>();
	private final IntBehaviourSubject _pos = new IntBehaviourSubject();
	private final BehaviourSubject<String> _descr = new BehaviourSubject<>("");

	private int _width;

	public JobProgressBar() {
		setVisible(false);
		setMinimum(0);
		setStringPainted(true);
		_pos.subscribe(this::setValue);
		_descr.subscribe(this::setString);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				_width = getWidth();
				setMaximum(_width);
			}
		});
	}

	public ProgressListener addJob(String description) {
		Job job = new Job(description);
		_jobs.add(job);
		refreshDescr();
		return (pos, size) -> {
			job.pos = pos;
			job.max = size;
			refreshValues();
			if (pos >= size) {
				_jobs.remove(job);
				refreshDescr();
			}
		};
	}

	private void refreshDescr() {
		int sz = _jobs.size();
		if (sz > 0) {
			setVisible(true);
			String suffix = sz > 1 ? " (" + sz + " jobs)" : "";
			_descr.next(
					_jobs.stream()
							.map((job) -> job.descr)
							.distinct()
							.collect(joining(" / ", "", suffix))
			);
		} else {
			setVisible(false);
			_descr.next("");
		}
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	private void refreshValues() {
		if (_jobs.isEmpty())
			return;
		long pos = 0, max = 0;
		for (Job job : _jobs) {
			pos += job.pos;
			max += job.max;
		}
		int intPos = (int) (pos * (double) _width / max);
		if (intPos >= _width) {
			if (_pos.get().intValue() != intPos)
				_pos.last(intPos);
		} else {
			if (_pos.get().intValue() != intPos)
				_pos.updateUI(intPos);
		}
	}
}
