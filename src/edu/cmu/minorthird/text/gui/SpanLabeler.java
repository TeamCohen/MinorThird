package edu.cmu.minorthird.text.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;

import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Interactivly edit document spans.
 * 
 * @author William Cohen
 */

public class SpanLabeler extends ViewerTracker{
	
	static final long serialVersionUID=200803014L;

//	private static final String EDITOR_PROP=SpanEditor.EDITOR_PROP;

	// map top-level spans to type that labels them
	private static final String LABEL_PROP="userLabel";

	private static final String NULL_TYPE="- pick label -";

	private static final String UNKNOWN_TYPE="- unknown -";

	// internal state
//	private String labelType=UNKNOWN_TYPE;

	// buttons
	final JLabel currentTypeLabel=new JLabel(UNKNOWN_TYPE);

	final JComboBox typeBox=new LabelChooserBox();

	final JButton addCurrentTypeButton=
			new JButton(new AddCurrentTypeAction("Accept class:"));

	final JTextField newTypeField=new JTextField(15);

	final JButton addNewTypeButton=
			new JButton(new AddNewTypeAction("New class:"));

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
	public SpanLabeler(TextLabels viewLabels,MutableTextLabels editLabels,
			JList documentList,SpanPainter spanPainter,StatusMessage statusMsg){
		super(viewLabels,editLabels,documentList,spanPainter,statusMsg);
		setViewEntireDocument(true);
		newTypeField.addActionListener(addNewTypeButton.getAction());

		restoreLabelProps();

		//
		// layout stuff
		//
		setLayout(new GridBagLayout());
		GridBagConstraints gbc;

		int col=0;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=2.0;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(currentTypeLabel,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(addCurrentTypeButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(typeBox,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(addNewTypeButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(newTypeField,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(upButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(downButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=2;
		add(saveButton,gbc);
		// saveButton.setEnabled( saveAsFile!=null );

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.gridwidth=col;
		add(editorHolder,gbc);

		loadSpan(nullSpan());
	}

	// if documentSpan s has type t, then setProperty(s,LABEL_PROP,t)
	private void restoreLabelProps(){
		// System.out.println("restoring label properties");
		for(Iterator<Span> i=editLabels.getTextBase().documentSpanIterator();i
				.hasNext();){
			Span s=i.next();
			for(Iterator<String> j=editLabels.getTypes().iterator();j.hasNext();){
				String t=j.next();
				if(editLabels.hasType(s,t)){
					// System.out.println("restoring "+t+" for "+s);
					editLabels.setProperty(s,LABEL_PROP,t);
				}
			}
		}
	}

	@Override
	protected void loadSpanHook(){
		String oldLabel=editLabels.getProperty(documentSpan,LABEL_PROP);
		if(oldLabel==null){
			currentTypeLabel.setText(UNKNOWN_TYPE);
		}else{
			currentTypeLabel.setText(oldLabel);
		}
	}

	/** Say where the viewer is.. */
	public void addViewer(TextBaseViewer viewer){
		this.viewer=viewer;
	}

	private TextBaseViewer viewer=null;

	private class AddNewTypeAction extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public AddNewTypeAction(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			String type=newTypeField.getText().trim();
			if(!editLabels.isType(type)){
				typeBox.addItem(type);
				if(viewer!=null){
					viewer.getGuessBox().addItem(type);
					viewer.getTruthBox().addItem(type);
					viewer.getDisplayedTypeBox().addItem(type);
				}
			}
			setDocumentType(type);
		}
	}

	private class AddCurrentTypeAction extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		public AddCurrentTypeAction(String msg){
			super(msg);
		}

		@Override
		public void actionPerformed(ActionEvent event){
			String type=(String)typeBox.getSelectedItem();
			if(!NULL_TYPE.equals(type))
				setDocumentType(type);
		}
	}

	private class LabelChooserBox extends JComboBox{

		static final long serialVersionUID=200803014L;
		
		public LabelChooserBox(){
			super();
			addItem(NULL_TYPE);
			for(Iterator<String> i=editLabels.getTypes().iterator();i.hasNext();){
				addItem(i.next());
			}
			addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent event){
					String t=(String)getSelectedItem();
					if(!NULL_TYPE.equals(t))
						setDocumentType(t);
				}
			});
		}
	}

	private void setDocumentType(String type){
		statusMsg.display("setting type="+type+"for "+documentSpan);
		currentTypeLabel.setText(type);
		String oldLabel=editLabels.getProperty(documentSpan,LABEL_PROP);
		if(oldLabel!=null){
			// clear the old label
			editLabels.defineTypeInside(oldLabel,documentSpan,Collections.<Span>emptySet().iterator());
		}
		editLabels.setProperty(documentSpan,LABEL_PROP,type);
		editLabels.addToType(documentSpan,type);
		editLabels.closeTypeInside(type,documentSpan);
	}
}
