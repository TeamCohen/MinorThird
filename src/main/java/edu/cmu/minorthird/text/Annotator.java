package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Something that extends a text labeling with additional annotations.
 *
 * @author William Cohen
 */

public interface Annotator
{
	/** Add some extra information to the labels. */
	public void annotate(MonotonicTextLabels labels);

	/** Create a copy of the labels with some additional
			information added. */
	public TextLabels annotatedCopy(TextLabels labels);

	/** Explain how annotation was added to some part of the
	 * text base. */
	public String explainAnnotation(TextLabels labels,Span documentSpan);
}
