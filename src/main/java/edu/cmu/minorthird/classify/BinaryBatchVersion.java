package edu.cmu.minorthird.classify;



/**
 * Batch version of an OnlineBinaryClassifierLearner
 *
 * @author William Cohen
 */

public class BinaryBatchVersion extends BatchVersion implements BinaryClassifierLearner
{
	public BinaryBatchVersion(OnlineBinaryClassifierLearner innerLearner,int numberOfEpochs)
	{
		super(innerLearner,numberOfEpochs);
	}
	public BinaryBatchVersion(OnlineBinaryClassifierLearner innerLearner)
	{
		super(innerLearner);
	}

	@Override
	final public BinaryClassifier getBinaryClassifier()
	{
		return (BinaryClassifier)getClassifier();
	}

}
