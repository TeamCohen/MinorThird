package edu.cmu.minorthird.classify.sequential;


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

