package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.MonotonicTextEnv;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextEnv;

/**
 * Something that extends a text environment with additional annotations.
 *
 * @author William Cohen
 */

public interface Annotator
{
	/** Add some extra information to the environment. */
	public void annotate(MonotonicTextEnv env);

	/** Create a copy of the environment with some additional
			information added. */
	public TextEnv annotatedCopy(TextEnv env);

	/** Explain how annotation was added to some part of the
	 * text base. */
	public String explainAnnotation(TextEnv Env,Span documentSpan);
}
