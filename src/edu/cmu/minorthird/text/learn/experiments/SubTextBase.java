package edu.cmu.minorthird.text.learn.experiments;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.minorthird.text.AbstractTextBase;
import edu.cmu.minorthird.text.Document;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.Tokenizer;

/**
 * A read-only TextBase which is a subset of another TextBase.
 * 
 * 
 * @author William Cohen
 */
public class SubTextBase extends AbstractTextBase{

	private Set<Span> validDocumentSpans;

	private TextBase base;

	public static class UnknownDocumentException extends Exception{
		
		static final long serialVersionUID=20080314L;
		
		public UnknownDocumentException(String s){
			super(s);
		}
	}

	public SubTextBase(TextBase base,Iterator<Span> documentSpanIterator)
			throws UnknownDocumentException{
		super(base.getTokenizer());
		this.base=base;
		validDocumentSpans=new TreeSet<Span>();
		while(documentSpanIterator.hasNext()){
			Span span=documentSpanIterator.next();
			if(base.documentSpan(span.getDocumentId())==null){
				throw new UnknownDocumentException("documentId not in textBase: "+
						span.getDocumentId());
			}
			validDocumentSpans.add(span);
		}
	}

	/** True if a span is contained by this TextBase */
	public boolean contains(Span span){
		return validDocumentSpans.contains(span.documentSpan());
	}

	//
	// Implementations of abstract methods from AbstractTextBase
	@Override
	public Tokenizer getTokenizer(){
		return base.getTokenizer();
	}

	@Override
	public Document getDocument(String documentId){
		return base.getDocument(documentId);
	}

	@Override
	public int size(){
		return validDocumentSpans.size();
	}

	@Override
	public Iterator<Span> documentSpanIterator(){
		return validDocumentSpans.iterator();
	}

	@Override
	public Span documentSpan(String documentId){
		Span span=base.documentSpan(documentId);
		return validDocumentSpans.contains(span)?span:null;
	}

}
