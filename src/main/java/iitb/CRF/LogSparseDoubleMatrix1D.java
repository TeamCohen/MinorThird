/** LogSparseDoubleMatrix1D
 * 
 *  @author Sunita Sarawagi
 *  @since 1.2
 *  @version 1.3
 */
package iitb.CRF;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.TreeSet;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.IntDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseDoubleMatrix1D;

public class LogSparseDoubleMatrix1D extends SparseDoubleMatrix1D {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -3121976644211168250L;
	static double map(double val) {
        if (val == RobustMath.LOG0)
            return 0;
        if (val == 0) 
            return Double.MIN_VALUE;
        return val;
    }
    static double reverseMap(double val) {
        if (val == 0) {
            return RobustMath.LOG0;
        }
        if (val == Double.MIN_VALUE)
            return 0;
        return val;
    }
    public LogSparseDoubleMatrix1D(int numY) {super(numY);}
    public LogSparseDoubleMatrix1D(DoubleMatrix1D doubleMatrix) {super((int) doubleMatrix.size());
    	double val;
	    for (int y = 0; y < size(); y++) {
		    if ((val = doubleMatrix.getQuick(y)) != 0) 
		        super.setQuick(y, map(val));
		}
    }
    public DoubleMatrix1D assign(double val) {
        return super.assign(map(val));
    }
    public void  set(int row, double val) {
        super.set(row,map(val));
    }
    public double  get(int row) {
        return reverseMap(super.get(row));
    }
    public double zSum() {
        TreeSet<Double> logProbVector = new TreeSet<Double>();
        // TODO
        for (int row = 0; row < size(); row++) {
            if (getQuick(row) != 0)
                RobustMath.addNoDups(logProbVector,get(row));
        }
        return RobustMath.logSumExp(logProbVector);
        
    }
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public SparseDoubleMatrix1D forEachNonZero(IntDoubleFunction func) {
        for (int y = 0; y < size(); y++) {
            if (getQuick(y) != 0) 
                setQuick(y,func.apply(y,get(y)));
        }
        return this;
    }
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public DoubleMatrix1D assign(DoubleMatrix1D v2, DoubleDoubleFunction func) {
        // TODO..
        for (int row = 0; row < size(); row++) {
            if ((v2.getQuick(row) != 0) || (getQuick(row) != 0))
                set(row,func.apply(get(row), v2.get(row)));
        }
        return this;
    }
    public boolean equals(Object arg) {
        DoubleMatrix1D mat = (DoubleMatrix1D)arg;
        for (int row = (int) (size()-1); row >= 0; row--)
            if (Math.abs(mat.get(row)-get(row))/Math.abs(mat.get(row)) > 0.0001)
                return false;
        return true;
    }
};



class LogSparseDoubleMatrix1DOld extends SparseDoubleMatrix1D {
    private static final long serialVersionUID = 1L;
    TIntDoubleHashMap elementsZ;
    static double map(double val) {
        if (val == RobustMath.LOG0)
            return 0;
        if (val == 0) 
            return Double.MIN_VALUE;
        return val;
    }
    static double reverseMap(double val) {
        if (val == 0) {
            return RobustMath.LOG0;
        }
        if (val == Double.MIN_VALUE)
            return 0;
        return val;
    }
    LogSparseDoubleMatrix1DOld(int numY) {
        super(numY);
        elementsZ = new TIntDoubleHashMap();
    }
    public DoubleMatrix1D assign(double val) {
        //super.assign(map(val));
        double newVal = map(val);
        if (newVal != 0) {
            for (int i = (int) size()-1; i >= 0; i--)
                setQuick(i,newVal);
        }
        return this;
    }
    public void  set(int row, double val) {
        setQuick(row,map(val));
    }
    public double  get(int row) {
        return reverseMap(getQuick(row));
    }
    public double zSum() {
        TreeSet<Double> logProbVector = new TreeSet<Double>();
        // TODO
        for (int row = 0; row < size(); row++) {
            if (getQuick(row) != 0)
                RobustMath.addNoDups(logProbVector,get(row));
        }
        return RobustMath.logSumExp(logProbVector);
        
    }
    /*	static class IntDoubleFunctionWrapper implements IntDoubleFunction {
     IntDoubleFunction func;
     public double apply(int row, double val) {
     return func.apply(row, reverseMap(val));
     }		
     }
     IntDoubleFunctionWrapper funcWrapper = new IntDoubleFunctionWrapper();
     public SparseDoubleMatrix1D forEachNonZero(IntDoubleFunction func) {
     funcWrapper.func = func;
     return this;
     //return super.forEachNonZero(funcWrapper);
      }
      */
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public DoubleMatrix1D forEachNonZero(IntDoubleFunction func) {
        for (int y = 0; y < size(); y++) {
            if (getQuick(y) != 0) 
                setQuick(y,func.apply(y,get(y)));
        }
        return this;
    }
    static class DoubleDoubleFunctionWrapper implements DoubleDoubleFunction {
        DoubleDoubleFunction func;
        public double apply(double val1, double val2) {
            return map(func.apply(reverseMap(val1), reverseMap(val2)));
        }		
    }
    DoubleDoubleFunctionWrapper funcWrapper = new DoubleDoubleFunctionWrapper();
    
    // WARNING: this is only correct for functions that leave the infinity unchanged.
    public DoubleMatrix1D assign(DoubleMatrix1D v2, DoubleDoubleFunction func) {
        for (TIntDoubleIterator iter = ((LogSparseDoubleMatrix1DOld)v2).elementsZ.iterator(); iter.hasNext();) {
            iter.advance();
            int row = iter.key()-1;
            set(row,func.apply(get(row), v2.get(row)));
        }
        for (TIntDoubleIterator iter = elementsZ.iterator(); iter.hasNext();) {
            iter.advance();
            int row = iter.key()-1;
            if (v2.getQuick(row)==0)
                set(row,func.apply(get(row), v2.get(row)));
        }
        
        
        return this;
    }
    
    
    /* (non-Javadoc)
     * @see cern.colt.matrix.DoubleMatrix1D#getQuick(int)
     */
    public double getQuick(int arg0) {
        return elementsZ.get(arg0+1);
    }
    /* (non-Javadoc)
     * @see cern.colt.matrix.DoubleMatrix1D#like(int)
     */
    public DoubleMatrix1D like(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see cern.colt.matrix.DoubleMatrix1D#like2D(int, int)
     */
    public DoubleMatrix2D like2D(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see cern.colt.matrix.DoubleMatrix1D#setQuick(int, double)
     */
    public void setQuick(int arg0, double arg1) {
        if (arg1 != 0)
            elementsZ.put(arg0+1,arg1);
    }
    /* (non-Javadoc)
     * @see cern.colt.matrix.DoubleMatrix1D#viewSelectionLike(int[])
     */
    protected DoubleMatrix1D viewSelectionLike(int[] arg0) {
        // TODO Auto-generated method stub
        return null;
    }
};
