package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;

/**
 * Train a CMM (in batch mode).
 *
 * @author William Cohen
 */

public class CMMLearner implements BatchSequenceClassifierLearner
{
	private ClassifierLearner baseLearner;
	private int historySize;

	public int getHistorySize() { return historySize; }
	public void setHistorySize(int newHistorySize) { this.historySize = newHistorySize; }

	public CMMLearner()
	{
		this(new SVMLearner(),3);
	}

	public CMMLearner(ClassifierLearner baseLearner,int historySize)
	{
		this.baseLearner = baseLearner;
		this.historySize = historySize;
	}

	public void setSchema(ExampleSchema schema) {;}

	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		baseLearner.reset();
		baseLearner.setSchema( schema );
		dataset.setHistorySize(historySize);
		for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			baseLearner.addExample( e );
		}
		Classifier classifier = baseLearner.getClassifier();
		return new CMM(classifier,historySize,schema);
	}
}

