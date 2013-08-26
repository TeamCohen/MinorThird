/** AStarSearch.java
 * Created on Apr 16, 2005
 *
 * @author imran
 * @version 1.3
 */
package iitb.AStar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * @author imran
 *
 */
public class AStarSearch {

    
    State startState, goalState;
    TreeSet<State> stateQueue;
    long numExpansions = 0;
    boolean profiling = false, debug = false;
    private ArrayList<State> expansionList;
    
    long maxExapnsions = Long.MAX_VALUE;
    int avgStatesPerExpansion = 100;
    int lowBoundCalStep = 50;
    int curLowBoundCalStep = 0;
    int numBoundUpdate = 0;
    long maxQueueSize = Long.MAX_VALUE;
    
    double lowBoundCalStepIncrFactor = 0.5;
    
    private BoundUpdate boundUpdate = null;
    double lowerBound = Long.MIN_VALUE;
    
    public AStarSearch() {
        init();
    }
    
    /*
    public AStarSearch(BoundUpdate boundUpdate){
        this(boundUpdate, Long.MAX_VALUE, false);
    }

    public AStarSearch(long maxExpansions) {
        this(null, maxExpansions, false);
    }
    
    public AStarSearch(boolean profiling) {
        this(null, Long.MAX_VALUE, profiling);
    }
    
    public AStarSearch(long maxExpansions, boolean profiling){
        this(null, maxExpansions, profiling);
    }
    
    public AStarSearch(BoundUpdate boundUpdate,long maxExpansions, boolean profiling){
        this(boundUpdate, 100, maxExpansions, Long.MAX_VALUE, profiling);
    }
    */
    
    public AStarSearch(BoundUpdate boundUpdate,int avgStatePerExpansion, long maxExpansions, long maxQueueSize, boolean profiling){
        this.boundUpdate = boundUpdate;
        this.avgStatesPerExpansion = avgStatePerExpansion;
        this.maxExapnsions = maxExpansions;
        this.profiling = profiling;
        this.maxQueueSize = maxQueueSize;
        init();                
    }
    
    void init(){
        stateQueue = new TreeSet<State>();
    }
    
    private void initSearch(State startState) {
        this.startState = startState;
        goalState = null;
        stateQueue.clear();
        stateQueue.add(startState);
        numExpansions = 0;
        lowerBound = Long.MIN_VALUE;
        numBoundUpdate = 0;
        curLowBoundCalStep = lowBoundCalStep;
        if (profiling)
            expansionList = new ArrayList<State>();
        else
            expansionList = null;
    }
        
    public State performAStarSearch(State startState){
        initSearch(startState);        
        State curState = null, successors[], lastState = null;
        
        //A* search algorithm        
        while(numExpansions < maxExapnsions && stateQueue.size() < maxQueueSize){
            
            try{
                curState = stateQueue.first();
                stateQueue.remove(curState);
            }catch(NoSuchElementException nsee){
                System.err.println("Exception in AStar loop::" + nsee);
                curState = null;                
            }
            
            if(curState == null || curState.goalState()) //failure or sucess?
                break;
                       
            successors = curState.getSuccessors();
            numExpansions++; //only for measuring performance
            if(successors != null){
	            for(int i = 0; i < successors.length; i++){
	                if(successors[i] != null && successors[i].validState())
	                    stateQueue.add(successors[i]);
	            }
            }else if(debug)
                System.err.println("Null sucessors:" + curState);
            
            if(profiling){
                expansionList.add(curState);
                if(debug)
                    System.out.print(numExpansions + "\t" + stateQueue.size() + "\t");
            }
            
            if(shouldUpdateBound()){
                numBoundUpdate++;
                updateBound(curState);
            }            
            lastState = curState;
        }


        //System.out.print(numExpansions + "\t" + stateQueue.size() + "\t");
        if(debug && profiling)
            System.out.println("NumExpansions:" + numExpansions + " " + (Math.log(numExpansions)/Math.log(2)) + " :" + stateQueue.size() + " " + (Math.log(stateQueue.size())/Math.log(2)));
        
        if(debug && curState != null && !curState.goalState())
            System.err.println("Expansion limit reached");
        
        return (goalState = (curState != null ? curState : lastState));
    }

    /**
     * 
     */
    private void updateBound(State curState) {
        double lb = boundUpdate.getLowerBound(curState);
        if(lb > this.lowerBound){
            pruneQueue(new State(lb, 0));
            this.lowerBound = lb;                        
            curLowBoundCalStep += curLowBoundCalStep * lowBoundCalStepIncrFactor;
        }else
            curLowBoundCalStep += curLowBoundCalStep * 2 * lowBoundCalStepIncrFactor;        
    }

    private void pruneQueue(State state) {
        Collection<State> headSet = stateQueue.headSet(state);
        if(headSet.size() < stateQueue.size()){
            //System.out.println("HeadSetSize:" + headSet.size() + "Reduction in queueSize:" + (stateQueue.size() - headSet.size()));
            TreeSet<State> tempQueue = new TreeSet<State>();
            tempQueue.addAll(headSet);
            stateQueue.clear();
            stateQueue = tempQueue;
        }        
    }

    private boolean shouldUpdateBound() {
        return (boundUpdate != null && (numExpansions % curLowBoundCalStep == 0) 
                && (
                        numExpansions * avgStatesPerExpansion < stateQueue.size() || 
                        stateQueue.size() > maxQueueSize
                 )
                 );
    }

    public State getGoalState() {
        return goalState;
    }
    
    public State getStartState() {
        return startState;
    }
    
    public void setStartState(State startState) {
        this.startState = startState;
    }
        
    public ArrayList<State> getExpansionList() {
        return expansionList;
    }
    
    public long getNumExapansions(){
        return (profiling ? numExpansions : -1);
    }
    
    public boolean isProfiling() {
        return profiling;
    }
    
    public void setProfiling(boolean profiling) {        
        this.profiling = profiling;
    }
    
    public long getMaxExapnsions() {
        return maxExapnsions;
    }

    public void setMaxExapnsions(long maxExapnsions) {
        this.maxExapnsions = maxExapnsions;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public static void main(String[] args) {
    }    

    public boolean boundUpdate() {
        return boundUpdate != null;
    }
}
