/** BSegmentCRF.java
 * Created on Apr 14, 2005
 *
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 * 
 * BSegmentCRF (A significantly faster version of Semi-CRFs that employs a compact feature representation) 
 * for fast training and inference of semi-Markov models.
 */
package iitb.BSegmentCRF;

import gnu.trove.map.hash.TIntDoubleHashMap;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentCRF;
import iitb.CRF.Trainer;
import iitb.CRF.Viterbi;

import java.util.Properties;

public class BSegmentCRF extends SegmentCRF {
    /**
	 * 
	 */
	private static final long serialVersionUID = 655531342585250258L;
	BFeatureGenerator bfgen;
    /**
     * @param numLabels
     * @param fgen
     * @param arg
     */
    public BSegmentCRF(int numLabels, BFeatureGenerator fgen, String arg) {
        super(numLabels, fgen, arg);
        bfgen = fgen;
    }
    /**
     * @param numLabels
     * @param fgen
     * @param configOptions
     */
    public BSegmentCRF(int numLabels, BFeatureGenerator fgen,
            Properties configOptions) {
        super(numLabels, fgen, configOptions);
        bfgen = fgen;
    }
    protected Trainer getTrainer() {
        Trainer thisTrainer = dynamicallyLoadedTrainer();
        if (thisTrainer != null)
            return thisTrainer;
        return new BSegmentTrainer(params);
    }
    
    public Viterbi getViterbi(int beamsize) {
        return params.miscOptions.getProperty("segmentViterbi", "false").equals("true")?
                super.getViterbi(beamsize):new BSegmentViterbi(this,numY,beamsize);
    }
    public double segmentMarginalProbabilities(DataSequence dataSequence, TIntDoubleHashMap segmentMarginals[][], TIntDoubleHashMap edgeMarginals[][][]) {
        System.err.println("Not yet implemented for this CRF--use SegmentCRF");
        System.exit(-1);
        return 0;
    }
        
}
