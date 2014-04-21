package edu.cmu.minorthird.text.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;

import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/** Interactivly edit the subspans associated with a particular
 * document span.
 *
 * @author William Cohen
 */

public class SpanEditor extends ViewerTracker{

	static final long serialVersionUID=200803014L;
	
	public static final String EDITOR_PROP="_edited";

	// internal state
	private String importType,exportType;

	private JLabel ioTypeLabel;

	private SortedSet<Span> editedSpans;

	private int editSpanCursor=-1; // indicates nothing selected

	private boolean readOnly=false;

	// buttons
	JButton readOnlyButton=
			new JButton(new ReadOnlyButton(readOnly?"Edit":"Read"));

	JButton importButton=new JButton(new ImportGuessSpans("Import"));

	JButton exportButton=new JButton(new ExportGuessSpans("Export"));

	JButton addButton=new JButton(new AddSelection("Add"));

	JButton deleteButton=new JButton(new DeleteCursoredSpan("Delete"));

	JButton propButton=new JButton(new EditSpanProperties("Props"));

	JButton prevButton=new JButton(new MoveSpanCursor("Prev",-1));

	JButton nextButton=new JButton(new MoveSpanCursor("Next",+1));

	private List<JButton> buttonsThatChangeStuff=new ArrayList<JButton>();

	/**
	 * @param viewLabels a superset of editLabels which may include some additional read-only information
	 * @param editLabels the labels being modified
	 * @param documentList the document Span being edited is associated with
	 * the selected entry of the documentList.
	 * @param spanPainter used to repaint documentList elements
	 * @param statusMsg a JLabel used for status messages.
	 */
	public SpanEditor(TextLabels viewLabels,MutableTextLabels editLabels,
			JList documentList,SpanPainter spanPainter,StatusMessage statusMsg){
		super(viewLabels,editLabels,documentList,spanPainter,statusMsg);
		init();

	}

	private void init(){
		this.importType=this.exportType=null;
		this.ioTypeLabel=new JLabel("Types: [None/None]");

		initLayout();

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

		//------------ ioType button ------------------//
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(ioTypeLabel,gbc);

		//------------ read only  button ------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(readOnlyButton,gbc);

		//------------ import button ------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		buttonsThatChangeStuff.add(importButton);
		add(importButton,gbc);

		//------------ export button ------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		buttonsThatChangeStuff.add(exportButton);
		add(exportButton,gbc);

		//------------ add button -------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		buttonsThatChangeStuff.add(addButton);
		add(addButton,gbc);

		//-------------- delete button --------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		buttonsThatChangeStuff.add(deleteButton);
		add(deleteButton,gbc);

		//-------------- delete button --------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		buttonsThatChangeStuff.add(propButton);
		add(propButton,gbc);

		//------------ prev button ---------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(prevButton,gbc);

		//-------------- next button --------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(nextButton,gbc);

		//------------- up button -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(upButton,gbc);

		//------------- down button ------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(downButton,gbc);

		//-------------- context slider -----------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(contextWidthSlider,gbc);

		//----------- save button -------------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(saveButton,gbc);
		buttonsThatChangeStuff.add(saveButton);
		//System.out.println("create saveButton: saveAsFile=" + saveAsFile + " enabled: " + (saveAsFile != null));
		saveButton.setEnabled(saveAsFile!=null);

		//------------- editorHolder ---------------//
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.gridwidth=col;
		add(editorHolder,gbc);
	}

	/** Set mode to read-only or not.  In read-only mode, the
	 * document viewed has the same highlighting as in the
	 * documentList.  In write mode, the "truth" spans
	 * are shown, and the "guess" spans are imported.
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
		Keymap keymap=JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control I"),
				importButton.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control E"),
				exportButton.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control S"),
				exportButton.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control A"),addButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("DELETE"),deleteButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control D"),
				deleteButton.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("LEFT"),prevButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control B"),prevButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("RIGHT"),nextButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("control F"),nextButton
				.getAction());
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("TAB"),nextButton
				.getAction());
		editedSpans=new TreeSet<Span>();
	}

	/** Toggles readOnly status */
	private class ReadOnlyButton extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public ReadOnlyButton(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			setReadOnly(!readOnly);
			if(documentSpan!=null)
				loadSpan(documentSpan);
			readOnlyButton.setText(readOnly?"Edit":"Read");
		}
	}

	/** Imports the spans associated with the documentSpan into
	 * the set currently being edited */
	private class ImportGuessSpans extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public ImportGuessSpans(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			if(importType==null){
				statusMsg.display("what type?");
				return;
			}
			editedDoc.resetHighlights();
			editedSpans=new TreeSet<Span>();

			for(Iterator<Span> i=
					viewLabels.instanceIterator(importType,documentSpan.getDocumentId());i
					.hasNext();){
				Span guessSpan=i.next();
				editedDoc.highlight(guessSpan,HiliteColors.yellow);
				editedSpans.add(guessSpan);
			}
			editSpanCursor=-1;
			statusMsg.display("imported "+editedSpans.size()+" "+importType+
					" spans to "+documentSpan.getDocumentId());
		}
	}

	/** Exports the spans associated with the documentSpan into
	 * the set currently being edited */
	private class ExportGuessSpans extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public ExportGuessSpans(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			if(exportType==null){
				statusMsg.display("what type?");
				return;
			}
			Iterator<Span> newSpans=editedSpans.iterator();
			editLabels.defineTypeInside(exportType,documentSpan,newSpans);
			//editLabels.setProperty(documentSpan.documentSpan(), EDITOR_PROP, "t");
			spanPainter.paintDocument(documentSpan.getDocumentId());
			editSpanCursor=-1;
			statusMsg.display("exported "+editedSpans.size()+" "+exportType+" spans");
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
			int lo=editorPane.getSelectionStart();
			int hi=editorPane.getSelectionEnd();
			// compensate for the fact that the document being viewed
			// doesn't start at char position zero
			lo=editedDoc.toLogicalCharIndex(lo);
			hi=editedDoc.toLogicalCharIndex(hi);
			Span span=documentSpan.charIndexSubSpan(lo,hi);
//			int spanLo=-1,spanHi=-1;
//			if(span.size()>0){
//				spanLo=span.getTextToken(0).getLo();
//				spanHi=span.getTextToken(span.size()-1).getHi();
//			}
			//System.out.println("spanSize="+span.size()+" lo="+lo+" hi="+hi+" spanLo="+spanLo+" spanHi="+spanHi);
			// figure out if we need to move the selected span
			int correction=0;
			if(editSpanCursor>=0&&(span.compareTo(getEditSpan(editSpanCursor))<0)){
				correction=1;
			}
			editedDoc.highlight(span,HiliteColors.yellow);
			editedSpans.add(span);
			editSpanCursor+=correction;
			statusMsg.display("adding "+span);
		}
	}

	/** Delete the span under the cursor. */
	private class DeleteCursoredSpan extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public DeleteCursoredSpan(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			Span span=null;
			if(editSpanCursor>=0){
				span=getEditSpan(editSpanCursor);
			}else{
				int lo=editorPane.getSelectionStart();
				int hi=editorPane.getSelectionEnd();
				span=documentSpan.charIndexSubSpan(lo,hi);
			}
			editedDoc.highlight(span,SimpleAttributeSet.EMPTY);
			editedSpans.remove(span);
			if(editSpanCursor>=editedSpans.size()){
				editSpanCursor=-1;
			}else if(editSpanCursor>=0){
				// highlight next span after the deleted one
				editedDoc.highlight(getEditSpan(editSpanCursor),
						HiliteColors.cursorColor);
			}
		}
	}

	/** Change the properties of the span. */
	private class EditSpanProperties extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public EditSpanProperties(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			Span span=null;
			if(editSpanCursor>=0){
				span=getEditSpan(editSpanCursor);
			}else{
				int lo=editorPane.getSelectionStart();
				int hi=editorPane.getSelectionEnd();
				span=documentSpan.charIndexSubSpan(lo,hi);
			}
			Viewer viewer=new SpanPropertyViewer();
			viewer.setContent(span);
			new ViewerFrame("Span to edit",viewer);
		}
	}

	private class SpanPropertyViewer extends ComponentViewer{

		static final long serialVersionUID=200803014L;
		
		@Override
		public JComponent componentFor(Object o){
			final Span span=(Span)o;
			final JTabbedPane pane=new JTabbedPane();
			final JTextField propField=new JTextField(10);
			final JTextField valField=new JTextField(10);
			final JTable table=makePropertyTable(span);
			final JScrollPane tableScroller=new JScrollPane(table);
			final JButton addButton=
					new JButton(new AbstractAction("Insert Property"){

						static final long serialVersionUID=200803014L;
						
						@Override
						public void actionPerformed(ActionEvent event){
							editLabels.setProperty(span,propField.getText(),valField
									.getText());
							tableScroller.getViewport().setView(makePropertyTable(span));
							tableScroller.revalidate();
							pane.revalidate();
						}
					});
			GridBagConstraints gbc=fillerGBC();
			//gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth=3;
			final JPanel subpanel=new JPanel();
			subpanel.setLayout(new GridBagLayout());
			subpanel.add(tableScroller,gbc);
			subpanel.add(addButton,myGBC(0));
			subpanel.add(propField,myGBC(1));
			subpanel.add(valField,myGBC(2));
			pane.add("Properties",subpanel);
			pane.add("Span",new SmartVanillaViewer(span));
			return pane;
		}

		private GridBagConstraints myGBC(int col){
			GridBagConstraints gbc=fillerGBC();
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.gridx=col;
			gbc.gridy=1;
			return gbc;
		}

		private JTable makePropertyTable(final Span span){
			//System.out.println("editLabels="+editLabels);
			//System.out.println("spanProps="+editLabels.getSpanProperties());
			Object[] spanProps=editLabels.getSpanProperties().toArray();
			Object[][] table=new Object[spanProps.length][2];
			for(int i=0;i<spanProps.length;i++){
				table[i][0]=spanProps[i];
				table[i][1]=editLabels.getProperty(span,(String)spanProps[i]);
			}
			String[] colNames=new String[]{"Property","Property's Value"};
			return new JTable(table,colNames);
		}
	}

	/** Move through list of spans */
	private class MoveSpanCursor extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		private int delta;

		public MoveSpanCursor(String msg,int delta){
			super(msg);
			this.delta=delta;
		}

		@Override
		public void actionPerformed(ActionEvent event){
			if(editedSpans==null||editedSpans.isEmpty())
				return;
			if(editSpanCursor>=0){
				editedDoc.highlight(getEditSpan(editSpanCursor),HiliteColors.yellow);
				editSpanCursor=editSpanCursor+delta;
				// wrap around
				if(editSpanCursor<0)
					editSpanCursor+=editedSpans.size();
				else if(editSpanCursor>=editedSpans.size())
					editSpanCursor-=editedSpans.size();
			}else{
				// move to first legit span
				editSpanCursor=0;
			}
			editedDoc.highlight(getEditSpan(editSpanCursor),HiliteColors.cursorColor);
			statusMsg.display("to span#"+editSpanCursor+": "+
					getEditSpan(editSpanCursor));
		}
	}

	private Span getEditSpan(int k){
		for(Iterator<Span> i=editedSpans.iterator();i.hasNext();){
			Span s=i.next();
			if(k--==0)
				return s;
		}
		throw new IllegalStateException("bad editedSpan index "+k);
	}
}
