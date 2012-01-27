package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import java.util.*;

/**
 * A scheme for reducing an extraction task to a tagging task.
 *
 * @author William Cohen
 */

public abstract class Extraction2TaggingReduction
{
	/** Convert the information in a single annotation example to token
	 * 'tags' (token properties, assigned to every token).
	 */
	abstract public void reduceExtraction2Tagging(AnnotationExample example);

	/** Return the token property used for the tags which encode the
	 * extraction task.
	 */
	abstract public String getTokenProp();
	
	/** Get all the tag values that were used.
	 */
	abstract public Set<String> getNonDefaultTagValues();

	/** Return the TextLabels holding the tags which encode the
	 * extraction task.
	 */
	abstract public TextLabels getTaggedLabels();

	/** Alter a TextLabels object with tagged tokens by using them to
	 * solve the extraction problem. */
	abstract public void extractFromTags(String output, MonotonicTextLabels labels);

	//
	// convenience methods
	//

	/** 
	 * Label all tokens as negative, including ones which are inside a
	 * span to be extracted. This is intended to be used before labeling
	 * the positive tokens appropriately.
	 */
	protected void assignDefaultLabels(Span doc,MonotonicTextLabels taggedLabels,String spanType,String spanProp)
	{
		if(spanType==null){
			// label every token as negative, since there's no CW information for properties....
			for(int j=0;j<doc.size();j++){
				taggedLabels.setProperty(doc.getToken(j),getTokenProp(),ExampleSchema.NEG_CLASS_NAME );
			}
		}
		else{
			// use the closed world information
			for (Iterator<Span> i=taggedLabels.closureIterator(spanType,doc.getDocumentId());i.hasNext();){
				Span span=i.next();
				for(int j=0;j<span.size();j++) {
					taggedLabels.setProperty( span.getToken(j), getTokenProp(), ExampleSchema.NEG_CLASS_NAME );
				}
			}
		}
	}
}
