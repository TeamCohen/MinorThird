package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.BatchClassifierLearner;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.sequential.BeamSearcher;
import edu.cmu.minorthird.classify.sequential.CMM;
import edu.cmu.minorthird.classify.sequential.CRFLearner;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

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
	public boolean logSpace = true;
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
    
	public void setLogSpace(boolean b) {
		if(b)
	    crfLearner.setLogSpaceOption();
		else crfLearner.removeLogSpaceOption();
		logSpace = b;
	}
	public boolean getLogSpace() {
		return logSpace;
	}

	@Override
	public void setSchema(ExampleSchema schema) { crfLearner.setSchema(schema); }
	
	@Override
	public ExampleSchema getSchema(){return crfLearner.getSchema();}
	
	@Override
	public Classifier batchTrain(Dataset dataset)
	{
		SequenceDataset seqData = new SequenceDataset();
		for (Iterator<Example> i=dataset.iterator(); i.hasNext(); ) {
	    Example e = i.next();
	    seqData.addSequence( new Example[]{e} );
		}
		CMM c = (CMM)crfLearner.batchTrain(seqData);
		return new MyClassifier(c.getClassifier(),seqData.getSchema(),scaleScores);
	}
    
	public static class MyClassifier implements Classifier,Serializable,Visible
	{
		static private final long serialVersionUID = 20080128L;
	
		private Classifier c;
		private ExampleSchema schema;
		private boolean scaleScores;
	
		public MyClassifier(Classifier c,ExampleSchema schema,boolean scaleScores) 
		{	
	    this.c = c;	
	    this.schema=schema;
	    this.scaleScores=scaleScores; 
		}
		@Override
		public ClassLabel classification(Instance instance) 
		{
	    // classify transformed instance
	    ClassLabel label = c.classification( BeamSearcher.getBeamInstance(instance,1) );
	    return scaleScores?transformScores(label):label;
		}
		@Override
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
	
		@Override
		public Explanation getExplanation(Instance instance) {
	    Explanation.Node top = new Explanation.Node("MaxEntClassifier Explanation");
	    Instance augmentedInstance = BeamSearcher.getBeamInstance(instance,1);
	    if (scaleScores) {
				Explanation.Node ai = new Explanation.Node("Augmented instance: "+augmentedInstance);		      
				String augmentedEx = c.explain(augmentedInstance);
				String[] split = augmentedEx.split("\n");
				Explanation.Node curTopNode = ai;
				for(int i=0; i<split.length; i++) {		    
					Explanation.Node exNode = new Explanation.Node(split[i]);		   
					if(split[i].charAt(0) != ' ') {
						curTopNode = exNode;
						ai.add(exNode);
					} else curTopNode.add(exNode);
				}
				top.add(ai);
				Explanation.Node ts = new Explanation.Node("\nTransformed score: "+classification(instance));
				top.add(ts);
	    } else {		   
				Explanation.Node ai = new Explanation.Node("Augmented instance: "+augmentedInstance);		      
				String augmentedEx = c.explain(augmentedInstance);
				String[] split = augmentedEx.split("\n");
				Explanation.Node curTopNode = ai;
				for(int i=0; i<split.length; i++) {
					Explanation.Node exNode = new Explanation.Node(split[i]);
					if(split[i].charAt(0) != ' ') {
						curTopNode = exNode;
						ai.add(exNode);
					} else curTopNode.add(exNode);
				}
				top.add(ai);
	    }
	    Explanation ex = new Explanation(top);		    
	    return ex;
		}

		/** convert from log pseudoProbabilities to log probabilities */
		// in principle, the MaxEntLearner's weights will converge so that
		// Prob(x,y) = logistic( sum_i fi(x,y) ).  In experiments on on
		// artificial two-class problems, weights for POS and NEG
		// hyperplanes approximate 1/2 the actual coefficients of the
		// logistic term.  Eg, if Prob(y=+|x) = logistic(ax+b), then the
		// total bias terms approach b/2, 
		// and the total weight for x approaches a/2.  The pos score for
		// "POS" with instance is then (1/2 * (ax+b)), and the score for
		// "NEG" is (-1/2 * (ax +b)).  This transform takes care of that
		// and converts to log-odds scores.
		//
		private ClassLabel transformScores(ClassLabel label)
		{
	    double[] pseudoProb = new double[schema.getNumberOfClasses()];
	    double normalizer = 0;
	    for (int i=0; i<schema.getNumberOfClasses(); i++) {
				String yi = schema.getClassName(i);
				pseudoProb[i] = Math.exp( label.getWeight(yi) );
				normalizer += pseudoProb[i];
	    }
	    ClassLabel transformed = new ClassLabel();
	    for (int i=0; i<schema.getNumberOfClasses(); i++) {
				String yi = schema.getClassName(i);
				double p = pseudoProb[i]/normalizer;
				transformed.add( yi, Math.log(p/(1-p)) );
	    }
	    //System.out.println("schema: "+StringUtil.toString(schema.validClassNames()));
	    //System.out.println("Pseudo: "+label.toDetails());
	    //System.out.println("exp:    "+StringUtil.toString(pseudoProb));
	    //System.out.println("z="+normalizer);
	    //System.out.println("Xform:  "+transformed.toDetails());
	    return transformed;
		}

		public Classifier getRawClassifier() { return c; }

		@Override
		public Viewer toGUI()
		{
	    Viewer v = new TransformedViewer(new SmartVanillaViewer()) {
	    	static final long serialVersionUID=20080128L;
					@Override
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
