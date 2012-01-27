/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Buffers features constructed using the SpanFE.SpanResult subclasses.
 * 
 * <p>
 * This is intended to be used as an alternative to using the SpanFE class to
 * build an Span2Instance converter, eg
 * 
 * <pre><code>
 * fe=new Span2Instance(){
 * 
 * 	public extractInstance(Span s){
 * 		FeatureBuffer buf=new FeatureBuffer();
 * 		SpanFE.from(s,buf).tokens().emit();
 * 		SpanFE.from(s,buf).left().subspan(-2,2).emit();
 * 		SpanFE.from(s,buf).right().subspan(0,2).emit();
 * 		buf.getInstance();
 * 	}
 * }
 * </code></pre>
 * 
 * @author William Cohen
 */

public class FeatureBuffer extends SpanFE{

	static final long serialVersionUID=20080306L;
	
//	private Span span=null;

	public FeatureBuffer(TextLabels labels,Span span){
		super();
		this.instance=new MutableInstance(span,span.getDocumentGroupId());
	}

	public FeatureBuffer(Span span){
		this(null,span);
	}

	public Instance getInstance(){
		return instance;
	}

	@Override
	public void extractFeatures(TextLabels labels,Span s){
		throw new IllegalStateException("improper use of FeatureBuffer");
	}

	@Override
	public void extractFeatures(Span s){
		throw new IllegalStateException("improper use of FeatureBuffer");
	}
}
