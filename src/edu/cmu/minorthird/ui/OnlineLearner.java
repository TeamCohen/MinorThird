package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Allows you to add examples to an Annotator
 *
 * @author Cameron Williams
 */

public class OnlineLearner extends UIMain
{

	// private data needed to test a classifier

    private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
    private CommandLineUtil.OnlineLearnerParams olp = new CommandLineUtil.OnlineLearnerParams();
    private TextLabels annLabels = null;

	// for gui
	public CommandLineUtil.OnlineLearnerParams getOnlineLearnerParams() { return olp; }
	public void setOnlineLearnerParameters(CommandLineUtil.OnlineLearnerParams p) { olp=p; }

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),olp});
	}

	//
	// load and test a classifier
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (olp.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");
		if (olp.data==null) throw new IllegalArgumentException("-data must be specified");

		// load the classifier
		Annotator ann = null;
		try {
			ann = (Annotator)IOUtil.loadSerialized(olp.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+olp.loadFrom+": "+ex);
		}

		if(!(ann instanceof ClassifierAnnotator)) throw new IllegalArgumentException("Annotator must me a ClassifierAnnotator");

		// do the annotation
		TextLabels annLabels;	    
		annLabels = ann.annotatedCopy((TextLabels)olp.data);

		String classLearnerName = ((ClassifierAnnotator)ann).getClassifierLearner();
		ClassifierLearner classLearner = (ClassifierLearner)CommandLineUtil.newObjectFromBSH(classLearnerName,ClassifierLearner.class); 
		
		if(!olp.experiment) {
		    OnlineLearnerEditor editor = OnlineLearnerEditor.edit(annLabels, olp.data, olp.repositoryKey, 
									  (ClassifierAnnotator)ann, classLearner, classLearnerName);
		} else {
		    OnlineExperiment onlineExpt = new OnlineExperiment((TextLabels)olp.data,(ClassifierAnnotator)ann, 
		    						       classLearner, classLearner, classLearnerName);
		}
	}

	public Object getMainResult() { return annLabels; }

	public static void main(String args[])
	{
		new OnlineLearner().callMain(args);
	}
}