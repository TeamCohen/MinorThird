/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import java.io.Serializable;

import javax.swing.JComponent;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OneVsAllClassifier;
import edu.cmu.minorthird.classify.OnlineClassifierLearner;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

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
    } catch (Exception ex) {
      throw new IllegalArgumentException("innerLearner must be cloneable");
    }
  }
  
  /** Wraps the OneVsAllClassifier, and provides a more convenient constructor. */ 
  public static class MultiClassClassifier extends OneVsAllClassifier implements Serializable
  {
  	static private final long serialVersionUID = 20080207L;
    
    public MultiClassClassifier(ExampleSchema schema,ClassifierLearner[] learners)
    {
      super(schema.validClassNames(), getBinaryClassifiers(learners));
    }
    public MultiClassClassifier(ExampleSchema schema,Classifier[] classifiers)
    {
      super(schema.validClassNames(), classifiers);
    }
    public ExampleSchema getSchema() { return new ExampleSchema(getClassNames()); }
    static public BinaryClassifier[] getBinaryClassifiers(ClassifierLearner[] learners) 
    {
      BinaryClassifier[] result = new BinaryClassifier[learners.length];
      for (int i=0; i<learners.length; i++) {
        result[i] = new MyBinaryClassifier(learners[i].getClassifier());
      }
      return result;
    }
    static private class MyBinaryClassifier extends BinaryClassifier implements Visible
    {
    	static final long serialVersionUID=20080207L;
      private Classifier c;
      public MyBinaryClassifier(Classifier c) { this.c = c; }
      @Override
			public double score(Instance instance) { return c.classification(instance).posWeight(); };
      @Override
			public String explain(Instance instance) { return c.explain(instance); }
	@Override
	public Explanation getExplanation(Instance instance) {
	    Explanation.Node top = c.getExplanation(instance).getTopNode();
	    Explanation ex = new Explanation(top);
	    return ex;
	}
      @Override
			public Viewer toGUI() { 
        Viewer v = new ComponentViewer() {
        	static final long serialVersionUID=20080207L;
            @Override
						public JComponent componentFor(Object o) {
              MyBinaryClassifier b = (MyBinaryClassifier)o;
              return (b.c instanceof Visible)?((Visible)b.c).toGUI():new VanillaViewer(c);				
            }
          };
        v.setContent(this);
        return v;
      }
      @Override
			public String toString() { return "[MyBC "+c+"]"; }
    };
  }
}

