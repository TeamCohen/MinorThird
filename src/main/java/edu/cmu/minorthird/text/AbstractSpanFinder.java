package edu.cmu.minorthird.text;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Abstract implementation of a SpanFinder.
 *
 * @author William Cohen
 */

public abstract class AbstractSpanFinder implements SpanFinder{

	/** Find subspans of each span produced by the documentSpanIterator. */
	@Override
	final public Iterator<Span> findSpans(TextLabels labels,Iterator<Span> documentSpanIterator){
		TreeSet<Span> set=new TreeSet<Span>();
		while(documentSpanIterator.hasNext()){
			for(Iterator<Span> i=findSpans(labels,documentSpanIterator);i.hasNext();set.add(i.next()));
		}
		return set.iterator();
	}

	/** Find subspans of the given document span. */
	@Override
	abstract public Iterator<Span> findSpans(TextLabels labels,Span documentSpan);

	/** Explain how spans were found. */
	@Override
	abstract public String explainFindSpans(TextLabels labels,Span documentSpan);
}
