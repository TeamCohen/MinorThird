/** NestedModel.java
 * 
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */
package iitb.Model;
import gnu.trove.list.array.TIntArrayList;
import iitb.CRF.DataSequence;
import iitb.CRF.SegmentDataSequence;

import java.io.Serializable;
import java.util.StringTokenizer;

public class NestedModel extends Model {
    /**
	 * 
	 */
	private static final long serialVersionUID = -1954054779451180599L;
	int _numStates;
    int _numEdges;
    int nodeOffsets[]; // the number of states in the labels before this.
    Model inner[];
    Model outer;
    int startStates[];
    int endStates[];

    public NestedModel(int num, String specs) throws Exception {
        super(num);
        name = "Nested";
        nodeOffsets = new int[numLabels];
        inner = new Model[numLabels];

        StringTokenizer start = new StringTokenizer(specs, ",");
        assert start.hasMoreTokens();
        outer = Model.getNewBaseModel(numLabels, (String)start.nextToken());
        String commonStruct = null;
        for(int i=0 ; i<numLabels ; i++) {
            String thisStruct = commonStruct;
            if (thisStruct == null) {
                assert start.hasMoreTokens();
                thisStruct = start.nextToken();
                if (thisStruct.endsWith("*")) {
                    thisStruct = thisStruct.substring(0,thisStruct.length()-1);
                    commonStruct = thisStruct;
                }
            }
            inner[i] = new GenericModel(thisStruct,i);
        }
        _numEdges = 0;
        _numStates = 0;
        for (int l = 0; l < numLabels; l++) {
            nodeOffsets[l] += _numStates;
            _numStates += inner[l].numStates();
            _numEdges += inner[l].numEdges();
        }
        EdgeIterator outerIter = outer.edgeIterator();
        while (outerIter.hasNext()) {
            Edge e = outerIter.next();
            _numEdges += inner[e.end].numStartStates()*inner[e.start].numEndStates();
        }

        int numStart = 0;
        for (int i = 0; i < outer.numStartStates(); i++) {
            numStart += inner[outer.startState(i)].numStartStates();
        }
        startStates = new int[numStart];
        int index = 0;
        for (int i = 0; i < outer.numStartStates(); i++) {
            for (int j = 0; j < inner[outer.startState(i)].numStartStates(); j++) {
                startStates[index++] = inner[outer.startState(i)].startState(j) + nodeOffsets[outer.startState(i)];
            }
        }

        int numEnd = 0;
        for (int i = 0; i < outer.numEndStates(); i++) {
            numEnd += inner[outer.endState(i)].numEndStates();
        }
        endStates = new int[numEnd];
        index = 0;
        for (int i = 0; i < outer.numEndStates(); i++) {
            for (int j = 0; j < inner[outer.endState(i)].numEndStates(); j++) {
                endStates[index++] = inner[outer.endState(i)].endState(j) + nodeOffsets[outer.endState(i)];
            }
        }

    }    
    public int numStates() {return _numStates;}
    public int numEdges() {return _numEdges;}
    public int firstStartStateId(int label) {return nodeOffsets[label]+inner[label].startState(0);}
    public int lastEndStateId(int label) {return nodeOffsets[label]+inner[label].endState(inner[label].numEndStates()-1);}
    public int label(int stateNum) {
        assert (stateNum >= 0) && (stateNum < numStates());
        // TODO -- convert to binary scan.
        for (int i = 0; i < nodeOffsets.length; i++) {
            if (stateNum < nodeOffsets[i])
                return i-1;
        }
        return nodeOffsets.length-1;
    }
    public int numStartStates() {
        return startStates.length;
    } 
    public int numEndStates() {
        return endStates.length;
    }
    public int startState(int i) {
        return ((i < numStartStates())?startStates[i]:-1);
    }
    public int endState(int i) {
        return ((i < numEndStates())?endStates[i]:-1);// endStates[i];
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
    public void stateMappings(DataSequence data, int len, int start) throws Exception 
    {
        assert false;
    }	
    public void stateMappings(SegmentDataSequence data) throws Exception {
        if (data.length() == 0)
            return;
        for (int lstart = 0; lstart < data.length();) {
            int lend = data.getSegmentEnd(lstart)+1;
            if (lend == 0) {
                throw new Exception("Invalid segment end value");
            }
            int label = data.y(lstart);
            if (label >= 0) {
                inner[label].stateMappings(data,lend-lstart, lstart);
                for (int k = lstart; k < lend; k++) {
                    data.set_y(k, nodeOffsets[label]+data.y(k));
                }
            }
            lstart=lend;
        }
    }
    public void stateMappings(DataSequence data) throws Exception {
        if (data.length() == 0)
            return;
        for (int lstart = 0; lstart < data.length();) {
            int lend = lstart+1;
            for (;(lend < data.length()) && (data.y(lend) == data.y(lstart)); lend++);
            int label = data.y(lstart);
            if (label >= 0) {
                inner[label].stateMappings(data,lend-lstart, lstart);
                for (int k = lstart; k < lend; k++) {
                    data.set_y(k, nodeOffsets[label]+data.y(k));
                }
            }
            lstart=lend;
        }
    }
    public int stateMappingGivenLength(int label, int len, int posFromStart) 
    throws Exception {
        return inner[label].stateMappingGivenLength(label,len,posFromStart)+nodeOffsets[label];
    }
    public void stateMappingGivenLength(int label, int len, TIntArrayList stateIds) 
    throws Exception {
        inner[label].stateMappingGivenLength(label,len,stateIds);
        for(int i = stateIds.size()-1; i >= 0; i--) {
            stateIds.setQuick(i, stateIds.getQuick(i)+ nodeOffsets[label]);
        }
    }
    public class NestedEdgeIterator implements EdgeIterator, Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 7023718575291288030L;
		NestedModel model;
        int label;
        Edge edge;
        EdgeIterator edgeIter[], outerEdgeIter;
        Edge outerEdge;
        boolean outerEdgesSent;
        int index1, index2;
        boolean sendOuter, sendInner;
        int edgesFrom=-1;

        NestedEdgeIterator(NestedModel m) {
            this(m,true,true);
        }
        NestedEdgeIterator(NestedModel m, boolean sendOuter) {
            this(m,sendOuter,true);
        }
        NestedEdgeIterator(NestedModel m, boolean sendOuter, boolean sendInner) {
            model = m;
            edge = new Edge();
            this.sendInner=sendInner;
            if (sendInner) {
            edgeIter = new EdgeIterator[model.numLabels];
                for (int l = 0; l < model.numLabels; l++) {
                    edgeIter[l] = model.inner[l].edgeIterator();
                }
            } else {
                edgeIter = new EdgeIterator[0];
            }
            outerEdgeIter = model.outer.edgeIterator();
            this.sendOuter = sendOuter;
            start();
           
        }
        NestedEdgeIterator(NestedModel m, int edgesFrom) {
            model = m;
            edge = new Edge();
            edgeIter = new EdgeIterator[0];
            int l = model.label(edgesFrom);
            outerEdgeIter = model.outer.edgeIterator();
            this.sendOuter = true;
            this.sendInner=true;
            this.edgesFrom=edgesFrom;
            start();
            index1=l;
            label=model.numLabels;
        }
        public void start() {
            label = 0;
            if (sendInner==false) {
                label=model.numLabels;
            }
            for (int l = 0; l < edgeIter.length; l++) {
                edgeIter[l].start();
            } 
            outerEdgeIter.start();

            outerEdge = outerEdgeIter.next();

            //check for the null edge
            if ((outerEdge == null) || !sendOuter)
                outerEdgesSent = true;
            else
                outerEdgesSent = false;
            index1 = index2 = 0;
        }
        public boolean hasNext() {
            return (label < model.numLabels) || !outerEdgesSent;
        }
        public Edge nextOuterEdge() {
            edge.start = model.inner[outerEdge.start].endState(index1) + model.nodeOffsets[outerEdge.start];
            edge.end = model.inner[outerEdge.end].startState(index2) + model.nodeOffsets[outerEdge.end];
            index2++;
            if (index2 == model.inner[outerEdge.end].numStartStates()) {
                index2 = 0;
                index1++;
                if (index1  == model.inner[outerEdge.start].numEndStates()) {
                    if ((outerEdgeIter.hasNext()) && (edgesFrom==-1)) {
                        outerEdge = outerEdgeIter.next();
                        index1 = index2 = 0;
                    } else {
                        outerEdgesSent = true;
                    }
                }
            }
            return edge;
        }
        public Edge nextInnerEdge() {
            Edge edgeToRet = edgeIter[label].next();
            edge.start = edgeToRet.start;
            edge.end = edgeToRet.end;
            assert (edge != null);
            assert (model.nodeOffsets != null);
            assert (label < model.nodeOffsets.length);
            edge.start += model.nodeOffsets[label];
            edge.end += model.nodeOffsets[label];
            if (!edgeIter[label].hasNext())
                label++;
            return edge;
        }
        public Edge next() {
            if (!nextIsOuter()) {
                return nextInnerEdge();
            } else {
                return nextOuterEdge();
            }
        }
        /* (non-Javadoc)
         * @see iitb.Model.EdgeIterator#nextIsOuter()
         */
        public boolean nextIsOuter() {
            return (label >= model.numLabels);
        }
    };
    public EdgeIterator edgeIterator() {
        return new NestedEdgeIterator(this);
    }

    public static void main(String args[]) {
        try {
            args = new String[]{"3", "naiveNoLoop:2,boundary,boundary,naive"};
            System.out.println(args[0]);
            System.out.println(args[1]);
            Model model = new NestedModel(Integer.parseInt(args[0]), args[1]);
            System.out.println(model.numStates());
            System.out.println(model.numEdges());
            System.out.println(model.numStartStates());
            System.out.println(model.numEndStates());
            EdgeIterator edgeIter = model.edgeIterator();
            //	EdgeIterator edgeIter2 = model.edgeIterator();
            for (int edgeNum = 0; edgeIter.hasNext(); edgeNum++) {
                boolean edgeIsOuter = edgeIter.nextIsOuter();
                Edge e = edgeIter.next();
                System.out.println(e.start + "("+ model.label(e.start) + ")" + " -> " + e.end + ":" + edgeIsOuter+ ";");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            //	    System.out.println(e.getStackTrace().getLineNumber());
        }
    }
    /* (non-Javadoc)
     * @see iitb.Model.Model#innerEdgeIterator()
     */
    public EdgeIterator innerEdgeIterator() {
        return new NestedEdgeIterator(this,false);
    }
    public EdgeIterator outerEdgeIterator() {
        return new NestedEdgeIterator(this,true,false);
    }
    @Override
    public EdgeIterator nextEdgeFrom(int start) {
        return new NestedEdgeIterator(this,start);
    }
    @Override
    public int numStates(int label) {
        return inner[label].numStates();
    }
};

