/*
 * Created on Oct 28, 2005
 *   This implements the exponentiated gradient optimization algorithm
 *   for training graphical models as described in 
 *   @incollection{NIPS2005_621,
 *      author = {Peter L. {Bartlett} and Michael {Collins} and Ben {Taskar} and David {McAllester}},
 *      title = {Exponentiated Gradient Algorithms for Large-margin Structured Classification},
 *      booktitle = {Advances in Neural Information Processing Systems 17},
 *      year = {2005}}
 *      
 */
package iitb.CRF;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
public class ExponentiatedGradientTrainer extends Trainer {
    double C = 1000;
    double eta = 1/C; // the learning rate.
    double zt[]; // as in the paper
    double wt[]; // learnt parameter values
    DoubleMatrix2D fMi_YY;
    Viterbi viterbi;
    static class AddConstant implements DoubleFunction {
        public double addend = 1.0;
        public double apply(double a) {return a + addend;}
    };
    static AddConstant addConstant = new AddConstant();
    public ExponentiatedGradientTrainer(CrfParams p) {
        super(p);
    }
    @Override
    protected void init(CRF model, DataIter data, double[] l) {
        super.init(model, data, l);
        zt = new double[numF];
        wt = lambda; // this is important to retain since lambda is passed to CRF.
        fMi_YY = new DenseDoubleMatrix2D(Mi_YY.rows(),Mi_YY.columns());
        logProcessing = true;
        viterbi = new LossAugmentedViterbi(model,1);
    }
    protected void doTrain() {
        icall=0;
        params.invSigmaSquare = 0;
        for (int j = 0 ; j < zt.length ; j++) {
            zt[j] = getInitValue();
        }
        double prevObjValue = Double.MAX_VALUE;
        do {
            double f = computeFunctionGradient(zt,gradLogli); 
            for (int j = 0 ; j < zt.length ; j++) {
                wt[j] = (C*gradLogli[j]);
                zt[j] += wt[j];
            } 
            if ((evaluator != null) && (evaluator.evaluate() == false))
                break;
            icall += 1;
            
            // calculate the value of the objective 0.5*wnorm + C\sum_i\xi
            double objective = 0.5*normSquare(wt);
            double sumXi = 0;
            diter.startScan();
            for (int numRecord = 0; diter.hasNext(); numRecord++) {
                double corrScore = viterbi.viterbiSearch(diter.next(),wt,true);
                double topViterbiScore = viterbi.getBestSoln(0).score;
                if (corrScore < topViterbiScore)
                    sumXi += C*(topViterbiScore-corrScore);
            }
            objective += sumXi;
            System.out.println("Primal objective " + objective + " norm^sq " + normSquare(wt) + " sumXi " + sumXi);
 //           if (prevObjValue-objective < params.epsForConvergence) break;
            prevObjValue = objective;
        } while (icall <= params.maxIters);
    }
    private double normSquare(double[] vec) {
        double v = norm(vec);
        return v*v;
    }
    protected boolean computeLogMiTrainMode(FeatureGenerator featureGen, double lambda[], 
            DataSequence dataSeq, int i, 
            DoubleMatrix2D Mi_YY,
            DoubleMatrix1D Ri_Y, boolean takeExp, boolean reuseM, boolean initMDone) {
        boolean retVal = Trainer.computeLogMi(featureGen,lambda,dataSeq,i,fMi_YY,Ri_Y,false,reuseM,initMDone);
        int iterNum = icall;
        for (int c = 0; c < Ri_Y.size();c++) {
            int loss =  (dataSeq.y(i)!=c)?1:0;
            Ri_Y.set(c,eta*C*(Ri_Y.get(c)+iterNum*loss));
            /*
             * don't need loss for this since it is already accounted for in Ri_Y
             */
            if (i > 0) {
                for (int r = 0; r < Mi_YY.rows(); r++) {
                    Mi_YY.set(r,c,eta*C*(fMi_YY.get(r,c)));
                }
            }
        }
        return retVal;
    }
    class LossAugmentedViterbi extends Viterbi {
        /**
		 * 
		 */
		private static final long serialVersionUID = 3806943864517186914L;
		LossAugmentedViterbi(CRF model, int bs) {
            super(model, bs);
        }
        protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
            Trainer.computeLogMi(model.featureGenerator,lambda,dataSeq,i,Mi,Ri,false);
            for (int c = 0; c < Ri.size();c++) {
               Ri.set(c,Ri.get(c)+((dataSeq.y(i)!=c)?1:0));
        }
    }
    }
}
