/** OptimizedSparseDoubleMatrix1D.java
 * 
 * @author Imran Mansuri
 * @since 1.2
 * @version 1.3
 */
package iitb.Utils;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;

public class OptimizedSparseDoubleMatrix1D implements TIntDoubleProcedure{

    TIntDoubleHashMap values;
    ForEachNonZeroReadOnly applyNonZero;

    public interface ForEachNonZeroReadOnly {
        public void apply(int index,double value); 
    }
    
    public OptimizedSparseDoubleMatrix1D(){
        this(0);
    }
    
    public OptimizedSparseDoubleMatrix1D(int capacity){
        applyNonZero = null;
        values = new TIntDoubleHashMap(capacity);
    }
    public void setQuick(int index, double value){
        values.put(index, value);
    }
    
    public double getQuick(int index){
        return values.get(index);
    }
    
    public void forEachNonZero(ForEachNonZeroReadOnly applyNonZero){
        this.applyNonZero = applyNonZero;
        values.forEachEntry(this);
    }

    public boolean execute(int index, double value) {
        applyNonZero.apply(index, value);
        return true;
    }
    
    public void clear(){
        values.clear();
    }
}