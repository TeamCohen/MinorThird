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
	private BinaryClassifierLearner[] innerLearner = null;
	private ExampleSchema schema;

	public OneVsAllLearner()
	{
		this(new ClassifierLearnerFactory("new VotedPerceptron()"));
	}

	/** 
	 * @param ClassifierLearnerFactory should produce a BinaryClassifier with each call.
	 */
	public OneVsAllLearner(ClassifierLearnerFactory learnerFactory)
	{
		this.learnerFactory = learnerFactory;
	}
	public void setSchema(ExampleSchema schema) 
	{
		this.schema = schema;
		innerLearner = new BinaryClassifierLearner[schema.getNumberOfClasses()];
		for (int i=0; i<innerLearner.length; i++) {
			innerLearner[i] = (BinaryClassifierLearner)learnerFactory.getLearner();
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
			int label = classIndex==i ? +1 : -1;
			innerLearner[i].addExample( new BinaryExample( answeredQuery.asInstance(), label ) );
		}
	}

	public Classifier getClassifier()
	{
		BinaryClassifier[] classifiers = new BinaryClassifier[ innerLearner.length ];
		for (int i=0; i<innerLearner.length; i++) {
			classifiers[i] = innerLearner[i].getBinaryClassifier();
		}
		return new OneVsAllClassifier( schema.validClassNames(), classifiers );
	}

}
