package org.riekr.jloga.utils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import static javax.swing.JComponent.WHEN_FOCUSED;

public class SelectionHighlight {

	private final JTextArea                    _textArea;
	private final Highlighter.HighlightPainter _painter = new DefaultHighlighter.DefaultHighlightPainter(Color.blue);
	private final ArrayList<Object>            _tags    = new ArrayList<>();
	private       String                       _highlightedText;
	private       boolean                      _forced;

	public SelectionHighlight(@NotNull JTextArea textArea) {
		_textArea = textArea;
		_textArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!_forced)
					clear();
			}
		});
		_textArea.addCaretListener(e -> {
			int start = _textArea.getSelectionStart();
			int length = _textArea.getSelectionEnd() - start;
			if (length > 0) {
				try {
					String text = _textArea.getText(start, length);
					if (text != null && !text.trim().isEmpty() && !text.equals(_highlightedText)) {
						_highlightedText = text;
						refresh();
					}
				} catch (BadLocationException ex) {
					ex.printStackTrace(System.err);
				}
			}
		});
		KeyUtils.addKeyStrokeAction(_textArea, KeyUtils.ESC, this::clear, WHEN_FOCUSED);
		if (_textArea.isEditable()) {
			DocumentListener documentListener = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {refresh();}

				@Override
				public void removeUpdate(DocumentEvent e) {refresh();}

				@Override
				public void changedUpdate(DocumentEvent e) {refresh();}
			};
			_textArea.addPropertyChangeListener("document", (evt) -> {
				this.refresh();
				_textArea.getDocument().addDocumentListener(documentListener);
			});
		} else
			_textArea.addPropertyChangeListener("document", (evt) -> this.refresh());
	}

	public void setText(String text) {
		_forced = text != null;
		if ((_highlightedText == null ^ text == null) || (_highlightedText == null || !_highlightedText.equals(text))) {
			_highlightedText = text;
			refresh();
		}
	}

	public String getText() {
		return _highlightedText;
	}

	public void clear() {
		if (_highlightedText != null) {
			_highlightedText = null;
			_forced = false;
			refresh();
		}
	}

	public void refresh() {
		Highlighter highlighter = _textArea.getHighlighter();
		_tags.forEach(highlighter::removeHighlight);
		String selection = _highlightedText;
		if (selection != null) {
			String text = _textArea.getText();
			int next = 0;
			int from;
			while ((from = text.indexOf(selection, next)) != -1) {
				next = from + selection.length();
				try {
					_tags.add(highlighter.addHighlight(from, next, _painter));
				} catch (BadLocationException e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}

}
