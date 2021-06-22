package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFileAnalyzer extends JComponent {

	private final SearchPanel _mainTextArea;
	private final JLabel _title;

	public TextFileAnalyzer(TextSource src, JProgressBar progressBar) {
		_mainTextArea = new SearchPanel(progressBar, 0);
		_mainTextArea.setMinimumSize(new Dimension(0, 0));
		_title = new JLabel();

		// layout
		setLayout(new BorderLayout());
		add(_title, BorderLayout.NORTH);
		add(_mainTextArea, BorderLayout.CENTER);
		add(progressBar, BorderLayout.SOUTH);

		// init
		setTextSource(src);
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		_mainTextArea.setFont(f);
	}

	public void setTextSource(@NotNull TextSource textSource) {
		_mainTextArea.setTextSource(textSource);
		_title.setText(textSource.toString());
	}

	@Override
	public String toString() {
		String text = _title.getText();
		System.out.println(text);
		Matcher m = Pattern.compile("(.*[\\\\/])+(.*)").matcher(text);
		return m.matches() ? m.group(2) : "";
	}
}
