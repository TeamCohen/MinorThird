package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.mixup.*;

import java.util.*;
import java.io.*;

/**
 * Reducing an extraction task to tagging tokens as one of five
 * categories.  The categories are: a token beginning the type to
 * extract, a token ending the type to extract, a single token which
 * is the type to extract, otherwise inside the type to extract, or
 * outside the type to extract.
 *
 * @author William Cohen
 */

// to do - test with spanProp


public class BeginContinueEndUniqueReduction extends Extraction2TaggingReduction implements Serializable
{
	private static final long serialVersionUID = 1;
	// saves result of last reduction
	transient private NestedTextLabels taggedLabels;
	private String tokenProp = "_entityPart"; 
	// all tag values that were used
	private Set tagset = new HashSet();
	private boolean useSpanType = true;

	public void reduceExtraction2Tagging(AnnotationExample example)
	{
		reduceDocument(example.getDocumentSpan(),example.getLabels(),example.getInputType(),example.getInputProp());
	}

	private void reduceDocument(Span doc,TextLabels labels,String spanType,String spanProp)
	{
		useSpanType = spanType!=null;
		taggedLabels = new NestedTextLabels(labels);
		// label all tokens as NEG
		assignDefaultLabels(doc,taggedLabels,spanType,spanProp);
		// label the tokens inside a span to be extracted as POS, if there's just one
		// type to extract, or with the property value, otherwise.
		String id = doc.getDocumentId();
		Span.Looper i = useSpanType?taggedLabels.instanceIterator(spanType,id):taggedLabels.getSpansWithProperty(spanProp,id);
		while (i.hasNext()) {
			Span span = i.nextSpan();
			String baseTag = useSpanType?spanType:taggedLabels.getProperty(span,spanProp);
			tagset.add(baseTag);
			if (span.size()==1) {
				String tag = baseTag+"Unique";
				taggedLabels.setProperty( span.getToken(0), tokenProp, tag );
			} else {
				String beginTag = baseTag+"Begin";
				taggedLabels.setProperty( span.getToken(0), tokenProp, beginTag );
				String endTag = baseTag+"End";
				taggedLabels.setProperty( span.getToken(span.size()-1), tokenProp, endTag );
				if (span.size()>2) {
					String contTag = baseTag+"Continue";
					for (int j=1; j<span.size()-1; j++) {
						taggedLabels.setProperty( span.getToken(j), tokenProp, contTag);
					}
				}
			}
		}
	}

	public String getTokenProp() { return tokenProp; }
	
	public Set getNonDefaultTagValues() 
	{
		Set result = new HashSet();
		for (Iterator i=tagset.iterator(); i.hasNext(); ) {
			String baseTag = (String)i.next();
			result.add(baseTag+"Unique");
			result.add(baseTag+"Begin");
			result.add(baseTag+"End");
			result.add(baseTag+"Continue");
		}
		return result;
	}

	public TextLabels getTaggedLabels() { return taggedLabels; }

	/** Return a TextLabels in which tagged tokens are used 
	 * to solve the extraction problem. */
	public void extractFromTags(String output,MonotonicTextLabels taggedLabels)
	{
		try {
			MixupProgram p = new MixupProgram();
			if (useSpanType) {
				String baseTag = (String)tagset.iterator().next();
				p.addStatement("defSpanType "+output+" =: "+makePattern(baseTag));
			} else {
				for (Iterator i=tagset.iterator(); i.hasNext(); ) {
					String baseTag = (String)i.next();
					p.addStatement("defSpanProp "+output+":"+baseTag+" =: "+makePattern(baseTag));
				}
			}
			//System.out.println("extractFromTags program:\n"+p);
			p.eval(taggedLabels,taggedLabels.getTextBase());
		} catch (Mixup.ParseException ex) {
			throw new IllegalStateException("mixup error: "+ex);
		}
	}
	private String makePattern(String baseTag)
	{
		String p = tokenProp+":"+baseTag;
		return "... ["+p+"Begin L "+p+"Continue* R "+p+"End] ... || ... ["+p+"Unique] ...";
	}
}
