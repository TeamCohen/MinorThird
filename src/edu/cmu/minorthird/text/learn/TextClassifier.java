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

/** Provides a way to find the score of a String rather than an instance
 *
 * @author Cameron Williams
 */

public interface TextClassifier
{
    /** Returns the weight for a String being in the positive class */
    public double score(String text);

}