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

public class PoissonLearner extends BatchBinaryClassifierLearner
{

  static private Logger log = Logger.getLogger(PoissonLearner.class);

  private static final boolean LOG = true;
  private double SCALE;

  public PoissonLearner() {
    this.SCALE = 10.0;
    reset();
  }
  public PoissonLearner(double scale) {
    this.SCALE = scale;
    reset();
  }


  public Classifier batchTrain(Dataset data)
  {
    TreeMap numGivenPos = new TreeMap();
    TreeMap numGivenNeg = new TreeMap();

    BasicFeatureIndex index = new BasicFeatureIndex(data);

    double numPos = (double)index.size("POS");
    double numNeg = (double)index.size("NEG");

    for (Feature.Looper i=index.featureIterator(); i.hasNext(); )
    {
      Feature f = i.nextFeature();
      //System.out.println("feature = "+f);
      numGivenPos.put(f, new Double( index.getCounts(f,"POS") ));
      numGivenNeg.put(f, new Double( index.getCounts(f,"NEG") ));
    }

    // Evaluate the logg-odds at ML estimates
    PoissonClassifier c = new PoissonClassifier();
    c.setScale(SCALE); // Method-Selector should learn it!
    double featurePrior = 1.0 / index.numberOfFeatures();
    //System.out.println("size=" + featureSet.size() + " prior=" +featurePrior);
    for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
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
