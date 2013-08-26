/*
 * Created on Oct 15, 2005
 *  This class implements the piecewise approximate trainer as described
 *  in this paper: Piecewise Training for Undirected Models. Charles Sutton and Andrew McCallum. UAI, 2005
 */
package iitb.CRF;

public class PiecewiseTrainer extends Trainer {

    public PiecewiseTrainer(CrfParams p) {
        super(p);
    }
    protected double sumProduct(DataSequence dataSeq, FeatureGenerator featureGenerator, 
            double lambda[], double grad[], double expFVals[], boolean onlyForwardPass, int numRecord, 
            FeatureGenerator fgenForExpVals) {
        double thisSeqLogli = 0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            // compute log partition function.
            alpha_Y.assign(Ri_Y);
            if (i > 0) {
                for (int colNum = 0; colNum < Mi_YY.columns(); colNum++) {
                    alpha_Y.set(colNum, Ri_Y.get(colNum)+RobustMath.logSumExp(Mi_YY.viewColumn(colNum)));
                }
            }
            lZx = RobustMath.logSumExp(alpha_Y);
            if (fgenForExpVals != null) {
                for (int f = 0; f < ExpF.length; f++)
                    ExpF[f] = RobustMath.LOG0;
            // find features that fire at this position..
                fgenForExpVals.startScanFeaturesAt(dataSeq, i);
                while (fgenForExpVals.hasNext()) { 
                    Feature feature = fgenForExpVals.next();
                    int f = feature.index();
                    
                    int yp = feature.y();
                    int yprev = feature.yprev();
                    float val = feature.value();
                    
                    if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                        grad[f] += val;
                        thisSeqLogli += val*lambda[f];
                        if (params.debugLvl > 2) {
                            System.out.println("Feature fired " + f + " " + feature);
                        } 
                    }
                    if (yprev < 0) {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yp)+RobustMath.log(val));
                    } else {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val));
                    }
                }
                for (int f = 0; f < grad.length; f++) {
                    grad[f] -= RobustMath.exp(ExpF[f]-lZx);
                }
            }
            thisSeqLogli -= lZx;
            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y.toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
        }
        return thisSeqLogli;
    }
}
