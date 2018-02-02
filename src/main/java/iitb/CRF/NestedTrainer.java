package iitb.CRF;

import cern.colt.matrix.impl.*;


/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class NestedTrainer extends Trainer {
    public NestedTrainer(CrfParams p) {
        super(p);
    }
    DenseDoubleMatrix1D alpha_Y_Array[];
    
    protected double sumProductInner(DataSequence data, FeatureGenerator featureGenerator, 
            double lambda[], double grad[],  
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        FeatureGeneratorNested featureGenNested = (FeatureGeneratorNested)featureGenerator;
        SegmentDataSequence dataSeq = (SegmentDataSequence)data;
        
        int base = -1;
        if ((alpha_Y_Array == null) || (alpha_Y_Array.length < dataSeq.length()-base)) {
            alpha_Y_Array = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < alpha_Y_Array.length; i++)
                alpha_Y_Array[i] = new DenseDoubleMatrix1D(numY);
        }
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < beta_Y.length; i++)
                beta_Y[i] = new DenseDoubleMatrix1D(numY);
        }
        // compute beta values in a backward scan.
        // also scale beta-values as much as possible to avoid numerical overflows
        beta_Y[dataSeq.length()-1].assign(0);
        for (int i = dataSeq.length()-2; i >= 0; i--) {
            beta_Y[i].assign(RobustMath.LOG0);
            for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i+ell < dataSeq.length()); ell++) {		    
                // compute the Mi matrix
                featureGenNested.startScanFeaturesAt(dataSeq, i, i+ell);
                //if (! featureGenNested.hasNext())
                //	break;
                initMDone = computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,false,reuseM,initMDone);
                tmp_Y.assign(beta_Y[i+ell]);
                tmp_Y.assign(Ri_Y,sumFunc);
                RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i],1,1,false,edgeGen);
            }
        }
        double thisSeqLogli = 0;
        alpha_Y_Array[0].assign(0);
        int segmentStart = 0;
        int segmentEnd = -1;
        boolean invalid = false;
        for (int i = 0; i < dataSeq.length(); i++) {
            if (segmentEnd < i) {
                segmentStart = i;
                segmentEnd = dataSeq.getSegmentEnd(i);
            }
            if (segmentEnd-segmentStart+1 > featureGenNested.maxMemory()) {
                if (icall == 0) {
                    System.out.println("Ignoring record with segment length greater than maxMemory " + dataSeq);
                }
                invalid = true;
                break;
            }
            alpha_Y_Array[i-base].assign(RobustMath.LOG0);
            for (int ell = 1; (ell <= featureGenNested.maxMemory()) && (i-ell >= base); ell++) {
                // compute the Mi matrix
                featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
                // if (!featureGenNested.hasNext())
                //	break;
                initMDone = computeLogMi(featureGenNested,lambda,Mi_YY,Ri_Y,false,reuseM,initMDone);
                
                if (fgenForExpVals != null) {
                    // find features that fire at this position..
                    ((FeatureGeneratorNested)fgenForExpVals).startScanFeaturesAt(dataSeq, i-ell,i);
                    boolean isSegment = ((i-ell+1==segmentStart) && (i == segmentEnd));
                    while (fgenForExpVals.hasNext()) { 
                        Feature feature = fgenForExpVals.next();
                        int f = feature.index();
                        int yp = feature.y();
                        int yprev = feature.yprev();
                        float val = feature.value();
                        boolean allEllMatch = isSegment && (dataSeq.y(i) == yp);
                        if (allEllMatch && (((i-ell >= 0) && (yprev == dataSeq.y(i-ell))) || (yprev < 0))) {
                            grad[f] += val;
                            thisSeqLogli += val*lambda[f];
                        }
                        if ((yprev < 0) && (i-ell >= 0)) {
                            for (yprev = 0; yprev < Mi_YY.rows(); yprev++) 
                                ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[i-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp) + RobustMath.log(val)+beta_Y[i].get(yp)));
                        } else if (i-ell < 0) {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], (Ri_Y.get(yp)+RobustMath.log(val)+beta_Y[i].get(yp)));
                        } else {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[i-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[i].get(yp)));
                        }
                    }
                }
                if (i-ell >= 0) {
                    RobustMath.logMult(Mi_YY, alpha_Y_Array[i-ell-base],tmp_Y,1,0,true,edgeGen);
                    tmp_Y.assign(Ri_Y,sumFunc);
                    RobustMath.logSumExp(alpha_Y_Array[i-base],tmp_Y);
                } else {
                    RobustMath.logSumExp(alpha_Y_Array[i-base],Ri_Y);
                }
            }
            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y_Array[i-base].toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
            if (params.debugLvl > 1) {
                System.out.println(" pos "  + i + " " + thisSeqLogli);
            }
        }
        if (invalid)
            return 0;
        lZx = RobustMath.logSumExp(alpha_Y_Array[dataSeq.length()-1-base]);
        return thisSeqLogli;
    }
};
