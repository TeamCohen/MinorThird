/** Model.java
 *
 * This class and its children provide various graph classes. This
 * allows you to create CRFs where you can have more than one state
 * per label.
 *
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */
package iitb.Model;
import gnu.trove.list.array.TIntArrayList;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentCRF;
import iitb.CRF.SegmentDataSequence;

import java.io.Serializable;

interface EdgeIterator {
    void start();
    boolean hasNext();
    Edge next();
    boolean nextIsOuter(); // returns true if the next edge it will return is outer
};

public abstract class Model implements Serializable, SegmentCRF.ModelGraph {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1046518374934480548L;
	int numLabels;
    public String name;
    Model(int nlabels) {
	numLabels = nlabels;
    }
    public int numberOfLabels() {return numLabels;}
    public abstract int numStates();
    public abstract int label(int stateNum);
    public abstract int numEdges();
    public abstract EdgeIterator edgeIterator();
    public EdgeIterator innerEdgeIterator() {return null;}
    public EdgeIterator outerEdgeIterator() {return edgeIterator();}
    public abstract int numStartStates();
    public abstract int numEndStates();
    public abstract boolean isEndState(int i);
    public abstract boolean isStartState(int i);
    /*
     * returns the i-th edge starting from start node. returns -1 at the last edge.
     */
    public EdgeIterator nextEdgeFrom(int start) {return null;}
    /**
       return the i-th start state
     */
    public abstract int startState(int i);
    public abstract int endState(int i);
    public abstract void stateMappings(DataSequence data) throws Exception;
    public  void stateMappings(SegmentDataSequence data) throws Exception {stateMappings((DataSequence)data);}
    public abstract void stateMappings(DataSequence data, int len, int start) throws Exception;
    public int stateMappingGivenLength(int label, int len, int posFromStart) throws Exception {return label;}
    public int firstStartStateId(int label) {return label;}
    public int lastEndStateId(int label) {return label;}
    public void printGraph() {
	System.out.println("Numnodes = " + numStates() + " NumEdges " + numEdges());
	EdgeIterator iter = edgeIterator();
	for (iter.start(); iter.hasNext(); ) {
	    Edge edge = iter.next();
	    System.out.println(edge.start + "-->" + edge.end);
	}
	System.out.print("Start states");
	for (int i = 0; i< numStartStates(); i++)
	    System.out.print(" " + startState(i));
	System.out.println("");

	System.out.print("End states");
	for (int i = 0; i< numEndStates(); i++)
	    System.out.print(" " + endState(i));
	System.out.println("");
    }
    public static Model getNewBaseModel(int numLabels, String modelSpecs) throws Exception {
    	if (modelSpecs.equalsIgnoreCase("naive") || (modelSpecs.equalsIgnoreCase("semi-markov"))) {
    	    return new CompleteModel(numLabels);
    	} else if (modelSpecs.equalsIgnoreCase("noEdge")) {
    	    return new NoEdgeModel(numLabels);
    	} else if (modelSpecs.startsWith("naiveFollow")) {
    	    return new CompleteModelRestricted(modelSpecs,numLabels);
        } else if (modelSpecs.startsWith("naiveNoLoop")) {
            return new CompleteModelNoSelfLoop(modelSpecs,numLabels);
        }
    	throw new Exception("Base model can be one of {naive, noEdge, semi-Markov}");
    }
    public static Model getNewModel(int numLabels, String modelSpecs) throws Exception {
    	try {
    		return getNewBaseModel(numLabels,modelSpecs);
    	} catch (Exception e) {
    		return new NestedModel(numLabels, modelSpecs);
    	}
    }
	/**
	 * @param sequence
	 */
	public void mapStatesToLabels(SegmentDataSequence dataSeq) {
		int dataLen = dataSeq.length();
		if (dataLen == 0)
			return;
		for (int segStart = 0, segEnd=0; segStart < dataLen; segStart = segEnd+1) {
			for (segEnd=segStart; segEnd < dataLen; segEnd++) {
				if (label(dataSeq.y(segStart)) != label(dataSeq.y(segEnd))) {
				    segEnd -= 1;
				    //System.out.println("WARNING: label ending in a state not marked as a End-state");
				    break;
				}
                if (numStates(label(dataSeq.y(segStart)))==1) {
                    // naive model --- entity boundaries can only be based on change of label.
                    continue;
                }
				if (isEndState(dataSeq.y(segEnd)) && ((segEnd == dataLen-1) || isStartState(dataSeq.y(segEnd+1)))) {
					break;
				}				    
			}
			if (segEnd == dataLen) {
				if (!isEndState(dataSeq.y(dataLen-1))) {
                    System.out.println("WARNING: End state not found until the last position");
                    System.out.println(dataSeq);
                }
				segEnd = dataLen-1;
			}
			dataSeq.setSegment(segStart,segEnd,label(dataSeq.y(segStart)	));
		}
	}
	public int numStates(int label) {
        return 1;
    }
    /**
	 * @param label
	 * @param len
	 * @param stateIds
	 * @return
	 */
	 public void stateMappingGivenLength(int label, int len, TIntArrayList stateIds) 
	    throws Exception {;}
};
