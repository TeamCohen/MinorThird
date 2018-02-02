/** Viterbi.java
 *
 * Viterbi search.
 *
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */ 
package iitb.CRF;

import java.io.Serializable;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class Viterbi implements Serializable {
    private static final long serialVersionUID = 8122L;
    protected CRF model;
    protected int beamsize;
    public Viterbi(CRF model, int bs) {
        this.model = model;
        beamsize = bs;
        if (model != null && model.params.miscOptions.getProperty("beamSize") != null)
            beamsize = Integer.parseInt(model.params.miscOptions.getProperty("beamSize"));
    }
    protected class Entry {
        public Soln solns[]; // TODO.
        boolean valid=true;
        protected Entry() {}
        protected Entry(int beamsize, int id, int pos) {
            solns = new Soln[beamsize];
            for (int i = 0; i < solns.length; i++)
                solns[i] = newSoln(id, pos);
        }
        protected Soln newSoln(int label, int pos) {
            return new Soln(label,pos);
        }
        protected void clear() {
            valid = false;
            for (int i = 0; i < solns.length; i++)
                solns[i].clear();
        }
        public int size() {return solns.length;}
        public Soln get(int i) {return solns[i];}
        protected void insert(int i, float score, Soln prev) {
            Soln saved = solns[size()-1];
            for (int k = size()-1; k > i; k--) {
                //solns[k].copy(solns[k-1]);
                solns[k] = solns[k-1];
            }
            solns[i] = saved;
            solns[i].setPrevSoln(prev,score);
        }
        protected void add(Entry e, float thisScore) {
            assert(valid);
            if (e == null) {
                add(thisScore);
                return;
            }
            // the soln within each entry are sorted.
            int insertPos = 0;
            for (int i = 0; (i < e.size()) && (insertPos < size()); i++) {
                float score = e.get(i).score + thisScore;
                insertPos = findInsert(insertPos, score, e.get(i));
            }
            
            //print()
        }
        protected int findInsert(int insertPos, float score, Soln prev) {
            for (; insertPos < size(); insertPos++) {
                if (score >= get(insertPos).score) {
                    insert(insertPos, score, prev);
                    insertPos++;
                    break;
                }
            }
            return insertPos;
        }
        protected void add(float thisScore) {
            findInsert(0, thisScore, null);
        }
        public int numSolns() {
            for (int i = 0; i < solns.length; i++)
                if (solns[i].isClear())
                    return i;
            return size();
        }
        public void setValid() {valid=true;}
        void print() {
            String str = "";
            for (int i = 0; i < size(); i++)
                str += ("["+i + " " + solns[i].score + " i:" + solns[i].pos + " y:" + solns[i].label+"]");
            System.out.println(str);
        }
        
        public String toString(){
            assert(solns != null && solns[0] != null);
            String toString = "";
            toString += "[" + solns[0].pos + " " + solns[0].label + " " + solns[0].score;
            if(solns[0].prevSoln != null)
                toString += " : " + solns[0].prevSoln.pos + " " + solns[0].prevSoln.label + " " + solns[0].prevSoln.score;
            toString += "]";
            return toString;
        }
        public void sortEntries() {
        }
    };
    
    Entry winningLabel[][];
    protected Entry finalSoln;
    protected DoubleMatrix2D Mi;
    protected DoubleMatrix1D Ri;

    void allocateScratch(int numY) {
        Mi = new DenseDoubleMatrix2D(numY,numY);
        Ri = new DenseDoubleMatrix1D(numY);
        winningLabel = new Entry[numY][];
        finalSoln = new Entry(beamsize,0,0);
    }
    protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        Trainer.computeLogMi(model.featureGenerator,lambda,dataSeq,i,Mi,Ri,false);
    }
    double fillArray(DataSequence dataSeq, double lambda[], boolean calcScore) {
        double corrScore = 0;
        int numY = model.numY;
        for (int i = 0; i < dataSeq.length(); i++) {
            // compute Mi.
            computeLogMi(dataSeq,i,1,lambda);
            for (int yi = 0; yi < numY; yi++) {
                winningLabel[yi][i].clear();
                winningLabel[yi][i].valid = true;
            }
            for (int yi = model.edgeGen.firstY(i); yi < numY; yi = model.edgeGen.nextY(yi,i)) {
                if (i > 0) {
                    for (int yp = model.edgeGen.first(yi); yp < numY; yp = model.edgeGen.next(yi,yp)) {
                        double val = Mi.get(yp,yi)+Ri.get(yi);
                        winningLabel[yi][i].add(winningLabel[yp][i-1], (float)val);
                    }
                } else {
                    winningLabel[yi][i].add((float)Ri.get(yi));
                }
            }
            if (calcScore)
                corrScore += (Ri.get(dataSeq.y(i)) + ((i > 0)?Mi.get(dataSeq.y(i-1),dataSeq.y(i)):0));
        }
        return corrScore;
    }

    public double viterbiSearchBackward(DataSequence dataSeq, double lambda[], DoubleMatrix2D Mis[], DoubleMatrix1D Ris[], boolean calcCorrectScore) {
        if ((Mi == null)||(winningLabel==null)) {
            allocateScratch(model.numY);
        }
        if ((winningLabel[0] == null) || (winningLabel[0].length < dataSeq.length())) {
            for (int yi = 0; yi < winningLabel.length; yi++) {
                winningLabel[yi] = new Entry[dataSeq.length()];
                for (int l = 0; l < dataSeq.length(); l++)
                    winningLabel[yi][l] = new Entry(beamsize, yi, l);
            }
        }
        Entry firstEntries[] = new Entry[model.numY];
        for (int yi = 0; yi < winningLabel.length; yi++) {
            firstEntries[yi] = new Entry(1, yi, 0);	
        }
        double corrScore = fillArrayBackward(dataSeq, lambda,firstEntries, Mis, Ris, calcCorrectScore);
        
        finalSoln.clear();
        finalSoln.valid = true;
        for (int yi = 0; yi < model.numY; yi++) {
            finalSoln.add(firstEntries[yi], 0);
        }
        return corrScore;
    }

    double fillArrayBackward(DataSequence dataSeq, double lambda[], Entry firstEntries[], DoubleMatrix2D Mis[], DoubleMatrix1D Ris[], boolean calcScore) {
        double corrScore = 0;
        int numY = model.numY;

        for (int i = dataSeq.length() - 1; i >= 0; i--) {
            for (int yi = 0; yi < numY; yi++) {
                winningLabel[yi][i].clear();
                winningLabel[yi][i].valid = true;
                if(i == dataSeq.length() - 1)
                    winningLabel[yi][i].add(0);
            }
        }    	
        for (int i = dataSeq.length() - 1; i >= 0; i--) {
            // compute Mi.
            computeLogMi(dataSeq,i,1,lambda);
            Mis[i].assign(Mi);
            Ris[i].assign(Ri);
            if(i == 0)
                break;
            for (int yi = model.edgeGen.firstY(i); yi < numY; yi = model.edgeGen.nextY(yi,i)) {
                for (int yp = model.edgeGen.first(yi); yp < numY; yp = model.edgeGen.next(yi,yp)){
                    double val = Mi.get(yp,yi)+Ri.get(yi);
                    winningLabel[yp][i-1].add(winningLabel[yi][i], (float)val);
                }
            }
            if (calcScore)
                corrScore += (Ri.get(dataSeq.y(i)) + ((i > 0)?Mi.get(dataSeq.y(i-1),dataSeq.y(i)):0));            
        }
        for(int yi = 0; yi < numY; yi++){
            firstEntries[yi].clear();
            firstEntries[yi].valid = true;
            firstEntries[yi].add(winningLabel[yi][0], (float)Ri.get(yi));
        }
        
        return corrScore;
    }
    
    protected void setSegment(DataSequence dataSeq, int prevPos, int pos, int label) {
        dataSeq.set_y(pos, label);
    }
    public double bestLabelSequence(DataSequence dataSeq, double lambda[]) {
        double corrScore = viterbiSearch(dataSeq, lambda,false);
        if(model.params.debugLvl > 1)
            System.out.println("Score of best sequence "+finalSoln.get(0).score + " corrScore " + corrScore);
        /*if (finalSoln.get(0).prevSoln == null) {
            viterbiSearch(dataSeq, lambda,false);
            assert(false);
        }
        */
        assignLabels(dataSeq);   
        return finalSoln.get(0).score;
    }
    
    protected void assignLabels(DataSequence dataSeq) {
        Soln ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        int pos=-1;
        assert(ybest.pos == dataSeq.length()-1);
        while (ybest != null) {
            pos = ybest.pos;
            setSegment(dataSeq,ybest.prevPos(),ybest.pos, ybest.label);
            ybest = ybest.prevSoln;
        }
        assert(pos>=0);
    }
    
    public double viterbiSearch(DataSequence dataSeq, double lambda[], boolean calcCorrectScore) {
        if ((Mi == null)||(winningLabel==null)) {
            allocateScratch(model.numY);
        }
        if ((winningLabel[0] == null) || (winningLabel[0].length < dataSeq.length())) {
            for (int yi = 0; yi < winningLabel.length; yi++) {
                winningLabel[yi] = new Entry[dataSeq.length()];
                for (int l = 0; l < dataSeq.length(); l++)
                    winningLabel[yi][l] = new Entry((l==0)?1:beamsize, yi, l);
            }
        }
        
        double corrScore = fillArray(dataSeq, lambda,calcCorrectScore);
        
        finalSoln.clear();
        finalSoln.valid = true;
        if (dataSeq.length() > 0)
            for (int yi = 0; yi < model.numY; yi++) {
                finalSoln.add(winningLabel[yi][dataSeq.length()-1], 0);
            }
        return corrScore;
    }   
    
    public int numSolutions() {return finalSoln.numSolns();}
    public Soln getBestSoln(int k) {
        return finalSoln.get(k).prevSoln;
    }
    protected LabelSequence newLabelSequence(int len){
        return new LabelSequence(len);
    }
    /**
     * @param dataSeq
     * @param lambda
     * @param numLabelSeqs
     * @param scores
     * @return
     */
    public LabelSequence[] topKLabelSequences(DataSequence dataSeq, double[] lambda, int numLabelSeqs, boolean getScores) {
        viterbiSearch(dataSeq, lambda,false);
        double lZx=0;
        if (getScores) {
            lZx = model.getLogZx(dataSeq);
        }
        int numSols = Math.min(finalSoln.numSolns(), numLabelSeqs);
        LabelSequence labelSequences[] = new LabelSequence[numSols];
        for (int k = numSols-1; k >= 0; k--) {
            Soln ybest = finalSoln.get(k);
            labelSequences[k] = newLabelSequence(dataSeq.length());
            labelSequences[k].score = ybest.score;
            if (getScores) labelSequences[k].score = Math.exp((double)ybest.score-lZx);
            ybest = ybest.prevSoln;
            while (ybest != null) {	
                labelSequences[k].add(ybest.prevPos(), ybest.pos, ybest.label);
                ybest = ybest.prevSoln;
            }
            labelSequences[k].doneAdd();
        }
        return labelSequences;
    }
};
