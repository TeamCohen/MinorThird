package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

/** 
 * Multivariate logistic regression.  Follows the iterative
 * boosting-like approach outlined in "Logistic Regression, AdaBoost
 * and Bregman Distances", by Collins, Schapire, and Singer, Machine
 * Learning 48(1/2/3), July/August/Sep 2002, specifically the
 * algorithm of Figure 1 parameterized by using LogLoss (Equation 21).
 *
 * @author William Cohen
 */

public class LogisticRegressor extends BatchBinaryClassifierLearner
{
	static private Logger log = Logger.getLogger(LogisticRegressor.class);
	static private final boolean DEBUG = log.getEffectiveLevel().isGreaterOrEqual( Level.DEBUG );

	static public final Feature UNIVARIATE_SLOPE = new Feature("x");
	static public final Feature UNIVARIATE_INTERCEPT = Hyperplane.BIAS_TERM;

	// regression will push values to 1-EPSILON or -1+EPSILON
	static private final double EPSILON = 0.01;

	// convergence control
	private int maxRounds = 10;
	private double tolerance = 0.01;

	/**
	 * @param maxRounds = maximum number of iterations to use in optimization.
	 * @param tolerance = the tolerance required for parameters.
	 * Optimization will be halted after any iteration in which no parameter changes by at least this much.
	 */
	public LogisticRegressor(int maxRounds,double tolerance)
	{
		this.maxRounds = maxRounds;
		this.tolerance = tolerance;
	}
	public LogisticRegressor() { this(20,0.001);	}

	public int getMaxRounds() { return maxRounds; }
	public void setMaxRounds(int n) { maxRounds=n; }	
	public double getTolerance() { return tolerance; }
	public void setTolerance(double t) { this.tolerance=t; }

	//
	// training algorithm
	//

	public Classifier batchTrain(Dataset data)
	{
		// compute scale factor - used to ensure that |M_ij|<=1
		double scaleFactor = 1.0;
		for (Example.Looper i=data.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			for (Feature.Looper j=e.featureIterator(); j.hasNext(); ) {
				Feature fj = j.nextFeature();
				double a = MathUtil.abs( e.getWeight(fj) );
				if (a>=scaleFactor) scaleFactor = a;
			}
		}

		// lambda in Collins et al's notation - the weight vectors to learn
		Hyperplane lambda = new Hyperplane();

		// q_t, initialized to zero
		double[] q = new double[ data.size() ];

		// q_{t-1} in update, initialized to 0.5
		double[] qPrevious = new double[ data.size() ];
		for (int i=0; i<data.size(); i++) qPrevious[i] = 0.5;

		// delta_{t-1} in top of loop below, then delta_t at end of loop, initialize to zero's
		Hyperplane delta = new Hyperplane();

		// main iteration

		ProgressCounter pc = new ProgressCounter("logistic regression", "iteration", maxRounds);

		Example.Looper it = null;
		boolean converged = false;
		for (int t=1; t<=maxRounds && !converged; t++) {

			log.info("iteration: "+t);

			// compute q_t from q_{t-1}, delta_{t-1}, M_{i,j}
			it = data.iterator();
			for (int i=0; i<data.size(); i++) {
				Example ei = it.nextExample();
				double tmpSum = 0.0;
				for (Feature.Looper j=ei.featureIterator(); j.hasNext(); ) {
					Feature fj = j.nextFeature();
					tmpSum += delta.featureScore( fj ) * m(ei,fj,scaleFactor);
				}
				tmpSum += delta.featureScore( Hyperplane.BIAS_TERM ) * m(ei,Hyperplane.BIAS_TERM,scaleFactor);
				q[i] = qPrevious[i] / ( (1.0-qPrevious[i]) * Math.exp( tmpSum ) + qPrevious[i] );
				if (DEBUG) log.debug("q["+i+"]="+q[i]+" tmpSum:"+ tmpSum+" on iteration: "+t);
			}
			for (int i=0; i<q.length; i++) qPrevious[i] = q[i];

			// compute W_t^+ and W_t^-
			Hyperplane wPos = new Hyperplane();
			Hyperplane wNeg = new Hyperplane();
			it = data.iterator();
			for (int i=0; i<data.size(); i++) {
				Example ei = it.nextExample();
				for (Feature.Looper j=ei.featureIterator(); j.hasNext(); ) {
					Feature fj = j.nextFeature();
					double mij = m(ei,fj,scaleFactor);
					Hyperplane wij = mij>=0 ? wPos : wNeg;
					wij.increment(fj, q[i]*Math.abs(mij) );
				}
				Feature fb = Hyperplane.BIAS_TERM;
				double mib = m(ei,fb,scaleFactor);
				Hyperplane wij = mib>=0 ? wPos : wNeg;
				wij.incrementBias( q[i]*Math.abs(mib) );
			}

			// compute delta_t, and check convergence
			converged = true;
			delta = new Hyperplane();
			double epsilon = Math.min( 0.5/data.size(), 0.001); // smoothing
			for (Feature.Looper j=wPos.featureIterator(); j.hasNext(); ) {
				Feature fj = j.nextFeature();
				double tmp = confidence(fj,wPos,wNeg,epsilon);
				delta.increment(fj, tmp);
				if (Math.abs(tmp)>tolerance) converged=false;
			}

			// update lambda
			lambda.increment( delta );

			if (DEBUG) {
				log.debug("q:     "+StringUtil.toString(q));
				log.debug("wPos:  "+wPos);
				log.debug("wNeg:  "+wNeg);
				log.debug("delta: "+delta);
				log.debug("lambda: "+lambda);
			}

			pc.progress();

		} // for t=1 to MAX_ROUNDS

		pc.finished();

		return lambda;
	}

	// a useful subcomputation in 'regress'
	private double confidence(Feature f,Hyperplane wPos,Hyperplane wNeg,double epsilon)
	{
		return Math.log( wPos.featureScore(f)+epsilon ) - Math.log( wNeg.featureScore(f)+epsilon ) ;
	}

	// M_ij in Collins et al's notation, after scaling
	private double m(Example ei,Feature fj,double scaleFactor)
	{
		double w = (fj==Hyperplane.BIAS_TERM) ? 1.0 : ei.getWeight(fj);
		return ei.getLabel().numericScore() *  w / scaleFactor;
	}

	public String toString()
	{
		return "[LogisticRegressor maxRounds:"+maxRounds+" tolerance:"+tolerance+"]";
	}

	//
	// univariate case
	//

	private static class UnivariateInstance implements Instance
	{
		static private Set featureSet = Collections.singleton(UNIVARIATE_SLOPE);
		private double x;
		public UnivariateInstance(double x)	{ this.x = x; }
		public double getWeight(Feature f) { return x; }
		public Feature.Looper binaryFeatureIterator() { return new Feature.Looper(Collections.EMPTY_SET); }
		public Feature.Looper numericFeatureIterator() { return new Feature.Looper(featureSet); }
		public Feature.Looper featureIterator() { return numericFeatureIterator(); }
		public double getWeight() { return 1.0; }
		public Object getSource() { return Double.toString(x); }
		public String getSubpopulationId() { return null; }
		public String toString() { return "[UnivarInst x="+x+"]"; }
		public Viewer toGUI() { return new VanillaViewer(this); }
	}

	/** Create a binary example with one independent variable x, and class y. 
	 */
	public static BinaryExample univariateExample(double x,double y)
	{
		return new BinaryExample(new UnivariateInstance(x), y);
	}

	

	/** A simple test routine. */
	public static void main(String[] args)
	{
		int m=20;
		double a=0.5,b=2;
		if (args.length>0) {
			try {
				m = Integer.parseInt(args[0]);
				a = Double.parseDouble(args[1]);
				b = Double.parseDouble(args[2]);
			} catch (Exception ex) {
				System.out.println("usage: m a b");
			}
		}
		System.out.println("test on "+m+" data points from logistic("+a+"x + "+b+")");

		LogisticRegressor lr = new LogisticRegressor();
		Dataset data = SampleDatasets.makeLogisticRegressionData(new Random(0),m,a,b);
		BinaryClassifier c = lr.batchTrainBinary(data);
		System.out.println("result is "+c);
		for (Example.Looper i=data.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			double x = e.getWeight(UNIVARIATE_SLOPE);
			double y = e.getLabel().numericScore();
			double yHat = MathUtil.logistic( c.score(e) );
			System.out.println("x: "+x+" p: "+MathUtil.logistic(a*x+b)+" pHat: "+yHat+" actual: "+y);
		}
		System.out.println("avg logLoss = "+Tester.logLoss(c,data));
	}
}
