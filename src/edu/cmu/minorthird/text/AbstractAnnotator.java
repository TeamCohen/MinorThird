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

	/** The implementation for this method annotates an
	 * environment in-line. */
	abstract protected void doAnnotate(MonotonicTextEnv env);

	/** The implementation for this method should explain how annotation
	 * would be added to some part of the text base. */
	abstract public String explainAnnotation(TextEnv Env,Span documentSpan);

	final public void annotate(MonotonicTextEnv env) {
		for (Iterator i=prereqs.iterator(); i.hasNext(); ) {
			String req = (String)i.next();
			if (!env.isAnnotatedBy(req))
        Dependencies.runDependency(env, req, null);
//				throw new IllegalArgumentException("env is not been annotated by "+req);
		}
		doAnnotate(env);
	}

	final public TextEnv annotatedCopy(TextEnv env) {
		MonotonicTextEnv copy = new NestedTextEnv(env);
		annotate(copy);
		return copy;
	}

	/** Specify a 'pre-req' - an annotation that must exist
	 * before this annotation can be used. */
	final public void addPrereq(String s) { prereqs.add(s); }

}
