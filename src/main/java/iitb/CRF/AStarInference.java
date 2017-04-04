/** AStarInference.java
 * 
 * @author Imran Mansuri
 * @since 1.2
 * @version 1.3
 * 
 * A* search 
 */
package iitb.CRF;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntHashSet;
import iitb.AStar.AStarSearch;
import iitb.AStar.State;
import iitb.CRF.Viterbi.Entry;
import iitb.Model.Model;

import java.io.Serializable;
import java.util.BitSet;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class AStarInference implements Serializable {
    private static final long serialVersionUID = 81236L;

    CRF model;

    Viterbi viterbi;

    int beamsize;

    DataSequence dataSeq;

    //TODO this used to be a generic
    //TIntObjectHashMap<BitSet> conflictingLabels;

    TIntObjectHashMap conflictingLabels;

    Model graphModel;

    DoubleMatrix2D Mi[];

    DoubleMatrix1D Ri[];

    boolean constraintCheck = true;

    Soln ubSoln; //Upper bound solution, without constraints

    int path[];

    float[] ubScores;

    float ubScore[][];

    float upperBound = 0;

    AStarState goalState;

    AStarSearch aStar;
    
    long maxExpansions = Long.MAX_VALUE;
    long queueSizeLimit = Long.MAX_VALUE;
    int avgStatesPerExpansion = 50;
    boolean boundUpdate = false;
    int forwardViterbiBeamSize = 1;
    int backwardViterbiBeamSize = 1;
    
    boolean debug = false;

    protected class CloneableIntSet extends TIntHashSet implements Cloneable{
        public Object clone() {

            return super.clone();

            /*try {
                return super.clone();
            } catch (CloneNotSupportedException cnse) {
                return null; // it's supported
            }*/
        }
    }
    
    protected AStarInference() {
    }
    
    public AStarInference(CRF model, int bs) {
        this(model, bs, null, null);
        getParameters();
    }

    protected void getParameters() {
        if(model.params.miscOptions.getProperty("maxExpansions") != null){
            try{
                maxExpansions = Long.parseLong(model.params.miscOptions.getProperty("maxExpansions"));                
            }catch(NumberFormatException nfe){}
        }

        if(model.params.miscOptions.getProperty("queueSizeLimit") != null){
            try{
                queueSizeLimit = Long.parseLong(model.params.miscOptions.getProperty("queueSizeLimit"));
            }catch(NumberFormatException nfe){}
        }

        if(model.params.miscOptions.getProperty("avgStatesPerExpansion") != null){
            try{
                avgStatesPerExpansion = Integer.parseInt(model.params.miscOptions.getProperty("avgStatesPerExpansions"));
            }catch(NumberFormatException nfe){}
        }

        if(model.params.miscOptions.getProperty("boundUpdate") != null){
            try{
                boundUpdate = Boolean.valueOf(model.params.miscOptions.getProperty("boundUpdate")).booleanValue();
            }catch(Exception nfe){}
        }

        if(model.params.miscOptions.getProperty("forwardViterbiBeamSize") != null){
            try{
                forwardViterbiBeamSize = Integer.parseInt(model.params.miscOptions.getProperty("forwardViterbiBeamSize"));
            }catch(NumberFormatException nfe){}
        }

        if(model.params.miscOptions.getProperty("backwardViterbiBeamSize") != null){
            try{
                backwardViterbiBeamSize = Integer.parseInt(model.params.miscOptions.getProperty("backwardViterbiBeamSize"));
            }catch(NumberFormatException nfe){}
        }
        
        if (model.params.miscOptions.getProperty("beamSize") != null)
        {
            try{
                beamsize = Integer.parseInt(model.params.miscOptions
                        .getProperty("beamSize"));
                }catch(NumberFormatException nfe){}
        }
            
        
        if(model.params.debugLvl > 2)
            debug = true;
    }

    public AStarInference(CRF model, int bs, TIntObjectHashMap confLabelMap,
            Model graphModel) {
        this.model = model;
        this.graphModel = graphModel;
        beamsize = bs;
        getParameters();
        aStar = new AStarSearch(null, avgStatesPerExpansion, maxExpansions, queueSizeLimit, debug);
        viterbi = new Viterbi(model, forwardViterbiBeamSize);
        Mi = new DenseDoubleMatrix2D[0];
        Ri = new DenseDoubleMatrix1D[0];
        initConflictLables(confLabelMap);        
    }

    private void initConflictLables(TIntObjectHashMap confLabelMap) {
        if (confLabelMap == null || confLabelMap.size() == 0)
            return;
        conflictingLabels = new TIntObjectHashMap();
        int keys[] = confLabelMap.keys();
        TIntHashSet labelSet;
        BitSet bitSet;
        for (int i = 0; i < keys.length; i++) {
            labelSet = (TIntHashSet) confLabelMap.get(keys[i]);
            bitSet = new BitSet();
            int labelArray[] = labelSet.toArray();
            for (int j = 0; j < labelArray.length; j++) {
                bitSet.set(labelArray[j]);
            }
            conflictingLabels.put(keys[i], bitSet);
        }
    }

    int nonMatchCount = 0;

    /*
     * Equivalent of viterbiSearch
     */
    public void bestLabelSequence(DataSequence dataSeq, double lambda[]) {
        double corrScore = aStarSearch(dataSeq, lambda, true);
        //constraintCheck = false;
        int pos;
        //check whether the search succeeded or not
        boolean nonMatch = false;
        int lastPos = 0, lastLabel = 0;

        if (goalState != null) {
            do {
                pos = goalState.pos;
                dataSeq.set_y(pos, goalState.y);
                goalState = goalState.predecessor;
            } while (goalState != null && goalState.pos >= 0);
            assert (pos == 0);
        } else {
            System.err.println("Error! Failure in A* search");
        }
        return;
    }

    Entry winningLabel[][];

    public double aStarSearch(DataSequence dataSeq, double lambda[],
            boolean calcCorrectScore) {
        this.dataSeq = dataSeq;

        //      allocate data structures
        allocateScratch(model.numY, dataSeq.length());

        if (!getUpperBoundSolution(dataSeq, lambda)) {
            goalState = null;
            return 0;
        }

        //perform AStar search
        goalState = (AStarState) aStar.performAStarSearch(getStartState());
        return goalState.g();
        //return 0;
    }

    float scores[];

    Soln lastUbSoln;

    private boolean getUpperBoundSolution(DataSequence dataSeq, double[] lambda) {
        int seqLength = dataSeq.length(), pos = 0;

        viterbi.viterbiSearchBackward(dataSeq, lambda, Mi, Ri, false);
        winningLabel = viterbi.winningLabel;
        ubScore = new float[dataSeq.length()][model.numY];
        for (pos = 0; pos < dataSeq.length(); pos++) {
            for (int y = 0; y < model.numY; y++) {
                ubScore[pos][y] = winningLabel[y][pos].get(0).score;
            }
        }
        return true;
    }

    /*
     * Store all Mi, Ri matrices
     */
    void fillArray(DataSequence dataSeq, double lambda[], boolean calcScore) {
        int numY = model.numY;
        computeMi(dataSeq, lambda);
    }

    private void computeMi(DataSequence dataSeq, double lambda[]) {
        int seqLength = dataSeq.length();
        for (int pos = 0; pos < seqLength; pos++) {
            // compute Mi.
            Trainer.computeLogMi(model.featureGenerator, lambda, dataSeq, pos,
                    Mi[pos], Ri[pos], false);
        }
    }

    void allocateScratch(int numY, int seqLength) {
        if (Mi.length < seqLength) { //what if Mi.length > seqLength, I hope
            DoubleMatrix2D tempMi[] = Mi;
            DoubleMatrix1D tempRi[] = Ri;
            int i;
            Mi = new DenseDoubleMatrix2D[seqLength];
            Ri = new DenseDoubleMatrix1D[seqLength];
            for (i = 0; i < tempMi.length; i++) {
                Mi[i] = tempMi[i];
                Ri[i] = tempRi[i];
            }
            for (; i < seqLength; i++) {
                Mi[i] = new DenseDoubleMatrix2D(numY, numY);
                Ri[i] = new DenseDoubleMatrix1D(numY);
            }
        }
    }

    private AStarState getStartState() {
        return (conflictingLabels == null ? new AStarState(-1, -1, upperBound,
                0, null, null) : new AStarState(-1, -1, upperBound, 0, null,
                new BitSet()));
    }

    int numSolutions() {
        return 1;
    }//bs not supported for now

    Soln getBestSoln(int k) {
        return null;
    }

    public TIntObjectHashMap getConflictingLabels() {
        return conflictingLabels;
    }

    public void setConflictingLabels(TIntObjectHashMap conflictingLabels) {
        this.conflictingLabels = conflictingLabels;
    }

    class AStarState extends State {
        int pos;

        int y;

        boolean valid = true;

        AStarState predecessor;

        BitSet assignedLabels;
        protected CloneableIntSet labelsOnPath;

        public AStarState(int pos, int label, double h, double g,
                AStarState predecessor, BitSet assignedLabels) {
            super(g, h);
            this.pos = pos;
            this.predecessor = predecessor;
            this.y = label;
            this.assignedLabels = assignedLabels;
            checkValidity();
        }

        public double estimate() {
            return h + g;
        }

        public State[] getSuccessors() {
            return generateSucessors();
        }

        State[] generateSucessors() {
            AStarState[] successors = new AStarState[model.numY];
            int nextPos = pos + 1;
            //for (int nextY = model.edgeGen.first(label); nextY < model.numY;
            // nextY = model.edgeGen.next(nextY,label)){
            BitSet succLabels = null;
            if (assignedLabels != null) {
                succLabels = (BitSet) assignedLabels.clone();
                if (y >= 0) {
                    succLabels.set(graphModel.label(y));
                }
            }
            for (int nextY = 0; nextY < model.numY; nextY++) {
                double succScore = 0;
                succScore += (nextPos > 0 ? Mi[nextPos].get(y, nextY) : 0);
                succScore += Ri[nextPos].get(nextY) + g;
                if (assignedLabels != null) {
                    if (!conflicting(succLabels, nextY))
                        successors[nextY] = new AStarState(nextPos, nextY,
                                ubScore[nextPos][nextY], succScore, this,
                                succLabels);//todo
                    else
                        successors[nextY] = null;//invalid state
                } else
                    successors[nextY] = new AStarState(nextPos, nextY,
                            ubScore[nextPos][nextY], succScore, this, null);
            }
            return successors;
        }

        public double g() {
            return g;
        }

        public double h() {
            return h;
        }

        public boolean goalState() {
            return pos == (dataSeq.length() - 1);
        }

        public boolean validState() {
            return valid;
        }

        public void checkValidity() {
            valid = !conflicting(assignedLabels, y);
        }

        public boolean conflicting(BitSet assignedLabels, int stateId) {
            if (constraintCheck && assignedLabels != null
                    && conflictingLabels.get(graphModel.label(stateId)) != null) {
                return (assignedLabels.intersects((BitSet) conflictingLabels
                        .get(graphModel.label(stateId))));
            }
            return false;
        }

        public String toString() {
            return "Pos:"
                    + pos
                    + " Y="
                    + graphModel.label(y)
                    + " h="
                    + h
                    + " g="
                    + g
                    + " f="
                    + (h + g)
                    + (predecessor != null ? " Par=(" + predecessor.pos + ", "
                            + predecessor.y + ")" : "");
        }

        public int prevPos() {
            if(predecessor == null)
                return -1;
            else
                return predecessor.pos;
        }

    }

};