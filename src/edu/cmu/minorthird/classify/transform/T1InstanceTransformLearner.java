package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.util.TreeMap;

/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */
public class T1InstanceTransformLearner implements InstanceTransformLearner {

    static private Logger log = Logger.getLogger(T1InstanceTransformLearner.class);
    public static double REF_LENGTH; // word-length of the reference document
                                     // See static methods at the bottom of this class

    public T1InstanceTransformLearner() {
        this.REF_LENGTH = 100.0;
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
        //T1Filter.setPDF("Negative-Binomial"); // To-Do: Set according to mean/var of counts
        BasicFeatureIndex index = new BasicFeatureIndex(dataset);
        int N = dataset.size();
        // loop features
        for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();
            //System.out.println( f );
            // fill array of <counts_ex(feature), length_ex> for POS class
            double[] xPos = new double[ index.size(f,"POS") ];
            double[] omegaPos = new double[ index.size(f,"POS") ];
            int position=0;
            for (int j=0; j<index.size(f); j++) {
                Example e = index.getExample(f,j);
                //System.out.println( e );
                if ( "POS".equals( e.getLabel().bestClassName() ) ) {
                    xPos[position] = e.getWeight(f);
                    omegaPos[position] = getLength(e) / REF_LENGTH;
                    //System.out.println( f.toString() + ":" + xPos[position] + "," + omegaPos[position]);
                    position += 1;
                }
            }
            // fill array of <counts(example,feature), length(example)> for NEG class
            double[] xNeg = new double[ index.size(f,"NEG") ];
            double[] omegaNeg = new double[ index.size(f,"NEG") ];
            position=0;
            for (int j=0; j<index.size(f); j++) {
                Example e = index.getExample(f,j);
                //System.out.println( e );
                if ( "NEG".equals( e.getLabel().bestClassName() ) ) {
                    xNeg[position] = e.getWeight(f);
                    omegaNeg[position] = getLength(e) / REF_LENGTH;
                    //System.out.println( f.toString() + ":" + xNeg[position] + "," + omegaNeg[position]);
                    position += 1;
                }
            }
            // estimate Parameters for the two classes and update the T1-Filter
            T1Filter.setT1( f,T1((int)index.getCounts(f,"POS"),(int)index.getCounts(f,"NEG")) );
            if ( T1Filter.getPDF().equals("Poisson") ) {
                // learn Poisson parameters
                double muPos = MaximumLikelihoodPoisson(xPos,omegaPos);
                if ( new Double(muPos).isNaN() ) muPos = 0.0; // room for prior on unseen words
                double muNeg = MaximumLikelihoodPoisson(xNeg,omegaNeg);
                if ( new Double(muNeg).isNaN() ) muNeg = 0.0; // room for prior on unseen words
                //System.out.println("mu - " + f.toString() + ":" + muPos + "," + muNeg);
                // update T1 Filter
                T1Filter.setPosMu( f,muPos );
                T1Filter.setNegMu( f,muNeg );
            } else if ( T1Filter.getPDF().equals("Negative-Binomial") ) {
                // learn Negative-Binomial parameters
                TreeMap mudeltaPos = MethodOfMomentsNegBin(xPos,omegaPos); // check for NaN
                TreeMap mudeltaNeg = MethodOfMomentsNegBin(xNeg,omegaNeg); // check for NaN
                // update T1 Filter
                T1Filter.setPosMu( f,((Double)mudeltaPos.get("mu")).doubleValue() );
                T1Filter.setPosDelta( f,((Double)mudeltaPos.get("delta")).doubleValue() );
                T1Filter.setNegMu( f,((Double)mudeltaNeg.get("mu")).doubleValue() );
                T1Filter.setNegDelta( f,((Double)mudeltaNeg.get("delta")).doubleValue() );
                //System.out.println("   mu - " + f.toString() + ":" + mudeltaPos.get("mu") + "," + mudeltaNeg.get("mu"));
                //System.out.println("delta - " + f.toString() + ":" + mudeltaPos.get("delta") + "," + mudeltaNeg.get("delta"));
            }
        }
        return T1Filter;
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

    /** Compute the method of moment estimates of the rate 'mu' and the parameter
     *  which controls the variability 'delta' of a Negative-Binomial models, using
     *  integer counts x[] from examples with different lengths omega[].
     */
    public TreeMap MethodOfMomentsNegBin(double[] x, double[] omega) {
        double j = x.length;
        double sumX=0;
        double sumOmega=0;
        double sumOmega2=0;
        for(int i=0; i<x.length; i++){
            sumX += x[i];
            sumOmega += omega[i];
            sumOmega2 += Math.pow( omega[i],2 );
        }
        double mu = sumX/sumOmega;
        double r = (sumOmega - sumOmega2/sumOmega) / (j-1);
        double v=0;
        for(int i=0; i<x.length; i++){
            v += omega[i] * Math.pow( x[i]/omega[i]-mu,2 );
        }
        double d = Math.max( 0.0,(v-mu)/(r*mu) );
        TreeMap mudelta = new TreeMap();
        mudelta.put( "mu",new Double(mu) );
        mudelta.put( "delta",new Double(d) );
        return mudelta;
    }

    //
    // Static Methods
    //

    /** Set REF_LENGTH to the desired value */
    public static void setREF_LENGTH(double desiredLength) {
        REF_LENGTH = desiredLength;
    }

    /** Get the current value of REF_LENGTH */
    public static double getREF_LENGTH() {
        return REF_LENGTH;
    }

    // Test T1
    static public void main(String[] args)
    {
        Dataset dataset = SampleDatasets.sampleData("movies",false);
        T1InstanceTransformLearner learner = new T1InstanceTransformLearner();
        InstanceTransform t1Statistics = new T1InstanceTransform();
        t1Statistics = learner.batchTrain( dataset );
        //System.out.println( "old data:\n" + dataset );
        dataset = t1Statistics.transform( dataset );
        System.out.println( "new data:\n" + dataset );
    }
}
