package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Start an Online Learner
 *
 * @author Cameron Williams
 */

public class OnlineLearner extends UIMain
{
    private static Logger log = Logger.getLogger(OnlineLearner.class);

    // private data needed to train a classifier

    protected CommandLineUtil.OnlineBaseParams unlabeledData = new CommandLineUtil.OnlineBaseParams();   
    private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams(base);
    private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
    OnlineTextClassifierLearner textLearner = null;
    private Classifier classifier = null;

    public CommandLineUtil.OnlineBaseParams get_UnlabeledDataParameters() { return unlabeledData; }
    public void set_UnlabeledDataParameters(CommandLineUtil.OnlineBaseParams unlabeledData) { this.unlabeledData=unlabeledData; }
    public CommandLineUtil.ClassificationSignalParams getSignalParameters() { return signal; } 
    public void setSignalParameters(CommandLineUtil.ClassificationSignalParams p) { signal=p; } 
    public CommandLineUtil.TrainClassifierParams getAdditionalParameters() { return train; } 
    public void setAdditionalParameters(CommandLineUtil.TrainClassifierParams p) { train=p; } 


    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,unlabeledData,signal,train});
    }

    //
    // do the experiment
    // 

    public void doMain()
    {
	// check that inputs are valid
	if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
	if (signal.spanType==null) 
	    throw new IllegalArgumentException("-spanType must be specified");
	if(!(train.learner instanceof OnlineBinaryClassifierLearner))
	    throw new IllegalArgumentException("The learner must be an OnlineBinaryClassifierLearner");
	if(unlabeledData.unlabeledData == null)
	    throw new IllegalArgumentException("You must specify some unlabeled Data");
	    
	String outputType = signal.getOutputType(train.output);
	textLearner = new OnlineBinaryTextClassifierLearner((OnlineBinaryClassifierLearner)train.learner,signal.spanType, base.labels, train.fe);
	ClassifierAnnotator ann = textLearner.getAnnotator();
	TextLabels annLabels;	    
	annLabels = ann.annotatedCopy((TextLabels)unlabeledData.unlabeledData);
		
	OnlineLearnerEditor editor = OnlineLearnerEditor.edit(annLabels, unlabeledData.unlabeledData, unlabeledData.repositoryKey, textLearner);
    }

    public Object getMainResult() { return textLearner.getClassifier(); }

    public static void main(String args[])
    {
	new OnlineLearner().callMain(args);
    }
}