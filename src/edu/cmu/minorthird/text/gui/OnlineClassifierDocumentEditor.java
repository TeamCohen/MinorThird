package edu.cmu.minorthird.text.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.filechooser.FileFilter;

import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.ClassifierAnnotator;
import edu.cmu.minorthird.text.learn.OnlineTextClassifierLearner;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Interactivly edit the documents for an OnlineLearning Experiment
 * 
 * @author Cameron Williams
 */

public class OnlineClassifierDocumentEditor extends ViewerTracker{
	
	static final long serialVersionUID=20080314L;

	public static final String LABEL_DOCUMENT="-choose label-";

	// internal state
	private String importType,exportType;

	private JLabel ioTypeLabel;

//	private int editSpanCursor=-1; // indicates nothing selected

	private boolean readOnly=false;

//	private SpanFeatureExtractor fe=null;

	private OnlineTextClassifierLearner textLearner=null;

//	private OnlineBinaryClassifierLearner learner=null;

	private String[] spanTypes=null;

//	private String learnerName="";

	public OnlineClassifierDocumentEditor ocdEditor;

	private TextBaseViewer tbViewer=null;

	public List<EditedSpan> editedSpans=null;

	public ClassifierAnnotator ann=null;

	// buttons
	JComboBox labelBox=new JComboBox();

	JButton addButton=new JButton(new AddSelection("Add Doc(s)"));

	JButton classifierButton=new JButton(new GetClassifier("Show Classifier"));

	JButton saveAnnButton=new JButton(new SaveAnnotator("Save TextLearner"));

	JButton resetButton=new JButton(new Reset("Reset"));

	JButton completeButton=new JButton(new CompleteTraining("Complete Training"));

	JButton thisUpButton=new JButton(new MoveOnlineDocumentCursor("Up",-1));

	JButton thisDownButton=new JButton(new MoveOnlineDocumentCursor("Down",+1));

	private List<JButton> buttonsThatChangeStuff=new ArrayList<JButton>();

	/**
	 * @param viewLabels
	 *          a superset of editLabels which may include some additional
	 *          read-only information
	 * @param editLabels
	 *          the labels being modified
	 * @param documentList
	 *          the document Span being edited is associated with the selected
	 *          entry of the documentList.
	 * @param spanPainter
	 *          used to repaint documentList elements
	 * @param statusMsg
	 *          a JLabel used for status messages.
	 */
	public OnlineClassifierDocumentEditor(OnlineTextClassifierLearner learner,
			TextLabels viewLabels,TextBaseViewer tbViewer,
			MutableTextLabels editLabels,JList documentList,SpanPainter spanPainter,
			StatusMessage statusMsg){
		super(viewLabels,editLabels,documentList,spanPainter,statusMsg);

		this.textLearner=learner;
		ann=textLearner.getAnnotator();
		this.editLabels=editLabels;
		TextBase tb=editLabels.getTextBase();
		this.spanTypes=learner.getTypes();

		// Initialize editedSpans to include any data that is already labeled
		editedSpans=new ArrayList<EditedSpan>();
		int index=0;
		for(Iterator<Span> j=tb.documentSpanIterator();j.hasNext();){
			Span s=j.next();
			for(int x=0;x<spanTypes.length;x++){
				if(editLabels.hasType(s,spanTypes[x]))
					editedSpans.add(new EditedSpan(s,spanTypes[x],index));
			}
			index++;
		}

		this.tbViewer=tbViewer;
		init();
		ocdEditor=this;
	}

	private void init(){
		this.importType=this.exportType=null;
		this.ioTypeLabel=new JLabel("Types: [None/None]");

		initLayout();
		for(int i=0;i<spanTypes.length;i++){
			labelBox.addItem(spanTypes[i]);
		}
		labelBox.addActionListener(new LabelDocument("Label Document"));

		loadSpan(nullSpan());
	}

	private void initLayout(){
		//
		// layout stuff
		//
		setLayout(new GridBagLayout());
		GridBagConstraints gbc;

		int col=0;
		gbc=new GridBagConstraints();

		// ------------- up button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(thisUpButton,gbc);

		// ------------- down button ------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(thisDownButton,gbc);

		// ------------ label box -------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		labelBox.addItem(LABEL_DOCUMENT);
		add(labelBox,gbc);

		// ------------- add document button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(addButton,gbc);

		// ------------- get classifier button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(classifierButton,gbc);

		// ------------- save annotator button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(saveAnnButton,gbc);

		// ------------- reset button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(resetButton,gbc);

		// ------------- complete training button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(completeButton,gbc);

		// ----------- save button -------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(saveButton,gbc);
		buttonsThatChangeStuff.add(saveButton);
		saveButton.setEnabled(saveAsFile!=null);

		// ------------- editorHolder ---------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.gridwidth=col;
		add(editorHolder,gbc);
	}

	/**
	 * Set mode to read-only or not. In read-only mode, the document viewed has
	 * the same highlighting as in the documentList. In write mode, the "truth"
	 * spans are shown, and the "guess" spans are imported.
	 */
	public void setReadOnly(boolean readOnly){
		for(Iterator<JButton> i=buttonsThatChangeStuff.iterator();i.hasNext();){
			JButton button=i.next();
			button.setEnabled(readOnly?false:true);
		}
		this.readOnly=readOnly;
	}

	/** Declare which types are being edited. */
	public void setTypesBeingEdited(String inType,String outType){
		this.importType=inType;
		this.exportType=outType;
		ioTypeLabel.setText("Edit: "+importType+"/"+exportType);
	}

	@Override
	protected void loadSpanHook(){
		if(readOnly&&!DUMMY_ID.equals(documentSpan.getDocumentId())){
			importDocumentListMarkup(documentSpan.getDocumentId());
		}
//		Keymap keymap=JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
	}

	/** Toggles readOnly status */
//	private class ReadOnlyButton extends AbstractAction{
//		static final long serialVersionUID=20080314L;
//		public ReadOnlyButton(String msg){
//			super(msg);
//		}
//
//		public void actionPerformed(ActionEvent event){
//			setReadOnly(!readOnly);
//			if(documentSpan!=null)
//				loadSpan(documentSpan);
//		}
//	}

	/** Add span associated with selected text. */
	private class LabelDocument extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public LabelDocument(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			JComboBox cb=(JComboBox)event.getSource();
			String type=(String)cb.getSelectedItem();
			int docIndex=documentList.getMinSelectionIndex();
			if(type!="-choose label-"){
				if(editDocument(documentSpan,type,docIndex))
					statusMsg.display("Document labeled: "+type);
				else
					statusMsg
							.display("Document label cannot be changed - document already added");
			}else
				statusMsg.display("Document has NOT been labeled");
		}
	}

	/** Add span associated with selected text. */
	private class AddSelection extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public AddSelection(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			AddDocuments();
			tbViewer.highlightAction.paintDocument(null);
			statusMsg.display("Documents added to learner");
		}
	}

	/** Return the current classifier */
	private class GetClassifier extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public GetClassifier(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			new ViewerFrame("Classifier",new SmartVanillaViewer(textLearner
					.getClassifier()));
			statusMsg.display("Getting the Classifier");
		}
	}

	public void saveAnn(File file,String format) throws IOException{
		// if (!format.equals(FORMAT_NAME)) throw new
		// IllegalArgumentException("illegal format "+format);
		try{
			try{
				// Annotator ann = (Annotator)textLearner;
				IOUtil.saveSerialized((Serializable)textLearner,file);
			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+file+": "+e);
			}
		}catch(Exception e){
			System.out.println("Error Opening Excel File");
			e.printStackTrace();
		}
	}

	/** Return the current classifier */
	private class SaveAnnotator extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public SaveAnnotator(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			JFileChooser chooser=new JFileChooser();
			int returnVal=chooser.showSaveDialog(ocdEditor);
			if(returnVal==JFileChooser.APPROVE_OPTION){
				try{
					FileFilter filter=chooser.getFileFilter();
//					String fmt=filter.getDescription();
					String ext=".ann";
					File file0=chooser.getSelectedFile();
					File file=
							(file0.getName().endsWith(ext))?file0:new File(file0
									.getParentFile(),file0.getName()+ext);
					ocdEditor.saveAnn(file,filter.getDescription());
				}catch(Exception ex){
					statusMsg.display("Error Saving Annotator");
				}
			}
			// statusMsg.display("Saving the Annotator");
		}

	}

	/** Forget about all previous examples */
	private class Reset extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public Reset(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			textLearner.reset();
			statusMsg.display("Learner Reset - previous examples forgotten");
		}
	}

	/** Complete Training - Announce that no more examples will be coming */
	private class CompleteTraining extends AbstractAction{
		static final long serialVersionUID=200803014L;
		public CompleteTraining(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			textLearner.completeTraining();
			statusMsg.display("Training Completed - no more examples will be added");
		}
	}

	protected void documentMessage(int nextCursor){
		if(nextCursor>-1){
			String label=checkLabel(nextCursor);
			if(checkIfAdded(nextCursor)){
				statusMsg
						.display("Cannot change label: Document has already been added to Classifier as: "+
								label);
				labelBox.setSelectedItem(label);
			}else if(label!=null){
				statusMsg.display("Document is currently labeled: "+label);
				labelBox.setSelectedItem(label);
			}else{
				statusMsg
						.display("In MOVEDOCCURSOI: This document has NOT been labeled");
				labelBox.setSelectedItem(LABEL_DOCUMENT);
			}
		}
	}

	@Override
	protected void loadSpan(Span span){
		super.loadSpan(span);
		documentMessage(documentList.getMinSelectionIndex());
	}

	/** Move through list of spans */
	protected class MoveOnlineDocumentCursor extends AbstractAction{
		static final long serialVersionUID=200803014L;
		private int delta;

		public MoveOnlineDocumentCursor(String msg,int delta){
			super(msg);
			this.delta=delta;
		}

		@Override
		public void actionPerformed(ActionEvent event){
			int nextCursor;
			synchronized(documentList){
				int currentCursor=documentList.getSelectedIndex();
				// if nothing's selected, pretend it was the first thing
				if(currentCursor<0)
					currentCursor=0;
				nextCursor=currentCursor+delta;
				if(nextCursor<documentList.getModel().getSize()&&nextCursor>=0){
					documentList.setSelectedIndex(nextCursor);
				}
			}
			documentMessage(nextCursor);
		}
	}

//	private class SpanPropertyViewer extends ComponentViewer{
//		static final long serialVersionUID=200803014L;
//		public JComponent componentFor(Object o){
//			final Span span=(Span)o;
//			final JTabbedPane pane=new JTabbedPane();
//			final JTextField propField=new JTextField(10);
//			final JTextField valField=new JTextField(10);
//			final JTable table=makePropertyTable(span);
//			final JScrollPane tableScroller=new JScrollPane(table);
//			final JButton addButton=
//					new JButton(new AbstractAction("Insert Property"){
//						static final long serialVersionUID=200803014L;
//						public void actionPerformed(ActionEvent event){
//							editLabels.setProperty(span,propField.getText(),valField
//									.getText());
//							tableScroller.getViewport().setView(makePropertyTable(span));
//							tableScroller.revalidate();
//							pane.revalidate();
//						}
//					});
//			GridBagConstraints gbc=fillerGBC();
//			// gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.gridwidth=3;
//			final JPanel subpanel=new JPanel();
//			subpanel.setLayout(new GridBagLayout());
//			subpanel.add(tableScroller,gbc);
//			subpanel.add(addButton,myGBC(0));
//			subpanel.add(propField,myGBC(1));
//			subpanel.add(valField,myGBC(2));
//			pane.add("Properties",subpanel);
//			pane.add("Span",new SmartVanillaViewer(span));
//			return pane;
//		}
//
//		private GridBagConstraints myGBC(int col){
//			GridBagConstraints gbc=fillerGBC();
//			gbc.fill=GridBagConstraints.HORIZONTAL;
//			gbc.gridx=col;
//			gbc.gridy=1;
//			return gbc;
//		}
//
//		private JTable makePropertyTable(final Span span){
//			Object[] spanProps=editLabels.getSpanProperties().toArray();
//			Object[][] table=new Object[spanProps.length][2];
//			for(int i=0;i<spanProps.length;i++){
//				table[i][0]=spanProps[i];
//				table[i][1]=editLabels.getProperty(span,(String)spanProps[i]);
//			}
//			String[] colNames=new String[]{"Property","Property's Value"};
//			return new JTable(table,colNames);
//		}
//	}

	/**
	 * Check if the documentSpan with docID has already been added to the
	 * classifier
	 */
	public boolean checkIfAdded(int docID){
		for(int i=0;i<editedSpans.size();i++){
			EditedSpan eSpan=editedSpans.get(i);
			if(eSpan.id==docID&&eSpan.added){
				return true;
			}
		}
		return false;
	}

	/** Returns the label of the document if it has been labeled */
	public String checkLabel(int docID){
		for(int i=0;i<editedSpans.size();i++){
			EditedSpan eSpan=editedSpans.get(i);
			if(eSpan.id==docID){
				return eSpan.label;
			}
		}
		return null;
	}

	/**
	 * Labels a document - unless the document has already been added to the
	 * classifier
	 */
	public boolean editDocument(Span s,String label,int docID){
		// Checks if Span has already been edited
		// If Span is alread added to the classifier, return false
		// Otherwise change label
		for(int i=0;i<editedSpans.size();i++){
			EditedSpan eSpan=editedSpans.get(i);
			if(eSpan.id==docID){
				if(eSpan.added)
					return false;
				else{
					eSpan.label=label;
					return true;
				}
			}
		}
		// If Span is not added, create new Edited span, return true
		EditedSpan eSpan=new EditedSpan(s,label,docID);
		editedSpans.add(eSpan);
		return true;
	}

	/**
	 * Adds all the documents that have been edited but not already added to the
	 * classifier
	 */
	public void AddDocuments(){
		// Add all edited spans to editLabels
		for(int i=0;i<editedSpans.size();i++){
			EditedSpan eSpan=(editedSpans.get(i));
			if(!eSpan.added&&eSpan.s!=null){
				editLabels.addToType(eSpan.s,eSpan.label);
				textLearner.addDocument(eSpan.label,eSpan.s.asString());
			}
		}
		// Mark all edited Spans as added
		for(int j=0;j<editedSpans.size();j++){
			EditedSpan eSpan=(editedSpans.get(j));
			eSpan.add();
		}
		ann=textLearner.getAnnotator();
		viewLabels=textLearner.annotatedCopy(editLabels);
		tbViewer.updateTextLabels(viewLabels);
	}

//	private Span getEditSpan(int k){
//		/*
//		 * for (int i=0; i<editedSpans.length; i++) { Span s = editedSpans[i]; if
//		 * (k-- == 0) return s; }
//		 */
//		throw new IllegalStateException("bad editedSpan index "+k);
//	}

	/**
	 * Stores a documentSpan, its label, its index, and whether or not it has been
	 * added to the Classifier
	 */
	public class EditedSpan{

		public Span s;

		public String label;

		public int id;

		public boolean added=false;

		public EditedSpan(Span s,String label,int id){
			this.s=s;
			this.label=label;
			this.id=id;
		}

		public void add(){
			this.added=true;
		}
	}
}