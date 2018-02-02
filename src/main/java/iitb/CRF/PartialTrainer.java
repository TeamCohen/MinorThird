package iitb.CRF;

import java.util.Arrays;

import cern.colt.matrix.DoubleMatrix1D;

public class PartialTrainer extends SparseTrainer {

	protected DoubleMatrix1D constrained_alpha_Y, constrained_newAlpha_Y;
	protected DoubleMatrix1D constrained_beta_Y[];
	protected double constrainedExpF[], constrainedlZx;

	public PartialTrainer(CrfParams p) {
		super(p);
		logTrainer = true;
		logProcessing = true;	
	}
	protected void init(CRF model, DataIter data, double[] l) {
        super.init(model,data,l);
    	constrained_alpha_Y = newLogDoubleMatrix1D(numY);
    	constrained_newAlpha_Y = newLogDoubleMatrix1D(numY);
    	constrainedExpF = new double[l.length];
		logTrainer = true;
		logProcessing = true;	
	}
	
    protected DoubleMatrix1D[] computeBetaArray(DataSequence dataSeq, double[] lambda, FeatureGenerator featureGenerator) {
    	
    	if(dataSeq.y(dataSeq.length()-1) < 0) {
    		constrained_beta_Y[dataSeq.length()-1].assign(0);	
    	} else {
    		constrained_beta_Y[dataSeq.length()-1].assign(RobustMath.LOG0);
    		constrained_beta_Y[dataSeq.length()-1].set(dataSeq.y(dataSeq.length()-1), 0);
    	}

    	beta_Y[dataSeq.length()-1].assign(0);
        for (int i = dataSeq.length()-1; i > 0; i--) {
            // compute the Mi matrix
            //initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
            if(dataSeq instanceof CandSegDataSequence)
            	initMDone = SegmentTrainer.computeLogMi((CandSegDataSequence) dataSeq,i-1,i,(FeatureGeneratorNested) featureGenerator,lambda,Mi_YY,Ri_Y,reuseM,initMDone);
            else
            	initMDone = SegmentTrainer.computeLogMi(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, false, reuseM, initMDone);
            tmp_Y.assign(beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y[i-1],1,0,false);
            
            tmp_Y.assign(constrained_beta_Y[i]);
            tmp_Y.assign(Ri_Y,sumFunc);
            RobustMath.logMult(Mi_YY, tmp_Y, constrained_beta_Y[i-1],1,0,false);

            if(dataSeq.y(i-1) >= 0) {
                for(int y=0;y < numY;y++) {
                    if(y != dataSeq.y(i-1)) {
                        constrained_beta_Y[i-1].set(y, RobustMath.LOG0);
                    }
                }
            }
        }
        return beta_Y;
    }
    
    protected void allocateAlphaBeta(int newSize) {
    	super.allocateAlphaBeta(newSize);
    	constrained_beta_Y = new DoubleMatrix1D[newSize];
        for (int i = 0; i < constrained_beta_Y.length; i++)
            constrained_beta_Y[i] = newLogDoubleMatrix1D(numY);
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
        constrained_alpha_Y.assign(0);
        
        double constrainedlZx = RobustMath.LOG0;
        
        Arrays.fill(constrainedExpF, RobustMath.LOG0);
        
        double wDotF = 0;
        
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute the Mi matrix
            //initMDone = computeLogMiTrainMode(featureGenerator,lambda,dataSeq,i,Mi_YY,Ri_Y,false,reuseM,initMDone);
        	if(dataSeq instanceof CandSegDataSequence)
        		initMDone = SegmentTrainer.computeLogMi((CandSegDataSequence) dataSeq,i-1,i,(FeatureGeneratorNested) featureGenerator,lambda,Mi_YY,Ri_Y,reuseM,initMDone);
        	else
        		initMDone = SegmentTrainer.computeLogMi(featureGenerator, lambda, dataSeq, i, Mi_YY, Ri_Y, false, reuseM, initMDone);
            if (i > 0) {
                tmp_Y.assign(alpha_Y);
                RobustMath.logMult(Mi_YY, tmp_Y, newAlpha_Y,1,0,true);
                newAlpha_Y.assign(Ri_Y,sumFunc); 
                
                tmp_Y.assign(constrained_alpha_Y);
                RobustMath.logMult(Mi_YY, tmp_Y, constrained_newAlpha_Y,1,0,true);
                constrained_newAlpha_Y.assign(Ri_Y,sumFunc); 
                
            } else {
                newAlpha_Y.assign(Ri_Y);
                constrained_newAlpha_Y.assign(Ri_Y);
            }

            if(dataSeq.y(i) >= 0) {
            	double d = constrained_newAlpha_Y.get(dataSeq.y(i));
            	constrained_newAlpha_Y.assign(RobustMath.LOG0);
            	constrained_newAlpha_Y.set(dataSeq.y(i), d);
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
        
                    if ((dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1)) && (dataSeq.y(i-1)>=0)) || (yprev < 0))) {
                    	wDotF += lambda[f]*val;
                    }
                    /*
                    if ((grad != null) && (dataSeq.y(i) == yp) && (((i-1 >= 0) && (yprev == dataSeq.y(i-1))) || (yprev < 0))) {
                        grad[f] += val;
                        if (params.debugLvl > 2) {
                            System.out.println("Feature fired " + f + " " + feature);
                        } 
                    }*/
                    if (Math.abs(val) < Double.MIN_VALUE) continue;
                    if (val < 0) {
                        System.out.println("ERROR: Cannot process negative feature values in log domains: " 
                                + "either disable the '-trainer=ll' flag or ensure feature values are not -ve");
                        continue;
                    }
                    if (yprev < 0) {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], newAlpha_Y.get(yp) + RobustMath.log(val) + beta_Y[i].get(yp));
                        constrainedExpF[f] = RobustMath.logSumExp(constrainedExpF[f], constrained_newAlpha_Y.get(yp) + RobustMath.log(val) + constrained_beta_Y[i].get(yp));
                    } else {
                        ExpF[f] = RobustMath.logSumExp(ExpF[f], alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+beta_Y[i].get(yp));
                        constrainedExpF[f] = RobustMath.logSumExp(constrainedExpF[f], constrained_alpha_Y.get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(val)+constrained_beta_Y[i].get(yp));
                    }
                }
            }
            alpha_Y.assign(newAlpha_Y);
            //System.out.println("alpha_Y at i="+i+": "+alpha_Y);            		
            constrained_alpha_Y.assign(constrained_newAlpha_Y);
            
            if (params.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y.toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y[i].toString());
            }
        }
        lZx = alpha_Y.zSum();
        constrainedlZx = constrained_alpha_Y.zSum();
        if(params.debugLvl > 1) {
            System.out.println("constrainedLZx " + constrainedlZx+", wDotF "+wDotF+ " constrainedLZxBeta "+constrained_beta_Y[0].zSum());
        }
        if(grad != null) {
        	for(int i=0;i < grad.length;i++) {
        		grad[i] += RobustMath.exp(constrainedExpF[i] - constrainedlZx);
        	}
        }
        
        return constrainedlZx;
    }

	 protected void getMarginals(DataSequence dataSeq, FeatureGenerator featureGenerator, double lambda[], float nodeMargs[][], float edgeMargs[][][]) {
	    	allocateAlphaBeta(2*dataSeq.length()+1);
	    	computeBetaArray(dataSeq,lambda,featureGenerator);
	    	constrained_alpha_Y.assign(0);
	    	for (int i = 0; i < dataSeq.length(); i++) {
	            // compute the Mi matrix
	            initMDone = SegmentTrainer.computeLogMi((CandSegDataSequence) dataSeq,i-1,i,(FeatureGeneratorNested) featureGenerator,lambda,Mi_YY,Ri_Y,reuseM,initMDone);
	            if (i > 0) {
	                tmp_Y.assign(constrained_alpha_Y);
	                RobustMath.logMult(Mi_YY, tmp_Y, constrained_newAlpha_Y,1,0,true);
	                constrained_newAlpha_Y.assign(Ri_Y,sumFunc); 
	            } else {
	                constrained_newAlpha_Y.assign(Ri_Y);
	            }
	            if(dataSeq.y(i) >= 0) {
	            	double d = constrained_newAlpha_Y.get(dataSeq.y(i));
	            	constrained_newAlpha_Y.assign(RobustMath.LOG0);
	            	constrained_newAlpha_Y.set(dataSeq.y(i), d);
	            }
	            for (int y = 0; y < numY; y++) {
	            	nodeMargs[i][y] = (float) (constrained_newAlpha_Y.get(y)+constrained_beta_Y[i].get(y));
	            	if (i > 0) {
	            		for (int yprev = 0; yprev < numY; yprev++) {
							edgeMargs[i][yprev][y] = (float) (constrained_alpha_Y.get(yprev)+constrained_beta_Y[i].get(y)+Ri_Y.get(y)+Mi_YY.get(yprev,y));
						}
	            	}
				}
	            constrained_alpha_Y.assign(constrained_newAlpha_Y);
	        }
	        double logZx = constrained_alpha_Y.zSum();
	        for (int i = 0; i < edgeMargs.length; i++) {
				for (int y = 0; y < numY; y++) {
					nodeMargs[i][y] = (float) Math.exp(nodeMargs[i][y] - logZx);
					assert(!Float.isNaN(nodeMargs[i][y]));
					if (i==0) continue;
					for (int yprev = 0; yprev < numY; yprev++) {
						edgeMargs[i][yprev][y] = (float) Math.exp(edgeMargs[i][yprev][y]-logZx);
						assert(!Float.isNaN(edgeMargs[i][yprev][y]));
					}
				}
			}
	    }
}
