package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SigFileAnnotator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * This class...
 * @author ksteppe
 */
public class AnnotatorRunner
{
  private static Logger log = Logger.getLogger(AnnotatorRunner.class);

  public static void main(String[] args)
  {// load the documents into a textbase

    TextBase base = new BasicTextBase();
    TextBaseLoader loader = new TextBaseLoader();
    File dir = new File("C:/radar/extract/src/com/wcohen/text/ann/samplemail"); //put the directory with emails here.
    //File dir = new File("C:/boulder/randomNOSig"); //put the directory with emails here.
    try
    {
      loader.loadTaggedFiles(base, dir);
    }
    catch (IOException e)
    {
      log.fatal(e, e);
      System.exit(1);
    }
    MutableTextEnv env = new BasicTextEnv(base);

    Annotator annotator = new SigFileAnnotator();
    // Annotator annotator = new POSTagger();
    annotator.annotate(env);

// output the results
    for (Span.Looper i = env.instanceIterator("sig"); i.hasNext();)
    {
      Span span = i.nextSpan();
      //System.out.println( span.asString().replace('\n',' ') );
      System.out.println(span.toString().replace('\n', ' '));
    }
  }


}
