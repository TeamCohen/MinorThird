package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.FilteredFinder;

/**
 * Learn a FilteredFinder.
 *
 * @author William Cohen
 */

public class BatchFilteredFinderLearner implements AnnotatorLearner
{
	private SpanFeatureExtractor fe;
	private BinaryClassifierLearner classifierLearner;
	private SpanFinder candidateFinder;
	private String annotationType = "_prediction";
	private Classifier filter = null;

	private Dataset dataset = new BasicDataset(); // buffer

	public void reset() 
	{
		dataset = new BasicDataset();
	}
			
  public BatchFilteredFinderLearner(
		SpanFeatureExtractor fe,BinaryClassifierLearner classifierLearner,SpanFinder candidateFinder)
	{
    init(fe, classifierLearner, candidateFinder);
  }

  /**
   * Initialize variables
   * @param fe SpanFeatureExtractor
   * @param classifierLearner BinaryClassifierLearner
   * @param candidateFinder SpanFinder
   */
  public void init(SpanFeatureExtractor fe, BinaryClassifierLearner classifierLearner, SpanFinder candidateFinder)
  {
    this.fe = fe;
    this.classifierLearner = classifierLearner;
    this.candidateFinder = candidateFinder;
  }

  public void copyFrom(BatchFilteredFinderLearner copy)
  {
    init(copy.getSpanFeatureExtractor(), copy.getClassifierLearner(), copy.getCandidateFinder());
    this.annotationType = copy.annotationType;
    this.filter = copy.getClassifier();
  }

  //protected Object clone() throws CloneNotSupportedException
  //{
	//BatchFilteredFinderLearner newObj = new BatchFilteredFinderLearner();
	//newObj.copyFrom(this);
	//return newObj;
  //}

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
	public void setAnswer(AnnotationExample answeredQuery) {
		TextLabels queryLabels = answeredQuery.getLabels();
		for (Span.Looper i=candidateFinder.findSpans(queryLabels, answeredQuery.getDocumentSpan() ); i.hasNext(); ) {
			Span candidate = i.nextSpan();
			if (answeredQuery.getLabels().hasType( candidate, answeredQuery.getInputType() )) {
				dataset.add( new Example( fe.extractInstance(queryLabels,candidate), ClassLabel.binaryLabel(+1)) );
			} else {
				dataset.add( new Example( fe.extractInstance(queryLabels, candidate), ClassLabel.binaryLabel(-1)) );
			}
		}
	}

	/** Return the learned annotator
	 */
	public Annotator getAnnotator() {
		filter = new DatasetClassifierTeacher(dataset).train( classifierLearner );
		FilteredFinder filteredFinder = new FilteredFinder( (BinaryClassifier)filter, fe, candidateFinder );
		return new FinderAnnotator( filteredFinder, annotationType, true );
	}
	
	/** Return the classifier the annotator is based on
	 */
	public Classifier getClassifier() {
		return filter;
	}

  public SpanFinder getCandidateFinder()
  { return candidateFinder; }

  public void setCandidateFinder(SpanFinder candidateFinder)
  { this.candidateFinder = candidateFinder; }

  public BinaryClassifierLearner getClassifierLearner()
  { return classifierLearner; }

  public void setClassifierLearner(BinaryClassifierLearner classifierLearner)
  { this.classifierLearner = classifierLearner; }

  public SpanFeatureExtractor getSpanFeatureExtractor()
  { return fe; }

  public void setSpanFeatureExtractor(SpanFeatureExtractor fe)
  { this.fe = fe; }
}

