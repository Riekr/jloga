package org.riekr.jloga.ui;

import static java.lang.Integer.min;
import static java.lang.Math.max;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.httpd.FinosPerspectiveServer;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.transform.FastSplitOperation;
import org.riekr.jloga.ui.utils.ContextMenu;
import org.riekr.jloga.ui.utils.SelectionHighlight;
import org.riekr.jloga.ui.utils.UIUtils;

public class VirtualTextArea extends JComponent implements FileDropListener {
	private static final long serialVersionUID = -2704231180724047955L;

	private static final int _GRID_HEADER_CHECK_LINES = 25;

	private int _lineHeight;

	private       int                       _fromLine        = 0;
	private       int                       _lineCount       = 0;
	private       int                       _allLinesCount   = 0;
	private final BehaviourSubject<Integer> _highlightedLine = new BehaviourSubject<>(null);

	private final String          _title;
	private final VirtualTextArea _parent;

	private final JScrollPane         _scrollPane;
	private final JTextArea           _text;
	private final LineNumbersTextArea _lineNumbers;
	private final JScrollBar          _scrollBar;

	private final JToggleButton     _gridToggle;
	private       JTextAreaGridView _gridView;
	private       String            _header;
	private       boolean           _ownHeader;
	private final JButton           _perspectiveBtn;

	private TextSource     _textSource;
	private Unsubscribable _textSourceUnsubscribable;

	private @Nullable IntConsumer _lineListener;
	private @Nullable Runnable    _lineListenerUnsubscribe;

	public VirtualTextArea(@Nullable TabNavigation tabNavigation, @Nullable String title, @Nullable VirtualTextArea parent) {
		_title = title;
		_parent = parent;

		setOpaque(false);
		JPanel root = new JPanel();
		root.setLayout(new BorderLayout());
		root.setOpaque(false);

		_lineNumbers = new LineNumbersTextArea();
		root.add(_lineNumbers, BorderLayout.LINE_START);

		_text = new JTextArea();
		_text.addKeyListener(new ROKeyListener() {
			@Override
			protected void onPageUp() {
				pageUp();
			}

			@Override
			protected void onPageDn() {
				pageDn();
			}

			@Override
			protected void onDocumentStart() {
				toBeginning();
			}

			@Override
			protected void onDocumentEnd() {
				toEnding();
			}

			@Override
			protected void onPreviousTab() {
				if (tabNavigation != null)
					tabNavigation.goToPreviousTab();
			}

			@Override
			protected void onNextTab() {
				if (tabNavigation != null)
					tabNavigation.goToNextTab();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				if (!e.isConsumed() && _lineListener != null && e.getModifiersEx() == 0) {
					Integer line = null;
					switch (e.getKeyCode()) {
						case 38: // up
							if ((line = _highlightedLine.get()) != null)
								line = max(0, line - 1);
							break;
						case 40: // down
							if ((line = _highlightedLine.get()) != null)
								line = min(_allLinesCount - 1, line + 1);
							break;
					}
					if (line != null) {
						setHighlightedLine(line);
						_lineListener.accept(line);
					}
				}
			}
		});
		_text.setAutoscrolls(false);
		_text.setLineWrap(false);

		_scrollPane = new JScrollPane(_text, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		_scrollPane.setWheelScrollingEnabled(false);
		BoundedRangeModel vscrollModel = _scrollPane.getVerticalScrollBar().getModel();
		vscrollModel.addChangeListener(e -> vscrollModel.setValue(0));
		root.add(_scrollPane, BorderLayout.CENTER);
		_scrollPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				recalcLineCount();
			}
		});
		_scrollPane.addMouseWheelListener((e) -> {
			if (e.isShiftDown()) {
				JScrollBar scrollBar = _scrollPane.getHorizontalScrollBar();
				int incr = (scrollBar.getMaximum() - scrollBar.getMinimum()) / 10;
				scrollBar.setValue(scrollBar.getValue() + (e.getWheelRotation() * incr));
			} else {
				if (e.getWheelRotation() < 0)
					pageUp();
				else
					pageDn();
			}
		});

		_scrollBar = new JScrollBar(JScrollBar.VERTICAL);
		_scrollBar.setMinimum(0);
		_scrollBar.setEnabled(false);
		_scrollBar.addAdjustmentListener(new HalfeningAdjustmentListener(this::setFromLineNoScroll));
		root.add(_scrollBar, BorderLayout.LINE_END);

		// setup selection highlight
		new SelectionHighlight(_text);

		// floating buttons
		final Box buttons = Box.createVerticalBox();
		Box buttonsContainer = Box.createHorizontalBox();
		buttons.add(buttonsContainer);
		buttons.add(Box.createVerticalGlue());
		buttonsContainer.add(Box.createHorizontalGlue());
		_gridToggle = new JToggleButton("\u25A6");
		buttonsContainer.add(_gridToggle);
		_gridToggle.addActionListener((e) -> setGridView(_gridToggle.isSelected()));
		_perspectiveBtn = new JButton("\uD83D\uDCC8");
		_perspectiveBtn.addActionListener(e -> openInPerspective());
		buttonsContainer.add(_perspectiveBtn);
		buttonsContainer.add(Box.createRigidArea(new Dimension(_scrollBar.getPreferredSize().width, 0)));

		// overlay layout
		setLayout(new OverlayLayout(this));
		add(buttons);
		add(root);

		// finishing
		recalcLineHeight();
		_highlightedLine.subscribe(this::highlightLine);
		ContextMenu.addActionCopy(this, _text, _lineNumbers);
	}

	public void setGridView(boolean active) {
		if (active) {
			if (_gridView == null) {
				String header = requireHeader();
				if (header == null)
					return;
				try {
					_gridView = new JTextAreaGridView(_text, header);
				} catch (IllegalArgumentException e) {
					gridNotAvailable(e.getLocalizedMessage());
					return;
				}
				_gridView.setRowHeight(_lineHeight);
				_gridView.getTableHeader().setPreferredSize(new Dimension(_scrollPane.getWidth(), _lineHeight));
				_scrollPane.setViewportView(_gridView);
			}
		} else {
			if (_gridView != null) {
				_gridView = null;
				_scrollPane.setViewportView(_text);
			}
		}
	}

	private void gridNotAvailable(String reason) {
		_header = "";
		_ownHeader = true;
		_gridToggle.setSelected(false);
		_gridToggle.setEnabled(false);
		_perspectiveBtn.setEnabled(false);
		if (reason != null)
			JOptionPane.showMessageDialog(this, reason, "Grid view not available", JOptionPane.INFORMATION_MESSAGE);
	}

	private String requireHeader() {
		String header = getHeader();
		if (header.isEmpty()) {
			EventQueue.invokeLater(() -> gridNotAvailable("Grid column count is not stable across the first " + _GRID_HEADER_CHECK_LINES + " lines"));
			return null;
		}
		return header;
	}


	/**
	 * Search for header recurively to parents as the search may have stripped it out
	 */
	@NotNull
	public String getHeader() {
		if (_header == null) {
			if (_parent != null)
				_header = _parent.getHeader();
			if (_header == null || _header.isEmpty()) {
				_header = _textSource.getText(0);
				int splitHeader = FastSplitOperation.count(_header);
				// avoid calling getLineCount() here or may block til the end of indexing
				int count = Math.min(_allLinesCount, _GRID_HEADER_CHECK_LINES);
				for (int i = 1; i < count; i++) {
					int verify = FastSplitOperation.count(_textSource.getText(i));
					if (splitHeader != verify) {
						_header = "";
						break;
					}
				}
				_ownHeader = true;
			}
		}
		return _header;
	}

	/** Will open finos perspective in a standalone browser window.*/
	public void openInPerspective() {
		_textSource.requestStream((stream) -> {
			String header = requireHeader();
			if (header != null && !_ownHeader)
				stream = Stream.concat(Stream.of(requireHeader()), stream);
			// the server will automatically close when the browser closes (websocket disconnected)
			// the port is automatically determined in the constructor
			FinosPerspectiveServer server = new FinosPerspectiveServer();
			server.start(true);
			server.load(_title, stream);
		});
	}

	@Override
	public boolean isOptimizedDrawingEnabled() {
		// (not a bug, just for clarification)
		// https://bugs.openjdk.java.net/browse/JDK-6459830
		return false;
	}

	public int getFromLine() {
		return _fromLine;
	}

	public void setFromLine(int fromLine) {
		if (setFromLineNoScroll(fromLine))
			_scrollBar.setValue(_fromLine);
	}

	private boolean setFromLineNoScroll(int fromLine) {
		if (fromLine < 0)
			fromLine = 0;
		else if (fromLine >= _allLinesCount - _lineCount)
			fromLine = max(0, _allLinesCount - _lineCount - 1);
		if (fromLine != _fromLine) {
			_fromLine = fromLine;
			requireText();
			return true;
		}
		return false;
	}

	public void pageUp() {
		setFromLine(_fromLine - (_lineCount / Preferences.PAGE_SCROLL_DIVIDER.get()));
	}

	public void pageDn() {
		setFromLine(_fromLine + (_lineCount / Preferences.PAGE_SCROLL_DIVIDER.get()));
	}

	public void toBeginning() {
		setHighlightedLine(0);
	}

	public void toEnding() {
		if (_textSource.isIndexing())
			setHighlightedLine(_allLinesCount);
		else {
			int limit = _allLinesCount - 100;
			TextSource.EXECUTOR.submit(() -> {
				for (int line = _allLinesCount; line > limit; line--) {
					String text = _textSource.getText(line);
					if (text != null && !text.isEmpty()) {
						setHighlightedLine(line);
						return;
					}
				}
				setHighlightedLine(_allLinesCount);
			});
		}
	}

	public void centerOn(int line) {
		_highlightedLine.next(line);
		setFromLine(line - (_lineCount / 2));
	}

	@Override
	public void setFont(Font f) {
		_text.setFont(f);
		if (_text.getDocument() != null)
			recalcLineHeight();
		_lineNumbers.setFont(f);
	}

	public void setTextSource(TextSource textSource) {
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
		_textSource = textSource;
		if (textSource != null) {
			if ((_title != null && Preferences.AUTO_GRID.get() && Pattern.compile("\\.[tc]sv$", Pattern.CASE_INSENSITIVE).matcher(_title).find())
					|| (textSource.mayHaveTabularData() && Preferences.AUTO_TAB_GRID.get() && !getHeader().isEmpty())) {
				EventQueue.invokeLater(() -> {
					_gridToggle.setSelected(true);
					setGridView(true);
				});
			}
			_textSourceUnsubscribable = textSource.requestLineCount(this::setFileLineCount);
			requireText();
		}
	}

	private void setFileLineCount(int lineCount) {
		if (_allLinesCount != lineCount) {
			// +1 to ensure last line is shown even if window borders fall across it
			_allLinesCount = lineCount + 1;
			recalcScrollBarMaximum();
			_scrollBar.setEnabled(true);
			requireText();
		}
	}

	private void recalcScrollBarMaximum() {
		_scrollBar.setMaximum(_allLinesCount - _lineCount);
	}

	private void recalcLineHeight() {
		final int newValue = _text.getFontMetrics(_text.getFont()).getHeight();
		if (newValue != _lineHeight) {
			_lineHeight = newValue;
			recalcLineCount();
		}
	}

	private void recalcLineCount() {
		final int newValue = max(1, (int)Math.floor(getHeight() / (double)_lineHeight) - 1);
		if (newValue != _lineCount) {
			_lineCount = newValue;
			_scrollBar.setBlockIncrement(newValue);
			recalcScrollBarMaximum();
			requireText();
		}
	}

	private void requireText() {
		if (_textSource == null)
			_text.setText("");
		else {
			_textSource.requestText(_fromLine, _lineCount, (reader) -> {
				try {
					// _text.setText(text); simply does not work every time
					_text.read(reader, _fromLine);
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
				if (_gridView != null)
					_gridView.refresh();
				reNumerate();
				EventQueue.invokeLater(() -> {
					_scrollPane.getHorizontalScrollBar().setValue(0);
					highlightLine(_highlightedLine.get());
				});
			});
		}
	}

	private void highlightLine(Integer highlightedLine) {
		if (highlightedLine != null) {
			int line = highlightedLine - _fromLine;
			try {
				Highlighter highlighter = _text.getHighlighter();
				highlighter.removeAllHighlights();
				if (line >= 0 && line <= _lineCount) {
					int start = _text.getLineStartOffset(line);
					highlighter.addHighlight(
							start,
							_text.getLineEndOffset(line),
							new DefaultHighlighter.DefaultHighlightPainter(_text.getSelectionColor())
					);
					_text.setCaretPosition(start);
				}
			} catch (BadLocationException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private void reNumerate() {
		_lineNumbers.reNumerate(
				_fromLine,
				_fromLine + _lineCount,
				_allLinesCount
		);
	}

	public TextSource getTextSource() {
		return _textSource;
	}

	public void setLineClickListener(IntConsumer listener) {
		if (_lineListenerUnsubscribe != null) {
			_lineListenerUnsubscribe.run();
			_lineListenerUnsubscribe = null;
		}
		_lineListener = listener;
		if (listener != null) {
			MouseListener mouseListener = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1)
						listener.accept(_fromLine + (e.getY() / _lineHeight));
					else
						_highlightedLine.next(null);
				}
			};
			_text.addMouseListener(mouseListener);
			_lineNumbers.addMouseListener(mouseListener);
			_lineListenerUnsubscribe = () -> {
				_text.removeMouseListener(mouseListener);
				_lineNumbers.removeMouseListener(mouseListener);
			};
		}
	}

	public void setHighlightedLine(Integer line) {
		_highlightedLine.next(line);
		if (line != null) {
			if (line < _fromLine)
				setFromLine(line);
			else if (line > (_fromLine + _lineCount))
				setFromLine(line - _lineCount);
		}
	}

	public void onClose() {
		_textSource.onClose();
		if (_lineListenerUnsubscribe != null)
			_lineListenerUnsubscribe.run();
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
	}

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		UIUtils.setFileDropListener(_text, consumer);
		UIUtils.setFileDropListener(_lineNumbers, consumer);
	}
}
