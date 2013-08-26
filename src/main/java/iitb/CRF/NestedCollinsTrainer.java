package iitb.CRF;

/**
 * Implements the CollinsVotedPerceptron training algorithm
 *
 * @author Sunita Sarawagi
 *
 */ 

class NestedCollinsTrainer extends CollinsTrainer {
    public NestedCollinsTrainer(CrfParams p) {
	super(p);
    }
    int getSegmentEnd(DataSequence dataSeq, int ss) {
	return ((SegmentDataSequence)dataSeq).getSegmentEnd(ss);
    }
    void startFeatureGenerator(FeatureGenerator _featureGenerator, DataSequence dataSeq, Soln soln) {
	((FeatureGeneratorNested)_featureGenerator).startScanFeaturesAt(dataSeq, soln.prevPos(), soln.pos);
    }
};
