/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import java.io.*;

/**
 * Utilities for sequential learning.
 *
 * @author William Cohen
 */

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

public class SequenceUtils
{
  /** Create an array of n copies of the prototype learner. */
  static public OnlineClassifierLearner[] duplicatePrototypeLearner(OnlineClassifierLearner prototype,int n)
    {
      try {
	OnlineClassifierLearner[] result = new OnlineClassifierLearner[n];		
	for (int i=0; i<n; i++) {
	  result[i] = (OnlineClassifierLearner)prototype.copy();
	  result[i].reset();
	}
	return result;
      } catch (CloneNotSupportedException ex) {
	throw new IllegalArgumentException("innerLearner must be cloneable");
      }
    }
  
  /** Wraps the OneVsAllClassifier, and provides a more convenient constructor. */ 
  public static class MultiClassClassifier extends OneVsAllClassifier implements Serializable
  {
    static private long serialVersionUID = 1;
    private int CURRENT_VERSION = 1;
    
    public MultiClassClassifier(ExampleSchema schema,ClassifierLearner[] learners)
    {
      super(schema.validClassNames(), getBinaryClassifiers(learners));
    }
    public MultiClassClassifier(ExampleSchema schema,BinaryClassifier[] classifiers)
    {
      super(schema.validClassNames(), classifiers);
    }
    static private BinaryClassifier[] getBinaryClassifiers(ClassifierLearner[] learners) 
    {
      BinaryClassifier[] result = new BinaryClassifier[learners.length];
      for (int i=0; i<learners.length; i++) {
	result[i] = new MyBinaryClassifier(learners[i].getClassifier());
      }
      return result;
    }
    static private class MyBinaryClassifier extends BinaryClassifier implements Visible
    {
      private Classifier c;
      public MyBinaryClassifier(Classifier c) { this.c = c; }
      public double score(Instance instance) { return c.classification(instance).posWeight(); };
      public String explain(Instance instance) { return c.explain(instance); }
      public Viewer toGUI() { 
	Viewer v = new ComponentViewer() {
	    public JComponent componentFor(Object o) {
	      MyBinaryClassifier b = (MyBinaryClassifier)o;
	      return (b.c instanceof Visible)?((Visible)b.c).toGUI():new VanillaViewer(c);				
	    }
	  };
	v.setContent(this);
	return v;
      }
      public String toString() { return "[MyBC "+c+"]"; }
    };
  }
}

