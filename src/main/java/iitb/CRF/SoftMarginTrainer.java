/*
 * Created on Nov 14, 2007
 * @author sunita
 * Objective is log (sum_y exp(W.(F(xi,y)-F(xi,yi))+hammingLoss(y)))
 */
package iitb.CRF;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class SoftMarginTrainer extends Trainer {
    public SoftMarginTrainer(CrfParams p) {
        super(p);
        logProcessing=true;
    }
    @Override
    protected boolean computeLogMiTrainMode(FeatureGenerator featureGenerator, double[] lambda, DataSequence dataSeq, int i, DoubleMatrix2D mi_YY, DoubleMatrix1D ri_Y, boolean b, boolean reuseM, boolean initMDone) {
        boolean initDoneNow = super.computeLogMiTrainMode(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, b, reuseM, initMDone);
        for (int y = 0; y < numY; y++) {
            int loss=(y==dataSeq.y(i))?0:1;
            Ri_Y.set(y, Ri_Y.get(y)+loss);
        }
        return initDoneNow;
    }
}
