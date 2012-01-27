package edu.cmu.minorthird.text;

/**
 * Generic implementation of an annotator.
 *
 *
 * @author William Cohen
 */

public abstract class AbstractAnnotator implements Annotator{

	/** The implementation for this method annotates labels in-line. */
	abstract protected void doAnnotate(MonotonicTextLabels labels);

	/** The implementation for this method should explain how annotation
	 * would be added to some part of the text base. */
	@Override
	abstract public String explainAnnotation(TextLabels labels,Span documentSpan);

	@Override
	final public void annotate(MonotonicTextLabels labels){
		doAnnotate(labels);
	}

	@Override
	final public TextLabels annotatedCopy(TextLabels labels){
		MonotonicTextLabels copy=new NestedTextLabels(labels);
		annotate(copy);
		return copy;
	}
}
