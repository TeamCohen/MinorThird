package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.BasicSpanLooper;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/** A read-only TextBase.
 *  A textBase that is a subset of a parent textBase containing one span type
 *
 * @author Cameron Williams
 */

public class SpanTypeTextBase extends ImmutableTextBase
{
    private Set validDocumentSpans;
    private TextBase base;
    private String spanType;
    private MutableTextLabels labels;

    /** Initialize a TextBase with a single spanType from a parent TextBase */
    public SpanTypeTextBase(TextLabels parentLabels, String spanType)
    {
	base = new BasicTextBase(); 
	this.spanType = spanType;
	validDocumentSpans = new TreeSet();
	Span.Looper i = parentLabels.instanceIterator(spanType);
	
	while( i.hasNext() ) {
	    Span s = i.nextSpan();
	    String docID = s.getDocumentId();
	    String docText = s.asString();
	    int startIndex = s.getLoChar();
	    base.loadDocument(docID, docText, startIndex);
	}
	for(Span.Looper j = base.documentSpanIterator(); j.hasNext(); ) {
	    Span span = j.nextSpan();
	    validDocumentSpans.add(span);
	}
    }

    /** Import the labels from the parent TextBase */
    public TextLabels importLabels(TextLabels parentLabels) {
	labels = new BasicTextLabels(base);
	Span.Looper parentIterator = parentLabels.instanceIterator(spanType);
	//Reiterate over the spans with spanType in the parent labels
	while(parentIterator.hasNext()) {
	    Span parentSpan = parentIterator.nextSpan();
	    String docID = parentSpan.getDocumentId();
	    Span childDocSpan = base.documentSpan(docID); //The matching span in the child textbase
	    int childDocStartIndex = base.getDocument(docID).charOffset; //the number of charaters the child span is offset
	    Set types = parentLabels.getTypes();  
	    Iterator typeIterator = types.iterator();
	    //Iterate over span types in the parent textBase
	    while(typeIterator.hasNext()) {
		String type = (String)typeIterator.next();
		Set spansWithType = ((BasicTextLabels)parentLabels).getTypeSet(type, docID);
		Iterator spanIterator = spansWithType.iterator();
		//Iterate over the spans with a type
		while(spanIterator.hasNext()) {
		    Span s = (Span)spanIterator.next();
		    //See if the parent span conains the span
		    if(parentSpan.contains(s)) {
			//find the matching span in the child doc span and add it to the child Labels
			Span subSpan = childDocSpan.charIndexSubSpan(s.getLoChar()-childDocStartIndex, s.getHiChar()-childDocStartIndex);
			labels.addToType(subSpan, type);
		    }
		}
	    }	    	    
	}
	return labels;
    }

    public int size() { return validDocumentSpans.size(); }

    public Span.Looper documentSpanIterator() {
	return new BasicSpanLooper( validDocumentSpans );
    }

    public Span documentSpan(String documentId) {
	Span span = base.documentSpan(documentId);
	return validDocumentSpans.contains(span) ? span : null;
    }

    /** True if a span is contained by this TextBase */
    public boolean contains(Span span) {
	return validDocumentSpans.contains( span.documentSpan() );
    }

    public TextBase retokenize(Tokenizer tok)
    {
	TextBase tb = new BasicTextBase();
	tb = base.retokenize(new Tokenizer());
	return tb;
    }

}