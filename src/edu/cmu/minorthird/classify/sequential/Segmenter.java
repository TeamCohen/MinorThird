package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Instance;

/**
 * @author William Cohen
 */

public interface Segmenter
{
	/** Return a predicted type for each element of the sequence. */
	public Segmentation segmentation(CandidateSegmentGroup group);

	/** Return some string that 'explains' the classification */
	public String explain(CandidateSegmentGroup group);
}

