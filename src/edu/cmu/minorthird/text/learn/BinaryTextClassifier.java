package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.semisupervised.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.util.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.*;

/** Class the returns the score of a string rather than an instance
 *
 * @author Cameron Williams
 */

public class BinaryTextClassifier implements TextClassifier
{
    private OnlineClassifierLearner learner;
    private SpanFeatureExtractor fe = null;
    private int docNum;

    private final static String DOC = "OnlineDocument_";

    public BinaryTextClassifier(OnlineClassifierLearner learner, SpanFeatureExtractor fe)
    {
	this.learner = learner;
	this.fe = fe;
	docNum = 0;
    }
    
    /** Returns the weight for a String being in the positive class */
    public double score(String text) 
    {
	BasicTextBase tb = new BasicTextBase();
	docNum++;
	String docID = DOC + docNum;
	tb.loadDocument(docID, text);	
	Span docSpan = tb.documentSpan(docID);

	TextLabels textLabels = new EmptyLabels();
	Instance i = fe.extractInstance(textLabels,docSpan);
	Classifier c = learner.getClassifier();
	double score;
	if(c instanceof BinaryClassifier) 
	    score = ((BinaryClassifier)c).score(i);
	else throw new IllegalArgumentException("The classifier must be binary");
	
	return score;
    }

}
