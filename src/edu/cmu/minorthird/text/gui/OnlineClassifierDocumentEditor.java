package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/** Interactivly edit the documents for an OnlineLearning Experiment
 *
 * @author Cameron Williams
 */

public class OnlineClassifierDocumentEditor extends ViewerTracker
{

  public static final String LABEL_DOCUMENT = "-choose label-";

  // internal state
  private String importType, exportType;
  private JLabel ioTypeLabel;
  private Span[] editedSpans;
    private String[] spanLabels;
    private String[] addedLabels;
  private int editSpanCursor = -1; // indicates nothing selected
  private boolean readOnly = false;
    private SpanFeatureExtractor fe = null;
    private ClassifierLearner learner = null;
    private String spanType = null;
    private String learnerName = "";
    public OnlineClassifierDocumentEditor ocdEditor;
    private ClassifierAnnotator ann = null;
    private TextBaseViewer tbViewer = null;

  // buttons
  JComboBox labelBox = new JComboBox();
    JButton addButton = new JButton(new AddSelection("Add Doc(s)"));
    JButton classifierButton = new JButton(new GetClassifier("Show Classifier"));
    JButton saveAnnButton = new JButton(new SaveAnnotator("Save Annotator"));
    JButton resetButton = new JButton(new Reset("Reset"));
    JButton completeButton = new JButton(new CompleteTraining("Complete Training"));
    JButton thisUpButton = new JButton(new MoveOnlineDocumentCursor("Up", -1));
    JButton thisDownButton = new JButton(new MoveOnlineDocumentCursor("Down", +1));

  private ArrayList buttonsThatChangeStuff = new ArrayList();

  /**
   * @param viewLabels a superset of editLabels which may include some additional read-only information
   * @param editLabels the labels being modified
   * @param documentList the document Span being edited is associated with
   * the selected entry of the documentList.
   * @param spanPainter used to repaint documentList elements
   * @param statusMsg a JLabel used for status messages.
   */
  public OnlineClassifierDocumentEditor(
    TextLabels viewLabels,
    TextBaseViewer tbViewer,
    MutableTextLabels editLabels,
    JList documentList,
    SpanPainter spanPainter,
    StatusMessage statusMsg,
    ClassifierAnnotator ann,
    ClassifierLearner learner,
    String learnerName)
  {
    super(viewLabels, editLabels, documentList, spanPainter, statusMsg);

    this.tbViewer = tbViewer;
    TextBase tb = editLabels.getTextBase();
    int tbSize = tb.size();
    editedSpans = new Span[tbSize];
    spanLabels = new String[tbSize];
    addedLabels = new String[tbSize];
    for(int i=0; i<tbSize; i++) {
	editedSpans[i] = null;
	spanLabels[i] = null;
	addedLabels[i] = null;
    }
    
    this.spanType = ((ClassifierAnnotator)ann).getLearnedSpanType();
    if(spanType == null) throw new IllegalArgumentException("The annotator must be trained on a Span Type");
	
    this.fe = ann.getFE();
	
    if(!(learner instanceof OnlineBinaryClassifierLearner)) throw new IllegalArgumentException("The learner must be an OnlineBinaryClassifierLearner");
    Classifier c = ann.getClassifier();
    if(!(c instanceof Hyperplane)) throw new IllegalArgumentException("The classifier must be an instance of Hyperplane");
    ((OnlineBinaryClassifierLearner)learner).addClassifier((Hyperplane)c);

    this.ann = new ClassifierAnnotator(fe,learner.getClassifier(),spanType,null,null);
    ann.setLearnedSpanType(spanType);
    ann.setClassifierLearner(learnerName);
    this.learner = learner;
    this.learnerName = learnerName;
    init();
    ocdEditor = this;
  }

  private void init()
  {
    this.importType = this.exportType = null;
    this.ioTypeLabel = new JLabel("Types: [None/None]");

    initLayout();
    labelBox.addItem(spanType);
    labelBox.addItem("NOT" + spanType);
    labelBox.addActionListener(new LabelDocument("Label Document"));

    loadSpan(nullSpan());
  }

  private void initLayout()
  {
    //
    // layout stuff
    //
    setLayout(new GridBagLayout());
    GridBagConstraints gbc;

    int col = 0;
    gbc = new GridBagConstraints();

    
    //------------- up button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(thisUpButton, gbc);

    //------------- down button ------------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(thisDownButton, gbc);

    //------------ label box -------------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    labelBox.addItem(LABEL_DOCUMENT);
    add(labelBox, gbc);

    //------------- add document button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(addButton, gbc);

    //------------- get classifier button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(classifierButton, gbc);

    //------------- save annotator button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(saveAnnButton, gbc);

    //------------- reset button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(resetButton, gbc);

    //------------- complete training button -----------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(completeButton, gbc);

    //----------- save button -------------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.weighty = 0.0;
    gbc.gridx = ++col;
    gbc.gridy = 2;
    add(saveButton, gbc);
    buttonsThatChangeStuff.add(saveButton);
    //System.out.println("create saveButton: saveAsFile=" + saveAsFile + " enabled: " + (saveAsFile != null));
    saveButton.setEnabled(saveAsFile != null);

    //------------- editorHolder ---------------//
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = col;
    add(editorHolder, gbc);
  }

  /** Set mode to read-only or not.  In read-only mode, the
   * document viewed has the same highlighting as in the
   * documentList.  In write mode, the "truth" spans
   * are shown, and the "guess" spans are imported.
   */
  public void setReadOnly(boolean readOnly)
  {
    for (Iterator i = buttonsThatChangeStuff.iterator(); i.hasNext();)
    {
      JButton button = (JButton) i.next();
      button.setEnabled(readOnly ? false : true);
    }
    this.readOnly = readOnly;
  }

  /** Declare which types are being edited. */
  public void setTypesBeingEdited(String inType, String outType)
  {
    this.importType = inType;
    this.exportType = outType;
    ioTypeLabel.setText("Edit: " + importType + "/" + exportType);
  }


  protected void loadSpanHook()
  {
    if (readOnly && !DUMMY_ID.equals(documentSpan.getDocumentId()))
    {
      importDocumentListMarkup(documentSpan.getDocumentId());
    }
    Keymap keymap = editorPane.getKeymap(JTextComponent.DEFAULT_KEYMAP);
  }

  /** Toggles readOnly status */
  private class ReadOnlyButton extends AbstractAction
  {
    public ReadOnlyButton(String msg)
    {
      super(msg);
    }

    public void actionPerformed(ActionEvent event)
    {
      setReadOnly(!readOnly);
      if (documentSpan != null) loadSpan(documentSpan);
    }
  }

    /** Add span associated with selected text. */
    private class LabelDocument extends AbstractAction
    {
	public LabelDocument(String msg)
	{
	    super(msg);
	}
      
	public void actionPerformed(ActionEvent event)
	{
	    JComboBox cb = (JComboBox)event.getSource();
	    String type = (String)cb.getSelectedItem();
	    int docIndex = documentList.getMinSelectionIndex();
	    if(addedLabels[docIndex] != null) {
		statusMsg.display("Cannot change label: Document has already been added to Classifier as: " +addedLabels[docIndex]);
	    }else if(type.equals(LABEL_DOCUMENT)) {
		editedSpans[docIndex] = null;
		spanLabels[docIndex] = null;
	    }else if(spanLabels[docIndex] == null || !(spanLabels[docIndex].equals(type))) {
		editedSpans[docIndex] = documentSpan;
		spanLabels[docIndex] = type;
		statusMsg.display("Document has been labeled as: " + type);
	    } 
	}
    }


  /** Add span associated with selected text. */
  private class AddSelection extends AbstractAction
  {
    public AddSelection(String msg)
    {
      super(msg);
    }

    public void actionPerformed(ActionEvent event)
      {
	  for(int x=0; x<editedSpans.length; x++ ) {
	      Span s = editedSpans[x];
	      String spanType = spanLabels[x];
	      if(addedLabels[x] == null && s != null) {
		  editLabels.addToType(s,spanType);		  
	      }
	  }
	BasicDataset data = (BasicDataset)toDataset(editLabels, fe, spanType);
	for(Example.Looper i=data.iterator(); i.hasNext(); ) {
	    Example ex = i.nextExample();
	    learner.addExample(ex);
	}
	for(int j=0; j<spanLabels.length; j++) {
	    addedLabels[j] = spanLabels[j];
	}
	ann = new ClassifierAnnotator(fe,learner.getClassifier(),spanType,null,null);
	//TextLabels newLabels = /*ann.annotatedCopy*/((TextLabels)editLabels);
	//tbViewer.updateTextLabels((TextLabels)editLabels);
	statusMsg.display("Documents added to learner");
    }
  }

    /** Build a classification dataset from the necessary inputs. 
     */
    public Dataset 
	toDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanType)
    {
	// use this to print out a summary
	Map countByClass = new HashMap();

	NestedTextLabels safeLabels = new NestedTextLabels(textLabels);

	// binary dataset - anything labeled as in this type is positive

	if (spanType!=null) {
	    Dataset dataset = new BasicDataset();
	    for (int i=0; i<editedSpans.length; i++ ) {
		Span s = editedSpans[i];
		if(addedLabels[i] == null && s != null) {
		    int classLabel = textLabels.hasType(s,spanType) ? +1 : -1;
		    int negClassLabel = textLabels.hasType(s,"NOT"+spanType) ? +1 : -1;
		    if(classLabel > 0 || negClassLabel > 0) {
			String className = classLabel<0 ? ExampleSchema.NEG_CLASS_NAME : ExampleSchema.POS_CLASS_NAME;
			dataset.add( new Example( fe.extractInstance(safeLabels,s), ClassLabel.binaryLabel(classLabel)) );
			Integer cnt = (Integer)countByClass.get( className );
			if (cnt==null) countByClass.put( className, new Integer(1) );
			else countByClass.put( className, new Integer(cnt.intValue()+1) );
		    }
		}
	    }
	    System.out.println("Number of examples by class: "+countByClass);
	    return dataset;
	}
	return null;
    }

     /** Return the current classifier */
  private class GetClassifier extends AbstractAction
  {
    public GetClassifier(String msg)
    {
      super(msg);
    }

    public void actionPerformed(ActionEvent event)
    {	
	new ViewerFrame("Classifier", new SmartVanillaViewer(learner.getClassifier()));
	statusMsg.display("Getting the Classifier");
    }
  }

    public void saveAnn(File file,String format) throws IOException {
	System.out.println("In SAVE  AS");
	//if (!format.equals(FORMAT_NAME)) throw new IllegalArgumentException("illegal format "+format);
	try {	    	    
	    try {
		IOUtil.saveSerialized((Serializable)ann,file);
	    } catch (IOException e) {
		throw new IllegalArgumentException("can't save to "+file+": "+e);
	    }
	} catch (Exception e) {
	    System.out.println("Error Opening Excel File");
	    e.printStackTrace();
	}	
    }

       /** Return the current classifier */
  private class SaveAnnotator extends AbstractAction
  {
      public SaveAnnotator(String msg) {
	  super(msg);
      }

      public void actionPerformed(ActionEvent event)
      {	
	  JFileChooser chooser = new JFileChooser();
	  int returnVal = chooser.showSaveDialog(ocdEditor);
	  if(returnVal == JFileChooser.APPROVE_OPTION) {
	      try {
		  FileFilter filter = chooser.getFileFilter();
		  String fmt = filter.getDescription();
		  String ext = ".ann";
		  File file0 = chooser.getSelectedFile();
		  File file = 
		      (file0.getName().endsWith(ext)) ? 
		      file0 : new File(file0.getParentFile(), file0.getName()+ext);
		  ocdEditor.saveAnn( file, filter.getDescription() );
	      } catch (Exception ex) {
		  statusMsg.display("Error Saving Annotator");
	      }
	  }
	  //statusMsg.display("Saving the Annotator");
      }
      
  }

    /** Forget about all previous examples */
  private class Reset extends AbstractAction
  {
    public Reset(String msg)
    {
      super(msg);
    }

    public void actionPerformed(ActionEvent event)
    {
	learner.reset();
	statusMsg.display("Learner Reset - previous examples forgotten");
    }
  }

       /** Complete Training - Announce that no more examples will be coming */
  private class CompleteTraining extends AbstractAction
  {
    public CompleteTraining(String msg)
    {
      super(msg);
    }

    public void actionPerformed(ActionEvent event)
    {
	learner.completeTraining();
	statusMsg.display("Training Completed - no more examples will be added");
    }
  }

    protected void documentMessage(int nextCursor) 
    {
	if(nextCursor > -1 && nextCursor < spanLabels.length) {
	    if(addedLabels[nextCursor] != null) {
		statusMsg.display("Cannot change label: Document has already been added to Classifier as: " +addedLabels[nextCursor]);
		labelBox.setSelectedItem(addedLabels[nextCursor]);
	    } else if(spanLabels[nextCursor] != null) {
		statusMsg.display("Document is currently labeled: " + spanLabels[nextCursor]);
		labelBox.setSelectedItem(spanLabels[nextCursor]);
	    } else {
		statusMsg.display("This document has NOT been labeled");
		labelBox.setSelectedItem(LABEL_DOCUMENT);
	    }
	}
    }

    protected void loadSpan(Span span)
    {
	super.loadSpan(span);
	documentMessage(documentList.getMinSelectionIndex());
    }

     /** Move through list of spans */
    protected class MoveOnlineDocumentCursor extends AbstractAction
    {
        private int delta;

        public MoveOnlineDocumentCursor(String msg, int delta)
        {
            super(msg);
            this.delta = delta;
        }

        public void actionPerformed(ActionEvent event)
        {
	    int nextCursor;
            synchronized (documentList)
            {
                int currentCursor = documentList.getSelectedIndex();
                // if nothing's selected, pretend it was the first thing
                if (currentCursor < 0) currentCursor = 0;
                nextCursor = currentCursor + delta;
                if (nextCursor < documentList.getModel().getSize() && nextCursor >= 0)
                {
                    documentList.setSelectedIndex(nextCursor);
                }
            }
	    documentMessage(nextCursor);
        }
    }

  private class SpanPropertyViewer extends ComponentViewer
  {
    public JComponent componentFor(Object o)
    {
      final Span span = (Span)o;
      final JTabbedPane pane = new JTabbedPane();
      final JTextField propField = new JTextField(10);
      final JTextField valField = new JTextField(10);
      final JTable table = makePropertyTable(span);
      final JScrollPane tableScroller = new JScrollPane(table);
      final JButton addButton =
          new JButton(new AbstractAction("Insert Property") {
            public void actionPerformed(ActionEvent event) {
              editLabels.setProperty(span, propField.getText(), valField.getText());
              tableScroller.getViewport().setView(makePropertyTable(span));
              tableScroller.revalidate();
              pane.revalidate();
            }
          });
      GridBagConstraints gbc = fillerGBC();
      //gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth=3;
      final JPanel subpanel = new JPanel();
      subpanel.setLayout(new GridBagLayout());
      subpanel.add(tableScroller,gbc);
      subpanel.add(addButton,myGBC(0));
      subpanel.add(propField,myGBC(1));
      subpanel.add(valField,myGBC(2));
      pane.add("Properties",subpanel);
      pane.add("Span", new SmartVanillaViewer(span));
      return pane;
    }
    private GridBagConstraints myGBC(int col) {
      GridBagConstraints gbc = fillerGBC();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = col;
      gbc.gridy = 1;
      return gbc;
    }
    private JTable makePropertyTable(final Span span)
    {
      //System.out.println("editLabels="+editLabels);
      //System.out.println("spanProps="+editLabels.getSpanProperties());
      Object[] spanProps = editLabels.getSpanProperties().toArray();
      Object[][] table = new Object[spanProps.length][2];
      for (int i=0; i<spanProps.length; i++) {
        table[i][0] = spanProps[i];
        table[i][1] = editLabels.getProperty(span,(String)spanProps[i]);
      }
      String[] colNames = new String[] { "Property", "Property's Value" };
      return new JTable(table,colNames);
    }
  }

  private Span getEditSpan(int k)
  {
      for (int i=0; i<editedSpans.length; i++)
    {
      Span s = editedSpans[i];
      if (k-- == 0) return s;
    }
    throw new IllegalStateException("bad editedSpan index " + k);
  }
}