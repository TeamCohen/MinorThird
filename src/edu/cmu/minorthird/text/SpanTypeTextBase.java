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
    private MonotonicTextLabels labels;

    /** Initialize a TextBase with a single spanType from a parent TextBase */
    public SpanTypeTextBase(TextLabels parentLabels, String spanType)
    {
	base = new BasicTextBase(); 
	this.spanType = spanType;
	validDocumentSpans = new TreeSet();
	Span.Looper i = parentLabels.instanceIterator(spanType);
	
	String prevDocId = ""; //useful for checking whether the next span is in the same doc
	int docNum = 0; //counts how many spans have the type in each document
	while( i.hasNext() ) {
	    Span s = i.nextSpan();
	    String curDocId = s.getDocumentId();
	    if(curDocId.equals(prevDocId))
		docNum++;		
	    else docNum = 0;
	    String docID = "childTB" + docNum + "-" + curDocId;
	    prevDocId = curDocId;
	    String docText = s.asString();
	    int startIndex = s.getLoChar();
	    base.loadDocument(docID, docText, startIndex);
	}
	for(Span.Looper j = base.documentSpanIterator(); j.hasNext(); ) {
	    Span span = j.nextSpan();
	    validDocumentSpans.add(span);
	}
    }

    /**Import Labels from another TextBase - as long as the current TextBase is some subset of the original */
    public MonotonicTextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels) {
	return origLabels;
    }

    /** Import the labels from the parent TextBase */
    public MonotonicTextLabels importLabels(TextLabels parentLabels) {
	labels = new BasicTextLabels(base);
	Span.Looper parentIterator = parentLabels.instanceIterator(spanType);
	String prevDocId = "";
	int docNum = 0;
	//Reiterate over the spans with spanType in the parent labels
	while(parentIterator.hasNext()) {
	    Span parentSpan = parentIterator.nextSpan();
	    String docID = parentSpan.getDocumentId();
	    if(docID.equals(prevDocId))
		docNum++;
	    else docNum=0;
	    Span childDocSpan = base.documentSpan("childTB" + docNum + "-" + docID); //The matching span in the child textbase
	    prevDocId = docID;
	    int childDocStartIndex = parentSpan.getLoTextToken();
	    Set types = parentLabels.getTypes();  
	    Iterator typeIterator = types.iterator();
	    //Iterate over span types in the parent textBase
	    while(typeIterator.hasNext()) {
		String type = (String)typeIterator.next();
		Set spansWithType = parentLabels.getTypeSet(type, docID);
		Iterator spanIterator = spansWithType.iterator();
		//Iterate over the spans with a type
		while(spanIterator.hasNext()) {
		    Span s = (Span)spanIterator.next();
		    //See if the parent span conains the span
		    if(parentSpan.contains(s) && childDocSpan.size()>=s.getLoTextToken()-childDocStartIndex+s.size() ) {
			//find the matching span in the child doc span and add it to the child Labels
			Span subSpan = childDocSpan.subSpan(s.getLoTextToken()-childDocStartIndex, s.size());
			labels.addToType(subSpan, type);
		    }
		}
	    }	    	    
	}
	return labels;
    }

    /** Import the labels of type from the parent TextBase */
    public TextLabels importLabels(MonotonicTextLabels origLabels, TextLabels parentLabels, String type, String newName) {
	labels = new BasicTextLabels(base);
	Span.Looper parentIterator = parentLabels.instanceIterator(spanType);
	//Reiterate over the spans with spanType in the parent labels
	while(parentIterator.hasNext()) {
	    Span parentSpan = parentIterator.nextSpan();
	    String docID = parentSpan.getDocumentId();
	    Span childDocSpan = base.documentSpan(docID); //The matching span in the child textbase
	    //int childDocStartIndex = base.getDocument(docID).charOffset; //the number of charaters the child span is offset
	    int childDocStartIndex = parentSpan.getLoTextToken();
	    Set spansWithType = ((BasicTextLabels)parentLabels).getTypeSet(type, docID);
	    Iterator spanIterator = spansWithType.iterator();
	    //Iterate over the spans with a type
	    while(spanIterator.hasNext()) {
		Span s = (Span)spanIterator.next();
		//See if the parent span conains the span
		if(parentSpan.contains(s)) {
		    //find the matching span in the child doc span and add it to the child Labels
		    //Span subSpan = childDocSpan.charIndexSubSpan(s.getLoChar()-childDocStartIndex, s.getHiChar()-childDocStartIndex);
		    Span subSpan = childDocSpan.subSpan(s.getLoTextToken()-childDocStartIndex, s.size());
		    labels.addToType(subSpan, newName);
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
	tb = base.retokenize(tok);
	return tb;
    }

    /**Retokenize the textBase creating psuedotokens for a certain spanType */
    public MonotonicTextLabels createPseudotokens(MonotonicTextLabels labels, String spanType) {
	return labels;
    }

}