package iitb.Utils;

import java.util.*;

public class Counters {
    int cnts[] = null;
    int maxVals[] = null;
    BitSet fixedVals;
    public Counters(int numCtrs, int maxVal) {
	this(numCtrs, new int[numCtrs]);
	for (int i = 0; i < maxVals.length; maxVals[i++] = maxVal);
    }
    public Counters(int numCtrs, int maxVals[]) {
	cnts = new int[numCtrs];
	fixedVals = new BitSet(numCtrs+1);
	this.maxVals = maxVals; 
    }
    public void fix(int index, int val) {cnts[index ] =val; fixedVals.set(index);}
    public void clear() {
	for (int i = 0; i < cnts.length; cnts[i++] = 0);
	fixedVals.clear();
    }
    public void init(int maxVal[]) {
	clear();
	for (int i = 0; i < maxVals.length; maxVals[i] = maxVal[i],i++) {
		if (maxVal[i]==0)
			cnts[cnts.length-1]=maxVal[cnts.length-1];
	}
    }
    public void init(int maxVal) {
    	clear();
    	for (int i = 0; i < maxVals.length; maxVals[i] = maxVal,i++);
        }
    int nextNonFixed(int i) {return fixedVals.nextClearBit(i);}
    public boolean isFixed(int index) {return fixedVals.get(index);}
    public boolean advance() {
	for (int i = 0; (i < cnts.length); i++) {
	    i = nextNonFixed(i);
	    if (i < cnts.length) {
		cnts[i]++;
		if (cnts[i] < maxVals[i])
		    return true;
		else
		    if (i < cnts.length-1) cnts[i] = 0;
	    }
	}
	return false;
    }
    public boolean done() {return (cnts[cnts.length-1] >= maxVals[cnts.length-1]);}
    public int get(int index) {return cnts[index];}
    public int value(int endIndex, int startIndex) {
	int val = 0;
	for (int i = endIndex; i >= startIndex; i--) {
	    val = (val*maxVals[i] + cnts[i]);
	}
	return val;
    }
    public int value() {return value(cnts.length-1,0);}
    public void arrayCopy(int endIndex, int startIndex, int arr[]) {
	for (int i = endIndex; i >= startIndex; i--) {
	    arr[i-startIndex] = cnts[i];
	}
    }
}; 
