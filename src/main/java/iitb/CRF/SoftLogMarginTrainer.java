/* SoftLogMarginTrainer.java
 * Created on Oct 16, 2007
 * 
 * @author sunita
 * @version 1.3
 * 
 * Objective is log (sum_y hammingLoss(y)*exp(W.(F(xi,y)-F(xi,yi))))
 * 
 */
package iitb.CRF;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class SoftLogMarginTrainer extends Trainer {
    @Override
    protected void setInitValue(double[] lambda) {
        if (initPhase) super.setInitValue(lambda);
    }


    DoubleMatrix1D alphas[]= new DenseDoubleMatrix1D[0];
    DoubleMatrix1D alphaLoss[]= new DenseDoubleMatrix1D[0];
    DoubleMatrix1D betaLoss[]= new DenseDoubleMatrix1D[0];
    boolean initPhase;
    public SoftLogMarginTrainer(CrfParams p) {
        super(p);
        logProcessing=true;
        
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval) {
        init(model,data,l);
        evaluator = eval;
        if (params.debugLvl > 0) {
            Util.printDbg("Number of features :" + lambda.length);      
        }
        initPhase=true;
        doTrain();
        initPhase=false;
        System.out.println("Exponential loss training...");
        doTrain();
    }
    @Override
    protected double sumProductInner(DataSequence dataSeq, FeatureGenerator featureGenerator, double[] lambda, double[] grad, 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < beta_Y.length; i++)
                beta_Y[i] = new DenseDoubleMatrix1D(numY);
            alphas= new DenseDoubleMatrix1D[beta_Y.length];
            alphaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            betaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            for (int i = 0; i < betaLoss.length; i++) {
                betaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphas[i] = new DenseDoubleMatrix1D(numY);
            }
        }
        if (initPhase) return super.sumProductInner(dataSeq, featureGenerator, 
                lambda, grad, onlyForwardPass, numRecord, fgenForExpVals);
        beta_Y[dataSeq.length()-1].assign(0);
        betaLoss[dataSeq.length()-1].assign(RobustMath.LOG0);
        double trainWDotF=0;
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
            int ycorr = dataSeq.y(i);
            trainWDotF += (Ri_Y.get(ycorr)+Mi_YY.get(dataSeq.y(i-1),ycorr));
            
            betaLoss[i-1].assign(RobustMath.LOG0);
           
            for (int yprev=0; yprev < numY; yprev++) {
                betaLoss[i-1].set(yprev,beta_Y[i].get(ycorr)+Ri_Y.get(ycorr)+Mi_YY.get(yprev,ycorr));
            }

            tmp_Y.assign(betaLoss[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, betaLoss[i-1],1,1,false,edgeGen);
        }
        double betaLogZ=0;
        double logZ=0;
        double obj = 0;
        double betat1=0, betat2=0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            if (i > 0) {
                tmp_Y.assign(alphas[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphas[i],1,0,true,edgeGen);
                alphas[i].assign(Ri_Y,sumFunc); 
            } else {
                alphas[i].assign(Ri_Y);

                tmp_Y.assign(beta_Y[0]);
                tmp_Y.assign(Ri_Y, sumFunc);
                
                int ycorr=dataSeq.y(0);
                trainWDotF += Ri_Y.get(ycorr);
                
                tmp_Y.assign(betaLoss[i]);
                tmp_Y.assign(Ri_Y,sumFunc);
                
                double t1 = RobustMath.logSumExp(tmp_Y)+Math.log(dataSeq.length())-trainWDotF;
                double t2= RobustMath.logSumExp(RobustMath.logSumExp(tmp_Y)
                        ,beta_Y[0].get(ycorr)+Ri_Y.get(ycorr))-trainWDotF;
                betat1=t1; betat2=t2;
                try {
                    assert(t1 > t2);
                    betaLogZ = RobustMath.logMinusExp(t1,t2);
                    logZ = Math.exp(t1)-Math.exp(t2);
//                    System.out.println("Diff "+(logZ-betaLogZ) + " "+logZ);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (i > 0) {
                tmp_Y.assign(alphaLoss[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphaLoss[i],1,0,true,edgeGen);
                alphaLoss[i].assign(Ri_Y,sumFunc);
            } else {
                alphaLoss[i].assign(RobustMath.LOG0);
            }
            int ycorr = dataSeq.y(i);
            alphaLoss[i].set(ycorr, RobustMath.logSumExp(alphaLoss[i].get(ycorr),alphas[i].get(ycorr)));

            // find features that fire at this position..
            fgenForExpVals.startScanFeaturesAt(dataSeq, i);
            while (fgenForExpVals.hasNext()) { 
                Feature feature = fgenForExpVals.next();
                int f = feature.index();
                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if (Math.abs(val) < Double.MIN_VALUE) continue;
                if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                    grad[f] += val*logZ;
                    //obj += val*lambda[f];
                }
                double logpr=beta_Y[i].get(yp);
                double logt2=0;
                if (yprev < 0) {
                    logpr += alphas[i].get(yp);
                    logt2 = RobustMath.logSumExp(alphaLoss[i].get(yp)+beta_Y[i].get(yp),
                            alphas[i].get(yp)+betaLoss[i].get(yp));
                } else {
                    logpr += alphas[i-1].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp);
                    
                    logt2 =alphaLoss[i-1].get(yprev)+beta_Y[i].get(yp);
                    logt2 = RobustMath.logSumExp(logt2,alphas[i-1].get(yprev)+betaLoss[i].get(yp));
                    if (yp==dataSeq.y(i)) {
                        logt2 = RobustMath.logSumExp(logt2,alphas[i-1].get(yprev)+beta_Y[i].get(yp));
                    }
                    logt2 += (Ri_Y.get(yp)+Mi_YY.get(yprev,yp));
                }
                grad[f] -= val*dataSeq.length()*Math.exp(logpr-trainWDotF);
                grad[f] += val*Math.exp(logt2-trainWDotF);
                assert(!Double.isInfinite(grad[f]));
                assert(!Double.isNaN(grad[f]));
            }
        }
        assert(!Double.isNaN(norm(grad)));
        
        double testVal=RobustMath.LOG0;
        for (int i = 0; i < dataSeq.length(); i++) {
            double t = alphas[i].get(dataSeq.y(i))+beta_Y[i].get(dataSeq.y(i));
            testVal = RobustMath.logSumExp(testVal, t);
        }
        testVal -= trainWDotF;
        assert(Math.abs(testVal-betat2) < 1e-4);
        /*
        double t1 = RobustMath.logSumExp(alphas[dataSeq.length()-1])+Math.log(dataSeq.length());
        double t2= RobustMath.logSumExp(alphaLoss[dataSeq.length()-1]);
        try {
            logZ = RobustMath.logMinusExp(t1,t2);
            if (Math.abs(logZ-betaLogZ) > 1e-2) {
                System.out.println((t1-t2)+ " "+(betat1-betat2));
                RobustMath.logMinusExp(t1,t2);
                RobustMath.logMinusExp(betat1, betat2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        return obj-logZ;
    }
    
    
    protected double sumProductInnerOld(DataSequence dataSeq, FeatureGenerator featureGenerator, double[] lambda, double[] grad, 
            boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < beta_Y.length; i++)
                beta_Y[i] = new DenseDoubleMatrix1D(numY);
            alphas= new DenseDoubleMatrix1D[beta_Y.length];
            alphaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            betaLoss= new DenseDoubleMatrix1D[beta_Y.length];
            for (int i = 0; i < betaLoss.length; i++) {
                betaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphaLoss[i] = new DenseDoubleMatrix1D(numY);
                alphas[i] = new DenseDoubleMatrix1D(numY);
            }
        }
        beta_Y[dataSeq.length()-1].assign(0);
        betaLoss[dataSeq.length()-1].assign(RobustMath.LOG0);
        double trainWDotF=0;
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
            int ycorr = dataSeq.y(i);
            trainWDotF += (Ri_Y.get(ycorr)+Mi_YY.get(dataSeq.y(i-1),ycorr));
            
            betaLoss[i-1].assign(RobustMath.LOG0);
           
            for (int yprev=0; yprev < numY; yprev++) {
                betaLoss[i-1].set(yprev,beta_Y[i].get(ycorr)+Ri_Y.get(ycorr)+Mi_YY.get(yprev,ycorr));
            }

            tmp_Y.assign(betaLoss[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, betaLoss[i-1],1,1,false,edgeGen);
        }
        double betaLogZ=0;
        double logZ=0;
        double obj = 0;
        double betat1=0, betat2=0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            if (i > 0) {
                tmp_Y.assign(alphas[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphas[i],1,0,true,edgeGen);
                alphas[i].assign(Ri_Y,sumFunc); 
            } else {
                alphas[i].assign(Ri_Y);

                tmp_Y.assign(beta_Y[0]);
                tmp_Y.assign(Ri_Y, sumFunc);
                double t1 = RobustMath.logSumExp(tmp_Y)+Math.log(dataSeq.length());
                
                int ycorr=dataSeq.y(0);
                trainWDotF += Ri_Y.get(ycorr);
                
                tmp_Y.assign(betaLoss[i]);
                tmp_Y.assign(Ri_Y,sumFunc);
                
                double t2= RobustMath.logSumExp(RobustMath.logSumExp(tmp_Y)
                        ,beta_Y[0].get(ycorr)+Ri_Y.get(ycorr));
                betat1=t1; betat2=t2;
                try {
                    assert(t1 > t2);
                    betaLogZ = RobustMath.logMinusExp(t1,t2);
                    logZ = Math.log(Math.exp(t1)-Math.exp(t2));
                    System.out.println("Diff "+(logZ-betaLogZ) + " "+logZ);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (i > 0) {
                tmp_Y.assign(alphaLoss[i-1]);
                RobustMath.logMult(Mi_YY, tmp_Y, alphaLoss[i],1,0,true,edgeGen);
                alphaLoss[i].assign(Ri_Y,sumFunc);
            } else {
                alphaLoss[i].assign(RobustMath.LOG0);
            }
            int ycorr = dataSeq.y(i);
            alphaLoss[i].set(ycorr, RobustMath.logSumExp(alphaLoss[i].get(ycorr),alphas[i].get(ycorr)));

            // find features that fire at this position..
            fgenForExpVals.startScanFeaturesAt(dataSeq, i);
            while (fgenForExpVals.hasNext()) { 
                Feature feature = fgenForExpVals.next();
                int f = feature.index();
                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if (Math.abs(val) < Double.MIN_VALUE) continue;
                if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                    grad[f] += val;
                    obj += val*lambda[f];
                }
                double logpr=beta_Y[i].get(yp);
                double logt2=0;
                if (yprev < 0) {
                    logpr += alphas[i].get(yp);
                    logt2 = RobustMath.logSumExp(alphaLoss[i].get(yp)+beta_Y[i].get(yp),
                            alphas[i].get(yp)+betaLoss[i].get(yp));
                } else {
                    logpr += alphas[i-1].get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp);
                    
                    logt2 =alphaLoss[i-1].get(yprev)+beta_Y[i].get(yp);
                    logt2 = RobustMath.logSumExp(logt2,alphas[i-1].get(yprev)+betaLoss[i].get(yp));
                    if (yp==dataSeq.y(i)) {
                        logt2 = RobustMath.logSumExp(logt2,alphas[i-1].get(yprev)+beta_Y[i].get(yp));
                    }
                    logt2 += (Ri_Y.get(yp)+Mi_YY.get(yprev,yp));
                }
                grad[f] -= val*Math.exp(RobustMath.logMinusExp(Math.log(dataSeq.length())+logpr,logt2)-logZ);
                assert(!Double.isInfinite(grad[f]));
                assert(!Double.isNaN(grad[f]));
            }
        }
        assert(!Double.isNaN(norm(grad)));
        
        double testVal=RobustMath.LOG0;
        for (int i = 0; i < dataSeq.length(); i++) {
            double t = alphas[i].get(dataSeq.y(i))+beta_Y[i].get(dataSeq.y(i));
            testVal = RobustMath.logSumExp(testVal, t);
        }
        assert(Math.abs(testVal-betat2) < 1e-4);
        
        double t1 = RobustMath.logSumExp(alphas[dataSeq.length()-1])+Math.log(dataSeq.length());
        double t2= RobustMath.logSumExp(alphaLoss[dataSeq.length()-1]);
        try {
            logZ = RobustMath.logMinusExp(t1,t2);
            if (Math.abs(logZ-betaLogZ) > 1e-2) {
                System.out.println((t1-t2)+ " "+(betat1-betat2));
                RobustMath.logMinusExp(t1,t2);
                RobustMath.logMinusExp(betat1, betat2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj-logZ;
    }
    
}
