package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/** Runs an Online Experiment
 *
 * @author Cameron Williams
 */

public class OnlineExperiment
{
    private TextLabels labels;
    private ClassifierAnnotator ann;
    private ClassifierLearner learner, oldLearner, newLearner;
    private String learnerName;
    private String spanType;

    private Evaluation evaluation = null, evaluation_old = null, evaluation_new = null;
    private Span.Looper documentIterator;
    private double numMistakes = 0; // mistakes made when bothe old data and new data is used
    private double oldMistakes = 0; // mistakes made when only old data is considered
    private double newMistakes = 0; // mistakes made when only new data is considered

    public OnlineExperiment(TextLabels labels, ClassifierAnnotator ann, ClassifierLearner learner, ClassifierLearner newLearner, String learnerName) {
	this.labels = labels;
	this.ann = ann;
	this.learner = learner;
	this.newLearner = newLearner;
	this.learnerName = learnerName;	
	this.spanType = ann.getLearnedSpanType();	
	if(spanType == null) throw new IllegalArgumentException("The annotator must be trained on a Span Type");
	this.documentIterator = labels.getTextBase().documentSpanIterator();

	if(!(learner instanceof OnlineBinaryClassifierLearner)) throw new IllegalArgumentException("The learner must be an OnlineBinaryClassifierLearner");
	Classifier c = ann.getClassifier();
	if(!(c instanceof Hyperplane)) throw new IllegalArgumentException("The classifier must be an instance of Hyperplane");	
	((OnlineBinaryClassifierLearner)learner).addClassifier((Hyperplane)c);
	this.oldLearner = learner; // Ignores second dataset
	
	onlineExperiment();
	oldExperiment();
	newExperiment();
	System.out.println("Number of Mistakes considering all data: " + numMistakes);
	System.out.println(evaluation.confusionMatrix().toString());
	System.out.println("");
	System.out.println("Number of Mistakes considering only old data: " + oldMistakes);
	System.out.println(evaluation_old.confusionMatrix().toString());
	System.out.println("");
	System.out.println("Number of Mistakes considering only new data: " + newMistakes);
	System.out.println(evaluation_new.confusionMatrix().toString());
    }

    private void onlineExperiment() {
	// do the testing and show the result
	Dataset d = nextDataset(labels,ann.getFE(),spanType);	
	evaluation = new Evaluation(d.getSchema());
	while(d != null) {
	    //new ViewerFrame("Dataset", d.toGUI());	    
	    evaluation.extend( learner.getClassifier(), d, 0 );	    

	    for(Example.Looper i=d.iterator(); i.hasNext(); ) {
		Example ex = i.nextExample();
		learner.addExample(ex);
	    }
	    d = nextDataset(labels, ann.getFE(), spanType);
	}
	numMistakes = evaluation.numErrors();
	//new ViewerFrame("Result", new SmartVanillaViewer(evaluation));
	//new ViewerFrame("Classifier", new SmartVanillaViewer(learner.getClassifier()));
	
    }

    private void oldExperiment() {
	// do the testing and show the result
	Dataset d = nextDataset(labels,ann.getFE(),spanType);
	evaluation_old = new Evaluation(d.getSchema());
	newLearner.reset();
	while(d != null) {    
	    evaluation_old.extend( ann.getClassifier(), d, 0 );	    
	    d = nextDataset(labels, ann.getFE(), spanType);
	}
	oldMistakes = evaluation_old.numErrors();
	//new ViewerFrame("Result_old", new SmartVanillaViewer(evaluation_old));
	//new ViewerFrame("Classifier_old", new SmartVanillaViewer(ann.getClassifier()));
	
    }

    private void newExperiment() {
	// do the testing and show the result
	Dataset d = nextDataset(labels,ann.getFE(),spanType);
	evaluation_new = new Evaluation(d.getSchema());
	newLearner.reset();
	while(d != null) {
	    //new ViewerFrame("Dataset", d.toGUI());	        
	    evaluation_new.extend( newLearner.getClassifier(), d, 0 );

	    for(Example.Looper i=d.iterator(); i.hasNext(); ) {
		Example ex = i.nextExample();
		newLearner.addExample(ex);
	    }
	    d = nextDataset(labels, ann.getFE(), spanType);
	}
	newMistakes = evaluation_new.numErrors();
	//new ViewerFrame("Result_new", new SmartVanillaViewer(evaluation_new));
	//new ViewerFrame("Classifier_new", new SmartVanillaViewer(learner.getClassifier()));
	
    }

    /** Build a classification dataset from the next Document 
     */
    public Dataset nextDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanType)
    {
	// use this to print out a summary
	Map countByClass = new HashMap();

	NestedTextLabels safeLabels = new NestedTextLabels(textLabels);

	// binary dataset - anything labeled as in this type is positive

	if (spanType!=null && documentIterator.hasNext()) {
	    Dataset dataset = new BasicDataset();
	    Span s = documentIterator.nextSpan();
	    int classLabel = textLabels.hasType(s,spanType) ? +1 : -1;
	    int negClassLabel = textLabels.hasType(s,"NOT"+spanType) ? +1 : -1;
	    //if(classLabel > 0 || negClassLabel > 0) {
		String className = classLabel<0 ? ExampleSchema.NEG_CLASS_NAME : ExampleSchema.POS_CLASS_NAME;
		dataset.add( new Example( fe.extractInstance(safeLabels,s), ClassLabel.binaryLabel(classLabel)) );
		Integer cnt = (Integer)countByClass.get( className );
		if (cnt==null) countByClass.put( className, new Integer(1) );
		else countByClass.put( className, new Integer(cnt.intValue()+1) );
		//}			    
	    System.out.println("Number of examples by class: "+countByClass);
	    return dataset;
	}
	documentIterator = labels.getTextBase().documentSpanIterator();
	return null;
    }

}