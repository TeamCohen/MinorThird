package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.CharAnnotation;
import edu.cmu.minorthird.text.StringAnnotator;


/*
 *  This class implements an annotator for signature files
 *  Dec 2003 - vitor@cs  
 */
public class SigFileAnnotator extends StringAnnotator
{
  protected CharAnnotation[] annotateString(String spanString)
  {
   //parse string and return line where sigfile starts
    int lastCharPosition = spanString.length();
    int sigStartPosition = SigFilePredictor.Predict(spanString);

    //sig file starts at char 100 and is 50 chars long (including white space)
    //CharAnnotation ann = new CharAnnotation(100, 50, "SIGFILE");
    //Predict returns -1 if sigFile is not found
    if(sigStartPosition == -1){
    	return null;
    }

    int sigLength = lastCharPosition - sigStartPosition + 1;

    CharAnnotation[] outV = new CharAnnotation[1];
    outV[0] = new CharAnnotation(sigStartPosition, sigLength, "SIGFILE");
    return outV;

  }

  public String explainAnnotation(edu.cmu.minorthird.text.TextEnv Env, edu.cmu.minorthird.text.Span documentSpan)
  {
    return "Not Implemented Yet";
  }
}
