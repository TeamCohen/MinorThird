package edu.cmu.minorthird.ui;

import java.io.IOException;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.experiments.ClassifiedDataset;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.text.learn.ClassifierAnnotator;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Test an existing text classifier.
 * 
 * @author William Cohen
 */

public class TestClassifier extends UIMain{

	// private data needed to test a classifier

	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.ClassificationSignalParams signal=
			new CommandLineUtil.ClassificationSignalParams(base);

	private CommandLineUtil.TestClassifierParams test=
			new CommandLineUtil.TestClassifierParams();

	private Object result=null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(CommandLineUtil.SaveParams p){
		save=p;
	}

	public CommandLineUtil.ClassificationSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(CommandLineUtil.ClassificationSignalParams p){
		signal=p;
	}

	public CommandLineUtil.TestClassifierParams getAdditionalParameters(){
		return test;
	}

	public void setAdditionalParameters(CommandLineUtil.TestClassifierParams p){
		test=p;
	}

	public String getTestClassifierHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TestClassifier%20Tutorial.htm\">TestClassifier Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				save,signal,test});
	}

	//
	// load and test a classifier
	// 

	@Override
	public void doMain(){
		// check that inputs are valid
		if(test.loadFrom==null)
			throw new IllegalArgumentException("-loadFrom must be specified");

		// load the classifier
		ClassifierAnnotator ann=null;
		try{
			ann=(ClassifierAnnotator)IOUtil.loadSerialized(test.loadFrom);
		}catch(IOException ex){
			throw new IllegalArgumentException("can't load annotator from "+
					test.loadFrom+": "+ex);
		}

		// do the testing and show the result
		Dataset d=
				CommandLineUtil.toDataset(base.labels,ann.getFE(),signal.spanProp,
						signal.spanType,signal.candidateType);
		Evaluation evaluation=null;
		if(test.showData)
			new ViewerFrame("Dataset",d.toGUI());
		if(test.showClassifier)
			new ViewerFrame("Classifier",new SmartVanillaViewer(ann.getClassifier()));
		evaluation=new Evaluation(d.getSchema());
		evaluation.extend(ann.getClassifier(),d,0);
		if(test.showTestDetails){
			result=new ClassifiedDataset(ann.getClassifier(),d);
		}else{
			result=evaluation;
		}

		if(base.showResult){
			new ViewerFrame("Result",new SmartVanillaViewer(result));
		}

		if(save.saveAs!=null){
			try{
				IOUtil.saveSerialized(evaluation,save.saveAs);
			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
		evaluation.summarize();
	}

	@Override
	public Object getMainResult(){
		return result;
	}

	public static void main(String args[]){
		new TestClassifier().callMain(args);
	}
}
