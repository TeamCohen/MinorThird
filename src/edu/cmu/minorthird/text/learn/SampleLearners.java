package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.BinaryClassifierLearner;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import org.apache.log4j.Logger;

/**
 * SampleLearners contains a number of singleton example Learners
 * @author ksteppe
 */
public class SampleLearners
{
  /**
   * A simple learning algorithm for extraction learning based on
   * classifying nodes as inside or outside the span
   */
  public final static AnnotatorLearner FILTERED_BIGRAM = getFilteredBigram();

  /**
   * A learning algorithm for extraction learning based on classifying
   * nodes beginning, or ending a span, and also learning a score
   * on possible span lengths.
   */
  public final static AnnotatorLearner START_END_LENGTH = getStartEndLength();

  /**
   * A simple learning algorithm for extraction learning based on
   * classifying nodes as inside or outside the span
   */
  public static final AnnotatorLearner INSIDE_OUTSIDE = getInsideOutside();

  /**
   * A simple learning algorithm for extraction learning based on
   * classifying nodes as inside or outside the span
   */
  public static final AnnotatorLearner CMM = getCMM();

  /**
   * A simple learning algorithm for image pointers
   * based on filtering candidates
   * NOT IMPLEMENTED
   */
//  public static final AnnotatorLearner IMAGE_PTR = getImagePtr();

  private static AnnotatorLearner getCMM()
  {

    SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(edu.cmu.minorthird.text.Span s) {
          return extractInstance(null,s);
        }
        public Instance extractInstance(edu.cmu.minorthird.text.TextLabels labels,edu.cmu.minorthird.text.Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels,s);
          SpanFE.from(s,buf).tokens().emit();
          SpanFE.from(s,buf).tokens().prop("cap").emit();
          SpanFE.from(s,buf).tokens().prop("name").emit();
          SpanFE.from(s,buf).left().subSpan(-2,2).emit();
          SpanFE.from(s,buf).right().subSpan(0,2).emit();
          return buf.getInstance();
        }
      };
    ClassifierLearner classifierLearner = new VotedPerceptron();
    AnnotatorLearner annotatorLearner = new CMMAnnotatorLearner( fe, classifierLearner, 1 );

    return annotatorLearner;
  }

  private static AnnotatorLearner getImagePtr()
  {
    return null;
    /* PerceptronBatchLearner not in cvs
    SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(Span s) {
          FeatureBuffer buf = new FeatureBuffer();
          SpanFE.from(s,buf).tokens().emit();
          SpanFE.from(s,buf).left().subspan(-2,2).emit();
          SpanFE.from(s,buf).right().subspan(0,2).emit();
          return buf.getInstance();
        }
      };
    SpanFinder candidateFinder = new MixupFinder( new mixup("... [eq('(') !eq(')'){1,15} eq(')')] ...") );
    BinaryClassifierLearner classifierLearner = new PerceptronBatchLearner();
    AnnotatorLearner annotatorLearner =
      new BatchFilteredFinderLearner( fe, classifierLearner, candidateFinder );

    return annotatorLearner;
    */
  }

  private static AnnotatorLearner getInsideOutside()
  {

    SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(edu.cmu.minorthird.text.Span s) {
          return extractInstance(null,s);
        }
        public Instance extractInstance(edu.cmu.minorthird.text.TextLabels labels,edu.cmu.minorthird.text.Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels,s);
          SpanFE.from(s,buf).tokens().emit();
          SpanFE.from(s,buf).tokens().prop("cap").emit();
          SpanFE.from(s,buf).tokens().prop("name").emit();
          SpanFE.from(s,buf).left().subSpan(-2,2).emit();
          SpanFE.from(s,buf).right().subSpan(0,2).emit();
          return buf.getInstance();
        }
      };
    ClassifierLearner classifierLearner = new AdaBoost( new DecisionTreeLearner(), 10);
    AnnotatorLearner annotatorLearner = new BatchInsideOutsideLearner( fe, classifierLearner );

    return annotatorLearner;

  }


  private static AnnotatorLearner getStartEndLength()
  {

    SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(edu.cmu.minorthird.text.Span s) {
          return extractInstance(null,s);
        }

        public Instance extractInstance(edu.cmu.minorthird.text.TextLabels labels,edu.cmu.minorthird.text.Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels,s);
          SpanFE.from(s,buf).tokens().emit();
          SpanFE.from(s,buf).tokens().prop("cap").emit();
          SpanFE.from(s,buf).tokens().prop("name").emit();
          SpanFE.from(s,buf).left().subSpan(-2,2).emit();
          SpanFE.from(s,buf).right().subSpan(0,2).emit();
          return buf.getInstance();
        }
      };

    BinaryClassifierLearner startLearner = new DecisionTreeLearner();
    BinaryClassifierLearner endLearner = new DecisionTreeLearner();
    AnnotatorLearner annotatorLearner = new BatchStartEndLengthLearner( fe, startLearner, endLearner );

    return annotatorLearner;
  }

  private static Logger log = Logger.getLogger(SampleLearners.class);
  private static AnnotatorLearner getFilteredBigram()
  {
    SpanFeatureExtractor fe = new SpanFeatureExtractor() {
        public Instance extractInstance(edu.cmu.minorthird.text.Span s) {
          return extractInstance(null,s);
        }
        public Instance extractInstance(edu.cmu.minorthird.text.TextLabels labels,edu.cmu.minorthird.text.Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels,s);
          SpanFE.from(s,buf).tokens().emit();
          SpanFE.from(s,buf).tokens().prop("cap").emit();
          SpanFE.from(s,buf).tokens().prop("name").emit();
          SpanFE.from(s,buf).left().subSpan(-2,2).emit();
          SpanFE.from(s,buf).right().subSpan(0,2).emit();
          return buf.getInstance();
        }
      };

    BinaryClassifierLearner classifierLearner = new DecisionTreeLearner();
    try
    {
      SpanFinder bigramFinder = new MixupFinder(new Mixup("... [any any] ... "));
      AnnotatorLearner annotatorLearner = new BatchFilteredFinderLearner(fe, classifierLearner, bigramFinder);
      return annotatorLearner;
    }
    catch (Exception e)
    {
      log.error(e, e);
      return null;
    }
  }
}
