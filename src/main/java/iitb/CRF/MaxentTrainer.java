/*
 * Created on Jul 9, 2005
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public class MaxentTrainer extends Trainer {
    
    /**
     * @param p
     */
    public MaxentTrainer(CrfParams p) {
        super(p);
    }
    
    protected double sumProduct(DataSequence dataSeq, FeatureGenerator featureGenerator, double[] lambda,
            double[] grad, double[] expFVals, boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        double thisSeqLogli=0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            if (i > 0) {
                Ri_Y.assign(Mi_YY.viewRow(dataSeq.y(i-1)),sumFunc);
            }
            if ((grad !=null)||(expFVals!=null)) {
                for (int f = 0; f < lambda.length; f++)
                    ExpF[f] = RobustMath.LOG0;
                // find features that fire at this position..
                featureGenerator.startScanFeaturesAt(dataSeq, i);
                while (featureGenerator.hasNext()) { 
                    Feature feature = featureGenerator.next();
                    int f = feature.index();
                    
                    int yp = feature.y();
                    int yprev = feature.yprev();
                    float val = feature.value();
                    if ((i > 0) && (yprev >= 0) && (yprev != dataSeq.y(i-1)))
                        continue;
                    if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                        grad[f] += val;
                        thisSeqLogli += val*lambda[f];
                        if (params.debugLvl > 2) {
                            System.out.println("Feature fired " + f + " " + feature);
                        } 
                    }
                    ExpF[f] = RobustMath.logSumExp(ExpF[f], Ri_Y.get(yp) +RobustMath.log(val));
                }
            }
            if (params.debugLvl > 2) {
                System.out.println("Ri " + Ri_Y.toString());
            }
            double lZx = RobustMath.logSumExp(Ri_Y);
            thisSeqLogli -= lZx;
            // update grad.
            if (grad != null) {
                for (int f = 0; f < grad.length; f++) {
                    grad[f] -= RobustMath.exp(ExpF[f]-lZx);
                }
            }
            if (expFVals!=null) {
                for (int f = 0; f < lambda.length; f++) {
                    expFVals[f] += RobustMath.exp(ExpF[f]-lZx);
                }
            }
            if (params.debugLvl > 1) {
                System.out.println("Sequence "  + thisSeqLogli  + " log(Zx) " + lZx + " Zx " + Math.exp(lZx));
            }
        }
        return thisSeqLogli;
    }
    
}
