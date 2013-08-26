package edu.cmu.minorthird.text.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.SampleTextBases;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.learn.OnlineTextClassifierLearner;

/** Interactively edit the contents of a TextBase and MutableTextLabels.
 *
 * @author William Cohen
 */

public class OnlineLearnerEditor extends TrackedTextBaseComponent{

	static final long serialVersionUID=200803014L;

	private OnlineClassifierDocumentEditor ocdEditor;

	public OnlineClassifierDocumentEditor getOnlineClassifierDocumentEditor(){
		return ocdEditor;
	}

	protected OnlineLearnerEditor(String[] args){
		super();
		log.debug("construct");
		try{
			setLabels(args);
		}catch(IOException e){
			log.fatal(e,e);
		}
	}

	public OnlineLearnerEditor(TextBase base,
			TextLabels viewLabels, // seen in viewer
			MutableTextLabels editLabels, // changed in editor
			String key,StatusMessage statusMsg,boolean readOnly,
			OnlineTextClassifierLearner learner){
//		super(base, viewLabels, editLabels, statusMsg);

		init(base,viewLabels,statusMsg,editLabels,key,readOnly,learner);
	}

	private void init(TextBase base,TextLabels viewLabels,
			StatusMessage statusMsg,MutableTextLabels editLabels,String key,
			boolean readOnly,OnlineTextClassifierLearner learner){
		super.init(base,viewLabels,editLabels,statusMsg);
		viewer=new TextBaseViewer(base,viewLabels,statusMsg);

		createOnlineClassifierDocumentEditor(viewLabels,viewer,editLabels,
				statusMsg,learner);
		ocdEditor=(OnlineClassifierDocumentEditor)viewerTracker;
		File saveLabels=new File(key+".labels");
		ocdEditor.setSaveAs(saveLabels);

		viewer.getTruthBox()
				.addActionListener(
						new EditTypeAction(viewer.getGuessBox(),viewer.getTruthBox(),
								ocdEditor));
		viewer.getGuessBox()
				.addActionListener(
						new EditTypeAction(viewer.getGuessBox(),viewer.getTruthBox(),
								ocdEditor));
		viewer.getDocumentList().addListSelectionListener(ocdEditor);
		ocdEditor.setReadOnly(readOnly);
		initializeLayout();
	}

	protected void createOnlineClassifierDocumentEditor(TextLabels viewLabels,
			TextBaseViewer viewer,MutableTextLabels editLabels,
			StatusMessage statusMsg,OnlineTextClassifierLearner learner){
		viewerTracker=
				new OnlineClassifierDocumentEditor(learner,viewLabels,viewer,
						editLabels,viewer.getDocumentList(),viewer.getSpanPainter(),
						statusMsg);
	}

	/** Change the type of span being edited. */
	public static class EditTypeAction extends AbstractAction{

		static final long serialVersionUID=200803014L;
		
		private JComboBox guessBox,truthBox;

		private OnlineClassifierDocumentEditor ocdEditor;

		public EditTypeAction(JComboBox guessBox,JComboBox truthBox,
				OnlineClassifierDocumentEditor ocdEditor){
			this.guessBox=guessBox;
			this.truthBox=truthBox;
			this.ocdEditor=ocdEditor;
		}

		@Override
		public void actionPerformed(ActionEvent event){
			String truthType=(String)truthBox.getSelectedItem();
			String guessType=(String)guessBox.getSelectedItem();
			if(!TextBaseViewer.NULL_TRUTH_ENTRY.equals(truthType))
				ocdEditor.setTypesBeingEdited(guessType,truthType);
			else
				ocdEditor.setTypesBeingEdited(guessType,guessType);
		}
	}

	/** Pop up a frame for editing the labels. */
	public static OnlineLearnerEditor edit(TextLabels labels,
			MutableTextLabels editLabels,String rk,OnlineTextClassifierLearner learner){
		TextBase textBase=labels.getTextBase();
		StatusMessage statusMsg=new StatusMessage();
		OnlineLearnerEditor editor=
				new OnlineLearnerEditor(textBase,labels,editLabels,rk,statusMsg,false,
						learner);
		editor.initializeLayout();
		editor.buildFrame();

		return editor;
	}

	private void setLabels(String[] args) throws IOException{
		boolean readOnly=checkReadOnly(args);

		TextBase base=null;
		MutableTextLabels labels=null;
		File saveFile=null;

		if(args.length==0){
			base=SampleTextBases.getTextBase();
			labels=SampleTextBases.getTruthLabels();
			log.info("Sample Text Bases");
			//labels = edu.cmu.minorthird.text.ann.TestExtractionProblem.getLabels();
			//base = labels.getTextBase();
		}else{
			log.debug("load from "+args[0]);
			labels=(MutableTextLabels)FancyLoader.loadTextLabels(args[0]);
			base=labels.getTextBase();
			if(args.length>1){
				saveFile=new File(args[1]);
				if(saveFile.exists())
					labels=new TextLabelsLoader().loadOps(base,saveFile);
				log.info("load text bases");
			}
		}
		init(base,labels,new StatusMessage(),labels,"default",readOnly,null);
		this.setSaveAs(saveFile);

	}

	private static boolean checkReadOnly(String[] args){
		boolean readOnly=false;
//		int argp = 0;
		for(int argp=0;argp<args.length;argp++){
			if("-readOnly".equals(args[argp])){
				readOnly=true;
				argp++;
			}
		}
		return readOnly;
	}

	/**
	     Entry point that runs a gui to examine labels and
	     change them.  
	     @param args first argument is labels file and second is save file
	 **/
	public static void main(String[] args){
		try{
			MutableTextLabels labels=
					(MutableTextLabels)FancyLoader.loadTextLabels(args[0]);
			File saveFile=new File(args[1]);
			TextBaseEditor.edit(labels,saveFile);
		}catch(Exception e){
			System.out.println("usage repositoryKey outputFile");
		}
	}
}