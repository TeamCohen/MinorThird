package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Run several annotators in sequence.
 *
 * @author William Cohen
 */

public class SerialAnnotator extends AbstractAnnotator
{
	private List annotatorList;

	public SerialAnnotator(List annotatorList)
	{
		this.annotatorList = annotatorList;
	}
	public SerialAnnotator(Annotator[] annotators)
	{
		this.annotatorList = new ArrayList(annotators.length);
		for (int i=0; i<annotators.length; i++) {
			annotatorList.add( annotators[i] );
		}
	}
	
	protected void doAnnotate(MonotonicTextLabels labels)
	{
		for (Iterator i=annotatorList.iterator(); i.hasNext(); ) {
			Annotator ann = (Annotator)i.next();
			ann.annotate(labels);
		}
	}

	public String explainAnnotation(TextLabels labels,Span documentSpan)
	{
		StringBuffer buf = new StringBuffer("");
		for (Iterator i=annotatorList.iterator(); i.hasNext(); ) {
			Annotator ann = (Annotator)i.next();
			buf.append(ann.explainAnnotation(labels,documentSpan));
		}
		return buf.toString();
	}
}
