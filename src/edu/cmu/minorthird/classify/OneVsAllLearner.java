package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;

/**
 * Multi-class version of a binary classifier.
 *
 * @author William Cohen
 */

public class OneVsAllLearner implements ClassifierLearner
{
	private ClassifierLearnerFactory learnerFactory;
	private ClassifierLearner[] innerLearner = null;
	private ExampleSchema schema;

	public OneVsAllLearner()
	{
		this(new ClassifierLearnerFactory("new VotedPerceptron()"));
	}

	/** 
	 * @param learnerFactory a ClassifierLearnerFactory which should produce a BinaryClassifier with each call.
	 */
	public OneVsAllLearner(ClassifierLearnerFactory learnerFactory)
	{
		this.learnerFactory = learnerFactory;
	}
	public void setSchema(ExampleSchema schema) 
	{
		this.schema = schema;
		innerLearner = new ClassifierLearner[schema.getNumberOfClasses()];
		for (int i=0; i<innerLearner.length; i++) {
			innerLearner[i] = (ClassifierLearner)learnerFactory.getLearner();
			innerLearner[i].setSchema( ExampleSchema.BINARY_EXAMPLE_SCHEMA );
		}
	}
	public void reset()
	{
		if (innerLearner!=null) {
			for (int i=0; i<innerLearner.length; i++) {
				innerLearner[i].reset();
			}
		}
	}
	public void setInstancePool(Instance.Looper looper) 
	{
		ArrayList list = new ArrayList();
		while (looper.hasNext()) list.add(looper.next());
		for (int i=0; i<innerLearner.length; i++) {
			innerLearner[i].setInstancePool( new Instance.Looper(list) );
		}
	}
	public boolean hasNextQuery()
	{
		for (int i=0; i<innerLearner.length; i++) {
			if (innerLearner[i].hasNextQuery()) return true;
		}
		return false;
	}

	public Instance nextQuery()
	{
		for (int i=0; i<innerLearner.length; i++) {
			if (innerLearner[i].hasNextQuery()) return innerLearner[i].nextQuery();
		}
		return null;
	}

	public void addExample(Example answeredQuery)
	{
		int classIndex = schema.getClassIndex( answeredQuery.getLabel().bestClassName() );
		for (int i=0; i<innerLearner.length; i++) {
			ClassLabel label = classIndex==i ? ClassLabel.positiveLabel(1.0) : ClassLabel.negativeLabel(-1.0);
			innerLearner[i].addExample( new Example( answeredQuery.asInstance(), label ) );
		}
	}

	public void completeTraining()
	{
		for (int i=0; i<innerLearner.length; i++) {
			innerLearner[i].completeTraining();
		}
	}

	public Classifier getClassifier()
	{
		Classifier[] classifiers = new Classifier[ innerLearner.length ];
		for (int i=0; i<innerLearner.length; i++) {
			classifiers[i] = innerLearner[i].getClassifier();
		}
		return new OneVsAllClassifier( schema.validClassNames(), classifiers );
	}

}
