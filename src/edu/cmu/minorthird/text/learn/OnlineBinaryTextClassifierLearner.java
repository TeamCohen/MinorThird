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

public class OnlineBinaryTextClassifierLearner
{
    // internal state       
    private SpanFeatureExtractor fe = null;
    public OnlineBinaryClassifierLearner learner;
    public ClassifierAnnotator ann = null;
    private int docNum;

    private final static String DOC = "OnlineDocument_";

    /**
     * @param viewLabels a superset of editLabels which may include some additional read-only information
     * @param editLabels the labels being modified
     * @param documentList the document Span being edited is associated with
     * the selected entry of the documentList.
     * @param spanPainter used to repaint documentList elements
     * @param statusMsg a JLabel used for status messages.
     */
    public OnlineBinaryTextClassifierLearner(OnlineBinaryClassifierLearner learner,
					     ClassifierAnnotator ann)
    {
	this.learner = learner;
	this.ann = ann;
	this.fe = ann.getFE();
	docNum = 0;
    }

    /** Provide document string with a label and add to the learner*/
    public void addDocument(String label, String text) 
    {
	String spanType = ann.getLearnedSpanType();

	TextBase tb = new BasicTextBase();
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

    /** Returns the Classifier */
    public TextClassifier getClassifier() {
	TextClassifier tc = new TextClassifier(learner, fe);
	return tc;
    }

    /** Tells the learner that no more examples are coming */
    public void completeTraining() {
	learner.completeTraining();
    }

    /** Erases all previous data from the learner */
    public void reset() {
	learner.reset();
    }


}