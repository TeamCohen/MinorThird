package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;

/**
 * Train a CMM (in batch mode).
 *
 * @author William Cohen
 */

public class CMMLearner implements BatchSequenceClassifierLearner
{
	private ClassifierLearner baseLearner;
	private int historySize;

	@Override
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

	@Override
	public void setSchema(ExampleSchema schema) {;}

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		baseLearner.reset();
		baseLearner.setSchema( schema );
		dataset.setHistorySize(historySize);
		for (Iterator<Example> i=dataset.iterator(); i.hasNext(); ) {
			Example e = i.next();
			baseLearner.addExample( e );
		}
		Classifier classifier = baseLearner.getClassifier();
		return new CMM(classifier,historySize,schema);
	}
}

