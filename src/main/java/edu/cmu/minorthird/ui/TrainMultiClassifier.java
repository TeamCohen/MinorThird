package edu.cmu.minorthird.ui;

import java.io.IOException;

import edu.cmu.minorthird.classify.multi.MultiClassifier;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiDatasetClassifierTeacher;
import edu.cmu.minorthird.classify.transform.AbstractInstanceTransform;
import edu.cmu.minorthird.classify.transform.PredictedClassTransform;
import edu.cmu.minorthird.classify.transform.TransformingMultiClassifier;
import edu.cmu.minorthird.text.learn.MultiClassifierAnnotator;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Train a text classifier.
 *
 * @author William Cohen
 */

public class TrainMultiClassifier extends UIMain{

	// private data needed to train a classifier

	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.MultiClassificationSignalParams signal=
			new CommandLineUtil.MultiClassificationSignalParams(base);

	private CommandLineUtil.TrainClassifierParams train=
			new CommandLineUtil.TrainClassifierParams();

	private MultiClassifier classifier=null;

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

	public CommandLineUtil.TrainClassifierParams getAdditionalParameters(){
		return train;
	}

	public void setAdditionalParameters(CommandLineUtil.TrainClassifierParams p){
		train=p;
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
		if(train.learner==null)
			throw new IllegalArgumentException("-learner must be specified");
		if(signal.multiSpanProp==null)
			throw new IllegalArgumentException("-multiSpanProp  must be specified");

		// construct the dataset
		MultiDataset d=
				CommandLineUtil.toMultiDataset(base.labels,train.fe,
						signal.multiSpanProp);
		if(signal.cross)
			d=d.annotateData();
		if(train.showData){
			System.out.println("Trying to show the Dataset");
			new ViewerFrame("Dataset",d.toGUI());
		}

		// train the classifier
		classifier=new MultiDatasetClassifierTeacher(d).train(train.learner);

		// create a transforming multiClassifier if cross
		if(signal.cross){
			AbstractInstanceTransform transformer=
					new PredictedClassTransform(classifier);
			classifier=new TransformingMultiClassifier(classifier,transformer);
		}

		if(base.showResult){
			Viewer cv=new SmartVanillaViewer();
			if(classifier instanceof TransformingMultiClassifier)
				cv.setContent(classifier);
			else
				cv.setContent(classifier);
			new ViewerFrame("Classifier",cv);
		}

		MultiClassifierAnnotator ann=
				new MultiClassifierAnnotator(train.fe,classifier,signal.multiSpanProp);

		if(save.saveAs!=null){
			try{
				IOUtil.saveSerialized(ann,save.saveAs);
			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	@Override
	public Object getMainResult(){
		return classifier;
	}

	public static void main(String args[]){
		new TrainMultiClassifier().callMain(args);
	}
}
