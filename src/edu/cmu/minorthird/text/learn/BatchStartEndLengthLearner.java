package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.util.HashMap;
import java.util.Map;

/**
 * Learn a StartEndLengthAnnotator from examples.
 *
 * This has not been extensively tested.
 *
 * @author William Cohen
 */

public class BatchStartEndLengthLearner implements AnnotatorLearner
{
	private SpanFeatureExtractor fe;
	private BinaryClassifierLearner startLearner;
	private BinaryClassifierLearner endLearner;
	// record stuff needed for length histogram
	private Map spanLengthMap = new HashMap();
	private int totalPosSpans = 0;
	// default
	private String annotationType = "_prediction";
	private static final boolean SAVE_DATA = false;
  private Dataset startData = SAVE_DATA ? new BasicDataset() : null;
  private Dataset endData = SAVE_DATA ? new BasicDataset() : null;

	public BatchStartEndLengthLearner(
		SpanFeatureExtractor fe,BinaryClassifierLearner startLearner,BinaryClassifierLearner endLearner)
	{
		this.fe = fe;
		this.startLearner = startLearner;
		this.endLearner = endLearner;
		reset();
	}

	public void reset() {
		spanLengthMap = new HashMap();
		totalPosSpans = 0;
		annotationType = "_prediction";
		startData = SAVE_DATA ? new BasicDataset() : null;
		endData = SAVE_DATA ? new BasicDataset() : null;
	}

	public String getAnnotationType() { return annotationType; }

	public void setAnnotationType(String s) { annotationType=s; }

	// temporary storage
	private Span.Looper documentLooper;

	/** Accept the pool of unlabeled documents. */
	public void setDocumentPool(Span.Looper documentLooper) {
		this.documentLooper = documentLooper;
	}
	
	/** Ask for labels on every document. */
	public boolean hasNextQuery() {
		return documentLooper.hasNext();
	}

	/** Return the next unlabeled document. */
	public Span nextQuery() {
		return documentLooper.nextSpan();
	}

	/** Accept the answer to the last query. */
	public void setAnswer(AnnotationExample answeredQuery)
	{
		TextLabels answerLabels = answeredQuery.labelTokensStartEnd("startEnd");
		Span document = answeredQuery.getDocumentSpan();
		for (int i=0; i<document.size(); i++) {
			Token tok = document.getToken(i);
			String value = answerLabels.getProperty(tok,"startEnd");
			if (AnnotationExample.START.equals(value)) {
				addInstance( fe.extractInstance(answerLabels,document.subSpan(i,1)), +1, -1);
			}  else if (AnnotationExample.END.equals( value )) {
				addInstance( fe.extractInstance(answerLabels,document.subSpan(i,1)), -1, +1);
			} else if (AnnotationExample.NOT_START_OR_END.equals( value )) {
				addInstance( fe.extractInstance(answerLabels,document.subSpan(i,1)), -1, -1);
			}
		}
		String inputType = answeredQuery.getInputType();
		String id = document.getDocumentId();
		for (Span.Looper i=answeredQuery.getLabels().instanceIterator(inputType,id); i.hasNext(); ) {
			Span s = i.nextSpan();
			if (s.size()>=1) {
				// record length
				totalPosSpans++;
				Integer key = new Integer(s.size());
				Integer c = (Integer)spanLengthMap.get(key);
				if (c==null) spanLengthMap.put(key, new Integer(1));
				else spanLengthMap.put(key, new Integer(c.intValue()+1));
			}
		}
	}

	private void addInstance(Instance instance,double startLabel,double endLabel)
	{
		startLearner.addExample( new BinaryExample(instance,startLabel) );
		endLearner.addExample( new BinaryExample(instance,endLabel) );
		if (SAVE_DATA) {
			startData.add( new BinaryExample(instance,startLabel) );
			endData.add( new BinaryExample(instance,endLabel) );
		}
	}

  public SpanFeatureExtractor getSpanFeatureExtractor()  { return fe; }

	/** Return the learned annotator
	 */
	public Annotator getAnnotator() {
		ViewerFrame s = new ViewerFrame("start",startData.toGUI());
		ViewerFrame e = new ViewerFrame("end",endData.toGUI());
		StartEndLengthAnnotator ann =
			new StartEndLengthAnnotator(
				(BinaryClassifier)startLearner.getClassifier(),
				(BinaryClassifier)endLearner.getClassifier(),
				fe, spanLengthMap, totalPosSpans, annotationType );
		ann.setThreshold( 0.00001 );
		return ann;
	}
}
