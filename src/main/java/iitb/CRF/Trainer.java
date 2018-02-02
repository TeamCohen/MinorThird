/** Trainer.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3 
 */
package iitb.CRF;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import riso.numerical.LBFGS;
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class Trainer {
    protected int numF,numY;
    protected double gradLogli[];
    double diag[];
    protected double lambda[];
    protected boolean reuseM, initMDone=false, logProcessing=false;
    
    protected double ExpF[], lZx;
    double scale[], rLogScale[];
    
    protected DoubleMatrix2D Mi_YY;
    protected DoubleMatrix1D Ri_Y;
    protected DoubleMatrix1D alpha_Y, newAlpha_Y;
    protected DoubleMatrix1D beta_Y[];
    protected DoubleMatrix1D tmp_Y;
    
    static class  MultFunc implements DoubleDoubleFunction {
        public double apply(double a, double b) {return a*b;}
    };
    static class  SumFunc implements DoubleDoubleFunction {
        public double apply(double a, double b) {return a+b;}
    };
    static MultFunc multFunc = new MultFunc(); 
    protected static SumFunc sumFunc = new SumFunc(); 
    
    class MultSingle implements DoubleFunction {
        public double multiplicator = 1.0;
        public double apply(double a) {return a*multiplicator;}
    };
    MultSingle constMultiplier = new MultSingle();
    
    protected DataIter diter;
    protected FeatureGenerator featureGenerator;
    protected CrfParams params;
    protected EdgeGenerator edgeGen;
    protected int icall;
    protected float instanceWts[];
    Evaluator evaluator = null;
    
    protected FeatureGenCache featureGenCache;
    
    protected double norm(double ar[]) {
        double v = 0;
        for (int f = 0; f < ar.length; f++)
            v += ar[f]*ar[f];
        return Math.sqrt(v);
    }
    public Trainer(CrfParams p) {
        params = p; 
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval) {
        trainInternal(model,data,l,eval,null,null);
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval, float[] instanceWts) {
        if (instanceWts==null) {
            // this is to ensure backward compatibility with trainers who might have overridden the above function.
            train(model,data,l,eval);
            return;
        }
        trainInternal(model,data,l,eval,instanceWts,null);
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval, float[] instanceWts, float misClassifyCost[][]) {
        if ((instanceWts==null) && (misClassifyCost==null)) {
            // this is to ensure backward compatibility with trainers who might have overridden the above function.
            train(model,data,l,eval);
            return;
        }
        trainInternal(model,data,l,eval,instanceWts, misClassifyCost);
    }
    // this last argument is ignored for logistic trainers on sequence data.
    private void trainInternal(CRF model, DataIter data, double[] l, Evaluator eval, float[] instanceWts, float misClassifyCost[][]) {
        init(model,data,l);
        evaluator = eval;
        this.instanceWts = instanceWts;
        if (params.debugLvl > 0) {
            Util.printDbg("Number of features :" + lambda.length);      
        }
        doTrain();
    }
    
    protected void setInitValue(double lambda[]) {
        if (params.miscOptions.getProperty("initValues") != null) {
            // starting values stored in a file where each line has (featureName, value) pair
            String fname = params.miscOptions.getProperty("initValues");
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(fname));
            
            String line;
            boolean idOrdered=Boolean.parseBoolean(params.miscOptions.getProperty("initValuesOrdered", "false"));
            Hashtable<String, Double> initVals = new Hashtable<String, Double>();
            for(int l = 0; ((line=in.readLine())!=null); l++) {
                StringTokenizer entry = new StringTokenizer(line);
                String featureName = entry.nextToken();
                double fval = Double.parseDouble(entry.nextToken());
                if (!idOrdered) 
                    initVals.put(featureName,fval);
                else
                    lambda[l] = fval;
            }
            if (!idOrdered) {
            for (int j = 0 ; j < lambda.length ; j ++) {
                String featureName = featureGenerator.featureName(j);
                lambda[j] = (initVals.get(featureName) != null)?initVals.get(featureName):getInitValue();
            }
            }
            return;
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("ERROR: in file initialization, using default init process");
            }
        } else if (Boolean.parseBoolean(params.miscOptions.getProperty("initValuesUseExisting", "false"))) {
            // use existing values of lambda from the model as starting point.
            return;
        } else
        for (int j = 0 ; j < lambda.length ; j ++) {
            lambda[j] = getInitValue();
        }
    }
    double getInitValue() { 
        return params.initValue;
    }
    protected void init(CRF model, DataIter data, double[] l) {
        edgeGen = model.edgeGen;
        lambda = l;
        numY = model.numY;
        diter = data;
        featureGenerator = model.featureGenerator;
        numF = featureGenerator.numFeatures();
        
        gradLogli = new double[numF];
        diag = new double [ numF ]; // needed by the optimizer
        ExpF = new double[lambda.length];
        initMatrices();
        reuseM = params.reuseM;
        if (params.trainerType.equals("ll"))
            logProcessing=true;
        
        if ((data != null) && params.miscOptions.getProperty("cache", "false").equals("true")) {
            featureGenCache = new FeatureGenCache(featureGenerator,reuseM);
            featureGenCache.setDataKeys(data);
            featureGenerator = featureGenCache;
        } else
            featureGenCache = null;
    }
    void initMatrices() {
        Mi_YY = new DenseDoubleMatrix2D(numY,numY);
        Ri_Y = new DenseDoubleMatrix1D(numY);
        
        alpha_Y = new DenseDoubleMatrix1D(numY);
        newAlpha_Y = new DenseDoubleMatrix1D(numY);
        tmp_Y = new DenseDoubleMatrix1D(numY);
    }
    
    protected void doTrain() {
        double f, xtol = 1.0e-16; // machine precision
        int iprint[] = new int [2], iflag[] = new int[1];
        icall=0;
        
        iprint [0] = params.debugLvl-2;
        iprint [1] = params.debugLvl-1;
        iflag[0]=0;
        double variables[] = lambda;
        boolean positiveConstraint = params.miscOptions.getProperty("prior", "gaussian").equals("exp");
        if (positiveConstraint) {
            variables = new double[lambda.length];
        }
        
        setInitValue(variables);
        
        do {
            if (positiveConstraint) {
                for (int i = 0; i < variables.length; i++) {
                    lambda[i] = Math.exp(variables[i]);
                }
                f = computeFunctionGradient(lambda,gradLogli); 
                for (int i = 0; i < gradLogli.length; i++) {
                    gradLogli[i] *= Math.exp(variables[i]);
                }
            } else {
                f = computeFunctionGradient(lambda,gradLogli); 
            }
            f = -1*f; // since the routine below minimizes and we want to maximize logli
            for (int j = 0 ; j < lambda.length ; j ++) {
                gradLogli[j] *= -1;
            } 
            
            if ((evaluator != null) && (evaluator.evaluate() == false))
                break;
            try	{
                LBFGS.lbfgs (numF, params.mForHessian, variables, f, gradLogli, false, diag, iprint, params.epsForConvergence, xtol, iflag);
            } catch (LBFGS.ExceptionWithIflag e)  {
                System.err.println( "CRF: lbfgs failed.\n"+e );
                if (e.iflag == -1) {
                    System.err.println("Possible reasons could be: \n \t 1. Bug in the feature generation or data handling code\n\t 2. Not enough features to make observed feature value==expected value\n");
                }
                return;
            }
            icall += 1;
        } while (( iflag[0] != 0) && (icall <= params.maxIters));
        reInit();
    }
    protected double computeFunctionGradient(double lambda[], double grad[]) {
        return computeFunctionGradient(lambda,grad,null,featureGenerator);
    }
    protected double finishGradCompute(double grad[], double lambda[], double logli) {
        return logli;
    }
    protected void computeFeatureExpectedValue(DataIter dataIter, FeatureGenerator fgen, double lambda[], double expFVals[]) {
        diter = dataIter;
        featureGenCache = null;
        for (int i = 0; i < expFVals.length; expFVals[i++] = 0);
        if (fgen.numFeatures() > ExpF.length) {
            // a different feature generator..
            ExpF = new double[fgen.numFeatures()];
        }
        computeFunctionGradient(lambda,null,expFVals,fgen);
    }
    protected double addPrior(double lambda[], double grad[], double logli) {
        if (params.miscOptions.getProperty("prior", "gaussian").equalsIgnoreCase("exp")) {
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*params.invSigmaSquare;
                logli -= (lambda[f]*params.invSigmaSquare);
            }
        } else if (params.miscOptions.getProperty("prior", "gaussian").equalsIgnoreCase("laplaceApprox")) {
            for (int f = 0; f < lambda.length; f++) {
                double approxL = Math.sqrt(lambda[f]*lambda[f]+1e-3);
                grad[f] = -1*lambda[f]/approxL*params.invSigmaSquare;
                logli -= params.invSigmaSquare*approxL; 
            }
        } else  {
            for (int f = 0; f < lambda.length; f++) {
                grad[f] = -1*lambda[f]*params.invSigmaSquare;
                logli -= ((lambda[f]*lambda[f])*params.invSigmaSquare)/2;
            }
        }
        return logli;
    }
    protected double computeFunctionGradient(double lambda[], double grad[], double expFVals[], 
            FeatureGenerator fgenForExpValCompute) {    
        try {
            double logli = 0;
            if (grad != null) {
                logli = addPrior(lambda,grad,logli);
            }
            diter.startScan();
            initMDone=false;
            if (featureGenCache != null) featureGenCache.startDataScan();
            int numRecord;
            for (numRecord = 0; diter.hasNext(); numRecord++) {
                if (params.debugLvl > 1)
                    Util.printDbg("Read next seq: " + numRecord + " logli " + logli);
                if (featureGenCache != null) featureGenCache.nextDataIndex();
                logli += sumProduct(diter.next(),featureGenerator,lambda,grad,expFVals,
                        false, numRecord, fgenForExpValCompute);
            } 
            logli = finishGradCompute(grad,lambda,logli);
            if (params.debugLvl > 2) {
                for (int f = 0; f < lambda.length; f++)
                    System.out.print(lambda[f] + " ");
                System.out.println(" :x");
                for (int f = 0; f < lambda.length; f++)
                    System.out.println(f + " " + featureGenerator.featureName(f) + " " + grad[f] + " ");
                System.out.println(" :g");
            }
            if (params.debugLvl > 0) {
                if (icall == 0) {
                    Util.printDbg("Number of training records " + numRecord);
                }
                if (grad != null) Util.printDbg("Iter " + icall + " loglikelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));
            }
            return logli;
        } catch (Exception e) {
            System.out.println("Alpha-i " + alpha_Y.toString());
            System.out.println("Ri " + Ri_Y.toString());
            System.out.println("Mi " + Mi_YY.toString());
            
            e.printStackTrace();
        }
        return 0;
    }
    protected double sumProduct(DataSequence dataSeq, FeatureGenerator featureGenerator, 
            double lambda[], double grad[], double expFVals[], boolean onlyForwardPass, int numRecord, 
            FeatureGenerator fgenForExpVals) {
        if (logProcessing) {
            return sumProductLL(dataSeq,featureGenerator,lambda,grad,expFVals,onlyForwardPass,numRecord,fgenForExpVals);
        }
        boolean doScaling = params.doScaling;
        alpha_Y.assign(1);
        for (int f = 0; f < lambda.length; f++)
            ExpF[f] = 0;
        
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
            beta_Y = new DenseDoubleMatrix1D[2*dataSeq.length()];
            for (int i = 0; i < beta_Y.length; i++)
                beta_Y[i] = new DenseDoubleMatrix1D(numY);
            
            scale = new double[2*dataSeq.length()];
        }
        float instanceWt = (float) ((instanceWts!=null)?instanceWts[numRecord]:1);
        // compute beta values in a backward scan.
        // also scale beta-values to 1 to avoid numerical problems.
        scale[dataSeq.length()-1] = (doScaling)?numY:1;
        beta_Y[dataSeq.length()-1].assign(1.0/scale[dataSeq.length()-1]);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            
            // compute the Mi matrix
            initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,multFunc);
            RobustMath.Mult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
            //		Mi_YY.zMult(tmp_Y, beta_Y[i-1]);
            
            // need to scale the beta-s to avoid overflow
            scale[i-1] = doScaling?beta_Y[i-1].zSum():1;
            if ((scale[i-1] < 1) && (scale[i-1] > -1))
                scale[i-1] = 1;
            constMultiplier.multiplicator = 1.0/scale[i-1];
            beta_Y[i-1].assign(constMultiplier);
        }
        
        double thisSeqLogli = 0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMi(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,true,reuseM,initMDone);
            if (i > 0) {
                tmp_Y.assign(alpha_Y);
                RobustMath.Mult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                //		Mi_YY.zMult(tmp_Y, newAlpha_Y,1,0,true);
                newAlpha_Y.assign(Ri_Y,multFunc); 
            } else {
                newAlpha_Y.assign(Ri_Y);     
            }
            if ((grad !=null) || (expFVals!=null)) {
//          find features that fire at this position..
            fgenForExpVals.startScanFeaturesAt(dataSeq, i);
            while (fgenForExpVals.hasNext()) { 
                Feature feature = fgenForExpVals.next();
                int f = feature.index();
                
                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                    grad[f] += instanceWt*val;
                    thisSeqLogli += val*lambda[f];
                }
                if (yprev < 0) {
                    ExpF[f] += newAlpha_Y.get(yp)*val*beta_Y[i].get(yp);
                } else {
                    ExpF[f] += alpha_Y.get(yprev)*Ri_Y.get(yp)*Mi_YY.get(yprev,yp)*val*beta_Y[i].get(yp);
                }
            }
            }
            alpha_Y.assign(newAlpha_Y);
            // now scale the alpha-s to avoid overflow problems.
            constMultiplier.multiplicator = 1.0/scale[i];
            alpha_Y.assign(constMultiplier);
            
            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y.toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
        }
        double Zx = alpha_Y.zSum();
        thisSeqLogli -= log(Zx);
        // correct for the fact that alpha-s were scaled.
        for (int i = 0; i < dataSeq.length(); i++) {
            thisSeqLogli -= log(scale[i]);
        }
        // update grad.
        if (grad != null) {
        for (int f = 0; f < grad.length; f++)
            grad[f] -= instanceWt*ExpF[f]/Zx;
        }
        if (expFVals!=null) {
            for (int f = 0; f < lambda.length; f++) {
                expFVals[f] += ExpF[f]/Zx;
            }
        }
        if (params.debugLvl > 1) {
            System.out.println("Sequence "  + thisSeqLogli + " log(Zx) " + Math.log(Zx) + " Zx " + Zx);
        }
        return thisSeqLogli*instanceWt;
    }
    static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp) {
        computeLogMi(featureGen,lambda,Mi_YY,Ri_Y,takeExp,false,false);
    }
    static boolean computeLogMiInitDone(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, double DEFAULT_VALUE) {
        if ((Mi_YY==null) && (featureGen instanceof FeatureGenCache) && (DEFAULT_VALUE==0)) {
            ((FeatureGenCache)featureGen).noEdgeFeatures();
        }
        boolean mSet = false;
        while (featureGen.hasNext()) { 
            Feature feature = featureGen.next();
            int f = feature.index();
            int yp = feature.y();
            int yprev = feature.yprev();
            float val = feature.value();
            if (yprev == -1) {
                // this is a single state feature.
                
                // if default value was a negative_infinity, need to
                // reset to.
                double oldVal = Ri_Y.get(yp);
                if (oldVal == DEFAULT_VALUE)
                    oldVal = 0;
                Ri_Y.set(yp,oldVal+lambda[f]*val);
            } else {
                //if (Ri_Y.get(yp) == DEFAULT_VALUE)
                 //   Ri_Y.set(yp,0);
                if (Mi_YY != null) {
                    double oldVal = Mi_YY.get(yprev,yp);
                    if (oldVal == DEFAULT_VALUE) {
                        oldVal = 0;
                    }
                    Mi_YY.set(yprev,yp,oldVal+lambda[f]*val);
                    mSet = true;
                }
            }
        }
        return mSet;
    }
    public static double initLogMi(double defaultValue, Iterator constraints,
            DoubleMatrix2D Mi, DoubleMatrix1D Ri) {
        if (constraints != null) {
            defaultValue = RobustMath.LOG0;
            if (Mi != null) Mi.assign(defaultValue);
            Ri.assign(defaultValue);
            for (; constraints.hasNext();) {
                Constraint constraint = (Constraint)constraints.next();
                if (constraint.type() == Constraint.ALLOW_ONLY) {
                    RestrictConstraint cons = (RestrictConstraint)constraint;
                    /*
                     for (int c = cons.numAllowed()-1; c >= 0; c--) {
                     Ri.set(cons.allowed(c),0);
                     }
                     */
                    for (cons.startScan(); cons.hasNext();) {
                        cons.advance();
                        int y = cons.y();
                        int yprev = cons.yprev();
                        if (yprev < 0) {
                            Ri.set(y,0);
                        } else {
                            if (Mi != null) Mi.set(yprev,y,0);
                        }
                    }
                }
            }
        } else {
            if (Mi != null) Mi.assign(defaultValue);
            Ri.assign(defaultValue);    
        } 
        return defaultValue;
    }
    static boolean computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp,boolean reuseM, boolean initMDone) {
        
        if (reuseM && initMDone) {
            Mi_YY = null;
        } else {
            initMDone = false;
        }
        if (Mi_YY != null) Mi_YY.assign(0);
        Ri_Y.assign(0);
        initMDone = computeLogMiInitDone(featureGen,lambda,Mi_YY,Ri_Y,0);
        if (takeExp) {
            for(int r = (int) (Ri_Y.size()-1); r >= 0; r--) {
                Ri_Y.setQuick(r,expE(Ri_Y.getQuick(r)));
                if (Mi_YY != null)
                    for(int c = Mi_YY.columns()-1; c >= 0; c--) {
                        Mi_YY.setQuick(r,c,expE(Mi_YY.getQuick(r,c)));
                    }
            }
        }
        return initMDone;
    }
    public static void computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp) {
        computeLogMi(featureGen, lambda, dataSeq, i, Mi_YY, Ri_Y, takeExp,false,false);
    }
    public static boolean computeLogMi(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp, boolean reuseM, boolean initMDone) {
        featureGen.startScanFeaturesAt(dataSeq, i);
        return computeLogMi(featureGen, lambda, Mi_YY, Ri_Y, takeExp,reuseM, initMDone);
    }
    protected void allocateAlphaBeta(int newSize) {
        beta_Y = new DoubleMatrix1D[newSize];
        for (int i = 0; i < beta_Y.length; i++)
            beta_Y[i] = newLogDoubleMatrix1D(numY);
    }
    protected DoubleMatrix1D newLogDoubleMatrix1D(int numY) {
        return new DenseDoubleMatrix1D(numY);
    }
    protected DoubleMatrix2D newLogDoubleMatrix2D(int numR, int numC) {
        return new DenseDoubleMatrix2D(numR,numC);
    }
    protected double sumProductLL(DataSequence dataSeq, FeatureGenerator featureGenerator, double lambda[], 
            double grad[], double expFVals[], boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
    	
    	float instanceWt = (float) ((instanceWts!=null)?instanceWts[numRecord]:1);
    	
        for (int f = 0; f < ExpF.length; f++)
            ExpF[f] = RobustMath.LOG0;
        
        double gradThisInstance[] =grad;
        if ((instanceWt != 1) && (grad != null)) {
        	gradThisInstance = new double[grad.length];
        }
        double thisSeqLogli = sumProductInner(dataSeq,featureGenerator,lambda,gradThisInstance
                ,onlyForwardPass, numRecord, ((grad != null)||(expFVals!=null))?fgenForExpVals:null);
        
        thisSeqLogli -= lZx;
        
        // update grad.
        if (grad != null) {
            for (int f = 0; f < grad.length; f++) {
                grad[f] -= RobustMath.exp(ExpF[f]-lZx)*instanceWt;
                if (gradThisInstance != grad) {
                	grad[f] += gradThisInstance[f]*instanceWt;
                }
            }
        }
        if (expFVals!=null) {
            for (int f = 0; f < expFVals.length; f++) {
                expFVals[f] += RobustMath.exp(ExpF[f]-lZx)*instanceWt;
            }
        }
        if (params.debugLvl > 1) {
            System.out.println("Sequence "  + thisSeqLogli  + " log(Zx) " + lZx + " Zx " + Math.exp(lZx));
        }
        return (grad == null)?-lZx:thisSeqLogli * instanceWt;
    }
    
    protected void getMarginals(DataSequence dataSeq, FeatureGenerator featureGenerator, double lambda[], float nodeMargs[][], float edgeMargs[][][]) {
    	allocateAlphaBeta(2*dataSeq.length()+1);
    	beta_Y = computeBetaArray(dataSeq,lambda,featureGenerator);
    	alpha_Y.assign(0);
    	for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            
            if (i > 0) {
                tmp_Y.assign(alpha_Y);
                RobustMath.logMult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                newAlpha_Y.assign(Ri_Y,sumFunc); 
            } else {
                newAlpha_Y.assign(Ri_Y);
            }
            for (int y = 0; y < numY; y++) {
            	nodeMargs[i][y] = (float) (newAlpha_Y.get(y)+beta_Y[i].get(y));
            	if (i > 0) {
            		for (int yprev = 0; yprev < numY; yprev++) {
						edgeMargs[i][yprev][y] = (float) (alpha_Y.get(yprev)+beta_Y[i].get(y)+Ri_Y.get(y)+Mi_YY.get(yprev,y));
					}
            	}
			}
            alpha_Y.assign(newAlpha_Y);
        }
        double logZx = RobustMath.logSumExp(alpha_Y);
        for (int i = 0; i < edgeMargs.length; i++) {
			for (int y = 0; y < numY; y++) {
				nodeMargs[i][y] = (float) Math.exp(nodeMargs[i][y] - logZx);
				if (i==0) continue;
				for (int yprev = 0; yprev < edgeMargs.length; yprev++) {
					edgeMargs[i][yprev][y] = (float) Math.exp(edgeMargs[i][yprev][y]-logZx);
				}
			}
		}
    }

    protected double sumProductInner(DataSequence dataSeq, FeatureGenerator featureGenerator, double lambda[], 
            double grad[], boolean onlyForwardPass, int numRecord, FeatureGenerator fgenForExpVals) {
        if ((beta_Y == null) || (beta_Y.length < dataSeq.length())) {
           allocateAlphaBeta(2*dataSeq.length()+1);
        }
        // compute beta values in a backward scan.
        // also scale beta-values to 1 to avoid numerical problems.
        if (!onlyForwardPass) {
            beta_Y = computeBetaArray(dataSeq,lambda,featureGenerator);
        }
        alpha_Y.assign(0);
        double thisSeqLogli = 0;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            
            if (i > 0) {
                tmp_Y.assign(alpha_Y);
                RobustMath.logMult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true,edgeGen);
                newAlpha_Y.assign(Ri_Y,sumFunc); 
            } else {
                newAlpha_Y.assign(Ri_Y);
            }

            if (fgenForExpVals != null) {
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
                    if (Math.abs(val) < Double.MIN_VALUE) continue;
                    if (val < 0) {
                        System.out.println("ERROR: Cannot process negative feature values in log domains: " 
                                + "either disable the '-trainer=ll' flag or ensure feature values are not -ve");
                        continue;
                    }
                    if (yprev < 0) {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], newAlpha_Y.get(yp) + RobustMath.log(val) + beta_Y[i].get(yp));
                    } else {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[i].get(yp));
                    }
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
        lZx = RobustMath.logSumExp(alpha_Y);
        return thisSeqLogli;
    }
    protected DoubleMatrix1D[] computeBetaArray(DataSequence dataSeq, double[] lambda, FeatureGenerator featureGenerator) {
        beta_Y[dataSeq.length()-1].assign(0);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false,edgeGen);
        }
        return beta_Y;
    }
    protected boolean computeLogMiTrainMode(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp, boolean reuseM, boolean initMDone) {
        return computeLogMi(featureGen,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone) || initMDone;
    }
    static double log(double val) {
        try {
            return logE(val);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return -1*Double.MAX_VALUE;
    }
    
    static double logE(double val) throws Exception {
        double pr = Math.log(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            throw new Exception("Overflow error when taking log of " + val);
        }
        return pr;
    } 
    static double expE(double val)  {
        double pr = RobustMath.exp(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            try {
                throw new Exception("Overflow error when taking exp of " + val + "\n Try running the CRF with the following option \"trainer ll\" to perform computations in the log-space.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }
        return pr;
    }
    static double expLE(double val) {
        double pr = RobustMath.exp(val);
        if (Double.isNaN(pr) || Double.isInfinite(pr)) {
            try {
                throw new Exception("Overflow error when taking exp of " + val 
                        + " you might need to redesign feature values so as to not reach such high values");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }
        return pr;
    }
    public void addFeatureVector(DataSequence dataSeq, double[] grad) {
        for (int i = 0; i < dataSeq.length(); i++) {
            // find features that fire at this position..
                featureGenerator.startScanFeaturesAt(dataSeq, i);
                while (featureGenerator.hasNext()) { 
                    Feature feature = featureGenerator.next();
                    int f = feature.index();
                    int yp = feature.y();
                    int yprev = feature.yprev();
                    float val = feature.value();
                    
                    if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                        grad[f] += val;
                    }
                }
        }
    }
    public void reInit() {
        initMDone=false;
    }
}
