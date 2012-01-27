package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OnlineClassifierLearner;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.EmptyLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

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
    @Override
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
