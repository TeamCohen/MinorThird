/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.learn.FeatureBuffer;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.SpanFE;
import org.apache.log4j.Logger;

/**
 *
 * A very simple feature extractor
 * @see edu.cmu.minorthird.text.learn.FeatureBuffer
 *
 * @author $ksteppe$
 * @version $Revision: 1.2 $
 */
public class SimpleFeatureExtractor implements SpanFeatureExtractor
{
    private Logger log = Logger.getLogger(this.getClass());

    /**
     * Uses the FeatureBuffer class to extract an instance.
     *
     * @param span Span to extract from
     * @return the extracted Instance
     */
    public Instance extractInstance(edu.cmu.minorthird.text.Span span)
    {
        edu.cmu.minorthird.text.learn.FeatureBuffer buf = new edu.cmu.minorthird.text.learn.FeatureBuffer(span);
        SpanFE.from(span, buf).tokens().emit();
        SpanFE.from(span, buf).left().subSpan(-1, 1).emit();
        SpanFE.from(span, buf).right().subSpan(0, 1).emit();
//        log.debug("buf.instance=" + buf.getInstance());
        return buf.getInstance();
    }

    /**
     * This version ignores the TextLabels
     * @param labels a TextLabels (ignored)
     * @param span The Span to extract from
     * @return an Instance
     */
    public Instance extractInstance(edu.cmu.minorthird.text.TextLabels labels, edu.cmu.minorthird.text.Span span)
    {
        return extractInstance(span);
    }
}
