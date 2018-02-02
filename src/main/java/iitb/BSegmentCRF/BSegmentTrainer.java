/* BSegmentTrainer.java
 * Created on Apr 2, 2005
 *
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.BSegmentCRF;

import iitb.CRF.CRF;
import iitb.CRF.CrfParams;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;
import iitb.CRF.FeatureGenerator;
import iitb.CRF.RobustMath;
import iitb.CRF.SegmentTrainer;
import iitb.CRF.Segmentation;
import cern.colt.matrix.DoubleMatrix1D;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

class BSegmentTrainer extends SegmentTrainer {
    BFeatureGenerator bfgen;
    DoubleMatrix1D openBeta[];
    DoubleMatrix1D openAlpha[];
    DoubleMatrix1D deltaRi,openDeltaRi;
    
    DoubleMatrix1D aMdRs[]=null;
    DoubleMatrix1D exactETerms[], prevExactETerms[];
    DoubleMatrix1D termSE = null, termSEPart;
    DoubleMatrix1D exactETerm=null, exactSTerm=null;
    DoubleMatrix1D rbetaTerms = null;
    DoubleMatrix1D rbeta=null;
    MatrixWithRange iOpenR = null,endOpenRi;
    FeatureStore.Iter fIter;
    
    static class MatrixWithRange  {
        DoubleMatrix1D mat;
        int start;
        int end;
        /**
         * @param numY
         */
        public MatrixWithRange(DoubleMatrix1D mat) {
            this.mat = mat;
        }
        void init(int start, int end) {
            this.start = start;
            this.end = end;
            mat.assign(RobustMath.LOG0);
        }
        /*
         void assign(MatrixWithRange arg) {
         this.start = arg.start;
         this.end = arg.end;
         this.mat.assign(arg.mat);
         }
         */
        /**
         * @param i
         * @param j
         * @param fstore
         */
        // initialize with all features where end boundary is open
        public void init(int start, int end, FeatureStore fstore,boolean startOpen) {
            init(start,Math.min(start-1,end));
            for (int i = start; i <= end; i++) {
                fstore.incrementRightB(null,this,startOpen);
            }
        }
//      initialize with all features where start and end boundaries are open
        public void init(int start, int end, FeatureStore fstore) {
            init(start,end,fstore,true);
        }
        /**
         * @param openRi
         * @param ri_Y
         */
        public void assign(MatrixWithRange arg, DoubleMatrix1D ri_Y) {
            this.start = arg.start;
            this.end = arg.end;
            this.mat.assign(ri_Y);
        }
    }
    MatrixWithRange openRi;
    FeatureStoreCache fstore;
    
    double F[];
    public BSegmentTrainer(CrfParams p) {
        super(p);
    }
    protected void init(CRF model, DataIter data, double[] l) {
        super.init(model,data,l);
        
        bfgen = ((BSegmentCRF)model).bfgen;
        F = new double[bfgen.numFeatures()];
        aMdRs = new DoubleMatrix1D[3*bfgen.maxBoundaryGap()];
        for (int i = 0; i < aMdRs.length; i++) {
            aMdRs[i] = newLogDoubleMatrix1D(numY);
        }
        exactETerms = new DoubleMatrix1D[bfgen.maxBoundaryGap()+1];
        prevExactETerms = new DoubleMatrix1D[bfgen.maxBoundaryGap()+1];
        for (int i = 0; i < exactETerms.length; i++) {
            prevExactETerms[i] = newLogDoubleMatrix1D(numY);
            exactETerms[i] = newLogDoubleMatrix1D(numY);
        }
        termSE = newLogDoubleMatrix1D(numY);
        termSEPart  = newLogDoubleMatrix1D(numY);
        exactETerm = newLogDoubleMatrix1D(numY);
        exactSTerm = newLogDoubleMatrix1D(numY);
        rbetaTerms = newLogDoubleMatrix1D(numY);
        rbeta = newLogDoubleMatrix1D(numY);
        
        fstore = new FeatureStoreCache(model.params.miscOptions.getProperty("cache","false").equals("true"), reuseM);
        fIter = fstore.getIterator();
        iOpenR = new MatrixWithRange(newLogDoubleMatrix1D(numY));
        openRi =  new MatrixWithRange(newLogDoubleMatrix1D(numY));
        endOpenRi = new MatrixWithRange(newLogDoubleMatrix1D(numY));
        deltaRi = newLogDoubleMatrix1D(numY);
        openDeltaRi = newLogDoubleMatrix1D(numY);
        
        
    }
    protected void allocateAlphaBeta(int newSize) {
        super.allocateAlphaBeta(newSize);
        openAlpha = new DoubleMatrix1D[newSize];
        for (int i = 0; i < openAlpha.length; i++) {
            openAlpha[i] = newLogDoubleMatrix1D(numY);
        }
        openBeta = new DoubleMatrix1D[newSize];
        for (int i = 0; i < openBeta.length; i++) {
            openBeta[i] = newLogDoubleMatrix1D(numY);
        }
        
    }
    boolean holdsInTrainingData(BFeature feature, Segmentation data) {
        if (data.getSegmentId(feature.start()) != data.getSegmentId(feature.end()))
            return false;
        int segNum = data.getSegmentId(feature.start());
        if (data.segmentLabel(segNum) != feature.y())
            return false;
        if (!feature.startOpen() && (data.segmentStart(segNum) != feature.start()))
            return false;
        if (!feature.endOpen() && (data.segmentEnd(segNum) != feature.end()))
            return false;
        if ((segNum==0) && (feature.yprev() >= 0))
            return false;
        if ((segNum > 0) && (feature.yprev() >= 0) && (data.segmentLabel(segNum-1) != feature.yprev()))
            return false;
        return true;
    }
    DoubleMatrix1D exactCompute(int ls, int le, int rs, int re, boolean multiplyAlpha) {
        int start = ls;
        MatrixWithRange openRi = new MatrixWithRange(newLogDoubleMatrix1D(numY));
        MatrixWithRange iOpenR = new MatrixWithRange(newLogDoubleMatrix1D(numY));
        DoubleMatrix1D rbeta = newLogDoubleMatrix1D(numY);
        DoubleMatrix1D rbetaTerms = newLogDoubleMatrix1D(numY);
        DoubleMatrix1D Ri_Y = newLogDoubleMatrix1D(numY);
        openRi.init(start+1,rs-1,fstore); 
        rbeta.assign(RobustMath.LOG0);
        for (int ip = start; ip >= le; ip--) {
            rbetaTerms.assign(RobustMath.LOG0);
            fstore.decrementLeftB(Ri_Y,openRi,true);
            iOpenR.assign(openRi, Ri_Y);
            for (int i = rs; i <= re; i++) {
                fstore.incrementRightB(Ri_Y,iOpenR);
                assert ((iOpenR.end == i) && (iOpenR.start == ip));
                tmp_Y.assign(beta_Y[i]);
                tmp_Y.assign(Ri_Y,sumFunc);
                rbetaTerms.assign(tmp_Y,RobustMath.logSumExpFunc);
            }
            int base = -1;
            if ((ip > 0) && multiplyAlpha)
                rbetaTerms.assign(alpha_Y_ArrayM[ip-1-base],sumFunc);
            rbeta.assign(rbetaTerms,RobustMath.logSumExpFunc);
        }
        return rbeta;
    }
    
    private double computeFeatureGradOpt(FeatureStore.Iter fIter, DataSequence dataSeq, int m, double grad[], double lambda[], double thisSeqLogli) {
        if (!fIter.hasNext())
            return thisSeqLogli;
        for (int i = 0; i <= m; exactETerms[i++].assign(RobustMath.LOG0)) {
            prevExactETerms[i].assign(RobustMath.LOG0);
        }
        int dataSize = dataSeq.length();
        int base = -1;
        // now compute the feature gradient for state features.
        // go over each feature in increasing (s,e) order.
        
        BFeature f = fIter.next();
        DoubleMatrix1D aMdR = aMdRs[0];
        for (int s = 0; s < dataSize; s++) {
            fstore.deltaR_LShift(s,s+m,deltaRi,openDeltaRi);
            if (s > 0) {
                aMdR.assign(openDeltaRi,sumFunc);
                
                tmp_Y.assign(deltaRi);
                tmp_Y.assign(alpha_Y_ArrayM[s-1-base],sumFunc);
                aMdR.assign(tmp_Y,RobustMath.logSumExpFunc);
            } else {
                aMdR.assign(deltaRi);
            }
            
//          if ((f != null) && (f.start() > s))
//          continue;
            if (!reuseM) fstore.getLogMi(s,Mi_YY);
            
            int e = Math.min(dataSize,s+m);
            
            DoubleMatrix1D tmpArr[]=prevExactETerms;
            prevExactETerms = exactETerms;
            exactETerms = tmpArr;
            
            if (s+m < dataSize) {
                exactETerm.assign(openAlpha[e-1]);
                exactETerm.assign(beta_Y[e],sumFunc);
                fstore.deltaR_RShift(s,e,deltaRi, openDeltaRi);
                exactETerm.assign(deltaRi,sumFunc);
                exactETerms[m].assign(exactETerm);
                
                fstore.deltaR_LShift(s,s+m,deltaRi, openDeltaRi);
                exactSTerm.assign(deltaRi);
                exactSTerm.assign(openBeta[s], sumFunc);
                
                termSE.assign(aMdR);
                termSE.assign(openBeta[s],sumFunc);
            } else {
                exactSTerm.assign(RobustMath.LOG0);
                termSE.assign(RobustMath.LOG0);
            }
            
            iOpenR.init(s,e,fstore,false);
            for (e--; e >= s; e--) {
                int l = e-s;
                assert (l < m);
                
                fstore.decrementRightB(Ri_Y,iOpenR);
                
                rbeta.assign(Ri_Y);
                rbeta.assign(beta_Y[e],sumFunc);
                tmp_Y.assign(rbeta);
                if (s > 0) {
                    tmp_Y.assign(alpha_Y_ArrayM[s-1-base],sumFunc);
                }
                exactETerm.assign(tmp_Y);
                
                if (s > 0) {
                    exactETerm.assign(prevExactETerms[l+1],RobustMath.logSumExpFunc);
                }
                exactETerms[l].assign(exactETerm);
                
                exactSTerm.assign(rbeta,RobustMath.logSumExpFunc);
                
                termSE.assign(exactETerm,RobustMath.logSumExpFunc);
                
                //assert (exactCompute(s,0,e,dataSize-1,true).equals(termSE));
                // termSE.assign(exactCompute(s,0,e,dataSize-1,true));
                
                //assert (exactCompute(s,0,e,e,true).equals(exactETerm));
                //exactETerm.assign(exactCompute(s,0,e,e,true));
                
                //assert (exactCompute(s,s,e,dataSize-1,false).equals(exactSTerm));
                //exactSTerm.assign(exactCompute(s,s,e,dataSize-1,false));
                
                // process all features with boundary (s,e)
                if (params.debugLvl > 2) System.out.println("Features for boundary: [" + s + " " + e + "]");
                while ((f != null) && ((f.start()==s) && (f.end()==e))) {
                    if ((icall == 0) && (grad != null) && holdsInTrainingData(f, (Segmentation)dataSeq)) {
                        F[f.index()] += f.value();
                        
                        if (params.debugLvl > 2) System.out.println("Holds " + f.index() + " " + bfgen.featureName(f.index()) + " " + f.start() + " " + f.startOpen() + " " + f.end());
                    }
                    if (params.debugLvl > 3) System.out.println(f);
                    double val = RobustMath.log(f.value());
                    if (!f.endOpen() && !f.startOpen()) {
                        val += (Ri_Y.get(f.y())+beta_Y[e].get(f.y()));
                        if (s > 0)
                            val += alpha_Y_ArrayM[s-base-1].get(f.y());
                    } else if (f.endOpen() && !f.startOpen()) {
                        val += exactSTerm.get(f.y());
                        if (s > 0) {
                            if (f.yprev() >= 0) {
                                val += (alpha_Y_Array[s-1-base].get(f.yprev()) + Mi_YY.get(f.yprev(), f.y()));
                            } else {
                                val += (alpha_Y_ArrayM[s-1-base].get(f.y()));
                            }
                        }
                    } else if (f.startOpen() && !f.endOpen()) {
                        val += exactETerm.get(f.y());
                    } else {
                        val += termSE.get(f.y());
                    }
                    ExpF[f.index()] = RobustMath.logSumExp(ExpF[f.index()], val);
                    if (fIter.hasNext()) {
                        f = fIter.next();
                        assert ((f.start() > s) || ((f.start() == s) && (f.end() <= e))); 
                    } else 
                        f = null;
                }
            }
        }
        return thisSeqLogli;
    }
    protected double sumProductInner(DataSequence dataSeq, FeatureGenerator featureGenerator, double lambda[], double grad[], 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        fstore.init(dataSeq,bfgen,lambda,numY,numRecord);
        int m = bfgen.maxBoundaryGap();
        int base = -1;
        int dataSize = dataSeq.length();
        if ((beta_Y==null) || beta_Y.length < dataSize+1)
            allocateAlphaBeta(2*dataSize);
        for (int i = dataSeq.length(); i >= 0; i--)
            initAlphaMDone[i] = false;
        DoubleMatrix1D oldBeta =  beta_Y[dataSeq.length()-1];
        beta_Y[dataSeq.length()-1] = allZeroVector;
        for (int i = dataSeq.length()-2; i >= 0; i--) {
            beta_Y[i].assign(RobustMath.LOG0);
        }
        if (reuseM && (dataSeq.length() > 0))
            fstore.getLogMi(1,Mi_YY);
        for (int i = dataSeq.length()-2; i >= 0; i--) {
            // compute beta[i]
            openRi.init(i+1,i);
            for (int ip = i+1; (ip <= i+m) && (ip < dataSize); ip++) {
                fstore.incrementRightB(Ri_Y,openRi);
                tmp_Y.assign(Ri_Y);
                if (ip < dataSize-1) tmp_Y.assign(beta_Y[ip], sumFunc);
                beta_Y[i].assign(tmp_Y, RobustMath.logSumExpFunc);
            }
            if (i <= dataSize-1-m) {
                fstore.removeExactStartFeatures(Ri_Y,i+1,i+m);
                openBeta[i].assign(Ri_Y); // R(i+1,i+m)
            }
            if (i < dataSize-m-1) {
                fstore.deltaR_LShift(i+1,i+m+1,deltaRi, openDeltaRi);
                tmp_Y.assign(deltaRi);
                tmp_Y.assign(openBeta[i+1], sumFunc);
                beta_Y[i].assign(tmp_Y, RobustMath.logSumExpFunc);
                
                openBeta[i].assign(beta_Y[i+m], sumFunc);
                tmp_Y.assign(openDeltaRi);
                tmp_Y.assign(openBeta[i+1], sumFunc);
                openBeta[i].assign(tmp_Y, RobustMath.logSumExpFunc);
            }
            if (i >= 0) {
                // get Mi.
                if (!reuseM) fstore.getLogMi(i+1,Mi_YY);
                tmp_Y.assign(beta_Y[i]);
                Mi_YY.zMult(tmp_Y, beta_Y[i],1,0,false);
            }
        }
        double thisSeqLogli = 0;
        
        alpha_Y_Array[0] = allZeroVector;
        for (int i = 0; i < dataSize; i++) {
            alpha_Y_Array[i-base].assign(RobustMath.LOG0);
            openRi.init(i+1,i);
            for (int ip = i; (ip > i-m) && (ip >= 0); ip--) {
                fstore.decrementLeftB(Ri_Y,openRi);
                if (ip-1 >= 0) {
                    if (!reuseM) {
                        fstore.getLogMi(ip,Mi_YY);
                    }
                    if (!initAlphaMDone[ip-1-base]) {
                        alpha_Y_ArrayM[ip-1-base].assign(RobustMath.LOG0);
                        Mi_YY.zMult(alpha_Y_Array[ip-1-base],alpha_Y_ArrayM[ip-1-base],1,0,true);
                        initAlphaMDone[ip-1-base] = true;
                    }
                    newAlpha_Y.assign(alpha_Y_ArrayM[ip-1-base]);
                    newAlpha_Y.assign(Ri_Y,sumFunc);
                } else {
                    newAlpha_Y.assign(Ri_Y);
                }
                if (params.debugLvl > 2) {
                    System.out.println("At sequence position "+i + " " + ip);
                    System.out.println("Alpha-i " + newAlpha_Y.toString());
                }
                alpha_Y_Array[i-base].assign(newAlpha_Y, RobustMath.logSumExpFunc);
            }
            // Ri at this point contains features for segment (i-m+1, i)
            if (i-m >= -1) {
                fstore.removeExactEndFeatures(Ri_Y, i-m+1, i);
                openAlpha[i].assign(Ri_Y);
            }
            if (i-m >= 0) {
                fstore.deltaR_RShift(i-m,i,deltaRi, openDeltaRi);
                tmp_Y.assign(openAlpha[i-1]);
                tmp_Y.assign(deltaRi, sumFunc);
                alpha_Y_Array[i-base].assign(tmp_Y, RobustMath.logSumExpFunc);
                
//              compute open-alpha
                tmp_Y.assign(openDeltaRi);
                tmp_Y.assign(openAlpha[i-1], sumFunc);
                openAlpha[i].assign(alpha_Y_ArrayM[i-m-base],sumFunc);
                openAlpha[i].assign(tmp_Y, RobustMath.logSumExpFunc);
            }
            
            if (params.debugLvl > 2) {
                System.out.println("At sequence position "+i);
                System.out.println("Alpha-i " + alpha_Y_Array[i-base].toString());
                System.out.println("Ri " + Ri_Y.toString());
                if (!reuseM) System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
        }
        if (fgenForExpVals != null) {
            FeatureStore.Iter featureIter = fIter;
            if (fgenForExpVals != featureGenerator) {
                // a feature generator different than used for training.
                FeatureStore featureStore = new FeatureStore(reuseM);
                featureStore.init(dataSeq,(BFeatureGenerator)fgenForExpVals,lambda,numY);
                featureIter = featureStore.getIterator();
                featureStore.scanFeaturesSorted(featureIter);
            } else {
                fstore.scanFeaturesSorted(fIter);
            }
            thisSeqLogli = computeFeatureGradOpt(featureIter, dataSeq,m,grad,lambda,thisSeqLogli);
        }
        lZx = alpha_Y_Array[dataSeq.length()-1-base].zSum();
        beta_Y[dataSeq.length()-1] = oldBeta;
        return thisSeqLogli;
    }
    protected double finishGradCompute(double grad[], double lambda[], double logli) {
        if (grad != null) {
        for (int fi = 0; fi < grad.length; fi++) {
            logli += F[fi]*lambda[fi];
            grad[fi] += F[fi];
        }
        }
        return logli;
    }
};
