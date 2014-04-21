/** GenericModel.java
 * 
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */
package iitb.Model;
import gnu.trove.list.array.TIntArrayList;
import iitb.CRF.DataSequence;

import java.util.Arrays;
import java.util.BitSet;
import java.util.StringTokenizer;

public class GenericModel extends Model {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6363225834538701213L;
	int _numStates;
    Edge _edges[];  // edges have to be sorted by their starting node id.
    int edgeStart[]; // the index in the edges array where edges out of node i start.
    int startStates[];
    int endStates[];
    int myLabel = -1;
    public int label(int s) {return (myLabel == -1)?s:myLabel;}
    public GenericModel(String spec, int thisLabel) throws Exception {
        super(1);
        name = spec;
        myLabel = thisLabel;
        if (spec.equals("naive"))
            spec="1-chain";
        if (spec.endsWith("-chain") || spec.endsWith("-long")) {
            StringTokenizer tok = new StringTokenizer(spec,"-");
            int len = Integer.parseInt(tok.nextToken());
            _numStates = len;
            startStates = new int[1];
            startStates[0] = 0;
            edgeStart = new int[_numStates];
            if (len == 1) {
                _edges = new Edge[1];
                _edges[0] = new Edge(0,0);
                endStates = new int[1];
                endStates[0] = 0;
                edgeStart[0] = 0;
            } else {
                _edges = new Edge[2*(len-1)];
                for (int i = 0; i < len-1; i++) {
                    _edges[2*i] = new Edge(i,i+1);
                    _edges[2*i+1] = new Edge(i,len-1);
                    edgeStart[i] = 2*i;
                }
                _edges[_edges.length-1] = new Edge(len-2,len-2);
                endStates = new int[2];
                endStates[0] = 0; // to allow one word entities.
                endStates[1] = len-1;
            }
        } else if (spec.endsWith("parallel")) {
            StringTokenizer tok = new StringTokenizer(spec,"-");
            int len = Integer.parseInt(tok.nextToken());
            _numStates = len*(len+1)/2;
            _edges = new Edge[len*(len-1)/2 + 1];
            edgeStart = new int[_numStates];
            startStates = new int[len];
            endStates = new int[len];
            int node = 0;
            int e = 0;
            for (int i = 0; i < len; i++) {
                node += i;
                for (int j = 0; j < i; j++) {
                    _edges[e++] = new Edge(node+j,node+j+1);
                    edgeStart[node+j] = e-1;
                }
                startStates[i] = node;
                endStates[i] = node + i;
            }
            node += len;
            _edges[e++] = new Edge(_numStates-2, _numStates-2);
            assert (e == _edges.length);
            assert (node == _numStates);
        } else if (spec.equals("boundary") || (spec.equals("BCEU"))) {
            // this implements a model where each label is either of a
            // Unique word (state 0) or broken into a Start state
            // (state 1) with a single token, Continuation state
            // (state 2) with multiple tokens (only state with
            // self-loop) and end state (state 3) with a single token.
            // The number of states is thus 4, and number of edges 4
            _numStates = 4;
            _edges = new Edge[4];
            _edges[0] = new Edge(1,2);
            _edges[1] = new Edge(1,3);
            _edges[2] = new Edge(2,2);
            _edges[3] = new Edge(2,3);
            startStates = new int[2];
            startStates[0] = 0;
            startStates[1] = 1;
            endStates = new int[2];
            endStates[0] = 0;
            endStates[1] = 3;
            edgeStart = new int[_numStates];
            edgeStart[0] = 4;
            edgeStart[1] = 0;
            edgeStart[2] = 2;
            edgeStart[3] = 4;
        } else if (spec.equals("BCE")) {
            // only "U" state missing, so "B" can also be an ending state.
            _numStates = 3;
            _edges = new Edge[4];
            _edges[0] = new Edge(0,1);
            _edges[1] = new Edge(0,2);
            _edges[2] = new Edge(1,1);
            _edges[3] = new Edge(1,2);
            startStates = new int[1];
            startStates[0] = 0;
            endStates = new int[2];
            endStates[0] = 0;
            endStates[1] = 2;
            edgeStart = new int[_numStates];
            edgeStart[0] = 0;
            edgeStart[1] = 2;
            edgeStart[2] = 4;
        }else if (spec.equals("BI")){
            // there is a start state with a transition to an I state. 
            // start state can also be the end state to allow for one token labels.
            _numStates = 2;
            _edges = new Edge[2];
            _edges[0] = new Edge(0,1);
            _edges[1] = new Edge(1,1);
            startStates = new int[1];
            startStates[0] = 0;
            endStates = new int[2];
            endStates[0] = 0;
            endStates[1] = 1;
            edgeStart = new int[_numStates];
            edgeStart[0] = 0;
            edgeStart[1] = 1;
        } else {
            throw new Exception("Unknown graph type: " + spec); 
        }
    }
    public void setEdgeStartPointers() {
        // sort the edges in increasing order of start node ids..
        Arrays.sort(_edges);
        edgeStart = new int[_numStates];
        for (int i = 0; i  < edgeStart.length; i++)
            edgeStart[i] = _numStates;
        for (int i = 0; i < _edges.length; i++) {
            if (edgeStart[_edges[i].start] > i)
                edgeStart[_edges[i].start] = i;
        }
    };
    public void fillStartEnd() {
        BitSet isStart = new BitSet(_numStates);
        BitSet isEnd = new BitSet(_numStates);
        isStart.flip(0,_numStates);
        isEnd.flip(0,_numStates);
        for (int i = 0; i < _edges.length; i++) {
            isStart.set(_edges[i].end,false);
            isEnd.set(_edges[i].start,false);
        }
        startStates = new int[isStart.cardinality()];
        int prev = 0;
        for (int i = 0; i < startStates.length; i++) {
            startStates[i] = isStart.nextSetBit(prev);
            prev = startStates[i]+1;
        }
        endStates = new int[isEnd.cardinality()];
        prev = 0;
        for (int i = 0; i < endStates.length; i++) {
            endStates[i] = isEnd.nextSetBit(prev);
            prev = endStates[i]+1;
        }
    }
    public void setEdges(Object[] edges) {
        _edges = new Edge[edges.length];
        for (int i = 0; i < _edges.length; i++)
            _edges[i] = (Edge)(edges[i]);
    }
    public void addEdge(int edgeNum, int st, int end) {
        _edges[edgeNum] = new Edge(st,end);
    }
    public GenericModel(int numNodes, int numEdges) throws Exception {
        super(numNodes);
        _numStates = numNodes;
        _edges = new Edge[numEdges];
    }
    public int numStates() {return _numStates;}
    public int numEdges() {return _edges.length;}
    public int numStartStates() {return startStates.length;}
    public int startState(int i) {return (i < numStartStates())?startStates[i]:-1;}
    public int numEndStates() {
        return endStates.length;
    }
    public int endState(int i) {
        return (i < numEndStates())?endStates[i]:-1;
    }
    public boolean isEndState(int i) {
        // TODO -- convert this to binary search
        for (int k = 0; k < endStates.length; k++)
            if (endStates[k] == i)
                return true;
        return false;
    }
    public boolean isStartState(int i) {
        // TODO -- convert this to binary search
        for (int k = 0; k < startStates.length; k++)
            if (startStates[k] == i)
                return true;
        return false;
    }
    public void stateMappings(DataSequence data) throws Exception {
        return;
    }
    public void stateMappings(DataSequence data, int len, int start) throws Exception {
        for (int i = 0; i < numStartStates(); i++) {
            if (pathToEnd(data,startState(i),len-1,start+1)) {
                data.set_y(start,startState(i));
                return;
            }
        }
        throw new Exception("No path in graph");
    }
    boolean pathToEnd(DataSequence data, int s, int lenLeft, int start) {
        if (lenLeft == 0) {
            return isEndState(s);
        }
        for (int e = edgeStart[s]; (e < numEdges()) && (_edges[e].start == s); e++) {
            int child = _edges[e].end;
            if (pathToEnd(data,child,lenLeft-1,start+1)) {
                data.set_y(start,child);
                return true;
            }
        }
        return false;
    }
    
    
    public int stateMappingGivenLength(int label, int len, int posFromStart) throws Exception {
        for (int i = 0; i < numStartStates(); i++) {
            int stateId = pathToEnd(startState(i),len-1,posFromStart-1);
            if (stateId >= 0) {
                if (posFromStart==0)
                    return startState(i);
                return stateId;
            }
        }
        throw new Exception("No path in graph");  
    }
    public void stateMappingGivenLength(int label, int len, TIntArrayList stateIds) 
    throws Exception {
    	stateIds.clear();
    	for (int i = 0; i < len; i++,stateIds.add(0));
        for (int i = 0; i < numStartStates(); i++) {
            if (pathToEnd(startState(i),len-1,1,stateIds)) {
                stateIds.setQuick(0,startState(i));
                return;
            }
        }
        throw new Exception("No path in graph");
    }
    boolean pathToEnd(int s, int lenLeft, int start, TIntArrayList stateIds) {
        assert (lenLeft >= 0);
        if (lenLeft == 0) {
            return isEndState(s);
        }
        for (int e = edgeStart[s]; (e < numEdges()) && (_edges[e].start == s); e++) {
            int child = _edges[e].end;
            if (pathToEnd(child,lenLeft-1,start+1,stateIds)) {
                stateIds.setQuick(start,child);
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return
     */
    private int pathToEnd(int s, int lenLeft, int posFromStart) {
        if (lenLeft == 0) {
            return isEndState(s)?s:-1;
        }
        for (int e = edgeStart[s]; (e < numEdges()) && (_edges[e].start == s); e++) {
            int child = _edges[e].end;
            int stateId = pathToEnd(child,lenLeft-1,posFromStart-1);
            if (stateId >= 0) {
                if (posFromStart == 0)
                    return child;
                return stateId;
            }
        }
        return -1;
    }

    public class GenericEdgeIterator implements EdgeIterator {
        int edgeNum;
        Edge edges[];
        GenericEdgeIterator(Edge[] e) {
            edges = e;
            start();
        };
        public void start() {
            edgeNum = 0;
        }
        public boolean hasNext() {
            return (edgeNum < edges.length);
        }
        public Edge next() {
            edgeNum++;
            return edges[edgeNum-1];
        }
        /* (non-Javadoc)
         * @see iitb.Model.EdgeIterator#nextIsOuter()
         */
        public boolean nextIsOuter() {
            return true;
        }
    };
    public EdgeIterator edgeIterator() {
        return new GenericEdgeIterator(_edges);
    }
    
};

