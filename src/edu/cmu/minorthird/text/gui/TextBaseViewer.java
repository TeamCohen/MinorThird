package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.*;
import java.io.File;

import org.apache.log4j.Logger;

/** Interactively view the contents of a TextBase and TextLabels.
 *
 * @author William Cohen
 */

public class TextBaseViewer extends JComponent
{
  public static final String NULL_TRUTH_ENTRY = "-compare to-";
  public static final String NULL_DISPLAY_TYPE = "-top-";
	
  // links to outside
  private final StatusMessage statusMsg;
	
  // internal state
  private final TextBase base;
  private TextLabels labels;
  // components
  private JList documentList;
  private JSlider documentCellHeightSlider;
  private JComboBox displayedTypeBox;
  private JCheckBox editedOnlyCheckBox;
  private JComboBox guessBox;
  private JComboBox truthBox;
  private HighlightAction highlightAction;
  private static Logger log = Logger.getLogger(TextBaseViewer.class);
	
  public JList getDocumentList()
  { return documentList; }
	
  public JComboBox getGuessBox()
  { return guessBox; }
	
  public JComboBox getTruthBox()
  { return truthBox; }
	
  public JComboBox getDisplayedTypeBox()
  { return displayedTypeBox; }
	
  public SpanPainter getSpanPainter()
  { return highlightAction; }
	
  /** change the text labels*/
  public void updateTextLabels(TextLabels newLabels)
  {
    this.labels = newLabels;
    highlightAction.paintDocument(null); // repaint everything
  }
	
  public TextBaseViewer(TextBase base, TextLabels labels, StatusMessage statusMsg)
  { this(base, labels, null, statusMsg); }
	
  public TextBaseViewer(TextBase base, TextLabels labels, String displayType, StatusMessage statusMsg)
  {
    this.base = base;
    this.labels = labels;
    this.statusMsg = statusMsg;
    initializeLayout(displayType);
  }
	
  // lay out the main window
  private void initializeLayout(String displayType)
  {
    documentList = new JList();
    documentList.setFixedCellWidth(760);
    resetDocumentList(labels, displayType, false);
		
    // select 'guess' spans
    guessBox = new JComboBox();
    guessBox.setEditable(false);
    for (Iterator i = labels.getTypes().iterator(); i.hasNext();)
    {
      String type = (String)i.next();
      guessBox.addItem(type);
    }
    // select 'truth' spans
    truthBox = new JComboBox();
    //truthBox.setEditable(false);
    truthBox.addItem(NULL_TRUTH_ENTRY);
    log.debug("types: " + labels.getTypes());
    for (Iterator i = labels.getTypes().iterator(); i.hasNext();)
    {
      truthBox.addItem(i.next().toString());
    }
    // select spans to display in the documentList
    displayedTypeBox = new JComboBox();
    displayedTypeBox.setEditable(false);
    displayedTypeBox.addItem(NULL_DISPLAY_TYPE);
    for (Iterator i = labels.getTypes().iterator(); i.hasNext();)
    {
      displayedTypeBox.addItem(i.next().toString());
      if (displayType != null) displayedTypeBox.setSelectedItem(displayType);
    }
    ResetDocumentListAction resetDocumentListAction = new ResetDocumentListAction();
    displayedTypeBox.addActionListener(resetDocumentListAction);
    editedOnlyCheckBox = new JCheckBox("Unlabeled");
    editedOnlyCheckBox.addActionListener(resetDocumentListAction);
		
    // create the highlightAction
    highlightAction = new HighlightAction("Highlight", guessBox, truthBox, documentList);
		
    // action for highlighting happens when guessBox, truthBox changes or button pressed
    guessBox.addActionListener(highlightAction);
    truthBox.addActionListener(highlightAction);
    JButton highlightButton = new JButton(highlightAction);
		
    documentCellHeightSlider = new DocumentCellHeightSlider(documentList);
		
    //
    // layout stuff
    //
    setPreferredSize(new Dimension(800, 600));
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); // 4 = border width
    setLayout(new GridBagLayout());
    GridBagConstraints gbc;
		
    // row 2 - the toolbar
    int col = 0;
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(highlightButton, gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(guessBox, gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(truthBox, gbc);
		
    gbc = new GridBagConstraints();
    gbc.weightx = gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new JLabel("H:"), gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(documentCellHeightSlider, gbc);
		
    gbc = new GridBagConstraints();
    gbc.weightx = gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new JLabel("W:"), gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new DocumentCellWidthSlider(documentList), gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new JButton(new ZoomAction("Font-2", -2, documentList)), gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new JButton(new ZoomAction("Font+2", +2, documentList)), gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(displayedTypeBox, gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(editedOnlyCheckBox, gbc);
		
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(new ContextWidthSlider(documentList), gbc);
		
    // row 1
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 2.0;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = col;
    add(new JScrollPane(documentList), gbc);
		
  }
	
  synchronized private void 
	resetDocumentList(TextLabels labels, String displayType, boolean onlyEditedSpans)
  {
    // collect all the top-level spans, and put them in a JList
    log.debug("reset for type " + displayType);
    ArrayList spans = new ArrayList();
    Span.Looper i = null;
    if (displayType == null || NULL_DISPLAY_TYPE.equals(displayType)) {
      i = base.documentSpanIterator();
		}  else {
      i = labels.instanceIterator(displayType);
		}
		
		// decide what type to filter on, if any
		String truthType = NULL_TRUTH_ENTRY;
		if (truthBox!=null) truthType = (String)truthBox.getSelectedItem();
		if (NULL_TRUTH_ENTRY.equals(truthType)) onlyEditedSpans=false;
		
    while (i.hasNext())  {
      Span s = i.nextSpan();
			if (!onlyEditedSpans) {
				spans.add(s);
			} else {
				// see if this span has been edited
				boolean wasEdited = false;
				for (Span.Looper j=labels.closureIterator(truthType,s.getDocumentId()); j.hasNext(); ){
					Span t = j.nextSpan();
					if (t.contains(s)) wasEdited = true;
				}
				if (!wasEdited) spans.add(s);
			}
		}
		if (spans.size() == 0) {
			statusMsg.display("no" + (onlyEditedSpans ? " unedited" : "") + " spans of type " + displayType);
		}	else {
			synchronized (documentList) {
				statusMsg.display("Displaying "+spans.size()+(onlyEditedSpans ? " unedited" : "")
													+" spans of type "+displayType);
				Span[] spanArray = (Span[])spans.toArray(new Span[spans.size()]);
				if (documentList == null) documentList = new JList();
				documentList.setVisible(false);
				documentList.setListData(spanArray);
				SpanRenderer renderer = new SpanRenderer(spanArray);
				documentList.setCellRenderer(renderer);
				documentList.setVisible(true);
				documentList.repaint();
				if (highlightAction!=null) 
					highlightAction.paintDocument(null); // repaint everything
				statusMsg.display("Displaying "+spans.size()+(onlyEditedSpans ? " unedited" : "")
													+" spans of type "+displayType);
			}
		}
  }
	
	
	/**
	 * highlights text in the spans displayed in documentList based on
	 * the options in truthBox, guessBox */
	private class HighlightAction extends AbstractAction implements SpanPainter
	{
		private JComboBox guessBox,truthBox;
		private JList documentList;
		private SpanDifference spanDifference;
		public HighlightAction(String msg, JComboBox guessBox, JComboBox truthBox, JList documentList)
		{
			super(msg);
			this.guessBox = guessBox;
			this.truthBox = truthBox;
			this.documentList = documentList;
		}
		public void actionPerformed(ActionEvent event)
		{
			synchronized (documentList)
			{
				paintDocument(null); // paint everything
			}
			DecimalFormat fmt = new DecimalFormat("##0.000");
			double tr = spanDifference.tokenRecall()*100; 
			double tp = spanDifference.tokenPrecision()*100; 
			double sr = spanDifference.spanRecall()*100; 
			double sp = spanDifference.spanPrecision()*100; 
			statusMsg.display("Token recall: "+fmt.format(tr)
												+" precision: "+fmt.format(tp)
												+" Span recall: "+fmt.format(sr)
												+" precision: "+fmt.format(sp));
		}
		
		public void paintDocument(String documentId)
		{
			synchronized (documentList)
			{
				try
				{
					SpanRenderer renderer = (SpanRenderer) documentList.getCellRenderer();
					//System.out.println("TBV: call differenceIterator "+documentId);
					renderer.highlightDiffs(
						differenceIterator(documentId), documentId, fpColor(), fnColor(), tpColor(), mpColor());
					documentList.repaint(); // seems to be needed
				}
				catch (Exception e)
				{
					System.out.println("error: " + e);
					e.printStackTrace();
				}
			}
		}
		
		// colors for false pos, false neg, true pos, "maybe" pos
		public AttributeSet fpColor()
		{
			return nullTruthType() ? HiliteColors.yellow : HiliteColors.red;
		}
		
		public AttributeSet fnColor()
		{
			return nullTruthType() ? HiliteColors.yellow : HiliteColors.blue;
		}
		
		public AttributeSet tpColor()
		{
			return nullTruthType() ? HiliteColors.yellow : HiliteColors.green;
		}
		
		public AttributeSet mpColor()
		{
			return HiliteColors.yellow;
		}
		
		// figure out what spans to update...
		public SpanDifference.Looper differenceIterator(String documentId)
		{
			String guessType = (String) guessBox.getSelectedItem();
			String truthType = (String) truthBox.getSelectedItem();
			Span.Looper guessLooper =
				documentId == null ? labels.instanceIterator(guessType) : labels.instanceIterator(guessType, documentId);
			if (nullTruthType())
			{
				Span.Looper nullLooper = new BasicSpanLooper(new HashSet().iterator());
				spanDifference = new SpanDifference(guessLooper, nullLooper);
				return spanDifference.differenceIterator();
			}
			else
			{
				//System.out.println("TBV: truthType: "+truthType+" document:" + documentId);
				
				Span.Looper truthSpanLooper =
					documentId==null ? labels.instanceIterator(truthType) : labels.instanceIterator(truthType, documentId);
				Span.Looper closureSpanLooper =
					documentId==null ? labels.closureIterator(truthType) : labels.closureIterator(truthType, documentId);
				
				/*
					System.out.println("TBV: iterating over "+truthType+" spans");
					for (Span.Looper ii=labels.instanceIterator(truthType,documentId); ii.hasNext(); ) {
					System.out.println("TBV: span type "+truthType+": "+ii.nextSpan());
					}
					for (Span.Looper ii=labels.closureIterator(truthType,documentId); ii.hasNext(); ) {
					System.out.println("TBV: closure span "+ii.nextSpan());
					}
				*/
				
				spanDifference = new SpanDifference(guessLooper, truthSpanLooper, closureSpanLooper);
				return spanDifference.differenceIterator();
			}
		}
		
		private boolean nullTruthType()
		{
			return NULL_TRUTH_ENTRY.equals((String) truthBox.getSelectedItem());
		}
	}
	
	private class ResetDocumentListAction extends AbstractAction
	{
		public ResetDocumentListAction()  { super("Display");}
		public void actionPerformed(ActionEvent event)  {
			synchronized (documentList) {
				String type = (String) displayedTypeBox.getSelectedItem();
				resetDocumentList(labels, type, editedOnlyCheckBox.isSelected());
				documentList.repaint();
			}
		}
	}
	
	private class ZoomAction extends AbstractAction
	{
		private JList documentList;
		private int sizeDelta;
		public ZoomAction(String msg, int sizeDelta, JList documentList)
		{
			super(msg);
			this.sizeDelta = sizeDelta;
			this.documentList = documentList;
		}
		public void actionPerformed(ActionEvent event)
		{
			synchronized (documentList)
			{
				SpanRenderer renderer = (SpanRenderer) documentList.getCellRenderer();
				renderer.zoomFont(sizeDelta);
				documentList.repaint();
			}
		}
	}
	
	private class ContextWidthSlider extends JSlider
	{
		final private JList documentList;
		
		public ContextWidthSlider(final JList documentList)
		{
			super(0, 10, 0);
			this.documentList = documentList;
			addChangeListener(new ChangeListener()
				{
					public void stateChanged(ChangeEvent e)
					{
						synchronized (documentList)
						{
							ContextWidthSlider slider = (ContextWidthSlider) e.getSource();
							if (!slider.getValueIsAdjusting())
							{
								int value = slider.getValue();
								SpanRenderer renderer = (SpanRenderer) slider.documentList.getCellRenderer();
								renderer.setContextWidth(value);
								slider.documentList.repaint();
							}
						}
					}
				});
		}
	}
	
	private class DocumentCellHeightSlider extends JSlider
	{
		public DocumentCellHeightSlider(final JList documentList)
		{
			super(-1, 100, -1);
			addChangeListener(new ChangeListener()
				{
					public void stateChanged(ChangeEvent e)
					{
						synchronized (documentList)
						{
							DocumentCellHeightSlider slider = (DocumentCellHeightSlider) e.getSource();
							if (!slider.getValueIsAdjusting())
							{
								documentList.setFixedCellHeight(slider.getValue());
							}
						}
					}
				});
		}
	}
	
	private class DocumentCellWidthSlider extends JSlider
	{
		public DocumentCellWidthSlider(final JList vList)
		{
			super(-1, 760, 760);
			addChangeListener(new ChangeListener()
				{
					public void stateChanged(ChangeEvent e)
					{
						synchronized (vList)
						{
							DocumentCellWidthSlider slider = (DocumentCellWidthSlider) e.getSource();
							if (!slider.getValueIsAdjusting())
							{
								vList.setFixedCellWidth(slider.getValue());
							}
						}
					}
				});
		}
	}
	
	/** ListCellRenderer for the List of Documents.
	 * This has some additional methods for highlighting spans.
	 */
	private class SpanRenderer implements ListCellRenderer
	{
		// a span plus a color to show it in
		private class SpanMarkup implements Comparable
		{
			public Span span;
			public AttributeSet color;
			
			public SpanMarkup(Span span, AttributeSet color)
			{
				this.span = span;
				this.color = color;
			}
			
			public int compareTo(Object other)
			{
				SpanMarkup cspan = (SpanMarkup)other;
				return span.compareTo(cspan.span);
			}
		}
		
		private JComponent spanComponents[]; // component for each span
		private SpanDocument spanDocs[];     // doc to hold each span
		private TreeSet[] spanMarkups;       // markup for each span
		private boolean[] spanIsDirty;       // true if document needs to be updated
		private Span[] spans;                // action spans
		private Map indexOfSpanMap;          // maps span to index in arrays above
		private Map spansWithDocumentMap;   // find all spans with a particular documentId
		private Font currentFont;            // font being used
		private int contextWidth = 0;        // how much context to show
		
		public SpanRenderer(Span[] spans)
		{
			this.spans = spans;
			
			// work out default font
			currentFont = new JTextPane().getFont();
			synchronized (documentList)
			{
				// cache out renderers and rendered documents for each span
				spanComponents = new JComponent[spans.length];
				spanDocs = new SpanDocument[spans.length];
				spanMarkups = new TreeSet[spans.length];
				spanIsDirty = new boolean[spans.length];
				spansWithDocumentMap = new HashMap();
				indexOfSpanMap = new HashMap();
				for (int i = 0; i < spans.length; i++)
				{
//            log.debug("render span loop on span: " + spans[i].asString());
					spanDocs[i] = new SpanDocument(spans[i], contextWidth);
					spanMarkups[i] = new TreeSet();
					spanIsDirty[i] = false;
					refreshSpanComponent(i);
					// update indexOfSpanMap
					indexOfSpanMap.put(spans[i], new Integer(i));
					// update spansWithDocumentMap
					String documentId = spans[i].getDocumentId();
					ArrayList spansWithDocument = (ArrayList)spansWithDocumentMap.get(documentId);
					if (spansWithDocument == null)
					{
						spansWithDocumentMap.put(documentId, (spansWithDocument = new ArrayList()));
					}
					spansWithDocument.add(spans[i]);
				}
			}
		}
		
		// used to force repaint of a component
		private void refreshSpanComponent(int i)
		{
			synchronized (documentList)
			{
				JTextPane pane = new JTextPane(spanDocs[i]);
				pane.setFont(currentFont);
				//spanComponents[i] = new JScrollPane(pane);
				spanComponents[i] = pane;
				spanComponents[i].setBorder(BorderFactory.createLineBorder(Color.black));
			}
		}
		
		public void setContextWidth(int contextWidth)
		{
			synchronized (documentList)
			{
				documentList.setVisible(false);
				this.contextWidth = contextWidth;
				for (int i = 0; i < spanComponents.length; i++)
				{
					spanDocs[i] = new SpanDocument(spans[i], contextWidth);
					spanIsDirty[i] = true;
					refreshSpanComponent(i);
				}
				documentList.setVisible(true);
				documentList.repaint();
			}
		}
		
		/** Implement the ListCellRenderer interface. */
		public Component getListCellRendererComponent(JList el, Object v, int index, boolean sel, boolean focus)
		{
			// handle synchronization errors?  there oughta be a better way!
			if (spanIsDirty == null || index >= spanIsDirty.length)
			{
				return new JLabel("sync error?");
			}
			synchronized (documentList)
			{
				if (spanIsDirty[index])
				{
					TreeSet marks = spanMarkups[index];
					spanDocs[index].resetHighlights();
					for (Iterator i = marks.iterator(); i.hasNext();)
					{
						SpanMarkup m = (SpanMarkup) i.next();
						spanDocs[index].highlight(m.span, m.color);
					}
					refreshSpanComponent(index);
					spanIsDirty[index] = false;
				}
				if (sel)
					spanComponents[index].setBorder(BorderFactory.createLineBorder(Color.blue, 2));
				else
					spanComponents[index].setBorder(BorderFactory.createLineBorder(Color.black, 2));
				return spanComponents[index];
			}
		}
		
		/** Highlight differences between two sets of spans. */
		public void
		highlightDiffs(SpanDifference.Looper i, String documentId,
									 AttributeSet fp, AttributeSet fn, AttributeSet tp, AttributeSet mp)
		{
			// to decode status into a color
			AttributeSet[] colorByStatus = new AttributeSet[SpanDifference.MAX_STATUS + 1];
			colorByStatus[SpanDifference.FALSE_POS] = fp;
			colorByStatus[SpanDifference.FALSE_NEG] = fn;
			colorByStatus[SpanDifference.TRUE_POS] = tp;
			colorByStatus[SpanDifference.UNKNOWN_POS] = mp;
			
			synchronized (documentList)
			{
				
				if (documentId != null)
				{
					// clear old markup from these spans
					ArrayList spansWithDocument = (ArrayList) spansWithDocumentMap.get(documentId);
					for (Iterator j = spansWithDocument.iterator(); j.hasNext();)
					{
						int index = ((Integer) indexOfSpanMap.get(j.next())).intValue();
						spanDocs[index].resetHighlights();
						spanMarkups[index] = new TreeSet();
						spanIsDirty[index] = true;
					}
				}
				else
				{
					// clear old markup from all spans
					for (int j = 0; j < spanDocs.length; j++)
					{
						if (!spanMarkups[j].isEmpty())
						{
							spanDocs[j].resetHighlights();
							spanMarkups[j] = new TreeSet();
							spanIsDirty[j] = true;
						}
					}
				}
				
				// highlight the differences
				while (i.hasNext())
				{
					Span diffSpan = i.nextSpan();
					int status = i.getStatus();
					String documentIdOfS = diffSpan.getDocumentId();
					ArrayList spansWithDocument = (ArrayList) spansWithDocumentMap.get(documentIdOfS);
					if (spansWithDocument != null)
					{
						for (Iterator j = spansWithDocument.iterator(); j.hasNext();)
						{
							// t is a span with the same document as diffSpan
							Span t = (Span) j.next();
							int indexOfT = ((Integer) indexOfSpanMap.get(t)).intValue();
							TreeSet marks = spanMarkups[indexOfT];
							marks.add(new SpanMarkup(diffSpan, colorByStatus[status]));
							spanIsDirty[indexOfT] = true;
						}
					}
				}
			}
		}
		
		/** Increase/decrease font size by delta */
		public void zoomFont(int delta)
		{
			synchronized (documentList)
			{
				int currentSize = currentFont.getSize();
				String newFont = currentFont.getFamily() + "-plain-" + (currentSize + delta);
				currentFont = Font.decode(newFont);
				statusMsg.display("current font is " + newFont);
				for (int i = 0; i < spanComponents.length; i++)
				{
					refreshSpanComponent(i);
				}
			}
		}
	}
	
	/** Pop up a frame for viewing the labels. */
	public static void view(TextLabels labels)
	{
		JFrame frame = new JFrame("TextBaseViewer");
		TextBase base = labels.getTextBase();
		
		StatusMessage statusMsg = new StatusMessage();
		TextBaseViewer viewer = new TextBaseViewer(base, labels, statusMsg);
		JComponent main = new StatusMessagePanel(viewer, statusMsg);
		frame.getContentPane().add(main, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args)
	{
		try {
			TextLabels labels = FancyLoader.loadTextLabels(args[0]);
			if (args.length>1) {
				MixupProgram p = new MixupProgram(new File(args[1]));
				p.eval((MonotonicTextLabels)labels, labels.getTextBase());
			}
			view(labels);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: TextBaseViewer key [mixupProgram]");
		}
	}
}
