package edu.cmu.minorthird.text.learn;

import java.util.Iterator;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Train an AnnotationExample from a previously annotated corpus (stored in a
 * TextLabels).
 * 
 * @author William Cohen
 */
public class TextLabelsAnnotatorTeacher extends AnnotatorTeacher{

	private TextLabels labels;

	private String userLabelType;

	private String userLabelProp;

	public TextLabelsAnnotatorTeacher(TextLabels labels,String userLabelType){
		this(labels,userLabelType,null);
	}

	public TextLabelsAnnotatorTeacher(TextLabels labels,String userLabelType,
			String userLabelProp){
		this.labels=labels;
		this.userLabelType=userLabelType;
		this.userLabelProp=userLabelProp;
	}

	@Override
	public Iterator<Span> documentPool(){
		return labels.getTextBase().documentSpanIterator();
	}

	@Override
	public AnnotationExample labelInstance(Span query){
		if(query.documentSpanStartIndex()!=0||
				query.size()!=query.documentSpan().size()){
			throw new IllegalArgumentException("can't label a partial document");
		}
		// should really generate a restricted view of this labels, containing just
		// one document...
		AnnotationExample example=
				new AnnotationExample(query,labels,userLabelType,userLabelProp);
		return example;
	}

	@Override
	public boolean hasAnswers(){
		return true;
	}

	@Override
	public TextLabels availableLabels(){
		return labels;
	}
}
