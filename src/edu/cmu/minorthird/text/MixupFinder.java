package edu.cmu.minorthird.text;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import edu.cmu.minorthird.text.mixup.Mixup;

/**
 * Finds spans using a mixup expression evaluated in a fixed labeling.
 *
 * @author William Cohen
 */

public class MixupFinder implements SpanFinder,Serializable{

	static private final long serialVersionUID=20080303L;

//	private static final TextLabels EMPTY_LABELS = new EmptyLabels();

	private Mixup mixup;

	public MixupFinder(Mixup mixup){
		this.mixup=mixup;
	}

	@Override
	public Iterator<Span> findSpans(TextLabels labels,Iterator<Span> documentSpanLooper){
		return mixup.extract(labels,documentSpanLooper);
	}

	@Override
	public Iterator<Span> findSpans(TextLabels labels,Span documentSpan){
		Iterator<Span> singletonLooper=Collections.singleton(documentSpan).iterator();
		return findSpans(labels,singletonLooper);
	}

	@Override
	public Details getDetails(Span s){
		return new Details(1.0,mixup);
	}

	@Override
	public String explainFindSpans(TextLabels labels,Span documentSpan){
		return "Spans found using mixup expression: "+mixup;
	}
}
