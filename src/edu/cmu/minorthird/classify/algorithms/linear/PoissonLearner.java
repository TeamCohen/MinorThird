package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Naive Bayes Poisson Classifier.
 *
 * @author Edoardo Airoldi
 */

/*
 * classify with maximum value of of Pr(class|instance)
 *
 * Pr(class|instance) = Pr(instance|class)*Pr(class)
 * = log Pr(instance|class)*Pr(class)
 * = log prod_f Pr(f|class)*Pr(class)
 * = sum_f log Pr(f|class) + log Pr(class)
 *
 * score is log odds = log( Pr(+|instance) / Pr(-|instance) )
 *  = sum_f log [Pr(f|+)-Pr(f|-)] + log Pr(+) - log Pr(-i)
 *  = sum_f { -mu(+) +mu(-) + f_counts * [ log mu(+) - log mu(-) ] } + log Pr(+) - log Pr(-)
 *
 * where:
 *       f_counts = {counts for feature f over all the examples}
 *     f_in_ex(.) = {counts for feature f in example ex of class .}
 *  MLE for mu(.) = [ sum_ex(.) f_in_ex(.) ] / [ sum_ex(.) sum_f f_in_ex(.) ]
*/

public class PoissonLearner implements BinaryClassifierLearner
{

    static private Logger log = Logger.getLogger(PoissonLearner.class);

	private static final boolean LOG = true;
	private TreeMap numGivenPos,numGivenNeg;
	private Set featureSet;
	double numPos, numNeg;

	public PoissonLearner() { reset(); }

	public void reset()
	{
		numGivenPos = new TreeMap();
		numGivenNeg = new TreeMap();
		featureSet = new HashSet();
		numPos=0;
		numNeg=0;
	}

	final public void setSchema(ExampleSchema schema)
	{
		if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)) {
			throw new IllegalStateException("can only learn binary example data");
		}
	}

	public void setInstancePool(Instance.Looper i) {;}
	public boolean hasNextQuery() { return false; }
	public Instance nextQuery() { return null; }

	public void addExample(Example example)
	{
        //System.out.println("In addExample"); // Edo test
        boolean isPos = example.getLabel().isPositive();
        //System.out.println("example = " + example.toString() + " weight=" + example.getWeight()); // Edo test
        double total = 0.0;
        for (Feature.Looper i=example.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
            //System.out.println("retriving feature : " + f.toString() + " with weight : " + example.getWeight(f));
			double wgt = example.getWeight(f);
            if (isPos) {
                Double d = (Double)numGivenPos.get(f);
                if (d==null) numGivenPos.put(f, new Double(wgt));
                else numGivenPos.put(f, new Double(d.doubleValue()+wgt) );
            } else {
                Double d = (Double)numGivenNeg.get(f);
                if (d==null) numGivenNeg.put(f, new Double(wgt));
                else numGivenNeg.put(f, new Double(d.doubleValue()+wgt));
            }
            total += wgt;
			featureSet.add( f );
        }
        if (isPos)numPos += total;
        else numNeg += total;
        //System.out.println("Out addExample"); // Edo test
	}

	public Classifier getClassifier() {
		return getBinaryClassifier();
	}

	public BinaryClassifier getBinaryClassifier()
	{
        // Evaluate the logg-odds at ML estimates
		PoissonClassifier c = new PoissonClassifier();
        c.setScale(10.0); // Method-Selector should learn it!
		double featurePrior = 1.0 / featureSet.size();
        //System.out.println("size=" + featureSet.size() + " prior=" +featurePrior);
		for (Iterator i=featureSet.iterator(); i.hasNext(); ) {
			Feature f = (Feature)i.next();
            double ngp = ( numGivenPos.get(f)==null ? 0.0 : ((Double)numGivenPos.get(f)).doubleValue() );
            double ngn = ( numGivenNeg.get(f)==null ? 0.0 : ((Double)numGivenNeg.get(f)).doubleValue() );
            //System.out.println(numGivenPos);
            //System.out.println(numGivenNeg);
            //System.out.println("feature:" + f + ", " + ngp + " " + ngn);
            //System.out.println(c.getScale());
            double pweight = estimatedProb( ngp, numPos/c.getScale(), featurePrior, 1.0/c.getScale() );
            double nweight = estimatedProb( ngn, numNeg/c.getScale(), featurePrior, 1.0/c.getScale() );
            c.increment( f, -pweight +nweight );
            pweight = estimatedProb( ngp, numPos/c.getScale(), featurePrior, 1.0/c.getScale(), LOG );
            nweight = estimatedProb( ngn, numNeg/c.getScale(), featurePrior, 1.0/c.getScale(), LOG );
            c.increment( f, pweight - nweight, LOG );
		}
        c.incrementBias( +estimatedProb(numPos, numPos+numNeg, 0.5, 1.0, LOG ) );
        c.incrementBias( -estimatedProb(numNeg, numPos+numNeg, 0.5, 1.0, LOG ) );
        //System.out.println("out of classifier");
		return c;
	}

    /** Compute the maximum likelihood estimate of the rate 'mu' of a Poisson model,
     *  using integer counts x[] from examples with different lengths omega[].
     */
    public double MaximumLikelihoodPoisson(double[] x, double[] omega) {
        double sumX=0;
        double sumOmega=0;
        for(int i=0; i<x.length; i++){
            sumX += x[i];
            sumOmega += omega[i];
        }
        double mu = sumX/sumOmega;
        return mu;
    }

	double estimatedProb(double k,double n, double prior, double pseudoCounts) {
        //System.out.println("psudoCounts:" + k); // Edo test
		return (k+prior*pseudoCounts) / (n+pseudoCounts);
	}

   	double estimatedProb(double k,double n, double prior, double pseudoCounts, boolean log) {
        //.out.println("psudoCounts:" + k); // Edo test
		return Math.log( (k+prior*pseudoCounts) / (n+pseudoCounts) );
	}


}
