package iitb.CRF;


/**
 *
 * NestedViterbi search
 *
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */ 

public class NestedViterbi extends Viterbi {
    /**
	 * 
	 */
	private static final long serialVersionUID = -1556279124003455655L;
	NestedCRF nestedModel;
    
    NestedViterbi(NestedCRF nestedModel, int bs) {
        super(nestedModel, bs);
        this.nestedModel = nestedModel;
    }
    double fillArray(DataSequence dataSeq, double lambda[], boolean calcScore) {
        int numY = model.numY;
        int maxLen = nestedModel.featureGenNested.maxMemory();
        for (int i = 0; i < dataSeq.length(); i++) {
            for (int yi = 0; yi < numY; winningLabel[yi++][i].clear());
            for (int ell = 1; (ell <= maxLen) && (i-ell >= -1); ell++) {
                nestedModel.featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
                Trainer.computeLogMi(model.featureGenerator,lambda,Mi,Ri,false);
                for (int yi = 0; yi < numY; yi++) {
                    if (i-ell < 0) {
                        winningLabel[yi][i].add((float)Ri.get(yi));
                    } else {
                        for (int yp = 0; yp < numY; yp++) {
                            double val = Mi.get(yp,yi)+Ri.get(yi);
                            winningLabel[yi][i].add(winningLabel[yp][i-ell], (float)val);
                        }
                    }
                }
            }
        }
        return 0;
    }
    public double bestLabelSequence(SegmentDataSequence dataSeq, double lambda[]) {
        viterbiSearch(dataSeq, lambda,false);
        Soln ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        while (ybest != null) {
            dataSeq.setSegment(ybest.prevPos()+1,ybest.pos,ybest.label);
            ybest = ybest.prevSoln;
        }
        return finalSoln.get(0).score;
    }
};
