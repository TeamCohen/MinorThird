/** FeatureStore.java
 * Created on Apr 2, 2005
 * 
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.BSegmentCRF;

import iitb.BSegmentCRF.BSegmentTrainer.MatrixWithRange;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.LogSparseDoubleMatrix1D;
import iitb.CRF.RobustMath;
import iitb.CRF.FeatureGenCache;

import java.util.Iterator;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class FeatureStore {
    BFeatureGenerator bfgen;
    int numLabels;
    class BasicFeature {
        float val;
        int _y;
        int _index;
        BasicFeature() {}
        BasicFeature(BFeature feature) {
            this.val = feature.value();
            this._y = feature.y();
            this._index = feature.index();
        }
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#index()
         */
        public int index() {
            return _index;
        }
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#y()
         */
        public int y() {
            return _y;
        }
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#yprev()
         */
        public int yprev() {
            return -1;
        }
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#value()
         */
        public float value() {
            return val;
        }
        public String toString() {
            return bfgen.featureName(_index) + " v" + value() + " y" + y();
        }
    }
    class BasicEdgeFeature extends BasicFeature {
        int _yprev;
        /**
         * @param f
         */
        public BasicEdgeFeature(BFeature f) {
            super(f);
            _yprev = f.yprev();
        }
        public BasicEdgeFeature(){}
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#yprev()
         */
        public int yprev() {
            return _yprev;
        }
    };
    class FeatureImpl extends BasicEdgeFeature implements BFeature {
        int startB;
        boolean openS;
        int endB;
        boolean openE;
        public FeatureImpl() {super();}
        /**
         * @param f
         */
        public FeatureImpl(BFeature f) {
            super(f);
            this.startB = f.start();
            endB = f.end();
            openS = f.startOpen();
            openE = f.endOpen();
        }
        public void init(Feature f, int s, int e, int type) {
            startB = s;
            endB = e;
            openS = ((type/2) == 1)?true:false;
            openE = ((type & 1)==1)?true:false;
            _y = f.y();
            val = f.value();
            _index = f.index();
            _yprev = -1;
        }
        public void init(Feature f, int s, int e) {
            init(f,s,e,1);
            _yprev = f.yprev();
        }
        
        /* (non-Javadoc)
         * @see iitb.BSegmentCRF.BFeature#start()
         */
        public int start() {
            return startB;
        }
        /* (non-Javadoc)
         * @see iitb.BSegmentCRF.BFeature#startOpen()
         */
        public boolean startOpen() {
            return openS;
        }
        /* (non-Javadoc)
         * @see iitb.BSegmentCRF.BFeature#end()
         */
        public int end() {
            return endB;
        }
        /* (non-Javadoc)
         * @see iitb.BSegmentCRF.BFeature#endOpen()
         */
        public boolean endOpen() {
            return openE;
        }
        /* (non-Javadoc)
         * @see iitb.CRF.Feature#yprevArray()
         */
        public int[] yprevArray() {
            return null;
        }
        public String toString() {
            return bfgen.featureName(_index) + " v" + value() + " y" + y() + "  s" + start() + ":" + startOpen() + "  e" + end() + ":" + endOpen();
        }
    };
    
    static int endOpen = 1;
    static int endExact = 0;
    static int startExact = 2;
    static int startOpen = 3;
    
    FeatureGenCache.AllFeatureCache allFeatureCache;
    FeatureGenCache.AllFeatureCache.Flist[][] stateFeatures[]=null;
    class EdgeFeatures {
    	FeatureGenCache.AllFeatureCache.FeatureVector edgeFeatureArray[];
        public EdgeFeatures(int size) {
            edgeFeatureArray = new FeatureGenCache.AllFeatureCache.FeatureVector[size];
            for (int i = 0; i < size; i++) {
                add(i);
            }
        }
        FeatureGenCache.AllFeatureCache.FeatureVector get(int i) {return edgeFeatureArray[i];}
        public void add(int i) {
            if (allFeatureCache.edgeFeaturesXIndependent && (i > 0)) {
                edgeFeatureArray[i] = allFeatureCache.edgeFeatures.getEdgeIds(0,1,dataLen);
            } else {
                edgeFeatureArray[i] = allFeatureCache.newFeatureVector();
            }
        }
        public void addEdgeFeature(int i, BFeature f) {
            if (!allFeatureCache.edgeFeaturesXIndependent) {
                edgeFeatureArray[i].add(f);
            } else {
                if (i == 1) {
                    allFeatureCache.edgeFeatures.addEdgeFeature(allFeatureCache.add(f), i-1,i,dataLen);
                }
            }
        }
        public void clear() {
            if (!allFeatureCache.edgeFeaturesXIndependent) {
                for (int i = 0; i < edgeFeatureArray.length; i++) {
                    edgeFeatureArray[i].clear();
                }
            }
        }
    }
    EdgeFeatures edgeFeatures;
    double DEFAULT_VALUE = RobustMath.LOG0;
    double lambda[];
    int dataLen;
    FeatureStore(boolean edgeFeatureXIndependent){
        allFeatureCache = new FeatureGenCache.AllFeatureCache(edgeFeatureXIndependent);
    }
    FeatureStore(FeatureGenCache.AllFeatureCache allFeatureCache) {
        this.allFeatureCache = allFeatureCache;
    }
    void init(DataSequence data, BFeatureGenerator fgen, double[] lambda, int numY) {
        bfgen = fgen;
        numLabels = numY;
        dataLen = data.length();
        this.lambda = lambda;
        allocateScratch(data.length());
        fgen.startScanFeaturesAt(data);
        boolean featuresFired = false;
        while (fgen.hasNext()) {
            BFeature f = fgen.nextFeature();
            if (f.yprev() >= 0) {
                edgeFeatures.addEdgeFeature(f.start(),f);
            } else {
                int type = (f.endOpen()?1:0) + (f.startOpen()?1:0)*2;
                stateFeatures[type][f.end()-f.start()][f.start()].add(f,lambda);
                featuresFired=true;
            }
        }
        allFeatureCache.edgeFeatures.doneOneRoundEdges();
        assert(featuresFired);
    }
    void copy(FeatureStore fstore) {
        dataLen = fstore.dataLen;
        stateFeatures = fstore.stateFeatures;
        edgeFeatures = fstore.edgeFeatures;
        lambda = fstore.lambda;
        bfgen = fstore.bfgen;
        numLabels = fstore.numLabels;
    }
    void setLambda(double[] lambda) {
        this.lambda = lambda;
        for (int i = 0; i < dataLen; i++) {
            for (int type = 0; type < 4; type++) {
                for (int l = bfgen.maxBoundaryGap()-1; l >=0; l--) {
                    stateFeatures[type][l][i].calcMatrix(lambda);
                }
            }
        }
    }
    /**
     * @param n
     */
    private void allocateScratch(int n) {
        if (stateFeatures == null) {
            int m = bfgen.maxBoundaryGap();
            stateFeatures = new FeatureGenCache.AllFeatureCache.Flist[4][m][0];
        }
        if ((stateFeatures[0][0] != null) && (stateFeatures[0][0].length >= n)) {
            for (int i = 0; i < n; i++) {
                for (int type = 0; type < 4; type++) {
                    for (int l = bfgen.maxBoundaryGap()-1; l >=0; l--) {
                        stateFeatures[type][l][i].clear();
                    }
                }
            }
            edgeFeatures.clear();
            return;
        }
        int size = 2*n;
        for (int type = 0; type < 4; type++) {
            for (int l = bfgen.maxBoundaryGap()-1; l >=0; l--) {
                stateFeatures[type][l] = new FeatureGenCache.AllFeatureCache.Flist[size];
            }
        }
        edgeFeatures = new EdgeFeatures(size);
        for (int i = 0; i < size; i++) {
            for (int type = 0; type < 4; type++) {
                for (int l = bfgen.maxBoundaryGap()-1; l >=0; l--) {
                    stateFeatures[type][l][i] = allFeatureCache.newFlist(numLabels);
                }
            }
        }
    }
    
    class Iter {
        int currentType;
        int currentLen;
        int s;
        Iterator<Feature> viter;
        FeatureImpl featureImpl = new FeatureImpl();
        public void init() {
            int m = bfgen.maxBoundaryGap();
            s = 0;
            currentLen = m-1;
            currentType = -1;
            viter = null;
            advance();
        }
        void advance() {
            while (true) {
                if ((viter != null) && viter.hasNext())
                    return;
                currentType++;
                if (currentType < 4) {
                    viter = stateFeatures[currentType][currentLen][s].iterator();
                    continue;
                } else if ((currentType == 4) && (currentLen == 0)) {
                    viter = edgeFeatures.get(s).iterator();
                    continue;
                } else {
                    currentType = -1;
                    currentLen--;
                    if (currentLen < 0) {
                        s++;
                        currentLen =  bfgen.maxBoundaryGap()-1;
                        if (s >= dataLen)
                            return;
                    }
                }
            }
        }
        /**
         * @return
         */
        public boolean hasNext() {
            return (viter != null) && viter.hasNext(); 
        }
        /**
         * @return
         */
        public BFeature next() {
            if (currentType == 4) {
                featureImpl.init((Feature)viter.next(),s,s+currentLen);
            } else {
                featureImpl.init((Feature)viter.next(),s,s+currentLen,currentType);
            }
            advance();
            return featureImpl;
        }
    }
    /*
     * 
     */
    public void scanFeaturesSorted(Iter iter) {
        iter.init();
    }
    /**
     * @param i
     * @param mi_YY
     * edge features are constrainted to have "=s" and >=s" as the start and end boundaries
     */
    public void getLogMi(int i, DoubleMatrix2D mi_YY) {
        double DEFAULT_VALUE = RobustMath.LOG0;
        mi_YY.assign(DEFAULT_VALUE);
        for (Iterator<Feature> iter = edgeFeatures.get(i).iterator(); iter.hasNext(); ) {
            Feature f = iter.next();
            double oldVal = mi_YY.get(f.yprev(), f.y());
            if (oldVal == DEFAULT_VALUE)
                oldVal = 0;
            mi_YY.set(f.yprev(),f.y(),oldVal+lambda[f.index()]*f.value());
        }
    }
    
    static class Condition {
        static int GE = 0;
        static int LE = 1;
        int val;
        char b;
        int op;
        boolean openOnly;
        void init(int val, char b, int op, boolean openOnly) {
            this.val = val;
            this.b = b;
            this.op = op;
            this.openOnly = openOnly;
        }
        /**
         * @param feature
         * @return
         */
        public boolean satisfies(BFeature feature) {
            int fval = ((b=='S')?feature.start():feature.end());
            boolean open = ((b=='S')?feature.startOpen():feature.endOpen());
            if (!open && openOnly)
                return false;
            if (!open)
                return (val == fval);
            return (op==GE)?(fval >= val):(fval <= val);
        }
    }
    
    private void addFeatures(DoubleMatrix1D mat, int type, int index, Condition predicate) {
        addFeatures(mat,type,index,predicate,true);
    }
    private void addFeatures(DoubleMatrix1D mat, int type, int index, Condition predicate, boolean add) {
        if ((type == endExact) || (type == endOpen)) {
            int t = 2 + ((type==endOpen)?1:0);
            int startLB = Math.max(index-bfgen.maxBoundaryGap()+1,0);
            int startUB = Math.min(dataLen-1,index);
            if (cond.op == Condition.GE) {
                startLB = Math.max(cond.val,startLB);
            } else {
                startUB = Math.min(cond.val,startUB);
            }
            for (int s = startLB; s <= startUB; s++) {
                if (index-s >= 0) addFeatures(stateFeatures[t][index-s][s],mat,add);
            }
            if (!cond.openOnly) {
                t = (type==endOpen)?1:0;
                int startB = cond.val;
                if ((index-startB < bfgen.maxBoundaryGap()) && (startB >= 0) && (startB < dataLen))
                    addFeatures(stateFeatures[t][index-startB][startB],mat,add);
            }
        } else {
            int t = 1 + 2*((type==startOpen)?1:0);
            int endUB = Math.min(index+bfgen.maxBoundaryGap()-1,dataLen-1);
            int endLB = index;
            if (cond.op == Condition.GE) {
                endLB = cond.val;
            } else {
                endUB = Math.min(cond.val,endUB);
            }
            for (int e = endLB; e <= endUB; e++) {
                if (e-index >= 0) addFeatures(stateFeatures[t][e-index][index],mat,add);
            }
            if (!cond.openOnly) {
                t = 2*((type==startOpen)?1:0);
                int endB = cond.val;
                if ((endB-index < bfgen.maxBoundaryGap()) && (endB-index >= 0) && (index < dataLen))
                    addFeatures(stateFeatures[t][endB-index][index],mat,add);
            }
        }
    }
    /**
     * @param vector
     * @param mat
     * @param add
     */
    boolean printFeatures = false;
    private void addFeatures3(FeatureGenCache.AllFeatureCache.Flist vector, DoubleMatrix1D mat, boolean add) {
        for (Iterator<Feature> iter = vector.iterator(); iter.hasNext();) {
            Feature feature = iter.next();
            if (printFeatures)
                System.out.println(feature);
            int f = feature.index();
            double oldVal = mat.get(feature.y());
            if (add) {
                if (oldVal == DEFAULT_VALUE)
                    oldVal = 0;
                mat.set(feature.y(),oldVal+lambda[f]*feature.value());
            } else {
                mat.set(feature.y(),oldVal-lambda[f]*feature.value()); 
            }
        }
    }
    /**
     * @param vector
     * @param mat
     * @param add
     */
    private void addFeatures(FeatureGenCache.AllFeatureCache.Flist vector, DoubleMatrix1D mat, boolean add) {
        if (vector.size()==0) {
            if (printFeatures) {System.out.println("No features");}
            return;
        }
        DoubleMatrix1D precomputedMat = vector.mat;
        for (int y = (int) (mat.size()-1); y >= 0; y--) {
            double val = precomputedMat.get(y);
            if (val == DEFAULT_VALUE) continue;
            double oldVal = mat.get(y);
            if (add) {
                if (oldVal==DEFAULT_VALUE) 
                    oldVal = 0;
                mat.set(y,oldVal+val);
            } else
                mat.set(y,oldVal-val);
        }
        if (printFeatures) {
            for (Iterator<Feature> iter = vector.iterator(); iter.hasNext();) {
                Feature feature = iter.next();
                System.out.println(bfgen.featureName(feature.index()));
            }
        }
    }
    /**
     * @param f
     * @param y
     * @param mat
     */
    private void removeFeatures(DoubleMatrix1D mat, int type, int index, Condition predicate) {
        addFeatures(mat,type,index,predicate,false);
    }
    Condition cond = new Condition();
    public void incrementRightB(DoubleMatrix1D ri_Y, MatrixWithRange openRi) {
        incrementRightB(ri_Y,openRi,false);
    }
    
    /**
     * @param ri_Y
     * @param openRi
     */
    public void incrementRightB(DoubleMatrix1D ri_Y, MatrixWithRange openRi, boolean openOnly) {
        openRi.end++;
        cond.init(openRi.start,'S', Condition.GE,openOnly);
        // add these to openRi
        addFeatures(openRi.mat,endOpen, openRi.end,cond);
        if (ri_Y != null) {
            ri_Y.assign(openRi.mat);
            addFeatures(ri_Y,endExact, openRi.end,cond);
            //if (!openOnly) checkMatrix(ri_Y,openRi);
        }
    }
    public void checkMatrix(DoubleMatrix1D ri_Y, MatrixWithRange openRi) {
        if (ri_Y==null)
            return;
        if ((openRi.end < 0) || (openRi.start < 0))
            return;
        assert(getExactR(openRi.start,openRi.end,new LogSparseDoubleMatrix1D((int) openRi.mat.size())).equals(ri_Y));
    }
    /**
     * @param ri_Y
     * @param openRi
     *  get all features with end boundary LE openRi.end and start boundary = openRi.start
     */
    public void decrementLeftB(DoubleMatrix1D ri_Y, MatrixWithRange openRi) {
        decrementLeftB(ri_Y,openRi,false);
    }
    /**
     * @param ri_Y
     * @param openRi
     *  get all features with end boundary LE openRi.end and start boundary = openRi.start
     */
    public void decrementLeftB(DoubleMatrix1D ri_Y, MatrixWithRange openRi, boolean endOpen) {
        openRi.start--;
        cond.init(openRi.end,'E', Condition.LE,endOpen);
        addFeatures(openRi.mat,startOpen, openRi.start,cond);
        if (ri_Y != null) {
            ri_Y.assign(openRi.mat);
            addFeatures(ri_Y,startExact, openRi.start, cond);
        }
       // if (!endOpen) checkMatrix(ri_Y,openRi);
    }
    /**
     * @param leftB
     * @param rightB
     * @param deltaRi
     * @param openDeltaRi
     * 
     * Get all features which are applicable for segments with left boundary <= leftB and right boundary = rightB
     */
    public void deltaR_RShift(int leftB, int rightB, DoubleMatrix1D deltaRi, DoubleMatrix1D openDeltaRi) {
        // feature should have open start boundary with start() at >= leftB 
        cond.init(leftB,'S',Condition.GE, true);
        openDeltaRi.assign(0);
        addFeatures(openDeltaRi,endOpen, rightB,cond);
        deltaRi.assign(openDeltaRi);
        addFeatures(deltaRi,endExact,rightB,cond);
    }
    /**
     * @param leftB
     * @param rightB
     * @param deltaRi
     * @param openDeltaRi
     * 
     * Get all features with left boundary = leftB, right boundary open with a value <= rightB
     */
    public void deltaR_LShift(int leftB, int rightB, DoubleMatrix1D deltaRi, DoubleMatrix1D openDeltaRi) {
        // TODO -- default value here should be set so as not to undo positions that are already enabled in full R.
        // current code will only word for the case of no restrict constraint.
        cond.init(rightB,'E',Condition.LE,true);
        if (openDeltaRi != null) {
            openDeltaRi.assign(0);
            addFeatures(openDeltaRi,startOpen,leftB,cond);
            deltaRi.assign(openDeltaRi);
        } else {
            deltaRi.assign(0);
        }
        addFeatures(deltaRi,startExact,leftB,cond);
    }
    /**
     * @return
     */
    public Iter getIterator() {
        return new Iter();
    }
    /**
     * @param ri_Y
     * @param i
     */
    public void removeExactEndFeatures(DoubleMatrix1D ri_Y, int leftB, int rightB) {
        if (rightB < 0)
            return;
        cond.init(leftB,'S',Condition.GE,false);
        removeFeatures(ri_Y,endExact,rightB,cond);
        // assert(getExactR(leftB,rightB,new LogSparseDoubleMatrix1D(ri_Y.size()),false).equals(ri_Y));
    }
    /**
     * @param ri_Y
     * @param i
     * @param j
     */
    public void removeExactStartFeatures(DoubleMatrix1D ri_Y, int leftB, int rightB) {
        cond.init(rightB,'E',Condition.LE,false);
        removeFeatures(ri_Y,startExact,leftB,cond);
    }
    /**
     * @param s
     * @param e
     * @param ri_Y
     */
    public DoubleMatrix1D getExactR(int s, int e, DoubleMatrix1D ri_Y) {
        return getExactR(s,e,ri_Y,true);
    }
    public DoubleMatrix1D getExactR(int s, int e, DoubleMatrix1D ri_Y, boolean endIsExact) {
        ri_Y.assign(DEFAULT_VALUE);
        cond.init(s,'S',Condition.GE,false);
        if (endIsExact) addFeatures(ri_Y,endExact,e,cond);
        for (int i = e; i >= s; i--)
            addFeatures(ri_Y,endOpen,i,cond);
        return ri_Y;
    }
    /**
     * @param ri_Y
     * @param openR
     */
    public void decrementRightB(DoubleMatrix1D ri_Y, MatrixWithRange openRi) {
        openRi.end--;
        cond.init(openRi.start,'S', Condition.GE,false);
        // add these to openRi
        removeFeatures(openRi.mat,endOpen,openRi.end+1,cond);
        if (ri_Y != null) {
            ri_Y.assign(openRi.mat);
            addFeatures(ri_Y,endExact,openRi.end,cond);
          //  checkMatrix(ri_Y,openRi);
        }
    }
}
