package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.Serializable;
import java.util.Iterator;
import java.util.*;

/**
 * Sequential learner based on the CRF algorithm
 *
 * @author Sunita Sarawagi
 */

public class CRFLearner implements BatchSequenceClassifierLearner,SequenceConstants,SequenceClassifier
{
    ExampleSchema schema;
    iitb.CRF.CRF crfModel;
    java.util.Properties defaults;
    java.util.Properties options;
    public CRFLearner()
    {
	defaults = new java.util.Properties();
	defaults.setProperty("modelGraph", "naive");
	defaults.setProperty("debugLvl", "0");
	options = defaults;
    }
    public CRFLearner(int histsize, int epoch) {this();}
    public CRFLearner(String args) {
	this();
	StringTokenizer argTok = new StringTokenizer(args, " ");
	options = new java.util.Properties(defaults);
	while (argTok.hasMoreTokens()) {
	    options.setProperty(argTok.nextToken(),argTok.nextToken());
	}
    }
    public CRFLearner(String args[]) {
	this();
	options = new java.util.Properties(defaults);
	for (int i = 0; i < args.length-1; i+=2) {
	    options.setProperty(args[i], args[i+1]);
	}
    }
    public void setSchema(ExampleSchema sch) {;}
    
    public int getHistorySize() {return 1;}
    class DataSequenceC implements iitb.CRF.DataSequence {
	Instance[] sequence;
	int labels[];
	void init(Instance[] tokens) {
	    sequence = tokens;
	    if (tokens != null) {
		if ((labels == null) || (tokens.length > labels.length)) {
		    labels = new int[tokens.length];
		}
	    }
	}
	public int length() {
	    return sequence.length;
	}
	public int y(int i) {
	    return labels[i];
	}
	public Object x(int i) {
	    return sequence[i];
	}
	public void set_y(int i, int label) {
	    labels[i] = label;
	}
    };

    class TrainDataSequenceC extends DataSequenceC {
	void init(Example[] tokens) {
	    super.init(tokens);
	    if (tokens != null) {
		for (int i = 0; i < sequence.length; i++) {
		    labels[i] = schema.getClassIndex(tokens[i].getLabel().bestClassName());
		}
	    }
	}
    };
    class TestDataSequenceC extends DataSequenceC {
	TestDataSequenceC(Instance[] tokens) {
	    init(tokens);
	}
	ClassLabel[] getLabels() {	
	    ClassLabel[] clabels = new ClassLabel[sequence.length];
	    for (int i = 0; i < sequence.length; i++) {
		clabels[i] = new ClassLabel(schema.getClassName(labels[i]));
	    }
	    return clabels;
	}
    };
    class CRFDataIter implements iitb.CRF.DataIter {
	Iterator iter;
	SequenceDataset dataset;
	TrainDataSequenceC sequence;
	CRFDataIter(SequenceDataset ds) {
	    dataset = ds;
	    sequence = new TrainDataSequenceC();
	}
	public void startScan() {
	    iter=dataset.sequenceIterator();
	}
	public boolean hasNext() {
	    return iter.hasNext();
	}
	public iitb.CRF.DataSequence next() {
	    sequence.init((Example[])iter.next());
	    return sequence;
	}
    };

    class  MTFeatureTypes extends iitb.Model.FeatureTypes {
	Feature.Looper featureLooper;
	Feature feature;
	int numStates;
	Instance example;
	int stateId;
	MTFeatureTypes(iitb.Model.Model m) {
	    super(m);
	    numStates = model.numStates();
	}
	void advance() {
	    stateId++;
	    if (stateId < numStates)
		return;
	    if (featureLooper.hasNext()) {
		feature = featureLooper.nextFeature();
		stateId = 0;
	    } else {
		feature = null;
		featureLooper=null;
	    }
	}
	public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
	    stateId = -1;
	    example = (Instance)data.x(pos);
	    featureLooper = example.featureIterator();
	    if (featureLooper.hasNext()) {
		feature = featureLooper.nextFeature();
		advance();
	    } else {
		feature = null;
		return false;
	    }
	    return true;
	}

	public boolean hasNext() {
	    return ((stateId  < numStates) && (feature != null));
	}
	public  void next(iitb.Model.FeatureImpl f) {
	    f.id = (feature.numericName()-1)*numStates + stateId;
	    f.yend = stateId;
	    f.ystart = -1;
	    f.val = (float)example.getWeight(feature);
	    f.type = "MT";
	    f.strId = feature.toString()+"_in_"+stateId;
	    advance();
	}
    };
    
    public class MTFeatureGenImpl extends iitb.Model.FeatureGenImpl {
	public MTFeatureGenImpl(String modelSpecs, int numLabels) throws Exception {
	    super(modelSpecs,numLabels,false);
	    addFeature(new iitb.Model.EdgeFeatures(model));
	    addFeature(new iitb.Model.StartFeatures(model));
	    addFeature(new iitb.Model.EndFeatures(model));
	    addFeature(new MTFeatureTypes(model));
	}
    };
    
    MTFeatureGenImpl featureGen;
    public SequenceClassifier batchTrain(SequenceDataset dataset)
    {
	try {
	    schema = dataset.getSchema();
	    featureGen = new MTFeatureGenImpl(options.getProperty("modelGraph"),schema.getNumberOfClasses());
	    iitb.CRF.DataIter trainData = new CRFDataIter(dataset);
	    featureGen.train(trainData);
	    crfModel = new iitb.CRF.CRF(featureGen.numStates(),featureGen,options);
	    crfModel.train(trainData);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return this;
    }
    
    /** Return a predicted type for each element of the sequence. */
    public ClassLabel[] classification(Instance[] sequence) {
	TestDataSequenceC seq = new TestDataSequenceC(sequence);
	crfModel.apply(seq);
	featureGen.mapStatesToLabels(seq);
	return seq.getLabels();
    }
    
    /** Return some string that 'explains' the classification */
    public String explain(Instance[] sequence) {
	return "";
    }
};
