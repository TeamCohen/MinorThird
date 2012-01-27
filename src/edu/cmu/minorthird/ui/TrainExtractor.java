package edu.cmu.minorthird.ui;

import java.io.IOException;
import java.io.Serializable;

import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.TextLabelsAnnotatorTeacher;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Train a named-entity extractor.
 * 
 * @author William Cohen
 */

public class TrainExtractor extends UIMain{

	// private data needed to train a extractor

	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.ExtractionSignalParams signal=
		new CommandLineUtil.ExtractionSignalParams(base);

	private CommandLineUtil.TrainExtractorParams train=
		new CommandLineUtil.TrainExtractorParams();

	private Annotator ann=null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(CommandLineUtil.SaveParams p){
		save=p;
	}

	public CommandLineUtil.TrainExtractorParams getAdditionalParameters(){
		return train;
	}

	public void setAdditionalParameters(CommandLineUtil.TrainExtractorParams p){
		train=p;
	}

	public CommandLineUtil.ExtractionSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(
			CommandLineUtil.ExtractionSignalParams newSignal){
		this.signal=newSignal;
	}

	public String getTrainExtractorHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TrainExtractor%20Tutorial.htm\">TrainExtractor Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				save,signal,train});
	}

	//
	// do the experiment
	// 

	@Override
	public void doMain(){
		// check that inputs are valid
		if(train.learner==null){
			throw new IllegalArgumentException("-learner must be specified");
		}

		if(train.fe!=null){
			train.learner.setSpanFeatureExtractor(train.fe);
		}

		train.learner.setAnnotationType(train.output);

		// do the training
		AnnotatorTeacher teacher=
			new TextLabelsAnnotatorTeacher(base.labels,signal.spanType,signal.spanProp);
		ann=teacher.train(train.learner);

		if(base.showResult){
			Viewer av=new SmartVanillaViewer();
			av.setContent(ann);
			new ViewerFrame("Extractor",av);
		}

		if(save.saveAs!=null){
			try{
				IOUtil.saveSerialized((Serializable)ann,save.saveAs);
			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	@Override
	public Object getMainResult(){
		return ann;
	}

	public static void main(String args[]){
		new TrainExtractor().callMain(args);
	}
}
