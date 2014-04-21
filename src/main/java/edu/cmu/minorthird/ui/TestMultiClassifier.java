package edu.cmu.minorthird.ui;

import java.io.IOException;
import java.io.Serializable;

import edu.cmu.minorthird.classify.multi.MultiClassifiedDataset;
import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiEvaluation;
import edu.cmu.minorthird.classify.transform.AbstractInstanceTransform;
import edu.cmu.minorthird.classify.transform.TransformingMultiClassifier;
import edu.cmu.minorthird.text.learn.MultiClassifierAnnotator;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Test an existing text classifier for multiple labels.
 * 
 * @author Cameron Williams
 */

public class TestMultiClassifier extends UIMain{

	// private data needed to test a classifier

	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.MultiClassificationSignalParams signal=
			new CommandLineUtil.MultiClassificationSignalParams(base);

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

	public CommandLineUtil.MultiClassificationSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(
			CommandLineUtil.MultiClassificationSignalParams p){
		signal=p;
	}

	public CommandLineUtil.TestClassifierParams getAdditionalParameters(){
		return test;
	}

	public void setAdditionalParameters(CommandLineUtil.TestClassifierParams p){
		test=p;
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
		MultiClassifierAnnotator ann=null;
		try{
			ann=(MultiClassifierAnnotator)IOUtil.loadSerialized(test.loadFrom);
		}catch(IOException ex){
			throw new IllegalArgumentException("can't load annotator from "+
					test.loadFrom+": "+ex);
		}

		// do the testing and show the result
		MultiDataset d=
				CommandLineUtil.toMultiDataset(base.labels,ann.getFE(),
						signal.multiSpanProp);
		MultiClassifier multiClassifier=ann.getMultiClassifier();
		if(signal.cross){
			// d=d.annotateData(multiClassifier);
			if(multiClassifier instanceof TransformingMultiClassifier){
				AbstractInstanceTransform transformer=
						((TransformingMultiClassifier)multiClassifier).getTransform();
				d=transformer.transform(d);
			}else{
				throw new IllegalArgumentException(
						"Must be a TransformingMultiClassifier to use cross dimensions");
			}

		}
		MultiEvaluation evaluation=null;
		if(test.showData)
			new ViewerFrame("Dataset",d.toGUI());
		if(test.showClassifier)
			new ViewerFrame("Classifier",new SmartVanillaViewer(multiClassifier));
		evaluation=new MultiEvaluation(d.getMultiSchema());
		evaluation.extend(multiClassifier,d);
		if(test.showTestDetails){
			result=new MultiClassifiedDataset(multiClassifier,d);
		}else{
			result=evaluation;
		}

		if(base.showResult){
			new ViewerFrame("Result",new SmartVanillaViewer(result));
		}

		if(save.saveAs!=null){
			try{
				IOUtil.saveSerialized((Serializable)evaluation,save.saveAs);
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
		new TestMultiClassifier().callMain(args);
	}
}
