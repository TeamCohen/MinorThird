/** SegmentViterbi.java
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.CRF;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public class SegmentViterbi extends SparseViterbi {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3055942733801323491L;
	protected SegmentCRF segmentModel;
    protected FeatureGeneratorNested featureGenNested;
    public static class LabelConstraints  {
        private static final long serialVersionUID = 1L;
        protected ConstraintDisallowedPairs disallowedPairs;
        protected ConstraintDisallowedPairsExtended disallowedPairsExt;
        
        public class Intersects implements TIntProcedure {
            public int label;
            public int prevLabel;
            public boolean execute(int arg0) {
                return !disallowedPairs.conflictingPair(label,arg0,prevLabel);
            }
        }
        protected Intersects intersectTest = new Intersects();
        /**
         * @param pairs
         */
        public LabelConstraints(ConstraintDisallowedPairs pairs) {
            disallowedPairs = pairs;
            if (disallowedPairs instanceof ConstraintDisallowedPairsExtended)
                disallowedPairsExt = (ConstraintDisallowedPairsExtended) disallowedPairs;
            else
                disallowedPairsExt = null;
        }
        public LabelConstraints(LabelConstraints labelCons) {
            this(labelCons.disallowedPairs);
        }
        /**
         * @param set
         * @param prevLabel
         * @param i
         * @return
         */
        public boolean valid(TIntHashSet set, int label, int prevLabel) {
            if (!conflicting(label))
                return true;
            if (disallowedPairs.conflictingPair(label,prevLabel,-1))
                return false;
            intersectTest.label = label;
            intersectTest.prevLabel = prevLabel;
            return set.forEach(intersectTest);
        }
        public boolean valid(TIntHashSet set, int yp, TIntHashSet set2) {
            intersectTest.label = yp;
            intersectTest.prevLabel = -1;
            boolean isValid = !conflicting(yp) || set2.forEach(intersectTest);
            for(TIntIterator iter = set.iterator(); iter.hasNext();) {
                if (!isValid) return false;
                intersectTest.label = iter.next();
                isValid = set2.forEach(intersectTest);
            }
            return isValid;
        }
        public boolean match(TIntHashSet set1, TIntHashSet set2) {
            return set1.equals(set2);
        }
        public TIntHashSet formPathLabels(TIntHashSet set, int label, TIntHashSet labelsOnPath) {
            if (!conflicting(label))
                return set;
            labelsOnPath.clear();
            labelsOnPath.add(canonicalId(label));
            for(TIntIterator iter = set.iterator(); iter.hasNext();  labelsOnPath.add(iter.next()));
            //            labelsOnPath.addAll(set.toArray());
            return labelsOnPath;
        }
        public int canonicalId(int label) {
            return disallowedPairsExt!=null?disallowedPairsExt.canonicalId(label):label;
        }
        /**
         * @param dataSeq
         * @return
         */
        public static LabelConstraints checkConstraints(CandSegDataSequence dataSeq, LabelConstraints labelCons) {
            Iterator constraints = dataSeq.constraints(-1,dataSeq.length());
            if (constraints != null) {
                for (; constraints.hasNext();) {
                    Constraint constraint = (Constraint)constraints.next();
                    if (constraint.type() == Constraint.PAIR_DISALLOW) {
                        if (labelCons != null) {
                            labelCons.init((ConstraintDisallowedPairs)constraint);
                            return labelCons;
                        } else
                            return new LabelConstraints((ConstraintDisallowedPairs)constraint);
                    }
                }
            }
            return null;
        }
        public TIntHashSet formPreviousLabel(TIntHashSet prevLabelsOnPath, TIntHashSet labelsOnPath, int prevLabel) {
            labelsOnPath.clear();
            for(TIntIterator iter = prevLabelsOnPath.iterator(); iter.hasNext();  labelsOnPath.add(iter.next()));
            if (conflicting(prevLabel))
                labelsOnPath.add(canonicalId(prevLabel));
            return labelsOnPath;
        }    
        protected void init(ConstraintDisallowedPairs pairs) {
            disallowedPairs = pairs;
        }
        /**
         * @param label
         * @return
         */
        public boolean conflicting(int label) {
            return disallowedPairs.conflicting(label);
        }
        public int countConflicting(int numY) {
            TIntHashSet maxSet = new TIntHashSet();
            for (int i = 0; i < numY; i++) {
                if (conflicting(i))
                    maxSet.add(canonicalId(i));
            }
            return Math.min(1 << maxSet.size(), 20);
        }
        public boolean contained(TIntHashSet labelsOnPath, TIntHashSet prevLabels) {
            if (labelsOnPath == null) return true;
            for(TIntIterator iter = labelsOnPath.iterator(); iter.hasNext();) {
                int thisL = iter.next();
                if (prevLabels != null && prevLabels.contains(thisL)) continue;
                return false;
            }
            return true;
        }
    }

    LabelConstraints labelConstraints=null;
    public class SolnWithLabelsOnPath extends Soln {
        public void clear() {
            super.clear();
            labelsOnPath.clear();
        }
        protected void copy(Soln soln) {
            super.copy(soln);
            labelsOnPath.clear();
            //            labelsOnPath.addAll(((SolnWithLabelsOnPath)soln).labelsOnPath.toArray());
            for(TIntIterator iter = ((SolnWithLabelsOnPath)soln).labelsOnPath.iterator(); iter.hasNext();  labelsOnPath.add(iter.next()));
        }
        private static final long serialVersionUID = 1L;
        public TIntHashSet labelsOnPath;
        /**
         * @param id
         * @param p
         */
        SolnWithLabelsOnPath(int id, int p) {
            super(id, p);
            labelsOnPath = new TIntHashSet();
        }
        public void setPrevSoln(Soln prevSoln, float score) {
            super.setPrevSoln(prevSoln,score);
            if ((prevSoln != null) && (labelConstraints != null)) {
                labelConstraints.formPreviousLabel(((SolnWithLabelsOnPath)prevSoln).labelsOnPath, labelsOnPath, prevSoln.label);
            }
        }
    }
    public class EntryForLabelConstraints extends Entry {
        TIntHashSet tmpLabels = new TIntHashSet();
        /**
         * @param beamsize
         * @param id
         * @param pos
         */
        protected EntryForLabelConstraints(int beamsize, int id, int pos, int numStatComb) {
            super();
            if (beamsize > 1)
                throw new UnsupportedOperationException();
            solns = new Soln[beamsize*numStatComb];
            for (int i = 0; i < solns.length; i++)
                solns[i] = new SolnWithLabelsOnPath(id, pos);
        }
        protected int findInsert(int insertPos, float score, Soln prev) {
            SolnWithLabelsOnPath prevSolL = (SolnWithLabelsOnPath)prev;
            // this solution conflicts with the label in this state..
            if ((prev != null) && labelConstraints!=null 
                    && !labelConstraints.valid(prevSolL.labelsOnPath,get(0).label, prev.label)) {
                return insertPos;
            }
            TIntHashSet prevLabels = ((prevSolL != null && labelConstraints != null)?labelConstraints.formPreviousLabel(prevSolL.labelsOnPath, tmpLabels, prevSolL.label):null);
            for (insertPos=0; insertPos < size(); insertPos++) {
             // if a better solution with a less restrictive condition exists..do not keep this one.
                if (score <= get(insertPos).score) {
                    if (prev == null || labelConstraints == null)
                        return 0;
                    SolnWithLabelsOnPath thisSolL = (SolnWithLabelsOnPath) get(insertPos);
                    if (labelConstraints.contained(thisSolL.labelsOnPath, prevLabels))
                        return 0;
                }
            }
            int minPos = -1; float minScore = Float.MAX_VALUE;
            for (insertPos=0; insertPos < size(); insertPos++) {
                if (score > get(insertPos).score) {
                   if (minScore > get(insertPos).score) {
                       minScore = get(insertPos).score;
                       minPos = insertPos;
                   }
                   if (labelConstraints != null && labelConstraints.contained(prevLabels, ((SolnWithLabelsOnPath) get(insertPos)).labelsOnPath)) {
                        insert(insertPos, score, prev);
                        return insertPos;
                   }
                } 
            }
            if (minPos >= 0) {
                insert(minPos, score, prev);
            }
            return minPos;
        }
        public void sortEntries() {
            Arrays.sort(solns);
            for (int i = 0; i < solns.length/2; i++) {
                Soln tmp = solns[i];
                solns[i] = solns[solns.length-1-i];
                solns[solns.length-1-i] = tmp;
            }
            for (int i = 1; i < solns.length; i++) {
                assert(solns[i-1].score >= solns[i].score);
            }
        }
    }
    class ContextForLabelConstraints extends Context {
        ContextForLabelConstraints(int numY, int beamsize, int pos, int startPos) {
            super(numY, beamsize, pos, startPos);
        }
        private static final long serialVersionUID = 1L;
        public void add(int y, Entry prevSoln, float thisScore) {
            if (labelConstraints==null) {
                super.add(y,prevSoln,thisScore);
            } else {
                if (getQuick(y) == null) {
                    setQuick(y, new EntryForLabelConstraints((pos==startPos)?1:beamsize, y, pos, labelConstraints.countConflicting((int) size()))); 
                }
                super.add(y,prevSoln,thisScore);
            }
        }
    }
    boolean markovModel=false;
    public SegmentViterbi(SegmentCRF nestedModel, int bs) {
        super(nestedModel, bs);
        this.segmentModel = nestedModel;
        this.featureGenNested = segmentModel.featureGenNested;
        setMarkovState(segmentModel.params);
    }
    private void setMarkovState(CrfParams params) {
        if (!params.miscOptions.getProperty("modelGraph", "semi-markov").equalsIgnoreCase("semi-markov"))
            markovModel=true;
    }
    public SegmentViterbi(CRF model,int bs) {
        super(model, bs);
        this.featureGenNested = (FeatureGeneratorNested) model.featureGenerator;
        setMarkovState(model.params);
    }
    protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        if (featureGenNested==null) {
            featureGenNested = segmentModel.featureGenNested;
        }
        SegmentTrainer.computeLogMi((CandSegDataSequence)dataSeq,i-ell,i,featureGenNested,lambda,Mi,Ri);

    }
    class SegmentIter extends Iter {
        int nc;
        CandidateSegments candidateSegs;
        protected void start(int i, DataSequence dataSeq) {
            candidateSegs = (CandidateSegments)dataSeq;
            nc = candidateSegs.numCandSegmentsEndingAt(i);
        }
        protected int nextEll(int i) {
            nc--;
            if (nc >= 0)
                return i -  candidateSegs.candSegmentStart(i,nc) + 1;
            return -1;
        }
    }	
    protected Iter getIter(){return (markovModel?new Iter():new SegmentIter());}
    /**
     * @return
     */
    int prevSegEnd = -1;
    protected double getCorrectScore(DataSequence dataSeq, int i, int ell, double[] lambda) {
        SegmentDataSequence data = (SegmentDataSequence)dataSeq;
        if (data.getSegmentEnd(i-ell+1) != i)
            return 0;
        if ((i - ell >= 0) && (prevSegEnd != i-ell))
            return RobustMath.LOG0;
        prevSegEnd = i;
        if ((labelConstraints != null) && labelConstraints.conflicting(data.y(i))) {
            for (int segStart = 0; segStart < i-ell+1; segStart = data.getSegmentEnd(segStart)+1) {
                int segEnd = data.getSegmentEnd(segStart);
                if (labelConstraints.disallowedPairs.conflictingPair(data.y(i),data.y(segStart),(segEnd==i-ell)?-1:0))  // TODO: 0 here is not correct. 
                    return RobustMath.LOG0;
            }
        }
        if (model.params.debugLvl > 1) {
            // output features that hold
            featureGenNested.startScanFeaturesAt(dataSeq,i-ell,i);
            while (featureGenNested.hasNext()) {
                Feature f = featureGenNested.next();
                if (((CandSegDataSequence)data).holdsInTrainingData(f,i-ell,i)) {
                    System.out.println("Feature " + (i-ell) + " " + i + " " + featureGenNested.featureName(f.index()) + " " + lambda[f.index()] + " " + f.value());
                }
            }
        }
        double val = (Ri.getQuick(dataSeq.y(i)) + ((i-ell >= 0)?Mi.get(dataSeq.y(i-ell),dataSeq.y(i)):0));
        if (Double.isInfinite(val)) {
            System.out.println("Infinite score");
        }
        return val;
    }
    protected void setSegment(DataSequence dataSeq, int prevPos, int pos, int label) {
        ((CandSegDataSequence)dataSeq).setSegment(prevPos+1,pos, label);
    }

    public void singleSegmentClassScores(CandSegDataSequence dataSeq, double lambda[], TIntFloatHashMap scores) {
        viterbiSearch(dataSeq, lambda,false);
        scores.clear();
        int i = dataSeq.length()-1;
        if (i >= 0) {
            double norm	 = RobustMath.LOG0;

            for (int y = 0; y < context[i].size(); y++) {
                if (context[i].entryNotNull(y)) {
                    Soln soln = ((Entry)context[i].getQuick(y)).get(0);
                    assert (soln.prevSoln == null); // only applicable for single segment.
                    norm = RobustMath.logSumExp(norm,soln.score);
                }
            }
            for (int y = 0; y < context[i].size(); y++) {
                if (context[i].entryNotNull(y)) {
                    Soln soln = ((Entry)context[i].getQuick(y)).get(0);
                    scores.put(soln.label,(float)Math.exp(soln.score-norm));
                }
            }
            /*context[i].getNonZeros(validPrevYs, prevContext);

            for (int prevPx = 0; prevPx < validPrevYs.size(); prevPx++) {
                Soln soln = ((Entry)prevContext.getQuick(prevPx)).get(0);
                assert (soln.prevSoln == null); // only applicable for single segment.
                norm = RobustMath.logSumExp(norm,soln.score);
            }
            for (int prevPx = 0; prevPx < validPrevYs.size(); prevPx++) {
                Soln soln = ((Entry)prevContext.getQuick(prevPx)).get(0);
                scores.put(soln.label,(float)Math.exp(soln.score-norm));
            }
             */
        }
    }
    protected Context newContext(int numY, int beamsize, int pos, int startPos){
        if (labelConstraints == null)
            return new Context(numY,beamsize,pos, startPos);        
        return  new ContextForLabelConstraints(numY,beamsize,pos,startPos); 
    }
    public double viterbiSearch(DataSequence dataSeq, double[] lambda,
            boolean calcCorrectScore) {
        //labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        return viterbiSearch(dataSeq, lambda, null, null, true, calcCorrectScore);
    }

    public double viterbiSearch(DataSequence dataSeq, double lambda[], 
            DoubleMatrix2D[][] Mis, DoubleMatrix1D[][] Ris, 
            boolean constraints, boolean calCorrectScore) {
        if(constraints)
            labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        else
            labelConstraints = null;
        return super.viterbiSearch(dataSeq, lambda, Mis, Ris, calCorrectScore);
    }

    public double viterbiSearch(DataSequence dataSeq, double lambda[],  
            DoubleMatrix2D[][] Mis, DoubleMatrix1D[][] Ris,  
            Soln soln, boolean constraints, boolean calCorrectScore) {
        if(constraints)
            labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        else
            labelConstraints = null;

        return super.viterbiSearch(dataSeq, lambda, Mis, Ris, soln, calCorrectScore);
    }	
    public double sumScoreTopKViolators(DataSequence dataSeq, double lambda[]) {
        LabelConstraints labelCons = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        if (labelCons==null)
            return RobustMath.LOG0;
        int oldbeamsize=beamsize;
        beamsize=20;
        viterbiSearch(dataSeq,lambda,null,null,false,false);
        double totalScore=RobustMath.LOG0;
        int numSols = finalSoln.numSolns();
        for (int k = numSols-1; k >= 0; k--) {
            Soln ybest = finalSoln.get(k);
            float score = ybest.score;
            ybest = ybest.prevSoln;
            TIntHashSet labelsSeen=new TIntHashSet();
            boolean violating=false;
            while (ybest != null) { 
                if (!labelCons.valid(labelsSeen,ybest.label,-1)) {
                    violating=true;
                    break;
                }
                if (labelCons.conflicting(ybest.label)) {
                    labelsSeen.add(ybest.label);
                }
                ybest = ybest.prevSoln;
            }
            if (violating) totalScore = RobustMath.logSumExp(score,totalScore);
        }
        beamsize=oldbeamsize;
        return totalScore;
    }
    public double viterbiSearchBackward(DataSequence dataSeq, double[] lambda,
            DoubleMatrix2D Mis[][],DoubleMatrix1D Ris[][],
            boolean calcCorrectScore) {
        labelConstraints = null;
        return super.viterbiSearchBackward(dataSeq, lambda, Mis, Ris, calcCorrectScore);
    }
    public double viterbiSearchBackward(DataSequence dataSeq, double[] lambda,
            DoubleMatrix2D Mis[][],DoubleMatrix1D Ris[][], boolean constraints,
            boolean calcCorrectScore) {
        if(constraints)
            labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        else
            labelConstraints = null;
        return super.viterbiSearchBackward(dataSeq, lambda, Mis, Ris, calcCorrectScore);
    }

    public static class SegmentationImpl extends LabelSequence implements Segmentation {
        class Segment implements Comparable {
            int start;
            int end;
            int label;
            int id;
            Segment(int start, int end, int label) {
                this.start = start;
                this.end = end;
                this.label = label;
            }
            /* (non-Javadoc)
             * @see java.lang.Comparable#compareTo(java.lang.Object)
             */
            public int compareTo(Object arg0) {
                return end - ((Segment)arg0).end;
            }
        }
        TreeSet<Segment> segments = new TreeSet<Segment>();
        Segment segmentArr[]=null;
        Segment dummySegment = new Segment(0,0,0);
        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#numSegments()
         */
        public int numSegments() {
            return segments.size();
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#segmentLabel(int)
         */
        public int segmentLabel(int segmentNum) {
            return segmentArr[segmentNum].label;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#segmentStart(int)
         */
        public int segmentStart(int segmentNum) {
            return segmentArr[segmentNum].start;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#segmentEnd(int)
         */
        public int segmentEnd(int segmentNum) {
            return segmentArr[segmentNum].end;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#getSegmentId(int)
         */
        public int getSegmentId(int offset) {
            dummySegment.end = offset;
            //            if (segments.headSet(dummySegment) == null)
            //              return 0;
            return ((Segment)segments.tailSet(dummySegment).first()).id;
        }

        /* (non-Javadoc)
         * @see iitb.CRF.Segmentation#setSegment(int, int, int)
         */
        public void setSegment(int segmentStart, int segmentEnd, int label) {
            Segment segment = new Segment(segmentStart, segmentEnd, label);
            segments.add(segment);
        }
        public void doneAdd() {
            segmentArr = new Segment[segments.size()];
            int p = 0;
            for (Iterator<Segment> iter = segments.iterator(); iter.hasNext();) {
                segmentArr[p++] = iter.next();
            }
            for (int i = segmentArr.length-1; i >= 0; segmentArr[i].id = i, i--);
        }
        public void apply(DataSequence data) {
            for (int i = 0; i < numSegments(); i++)
                ((CandSegDataSequence)data).setSegment(segmentStart(i),segmentEnd(i),segmentLabel(i));
        }
        /**
         * @param prevPos
         * @param pos
         * @param label
         */
        public void add(int prevPos, int pos, int label) {
            setSegment(prevPos+1,pos,label);
        };
    };
    public Segmentation[] segmentSequences(CandSegDataSequence dataSeq, double lambda[], int numLabelSeqs, double[] scores) {
        viterbiSearch(dataSeq, lambda,false);
        int numSols = Math.min(finalSoln.numSolns(), numLabelSeqs);
        Segmentation segments[] = new Segmentation[numSols];
        for (int k = numSols-1; k >= 0; k--) {
            Soln ybest = finalSoln.get(k);
            if (scores != null) scores[k] = (double)ybest.score;
            ybest = ybest.prevSoln;
            segments[k] = new SegmentationImpl();
            while (ybest != null) {	
                segments[k].setSegment(ybest.prevPos()+1,ybest.pos,ybest.label);
                ybest = ybest.prevSoln;
            }
            ((SegmentationImpl)segments[k]).doneAdd();
        }
        
        if (scores!=null) {
            double lZx = model.getLogZx(dataSeq);
            if (scores.length > numSols) scores[numSols] = lZx;
            for (int i = 0; i < numSols; i++) {
                scores[i] = Math.min(Math.exp(scores[i]-lZx),1);
            }
        }
        return segments;
    }
    protected LabelSequence newLabelSequence(int len){
        return new SegmentationImpl();
    }
};
