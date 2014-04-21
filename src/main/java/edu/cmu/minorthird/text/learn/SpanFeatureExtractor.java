/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Converts a span to an instance.  This interface is here because it's
 * easier to create anonymous interfaces in BeanShell than anonymous
 * implementations of abstract classes (like SpanFE.)
 *
 * @author William Cohen
 */

public interface SpanFeatureExtractor
{
    //public Instance extractInstance(Span s);
	public Instance extractInstance(TextLabels labels,Span s);
}
