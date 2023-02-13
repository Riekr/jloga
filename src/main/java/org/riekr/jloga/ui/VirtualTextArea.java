package org.riekr.jloga.ui;

import static java.awt.EventQueue.invokeLater;
import static java.lang.Integer.min;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
import static org.riekr.jloga.utils.AsyncOperations.asyncTask;
import static org.riekr.jloga.utils.TextUtils.ellipsize;

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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.httpd.FinosPerspectiveServer;
import org.riekr.jloga.io.ProgressListener;
import org.riekr.jloga.io.RangedTextSource;
import org.riekr.jloga.io.Section;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.prefs.ExtraLines;
import org.riekr.jloga.prefs.HighlightType;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.transform.HeaderDetector;
import org.riekr.jloga.utils.CaretLimiter;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.utils.PopupUtils;
import org.riekr.jloga.utils.SelectionHighlight;
import org.riekr.jloga.utils.UIUtils;

public class VirtualTextArea extends JComponent implements FileDropListener {
	private static final long serialVersionUID = -2704231180724047955L;

	public static class Dummy extends VirtualTextArea {
		private static final long serialVersionUID = 8358979652181379726L;

		public Dummy(String title) {
			super(null, title, null);
		}

		@Override
		public void openInPerspective() {}

	}

	private int _lineHeight;

	private int _fromLine      = 0;
	private int _lineCount     = 0;
	private int _allLinesCount = 0;

	private final BehaviourSubject<Integer> _highlightedLine = new BehaviourSubject<>(null);
	private       Object                    _lineHighlight;
	private final SelectionHighlight        _selectionHighlight;

	private final String          _title;
	private final VirtualTextArea _parent;

	private final JScrollPane              _scrollPane;
	private final JTextAreaWithFontMetrics _text;
	private final Selection                _textSelection;
	private final LineNumbersTextArea      _lineNumbers;
	private final JScrollBar               _scrollBar;

	private final Box               _floatingButtons;
	private final JToggleButton     _gridToggle;
	private       JTextAreaGridView _gridView;
	private       HeaderDetector    _header;

	private TextSource     _textSource;
	private List<Section>  _sections;
	private Unsubscribable _textSourceUnsubscribable;
	private Future<?>      _prevRequireTextRequest;
	private Future<?>      _prevReloadRequest;

	private @Nullable IntConsumer _lineListener;
	private @Nullable Runnable    _lineListenerUnsubscribe;

	private ExtraLines _extraLines = ExtraLines.HALF;

	public VirtualTextArea(@Nullable TabNavigation tabNavigation, @Nullable String title, @Nullable VirtualTextArea parent) {
		_title = title;
		_parent = parent;

		setOpaque(false);
		JPanel root = new JPanel(new BorderLayout());
		root.setOpaque(false);

		_lineNumbers = new LineNumbersTextArea();
		root.add(_lineNumbers, BorderLayout.LINE_START);

		_text = new JTextAreaWithFontMetrics();
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
		_textSelection = new Selection(() -> _fromLine);
		_text.addCaretListener(_textSelection);

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
		_selectionHighlight = new SelectionHighlight(_text);

		// floating buttons
		Box buttons = Box.createVerticalBox();
		_floatingButtons = Box.createHorizontalBox();
		buttons.add(_floatingButtons);
		buttons.add(Box.createVerticalGlue());
		_floatingButtons.add(Box.createHorizontalGlue());
		_gridToggle = new JToggleButton("\u25A6");
		_floatingButtons.add(_gridToggle);
		_gridToggle.addActionListener((e) -> setGridView(_gridToggle.isSelected(), false));
		ContextMenu.addAction(_gridToggle, "Change delimiter", this::changeGridDelimiter);
		ContextMenu.addAction(_gridToggle, "Change header", this::changeGridHeader);
		JButton perspectiveBtn = new JButton("\uD83D\uDCC8");
		perspectiveBtn.addActionListener(e -> openInPerspective());
		_floatingButtons.add(perspectiveBtn);
		_floatingButtons.add(Box.createRigidArea(new Dimension(_scrollBar.getPreferredSize().width, 0)));

		// overlay layout
		setLayout(new OverlayLayout(this));
		add(buttons);
		add(root);

		// finishing
		Preferences.EXTRA_LINES.subscribe(this, (value) -> _extraLines = value);
		Preferences.LINEHEIGHT.subscribe(this, (value) -> recalcLineHeight());
		_highlightedLine.subscribe(this::highlightLine);
		ContextMenu.addActionCopy(this, _text, _lineNumbers);
		CaretLimiter.setup(_text, () -> {
			// _allLinesCount is always +1
			int allLinesCount = _allLinesCount - 1;
			if (allLinesCount < _lineCount)
				return allLinesCount;
			if (_fromLine + _lineCount < allLinesCount)
				return _lineCount;
			return allLinesCount - _fromLine;
		});
	}

	public void reload(Supplier<ProgressListener> progressListenerSupplier) {
		if (_textSource != null && _textSource.supportsReload() && !_textSource.isIndexing()) {
			if (_prevReloadRequest != null)
				_prevReloadRequest.cancel(false);
			_prevReloadRequest = _textSource.requestReload(() -> {
				if (_textSourceUnsubscribable != null)
					_textSourceUnsubscribable.unsubscribe();
				_textSourceUnsubscribable = _textSource.subscribeLineCount(this::setFileLineCount);
				return progressListenerSupplier.get();
			});
		}
	}

	private void changeGridDelimiter() {
		String delim = JOptionPane.showInputDialog(this, "Enter new grid delimiter character:", _header.getDelim());
		if (delim != null && !delim.isEmpty()) {
			if (delim.length() != 1)
				PopupUtils.popupError("Delimiter must be 1 character", "Invalid delimiter");
			else {
				char ch = delim.charAt(0);
				if (_header.setDelim(ch) && _gridView != null) {
					_gridView.setDelim(ch);
					_gridView.refresh();
				}
			}
		}
	}

	private void changeGridHeader() {
		String header = JOptionPane.showInputDialog(this, "Enter new grid header (use delimiter between columns):", _header.getHeader());
		if (header != null && !header.isEmpty()) {
			if (_header.setHeader(header) && _gridView != null) {
				_gridView.setHeader(header);
				_gridView.refresh();
			}
		}
	}

	private void setGridView(boolean active, boolean fromDetection) {
		if (active != _gridToggle.isSelected())
			_gridToggle.setSelected(active);
		if (active) {
			if (_gridView == null) {
				String header = requireHeader(fromDetection);
				if (header == null)
					return;
				try {
					_gridView = new JTextAreaGridView(this, header, _header.isOwnHeader());
				} catch (IllegalArgumentException e) {
					gridNotAvailable(fromDetection ? null : e.getLocalizedMessage());
					return;
				}
				_gridView.setFont(getFont());
				_gridView.setRowHeight(_lineHeight);
				_gridView.getTableHeader().setPreferredSize(new Dimension(_scrollPane.getWidth(), _lineHeight));
				_scrollPane.setViewportView(_gridView);
			}
		} else {
			if (_gridView != null) {
				_gridView = null;
				_scrollPane.setViewportView(_text);
				refreshHighlights();
			}
		}
	}

	private void gridNotAvailable(String reason) {
		_gridToggle.setSelected(false);
		if (reason != null)
			JOptionPane.showMessageDialog(this, reason, "Grid view not available", JOptionPane.INFORMATION_MESSAGE);
	}

	private String requireHeader(boolean fromDetection) {
		String header = _header.getHeader();
		if (header.isEmpty()) {
			invokeLater(() -> gridNotAvailable(fromDetection ? null : "Text does not seems to be tabular data across the first " + _header.getCheckTarget() + " lines"));
			return null;
		}
		if (!_header.isAssured() && !fromDetection) {
			try {
				AtomicReference<String> newHeader = new AtomicReference<>(header);
				header = null;
				Runnable confirm = () -> {
					if (JOptionPane.showConfirmDialog(this,
							"Detected header is not certain, do you want to use it?\n" +
									ellipsize(newHeader.get(), 80),
							"Use uncertain header?",
							JOptionPane.YES_NO_OPTION
					) != JOptionPane.YES_OPTION) {
						newHeader.set(null);
						_gridToggle.setSelected(false);
					} else
						_header.setAssured(true);
				};
				if (EventQueue.isDispatchThread())
					confirm.run();
				else
					EventQueue.invokeAndWait(confirm);
				header = newHeader.get();
			} catch (InterruptedException | InvocationTargetException ignored) {}
		}
		return header;
	}

	/** Will open finos perspective in a standalone browser window.*/
	public void openInPerspective() {
		_textSource.requestStream((stream) -> {
			String header = requireHeader(false);
			if (header != null && !_header.isOwnHeader())
				stream = Stream.concat(Stream.of(requireHeader(false)), stream);
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
		else {
			int lineCount = _extraLines.apply(_lineCount);
			if (fromLine >= _allLinesCount - lineCount)
				fromLine = max(0, _allLinesCount - lineCount);
		}
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
		setFromLine(0);
	}

	public void toEnding() {
		setFromLine(_allLinesCount - _extraLines.apply(_lineCount));
	}

	public void centerOn(int line) {
		setFromLine(line - _extraLines.apply(_lineCount));
		_highlightedLine.next(line);
	}

	@Override
	public void setFont(Font f) {
		_text.setFont(f);
		if (_text.getDocument() != null)
			recalcLineHeight();
		_lineNumbers.setFont(f);
	}

	public void setTextSource(TextSource textSource) {
		if (_textSource == textSource)
			return;
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
		TextSource old = _textSource;
		_textSource = textSource;
		if (old != null) {
			if (!RangedTextSource.areCorrelated(old, textSource))
				old.close();
		}
		if (textSource != null) {
			_header = new HeaderDetector(textSource, this::detectHeaderDone, _parent == null ? null : _parent._header);
			setGridView(false, false);
			_header.detect();
			_fromLine = 0;
			_allLinesCount = 0;
			_textSourceUnsubscribable = textSource.subscribeLineCount(this::setFileLineCount);
			requireText();
			// I want to check sections only once and after the file has been loaded
			if (_sections == null) {
				asyncTask(() -> {
					if (_textSource == textSource) {
						try {
							textSource.getLineCount();
							invokeLater(this::checkSections);
						} catch (ExecutionException | InterruptedException ignored) {}
					}
				});
			}
		}
	}

	private void checkSections() {
		_sections = _textSource.sections();
		if (_sections == null || _sections.isEmpty())
			return;
		Section all = new Section("All", 0, _allLinesCount);
		Section[] options = Stream.concat(Stream.of(all), _sections.stream()).toArray(Section[]::new);
		JComboBox<Section> comboBox = new JComboBox<>(options);
		comboBox.setMaximumSize(_floatingButtons.getSize());
		_floatingButtons.add(comboBox, 1);
		TextSource origTextSource = _textSource;
		Map<Section, TextSource> sections = new HashMap<>();
		comboBox.addActionListener(e -> {
			Section sec = (Section)comboBox.getSelectedItem();
			if (sec == null || sec == all)
				setTextSource(origTextSource);
			else
				setTextSource(sections.computeIfAbsent(sec, (k) -> new RangedTextSource(origTextSource, k.from, k.to)));
		});
	}

	private void detectHeaderDone() {
		if ((_title != null && Preferences.AUTO_GRID.get() && Pattern.compile("\\.[tc]sv$", Pattern.CASE_INSENSITIVE).matcher(_title).find())
				|| (_textSource.mayHaveTabularData() && Preferences.AUTO_TAB_GRID.get() && !_header.getHeader().isEmpty())) {
			invokeLater(() -> setGridView(true, true));
		}
	}

	private void setFileLineCount(int lineCount) {
		// +1 to ensure last line is shown even if window borders fall across it
		if (_allLinesCount != ++lineCount) {
			int t = _allLinesCount;
			_allLinesCount = lineCount;
			recalcScrollBarMaximum();
			_scrollBar.setEnabled(true); // TODO: moving this line below disables scrollbar
			// if _allLinesCount (t) was 0 I'm loading from setTextSource and a requireText is in progress
			if (t > 0 && _fromLine <= lineCount && lineCount <= _fromLine + _lineCount)
				requireText();
			reNumerate();
		}
	}

	private void recalcScrollBarMaximum() {
		_scrollBar.setMaximum(_allLinesCount - _extraLines.apply(_lineCount));
	}

	private void recalcLineHeight() {
		final int newValue = _text.getFontMetrics(_text.getFont()).getHeight();
		if (newValue != _lineHeight) {
			_lineHeight = newValue;
			recalcLineCount();
		}
	}

	private void recalcLineCount() {
		final int newValue = 1 + (int)floor((float)getHeight() / (float)_lineHeight);
		if (newValue != _lineCount) {
			_lineCount = newValue;
			_scrollBar.setBlockIncrement(newValue);
			recalcScrollBarMaximum();
			requireText();
		}
	}

	private void refreshHighlights() {
		_selectionHighlight.refresh();
		highlightLine(_highlightedLine.get());
	}

	private void requireText() {
		if (_textSource == null)
			_text.setText("");
		else {
			if (_prevRequireTextRequest != null)
				_prevRequireTextRequest.cancel(false);
			_prevRequireTextRequest = _textSource.requestText(_fromLine, _lineCount, (text) -> {
				_prevRequireTextRequest = null;
				_textSelection.enabled = false;
				_text.setText(text);
				if (_gridView == null)
					refreshHighlights();
				else
					_gridView.refresh();
				invokeLater(() -> {
					_scrollPane.getHorizontalScrollBar().setValue(0);
					_textSelection.restore(_fromLine, _lineCount, _text);
					_textSelection.enabled = true;
				});
				reNumerate();
			});
		}
	}

	private void highlightLine(Integer highlightedLine) {
		Highlighter highlighter = _text.getHighlighter();

		// remove old highlight
		if (_lineHighlight != null) {
			highlighter.removeHighlight(_lineHighlight);
			_lineHighlight = null;
		}

		// stop if no highlight
		if (highlightedLine == null)
			return;

		// highlight current text area
		try {
			int line = highlightedLine - _fromLine;
			if (line >= 0 && line <= _lineCount) {
				int start = _text.getLineStartOffset(line);
				_lineHighlight = highlighter.addHighlight(
						start,
						_text.getLineEndOffset(line),
						new DefaultHighlighter.DefaultHighlightPainter(_text.getSelectionColor())
				);
				// _text.setCaretPosition(start);
			}
		} catch (BadLocationException e) {
			e.printStackTrace(System.err);
		}

		// eventually highlight parent text area
		if (_parent != null && Preferences.HLTYPE.get() == HighlightType.ALL_HIERARCHY) {
			Integer parentLine = _textSource.getSrcLine(highlightedLine);
			if (parentLine != null)
				_parent.centerOn(parentLine);
		}
	}

	private void reNumerate() {
		_lineNumbers.renumerate(
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
					if (e.getClickCount() == 1) {
						e.consume();
						if (e.getButton() == MouseEvent.BUTTON1) {
							// listener.accept(_fromLine + (e.getY() / _lineHeight));
							try {
								listener.accept(_fromLine + _text.getLineOfOffset(_text.getCaretPosition()));
								return;
							} catch (BadLocationException ex) {
								ex.printStackTrace(System.err);
							}
						}
						_highlightedLine.next(null);
					}
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
		setHighlightedLine(line, true);
	}

	public void setHighlightedLine(Integer line, boolean scroll) {
		_highlightedLine.next(line);
		if (scroll && line != null) {
			if (line < _fromLine)
				setFromLine(line);
			else if (line > (_fromLine + _lineCount))
				setFromLine(line - _lineCount + 1);
		}
	}

	public void setHighlightedText(String text) {
		_selectionHighlight.setText(text);
	}

	public void onClose() {
		_textSource.close();
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

	public String getDisplayedText() {
		return _text.getText();
	}

	public String getSelectedText() {
		String res = _text.getSelectedText();
		if (res == null)
			return _selectionHighlight.getText();
		return res;
	}

	public int getLineHeight() {
		return _lineHeight;
	}
}
