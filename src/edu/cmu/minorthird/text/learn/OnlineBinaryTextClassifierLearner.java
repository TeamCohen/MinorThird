package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.OnlineClassifierLearner;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.ui.Recommended;

/**
 * Provides a way to Edit document labels and add them to the learner
 * 
 * @author Cameron Williams
 */

public class OnlineBinaryTextClassifierLearner extends AbstractAnnotator
		implements OnlineTextClassifierLearner,Serializable{
	
	static final long serialVersionUID=20080306L;

	// internal state
	private SpanFeatureExtractor fe=null;

	public OnlineClassifierLearner learner;

	public String spanType,outputType;

	private int docNum;

	private final static String DOC="OnlineDocument_";

	public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner,
			String spanType){
		this(learner,spanType,null,null);
	}

	public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner,
			String spanType,TextLabels labeledData){
		this(learner,spanType,labeledData,null);
	}

	public OnlineBinaryTextClassifierLearner(OnlineClassifierLearner learner,
			String spanType,TextLabels labeledData,SpanFeatureExtractor fe){
		this.learner=learner;
		this.spanType=spanType;
		this.outputType="_predicted_"+spanType;
		this.docNum=0;
		if(fe!=null)
			this.fe=fe;
		else
			this.fe=new Recommended.DocumentFE();
		if(labeledData!=null)
			addLabeledData(labeledData);
	}

	/** Add already labeled data to the learner */
	private void addLabeledData(TextLabels labels){
		TextBase tb=labels.getTextBase();
		for(Iterator<Span> looper=tb.documentSpanIterator();looper.hasNext();){
			Span s=looper.next();
			int classLabel=labels.hasType(s,spanType)?+1:-1;

			Instance i=fe.extractInstance(labels,s);
			Example ex=new Example(i,ClassLabel.binaryLabel(classLabel));

			learner.addExample(ex);
		}
	}

	/** Provide document string with a label and add to the learner */
	@Override
	public void addDocument(String label,String text){
		BasicTextBase tb=new BasicTextBase();
		docNum++;
		String docID=DOC+docNum;
		tb.loadDocument(docID,text);

		MutableTextLabels textLabels=new BasicTextLabels(tb);
		Span docSpan=tb.documentSpan(docID);
		textLabels.addToType(docSpan,label);

		int classLabel=textLabels.hasType(docSpan,spanType)?+1:-1;
//		int negClassLabel=textLabels.hasType(docSpan,"NOT"+spanType)?+1:-1;

		Instance i=fe.extractInstance(textLabels,docSpan);
		Example ex=new Example(i,ClassLabel.binaryLabel(classLabel));

		learner.addExample(ex);
	}

	/** Returns the TextClassifier */
	@Override
	public TextClassifier getTextClassifier(){
		TextClassifier tc=new BinaryTextClassifier(learner,fe);
		return tc;
	}

	@Override
	public Classifier getClassifier(){
		return learner.getClassifier();
	}

	/** Tells the learner that no more examples are coming */
	@Override
	public void completeTraining(){
		learner.completeTraining();
	}

	/** Erases all previous data from the learner */
	@Override
	public void reset(){
		learner.reset();
	}

	@Override
	public String[] getTypes(){
		String[] types={spanType,"NOT"+spanType};
		return types;
	}

	@Override
	public void doAnnotate(MonotonicTextLabels labels){
		Iterator<Span> candidateLooper=labels.getTextBase().documentSpanIterator();

		Classifier c=learner.getClassifier();
		for(Iterator<Span> i=candidateLooper;i.hasNext();){
			Span s=i.next();
			ClassLabel classOfS=c.classification(fe.extractInstance(labels,s));
			if(spanType!=null&&classOfS.isPositive()){
				labels.addToType(s,outputType);
			}
		}
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		Classifier c=learner.getClassifier();
		return c.explain(fe.extractInstance(labels,documentSpan));
	}

	@Override
	public ClassifierAnnotator getAnnotator(){
		ClassifierAnnotator ann=
				new ClassifierAnnotator(fe,getClassifier(),outputType,null,null);
		return ann;
	}
}
