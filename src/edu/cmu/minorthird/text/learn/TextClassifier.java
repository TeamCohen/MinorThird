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

/** Provides a way to Edit document labels and add them to the learner
 *
 * @author Cameron Williams
 */

public class TextClassifier
{
    private OnlineBinaryClassifierLearner learner;
    private SpanFeatureExtractor fe = null;
    private int docNum;

    private final static String DOC = "OnlineDocument_";

    public TextClassifier(OnlineBinaryClassifierLearner learner, SpanFeatureExtractor fe)
    {
	this.learner = learner;
	this.fe = fe;
	docNum = 0;
    }
    
    public double score(String text) 
    {
	TextBase tb = new BasicTextBase();
	docNum++;
	String docID = DOC + docNum;
	tb.loadDocument(docID, text);

	//MutableTextLabels textLabels = new BasicTextLabels(tb);	
	Span docSpan = tb.documentSpan(docID);
	//textLabels.addToType(docSpan, label);

	//int classLabel = textLabels.hasType(docSpan,spanType) ? +1 : -1;
	//int negClassLabel = textLabels.hasType(docSpan,"NOT"+spanType) ? +1 : -1;

	Instance i = fe.extractInstance(/*textLabels,*/docSpan);
	Classifier c = learner.getClassifier();
	double score;
	if(c instanceof BinaryClassifier) 
	    score = ((BinaryClassifier)c).score(i);
	//else if(c instanceof MutinomialClassifier)
	//    score = ((MultinomialClassifier)c).score(i);
	else throw new IllegalArgumentException("The classifier must be binary or mutinomial");
	
	return score;
    }

}