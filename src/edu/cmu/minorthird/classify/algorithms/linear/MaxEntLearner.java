package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.Serializable;
import java.util.*;

/**
 * Maximum entropy learner.  Based on calling the CRFLearner with
 * sequences of length 1.
 *
 * @author William cohen
 */

public class MaxEntLearner extends BatchClassifierLearner
{
	private CRFLearner crfLearner;
  private boolean scaleScores=false;
	public MaxEntLearner(){	crfLearner = new CRFLearner("",1); }
	/**
	 * String is list of parameter-value pairs, e.g.
	 * "maxIters 20 mForHessian 5".
	 * <p>
	 * Allowed parameters: 
	 * <ul>
	 *<li>doScaling 
	 *<li>epsForConvergence: Convergence criteria for finding optimum lambda using BFGS 
	 *<li>initValue:  initial value for all the lambda arrays 
	 *<li>invSigmaSquare: 
          penalty term for likelihood function is ||lambda||^2*invSigmaSquare/2
					set this to zero, if no penalty needed 
	 *<li>maxIters: Maximum number of iterations over the training data during training 
   *<li>mForHessian: The number of corrections used in the BFGS update. 
   *<li>trainerType: ...  
   *<li>scaleScores: if 1, scale scores by assuming each class's hyperplane is log-odds of belonging to that class
	 *</ul>
	 * For more info, see the docs for the iitb.CRF package.
	 */
	public MaxEntLearner(String args) 
  {	
    crfLearner = new CRFLearner(args,1); 
    if (args.indexOf("scaleScores 1")>=0) {
      scaleScores=true;
      System.out.println("scaleScores => true");
    }
  }
	public void setSchema(ExampleSchema schema) { crfLearner.setSchema(schema); }
	
	public Classifier batchTrain(Dataset dataset)
	{
		SequenceDataset seqData = new SequenceDataset();
		for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			seqData.addSequence( new Example[]{e} );
		}
		CMM c = (CMM)crfLearner.batchTrain(seqData);
		return new MyClassifier(c.getClassifier(),seqData.getSchema(),scaleScores);
	}
	
	public static class MyClassifier implements Classifier,Serializable,Visible
	{
		static private long serialVersionUID = 2;
		private final int CURRENT_SERIAL_VERSION = 2;

		private Classifier c;
    private ExampleSchema schema;
    private boolean scaleScores;

		public MyClassifier(Classifier c,ExampleSchema schema,boolean scaleScores) 
    {	
      this.c = c;	
      this.schema=schema;
      this.scaleScores=scaleScores; 
    }
		public ClassLabel classification(Instance instance) 
    {
      // classify transformed instance
      ClassLabel label = c.classification( BeamSearcher.getBeamInstance(instance,1) );
      return scaleScores?transformScores(label):label;
		}
		public String explain(Instance instance) 
    {
      Instance augmentedInstance = BeamSearcher.getBeamInstance(instance,1);
      if (scaleScores) {
        return
          "Augmented instance: "+augmentedInstance+"\n" 
          + c.explain(augmentedInstance) + "\nTransformed score: "+classification(instance);
      } else {
        return
          "Augmented instance: "+augmentedInstance+"\n" 
          + c.explain(augmentedInstance);
      }
		}

    // in principle, the MaxEntLearner's weights will converge so that
    // Prob(x,y) = logistic( sum_i fi(x,y) ).  In experiments on on
    // artificial two-class problems, weights for POS and NEG
    // hyperplanes approximate 1/2 the actual coefficients of the
    // logistic term.  Eg, if Prob(y=+|x) = logistic(ax+b), then the
    // total bias terms approach b/2, //and the total weight for x
    // approaches a/2.  The pos score for "POS" with instance is then
    // (1/2 * (ax+b)), and the score for "NEG" is (-1/2 * (ax +b)).
    // This transform takes care of that and converts to log-odds
    // scores.
    //
    private ClassLabel transformScores(ClassLabel label)
    {
      ClassLabel transformed = new ClassLabel();
      for (int i=0; i<schema.getNumberOfClasses(); i++) {
        String y1 = schema.getClassName(i);
        double s = label.getWeight(y1);
        for (int j=0; j<schema.getNumberOfClasses(); j++) {
          String y2 = schema.getClassName(j);
          if (!y1.equals(y2)) s -= label.getWeight(y2);
        }
        double p1 = MathUtil.logistic(s);
        double odds1 = Math.log( p1/(1.0-p1) );
        transformed.add( y1,odds1 );
      }
      return transformed;
    }
		//public SequenceClassifier getSequenceClassifier() { return c; }
		public Viewer toGUI()
		{
			Viewer v = new TransformedViewer(new SmartVanillaViewer()) {
					public Object transform(Object o) {
						MyClassifier mycl = (MyClassifier)o;
						return mycl.c;
					}
				};
			v.setContent(this);
			return v;
		}
	}
}
