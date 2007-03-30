package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.util.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/** Provides a way to Edit document labels and add them to the learner
 *
 * @author Cameron Williams
 */

public class OnlineBinaryTextClassifierLearner extends AbstractAnnotator implements OnlineTextClassifierLearner, Serializable
{
    // internal state       
    private SpanFeatureExtractor fe = null;
    public OnlineClassifierLearner learner;
    public String spanType, outputType;
    private int docNum;

    private final static String DOC = "OnlineDocument_";

    public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner, String spanType)
    {
	this(learner, spanType, null, null);
    }

    public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner, String spanType, TextLabels labeledData)
    {
	this(learner, spanType, labeledData, null);
    }

    public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner, String spanType, TextLabels labeledData, SpanFeatureExtractor fe)
    {
	this.learner = learner;
	this.spanType = spanType;
	this.outputType = "_predicted_"+spanType;
	this.docNum = 0;
	if (fe != null)
	    this.fe = fe;
	else this.fe = new Recommended.DocumentFE();
	if(labeledData != null)
	    addLabeledData(labeledData);	
    }

    /** Add already labeled data to the learner */
    private void addLabeledData(TextLabels labels) {
	TextBase tb = labels.getTextBase();
	for(Span.Looper looper = tb.documentSpanIterator(); looper.hasNext(); ) {
	    Span s = looper.nextSpan();	    
	    int classLabel = labels.hasType(s,spanType) ? +1 : -1;

	    Instance i = fe.extractInstance(labels,s);
	    Example ex = new Example(i,  ClassLabel.binaryLabel(classLabel));

	    learner.addExample(ex);
	}
    }

    /** Provide document string with a label and add to the learner*/
    public void addDocument(String label, String text) 
    {
	BasicTextBase tb = new BasicTextBase();
	docNum++;
	String docID = DOC + docNum;
	tb.loadDocument(docID, text);

	MutableTextLabels textLabels = new BasicTextLabels(tb);	
	Span docSpan = tb.documentSpan(docID);
	textLabels.addToType(docSpan, label);

	int classLabel = textLabels.hasType(docSpan,spanType) ? +1 : -1;
	int negClassLabel = textLabels.hasType(docSpan,"NOT"+spanType) ? +1 : -1;

	Instance i = fe.extractInstance(textLabels,docSpan);
	Example ex = new Example(i,  ClassLabel.binaryLabel(classLabel));

	learner.addExample(ex);
    }

    /** Returns the TextClassifier */
    public TextClassifier getTextClassifier() {
	TextClassifier tc = new BinaryTextClassifier(learner, fe);
	return tc;
    }

    public Classifier getClassifier() {
	return learner.getClassifier();
    }

    /** Tells the learner that no more examples are coming */
    public void completeTraining() {
	learner.completeTraining();
    }

    /** Erases all previous data from the learner */
    public void reset() {
	learner.reset();
    }

    public String[] getTypes() {
	String[] types = {spanType, "NOT"+spanType};
	return types;
    }

    public void doAnnotate(MonotonicTextLabels labels)
    {
	Span.Looper candidateLooper =  labels.getTextBase().documentSpanIterator();

	Classifier c = learner.getClassifier();
	for (Span.Looper i=candidateLooper; i.hasNext(); ) {
	    Span s = i.nextSpan();
	    ClassLabel classOfS = c.classification(fe.extractInstance(labels, s));
	    if (spanType!=null && classOfS.isPositive()) {
		labels.addToType(s,outputType);
	    }
	}
    }
    public String explainAnnotation(TextLabels labels,Span documentSpan)
    {
	Classifier c = learner.getClassifier();
	return c.explain(fe.extractInstance(labels,documentSpan));
    }

    public ClassifierAnnotator getAnnotator() {
	ClassifierAnnotator ann = new ClassifierAnnotator(fe,getClassifier(),outputType,null,null);	
	return ann;
    }
}
