package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.AnnotationExample;

/**
 * Train an AnnotationExample from a previously annotated corpus (stored in a TextEnv).
 *
 * @author William Cohen
 */
public class TextEnvAnnotatorTeacher extends AnnotatorTeacher
{
	private TextEnv env;
	private String userLabelType;

	public TextEnvAnnotatorTeacher(TextEnv env,String userLabelType)
	{
		this.env = env;
		this.userLabelType = userLabelType;
	}

	public Span.Looper documentPool()
	{ 
		return env.getTextBase().documentSpanIterator();
	}

	public AnnotationExample labelInstance(Span query)
	{ 
		if (query.documentSpanStartIndex()!=0 || query.size()!=query.documentSpan().size()) {
			throw new IllegalArgumentException("can't label a partial document");
		}
		// should really generate a restricted view of this env, containing just one document...
		AnnotationExample example = new AnnotationExample( query, env, userLabelType, null );
		return example;
	}

	public boolean hasAnswers() { return true; }

	public TextEnv availableEnvironment() { return env; }
}
