package edu.cmu.minorthird.ui;

import java.io.IOException;
import java.io.Serializable;

import edu.cmu.minorthird.classify.experiments.FixedTestSetSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.multi.MultiCrossValidatedDataset;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiEvaluation;
import edu.cmu.minorthird.classify.multi.MultiExample;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

// to do:
//  show labels should be a better viewer
//  -baseType type

/**
 * Do a train/test experiment on a text classifier for data with multiple labels.
 *
 * @author Cameron Williams
 */

public class TrainTestMultiClassifier extends UIMain{

	// private data needed to train a classifier

	protected CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	protected CommandLineUtil.MultiClassificationSignalParams signal=
			new CommandLineUtil.MultiClassificationSignalParams(base);

	protected CommandLineUtil.TrainClassifierParams train=
			new CommandLineUtil.TrainClassifierParams();

	protected CommandLineUtil.SplitterParams trainTest=
			new CommandLineUtil.SplitterParams();

	protected Object result=null;

	// for GUI
	public CommandLineUtil.SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(CommandLineUtil.SaveParams base){
		this.save=base;
	}

	public CommandLineUtil.MultiClassificationSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(
			CommandLineUtil.MultiClassificationSignalParams base){
		this.signal=base;
	}

	public CommandLineUtil.TrainClassifierParams getTrainingParameters(){
		return train;
	}

	public void setTrainingParameters(CommandLineUtil.TrainClassifierParams train){
		this.train=train;
	}

	public CommandLineUtil.SplitterParams getSplitterParameters(){
		return trainTest;
	}

	public void setSplitterParameters(CommandLineUtil.SplitterParams trainTest){
		this.trainTest=trainTest;
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				save,signal,train,trainTest});
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

		// show the data if necessary
		if(train.showData)
			new ViewerFrame("Dataset",d.toGUI());

		// construct the splitter, if necessary
		if(trainTest.labels!=null){
			MultiDataset testData=
					CommandLineUtil.toMultiDataset(trainTest.labels,
							train.fe,signal.multiSpanProp);
			trainTest.splitter=new FixedTestSetSplitter<MultiExample>(testData.multiIterator());
		}

		// do the experiment
		MultiCrossValidatedDataset cvd=null;
		MultiEvaluation evaluation=null;
		if(trainTest.showTestDetails){
			cvd=
					new MultiCrossValidatedDataset(train.learner,d,trainTest.splitter,
							false,signal.cross);
			evaluation=cvd.getEvaluation();
			result=cvd;
		}else{
			cvd=null;
			evaluation=
					Tester.multiEvaluate(train.learner,d,trainTest.splitter,signal.cross);
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
		new TrainTestMultiClassifier().callMain(args);
	}
}
