package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.util.TreeMap;

/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */
public class T1InstanceTransformLearner implements InstanceTransformLearner {

  static private Logger log = Logger.getLogger(T1InstanceTransformLearner.class);
  private double REF_LENGTH; // word-length of the reference document
  private String PDF;
  // See static methods at the bottom of this class

  public T1InstanceTransformLearner() {
    this.REF_LENGTH = 100.0;
    this.PDF = "Poisson";
  }

  /** Accept an ExampleSchema - constraints on what the
   * Examples will be. */
  public void setSchema(ExampleSchema schema){
    if (!ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)) {
      throw new IllegalStateException("can only learn binary example data");
    }
  }

  /** Examine data, build an instance transformer */
  public InstanceTransform batchTrain(Dataset dataset){
    T1InstanceTransform T1Filter = new T1InstanceTransform();
    BasicFeatureIndex index = new BasicFeatureIndex(dataset);

    // prior for unseen words
    double featurePrior = 1.0/index.numberOfFeatures();

    // loop features
    for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
      Feature f = i.nextFeature();
      //System.out.println("feature: "+f); // DEBUG

      // fill array of <counts_ex(feature), length_ex> for POS class
      double[] xPos = new double[ index.size(f,"POS") ];
      double[] omegaPos = new double[ index.size(f,"POS") ];
      int position=0;
      for (int j=0; j<index.size(f); j++) {
        Example e = index.getExample(f,j);
        if ( "POS".equals( e.getLabel().bestClassName() ) ) {
          xPos[position] = e.getWeight(f);
          omegaPos[position] = getLength(e) / REF_LENGTH;
          position += 1;
        }
      }

      // fill array of <counts(example,feature), length(example)> for NEG class
      double[] xNeg = new double[ index.size(f,"NEG") ];
      double[] omegaNeg = new double[ index.size(f,"NEG") ];
      position=0;
      for (int j=0; j<index.size(f); j++) {
        Example e = index.getExample(f,j);
        if ( "NEG".equals( e.getLabel().bestClassName() ) ) {
          xNeg[position] = e.getWeight(f);
          omegaNeg[position] = getLength(e) / REF_LENGTH;
          position += 1;
        }
      }

      // estimate Parameters for the two classes and update the T1-Filter
      T1Filter.setT1( f,T1((int)index.getCounts(f,"POS"),(int)index.getCounts(f,"NEG")) );
      if ( PDF.equals("Poisson") ) {
        // learn Poisson parameters
        double muPos = MaximumLikelihoodPoisson(xPos,omegaPos,featurePrior);
        double muNeg = MaximumLikelihoodPoisson(xNeg,omegaNeg,featurePrior);
        // update T1 Filter
        T1Filter.setPosMu( f,muPos );
        T1Filter.setNegMu( f,muNeg );
        T1Filter.setFeaturePdf( f,"Poisson" );

      } else if ( PDF.equals("Negative-Binomial") ) {
        // learn Negative-Binomial parameters
        TreeMap mudeltaPos = MethodOfMomentsNegBin(xPos,omegaPos,featurePrior);
        TreeMap mudeltaNeg = MethodOfMomentsNegBin(xNeg,omegaNeg,featurePrior);
        // update T1 Filter
        T1Filter.setPosMu( f,((Double)mudeltaPos.get("mu")).doubleValue() );
        T1Filter.setPosDelta( f,((Double)mudeltaPos.get("delta")).doubleValue() );
        T1Filter.setNegMu( f,((Double)mudeltaNeg.get("mu")).doubleValue() );
        T1Filter.setNegDelta( f,((Double)mudeltaNeg.get("delta")).doubleValue() );
        if ( ((Double)mudeltaPos.get("delta")).doubleValue()==0.0 ||
            ((Double)mudeltaNeg.get("delta")).doubleValue()==0.0) { T1Filter.setFeaturePdf( f,"Poisson"); }
        else { T1Filter.setFeaturePdf( f,"Negative-Binomial"); }
      }
    }
    return T1Filter;
  }

  private String expandArray(double[] vec) {
    String buf="[ ";
    for (int i=0; i<vec.length; i++) {
      buf = buf + vec[i] + " ";
    }
    buf = buf + "]";
    return buf;
  }

  /** Get the total number of words in an Example */
  public double getLength(Example e) {
    double len=0.0;
    for (Feature.Looper i=e.featureIterator(); i.hasNext(); ) {
      Feature f = i.nextFeature();
      len += e.getWeight(f);
    }
    return len;
  }

  /** Compute the T1 statistic corresponding to the counts in two texts */
  public double T1(int x1, int x2) {
    double dx1 = new Integer(x1).doubleValue();
    double dx2 = new Integer(x2).doubleValue();
    double t = Math.pow( (dx1-dx2),2 ) / (dx1+dx2);
    return t;
  }

  /** Compute the maximum likelihood estimate of the rate 'mu' of a Poisson model,
   *  using integer counts x[] from examples with different lengths omega[].
   */
  public double MaximumLikelihoodPoisson(double[] x, double[] omega, double prior) {
    double mu;
    double sumX=0.0;
    double sumOmega=0.0;
    for(int i=0; i<x.length; i++){
      sumX += x[i];
      sumOmega += omega[i];
    }
    if (sumX==0.0 && sumOmega==0.0) { mu = prior/(1.0/REF_LENGTH); }
    else { mu = (sumX+prior)/(sumOmega+1.0/REF_LENGTH); }
    return mu;
  }

  /** Compute the method of moment estimates of the rate 'mu' and the parameter
   *  which controls the variability 'delta' of a Negative-Binomial models, using
   *  integer counts x[] from examples with different lengths omega[].
   */
  public TreeMap MethodOfMomentsNegBin(double[] x, double[] omega, double prior) {
    double j = x.length;
    double sumX=0.0;
    double sumOmega=0.0;
    double sumOmega2=0.0;
    for(int i=0; i<x.length; i++){
      sumX += x[i];
      sumOmega += omega[i];
      sumOmega2 += Math.pow( omega[i],2 );
    }
    // compute mu
    double mu;
    if (sumX==0.0 && sumOmega==0.0) { mu = prior/(1.0/REF_LENGTH); }
    else { mu = (sumX+prior)/(sumOmega+1.0/REF_LENGTH); }

    double r;
    if (j<=1.0) { r = 0.0; }
    else { r = (sumOmega - sumOmega2/sumOmega) / (j-1.0); }
    double v=0.0;
    for(int i=0; i<x.length; i++){
      v += ( omega[i] * Math.pow( x[i]/omega[i]-mu,2 ) ) / (j-1.0);
    }
    if (j<=1.0) { v = 0.0; }

    // compute delta
    double d;
    d = Math.max( 0.0,(v-mu)/(r*mu) );

    TreeMap mudelta = new TreeMap();
    mudelta.put( "mu",new Double(mu) );
    mudelta.put( "delta",new Double(d) );
    return mudelta;
  }


  //
  // Accessory Methods
  //

  /** Set REF_LENGTH to the desired value */
  public void setREF_LENGTH(double desiredLength) {
    REF_LENGTH = desiredLength;
  }

  /** Set PDF to the desired value */
  public void setPDF(String pdf) {
    PDF = pdf;
  }


  // Test T1
  static public void main(String[] args)
  {
    Dataset dataset = SampleDatasets.sampleData("movies",false);

    T1InstanceTransformLearner learner = new T1InstanceTransformLearner();
    learner.setREF_LENGTH(100.0);
    learner.setPDF("Poisson");
    //learner.setPDF("Negative-Binomial");

    InstanceTransform filter = learner.batchTrain( dataset );
    ((T1InstanceTransform)filter).setALPHA(0.05);
    ((T1InstanceTransform)filter).setMIN_WORDS(50);
    ((T1InstanceTransform)filter).setSAMPLE(10000);

    //System.out.println( "old data:\n" + dataset );
    dataset = filter.transform( dataset );
    System.out.println( "new data:\n" + dataset );
  }
}
