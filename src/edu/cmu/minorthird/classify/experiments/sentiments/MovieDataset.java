package edu.cmu.minorthird.classify.experiments.sentiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.T1InstanceTransformLearner;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.classify.transform.T1InstanceTransform;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.learn.SpanFE;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.FeatureBuffer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * @author Edoardo M. Airoldi
 * Date: Jan 22, 2004
 */

public class MovieDataset {

  static private Logger log = Logger.getLogger(MovieDataset.class);

  public static Dataset MovieReviewsData() {
    Dataset data = new BasicDataset();
    try {

      // load the documents into a textbase
      TextBase base = new BasicTextBase();
      TextBaseLoader loader = new TextBaseLoader();
      File dir = new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/movie-reviews-100");

      loader.loadTaggedFiles(base, dir);

      // set up labels
      MutableTextLabels labels = new BasicTextLabels(base);
      new TextLabelsLoader().importOps(labels, base, new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/movie-labels-100.env"));

      // for verification/correction of the labels, if we care...
      //TextBaseLabeler.label( labels, new File("my-document-labels.env"));


      // apply mixup file to get candidates, if there is one
      File mixupFile = new File("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/bigrams.mixup");
      MixupProgram p = new MixupProgram(mixupFile);
      p.eval(labels,labels.getTextBase());


      // set up a simple bag-of-words feature extractor
      System.out.println("Extract Features");
      SpanFeatureExtractor fe = new SpanFeatureExtractor()
      {
        public Instance extractInstance(TextLabels labels, Span s) {
          FeatureBuffer buf = new FeatureBuffer(labels, s);
          /*try {
          SpanFE.from(s, buf).tokens().eq().lc().punk().usewords("/Users/eairoldi/cmu.research/Text.Learning.Group/UAI.2004/Min3rd-Datasets/words.txt").emit();

          } catch (IOException e) {
          log.error(e, e);
          } */
          //SpanFE.from(s,buf).tokens().eq().lc().punk().stopwords("use").emit();
          SpanFE.from(s,buf).contains("bigram").eq().lc().punk().emit();
          return buf.getInstance();
        }
        public Instance extractInstance(Span s) {
          return extractInstance(null,s);
        }
      };

      // check
      //log.debug(labels.getTypes().toString());

      // create a binary dataset for the class 'Pos'
      System.out.println("Create Movie Reviews Dataset");
      for (Span.Looper i = base.documentSpanIterator(); i.hasNext();) {
        Span s = i.nextSpan();
        double label = labels.hasType(s, "Pos") ? +1 : -1;
        data.add(new BinaryExample(fe.extractInstance(s), label));
      }

      // Filter here, if you like ...
/*      System.out.println("Filter Features");
      T1InstanceTransformLearner learner = new T1InstanceTransformLearner();
      learner.setREF_LENGTH(100.0);
      learner.setPDF("Poisson");
      //learner.setPDF("Negative-Binomial");
      InstanceTransform t1stat = learner.batchTrain( data );
      //((T1InstanceTransform)t1stat).setALPHA(0.05);
      //((T1InstanceTransform)t1stat).setMIN_WORDS(50);
      ((T1InstanceTransform)t1stat).setSAMPLE(5000);
      data = t1stat.transform( data );*/

    } catch (Exception e) {
      log.error(e, e);
      System.exit(1);
    }

    return data;
  }
}
