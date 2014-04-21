/*
 * Created on Apr 23, 2005
 *
 */
package iitb.BSegmentCRF;

import cern.colt.function.*;
import cern.colt.function.tdouble.IntDoubleFunction;
import cern.colt.function.tdouble.IntIntDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import iitb.BSegmentCRF.BSegmentTrainer.MatrixWithRange;
import iitb.CRF.*;
import iitb.CRF.SegmentViterbi.SegmentationImpl;

/**
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
public class BSegmentViterbi extends SparseViterbi {
    protected double getCorrectScore(DataSequence dataSeq, int i, int ell, double[] lambda) {
        Segmentation segmentation = (Segmentation)dataSeq;
        int segNum = segmentation.getSegmentId(i);
        int segLength = segmentation.segmentEnd(segNum)-segmentation.segmentStart(segNum)+1;
        if ((segmentation.segmentEnd(segNum)!=i) ||
                ((segLength <= m) && (ell != segLength))
                || ((segLength > m) && (ell != 1)))
            return 0;
        
        if (segLength > m) {
            fstore.getExactR(i-segLength+1,i,Ri);
            if (!reuseM) fstore.getLogMi(i-segLength+1,Mi);
        }
        double val = (Ri.getQuick(dataSeq.y(i)) + ((i-segLength >= 0)?Mi.get(dataSeq.y(i-segLength),dataSeq.y(i)):0));
        //System.out.println("Score for segment: "+ (i-segLength+1) + " " + i + " " + val);
        return val;
    }
    private void adjustScore(DataSequence dataSeq, DoubleMatrix1D ri, MatrixWithRange openri, int i, int ell) {
        Segmentation segmentation = (Segmentation)dataSeq;
        int segNum = segmentation.getSegmentId(openri.start);
        int segStart = segmentation.segmentStart(segNum);
        for (int y = 0; y < numY; y++) {
            ri.set(y, ri.get(y)+1);
        }
        if (segStart == openri.start) {
            ri.set(dataSeq.y(segStart), ri.get(dataSeq.y(segStart))-1);
        } else {
            // because the previous segment ended wrongly..
            for (int y = 0; y < numY; y++) {
                ri.set(y, ri.get(y)+1);
            }
        }
        // now see if there is a corr seg included in this segment.
        if (openri.start+1 <= i) {
            segNum = segmentation.getSegmentId(openri.start+1);
            if (segmentation.segmentStart(segNum)==openri.start+1) {
                for (int y = 0; y < numY; y++) {
                    ri.set(y, ri.get(y)+1);
                    openri.mat.set(y, openri.mat.get(y)+1);
                } 
            }
        }
    }
    FeatureStore fstore;
    BSegmentCRF bmodel;
    BSegmentTrainer.MatrixWithRange openRi;
    LogSparseDoubleMatrix1D deltaRi,openDeltaRi;
    boolean reuseM;
    Context openContext[];
    int m; 
    int numY;
    boolean lossAugmentedScore=false;
    
    protected BSegmentViterbi(BSegmentCRF model, int numY, int bs) {
        super(model, bs);
        this.bmodel = model;        
        reuseM = model.params.reuseM;
        this.numY = numY;
    }
    public BSegmentViterbi(BSegmentCRF model, int numY, int bs, boolean lossAugmentedScore) {
        super(model, bs);
        this.bmodel = model;        
        reuseM = model.params.reuseM;
        this.numY = numY;
        this.lossAugmentedScore = lossAugmentedScore;
    }
    protected void computeLogMi(DataSequence dataSeq, int i, int ell, double lambda[]) {
        if ((openRi.end != i) || (openRi.start < i-ell+1)) {
            openRi.init(i+1,i);
        }
        while (openRi.start != i-ell+1) {
            fstore.decrementLeftB(Ri,openRi);
            if (lossAugmentedScore) adjustScore(dataSeq,Ri,openRi, i,ell);
        }
        assert((openRi.start==i-ell+1) && (openRi.end == i));
        int ip = i-ell+1;
        if (ip-1 >= 0) {
            if (!reuseM) {
                fstore.getLogMi(ip,Mi);
            }
        }
    }
    
    class ApplyFunc implements IntIntDoubleFunction, IntDoubleFunction {
        DoubleMatrix1D matRi;
        Context prevContext;
        Context thisContext;
        DoubleMatrix2D matMi;
        
        ApplyFunc init(DoubleMatrix1D matRi, Context thisContext) {
            this.prevContext = null;
            this.matRi = matRi;
            this.thisContext = thisContext;
            return this;
        }
        ApplyFunc init(DoubleMatrix1D matRi, DoubleMatrix2D matMi, Context prevContext, Context thisContext) {
            this.matRi = matRi;
            this.matMi = matMi;
            this.prevContext = prevContext;
            this.thisContext = thisContext;
            return this;
        }
        public double apply(int yp, int yi, double val) {
            if (prevContext.entryNotNull(yp))
                thisContext.add(yi, prevContext.getEntry(yp),(float)(matMi.get(yp,yi)+matRi.get(yi)));
            return val;
        }
        public double apply(int yi, double val) {
            thisContext.add(yi,(prevContext==null)?null:prevContext.getEntry(yi),(float)val);
            return val;
        }
    }
    ApplyFunc applyFunc = new ApplyFunc();
    protected void finishContext(int i) {
//      Ri at this point contains features for segment (i-m+1, i)
        openContext[i].clear();
        if (i-m >= -1) {
            fstore.removeExactEndFeatures(Ri, i-m+1, i);
            if (i-m==-1) Ri.forEachNonZero(applyFunc.init(Ri,openContext[i]));
        }
        if (i-m >= 0) {
            fstore.deltaR_RShift(i-m,i,deltaRi, openDeltaRi);
            deltaRi.forEachNonZero(applyFunc.init(deltaRi,null,openContext[i-1],context[i]));
            
            openDeltaRi.forEachNonZero(applyFunc.init(openDeltaRi,null,openContext[i-1],openContext[i]));
            Mi.forEachNonZero(applyFunc.init(Ri,Mi,context[i-m],openContext[i]));
        }
    }
    class MIter extends Iter {
        protected void start(int i, DataSequence dataSeq) {ell = 0;}
        protected int nextEll(int i) {
            if ((ell < m) && (i-ell >= 0)) {
                 ell++; return ell;
            }
            return 0;
        }
    }
    protected Iter getIter(){return new MIter();}
    protected void setSegment(DataSequence dataSeq, int prevPos, int pos, int label) {
        ((Segmentation)dataSeq).setSegment(prevPos+1,pos, label);
    }
    public static class BSegmentationImpl extends SegmentViterbi.SegmentationImpl {
        public void apply(DataSequence data) {
            for (int i = 0; i < numSegments(); i++)
                ((Segmentation)data).setSegment(segmentStart(i),segmentEnd(i),segmentLabel(i));
        }
    }
    protected LabelSequence newLabelSequence(int len){
        return new BSegmentationImpl();
    }
    private static final long serialVersionUID = 1L;
    protected void allocateScratch(int numY) {
        super.allocateScratch(numY);
        applyFunc = new ApplyFunc();
        fstore = new FeatureStore(false);
        openRi = new BSegmentTrainer.MatrixWithRange(new LogSparseDoubleMatrix1D(numY));
        deltaRi = new LogSparseDoubleMatrix1D(numY);
        openDeltaRi = new LogSparseDoubleMatrix1D(numY);
        m = bmodel.bfgen.maxBoundaryGap();
    }
    class OpenSoln extends Soln {
        /**
		 * 
		 */
		private static final long serialVersionUID = 6332992368741370660L;
		/*
        protected void setPrevSoln(Soln prevSoln, float score) {
            if (prevSoln instanceof OpenSoln) {
                prevSoln = prevSoln.prevSoln;
                assert (!(prevSoln instanceof OpenSoln));
            }
            super.setPrevSoln(prevSoln, score);
        }
        */
        public OpenSoln(int id, int p) {
            super(id, p);
        }
        protected Soln getSoln() {
            // this object does not really store the solution,
            // so return prevSoln.
            return prevSoln;
        }
    }
    class OpenEntry extends Entry {
        protected Soln newSoln(int label, int pos) {
            return new OpenSoln(label,pos);
        }
        protected OpenEntry(int beamsize, int id, int pos) {
            super(beamsize,id,pos);
        }
    }
    class OpenContext extends Context {
        /**
         * @param arg0
         * @param arg1
         * @param arg2
         */
        protected OpenContext(int arg0, int arg1, int arg2) {
            super(arg0, arg1, arg2, 0);
            // TODO Auto-generated constructor stub
        }
        protected Entry newEntry(int beamsize, int label, int pos) {
            return new OpenEntry(beamsize,label,pos);
        }
        private static final long serialVersionUID = 1L;
    }
    public double viterbiSearch(DataSequence dataSeq, double lambda[], boolean calcCorrectScore) {
        allocateScratch(numY);
        fstore.init(dataSeq,bmodel.bfgen,lambda,numY);
        openRi.init(1,0);
        if ((reuseM) && (dataSeq.length() > 0))
            fstore.getLogMi(1,Mi);
        if ((openContext == null) || openContext.length < dataSeq.length()) {
            int start = 0;
            Context oldopenContext[] = openContext;
            openContext = new Context[2*dataSeq.length()];
            if (oldopenContext != null) {
                for (int l = 0; l < oldopenContext.length; l++) {
                    openContext[l] = oldopenContext[l];
                }
                start = oldopenContext.length;
            }
            for (int l = start; l < openContext.length; l++) {
                openContext[l] = new OpenContext(numY,beamsize,l);
            }
        }
        double corrScore = super.viterbiSearch(dataSeq,lambda,calcCorrectScore);
       /* if (calcCorrectScore) {
            double score = 0;
            fstore.printFeatures = true;
            Segmentation segmentation = (Segmentation)dataSeq;
            for (int segNum = 0; segNum < segmentation.numSegments(); segNum++) {
                int segStart = segmentation.segmentStart(segNum);
                int segEnd = segmentation.segmentEnd(segNum);
                fstore.getExactR(segStart,segEnd,Ri);
                double val = Ri.get(dataSeq.y(segEnd));
                if (segNum > 0) {
                    if (!reuseM) fstore.getLogMi(segStart,Mi);
                    val += Mi.get(dataSeq.y(segStart-1),dataSeq.y(segEnd));
                }
                System.out.println("CScore for segment: "+ segStart + " "+segEnd + " " + val);
                score += val;
            }
            fstore.printFeatures = false;
            //assert (score == corrScore);
        }
        */
        return corrScore;
    }
}
