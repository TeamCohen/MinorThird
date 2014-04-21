/** Soln.java
 * Created on Apr 25, 2005
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.CRF;

import java.io.Serializable;

public class Soln implements Serializable, Comparable {
    private static final long serialVersionUID = 812L;
    public float score=-1*Float.MAX_VALUE;
    public Soln prevSoln=null;
    public int label = -1;
    public int pos;
    
    public Soln(int id, int p) {label = id;pos = p;}
    public void clear() {
        score=-1*Float.MAX_VALUE;
        prevSoln=null;
    }
    boolean isClear() {
        return (score == -1*Float.MAX_VALUE);
    }
    protected void copy(Soln soln) {
        score = soln.score;
        prevSoln = soln.prevSoln;
    }
    public void copyAll(Soln soln) {
        score = soln.score;
        prevSoln = soln.prevSoln;
        pos = soln.pos;
        label = soln.label;
    }
    public int prevPos() {
        return (prevSoln == null)?-1:prevSoln.pos;
    }
    protected int prevLabel() {
        return (prevSoln == null)?-1:prevSoln.label;
    }
    public boolean equals(Soln s) {
        return (label == s.label) && (pos == s.pos) && (prevPos() == s.prevPos()) && (prevLabel() == s.prevLabel());
    }
    
    /**
     * @param prevSoln2
     * @param score2
     */
    public void setPrevSoln(Soln prevSoln, float score) {
        this.prevSoln = (prevSoln==null)?null:prevSoln.getSoln();
        this.score = score;
    }
    protected Soln getSoln() {
        return this;
    }
    public int compareTo(Object s) {
		return ((((Soln)this).score-((Soln)s).score)>0)?1:-1; 
	}
    @Override
    public String toString() {
        String str = "["+pos + ":" + prevPos()+":"+ label + "]";
        if (prevSoln != null) str += (" "+prevSoln.toString());
        return str;
    }
    public boolean equalsRecursive(Soln s) {
        if (s==null) return false;
        if ((label != s.label) || (pos != s.pos))
            return false;
        if (prevSoln==s.prevSoln) return true;
        if (prevSoln==null) return false;
        return prevSoln.equalsRecursive(s.prevSoln);
    }
   
    
};
