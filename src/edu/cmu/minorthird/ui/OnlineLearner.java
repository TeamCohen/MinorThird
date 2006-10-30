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

    protected CommandLineUtil.OnlineBaseParams labeledData = new CommandLineUtil.OnlineBaseParams();   
    private CommandLineUtil.OnlineSignalParams signal = new CommandLineUtil.OnlineSignalParams(labeledData);
    private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
    private CommandLineUtil.TestClassifierParams test = new CommandLineUtil.TestClassifierParams();
    OnlineTextClassifierLearner textLearner = null;
    private Classifier classifier = null;

    public CommandLineUtil.OnlineBaseParams get_LabeledDataParameters() { return labeledData; }
    public void set_LabeledDataParameters(CommandLineUtil.OnlineBaseParams labeledData) { this.labeledData=labeledData; }
    public CommandLineUtil.OnlineSignalParams getSignalParameters() { return signal; } 
    public void setSignalParameters(CommandLineUtil.OnlineSignalParams p) { signal=p; } 
    public CommandLineUtil.TrainClassifierParams getAdditionalParameters() { return train; } 
    public void setAdditionalParameters(CommandLineUtil.TrainClassifierParams p) { train=p; }
    public CommandLineUtil.TestClassifierParams getTextLearnerParameters() { return test; } 
    public void setTextLearnerParameters(CommandLineUtil.TestClassifierParams p) { test=p; } 

    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,labeledData,signal,train,test});
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
	    
	String outputType = signal.getOutputType(train.output);
	if(test.loadFrom == null)
	    textLearner = new OnlineBinaryTextClassifierLearner((OnlineClassifierLearner)train.learner,signal.spanType, labeledData.labeledData, train.fe);
	else {
	    try { 
		OnlineBinaryTextClassifierLearner obtcl = (OnlineBinaryTextClassifierLearner)IOUtil.loadSerialized(test.loadFrom);
		textLearner = (OnlineTextClassifierLearner)obtcl;
	    } catch (IOException ex) {
		throw new IllegalArgumentException("can't load annotator from "+test.loadFrom+": "+ex);
	    }
	}


	TextLabels annLabels;	    
	annLabels = textLearner.annotatedCopy((TextLabels)base.labels);
		
	OnlineLearnerEditor editor = OnlineLearnerEditor.edit(annLabels, (MutableTextLabels)base.labels, base.repositoryKey, textLearner);
    }

    public Object getMainResult() { return textLearner.getClassifier(); }

    public static void main(String args[])
    {
	new OnlineLearner().callMain(args);
    }
}
