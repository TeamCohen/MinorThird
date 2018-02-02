/** SegmentAStar.java
 * 
 * @author imran
 * @since 1.2
 * @version 1.3
 */
package iitb.CRF;

import gnu.trove.TIntArrayList;
import iitb.AStar.AStarSearch;
import iitb.AStar.BoundUpdate;
import iitb.AStar.State;
import iitb.CRF.SegmentViterbi.LabelConstraints;
import iitb.CRF.SparseViterbi.Iter;
import iitb.Utils.OptimizedSparseDoubleMatrix1D;
import iitb.Utils.OptimizedSparseDoubleMatrix2D;
import iitb.Utils.StaticObjectHeap;

import java.util.ArrayList;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class SegmentAStar extends AStarInference {
    private static final long serialVersionUID = 8124L;
    
    double delta = 0.001;
    double lowerBound = 0;
    
    int forwardViterbiBeamSize = 1; //can set to any value
    int backwardViterbiBeamSize = 1; //one is sufficient
    
    DoubleMatrix2D[][] Mi;
    DoubleMatrix1D Ri[][];
    SparseViterbi.Context context[];

    SegmentViterbi segmentViterbi;
    SegmentViterbi backwardSegmentViterbi;
    SegmentViterbi.LabelConstraints labelConstraints = null;    
    SegmentIterForward iter;
    
    OptimizedSparseMatrixMapper stateGenerator;//not used
    ArrayList<SegmentState> states;
    CloneableIntSet nextLabelsOnPath = null;
    int succEll, succPos;
    Soln lbSoln;

    OptimizedSparseDoubleMatrix2D optimizedSparseMi[][];
    StaticHeapOptimizedSparseDoubleMatrix1D staticHeapOptSparseDoubleMatrix1D;
    StaticHeapOptimizedSparseDoubleMatrix2D staticHeapOptSparseDoubleMatrix2D;
 
    boolean sparseMatrix = false;
    
    double lambda[] = null;        
    public SegmentAStar(SegmentCRF model, int bs) {
        super();
        beamsize = bs;
        this.model = model;
        getParameters();
        aStar = new AStarSearch((boundUpdate?new AStarBoundUpdate() : null), avgStatesPerExpansion, maxExpansions, queueSizeLimit, debug);
        segmentViterbi = new SegmentViterbi(model, forwardViterbiBeamSize);
        backwardSegmentViterbi = new SegmentViterbi(model, backwardViterbiBeamSize);
        iter = new SegmentIterForward(segmentViterbi.new SegmentIter());
        states = new ArrayList<SegmentState>();
                       
        if(sparseMatrix){
            staticHeapOptSparseDoubleMatrix1D = new StaticHeapOptimizedSparseDoubleMatrix1D(0);
            staticHeapOptSparseDoubleMatrix2D = new StaticHeapOptimizedSparseDoubleMatrix2D(0);
            stateGenerator = new OptimizedSparseMatrixMapper();
        }        
    }
    
    protected void getParameters(){
        super.getParameters();
        sparseMatrix = (Boolean.valueOf(model.params.miscOptions.getProperty("sparse", "false"))).booleanValue();
    }

    public double bestLabelSequence(CandSegDataSequence dataSeq, double lambda[]) {
        double corrScore = aStarSearch(dataSeq, lambda, false);
        int pos;
        //check whether the search succeeded or not
        int segmentCount = 0;//profiling
        double score = goalState.g();
        if (goalState != null && goalState.goalState()) {
            do {
                pos = goalState.pos;
       			dataSeq.setSegment(goalState.prevPos()+1,goalState.pos,goalState.y);
                goalState = goalState.predecessor;
                segmentCount++;
            } while (goalState != null && goalState.pos >= 0);
            assert (pos == 0);
            return score;
        } else {
            //Error! Failure in A* search, finding solution using Viterbi
            Soln soln = getViterbiSoln(dataSeq, lambda, (SegmentState)goalState);
            if(soln == null || (lbSoln != null && Double.compare(soln.score, lbSoln.score) < 0)){
                soln = lbSoln;
            }
            if(soln != null){
                score = soln.score;
                Soln ybest = soln;
                while (ybest != null) {
                    pos = ybest.pos;
        			dataSeq.setSegment(ybest.prevPos()+1,ybest.pos,ybest.label);
                    ybest = ybest.prevSoln;
                }
            }                
        }
        return score;
    }

    public double aStarSearch(DataSequence dataSeq, double lambda[],
            boolean calcCorrectScore) {
        return aStarSearch(dataSeq, lambda, calcCorrectScore, true);
    }
    
    public double aStarSearch(DataSequence dataSeq, double lambda[],
            boolean calcCorrectScore, boolean checkConstraints) {
        labelConstraints = null;
        if(checkConstraints)
            labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        this.dataSeq = dataSeq;
        this.lambda = lambda;
        
        //init the forward-edge iterator
        iter.init(dataSeq);
        
        //get upper and lower bound for solution
        if (!getSolutionBound(dataSeq, lambda)) {
            goalState = null;
            return 0;
        }
        
        //perform AStar search
        goalState = (AStarState) aStar.performAStarSearch(getStartState());
        return (goalState != null ? goalState.g() : 0);
    }
    
    
    private boolean getSolutionBound(DataSequence dataSeq, double[] lambda) {
        cacheMis(dataSeq, lambda);

        //get lower bound using constrained viterbi
        Soln soln = null;
        segmentViterbi.viterbiSearch(dataSeq, lambda, Mi, Ri, true, false);
        soln = segmentViterbi.getBestSoln(0);
        lbSoln = copyViterbiSolution(soln);
        lowerBound = (lbSoln != null ? lbSoln.score : 0);//assume that viterbi returns some solution

        //get upper bound using backward-viterbi, to be used for heuristics
        backwardSegmentViterbi.viterbiSearchBackward(dataSeq, lambda,Mi, Ri, false);                       
        ubSoln = backwardSegmentViterbi.getBestSoln(0);
        upperBound = (ubSoln != null ? ubSoln.score : 0);
        
        //store context from backward viterbi for heuristic calculations
        context = backwardSegmentViterbi.context;        
        return true;
    }
    
    private Soln copyViterbiSolution(Soln soln) {
        Soln tempSoln = null, copiedSoln = null, lastSoln = null;
        while(soln != null){
            tempSoln = new Soln(soln.label, soln.pos);
            tempSoln.score = soln.score;
            if(lastSoln != null)
                lastSoln.prevSoln = tempSoln;
            if(copiedSoln == null){
                copiedSoln = tempSoln;                
            }    
            soln = soln.prevSoln;
            lastSoln = tempSoln;
        }        
        return copiedSoln;
    }

    private AStarState getStartState(){
        return new SegmentState(-1, 0, -1, upperBound, 0, null, (labelConstraints != null ? new CloneableIntSet() : null));
    }
    
    Soln getViterbiSoln(DataSequence dataSeq, double lambda[], SegmentState curState){
        Soln soln = getSoln(curState);
        cacheMis(dataSeq, lambda);
        segmentViterbi.viterbiSearch(dataSeq, lambda, Mi, Ri, soln, true, false);
        return segmentViterbi.getBestSoln(0);
    }
    
    private void cacheMis(DataSequence dataSeq, double[] lambda2) {
        segmentViterbi.cacheMis(dataSeq, lambda);
        Mi = segmentViterbi.getMis();
        Ri = segmentViterbi.getRis();
        if(sparseMatrix){
            createOptimizedSparseMatrices(Mi);
        }    
    }
    
    private void createOptimizedSparseMatrices(DoubleMatrix2D Mi[][]) {
        optimizedSparseMi = new OptimizedSparseDoubleMatrix2D[Mi.length][];
        for(int i = 0; i < Mi.length; i++){
            optimizedSparseMi[i] = new OptimizedSparseDoubleMatrix2D[Mi[i].length];
            for(int j = 0; j < Mi[i].length; j++){
                optimizedSparseMi[i][j] = (OptimizedSparseDoubleMatrix2D) staticHeapOptSparseDoubleMatrix2D.getObject();
                stateGenerator.init(optimizedSparseMi[i][j]);
                Mi[i][j].forEachNonZero(stateGenerator);
            }
        }
        
    }    

    private Soln getSoln(SegmentState curState) {
        Soln nextSoln = null, curSoln = null, soln = null;
        while(curState!= null && curState.pos >=0){
            curSoln = new Soln(curState.y, curState.pos);
            curSoln.score =  (float) curState.g();
            if(nextSoln != null)
                nextSoln.setPrevSoln(curSoln, nextSoln.score);
            else{
                soln = curSoln;
            }    
            nextSoln = curSoln;
            curState = (SegmentState) curState.predecessor;
        }
        return soln;
    }
    
    class StaticHeapOptimizedSparseDoubleMatrix1D extends StaticObjectHeap{
        public StaticHeapOptimizedSparseDoubleMatrix1D(int initCapacity) {
            super(initCapacity);
        }
        
        protected Object newObject() {
            return new OptimizedSparseDoubleMatrix1D();
        }

        protected Object getObject() {
            return getFreeObject();
        }
    }
    
    class StaticHeapOptimizedSparseDoubleMatrix2D extends StaticObjectHeap{
        public StaticHeapOptimizedSparseDoubleMatrix2D(int initCapacity) {
            super(initCapacity);
        }
        
        protected Object newObject() {
            return new OptimizedSparseDoubleMatrix2D();
        }

        protected Object getObject() {
            return getFreeObject();
        }
    }
    
    class SegmentIterForward extends SparseViterbi.Iter {
        int nc;
        DataSequence dataSeq;
        Iter iter;
        int startPos, index;        
        TIntArrayList segments[];
        
        public SegmentIterForward(Iter iter){
            segmentViterbi.super();
            this.iter = iter;
        }
        
        public void init(DataSequence dataSeq){
            this.dataSeq = dataSeq;
            segments = new TIntArrayList[dataSeq.length()];
            for(int j = dataSeq.length()-1; j >= 0; j--)
                segments[j] = new TIntArrayList();
            cacheEdges();
        }
        
        private void cacheEdges() {
            int ell = 0;
            for(int i = dataSeq.length()-1; i >= 0; i--){
                iter.start(i, dataSeq);
                while((ell = iter.nextEll(i)) > 0){
                    if(i - ell >= 0){
                        segments[i - ell].add(ell);
                    }else{
                        segments[segments.length-1].add(ell);                        
                    }    
                }
            }            
        }
        
        protected void start(int i, DataSequence dataSeq) {
            startPos = i;
            index = (i == -1 ? segments[segments.length - 1].size() : segments[i].size());
        }
        
        protected int nextEll(int i) {
            if(i == -1)
                return index > 0 ? segments[segments.length-1].get(--index) : -1 ;
                return (index > 0 ? segments[i].get(--index) : -1 );
        }        
    }
    
    class OptimizedSparseMatrixMapper implements IntIntDoubleFunction{
        
        private OptimizedSparseDoubleMatrix2D sparse2D;
        private OptimizedSparseDoubleMatrix1D sparse1D;

        public OptimizedSparseMatrixMapper(){
        }
        
        public void init(OptimizedSparseDoubleMatrix2D sparse2D){
            this.sparse2D = sparse2D;
        }
        
        public double apply(int yp, int yi, double val) {
            if((sparse1D = sparse2D.getRow(yp)) == null){
                sparse1D = (OptimizedSparseDoubleMatrix1D) staticHeapOptSparseDoubleMatrix1D.getObject();
                sparse1D.clear();
                sparse2D.setRow(yp, sparse1D);
            }
            sparse1D.setQuick(yi, val);
            return val;
        }        
    }
    int stateCount = 0;

    class AStarBoundUpdate implements BoundUpdate{

        public double getLowerBound(State curState) {
            Soln soln = getViterbiSoln(dataSeq, lambda, (SegmentState)curState);
            
            if(soln != null && soln.score > lowerBound)
                lowerBound = soln.score;
            return lowerBound - 2 * delta;
        }
    }
    class SegmentState extends AStarState implements OptimizedSparseDoubleMatrix1D.ForEachNonZeroReadOnly{
        int id;
        int ell;//pos is used as end of a segment whose "length" "ell"
        public SegmentState(int pos, int ell, int label, double h, double g, AStarState predecessor, CloneableIntSet labelsOnPath) {
            super(pos, label, h, g, predecessor, null);
            this.ell = ell;
            id = stateCount++;
            this.labelsOnPath = labelsOnPath;
        }
        
        State[] generateSucessors() {
            SegmentState successors[];

            iter.start(pos, dataSeq);
            states.clear();
            
            while((succEll = iter.nextEll(pos)) > 0){
                succPos = pos + succEll;
                if(pos != -1){
                    if(sparseMatrix)
	                    createSuccessors(optimizedSparseMi[succPos][succEll].getRow(y));	                    
	                else
	                    createSuccessors(Mi[succPos][succEll].viewRow(y));
                }else
                    createSuccessors(Ri[succPos][succEll]);
            }
            successors = new SegmentState[states.size()];
            for(int i = 0; i < states.size(); i++)
                successors[i] = states.get(i);
            return successors;
        }

        private void createSuccessors(DoubleMatrix1D miRow){
            SegmentState successor;
            double succG, succH;
            
            if(labelConstraints != null){
                nextLabelsOnPath = (CloneableIntSet) labelsOnPath.clone();
                if(y != -1 && labelConstraints.conflicting(y))
                    nextLabelsOnPath.add(y);
            }    
            for(int yi = (int) (miRow.size() - 1); yi >= 0; yi--){
                double val = miRow.getQuick(yi);
                if(val == 0 ||  
                        (labelConstraints != null && prevPos() >= 0 && !labelConstraints.valid(labelsOnPath, yi, y))){
                                        continue;
                }
                succG = (pos == -1 ? val + g : val+Ri[succPos][succEll].get(yi) + g);
                succH = context[succPos + 1].getEntry(yi).solns[0].score;
                if(Double.compare((succG + succH + delta), lowerBound) >= 0){
                    successor = new SegmentState(succPos, succEll, yi, succH, succG, this, nextLabelsOnPath);
                    states.add(successor);
                }
            }
        }
        
        private void createSuccessors(OptimizedSparseDoubleMatrix1D optimizedMiRow){
            SegmentState successor;
            double succG, succH;
            if(labelConstraints != null){
                nextLabelsOnPath = (CloneableIntSet) labelsOnPath.clone();
                if(y != -1 && labelConstraints.conflicting(y))
                    nextLabelsOnPath.add(y);
            }
            optimizedMiRow.forEachNonZero(this);
        }
        
        public void apply(int yi, double val){
            SegmentState successor;
            if((labelConstraints != null && prevPos() >= 0 && !labelConstraints.valid(labelsOnPath, yi, y))){
                double succG = (pos == -1 ? val + g : val+Ri[succPos][succEll].get(yi) + g);
                double succH = context[succPos + 1].getEntry(yi).solns[0].score;
                return;
            }
            
            double succG = (pos == -1 ? val + g : val+Ri[succPos][succEll].get(yi) + g);
            double succH = context[succPos + 1].getEntry(yi).solns[0].score;
            if(Double.compare((succG + succH + delta), lowerBound) >= 0){
                successor = new SegmentState(succPos, succEll, yi, succH, succG, this, nextLabelsOnPath);
                states.add(successor);
            }            
        }

        
        public boolean goalState(){
            return pos == (dataSeq.length() - 1); 
        }
        
        public String toString() {
            return  id + ">Pos:"
            + pos
            + " ell="
            + ell 
            + " Label="
            + y
            + " score="
            + (g-(predecessor != null ? predecessor.g() : 0))
            + " g="
            + g                        	
            + " h="
            + h
            + " f="
            + (h + g)
            + (predecessor != null ? " Par=(" + predecessor.pos + ", "
                    + predecessor.y + ")" : "");
        }
    }
    public static void main(String[] args) {
    }
}
