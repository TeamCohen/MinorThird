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
    public String spanType;
    private BasicTextLabels labels;

    /** Initialize a TextBase with a single spanType from a parent TextBase */
    public SpanTypeTextBase(TextLabels parentLabels, String spanType)
    {
	TextBaseMapper mapper = new TextBaseMapper(this);
	new SpanTypeTextBase(parentLabels, spanType, mapper);
    }

    public SpanTypeTextBase(TextLabels parentLabels, String spanType, TextBaseMapper mapper) {
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
	    Span docSpan = base.documentSpan(docID);
	    validDocumentSpans.add(docSpan);
	    mapper.mapDocument(curDocId, docSpan, startIndex); 
	}
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
	TextBaseMapper mapper = new TextBaseMapper(this);
	return retokenize(tok,mapper);
    }

    public TextBase retokenize(Tokenizer tok, TextBaseMapper mapper)
    {
	TextBase tb = base;
	tb = tb.retokenize(tok, mapper);
	return tb;
    }

    /**Retokenize the textBase creating psuedotokens for a certain spanType */
    public MonotonicTextLabels createPseudotokens(MonotonicTextLabels labels, String spanType) {
	TextBaseMapper mapper = new TextBaseMapper(this);
	return createPseudotokens(labels, spanType, mapper);
    }

    /**Retokenize the textBase creating psuedotokens for a certain spanType */
    public MonotonicTextLabels createPseudotokens(MonotonicTextLabels labels, String spanType, TextBaseMapper mapper) {
	TextBase tb = base;
	MonotonicTextLabels newLabels = tb.createPseudotokens(labels, spanType, mapper);
	return newLabels;
    }

}
