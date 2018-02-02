/** SegmentTrainer.java
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.CRF;

import gnu.trove.TIntDoubleIterator;
import gnu.trove.TIntDoubleHashMap;

import java.util.Iterator;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SegmentTrainer extends SparseTrainer {
    
    protected DoubleMatrix1D alpha_Y_Array[];
    protected DoubleMatrix1D alpha_Y_ArrayM[];
    protected boolean initAlphaMDone[];
    protected DoubleMatrix1D allZeroVector;
    
    public SegmentTrainer(CrfParams p) {
        super(p);
        logTrainer = true;
    }
    protected void init(CRF model, DataIter data, double[] l) {
        super.init(model,data,l);
        logProcessing = true;
        allZeroVector = newLogDoubleMatrix1D(numY);
        allZeroVector.assign(0);
    }
    protected double sumProductInner(DataSequence data, FeatureGenerator featureGenerator, double lambda[], double grad[], 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpCompute) {
        return sumProductInner(data,featureGenerator,lambda,grad,onlyForwardPass,numRecord,fgenForExpCompute,null,null);
    }
    double sumProductInner(DataSequence data, FeatureGenerator featureGenerator, double lambda[], double grad[], 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpCompute,TIntDoubleHashMap segmentMarginals[][],
            TIntDoubleHashMap edgeMarginals[][][]) {
        FeatureGeneratorNested featureGenNested  = (FeatureGeneratorNested)featureGenerator;
        CandSegDataSequence dataSeq = (CandSegDataSequence)data;
        FeatureGeneratorNested featureGenNestedForExpVals = (FeatureGeneratorNested)fgenForExpCompute;
        
        int base = -1;
        if ((alpha_Y_Array == null) || (alpha_Y_Array.length < dataSeq.length()-base)) {
            allocateAlphaBeta(2*dataSeq.length()+1);
        }
        int dataSize = dataSeq.length();
        CandidateSegments candidateSegs = (CandidateSegments)dataSeq;
        DoubleMatrix1D oldBeta =  (dataSize > 0)?beta_Y[dataSeq.length()-1]:null;
        if (!onlyForwardPass) {
            if (dataSize > 0) beta_Y[dataSize-1] = allZeroVector;
            for (int i = dataSeq.length()-2; i >= 0; i--) {
                beta_Y[i].assign(RobustMath.LOG0);
            }
            for (int segEnd = dataSeq.length()-1; segEnd >= 0; segEnd--) {
                int numCands = candidateSegs.numCandSegmentsEndingAt(segEnd)-1;
                for (int nc = 0; nc <= numCands; nc++) {
                    int segStart = candidateSegs.candSegmentStart(segEnd,nc);
                    int ell = segEnd-segStart+1;
                    int i = segStart-1;
                    if (i < 0)
                        continue;
                    // compute the Mi matrix
                    initMDone = computeLogMi(dataSeq,i,i+ell,featureGenNested,lambda,Mi_YY,Ri_Y,reuseM,initMDone);
                    tmp_Y.assign(Ri_Y);
                    if (i+ell < dataSize-1) tmp_Y.assign(beta_Y[i+ell], sumFunc);
                    if (!reuseM) Mi_YY.zMult(tmp_Y, beta_Y[i],1,1,false);
                    else beta_Y[i].assign(tmp_Y, RobustMath.logSumExpFunc);
                }
                if (reuseM && (segEnd-1 >= 0)) {
                    tmp_Y.assign(beta_Y[segEnd-1]);
                    Mi_YY.zMult(tmp_Y, beta_Y[segEnd-1],1,0,false);
                }
            }
        }
        double thisSeqLogli = 0;

        if (reuseM) {
            for (int i = dataSeq.length(); i >= 0; i--)
                initAlphaMDone[i] = false;
        }
        alpha_Y_Array[0] = allZeroVector; //.assign(0);
        
        int trainingSegmentEnd=-1;
        int trainingSegmentStart = 0;
        boolean trainingSegmentFound = true;
        boolean noneFired=true;
        for (int segEnd = 0; segEnd < dataSize; segEnd++) {
            alpha_Y_Array[segEnd-base].assign(RobustMath.LOG0);
            if ((grad != null) && (trainingSegmentEnd < segEnd)) {
                if ((!trainingSegmentFound)&& noneFired) {
                    System.out.println("Error: Training segment ("+trainingSegmentStart + " "+ trainingSegmentEnd + ") not found amongst candidate segments");
                }
                trainingSegmentFound = false;
                trainingSegmentStart = segEnd;
                trainingSegmentEnd =((SegmentDataSequence)dataSeq).getSegmentEnd(segEnd);
            }
            int numCands = candidateSegs.numCandSegmentsEndingAt(segEnd)-1;
            for (int nc = 0; nc <= numCands; nc++) {
            //for (int nc = candidateSegs.numCandSegmentsEndingAt(segEnd)-1; nc >= 0; nc--) {
                int ell = segEnd - candidateSegs.candSegmentStart(segEnd,nc)+1;
                // compute the Mi matrix
                initMDone=computeLogMi(dataSeq,segEnd-ell,segEnd,featureGenNested,lambda,Mi_YY,Ri_Y,reuseM,initMDone);
                boolean mAdded = false, rAdded = false;
                if (segEnd-ell >= 0) {
                    if (!reuseM) Mi_YY.zMult(alpha_Y_Array[segEnd-ell-base],newAlpha_Y,1,0,true);
                    else {
                        if (!initAlphaMDone[segEnd-ell-base]) {
                            alpha_Y_ArrayM[segEnd-ell-base].assign(RobustMath.LOG0);
                            Mi_YY.zMult(alpha_Y_Array[segEnd-ell-base],alpha_Y_ArrayM[segEnd-ell-base],1,0,true);
                            initAlphaMDone[segEnd-ell-base] = true;
                        }
                        newAlpha_Y.assign(alpha_Y_ArrayM[segEnd-ell-base]);
                    }
                    newAlpha_Y.assign(Ri_Y,sumFunc);
                } else 
                    newAlpha_Y.assign(Ri_Y);
                alpha_Y_Array[segEnd-base].assign(newAlpha_Y, RobustMath.logSumExpFunc);
                
                if (featureGenNestedForExpVals != null) {
                    // find features that fire at this position..
                    featureGenNestedForExpVals.startScanFeaturesAt(dataSeq, segEnd-ell,segEnd);
                    while (featureGenNestedForExpVals.hasNext()) { 
                        Feature feature = featureGenNestedForExpVals.next();
                        int f = feature.index();
                        int yp = feature.y();
                        int yprev = feature.yprev();
                        float val = feature.value();
                        if ((grad != null) && dataSeq.holdsInTrainingData(feature,segEnd-ell,segEnd)) {
                            grad[f] += val;
                            thisSeqLogli += val*lambda[f];
                            noneFired=false;
                            if (params.debugLvl > 2) {
                                System.out.println("Feature fired " + f + " " + feature);
                            }
                        }
                        if (yprev < 0) {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], (newAlpha_Y.get(yp)+RobustMath.log(val)+beta_Y[segEnd].get(yp)));
                        } else {
                            ExpF[f] = RobustMath.logSumExp(ExpF[f], (alpha_Y_Array[segEnd-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[segEnd].get(yp)));
                        }
                    }
                }
                
                if (segmentMarginals != null) {
                    for (int yp = (int) (newAlpha_Y.size()-1); yp >= 0; yp--) {
                        if ((segmentMarginals[yp][segEnd-ell+1]!=null) && (segmentMarginals[yp][segEnd-ell+1].containsKey(segEnd))) {
                            // segmentMarginals[yp][segEnd-ell+1] = new TIntDoubleHashMap();
                            segmentMarginals[yp][segEnd-ell+1].put(segEnd,newAlpha_Y.get(yp)+beta_Y[segEnd].get(yp));
                        //segmentMarginals[yp][segEnd-ell+1].put(segEnd,Ri_Y.get(yp));
                        }
                        if (edgeMarginals != null) {
                            for (int yprev = (int) (newAlpha_Y.size()-1); yprev >= 0; yprev--) {
                                if (edgeMarginals[yprev][yp][segEnd-ell+1]==null)
                                    edgeMarginals[yprev][yp][segEnd-ell+1] = new TIntDoubleHashMap();
                                edgeMarginals[yprev][yp][segEnd-ell+1].put(segEnd,alpha_Y_Array[segEnd-ell-base].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+beta_Y[segEnd].get(yp));
                                //edgeMarginals[yprev][yp][segEnd-ell+1].put(segEnd,Mi_YY.get(yprev,yp));
                            }
                        }
                    }
                }
                if ((grad != null) && (segEnd == trainingSegmentEnd) && (segEnd-ell+1==trainingSegmentStart)) {
                    trainingSegmentFound = true;
                    double val1 = Ri_Y.get(dataSeq.y(trainingSegmentEnd));
                    double val2 = 0;
                    if (trainingSegmentStart > 0) {
                        val2 = Mi_YY.get(dataSeq.y(trainingSegmentStart-1), dataSeq.y(trainingSegmentEnd));
                    }
                    if ((val1 == RobustMath.LOG0) || (val2 == RobustMath.LOG0)) {
                        System.out.println("Error: training labels not covered in generated features " + val1 + " "+val2
                                + " y " + dataSeq.y(trainingSegmentEnd));
                        System.out.println(dataSeq);
                        featureGenNested.startScanFeaturesAt(dataSeq, segEnd-ell,segEnd);
                        while (featureGenNested.hasNext()) { 
                            Feature feature = featureGenNested.next();
                            System.out.println(feature + " " + feature.yprev() + " "+feature.y());
                        }
                    }
                }
            }
            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y_Array[segEnd-base].toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[segEnd].toString());
            }
            
        }
        lZx = alpha_Y_Array[dataSeq.length()-1-base].zSum();
        if (dataSize > 0) beta_Y[dataSize-1] = oldBeta;
        if (segmentMarginals != null) {
            // normalize with respect to thisSeqLogLi.
            boolean normalize=false;
            if (normalize) {
            for (int y = 0; y < segmentMarginals.length; y++) {
                for (int segStart = 0; segStart < segmentMarginals[y].length; segStart++) {
                    if (segmentMarginals[y][segStart] == null) continue;
                    for (TIntDoubleIterator segEndProbIter = segmentMarginals[y][segStart].iterator(); segEndProbIter.hasNext();) {
                        segEndProbIter.advance();
                        segEndProbIter.setValue(Math.exp(segEndProbIter.value()-lZx));
                        //System.out.println(segEndProbIter.key() + " " + segEndProbIter.value());
                        assert (segmentMarginals[y][segStart].get(segEndProbIter.key()) < 1+0.0001);
                    }
                    if (edgeMarginals != null) {
                        for (int yprev = 0; yprev < edgeMarginals.length; yprev++) {
                            for (TIntDoubleIterator segEndProbIter = edgeMarginals[yprev][y][segStart].iterator(); segEndProbIter.hasNext();) {
                                segEndProbIter.advance();
                                segEndProbIter.setValue(Math.exp(segEndProbIter.value()-lZx));
                                assert (segEndProbIter.value() < 1+0.0001);
                            }
                        }
                    }
                }
            }
            }
            return lZx;
        }
        return thisSeqLogli;
    }
 
    /**
     * @param i
     */
    protected void allocateAlphaBeta(int newSize) {
        super.allocateAlphaBeta(newSize);
        alpha_Y_Array = new DoubleMatrix1D[newSize];
        for (int i = 0; i < alpha_Y_Array.length; i++)
            alpha_Y_Array[i] = newLogDoubleMatrix1D(numY);
        
        alpha_Y_ArrayM = new DoubleMatrix1D[newSize];
        for (int i = 0; i < alpha_Y_ArrayM.length; i++)
            alpha_Y_ArrayM[i] = newLogDoubleMatrix1D(numY);
        initAlphaMDone = new boolean[newSize];
        
    }
    // TODO..
    public static double initLogMi(CandSegDataSequence dataSeq, int prevPos, int pos, 
            FeatureGeneratorNested featureGenNested, double[] lambda, DoubleMatrix2D Mi, DoubleMatrix1D Ri) {
        featureGenNested.startScanFeaturesAt(dataSeq,prevPos,pos);
        Iterator constraints = dataSeq.constraints(prevPos,pos);
        return initLogMi(0.0,constraints,Mi,Ri);
    }
    
    public static boolean computeLogMi(CandSegDataSequence dataSeq, int prevPos, int pos, 
            FeatureGeneratorNested featureGenNested, 
            double[] lambda, DoubleMatrix2D Mi, DoubleMatrix1D Ri, 
            boolean reuseM, boolean initMDone) {
        if (reuseM && initMDone)
            Mi = null;
        computeLogMi(dataSeq, prevPos, pos, featureGenNested,lambda,Mi,Ri);
        if ((prevPos >= 0) && reuseM) {
            initMDone = true;
            //((FeatureGeneratorNestedSameTransitions)featureGenNested).transitionsCached();
        }
        return initMDone;
    }
    public static void computeLogMi(CandSegDataSequence dataSeq, int prevPos, int pos, 
            FeatureGeneratorNested featureGenNested, double[] lambda, DoubleMatrix2D Mi, DoubleMatrix1D Ri) {
        double defaultValue = initLogMi(dataSeq, prevPos,pos,featureGenNested,lambda,Mi,Ri);
        SparseTrainer.computeLogMiInitDone(featureGenNested,lambda,Mi,Ri,defaultValue);
    }
    
};
