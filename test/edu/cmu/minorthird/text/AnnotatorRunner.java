package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SigFileAnnotator;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * This class...
 * @author ksteppe
 */
public class AnnotatorRunner
{
  private static Logger log = Logger.getLogger(AnnotatorRunner.class);


  public static void main(String[] args)
  {// load the documents into a textbase

    try
    {
  //    TextBase base = new BasicTextBase();
  //    TextBaseLoader loader = new TextBaseLoader();
      File dir = new File("C:/radar/extract/src/com/wcohen/text/ann/samplemail"); //put the directory with emails here.
      //File dir = new File("C:/boulder/randomNOSig"); //put the directory with emails here.

      MutableTextLabels labels = null;
        labels = TextBaseLoader.loadDirOfTaggedFiles(dir);
  //    TextBase base = labels.getTextBase();

      Annotator annotator = new SigFileAnnotator();
      // Annotator annotator = new POSTagger();
      annotator.annotate(labels);

  // output the results
      for (Span.Looper i = labels.instanceIterator("sig"); i.hasNext();)
      {
        Span span = i.nextSpan();
        //System.out.println( span.asString().replace('\n',' ') );
        System.out.println(span.toString().replace('\n', ' '));
      }

    }
    catch (Exception e)
    {
      log.fatal(e, e);
    }

  }

}
