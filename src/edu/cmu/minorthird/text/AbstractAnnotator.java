package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.Dependencies;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Generic implementation of an annotator.
 *
 *
 * @author William Cohen
 */

public abstract class AbstractAnnotator implements Annotator
{
	private Set prereqs = new HashSet();

	/** The implementation for this method annotates labels in-line. */
	abstract protected void doAnnotate(MonotonicTextLabels labels);

	/** The implementation for this method should explain how annotation
	 * would be added to some part of the text base. */
	abstract public String explainAnnotation(TextLabels labels,Span documentSpan);

	final public void annotate(MonotonicTextLabels labels) {
		for (Iterator i=prereqs.iterator(); i.hasNext(); ) {
			String req = (String)i.next();
			if (!labels.isAnnotatedBy(req))
        Dependencies.runDependency(labels, req, null);
//				throw new IllegalArgumentException("labels is not been annotated by "+req);
		}
		doAnnotate(labels);
	}

	final public TextLabels annotatedCopy(TextLabels labels) {
		MonotonicTextLabels copy = new NestedTextLabels(labels);
		annotate(copy);
		return copy;
	}

	/** Specify a 'pre-req' - an annotation that must exist
	 * before this annotation can be used. */
	final public void addPrereq(String s) { prereqs.add(s); }

}
