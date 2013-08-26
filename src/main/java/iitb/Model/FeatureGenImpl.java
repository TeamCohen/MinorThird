/** FeatureGenImpl.java
 * 
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */

package iitb.Model;
import gnu.trove.set.hash.TIntHashSet;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.FeatureGeneratorNested;
import iitb.CRF.SegmentDataSequence;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * The FeatureGenerator is an aggregator over all these different
 * feature types. You can inherit from the FeatureGenImpl class and
 * after calling one of the constructors that does not make a call to
 * (addFeatures()) you can then implement your own addFeatures
 * class. There you will typically add the EdgeFeatures feature first
 * and then the rest.  So, for example if you wanted to add some
 * parameter for each label (like a prior), you can create a new
 * FeatureTypes class that will create as many featureids as the
 * number of labels. You will have to create a new class that is
 * derived from FeatureGenImpl and just have a different
 * implementation of the addFeatures subroutine. The rest will be
 * handled by the parent class.  
 * This class  is responsible for converting the
 * string-ids that the FeatureTypes assign to their features into
 * distinct numbers. It has a inner class called FeatureMap that will
 * make one pass over the training data and create the map of
 * featurenames->integer id and as a side effect count the number of
 * features.
 *
 * @author Sunita Sarawagi
 * */

public class FeatureGenImpl implements FeatureGeneratorNested {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7651911985442866611L;
	ArrayList<FeatureTypes> features;
    transient Iterator<FeatureTypes> featureIter;
    protected FeatureTypes currentFeatureType;
    protected FeatureImpl featureToReturn, feature;
    public Model model;
    int numFeatureTypes=0;
    int totalFeatures;
    boolean _fixedTransitions=true;
    public boolean generateOnlyXFeatures=false;
    public boolean addOnlyTrainFeatures=true;
    TIntHashSet retainedFeatureTypes=new TIntHashSet(); // all features of this type are retained.
    
    transient DataSequence data;
    int cposEnd;
    int cposStart;
    protected WordsInTrain dict;
    Vector<WordsInTrain> otherDicts = new Vector<WordsInTrain>();
    
    public void addFeature(FeatureTypes fType) {
        addFeature(fType,false);
    }
    public void addFeature(FeatureTypes fType, boolean retainThis) {
        features.add(fType);
        if (retainThis) retainedFeatureTypes.add(fType.getTypeId()+1);
        if (!fType.fixedTransitionFeatures())
            _fixedTransitions = false;
    }
    public void setDict(WordsInTrain d) {
        dict = d;
    }
    public WordsInTrain getDict(){
        if (dict == null)
            dict = new WordsInTrain();
        return dict;
    }
    protected void addFeatures() { 
        addFeature(new EdgeFeatures(this));
        addFeature(new StartFeatures(this));
        addFeature(new EndFeatures(this));
        
        dict = new WordsInTrain();
       addFeature(new UnknownFeature(this,dict));
        // addFeature(new KnownInOtherState(model, dict));
        //	addFeature(new KernelFeaturesForLongEntity(model,new WordFeatures(model, dict)));
        addFeature(new WordFeatures(this, dict));
        addFeature(new FeatureTypesEachLabel(this,new ConcatRegexFeatures(this,0,0)));
    }
    protected FeatureTypes getFeature(int i) {
        return features.get(i);
    }
    protected boolean keepFeature(DataSequence seq, FeatureImpl f) {
    	  if ((retainedFeatureTypes != null) && (retainedFeatureTypes.contains(currentFeatureType.getTypeId()+1)))
            return true;
    	  return retainFeature(seq,f);
    }
    protected boolean retainFeature(DataSequence seq, FeatureImpl f) {
        return ((seq.y(cposEnd) == f.y() || seq.y(cposEnd) < 0) 
                && ((cposStart == 0) || (f.yprev() < 0) || (seq.y(cposStart-1) == f.yprev()) || seq.y(cposStart-1) < 0));
    }
    protected boolean featureCollectMode = false;
    class FeatureMap implements Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = -2268366275560581428L;
		Hashtable<FeatureIdentifier, FeatureImpl> strToInt = new Hashtable<FeatureIdentifier, FeatureImpl>();
        FeatureIdentifier idToName[];
        FeatureMap(){
            featureCollectMode = true;
        }
        public int getId(FeatureImpl f) {
            int id = getId(f.identifier());
            
            if ((id >= 0) && featureCollectMode) {
                FeatureIdentifier storedFIdentifier = (strToInt.get(f.identifier())).identifier();
                if (!storedFIdentifier.name.equals(f.identifier().name)) {
                    System.out.println("WARNING: same feature-id for different feature names?: " 
                            + storedFIdentifier + ":" + f.identifier());
                }
            }
            
            if ((id < 0) && featureCollectMode && (!addOnlyTrainFeatures || keepFeature(data,f))) {
              //  System.out.println("Feature " + f.identifier().id + " " + f.identifier());
                return add(f);
            }
            return id;
        }
        private int getId(Object key) {
            if (strToInt.get(key) != null) {
//                return ((Integer)strToInt.get(key)).intValue();
                return ((FeatureImpl)strToInt.get(key)).index();
            }
            return -1;
        }
        public int getIndex(FeatureIdentifier fId) {return getId(fId);}
        public int add(FeatureImpl feature) {
            int newId = strToInt.size();
//            strToInt.put(feature.identifier().clone(), new Integer(newId));
            FeatureImpl newFeature = (FeatureImpl) feature.clone();
            newFeature.id = newId;
            strToInt.put(newFeature.identifier(),newFeature);
            return newId;
        }
        void freezeFeatures() {
            //	    System.out.println(strToInt.size());
            featureCollectMode = false;
            idToName = new FeatureIdentifier[strToInt.size()];
            for (Enumeration<FeatureIdentifier> e = strToInt.keys() ; e.hasMoreElements() ;) {
                //TODO: Just add immediately
            	FeatureIdentifier key = e.nextElement();
                idToName[getId(key)] = key;
            }
            totalFeatures = strToInt.size();
        }
        public int collectFeatureIdentifiers(DataIter trainData, int maxMem) throws Exception {
            for (trainData.startScan(); trainData.hasNext();) {
                DataSequence seq = trainData.next();
                addTrainRecord(seq);               
            }
            freezeFeatures();
            return strToInt.size();
        }
        public void write(PrintWriter out) throws IOException {
            out.println("******* Features ************");
            out.println(strToInt.size());
            for (Enumeration<FeatureIdentifier> e = strToInt.keys() ; e.hasMoreElements() ;) {
                Object key = e.nextElement();
                out.println(key + " " + getId(key));
            }
        }
        public int read(BufferedReader in) throws IOException {
            in.readLine();
            int len = Integer.parseInt(in.readLine());
            String line;
            for(int l = 0; (l < len) && ((line=in.readLine())!=null); l++) {
                StringTokenizer entry = new StringTokenizer(line," ");
                FeatureIdentifier key = new FeatureIdentifier(entry.nextToken());
                int pos = Integer.parseInt(entry.nextToken());
                strToInt.put(key,new FeatureImpl(pos,key));
            }
            freezeFeatures();
            return strToInt.size();
        }
        public FeatureIdentifier getIdentifier(int id) {return idToName[id];} 
        public String getName(int id) {return idToName[id].toString();} 
    };
    FeatureMap featureMap;
    static Model getModel(String modelSpecs, int numLabels) throws Exception {
        // create model..
        return Model.getNewModel(numLabels,modelSpecs);
    }
    public FeatureGenImpl(String modelSpecs, int numLabels) throws Exception {
        this(modelSpecs,numLabels,true);
    }
    public FeatureGenImpl(String modelSpecs, int numLabels, boolean addFeatureNow) throws Exception {
        this(getModel(modelSpecs,numLabels),numLabels,addFeatureNow);
    }
    public FeatureGenImpl(Model m, int numLabels, boolean addFeatureNow) throws Exception {
        model = m;
        features = new ArrayList<FeatureTypes>();
        featureToReturn = new FeatureImpl();
        feature = new FeatureImpl();
        featureMap = new FeatureMap();
        if (addFeatureNow) addFeatures();
    }
    
    public boolean stateMappings(DataIter trainData) throws Exception {
        if (model.numStates() == model.numberOfLabels())
            return false;
        for (trainData.startScan(); trainData.hasNext();) {
            DataSequence seq = trainData.next();
            if (seq instanceof SegmentDataSequence) {
                model.stateMappings((SegmentDataSequence)seq);
            } else {
                model.stateMappings(seq);
            }
        }
        return true;
    }
    public boolean mapStatesToLabels(DataSequence data) {
        if (model.numStates() == model.numberOfLabels())
            return false;
        if (data instanceof SegmentDataSequence) {
            model.mapStatesToLabels((SegmentDataSequence)data);
        } else {
            for (int i = 0; i < data.length(); i++) {
                data.set_y(i, label(data.y(i)));
            }
        }
        return true;
    }
    public void labelsToSegments(SegmentDataSequence data) {
         model.mapStatesToLabels((SegmentDataSequence)data);
    }
    public int maxMemory() {return 1;}
    public boolean train(DataIter trainData) throws Exception {
        return train(trainData,true);
    }
    public boolean train(DataIter trainData, boolean cachedLabels) throws Exception {
        return train(trainData,cachedLabels,true);
    }
    public boolean labelMappingNeeded() {return model.numStates() != model.numberOfLabels();}
   
    public boolean train(DataIter trainData, boolean cachedLabels, boolean collectIds) throws Exception {
        // map the y-values in the training set.
        boolean labelsMapped = false;
        if (cachedLabels) {
            labelsMapped = stateMappings(trainData);
        }
        if (dict != null) dict.train(trainData,model.numStates());
        for (WordsInTrain d : otherDicts) {
            d.train(trainData, model.numStates());
        }
        boolean requiresTraining = false;
        for (int f = 0; f < features.size(); f++) {
            if (getFeature(f).requiresTraining()) {
                requiresTraining = true;
                break;
            }
        }
        if (requiresTraining) {
            for (trainData.startScan(); trainData.hasNext();) {
                DataSequence seq = trainData.next();
                for (int f = 0; f < features.size(); f++) {
                    if (getFeature(f).requiresTraining()) {
                        trainFeatureType(getFeature(f),seq);
                    }       
                }               
            }
        }
        if (collectIds) totalFeatures = featureMap.collectFeatureIdentifiers(trainData,maxMemory());
        return labelsMapped;
    };
    /**
     * @param featureType
     * @param seq
     */
    protected void trainFeatureType(FeatureTypes featureType, DataSequence seq) {
        for (int l = 0; l < seq.length(); l++) {
            // train each feature type.
            featureType.train(seq,l);
        }
    }
    /**
     * @param seq
     */
    public int addTrainRecord(DataSequence seq) {
        int numF = 0;
        for (int l = 0; l < seq.length(); l++) {
            for (startScanFeaturesAt(seq,l); hasNext(); numF++) {
                next();
            }
        }
        return numF;
    }
    public void printStats() {
        System.out.println("Num states " + model.numStates());
        System.out.println("Num edges " + model.numEdges());
        if (dict != null) System.out.println("Num words in dictionary " + dict.dictionaryLength());
        System.out.println("Num features " + numFeatures());
    }
    protected FeatureImpl nextNoId() {
        feature.copy(featureToReturn);
        advance(false);
        return feature;
    }
    protected void advance() {
        advance(!featureCollectMode);
    }
    protected void advance(boolean returnWithId) {
        while (true) {
            for (;((currentFeatureType == null) || !currentFeatureType.hasNext()) && featureIter.hasNext();) {
                currentFeatureType = featureIter.next();
            }
            if (!currentFeatureType.hasNext())
                break;
            while (currentFeatureType.hasNext()) {
                featureToReturn.init();
                copyNextFeature(featureToReturn);
                
                featureToReturn.id = featureMap.getId(featureToReturn);
                
                if (featureToReturn.id < 0){
                    continue;
                }
                if (featureValid(data, cposStart, cposEnd, featureToReturn, model, _fixedTransitions))
                    return;
                
            }
        }
        featureToReturn.id = -1;
    }
    /**
     * @param featureToReturn
     */
    protected void copyNextFeature(FeatureImpl featureToReturn) {
        currentFeatureType.next(featureToReturn);
    }
    /**
     * @param featureToReturn
     * @param cposEnd
     * @param cposStart
     * @param data
     * @return
     */
    public boolean featureValid(DataSequence data, int cposStart, int cposEnd, FeatureImpl featureToReturn, Model model, boolean cacheEdgeFeatures) {
        return featureValidStatic(data, cposStart, cposEnd, featureToReturn, model, cacheEdgeFeatures);
    }
    public static boolean featureValidStatic(DataSequence data, int cposStart, int cposEnd, FeatureImpl featureToReturn, Model model, boolean cacheEdgeFeatures) {
        if (((cposStart > 0) && (cposEnd < data.length()-1)) 
                || (featureToReturn.y() >= model.numStates())
                || (featureToReturn.yprev() >= model.numStates())
                || ((featureToReturn.yprev() >= 0) && cacheEdgeFeatures))
            return true;
        if ((cposStart == 0) && (model.isStartState(featureToReturn.y()))
                && ((data.length()>1) || (model.isEndState(featureToReturn.y())))) 
            return true;
        if ((cposEnd == data.length()-1) && (model.isEndState(featureToReturn.y())))
            return true;
        return false;
    }
    protected void initScanFeaturesAt(DataSequence d) {
        data = d;
        currentFeatureType = null;
        featureIter = features.iterator();
        advance();
    }
    public void startScanFeaturesAt(DataSequence d, int prev, int p) {
        cposEnd = p;
        cposStart = prev+1;
        for (int i = 0; i < features.size(); i++) {
            getFeature(i).startScanFeaturesAt(d,prev,cposEnd);
        }
        initScanFeaturesAt(d);
    }
    public void startScanFeaturesAtOnlyNonCached(DataSequence d, int prev, int p) {
        cposEnd = p;
        cposStart = prev+1;
        for (int i = 0; i < features.size(); i++) {
            if (!getFeature(i).needsCaching()) 
                getFeature(i).startScanFeaturesAt(d,prev,cposEnd);
        }
        initScanFeaturesAt(d);
    }
    public void startScanFeaturesAt(DataSequence d, int p) {
        cposEnd = p;
        cposStart = p;
        for (int i = 0; i < features.size(); i++) {
            getFeature(i).startScanFeaturesAt(d,cposEnd);
        }
        initScanFeaturesAt(d);
    }
    public void startScanFeaturesAtOnlyNonCached(DataSequence d, int p) {
        cposEnd = p;
        cposStart = p;
        for (int i = 0; i < features.size(); i++) {
            if (!getFeature(i).needsCaching()) 
                getFeature(i).startScanFeaturesAt(d,cposEnd);
        }
        initScanFeaturesAt(d);
    }
    public boolean hasNext() {
        return (featureToReturn.id >= 0);
    }
    
    public Feature next() {
        feature.copy(featureToReturn);
        advance();
       // System.out.println(feature);
        return feature;
    }
    public void freezeFeatures() {
        if (featureCollectMode)
            featureMap.freezeFeatures();
    }
    public int numFeatures() {
        return totalFeatures;
    }
    public FeatureIdentifier featureIdentifier(int id) {return featureMap.getIdentifier(id);}
    public String featureName(int featureIndex) {
        return featureMap.getName(featureIndex);
    }
    public int featureIndex(FeatureIdentifier fId) {return featureMap.getIndex(fId);}
    public int numStates() {
        return model.numStates();
    }
    public int label(int stateNum) {
        return (stateNum >= 0)?model.label(stateNum):stateNum;
    }
    protected int numFeatureTypes() {
        return features.size();
    }
    public void read(String fileName) throws IOException {
        BufferedReader in=new BufferedReader(new FileReader(fileName));
        if (dict != null) dict.read(in, model.numStates());
        totalFeatures = featureMap.read(in);
    }
    public void write(String fileName) throws IOException {
        PrintWriter out=new PrintWriter(new FileOutputStream(fileName));
        if (dict != null) dict.write(out);
        featureMap.write(out);
        out.close();
    }
    public void displayModel(double featureWts[]) throws IOException {
        displayModel(featureWts,System.out);
    }
    public void displayModel(double featureWts[], PrintStream out) throws IOException {
        displayModel(featureWts, out, false);
    }
    public void displayModel(double featureWts[], PrintStream out, boolean origFName) throws IOException {
        int numF = numFeatures();
        for (int fIndex = 0; fIndex < numF; fIndex++) {
            Object feature = featureIdentifier(fIndex).name;
            int classIndex = featureIdentifier(fIndex).stateId;
            int label = model.label(classIndex);
            if (!origFName)
                out.println(feature + " " + label + " " + classIndex + " " + featureWts[fIndex]);
            else
                out.println(featureName(fIndex) + " "+featureWts[fIndex]);
        }
        /*
         out.println("Feature types statistics");
         for (int f = 0; f < features.size(); f++) {
         getFeature(f).print(featureMap, featureWts);
         }
         */
    }
    
    public boolean fixedTransitionFeatures() {
        return _fixedTransitions;
    }
    // returns the label-independent featureId of the current feature
    public int xFeatureIdCurrent() {
        return currentFeatureType.labelIndependentId(featureToReturn);
    }
    public void addDict(WordsInTrain ngramDict) {
        otherDicts.add(ngramDict);
    }
};
