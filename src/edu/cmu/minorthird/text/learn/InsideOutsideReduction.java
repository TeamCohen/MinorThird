package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.mixup.*;

import java.util.*;
import java.io.*;

/**
 * Reducing an extraction task to tagging tokens as inside the type to
 * extract, or outside the type to extract.
 *
 * @author William Cohen
 */

// to do - test with spanProp


public class InsideOutsideReduction extends Extraction2TaggingReduction implements Serializable
{
	private static final long serialVersionUID = 1;
	// saves result of last reduction
	transient private NestedTextLabels taggedLabels;
	private String tokenProp = "_inside"; 
	// all tag values that were used
	private Set tagset = new HashSet();

	public void reduceExtraction2Tagging(AnnotationExample example)
	{
		reduceDocument(example.getDocumentSpan(),example.getLabels(),example.getInputType(),example.getInputProp());
	}

	private void reduceDocument(Span doc,TextLabels labels,String spanType,String spanProp)
	{
		taggedLabels = new NestedTextLabels(labels);
		assignDefaultLabels(doc,taggedLabels,spanType,spanProp);
		// label the tokens inside a span to be extracted as POS, if there's just one
		// type to extract, or with the property value, otherwise.
		String id = doc.getDocumentId();
		Span.Looper i = 
			spanType!=null?taggedLabels.instanceIterator(spanType,id):taggedLabels.getSpansWithProperty(spanProp,id);
		while (i.hasNext()) {
			Span span = i.nextSpan();
			String tag = spanType!=null?ExampleSchema.POS_CLASS_NAME:taggedLabels.getProperty(span,spanProp);
			tagset.add( tag );
			for (int j=0; j<span.size(); j++) {
				taggedLabels.setProperty( span.getToken(j), tokenProp, tag);
			}
		}
	}

	public String getTokenProp() { return tokenProp; }
	
	public Set getNonDefaultTagValues() { return tagset; }

	public TextLabels getTaggedLabels() { return taggedLabels; }

	/** Return a TextLabels in which tagged tokens are used 
	 * to solve the extraction problem. */
	public void extractFromTags(String output,MonotonicTextLabels taggedLabels)
	{
		try {
			MixupProgram p = new MixupProgram();
			if (tagset.size()==1 && tagset.iterator().next().equals(ExampleSchema.POS_CLASS_NAME)) {
				p.addStatement("defSpanType "+output+" =: "+makePattern(ExampleSchema.POS_CLASS_NAME));
			} else {
				for (Iterator i=tagset.iterator(); i.hasNext(); ) {
					String tag = (String)i.next();
					p.addStatement("defSpanProp "+output+":"+tag+" =: "+makePattern(tag));
				}
			}
			p.eval(taggedLabels,taggedLabels.getTextBase());
		} catch (Mixup.ParseException ex) {
			throw new IllegalStateException("mixup error: "+ex);
		}
	}
	private String makePattern(String val)
	{
		return "... [L "+tokenProp+":"+val+"+ R] ...";
	}
}
