package edu.cmu.minorthird.classify.experiments.sentiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFE;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @author Edoardo M. Airoldi
 * Date: Jan 22, 2004
 */

public class MovieDataset {

    static private Logger log = Logger.getLogger(MovieDataset.class);

    public static Dataset MovieReviewsData() {
        Dataset data = new BasicDataset();
        try
        {
            // load the documents into a textbase
            TextBase base = new BasicTextBase();
            TextBaseLoader loader = new TextBaseLoader();
            File dir = new File("/usr1/edo/Min3rd-Datasets/movie-reviews-100");
            loader.loadTaggedFiles(base, dir);

            // set up labels
            MutableTextLabels labels = new BasicTextLabels(base);
            new TextLabelsLoader().importOps(labels, base, new File("/usr1/edo/Min3rd-Datasets/movie-labels-100.env"));

            // for verification/correction of the labels, if we care...
            //TextBaseLabeler.label( labels, new File("my-document-labels.env"));

            System.out.println("extract features");

            // set up a simple bag-of-words feature extractor
            edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe = new edu.cmu.minorthird.text.learn.SpanFeatureExtractor()
            {
                public Instance extractInstance(TextLabels labels, Span s)
                {
                    edu.cmu.minorthird.text.learn.FeatureBuffer buf = new edu.cmu.minorthird.text.learn.FeatureBuffer(labels, s);
                    /*try
                    {
                        edu.cmu.minorthird.text.learn.SpanFE.from(s, buf).tokens().eq().lc().punk().usewords("examples/t1.words.text").emit(); }
                        catch (IOException e)
                    {
                        log.error(e, e);
                    }*/
                    SpanFE.from(s,buf).tokens().eq().lc().punk().stopwords("use").emit();
                    return buf.getInstance();
                }
                public Instance extractInstance(Span s)
                {
                    return extractInstance(null,s);
                }
            };

            // check
            log.debug(labels.getTypes().toString());

            System.out.println("Create Movie Reviews Dataset");

            // create a binary dataset for the class 'Pos'
            for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
            {
                Span s = i.nextSpan();
                //System.out.println( labels );
                double label = labels.hasType(s, "Pos") ? +1 : -1;
                data.add(new BinaryExample(fe.extractInstance(s), label));
                //BinaryExample example = new BinaryExample( fe.extractInstance(s), label );
                //data.add( example );
            }

            /*T1InstanceTransformLearner T1learner = new T1InstanceTransformLearner();
            InstanceTransform t1Statistics = new T1InstanceTransform();
            t1Statistics = T1learner.batchTrain( data );
            System.out.println( "old data:\n" + data );
            data = t1Statistics.transform( data );
            System.out.println( "new data:\n" + data );*/
        }
        catch (Exception e)
        {
            log.error(e, e);
            System.exit(1);
        }
        return data;
    }
}
