package edu.cmu.minorthird.ui;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.experiments.CrossValidatedDataset;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.FixedTestSetSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.ui.CommandLineUtil.ClassificationSignalParams;
import edu.cmu.minorthird.ui.CommandLineUtil.SaveParams;
import edu.cmu.minorthird.ui.CommandLineUtil.SplitterParams;
import edu.cmu.minorthird.ui.CommandLineUtil.TrainClassifierParams;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

// to do:
//  show labels should be a better viewer
//  -baseType type

/**
 * Do a train/test experiment on a text classifier.
 * 
 * @author William Cohen
 */

public class TrainTestClassifier extends UIMain{

	protected static Logger log=Logger.getLogger(TrainTestClassifier.class);

	// private data needed to train a classifier

	protected SaveParams save=new SaveParams();
	protected ClassificationSignalParams signal=new ClassificationSignalParams(base);
	protected TrainClassifierParams train=new TrainClassifierParams();
	protected SplitterParams trainTest=new SplitterParams();
	protected Object result=null;

	// for GUI
	public SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(SaveParams save){
		this.save=save;
	}

	public ClassificationSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(ClassificationSignalParams signal){
		this.signal=signal;
	}

	public TrainClassifierParams getTrainingParameters(){
		return train;
	}

	public void setTrainingParameters(TrainClassifierParams train){
		this.train=train;
	}

	public SplitterParams getSplitterParameters(){
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

	public String getTrainTestClassifierHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TrainTestClassifier%20Tutorial.htm\">TrainTestClassifier Tutorial</A></html>";
	}

	@Override
	public void doMain(){
		// check that inputs are valid
		if(train.learner==null)
			throw new IllegalArgumentException("-learner must be specified");
		if(signal.spanProp==null&&signal.spanType==null)
			throw new IllegalArgumentException(
					"one of -spanProp or -spanType must be specified");
		if(signal.spanProp!=null&&signal.spanType!=null)
			throw new IllegalArgumentException(
					"only one of -spanProp or -spanType can be specified");

		// construct the dataset
		Dataset d=
				CommandLineUtil.toDataset(base.labels,train.fe,signal.spanProp,
						signal.spanType,signal.candidateType);

		// show the data if necessary
		if(train.showData)
			new ViewerFrame("Dataset",d.toGUI());

		// construct the splitter, if necessary
		if(trainTest.labels!=null){
			if(signal.spanPropString!=null)
				CommandLineUtil.createSpanProp(signal.spanPropString,trainTest.labels);
			Dataset testData=
					CommandLineUtil.toDataset(trainTest.labels,train.fe,signal.spanProp,
							signal.spanType,signal.candidateType);
			trainTest.splitter=new FixedTestSetSplitter<Example>(testData.iterator());
		}

		// do the experiment
		CrossValidatedDataset cvd=null;
		Evaluation evaluation=null;
		if(trainTest.showTestDetails){
			cvd=new CrossValidatedDataset(train.learner,d,trainTest.splitter);
			evaluation=cvd.getEvaluation();
			result=cvd;
		}else{
			cvd=null;
			evaluation=Tester.evaluate(train.learner,d,trainTest.splitter);
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
		new TrainTestClassifier().callMain(args);
	}
}
