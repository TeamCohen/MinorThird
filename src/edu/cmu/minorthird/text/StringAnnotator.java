package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.CharAnnotation;
import org.apache.log4j.Logger;

/**
 * An abstract annotator that is based on marking up substrings within
 * a string, using the CharAnnotation class.
 *
 * @author ksteppe
 */

public abstract class StringAnnotator extends AbstractAnnotator
{
  private static Logger log = Logger.getLogger(StringAnnotator.class);
	protected String providedAnnotation = null;

  protected void doAnnotate(MonotonicTextLabels labels)
	{
		//add the annotations into labels
		edu.cmu.minorthird.text.TextBase textBase = labels.getTextBase();
		for (edu.cmu.minorthird.text.Span.Looper it = textBase.documentSpanIterator(); it.hasNext();)
		{
			edu.cmu.minorthird.text.Span span = it.nextSpan();
			String spanString = span.asString();

      CharAnnotation[] annotations = annotateString(spanString);

      if (annotations != null)
      {
        for (int i = 0; i < annotations.length; i++)
        {
          CharAnnotation ann = annotations[i];
          int lo = ann.getOffset();
          edu.cmu.minorthird.text.Span newSpan = span.charIndexSubSpan(lo, lo + ann.getLength());
          labels.addToType(newSpan, ann.getType());
        }
      }
		}
		if (providedAnnotation!=null) labels.setAnnotatedBy(providedAnnotation);
	}

  protected String[] closedTypes()
  {
    return null;
  }

	/** Override this class to provide the actual annotations for a span.
	 */
  protected abstract CharAnnotation[] annotateString(String spanString);

}
