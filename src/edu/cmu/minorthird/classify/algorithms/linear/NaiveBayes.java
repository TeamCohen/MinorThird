package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Naive Bayes algorithm.  If feature weights are word counts then this 
 * implements the usual multinomial naive Bayes.  If feature weights
 * are presence/absence this implements binomial naive Bayes.
 *
 * @author William Cohen
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
 *  = sum_f log [Pr(f|+) - Pr(f|-)] + log Pr(+) - log Pr(-i)
 */

public class NaiveBayes extends OnlineBinaryClassifierLearner
{
    private static Logger log = Logger.getLogger(NaiveBayes.class);

    private Hyperplane numGivenPos, numGivenNeg;
    private Set featureSet;
    private double numPos, numNeg;

    public NaiveBayes()	{ 
	super();
	reset();	
    }

    public void reset() 
    {
	super.reset();
	log.info("resetting NaiveBayes");
	numGivenPos = new Hyperplane();
	numGivenNeg = new Hyperplane();
	featureSet = new HashSet();
	numPos=0;
	numNeg=0;
    }

    public void addExample(Example example) 
    {
	boolean isPos = example.getLabel().isPositive();
	if (isPos)numPos += example.getWeight();
	else numNeg += example.getWeight();
	for (Feature.Looper i=example.featureIterator(); i.hasNext(); ) {
	    Feature f = i.nextFeature();
	    if (isPos) numGivenPos.increment(f, 1);
	    else numGivenNeg.increment(f, 1);
	    featureSet.add( f );
	}
    }

    public Classifier getClassifier() 
    {
	if(c == null)
	    c = new Hyperplane();
	for (Iterator i=featureSet.iterator(); i.hasNext(); ) {
	    Feature f = (Feature)i.next();
	    double featurePrior = getFeaturePrior();
	    double m = getFeaturePriorPseudoCount();
	    double pweight = estimatedLogProb( numGivenPos.featureScore(f), numPos, featurePrior, m );
	    double nweight = estimatedLogProb( numGivenNeg.featureScore(f), numNeg, featurePrior, m );
	    c.increment( f, pweight - nweight );
	}
	c.incrementBias( +estimatedLogProb(numPos, numPos+numNeg, 0.5, 1.0 ) );
	c.incrementBias( -estimatedLogProb(numNeg, numPos+numNeg, 0.5, 1.0 ) );
	// note these weights are multiplied by feature values when classifying,
	// which gives the multinomial Naive Bayes implementation.
	return c;
    }

    private double estimatedLogProb(double k, double n, double prior, double pseudoCounts) {
	return Math.log( (k+prior*pseudoCounts) / (n+pseudoCounts) );
    }

    private double getFeaturePrior() {
	return 1.0 / featureSet.size();
    }

    private double getFeaturePriorPseudoCount() {
	return 1.0;
    }

    public String toString() { return "[NaiveBayes]"; }
}
