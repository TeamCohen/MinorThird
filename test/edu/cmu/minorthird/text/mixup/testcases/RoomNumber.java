package edu.cmu.minorthird.text.mixup.testcases;

import java.util.Iterator;

import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/** Trivial annotator used as test case. */
public class RoomNumber extends AbstractAnnotator{

	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return "Not implemented";
	}

	protected void doAnnotate(MonotonicTextLabels labels){
		for(Iterator<Span> i=labels.getTextBase().documentSpanIterator();i
				.hasNext();){
			Span s=i.next();
			for(int j=0;j<s.size();j++){
				if(s.getToken(j).getValue().equals("1112")){
					Span r=s.subSpan(j,1);
					labels.addToType(r,"extracted_room");
				}
			}
		}
		labels.setAnnotatedBy("RoomNumber");
	}
}
