package email; 

/**
 * This class is a first attemp to classify email messages into "speech acts".
 * 
 * It follows the description in 
 * "Learning to Classify Email into "Speech Acts"", 
 * V.R.Carvalho, W.W.Cohen, T. M. Mitchell ; EMNLP 2004
 *
 * To use it, please:
 * 1- add minorthird/apps/email/class to your CLASSPATH
 * 2- from minorthird/apps/email directory, compile it using "ant build"
 * 3- from minorhthird/ directory, run it using "java -Xmx500m email.SpeechAct directoryName"
 *
 * The output will be something like:
 * C:\minorthird>java -Xmx500m email.SpeechAct dummy
 * textbase size = 8
 * msgId_28975_fIRMID_N03F2_1997_09_15_00_39_57     (_____ _DLV_ _PROP_ _____ _____ ___________ _DLVCMT__ _____)
 * msgId_547_fIRMID_N03F2_1997_08_26_15_24_57     (_REQ_ _DLV_ _PROP_ _____ _____ _REQAMDPROP _________ _MEET_)
 * msgId_11137_fIRMID_N03F2_1997_09_04_23_56_57     (_REQ_ _DLV_ ______ _____ _____ _REQAMDPROP _DLVCMT_ ______)
 * 
 *
 * Reminder: it currently uses all words inside the text file (bag of lower case
 *   words model). If you want to use only the words surrounded by a <body> tag, 
 *   go to the main method, and please change the following lines:
 *
 * comment:
 *      for (Span.Looper it = textBase.documentSpanIterator(); it.hasNext();){
 *      Span span = (Span)it.next();	
 * and uncomment:
 *	    for (Iterator it = labels.instanceIterator("body"); it.hasNext();) {
 *      Span span = (Span)it.nextSpan();
 *
 *
 * @author Vitor R. Carvalho
 * Created Jun 15, 2004
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
import edu.cmu.minorthird.ui.CommandLineUtil;
import edu.cmu.minorthird.ui.Recommended;
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
  private Classifier meet_model;
  private Classifier ddata_model;
  private static Logger log = Logger.getLogger(SpeechAct.class);
  // serialization stuff
  static public final long serialVersionUID = 1;
  public final int CURRENT_VERSION_NUMBER = 1;

  private SpanFeatureExtractor fe = edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_LC_WORDS;
	
  public SpeechAct() {
    try {
    	//all models below are based on LC_BOW only. 
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
      File meetfile = new File("apps/email/models/meet_Model");//maxent
      meet_model = (Classifier) IOUtil.loadSerialized(meetfile);
      File ddatafile = new File("apps/email/models/ddata_Model");//vp15
      ddata_model = (Classifier) IOUtil.loadSerialized(ddatafile);
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
  
  private boolean bclassify(Classifier model, Instance mi) {
    return model.classification(mi).isPositive();
  }
  
  //------------------------------------------
  public Instance getInstance(String str){
	TextLabels tl = new BasicTextLabels(str);
	return fe.extractInstance(tl, tl.getTextBase().documentSpan("nullId"));		
  }
  public boolean isMeeting(String str) {
    return bclassify(meet_model, getInstance(str));    
  }
  
  public boolean isDData(String str) {
    return bclassify(ddata_model, getInstance(str));    
  }
  
  public boolean isRequest(String str) {
    return bclassify(req_model, getInstance(str));    
  }
  
  public boolean isDelivery(String str) {
    return bclassify(dlv_model, getInstance(str));    
  }
  
  public boolean isCommitment(String str) {
    return bclassify(cmt_model, getInstance(str));    
  }
  
  public boolean isProposal(String str) {
    return bclassify(prop_model, getInstance(str));    
  }
  
  public boolean isDirective(String str) {
    return bclassify(reqamdprop_model, getInstance(str));    
  }
  
  public boolean isCommissive(String str) {
    return bclassify(dlvcmt_model, getInstance(str));    
  }
  
  //------------------------------------------
  
  TextLabels readBsh(File dir, File envfile) throws Exception{
  	System.out.println("reading data files...");
  	TextLabels lala = TextBaseLoader.loadDirOfTaggedFiles(dir);
  	TextBase basevitor = lala.getTextBase();
  	TextLabelsLoader labelLoaderVitor = new TextLabelsLoader();
  	System.out.println("reading env file");
  	labelLoaderVitor.importOps((MutableTextLabels)lala, basevitor, envfile);
  	return lala;  	
  }
  
    //just for fun
  private void createModel(String[] args) throws IOException{
        String mytag = args[1];
        String modelName = mytag+"Model";
		Dataset dataset = new BasicDataset();
		TextLabels labels;
		try {
		  //I hope you have labeled data, otherwise...
		  //labels = Family.readBsh(new File("dummy/"), new File("dummy.env"));
          //labels = Family.readBsh(new File("C:/m3test/total/data/"), new File("C:/m3test/total/env/all"+mytag+".env"));
          labels = readBsh(new File("C:/m3test/total/data/"), new File("C:/m3test/total/env/all"+mytag+".env"));
		  TextBase base = labels.getTextBase();
		  dataset = CommandLineUtil.toDataset(labels, fe, null, mytag);
          //ClassifierLearner learner = new  BatchVersion(new VotedPerceptron(), 15);
          ClassifierLearner learner = new  Recommended.DecisionTreeLearner();

          //if you want to do crosssvalidation tests
		  //Splitter split = Expt.toSplitter("k5");
		  //Evaluation eval = Tester.evaluate(learner, dataset, split);
		  //ViewerFrame frame = new ViewerFrame("numeric demo", eval.toGUI());
		  //eval.summarize();

		  System.out.println("training the Model...");
		  Classifier cl = new DatasetClassifierTeacher(dataset).train(learner);
		  System.out.println("saving model in file..." + modelName);
		  IOUtil.saveSerialized((Serializable) cl, new File(modelName));
	    }
		catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}
  


  public static void main(String[] args) {
    //Usage check
    try {
      if ((args.length < 1)|| (args.length>3)) {
        usage();
        return;
      }
      
      boolean create = false;
	  String opt = args[0];
      if ((opt.startsWith("-create")) || (opt.startsWith("create"))) {
        SpeechAct sa = new SpeechAct();
		sa.createModel(args);
	  }
	  else {
	  	File dir;
	  	String outputFileName = "";
	  	StringBuffer reqBuf = new StringBuffer("Requests or Proposals\n");
	  	boolean sophie = false; 
	  	
	  	//list only the requests in a directory - Sophie's request
	  	//to Sophie - usage: SpeechAct -reqSophie tmpdirectory outputFileName
	  	if ((opt.startsWith("-sophie")) || (opt.startsWith("sophie"))) {
	  		//to Sophie - usage: SpeechAct -reqSophie tmpdirectory outputFileName
	  		sophie = true; 
	  		dir = new File(args[1]);//directory name
	  		outputFileName = args[2];//output file name
	  	}
	    else{
	    	dir = new File(args[0]);
	    }

        SpeechAct sa = new SpeechAct();
        MutableTextLabels labels = TextBaseLoader.loadDirOfTaggedFiles(dir);
        TextBase textBase = labels.getTextBase();
        System.out.println("textbase size = " + textBase.size());
        //TextBaseEditor.edit(labels, new File("moomoomoo"));
        for (Span.Looper it = textBase.documentSpanIterator(); it.hasNext();){
      	  Span span = (Span)it.next();
	      //for (Iterator it = labels.instanceIterator("body"); it.hasNext();) {
          //Span span = (Span)it.nextSpan();
          MutableInstance ins = (MutableInstance)sa.fe.extractInstance(labels, span);
	      boolean reqbool = sa.bclassify(sa.req_model, ins);
	      boolean dlvbool = sa.bclassify(sa.dlv_model, ins);
	      boolean propbool = sa.bclassify(sa.prop_model, ins);
	      boolean cmtbool = sa.bclassify(sa.cmt_model, ins);
	      boolean amdbool = sa.bclassify(sa.amd_model, ins);
	   	  boolean reqamdpropbool = sa.bclassify(sa.reqamdprop_model, ins);
	      boolean dlvcmtbool = sa.bclassify(sa.dlvcmt_model, ins);
	      boolean meetbool = sa.bclassify(sa.meet_model, ins);
	      boolean ddatabool = sa.bclassify(sa.ddata_model, ins);

		  if((sophie)&&(reqamdpropbool)){
		  	reqBuf.append(span.getDocumentId()+"\n");
	 	  }

	    
	      String reqs = reqbool?   "_REQ_":"_____";
	      String dlvs = dlvbool?   "_DLV_":"_____";
	      String props = propbool? "_PROP_":"______";
	      String cmts = cmtbool?   "_CMT_":"_____";
	   	  String amds = amdbool?   "_AMD_":"_____";
		  String reqamdprops = reqamdpropbool? "_REQAMDPROP":"___________";
	      String dlvcmts = dlvcmtbool? "_DLVCMT__":"_________";
	      String meets = meetbool? "__MEET___":"_________";
	      String ddatas = ddatabool? "__dDATA___":"_________";
	      System.out.print(span.getDocumentId()+"     ("+reqs+" "+dlvs+" "+props+" "+cmts+" "+amds+" "+reqamdprops+" "+dlvcmts+" "+meets+" "+ddatas+")\n");
         // String spanString = span.asString();
        }
        //kludge
        if(sophie) LineProcessingUtil.writeToOutputFile(outputFileName, reqBuf);
      }
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      usage();
    }
  }
  
  private static void usage() {
    System.out.println("usage: SpeechAct directoryName");
    System.out.println("\n\n OR, if you have labeled data and want to create a model\n");
    System.out.println("usage: SpeechAct -create VerbAct");
    System.out.println("VerbAct = Req, Dlv, Cmt, Prop, Amd, ReqAmdProp or DlvCmt");
  }
}
