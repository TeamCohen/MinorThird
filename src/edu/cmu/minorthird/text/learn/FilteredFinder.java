package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.AbstractSpanFinder;
import edu.cmu.minorthird.text.Details;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanFinder;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Finds spans by filtering a list of proposed candidates with a classifier.
 * 
 * 
 * @author William Cohen
 */

public class FilteredFinder extends AbstractSpanFinder implements Serializable{

	static private final long serialVersionUID=20080306L;

	private BinaryClassifier spanFilter;

	private SpanFeatureExtractor fe;

	private SpanFinder candidateFinder;

	private SortedMap<Span,Details> detailMap;

	public FilteredFinder(BinaryClassifier spanFilter,SpanFeatureExtractor fe,
			SpanFinder candidateFinder){
		this.spanFilter=spanFilter;
		this.fe=fe;
		this.candidateFinder=candidateFinder;
		this.detailMap=new TreeMap<Span,Details>();
	}

	@Override
	public Iterator<Span> findSpans(TextLabels labels,Span documentSpan){
		detailMap.clear();
		for(Iterator<Span> i=candidateFinder.findSpans(labels,documentSpan);i
				.hasNext();){
			Span candidate=i.next();
			double prediction=spanFilter.score(fe.extractInstance(labels,candidate));
			if(prediction>0){
				detailMap.put(candidate,new Details(prediction));
			}
		}
		return detailMap.keySet().iterator();
	}

	@Override
	public Details getDetails(Span s){
		return detailMap.get(s);
	}

	@Override
	public String toString(){
		return "[FilteredFinder "+spanFilter+"]";
	}

	@Override
	public String explainFindSpans(TextLabels labels,Span documentSpan){
		StringBuffer buf=new StringBuffer("");
		buf.append("Explaining findSpans for "+documentSpan+":\n");
		for(Iterator<Span> i=candidateFinder.findSpans(labels,documentSpan);i
				.hasNext();){
			Span candidate=i.next();
			buf.append("candidate: "+candidate+"\n");
			Instance instance=fe.extractInstance(labels,candidate);
			buf.append("instance: "+instance+"\n");
			buf.append("classification: "+spanFilter.explain(instance)+"\n");
			buf.append("\n");
		}
		return buf.toString();
	}
}
