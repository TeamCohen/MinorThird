/** SegmentCRF.java
 * Created on Nov 21, 2004
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 *
 * This is a version of the CRF model that applies the semi-markov
 * model on data where the candidate segments are provided by the dataset.
 */
package iitb.CRF;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;

public class SegmentCRF extends CRF {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4846441387460151325L;
	protected FeatureGeneratorNested featureGenNested;
	transient SegmentViterbi segmentViterbi;
	transient SegmentAStar segmentAStar;
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, String arg) {
		super(numLabels,fgen,arg);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
		segmentAStar = new SegmentAStar(this, 1);
	}
	public SegmentCRF(int numLabels, FeatureGeneratorNested fgen, java.util.Properties configOptions) {
		super(numLabels,fgen,configOptions);
		featureGenNested = fgen;
		segmentViterbi = new SegmentViterbi(this,1);
		segmentAStar = new SegmentAStar(this, 1);
	}
	public interface ModelGraph {
	    public int numStates();
	    public void stateMappingGivenLength(int label, int len, TIntArrayList stateIds) 
	    throws Exception;
	};

	protected Trainer getTrainer() {
        Trainer thisTrainer = dynamicallyLoadedTrainer();
        if (thisTrainer != null)
            return thisTrainer;
		if (params.trainerType.startsWith("SegmentCollins"))
			return new NestedCollinsTrainer(params);
		return new SegmentTrainer(params);
	}
	public Viterbi getViterbi(int beamsize) {
		return new SegmentViterbi(this,beamsize);
	}
    public void apply(CandSegDataSequence dataSeq, int rank) {
      System.out.println("Not implemented yet");
    }
	public double apply(DataSequence dataSeq) {
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, 1);
	        return segmentAStar.bestLabelSequence((CandSegDataSequence)dataSeq, lambda);
	    }else{//default
            return super.apply(dataSeq);
	    }
	}

	public void singleSegmentClassScores(CandSegDataSequence dataSeq, TIntFloatHashMap scores) {
	    if (segmentViterbi==null)
	        segmentViterbi = (SegmentViterbi)getViterbi(1);
		segmentViterbi.singleSegmentClassScores(dataSeq,lambda,scores); 
	}
	 public Segmentation[] segmentSequences(CandSegDataSequence dataSeq, int numLabelSeqs) {
	     return segmentSequences(dataSeq,numLabelSeqs,null);
	 }
	 public Segmentation[] segmentSequences(CandSegDataSequence dataSeq, int numLabelSeqs, double scores[]) {
	     if ((segmentViterbi==null) || (segmentViterbi.beamsize < numLabelSeqs))
		        segmentViterbi = (SegmentViterbi)getViterbi(numLabelSeqs);
	     return segmentViterbi.segmentSequences(dataSeq,lambda,numLabelSeqs,scores);
	 }
     public double segmentMarginalProbabilities(DataSequence dataSequence, TIntDoubleHashMap segmentMarginals[][], TIntDoubleHashMap edgeMarginals[][][]) {
            if (trainer==null) {
                trainer = getTrainer();
                trainer.init(this,null,lambda);
            }
            return -1*((SegmentTrainer)trainer).sumProductInner(dataSequence,featureGenerator,lambda,null,false, -1, null,segmentMarginals,edgeMarginals);
    }
    public double[] marginalProbsOfSegmentation(DataSequence dataSequence, Segmentation segmentation) {
        TIntDoubleHashMap segMargs[][] = new TIntDoubleHashMap[numY][dataSequence.length()];
        for (int i = segmentation.numSegments()-1; i >= 0; i--) {
            segMargs[segmentation.segmentLabel(i)][segmentation.segmentStart(i)] = new TIntDoubleHashMap();
            segMargs[segmentation.segmentLabel(i)][segmentation.segmentStart(i)].put(segmentation.segmentEnd(i), 0);
        }
        segmentMarginalProbabilities(dataSequence, segMargs, null);
        double margPr[]=new double[segmentation.numSegments()];
        for (int i = segmentation.numSegments()-1; i >= 0; i--) {
            margPr[i] = segMargs[segmentation.segmentLabel(i)][segmentation.segmentStart(i)].get(segmentation.segmentEnd(i));
        }
        return margPr;
    }
    
    @Override
    public double getLogZx(DataSequence dataSequence) {
        double logZ = super.getLogZx(dataSequence);
        if (segmentViterbi==null)
            segmentViterbi = (SegmentViterbi)getViterbi(20);
        double violatingScore = segmentViterbi.sumScoreTopKViolators(dataSequence,lambda);
        assert(violatingScore < logZ+1e-4);
       // if (Math.exp(violatingScore-logZ) > 0.5) {
       //     System.out.println("Lot of mass in violating labelings "+Math.exp(violatingScore-logZ));
       // }
        return RobustMath.logMinusExp(logZ, violatingScore);
    }
    
	/*
	public void apply(DataSequence dataSeq) {
		apply((CandSegDataSequence)dataSeq);
	}
	
	public void apply(CandSegDataSequence dataSeq) {
		if (params.debugLvl > 2) 
			Util.printDbg("SegmentCRF: Applying on " + dataSeq);
	    if(params.inferenceType.equalsIgnoreCase("AStar")){
	        if(segmentAStar == null)
	            segmentAStar = new SegmentAStar(this, params.beamSize);
	        segmentAStar.bestLabelSequence(dataSeq, lambda);
	    }else{
		    if (segmentViterbi==null)
		        segmentViterbi = new SegmentViterbi(this,params.beamSize);
		    segmentViterbi.bestLabelSequence(dataSeq,lambda);
	    }
	}
	*/
	/*
	public double score(DataSequence dataSeq) {
	    if (segmentViterbi==null)
	        segmentViterbi = new SegmentViterbi(this,1);
		return segmentViterbi.viterbiSearch(dataSeq,lambda,true);
	}
	*/

}
