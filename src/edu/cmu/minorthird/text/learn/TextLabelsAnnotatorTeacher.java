package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.AnnotationExample;

/**
 * Train an AnnotationExample from a previously annotated corpus (stored in a TextLabels).
 *
 * @author William Cohen
 */
public class TextLabelsAnnotatorTeacher extends AnnotatorTeacher
{
	private TextLabels labels;
	private String userLabelType;
	private String userLabelProp;

	public TextLabelsAnnotatorTeacher(TextLabels labels,String userLabelType)
	{
		this(labels,userLabelType,null);
	}

	public TextLabelsAnnotatorTeacher(TextLabels labels,String userLabelType,String userLabelProp)
	{
		this.labels = labels;
		this.userLabelType = userLabelType;
		this.userLabelProp = userLabelProp;
	}

	public Span.Looper documentPool()
	{ 
		return labels.getTextBase().documentSpanIterator();
	}

	public AnnotationExample labelInstance(Span query)
	{ 
		if (query.documentSpanStartIndex()!=0 || query.size()!=query.documentSpan().size()) {
			throw new IllegalArgumentException("can't label a partial document");
		}
		// should really generate a restricted view of this labels, containing just one document...
		AnnotationExample example = new AnnotationExample( query, labels, userLabelType, userLabelProp );
		return example;
	}

	public boolean hasAnswers() { return true; }

	public TextLabels availableLabels() { return labels; }
}
