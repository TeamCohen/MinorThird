package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.random.NegativeBinomial;
import edu.cmu.minorthird.classify.algorithms.random.Poisson;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.util.*;

/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */

public class T1InstanceTransform implements InstanceTransform {

    static private Logger log = Logger.getLogger(T1InstanceTransform.class);

    private double ALPHA; // tolerance level for the FDR in selecting features
    private int MIN_WORDS; // minimum number of features to keep, EVEN IF NOT significant
    private int SAMPLE; // points sampled to estimate T1's PDF, and compute p-values
    private double REF_LENGTH; // word-length of the reference document

    private String PDF; // model for T1: can be "Poisson" or "Negative-Binomial"
    private Map T1values;

    private Map muPosExamples;
    private Map deltaPosExamples;
    private Map muNegExamples;
    private Map deltaNegExamples;

	public T1InstanceTransform() {
        this.ALPHA = 0.1;
        this.MIN_WORDS = 50;  // 0,...,49
        this.SAMPLE = 2500;
        this.REF_LENGTH = T1InstanceTransformLearner.getREF_LENGTH(); // for REF_LENGTH-word-long documents
        this.PDF = "Poisson";
        this.T1values = new TreeMap();
        this.muPosExamples = new TreeMap();
        this.muNegExamples = new TreeMap();
        this.deltaPosExamples = new TreeMap();
        this.deltaNegExamples = new TreeMap();
	}


    /** Create a transformed copy of the instance. */
    public Instance transform(Instance instance) {
        System.out.println("Warning: cannot transform instance with T1 Statistic!");
        return instance;
    }

    /** Create a transformed copy of the example. */
    public Example transform(Example example){
        System.out.println("Warning: cannot transform example with T1 Statistic!");
        return example;
    }

    /** Create a transformed copy of a dataset. */
    public Dataset transform(Dataset dataset){
        BasicFeatureIndex index = new BasicFeatureIndex(dataset);
        int N = 0;
        for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();
            N += 1;
        }

        final Comparator VAL_COMPARATOR = new Comparator() {
            public int compare(Object o1, Object o2) {
                Pair p1 = (Pair)o1;
                Pair p2 = (Pair)o2;
                if (p1.value<p2.value) return -1;
                else if (p1.value>p2.value) return 1;
                else return (p1.key).compareTo( p2.key );
            }
        };

        // loop features
        List pValue = new ArrayList();
        int featureCounter = 0;
        for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();

            // Sample T1 values
            double[] T1array = new double[ SAMPLE ];
            if ( this.PDF.equals("Poisson") ) {
                // Sample T1 values from the ML Poisson
                Poisson Xp = new Poisson( getPosMu(f) );
                Poisson Xn = new Poisson( getNegMu(f) );
                for (int cnt=0; cnt<SAMPLE; cnt++) {
                    T1array[cnt] = T1( Xp.nextInt(),Xn.nextInt() );
                }
            } else if ( this.PDF.equals("Negative-Binomial") ) {
                // Sample T1 values from the MOME Negative-Binomial
                TreeMap npPos = mudelta2np( getPosMu(f),getPosDelta(f),1 );
                TreeMap npNeg = mudelta2np( getNegMu(f),getNegDelta(f),1 );
                NegativeBinomial Xp = new NegativeBinomial( ((Integer)(npPos.get("n"))).intValue(),((Double)(npPos.get("p"))).doubleValue() );
                NegativeBinomial Xn = new NegativeBinomial( ((Integer)(npNeg.get("n"))).intValue(),((Double)(npNeg.get("p"))).doubleValue() );
                for (int cnt=0; cnt<SAMPLE; cnt++) {
                    T1array[cnt] = T1( Xp.nextInt(),Xn.nextInt() );
                }
            } else {
                throw new IllegalStateException("Error: PDF not implemented!");
            }

            // compute p-value
            //printArray(T1array);
            Arrays.sort( T1array );
            //printArray(T1array);

            int newLength = 0;
            for (int j=0; j<T1array.length; j++) {
                //System.out.println( new Double(T1array[j]) );
                if ( new Double(T1array[j]).isNaN() ) {//|| new Double(T1array[j]).isInfinite() ) {
                    newLength = j;
                    //System.out.println( "T1[" + (j-1) + "] = " +T1array[j-1] );  // T1[0],...,T1[j-1]
                    //System.out.println( "T1[" + j + "] = " +T1array[j] );        // j elements
                    break;
                } else {
                    newLength = T1array.length;
                }
            }

            int greatestIndexBeforeT1Observed = 0;
            for (int j=0; j<T1array.length; j++) {
                if (T1array[j]<((Double)T1values.get(f)).doubleValue()) greatestIndexBeforeT1Observed = j;
            }

            Pair p = new Pair( ((double)(newLength-greatestIndexBeforeT1Observed))/((double)newLength),f.toString() );
            pValue.add( p );
            //System.out.println("T1obs="+T1values.get(f) + ", len="+newLength + ", idx="+greatestIndexBeforeT1Observed);
            //System.out.println( "pair=" + p );

            // Find a quantile
            /*double Q=0.9;
            int quantileIndex = (int)Math.floor( Q*(double)(newLength) );
            System.out.println( f.toString() );
            System.out.println( "T1 observed:" + ((Double)T1values.get(f)).doubleValue() + ", p-value:" + ((Pair)pValue.get(featureCounter)).value );
            System.out.println( "at T1=" + T1array[quantileIndex] + " prob=" + Q );*/
            featureCounter += 1;
        }

        // FDR correction to decide which features to retain
        Collections.sort( pValue, VAL_COMPARATOR);
        printCollection(pValue);

        int greatestIndexBeforeAccept = -1; // does not return any word at -1
        for (int j=1; j<=pValue.size(); j++) {
            double line = ((double)j) * ALPHA / ((double)pValue.size());
            if ( line>((Pair)pValue.get(j-1)).value ) greatestIndexBeforeAccept = j-1;
        }
        greatestIndexBeforeAccept = Math.min( pValue.size()-1,Math.max( greatestIndexBeforeAccept,MIN_WORDS ) );
        //System.out.println("max-index:" + greatestIndexBeforeAccept);

//        ArrayList usefulFeatures = new ArrayList();
      //log.setLevel(Level.DEBUG);

        TreeMap availableFeatures = new TreeMap();
      //log.debug("num avail features = " + greatestIndexBeforeAccept);
        for (int j=0; j<=greatestIndexBeforeAccept; j++) {
//            usefulFeatures.add( new Feature( ((Pair)pValue.get(j)).key) );
            availableFeatures.put( new Feature( ((Pair)pValue.get(j)).key),new Integer(1) );
          //log.debug("added available feature: " + pValue.get(j));
        }

        // create transformed dataset
        BasicDataset maskeDataset = new BasicDataset();
        for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
            Example e = i.nextExample();
            Instance mi = new MaskedInstance( e.asInstance(),availableFeatures );
            //System.out.println( mi );

//            maskeDataset.add( new Example( new CompactInstance(mi),e.getLabel()) );
          Example ex = new Example(mi, e.getLabel());
          maskeDataset.add( ex ); //new Example ( mi, e.getLabel()));
          /*log.debug("new example: " );//+ ex);
          log.debug("..." + ex.binaryFeatureIterator().estimatedSize());
          for (Feature.Looper l = ex.binaryFeatureIterator(); l.hasNext();)
          {
            log.debug("binary looper - " + l.nextFeature());
          }
          log.debug("..." + ex.numericFeatureIterator().estimatedSize());
          for (Feature.Looper l = ex.numericFeatureIterator(); l.hasNext();)
          {
            log.debug("looper - " + l.nextFeature());
          }*/

            /*MutableInstance mi = new MutableInstance();
            for (Feature.Looper j=e.featureIterator(); j.hasNext(); ) {
                Feature f = j.nextFeature();
                if ( usefulFeatures.contains( f ) ) mi.addNumeric( f,e.getWeight(f) );
            }
            maskeDataset.add( new Example(new CompactInstance(mi),e.getLabel()) );*/
        }
        //log.info("new daataset: " + maskeDataset);
        return maskeDataset;
    }

    private void printCollection(List pValue) {
        String buf = "";
        for (int j=0; j<pValue.size(); j++) {
            buf = buf + " " + pValue.get(j).toString();
        }
        buf = buf + "\n";
        System.out.println( buf );
    }

    private void printArray(double[] t1array) {
        String buf = "";
        for (int j=0; j<t1array.length; j++) {
            buf = buf + " " + t1array[j];
        }
        buf = buf + "\n";
        System.out.println( buf );
    }


    /** A class that we use to sort a TreeMap by values */
    private class Pair extends Object {
        double value;
        String key;

        public Pair(double v, String k) {
            this.value = v;
            this.key = k;
        }

        public String toString() {
             return "[ " + this.value + "," + this.key + " ]";
        }
    }

    /** Set ALPHA to the desired level */
    public void setALPHA(double desiredLevel) {
        this.ALPHA = desiredLevel;
    }

    /** Set PDF to the desired distribution:
     * can be "Poisson" or "Negative-Binomial".  */
    public void setPDF(String desiredPDF) {
        this.PDF = desiredPDF;
    }

    /** Get the current value of PDF */
    public String getPDF() {
        return this.PDF;
    }

    /** Set the value of T1 corresponding to feature f */
    public void setT1(Feature f,double delta) {
        Double d = (Double)T1values.get(f);
        if (d==null) T1values.put(f, new Double(delta));
        else System.out.println("Warning: T1 value already set for feature " + f.toString() + "!");
    }

    /** Set mu corresponding to the Positive examples of feature f */
    public void setPosMu(Feature f,double delta) {
        Double d = (Double)muPosExamples.get(f);
        if (d==null) muPosExamples.put(f, new Double(delta));
        else muPosExamples.put(f, new Double(d.doubleValue()+delta));
    }

    /** Get mu corresponding to the Positive examples of feature f */
    public double getPosMu( Feature f) {
        Double d = (Double)muPosExamples.get( f );
        if (d==null) return 0.0;
        else return d.doubleValue();
    }

    /** Set mu corresponding to the Positive examples of feature f */
    public void setNegMu(Feature f,double delta) {
        Double d = (Double)muNegExamples.get(f);
        if (d==null) muNegExamples.put(f, new Double(delta));
        else muNegExamples.put(f, new Double(d.doubleValue()+delta));
    }

    /** Get mu corresponding to the Positive examples of feature f */
    public double getNegMu( Feature f) {
        Double d = (Double)muNegExamples.get( f );
        if (d==null) return 0.0;
        else return d.doubleValue();
    }

    /** Set mu corresponding to the Positive examples of feature f */
    public void setPosDelta(Feature f,double delta) {
        Double d = (Double)deltaPosExamples.get(f);
        if (d==null) deltaPosExamples.put(f, new Double(delta));
        else deltaPosExamples.put(f, new Double(d.doubleValue()+delta));
    }

    /** Get mu corresponding to the Positive examples of feature f */
    public double getPosDelta( Feature f) {
        Double d = (Double)deltaPosExamples.get( f );
        if (d==null) return 0.0;
        else return d.doubleValue();
    }

    /** Set mu corresponding to the Positive examples of feature f */
    public void setNegDelta(Feature f,double delta) {
        Double d = (Double)deltaNegExamples.get(f);
        if (d==null) deltaNegExamples.put(f, new Double(delta));
        else deltaNegExamples.put(f, new Double(d.doubleValue()+delta));
    }

    /** Get mu corresponding to the Positive examples of feature f */
    public double getNegDelta( Feature f) {
        Double d = (Double)deltaNegExamples.get( f );
        if (d==null) return 0.0;
        else return d.doubleValue();
    }

    public double T1(int x1, int x2) {
        double dx1 = new Integer(x1).doubleValue();
        double dx2 = new Integer(x2).doubleValue();
        double t = Math.pow( (dx1-dx2),2 ) / (dx1+dx2);
        return t;
    }

    public TreeMap mudelta2np(double mu, double delta, double omega) {
        TreeMap np = new TreeMap();
        // from mu,delta to n
        int n = (int)Math.round(mu/delta);
        np.put( "n", new Integer(n) );
        // from mu,delta to p
        double p = omega*delta;
        np.put( "p", new Double(p) );
        return np;
    }

}
