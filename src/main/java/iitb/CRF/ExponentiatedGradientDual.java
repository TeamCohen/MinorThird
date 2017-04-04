/*
 * Created on Jul 16, 2008
 * @author sunita
 * 
 * Trainer as in "Exponentiated Gradient Algorithms for Conditional Random
Fields and Max-Margin Markov Networks" in JMLR 08
 */
package iitb.CRF;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class ExponentiatedGradientDual extends Trainer {
    Vector<DataSequence> dataSeqs = new Vector<DataSequence>();
    double thetaState[][][], thetaTransition[][][][];
    double etas[]; // the learning rate.
    public ExponentiatedGradientDual(CrfParams p) {
        super(p);
    }
    @Override
    protected void init(CRF model, DataIter data, double[] l) {
        super.init(model, data, l);
        int numRecs = 0;
        for(data.startScan(); data.hasNext();dataSeqs.add(data.next()), numRecs++);
        thetaState = new double[numRecs][0][0];
        thetaTransition = new double[numRecs][0][0][0];
        for (int i = 0; i < numRecs; i++) {
            thetaState[i] = new double[dataSeqs.get(i).length()][numY];
            thetaTransition[i] = new double[dataSeqs.get(i).length()][numY][numY];
        }
        tempMi_YY = new DenseDoubleMatrix2D(numY,numY);
        etas=new double[numRecs];
        Arrays.fill(etas, 0.01);
    }
    @Override
    protected void doTrain() {
        setInitialValues();
        Random random = new Random();
        for (int iter = 0; iter < params.maxIters; iter++) {
            int k = random.nextInt(dataSeqs.size());
            // now update thetas.
            DataSequence dataSeq = dataSeqs.get(k);
            calculateExpFValuesUsingTheta(dataSeq,ExpF,thetaState[k],thetaTransition[k],false);
            //TODO: need to set etas properly.
            double eta = etas[k];
            for (int i = 0; i < thetaState[k].length; i++) {
                initMDone = computeLogMiTrainMode(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, false, reuseM, initMDone);
                for (int y = 0; y < numY; y++) {
                    thetaState[k][i][y] -= eta*(thetaState[k][i][y]-params.invSigmaSquare*Ri_Y.get(y));
                    if (i > 0) {
                        for (int yprev = 0; yprev < numY; yprev++) {
                            thetaTransition[k][i][yprev][y] -= eta*(thetaTransition[k][i][yprev][y]-params.invSigmaSquare*Mi_YY.get(yprev,y));
                        }
                    }
                }
            }
            // update lambda..
            for (int f = 0; f < lambda.length; f++) {
                lambda[f] += ExpF[f];
            }
            calculateExpFValuesUsingTheta(dataSeq,ExpF,thetaState[k],thetaTransition[k],false);
            for (int f = 0; f < lambda.length; f++) {
                lambda[f] -= ExpF[f];
            } 
        }
        for (int f = 0; f < lambda.length; f++) {
            lambda[f] *= params.invSigmaSquare;
        }
    }
    DoubleMatrix2D tempMi_YY;
    private void calculateExpFValuesUsingTheta(DataSequence dataSeq, double[] expF, double[][] thetaStateK, double[][][] thetaEdgeK,boolean addCorrFVec) {
        Arrays.fill(expF,RobustMath.LOG0);
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            allocateAlphaBeta(2*dataSeq.length()+1);
        }
        beta_Y[dataSeq.length()-1].assign(0);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            tmp_Y.assign(thetaStateK[i]);
            tmp_Y.assign(beta_Y[i],sumFunc);
            tempMi_YY.assign(thetaEdgeK[i]);
            RobustMath.logMult(tempMi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
        }
        alpha_Y.assign(0);
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            if (i > 0) {
                tmp_Y.assign(alpha_Y);
                tempMi_YY.assign(thetaEdgeK[i]);
                RobustMath.logMult(tempMi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                tmp_Y.assign(thetaStateK[i]);
                newAlpha_Y.assign(tmp_Y,sumFunc); 
            } else {
                newAlpha_Y.assign(thetaStateK[i]);
            }
            // find features that fire at this position..
            featureGenerator.startScanFeaturesAt(dataSeq, i);
            while (featureGenerator.hasNext()) { 
                Feature feature = featureGenerator.next();
                int f = feature.index();

                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if ((addCorrFVec) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                    expF[f] -= val;
                    if (params.debugLvl > 2) {
                        System.out.println("Feature fired " + f + " " + feature);
                    } 
                }
                if (Math.abs(val) < Double.MIN_VALUE) continue;
                if (val < 0) {
                    System.out.println("ERROR: Cannot process negative feature values in log domains: " 
                            + "either disable the '-trainer=ll' flag or ensure feature values are not -ve");
                    continue;
                }
                if (yprev < 0) {
                    expF[f] = RobustMath.logSumExp(ExpF[f], newAlpha_Y.get(yp) + RobustMath.log(val) + beta_Y[i].get(yp));
                } else {
                    expF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yprev)+thetaStateK[i][yp]+tempMi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[i].get(yp));
                }
            }
            alpha_Y.assign(newAlpha_Y);

            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y.toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
        }
    }
    private void setInitialValues() {
        Arrays.fill(lambda, 0);
        for (int i = 0; i < dataSeqs.size(); i++) {
            calculateExpFValuesUsingTheta(dataSeqs.get(i), ExpF, thetaState[i], thetaTransition[i], true);
            for (int f = 0; f < lambda.length; f++) {
                lambda[f] -= ExpF[f];
            }
        }
    }
  
}
