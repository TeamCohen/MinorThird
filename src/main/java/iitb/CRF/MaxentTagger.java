/*
 * Created on Jul 9, 2005
 *
 */
package iitb.CRF;

import java.util.Properties;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

/**
 * This one implements the classical Maximum entropy tagger (as in Ratnaparkhi's thesis)
 * @author sunita
 * @since 1.2
 * @version 1.3
 */
public class MaxentTagger extends CRF {
    /**
	 * 
	 */
	private static final long serialVersionUID = 5822082582489471322L;
	/**
     * @param numLabels
     * @param fgen
     * @param arg
     */
    public MaxentTagger(int numLabels, FeatureGenerator fgen, String arg) {
        super(numLabels, fgen, arg);
    }
    /**
     * @param numLabels
     * @param fgen
     * @param configOptions
     */
    public MaxentTagger(int numLabels, FeatureGenerator fgen,
            Properties configOptions) {
        super(numLabels, fgen, configOptions);
    }
    /**
     * @param numLabels
     * @param histsize
     * @param fgen
     * @param configOptions
     */
    public MaxentTagger(int numLabels, int histsize, FeatureGenerator fgen,
            Properties configOptions) {
        super(numLabels, histsize, fgen, configOptions);
    }
    protected Trainer getTrainer() {
        return new MaxentTrainer(params);
    }
    public double getLogZx(DataSequence dataSequence) {
        // since scores are prenormalized.
        return 0;
    }
    class MaxentViterbi extends Viterbi {
        DoubleMatrix1D tmp_Y; 
        class SumSingle implements DoubleFunction {
            public double constVal = 1.0;
            public double apply(double a) {return a+constVal;}
        };
        SumSingle normalizer = new SumSingle();
        /**
         * @param model
         * @param bs
         */
        MaxentViterbi(CRF model, int bs) {
            super(model, bs);
            tmp_Y = new DenseDoubleMatrix1D(model.numY);
        }
        private static final long serialVersionUID = 1L;
        protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
            super.computeLogMi(dataSeq,i,ell,lambda);
            if (i == 0) {
                normalizer.constVal = -1*RobustMath.logSumExp(Ri);
                Ri.assign(normalizer);
                return;
            }
            for (int yprev = 0; yprev < Mi.rows(); yprev++) {
                tmp_Y.assign(Ri);
                tmp_Y.assign(Mi.viewRow(yprev),Trainer.sumFunc);
                normalizer.constVal = -1*RobustMath.logSumExp(tmp_Y);
                Mi.viewRow(yprev).assign(normalizer);
            }
        }
        public LabelSequence[] topKLabelSequences(DataSequence dataSeq, double[] lambda, int numLabelSeqs, boolean getScores) {
            viterbiSearch(dataSeq, lambda,false);
            int numSols = Math.min(finalSoln.numSolns(), numLabelSeqs);
            LabelSequence labelSequences[] = new LabelSequence[numSols];
            for (int k = numSols-1; k >= 0; k--) {
                Soln ybest = finalSoln.get(k);
                labelSequences[k] = newLabelSequence(dataSeq.length());
                labelSequences[k].score = ybest.score;
                ybest = ybest.prevSoln;
                while (ybest != null) {	
                    labelSequences[k].add(ybest.prevPos(), ybest.pos, ybest.label);
                    dataSeq.set_y(ybest.pos,ybest.label);
                    ybest = ybest.prevSoln;
                }
                labelSequences[k].doneAdd();
                if (getScores) labelSequences[k].score = Math.exp(labelSequences[k].score-model.getLogZx(dataSeq));
            }
            return labelSequences;
        }
    }
    public Viterbi getViterbi(int beamsize) {
        return new MaxentViterbi(this,beamsize);
    }
}
