package edu.cmu.minorthird.text.learn;


/** Provides a way to find the score of a String rather than an instance
 *
 * @author Cameron Williams
 */

public interface TextClassifier
{
    /** Returns the weight for a String being in the positive class */
    public double score(String text);

}