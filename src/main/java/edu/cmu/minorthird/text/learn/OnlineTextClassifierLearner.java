package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.text.TextLabels;

/** Interface for OnlineLearner.  Allows you to add to a learner by specifying a string rather than a Span.
 *  Can return a TextClassifier, which scores a String rather than a span.
 *
 * @author Cameron Williams
 */

public interface OnlineTextClassifierLearner
{
    /** Provide document string with a label and add to the learner*/
    public void addDocument(String label, String text);

    /** Returns the TextClassifier */
    public TextClassifier getTextClassifier();

    /** Returns the Classifier */
    public Classifier getClassifier();

    /** Tells the learner that no more examples are coming */
    public void completeTraining();

    /** Erases all previous data from the learner */
    public void reset();

    /** Returns an array of spanTypes that can be added to the learner */
    public String[] getTypes();

    /** Returns an annotated copy of TextLabels */
    public TextLabels annotatedCopy(TextLabels labels);

    public ClassifierAnnotator getAnnotator();

}