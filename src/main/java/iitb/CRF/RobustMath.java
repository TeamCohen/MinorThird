/** RobustMath.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.CRF;

import iitb.CRF.Trainer.SumFunc;

import java.util.TreeSet;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class RobustMath {
    public static double LOG0 = -1*Double.MAX_VALUE;
    public static double LOG2 = 0.69314718055;
    static final double MINUS_LOG_MINVAL=-1*Math.log(Double.MIN_VALUE);
    static final double MINUS_LOG_EPSILON = 30; //-1*Math.log(Double.MIN_VALUE);
    public static boolean useCache = true;
    
    public static double maxError=Double.NEGATIVE_INFINITY;
    public static double maxErrorAtVal = 0;
    public static int numInvoke=0;
    static class LogExpCache {
        static int CUT_OFF = 7;
        static int NUM_FINE = 100000;
        static int NUM_COARSE = 5000;
        static double vals[] = new double[CUT_OFF*NUM_FINE+((int)MINUS_LOG_EPSILON-CUT_OFF)*NUM_COARSE+2];
        static {
            for(int i = vals.length-1; i >= 0; vals[i--]=-1);
        }
        static double lookupAddErr(double val) {
            numInvoke++;
            double retval = lookupAdd(val);
            double actual = Math.log(Math.exp(-1*val) + 1.0);
            double err = Math.abs(retval-actual);
            if (err > maxError) {
                maxError=err;
                maxErrorAtVal=val;
                System.out.println("MaxError " + maxError + " "+val + " "+numInvoke);
            }
            return retval;
        }
        static double lookupAdd(double val) {
            if (!useCache)
                return Math.log(Math.exp(-1*val) + 1.0);
            int index = 0;
            //assert ((val < MINUS_LOG_EPSILON) && (val > 0));
            if (val < CUT_OFF) {
                index = (int)Math.rint(val*NUM_FINE);
            } else {
                index = NUM_FINE*CUT_OFF + (int)Math.rint((val-CUT_OFF)*NUM_COARSE);
            }
            if (vals[index] < 0) {
                vals[index] = Math.log(Math.exp(-1*val) + 1.0);
            }
            return vals[index];
        }
        
        //
        // Trial code for linear interpolation-based caching of values...that did not work
        static double endpts[]=new double[2];
        static double cvals[] = null;//new double[CUT_OFF*NUM_FINE+((int)MINUS_LOG_EPSILON-CUT_OFF)*NUM_COARSE+2];
        static double lookupAddWorse(double val) {
            if (!useCache)
                return Math.log(Math.exp(-1*val) + 1.0);
            int index = 0;
            //assert ((val < MINUS_LOG_EPSILON) && (val > 0));
            if (val < CUT_OFF) {
                index = (int)Math.floor(val*NUM_FINE);
            } else {
                index = NUM_FINE*CUT_OFF + (int)Math.floor((val-CUT_OFF)*NUM_COARSE);
            }
            for (int k = 0; k < 2; k++) {
                double vi=val;
                int i1=index+k;
                if (i1 < NUM_FINE*CUT_OFF) 
                    vi = i1/(double)NUM_FINE;
                else
                    vi = CUT_OFF + (i1-NUM_FINE*CUT_OFF)/(double)NUM_COARSE;
                endpts[k]=vi;
                if (cvals[i1] <= 0)
                    cvals[i1] = Math.log(Math.exp(-1*vi) + 1.0);
            }
            double a = (val-endpts[0])/(endpts[1]-endpts[0]);
            double retval = cvals[index]*a+cvals[index+1]*(1-a);
            System.out.println((retval-Math.log(Math.exp(-1*val) + 1.0))+ " "+(lookupAdd(val)-Math.log(Math.exp(-1*val) + 1.0)));
            return retval;
        }
    };
    public static double logSumExp(double v1, double v2) {
        if (Math.abs(v1-v2) < Double.MIN_VALUE)
            return v1 + LOG2;
        double vmin = Math.min(v1,v2);
        double vmax = Math.max(v1,v2);
        if ( vmax > vmin + MINUS_LOG_EPSILON ) {
            return vmax;
        } else {
            return vmax + LogExpCache.lookupAdd(vmax-vmin);
            /*
            double retval = vmax + Math.log(Math.exp(vmin-vmax) + 1.0);
            //System.out.println((vmax-vmin) + " " + (retval-vmax));
            return retval;
             */
        }
    }
    static class LogSumExp implements DoubleDoubleFunction {
        public double apply(double v1, double v2) {
            return logSumExp(v1,v2);
        }
    };
    public static LogSumExp logSumExpFunc = new LogSumExp();
    //TODO: Should TreeSet<Double> be replaced with a Trove type?
    static void addNoDups(TreeSet<Double> vec, double v) {
        Double val = new Double(v);
        if (!vec.add(val)) {
            vec.remove(val);
            addNoDups(vec, val.doubleValue()+LOG2);
        }
    }
    public static double logSumExp(TreeSet<Double> logProbVector) {
        while ( logProbVector.size() > 1 ) {
            double lp0 = logProbVector.first();
            logProbVector.remove(logProbVector.first());
            double lp1 = logProbVector.first();
            logProbVector.remove(logProbVector.first());
            addNoDups(logProbVector,logSumExp(lp0,lp1));
        }
        if (logProbVector.size() > 0)
            return ((Double)logProbVector.first()).doubleValue();
        return RobustMath.LOG0;
    }

    // matrix stuff for the older version..
    public static double logSumExp(DoubleMatrix1D logProb) {
        TreeSet<Double> logProbVector = new TreeSet<Double>();
        for ( int lpx = 0; lpx < logProb.size(); lpx++ )
            if (logProb.getQuick(lpx) != RobustMath.LOG0)
                addNoDups(logProbVector,logProb.getQuick(lpx));
        return logSumExp(logProbVector);
    }
    public static double logSumExp(double[] ds) {
        TreeSet<Double> logProbVector = new TreeSet<Double>();
        for ( int lpx = 0; lpx < ds.length; lpx++ )
            if (ds[lpx] != RobustMath.LOG0)
                addNoDups(logProbVector,ds[lpx]);
        return logSumExp(logProbVector);
    }
    static void logSumExp(DoubleMatrix1D v1, DoubleMatrix1D v2) {
        for (int i = 0; i < v1.size(); i++) {
            v1.set(i,logSumExp(v1.get(i), v2.get(i)));
        }
    }
    public static double logMinusExp(double v1, double v2)  {
        if (v1 - Double.MIN_VALUE < v2)
            return -1*MINUS_LOG_MINVAL;
//      throw new Exception("Cannot take log of negative numbers");
        double vmin = v2;
        double vmax = v1;
        if (vmax > vmin + MINUS_LOG_MINVAL) {
            return vmax;
        } else {
            return vmax + Math.log(1.0 - Math.exp(vmin - vmax));
        }
    }
    static class LogMult implements IntIntDoubleFunction {
        DoubleMatrix2D M;
        DoubleMatrix1D z;
        double lalpha;
        boolean transposeA;
        DoubleMatrix1D y;
        int cnt;
        public double apply(int i, int j, double val) {
            int r = i;
            int c = j;
            if (transposeA) {
                r = j;
                c = i;
            }
            z.set(r, RobustMath.logSumExp(z.get(r), M.get(i,j)+y.get(c)+lalpha));
            return val;
        }
    };
    static LogMult logMult = new LogMult();
    public static DoubleMatrix1D logMult(DoubleMatrix2D M, DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA) {
        // z = alpha * A * y + beta*z
        double lalpha = 0;
        if (alpha != 1)
            lalpha = Math.log(alpha);
        if (beta != 0) {
            if (beta != 1) {
                double lbeta = Math.log(beta);
                for (int i = 0; i < z.size(); z.set(i,z.get(i)+lbeta),i++);
            }
        } else {
            z.assign(RobustMath.LOG0);
        }
        // in log domain this becomes: 
        logMult.M = M;
        logMult.z = z;
        logMult.lalpha = lalpha;
        logMult.transposeA = transposeA;
        logMult.y = y;
        logMult.cnt=0;
        M.forEachNonZero(logMult);
//      System.out.println("Matrix "+M.size()+" "+M.columns()+ " "+logMult.cnt);
        return z;
    }

    public static DoubleMatrix1D logMult(DoubleMatrix2D M, DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA, EdgeGenerator edgeGen) {
        // z = alpha * A * y + beta*z
        // in log domain this becomes: 

        double lalpha = 0;
        if (alpha != 1)
            lalpha = Math.log(alpha);
        if (beta != 0) {
            if (beta != 1) {
                for (int i = 0; i < z.size(); z.set(i,z.get(i)+Math.log(beta)),i++);
            }
        } else {
            z.assign(LOG0);
        }
        for (int j = 0; j < M.columns(); j++) {
            for (int i = (edgeGen==null?j:edgeGen.first(j)); i < M.rows(); i = (edgeGen==null)?i+1:edgeGen.next(j,i)) {
                int r = i;
                int c = j;
                if (transposeA) {
                    r = j;
                    c = i;
                }
                z.setQuick(r, logSumExp(z.getQuick(r), M.getQuick(i,j)+y.get(c)+lalpha));
            }
        }
        return z;
    }
    static DoubleMatrix1D Mult(DoubleMatrix2D M, DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta, boolean transposeA, EdgeGenerator edgeGen) {
        // z = alpha * A * y + beta*z
        for (int i = 0; i < z.size(); z.set(i,z.get(i)*beta),i++);
        for (int j = 0; j < M.columns(); j++) {
            for (int i = (edgeGen==null)?j:edgeGen.first(j); i < M.rows(); i = (edgeGen==null)?i+1:edgeGen.next(j,i)) {
                int r = i;
                int c = j;
                if (transposeA) {
                    r = j;
                    c = i;
                }
                z.set(r, z.getQuick(r) + M.getQuick(i,j)*y.getQuick(c)*alpha);
            }
        }
        return z;
    }


    public static void main(String args[]) {
//      double vals[] = new double[]{10.172079, 7.452882, 2.429751, 7.452882, 10.818797, 8.573773, 19.215824};
        /*double vals[] = new double[]{2.883626, 1.670196, 0.553112, 1.670196, -0.935964, 1.864568, 2.064754};
        TreeSet vec = new TreeSet();
        double trueSum = 0;
        for (int i = 0; i < vals.length; i++) {
            addNoDups(vec,vals[i]);
            trueSum += Math.exp(vals[i]);
        }
        double sum = logSumExp(vec);
         */
        System.out.println(logSumExp(Double.parseDouble(args[0]), Double.parseDouble(args[1])));
    }
    /**
     * @param d
     * @return
     */
    public static double exp(double d) {
        if (Double.isInfinite(d) || ((d < 0) && (Math.abs(d) > MINUS_LOG_EPSILON)))
            return 0;
        //if ((d > 0) && (d < Double.MIN_VALUE))
        //    return 1;
        //System.out.println(d + " " + Math.exp(d));
        return Math.exp(d);
    }
    /**
     * @param val
     * @return
     */
    public static double log(float val) {
        return (Math.abs(val-1) < Double.MIN_VALUE)?0:Math.log(val);
    }
    public static void logMatrixMult(DoubleMatrix2D result, DoubleMatrix2D A, DoubleMatrix2D B, DoubleMatrix1D ri, boolean noMatrixMult) {
        DoubleDoubleFunction sumFunc = new SumFunc();
        if (noMatrixMult) 
            result.assign(B);
        else {
            for (int i = 0; i < A.rows(); i++) {
                for (int j = 0; j < B.columns(); j++) {
                    double value = LOG0;
                    for (int k = 0; k < B.rows(); k++) {
                        value = logSumExp(value, A.get(i,k)+B.get(k, j));
                    }
                    result.set(i, j, value);
                }
            }
        }
        for (int i = 0; i < A.rows(); i++) {
            result.viewRow(i).assign(ri, sumFunc);
        }
    }
   
};
