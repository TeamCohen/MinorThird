/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.SpanFE;

/**
 * Buffers features constructed using the SpanFE.SpanResult
 * subclasses.
 *
 * <p> This is intended to be used as an alternative to using the
 * SpanFE class to build an Span2Instance converter, eg
 * <pre><code>
 * fe = new Span2Instance() { 
 *   public extractInstance(Span s) {
 *     FeatureBuffer buf = new FeatureBuffer();
 *     SpanFE.from(s,buf).tokens().emit(); 
 *     SpanFE.from(s,buf).left().subspan(-2,2).emit(); 
 *     SpanFE.from(s,buf).right().subspan(0,2).emit(); 
 *     buf.getInstance();
 *   }
 * }
 *</code></pre>
 *
 * @author William Cohen
 */

public class FeatureBuffer extends SpanFE
{
	private Span span=null;

	public FeatureBuffer(TextLabels labels,Span span) {
		super();
		this.instance = new MutableInstance(span,span.getDocumentGroupId());
	}

	public FeatureBuffer(Span span) {
		this(null,span); 
	}

	public Instance getInstance() { return instance; }
	public void extractFeatures(TextLabels labels,Span s) {	throw new IllegalStateException("improper use of FeatureBuffer");	}
	public void extractFeatures(Span s) {	throw new IllegalStateException("improper use of FeatureBuffer");	}
}
