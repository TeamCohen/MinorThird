//package email; 

/**
 * This class is a first attemp to classify email messages into "speech acts".
 * 
 * It follows the description in 
 * "Learning to Classify Email into "Speech Acts"", 
 * V.R.Carvalho, W.W.Cohen, T. M. Mitchell ; EMNLP 2004
 *  
 * @author Vitor R. Carvalho
 * Jun 15, 2004
 *
  */

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.LineProcessingUtil;

//just for comparison with paper results
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.svm.*; 
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;

public class SpeechAct {

  private BinaryClassifier req_model;
  private BinaryClassifier dlv_model;
  private BinaryClassifier cmt_model;
  private BinaryClassifier prop_model;
  private BinaryClassifier amd_model;
  private BinaryClassifier reqamdprop_model;
  private BinaryClassifier dlvcmt_model;
  private static Logger log = Logger.getLogger(SpeechAct.class);
  // serialization stuff
  static public final long serialVersionUID = 1;
  public final int CURRENT_VERSION_NUMBER = 1;

  private SpanFeatureExtractor fe = edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS;
	
  public SpeechAct() {
    try {
      File reqfile = new File("apps/email/models/VPsigPredictionModel");
      req_model = (BinaryClassifier) IOUtil.loadSerialized(reqfile);
      File dlvfile = new File("apps/email/models/VPsigPredictionModel");
      dlv_model = (BinaryClassifier) IOUtil.loadSerialized(dlvfile);
      File propfile = new File("apps/email/models/VPsigPredictionModel");
      prop_model = (BinaryClassifier) IOUtil.loadSerialized(propfile);
      File cmtfile = new File("apps/email/models/VPsigPredictionModel");
      cmt_model = (BinaryClassifier) IOUtil.loadSerialized(cmtfile);
      File amdfile = new File("apps/email/models/VPsigPredictionModel");
      amd_model = (BinaryClassifier) IOUtil.loadSerialized(amdfile);
      File reqamdpropfile = new File("apps/email/models/VPsigPredictionModel");
      reqamdprop_model = (BinaryClassifier) IOUtil.loadSerialized(reqamdpropfile);
      File dlvcmtfile = new File("apps/email/models/VPsigPredictionModel");
      dlvcmt_model = (BinaryClassifier) IOUtil.loadSerialized(dlvcmtfile);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private ClassLabel classification(BinaryClassifier model, Instance mi) {
    return model.classification(mi);
  }

  private boolean bclassify(BinaryClassifier model, Instance mi) {
  	double Th = 0;
    return (model.score(mi)>Th)? true:false;    
  }
  


  public static void main(String[] args) {
    //Usage check
    try {
      if ((args.length < 1)|| (args.length>1)) {
        usage();
        return;
      }
      File dir = new File(args[0]);
      boolean clo;
      SpeechAct sa = new SpeechAct();
      MutableTextLabels labels = TextBaseLoader.loadDirOfTaggedFiles(dir);
      TextBase textBase = labels.getTextBase();
      for (Span.Looper it = textBase.documentSpanIterator(); it.hasNext();){
	  //for (Iterator i = labels.instanceIterator("mainbody"); i.hasNext();) {
        Span span = it.nextSpan();
        MutableInstance ins = (MutableInstance)sa.fe.extractInstance(labels, span);
	    clo = sa.bclassify(sa.req_model, ins);
	    String outt = clo? "___REQ":"NOTreq";
	    System.out.println("docId = "+span.getDocumentId()+"   class: "+outt);
       // String spanString = span.asString();
      }
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static void usage() {
    System.out.println("usage: SpeechAct directoryname");
//    System.out.println("OR");
//    System.out.println(
//      "usage: SigFilePredictor -create filename1 filename2 ...");
//    System.out.println(
//      "PS: to create, use \"Signature and Reply Dataset\" annotation stile as in www.cs.cmu.edu/~vitor/codeAndData.html");
  }
}
