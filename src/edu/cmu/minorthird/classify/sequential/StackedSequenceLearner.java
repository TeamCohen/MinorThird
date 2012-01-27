package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.transform.AugmentedInstance;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * @author William Cohen
 */
public class StackedSequenceLearner implements BatchSequenceClassifierLearner
{
	private static Logger log = Logger.getLogger(StackedSequenceLearner.class);

	private SequenceClassifierLearner baseLearner;
	private StackingParams params;

	/** Bundle of parameters for the StackedSequenceLearner. */
	public static class StackingParams {
		public int historySize=5, futureSize=5, stackingDepth=1;
		public boolean useLogistic=true,useTargetPrediction=true,useConfidence=true;
		public Splitter<Example[]> splitter=new CrossValSplitter<Example[]>(5);
		int crossValSplits=5;

		/** Number of instances before the current target for which the
		 * predicted class will be used as a feature. */
		public int getHistorySize() { return historySize; }
		public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }

		/** Number of instances after the current target for which the
		 * predicted class will be used as a feature. */
		public int getFutureSize() { return futureSize; }
		public void setFutureSize(int newFutureSize) { this.futureSize = newFutureSize; }
		
		/** If true, adjust all class confidences by passing them thru a logistic function */
		public boolean getUseLogisticOnConfidences() { return useLogistic; }
		public void setUseLogisticOnConfidences(boolean flag) { useLogistic=flag; }

		/** If true, use confidence in class predictions as weight for that feature. */
		public boolean getUseConfidences() { return useConfidence; }
		public void setUseConfidences(boolean flag) { useConfidence=flag; }

		/** If true, adjust all class confidences by passing them thru a logistic function */
		public boolean getUseTargetPrediction() { return useTargetPrediction; }
		public void setUseTargetPrediction(boolean flag) { useTargetPrediction=flag; }

		/** Number of iterations of stacking to use */
		public int getStackingDepth() { return stackingDepth; }
		public void setStackingDepth(int newStackingDepth) { this.stackingDepth = newStackingDepth; }

		/* Number of cross-validation splits to use in making predictions */
		public int getCrossValSplits() { return crossValSplits; }
		public void setCrossValSplits(int newCrossValSplits) { 
			this.splitter = new CrossValSplitter<Example[]>(newCrossValSplits);
			crossValSplits = newCrossValSplits;
		}
	}
	
	/** Number of instances before the current target for which the
	 * predicted class will be used as a feature. */
	@Override
	public int getHistorySize() { return params.historySize; }
	public void setHistorySize(int newHistorySize) { params.setHistorySize(newHistorySize); }

	public StackingParams getParams() { return params; }

	public StackedSequenceLearner()
	{
		this.baseLearner = new CMMLearner(new VotedPerceptron(),0);
		this.params = new StackingParams();
	}

 	public StackedSequenceLearner(SequenceClassifierLearner baseLearner,int depth)
	{
		this();
		this.baseLearner = baseLearner;
		params.setStackingDepth(depth);
	}

 	public StackedSequenceLearner(ClassifierLearner baseLearner,int depth)
	{
		this();
		this.baseLearner = new CMMLearner(baseLearner,0);
		params.setStackingDepth(depth);
	}

 	public StackedSequenceLearner(SequenceClassifierLearner baseLearner,int depth,int windowSize)
	{
		this();
		this.baseLearner = baseLearner;
		params.setStackingDepth(depth);
		params.setHistorySize(windowSize);
		params.setFutureSize(windowSize);
	}

 	public StackedSequenceLearner(ClassifierLearner baseLearner,int depth,int windowSize)
	{
		this();
		this.baseLearner = new CMMLearner(baseLearner,0);
		params.setStackingDepth(depth);
		params.setHistorySize(windowSize);
		params.setFutureSize(windowSize);
	}

	@Override
	public void setSchema(ExampleSchema schema) {;}

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		SequenceClassifier[] m = new SequenceClassifier[params.stackingDepth+1];
		SequenceDataset stackedDataset = dataset;
		stackedDataset.setHistorySize(0);
		ProgressCounter pc = new ProgressCounter("training stacked learner","stacking level",params.stackingDepth+1);
		for (int d=0; d<=params.stackingDepth; d++) {
			m[d] = new DatasetSequenceClassifierTeacher(stackedDataset).train(baseLearner);
			if (d+1 <= params.stackingDepth) {
				stackedDataset = stackDataset(stackedDataset);
				//new ViewerFrame("Dataset "+(d+1), new SmartVanillaViewer(stackedDataset));
			}
			pc.progress();
		}
		pc.finished();
		return new StackedSequenceClassifier(m, params);
	}

	/**
	 * Create a new dataset in which each instance has been augmented
	 * with the history features constructed from the *predicted* labels
	 * of previous examples, where the prediction is made using
	 * cross-validation.
	 */
	public SequenceDataset stackDataset(SequenceDataset dataset)
	{
//		String[] history = new String[params.historySize];
		SequenceDataset result = new SequenceDataset();
				
		Dataset.Split s = dataset.splitSequence(params.splitter);
//		ExampleSchema schema = dataset.getSchema();
		ProgressCounter pc = new ProgressCounter("labeling for stacking","fold",s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			SequenceDataset trainData = (SequenceDataset)s.getTrain(k);
			SequenceDataset testData = (SequenceDataset)s.getTest(k);
			log.info("splitting with "+params.splitter+", preparing to train on "+trainData.size()
							 +" and test on "+testData.size());
			SequenceClassifier c = new DatasetSequenceClassifierTeacher(trainData).train(baseLearner);
			for (Iterator<Example[]> i=testData.sequenceIterator(); i.hasNext(); ) {
				Example[] seq = i.next();
				ClassLabel[] labels = c.classification(seq);
				Example[] stackSeq = new Example[seq.length];
				for (int j=0; j<seq.length; j++) {
					//System.out.println("stackSeq["+j+"]="+stackSeq[j]);
					Instance stackInstance = stackInstance(j,seq[j].asInstance(),labels,params);
					stackSeq[j] = new Example(stackInstance,seq[j].getLabel());
				}
				result.addSequence( stackSeq );
			}
			log.info("splitting with "+params.splitter+", stored classified dataset");
			pc.progress();
		}
		pc.finished();
		result.setHistorySize(0);
		return result;
	}

	static private Instance stackInstance(int j,Instance instancej,ClassLabel[] labels,StackingParams params)
	{
		int numNewFeatures = params.historySize+params.futureSize+(params.useTargetPrediction?1:0);
		String[] features = new String[numNewFeatures];
		double[] values = new double[numNewFeatures];
		int index=0;
		for (int m=j-params.historySize; m<=j+params.futureSize; m++) {
			if (m!=j || params.useTargetPrediction) { 
				if (m>=0 && m<labels.length) {
					features[index] = stackFeatureName(m-j,labels[m].bestClassName());
					values[index] = 1.0;
					if (params.useConfidence) {
						double w = labels[m].bestWeight();
						values[index] = params.useLogistic ? MathUtil.logistic(w) : w;
					}
				} else {
					features[index] = stackFeatureName(m-j,"NULL");
					values[index] = 1.0;
				}
				index++;
			}
		}
		return new AugmentedInstance(instancej,features,values);
	}
	
	private static String stackFeatureName(int offsetFromTarget,String predictedClassName)
	{
		if (offsetFromTarget<0) return "pred.prev."+(-offsetFromTarget)+"."+predictedClassName;
		else if (offsetFromTarget>0) return "pred.next."+offsetFromTarget+"."+predictedClassName;
		else return "pred.here."+predictedClassName;
	}

	private class StackedSequenceClassifier implements SequenceClassifier,Visible
	{
		private SequenceClassifier[] m; 
//		private ExampleSchema schema;
		private StackingParams params;

		public StackedSequenceClassifier(SequenceClassifier[] m, StackingParams params) 
		{ 
			this.m = m; 
			this.params = params;
		}

		@Override
		public ClassLabel[] classification(Instance[] sequence)
		{
//			String[] history = new String[params.historySize];
			ClassLabel[] labels = m[0].classification(sequence);
			Instance[] augmentedSequence = new Instance[sequence.length];
			for (int d=1; d<m.length; d++) {
				// augment the examples with context from the last classifier
				for (int j=0; j<sequence.length; j++) {			
					augmentedSequence[j] = stackInstance(j, sequence[j], labels, params);
				}
				// label the augmented examples
				labels = m[d].classification(augmentedSequence);
			}
			return labels;
		}
		@Override
		public String explain(Instance[] sequence)
		{
			return "not implemented";
		}

	    @Override
			public Explanation getExplanation(Instance[] sequence) {
		Explanation ex = new Explanation(explain(sequence));
		return ex;
	    }

		@Override
		public Viewer toGUI()
		{
			ParallelViewer v = new ParallelViewer();
			for (int i=0; i<m.length; i++) {
				final int k = i;
				v.addSubView( 
					"Level "+k+" classifier",
					new TransformedViewer( new SmartVanillaViewer(m[k]) ) {
						static final long serialVersionUID=20080207L;
						@Override
						public Object transform(Object o) {
							StackedSequenceClassifier s = (StackedSequenceClassifier)o;
							return s.m[k];
						}});
			}
			v.setContent(this);
			return v;
		}
	}
}

