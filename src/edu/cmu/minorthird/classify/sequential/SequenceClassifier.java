package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Instance;

/**
 * Interface for a sequence classifier.
 *
 * @author William Cohen
 */

public interface SequenceClassifier
{
	/** Return a predicted type for each element of the sequence. */
	public ClassLabel[] classification(Instance[] sequence);

	/** Return some string that 'explains' the classification */
	public String explain(Instance[] sequence);
}

