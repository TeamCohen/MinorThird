package iitb.Model;
import iitb.CRF.DataSequence;
/**
 *
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */ 


public class CompleteModel extends Model {
    /**
	 * 
	 */
	private static final long serialVersionUID = 9000160197804401741L;
	public CompleteModel(int nlabels) {
        super(nlabels);
        name = "Complete";
    }
    public int numStates() {return numLabels;}
    public int label(int stateNum) {return stateNum;}
    public int numEdges() {return numLabels*numLabels;}
    public int numStartStates() {return numLabels;}
    public int numEndStates() {return numLabels;}
    public int startState(int i) {
        if (i < numStartStates())
            return i;
        return -1;
    }
    public int endState(int i) {
        if (i < numEndStates())
            return i;
        return -1;
    }
    public void stateMappings(DataSequence data) throws Exception {;}
    public void stateMappings(DataSequence data, int len, int start) throws Exception{;}
    public boolean isEndState(int i) {return true;}
    public boolean isStartState(int i) {return true;}

    public class SingleEdgeIterator implements EdgeIterator {
        CompleteModel model;
        Edge edge;
        Edge edgeToReturn; 
        SingleEdgeIterator(CompleteModel m) {
            model = m;
            edge = new Edge();
            edgeToReturn = new Edge();
            start();
        };
        public void start() {
            edge.start = 0;
            edge.end = 0;
        }
        public boolean hasNext() {
            return (edge.start < model.numStates());
        }
        public Edge next() {
            edgeToReturn.start = edge.start;
            edgeToReturn.end = edge.end;
            edge.end++;
            if (edge.end == model.numStates()) {
                edge.end= 0;
                edge.start++;
            }
            return edgeToReturn;
        }
        public boolean nextIsOuter() {
            return true;
        }
    };
    public EdgeIterator edgeIterator() {
        return new SingleEdgeIterator(this);
    }
    /* (non-Javadoc)
     * @see iitb.Model.Model#innerEdgeIterator()
     */
    public EdgeIterator innerEdgeIterator() {
        return null;
    }
};

