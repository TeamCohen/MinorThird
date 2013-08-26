package edu.cmu.minorthird.classify.sequential;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Sequential learner based on the CRF algorithm.  Source for the iitb.CRF
 * package available from http://crf.sourceforge.net.
 *
 * @author Sunita Sarawagi
 */

public class CRFLearner 
implements BatchSequenceClassifierLearner,SequenceConstants,SequenceClassifier,Visible,Serializable{
	static private final long serialVersionUID = 1;

	int histsize = 1;
	ExampleSchema schema;
	iitb.CRF.CRF crfModel;
	java.util.Properties defaults;
	java.util.Properties options;

	private static final boolean CONVERT_TO_MINORTHIRD_HYPERPLANE=true;

	public CRFLearner()
	{
		defaults = new java.util.Properties();
		defaults.setProperty("modelGraph", "naive");
		defaults.setProperty("debugLvl", "1");
		//defaults.setProperty("trainer", "ll");
		options = defaults;
	}
	public CRFLearner(String args) {
		this(args,1);
	}
	public CRFLearner(String args, int histsize) {
		this();
		this.histsize = histsize;
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
	public void setLogSpaceOption() {
		options.setProperty("trainer", "ll"); //option for german multi data (very large dataset!)
	}
	public void removeLogSpaceOption() {
		options.remove("trainer");
	}

	@Override
	public void setSchema(ExampleSchema schema){
		this.schema=schema;
	}
	
	public ExampleSchema getSchema(){
		return schema;
	}

	@Override
	public int getHistorySize() {return histsize;}

	public void setMaxIters (int newMaxIters) {
		defaults.setProperty("maxIters", Integer.toString(newMaxIters));
	}
	public int getMaxIters () {
		String maxIters = defaults.getProperty("maxIters");
		if (maxIters != null)
			return Integer.parseInt(maxIters);
		return 100;
	}
	public String maxItersHelp = new String("Number of training iterations over the training set; default set to 100");
	public String getMaxItersHelp() { return maxItersHelp; }

	public boolean getUseHighPrecisionArithmetic() {
		String value = defaults.getProperty("trainer");
		if ((value != null) && (value.equals("ll")))
			return true;
		return false;
	}
	public void setUseHighPrecisionArithmetic (boolean newUseHighPrecisionArithmetic) {
		if (newUseHighPrecisionArithmetic == true)
			this.setLogSpaceOption();
		else
			this.removeLogSpaceOption();
	}
	public String useHighPrecisionArithmeticHelp = new String("Make the learner use high precision arithmetic.");
	public String getUseHighPrecisionArithmeticHelp() { return useHighPrecisionArithmeticHelp; }

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
		@Override
		public int length() {
			return sequence.length;
		}
		@Override
		public int y(int i) {
			return labels[i];
		}
		@Override
		public Object x(int i) {
			return sequence[i];
		}
		@Override
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
		Iterator<Example[]> iter;
		SequenceDataset dataset;
		TrainDataSequenceC sequence;
		int dataSize;
		CRFDataIter(SequenceDataset ds) {
			dataset = ds;
			dataSize = ds.size();
			sequence = new TrainDataSequenceC();
		}
		@Override
		public void startScan() {
			iter=dataset.sequenceIterator();
		}
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}
		@Override
		public iitb.CRF.DataSequence next() {
			sequence.init(iter.next());
			return sequence;
		}
	};

	class  MTFeatureTypes extends iitb.Model.FeatureTypes {
		static final long serialVersionUID=20080207L;
		Iterator<Feature> featureLooper;
		Feature feature;
		int numStates;
		Instance example;
		int stateId;
		MTFeatureTypes(iitb.Model.FeatureGenImpl gen) {
			super(gen);
			numStates = model.numStates();
		}
		void advance() {
			stateId++;
			if (stateId < numStates)
				return;
			if (featureLooper.hasNext()) {
				feature = featureLooper.next();
				stateId = 0;
			} else {
				feature = null;
				featureLooper=null;
			}
		}
		boolean startScan() {
			stateId = -1;
			if (featureLooper.hasNext()) {
				feature = featureLooper.next();
				advance();
			} else {
				feature = null;
				return false;
			}
			return true;
		}
		@Override
		public  boolean startScanFeaturesAt(iitb.CRF.DataSequence data, int prevPos, int pos) {
			example = (Instance)data.x(pos);
			featureLooper = example.featureIterator();
			return startScan();
		}
		@Override
		public boolean hasNext() {
			return ((stateId  < numStates) && (feature != null));
		}
		@Override
		public  void next(iitb.Model.FeatureImpl f) {
			f.yend = stateId;
			f.ystart = -1;
			f.val = (float)example.getWeight(feature);
			setFeatureIdentifier(feature.getID()*numStates+stateId, stateId, feature,f);
			advance();
		}
	};

	public class MTFeatureGenImpl extends iitb.Model.FeatureGenImpl 
	{
		
		static final long serialVersionUID=20080207L;
		
		public MTFeatureGenImpl(String modelSpecs, int numLabels, String[] labelNames) throws Exception 
		{
			super(modelSpecs,numLabels,false);

			Feature features[] = new Feature[labelNames.length];
			for (int i = 0; i < labelNames.length; i++) {
				features[i] = new Feature(new String[]{ HISTORY_FEATURE, "1", labelNames[i]});
			}
			addFeature(new iitb.Model.EdgeFeatures(this, features));
			addFeature(new iitb.Model.StartFeatures(this, new Feature(new String[]{ HISTORY_FEATURE, "1", NULL_CLASS_NAME})));

			//wwc: I don't think this feature should be used for minorthird....
			//addFeature(new iitb.Model.EndFeatures(model, new Feature("E")));

			if (histsize > 1) {
				//uncomment this for all n-gram history features, 
				//addFeature(new iitb.Model.EdgeHistFeatures(model, HISTORY_FEATURE,labelNames,histsize));

				// this is for minorthird style linear history features...
				Feature histFeatures[][] = new Feature[histsize][labelNames.length];
				for (int k = 1; k < histsize; k++) {
					for (int i = 0; i < labelNames.length; i++)
						histFeatures[k][i] = new Feature(new String[]{ HISTORY_FEATURE, Integer.toString((k+1)), labelNames[i]});
				}		
				addFeature(new iitb.Model.EdgeLinearHistFeatures(this, histFeatures, histsize));		
			}
			addFeature(new MTFeatureTypes(this));
		}
	};

	iitb.Model.FeatureGenImpl featureGen;
	SequenceClassifier cmmClassifier = null;    
	double[] crfWs;

	iitb.CRF.DataIter  allocModel(SequenceDataset dataset) throws Exception {
		featureGen = new MTFeatureGenImpl(options.getProperty("modelGraph"),schema.getNumberOfClasses(),schema.validClassNames());
		//options.setProperty("trainer", "ll"); //option for german multi data (very large dataset!)
		System.out.println("Property: " + options.getProperty("trainer"));
		crfModel = new iitb.CRF.CRF(featureGen.numStates(),histsize,featureGen,options);	
		return new CRFDataIter(dataset);
	}

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		try {
			schema = dataset.getSchema();
			return doTrain(allocModel(dataset));
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("error in CRF: "+e);
		}
	}

	SequenceClassifier doTrain(iitb.CRF.DataIter trainData) throws Exception 
	{
		featureGen.train(trainData);
		ProgressCounter pc = new ProgressCounter("training CRF","iteration");
		crfWs = crfModel.train(trainData);
		pc.finished();

		if (CONVERT_TO_MINORTHIRD_HYPERPLANE) return toMinorthirdClassifier();
		else return this;

	}


	private SequenceClassifier toMinorthirdClassifier()
	{
		Hyperplane[] w_t;
		int numClasses = schema.getNumberOfClasses();
		w_t = new Hyperplane[numClasses];
		for (int i=0; i<numClasses; i++) {
			w_t[i] = new Hyperplane();
			w_t[i].setBias(0);
		}
		for (int fIndex = 0; fIndex < crfWs.length; fIndex++) {
			Feature feature = (Feature)featureGen.featureIdentifier(fIndex).name;
			int classIndex = featureGen.featureIdentifier(fIndex).stateId;
			w_t[classIndex].increment(feature,crfWs[fIndex]);
		}
		return new CMM(new SequenceUtils.MultiClassClassifier(schema,w_t), histsize, schema );	 
	}

	/** Return a predicted type for each element of the sequence. */
	@Override
	public ClassLabel[] classification(Instance[] sequence) {
		TestDataSequenceC seq = new TestDataSequenceC(sequence);
		crfModel.apply(seq);
		featureGen.mapStatesToLabels(seq);
		return seq.getLabels();
	}

	/** Return some string that 'explains' the classification */
	@Override
	public String explain(Instance[] sequence) {
		if (cmmClassifier==null) cmmClassifier = toMinorthirdClassifier();
		return cmmClassifier.explain(sequence);
	}

	@Override
	public Explanation getExplanation(Instance[] sequence) {
		if (cmmClassifier==null) cmmClassifier = toMinorthirdClassifier();
		Explanation.Node top = new Explanation.Node("CRF Explanation");
		Explanation.Node cmmEx = cmmClassifier.getExplanation(sequence).getTopNode();
		if(cmmEx == null)
			cmmEx = new Explanation.Node(cmmClassifier.explain(sequence));
		top.add(cmmEx);
		Explanation ex = new Explanation(top);
		return ex;
	}

	@Override
	public Viewer toGUI()
	{
		Viewer v = new ComponentViewer() {
			static final long serialVersionUID=20080207L;
			@Override
			public JComponent componentFor(Object o) {
//				CRFLearner cmm = (CRFLearner)o;
				JPanel mainPanel = new JPanel();
				mainPanel.setLayout(new BorderLayout());
				mainPanel.add(
						new JLabel("CRFLearner: historySize=1"),
						BorderLayout.NORTH);
				Viewer subView = new SmartVanillaViewer(toMinorthirdClassifier());
				subView.setSuperView(this);
				mainPanel.add(subView,BorderLayout.SOUTH);
				mainPanel.setBorder(new TitledBorder("CRFLearner"));
				return new JScrollPane(mainPanel);
			}
		};
		v.setContent(this);
		return v;
	}

}
