package email; 

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
import java.util.*;
import java.io.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
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
    	//all models below are based on LC_BOW only. original dataset is located
      File reqfile = new File("apps/email/models/Req_Model");//DT
      req_model = (BinaryClassifier) IOUtil.loadSerialized(reqfile);
      File dlvfile = new File("apps/email/models/Dlv_Model");//VP,batch15
      dlv_model = (BinaryClassifier) IOUtil.loadSerialized(dlvfile);
      File propfile = new File("apps/email/models/Prop_Model");//VP,batch15
      prop_model = (BinaryClassifier) IOUtil.loadSerialized(propfile);
      File cmtfile = new File("apps/email/models/Cmt_Model");//VP,batch15
      cmt_model = (BinaryClassifier) IOUtil.loadSerialized(cmtfile);
      File amdfile = new File("apps/email/models/Amd_Model");//VP,batch15
      amd_model = (BinaryClassifier) IOUtil.loadSerialized(amdfile);
      File reqamdpropfile = new File("apps/email/models/ReqAmdProp_Model");//DT
      reqamdprop_model = (BinaryClassifier) IOUtil.loadSerialized(reqamdpropfile);
      File dlvcmtfile = new File("apps/email/models/DlvCmt_Model");//VP,batch15
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
      System.out.println("textbase size = " + textBase.size());
      //TextBaseEditor.edit(labels, new File("moomoomoo"));
      //for (Span.Looper it = textBase.documentSpanIterator(); it.hasNext();){
	  for (Iterator it = labels.instanceIterator("mainbody"); it.hasNext();) {
        //Span span = (Span)it.nextSpan();
        Span span = (Span)it.next();
        MutableInstance ins = (MutableInstance)sa.fe.extractInstance(labels, span);
	    boolean reqbool = sa.bclassify(sa.req_model, ins);
	    boolean dlvbool = sa.bclassify(sa.dlv_model, ins);
	    boolean propbool = sa.bclassify(sa.req_model, ins);
	    boolean cmtbool = sa.bclassify(sa.dlv_model, ins);
	    boolean amdbool = sa.bclassify(sa.req_model, ins);
	   	boolean reqamdpropbool = sa.bclassify(sa.reqamdprop_model, ins);
	    boolean dlvcmtbool = sa.bclassify(sa.dlvcmt_model, ins);
	    
	    String reqs = reqbool?   "_REQ_":"_____";
	    String dlvs = dlvbool?   "_DLV_":"_____";
	    String props = propbool? "_PROP_":"______";
	    String cmts = cmtbool?   "_CMT_":"_____";
	   	String amds = amdbool?   "_AMD_":"_____";
		String reqamdprops = reqamdpropbool? "_REQAMDPROP":"___________";
	    String dlvcmts = dlvcmtbool? "_DLVCMT__":"_________";
	    System.out.print("docId = "+span.getDocumentId()+"\n\t\t("+reqs+" "+dlvs+" "+props+" "+cmts+" "+amds+" "+reqamdprops+" "+dlvcmts+"\n");
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
