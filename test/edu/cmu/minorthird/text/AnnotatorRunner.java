package edu.cmu.minorthird.text;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.Logger;

/** 
 * This class...
 * @author ksteppe
 */ 
public class AnnotatorRunner
{
  private static Logger log = Logger.getLogger(AnnotatorRunner.class);

  public static void main(String[] args)
  {
    try
    {
      //put the directory with emails here.
      File dir = new File("C:/radar/extract/src/com/wcohen/text/ann/samplemail"); 

      MutableTextLabels labels = null;
      TextBaseLoader loader = new TextBaseLoader();
      loader.load(dir);
      labels = loader.getLabels();

     //for sig annotations:
      //Annotator annotator = new SigFileAnnotator();
      //String tag = "sig";
      
      //in case you only want the reply-to lines
      //Annotator annotator = new ReplyToAnnotator();
      //String tag = "reply";
      
      //for POS experiments
       Annotator annotator = new POSTagger();
       String tag = "whatever";       
      
       annotator.annotate(labels);

      // to see the results
      for (Iterator<Span> i = labels.instanceIterator(tag); i.hasNext();)
      {
        Span span = i.next();
        System.out.println(span.toString().replace('\n', ' '));
      }
      
    }
    catch (Exception e)
    {
      log.fatal(e, e);
    }

  }
}