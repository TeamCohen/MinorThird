package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.*;
import org.apache.log4j.Logger;

/** 
 * View result of some sort of train/test experiment.
 *
 * @author William Cohen
 */

public class CrossValidatedDataset implements Visible
{
	static private Logger log = Logger.getLogger(CrossValidatedDataset.class);

	private ClassifiedDataset[] cds;
	private ClassifiedDataset[] trainCds;
	private Evaluation v;

	public CrossValidatedDataset(ClassifierLearner learner,Dataset d,Splitter<Example> splitter)
	{
		this(learner,d,splitter,false);
	}

	public CrossValidatedDataset(ClassifierLearner learner,Dataset d,Splitter<Example> splitter,boolean saveTrainPartitions)
	{
		Dataset.Split s = d.split(splitter);
		cds = new ClassifiedDataset[s.getNumPartitions()];
		trainCds = saveTrainPartitions ? new ClassifiedDataset[s.getNumPartitions()] : null;
		v = new Evaluation(d.getSchema());
		ProgressCounter pc = new ProgressCounter("train/test","fold",s.getNumPartitions());
		log.info("Number of splits: "+s.getNumPartitions());
		for (int k=0; k<s.getNumPartitions(); k++) {
			Dataset trainData = s.getTrain(k);
			Dataset testData = s.getTest(k);
			log.info("Split with "+splitter+": training on "+trainData.size()+" and testing on "+testData.size());
			Classifier c = new DatasetClassifierTeacher(trainData).train(learner);
			DatasetIndex testIndex = new DatasetIndex(testData);
			cds[k] = new ClassifiedDataset(c, testData, testIndex);
			if (trainCds!=null) trainCds[k] = new ClassifiedDataset(c, trainData, testIndex);
			v.extend(cds[k].getClassifier(),testData,k);
			v.setProperty("classesInFold"+(k+1),
										"train: "+classDistributionString(trainData.getSchema(),new DatasetIndex(trainData))
										+"     test: "+classDistributionString(testData.getSchema(),testIndex));
			log.info("Stored classified dataset");
			pc.progress();
		}
		pc.finished();
	}

	private String classDistributionString(ExampleSchema schema, DatasetIndex index)
	{
		StringBuffer buf = new StringBuffer(""); 
		java.text.DecimalFormat fmt = new java.text.DecimalFormat("#####");
		for (int i=0; i<schema.getNumberOfClasses(); i++) {
			if (buf.length()>0) buf.append("; "); 
			String label = schema.getClassName(i);
			buf.append(fmt.format(index.size(label)) + " " + label);
		}
		return buf.toString();
	}

	@Override
	public Viewer toGUI()
	{
		ParallelViewer main = new ParallelViewer();
		for (int i=0; i<cds.length; i++) {
			final int k = i;
			main.addSubView(
				"Test Partition "+(i+1), 
				new TransformedViewer(cds[0].toGUI()) {
					static final long serialVersionUID=20080130L;
					@Override
					public Object transform(Object o) {
					//what is this for? - frank
						//CrossValidatedDataset cvd = (CrossValidatedDataset)o;
						return cds[k];
					}});
		}
		if (trainCds!=null) {
			for (int i=0; i<trainCds.length; i++) {
				final int k = i;
				main.addSubView(
					"Train Partition "+(i+1), 
					new TransformedViewer(cds[0].toGUI()) {
						static final long serialVersionUID=20080130L;
						@Override
						public Object transform(Object o) {
							//what is this for? - frank
							//CrossValidatedDataset cvd = (CrossValidatedDataset)o;
							return trainCds[k];
						}});
			}
		}
		main.addSubView(
			"Overall Evaluation", 
			new TransformedViewer(v.toGUI()) {
				static final long serialVersionUID=20080130L;
				@Override
				public Object transform(Object o) {
					CrossValidatedDataset cvd = (CrossValidatedDataset)o;												
					return cvd.v;
				}
			});
		main.setContent(this);
		return main;
	}

	public Evaluation getEvaluation() { return v; }

	public static void main(String[] args)
	{
		Dataset train = SampleDatasets.sampleData("toy",false);
		ClassifierLearner learner = new DecisionTreeLearner();
		//ClassifierLearner learner = new NaiveBayes();
		CrossValidatedDataset cd = new CrossValidatedDataset(learner,train,new CrossValSplitter<Example>(3),true);
		new ViewerFrame("CrossValidatedDataset", cd.toGUI());
	}
	
}
