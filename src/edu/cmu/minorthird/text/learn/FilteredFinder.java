package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;

import java.util.TreeMap;

/**
 * Finds spans by filtering a list of proposed candidates with a
 * classifier.
 *
 *
 * @author William Cohen
 */

public class FilteredFinder extends AbstractSpanFinder
{
	private BinaryClassifier spanFilter;
	private SpanFeatureExtractor fe;
	private SpanFinder candidateFinder;
	private TreeMap detailMap;

	public FilteredFinder(BinaryClassifier spanFilter,SpanFeatureExtractor fe,SpanFinder candidateFinder) {
		this.spanFilter = spanFilter;
		this.fe = fe;
		this.candidateFinder = candidateFinder;
		this.detailMap = new TreeMap();
	}

	public Span.Looper findSpans(TextLabels labels,Span documentSpan)
	{
		detailMap.clear();
		for (Span.Looper i=candidateFinder.findSpans(labels,documentSpan); i.hasNext(); ) {
			Span candidate = i.nextSpan();
			double prediction = spanFilter.score( fe.extractInstance(labels,candidate) );
			if (prediction > 0) {
				detailMap.put( candidate, new Details(prediction) );
			}
		}
		return new BasicSpanLooper( detailMap.keySet().iterator() );
	}

	public Details getDetails(Span s) {
		return (Details)detailMap.get(s);
	}

	public String toString() { return "[FilteredFinder "+spanFilter+"]"; }


	public String explainFindSpans(TextLabels labels, Span documentSpan) {
		StringBuffer buf = new StringBuffer("");
		buf.append("Explaining findSpans for "+documentSpan+":\n");
		for (Span.Looper i=candidateFinder.findSpans(labels,documentSpan); i.hasNext(); ) {
			Span candidate = i.nextSpan();
			buf.append("candidate: "+candidate+"\n");
			Instance instance = fe.extractInstance(labels,candidate);
			buf.append("instance: "+instance+"\n");
			buf.append("classification: " + spanFilter.explain(instance) + "\n");
			buf.append("\n");
		}
		return buf.toString();
	}
}
