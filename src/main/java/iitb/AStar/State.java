/*
 * Created on Apr 16, 2005
 *
 */
package iitb.AStar;

/**
 * @author imran
 *
 */
public class State implements Comparable{
    protected double g;
    protected double h;
    public State(double g, double h){
        this.g = g;
        this.h = h;
    }
    public boolean validState(){
        return false;
    }
    public double g(){
        return g;
    }
    public double h(){
        return h;
    }
    public double estimate(){
        return h + g;
    }
    public State[] getSuccessors(){
        return null;
    }
    public boolean goalState(){
        return false;
    }
    public int compareTo(Object o) {            
        return -1 * Double.compare(estimate(), ((State)o).estimate());//we need decreasing order of estimates
    }
    
    public String toString(){
        return "h=" + h() + " g=" + g() + " f=" + estimate();
    }
}
