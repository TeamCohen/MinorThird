package email;

import edu.cmu.minorthird.text.CharAnnotation;
import edu.cmu.minorthird.text.StringAnnotator;
import edu.cmu.minorthird.util.*;
import java.io.*;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;


/*
 * Extracts the reply lines from email messages * 
 * 
 * It follows the description in "Learning to Extract Signature and Reply 
 * Lines from Email", 
 * V.R.Carvalho and W.W.Cohen, CEAS (Conference of Email and Anti-Span), 2004
 * 
 * @author Vitor R. Carvalho
 * May 2004
 */
public class ReplyToAnnotator extends StringAnnotator
{

  private BinaryClassifier model; 
  private static final int Th = 0;
  private static Logger log = Logger.getLogger(ReplyToAnnotator.class);
   // serialization stuff
  static private final long serialVersionUID = 1;
  private final int CURRENT_VERSION_NUMBER = 1;
	
 //--------------------- Constructors -----------------------------------------------------
 	 
	
  public ReplyToAnnotator(){
  	try{
  		File file = new File("apps/email/models/VPreplyModel");
  		//File file = new File("apps/email/models/replyModel");
		model = (BinaryClassifier)IOUtil.loadSerialized(file);  		
  	}
  	catch (Exception e){
        e.printStackTrace();
    }
  }
  	
  public ReplyToAnnotator(File file){
   	try{
		model = (BinaryClassifier)IOUtil.loadSerialized(file);
  	}
  	catch (Exception e){
        e.printStackTrace();
    }
  }
  	
	
//--------------------- Methods -----------------------------------------------------

  //	
  protected CharAnnotation[] annotateString(String spanString) 
  {    	  	
    ArrayList list = this.Predict(spanString);    	
    if(list.isEmpty()){return null;} 
    CharAnnotation[] cann = (CharAnnotation[])list.toArray(new CharAnnotation[list.size()]);
	return cann;
  }

  public String explainAnnotation(edu.cmu.minorthird.text.TextLabels labels, edu.cmu.minorthird.text.Span documentSpan)
  {
    return "reply-to extraction - not implemented yet!";
  }
  
 /*
 *  Classifies the lines of the incoming message, as being or not a 
 *  reply line
 *
 *  @param email message in string representation
 *  @return an ArrayList with all reply lines in a CharAnnotation[] format 
 */
  private ArrayList Predict(String msg)
  {  
  	SigFilePredictor.WindowRepresentation windowRep = new SigFilePredictor.WindowRepresentation(msg);
   	ArrayList herelist = ClassifyInstances(windowRep, "reply");
   	return herelist;    	
  }
  
 /*
 *  Classifies the lines in reply or non-reply lines
 *  - It calls the serializable classifier to each msg line
 *  @param windowRepresentation of the message (inner class of SigFilePredictor)
 *  @param tag to be used ("reply")
 *  @return an ArrayList with all reply lines in a CharAnnotation[] format
 * 
 */
  private ArrayList ClassifyInstances(SigFilePredictor.WindowRepresentation windowRep, String tag)
  {  	
	ArrayList bemlocal = new ArrayList();
	MutableInstance[] ins = windowRep.getInstances();
	int[] firstCharIndex = windowRep.getFirstCharIndex();
	String[] arrayOfLines = windowRep.getArrayOfLines();
	String wholeMessage = windowRep.getWholeMessage();
	for(int i=0; i<ins.length;i++){
			
		boolean decision = (model.score(ins[i])<Th)? false:true;
	    int charBegin;
		if(decision){
			//System.out.println("POSITIVE = " +i);//to debug
			//System.out.println(ins[i].toString());
			log.debug(arrayOfLines[i]);
			charBegin = wholeMessage.indexOf(arrayOfLines[i], firstCharIndex[i]-1);
			if(charBegin<0) charBegin = firstCharIndex[i]; //just in case
			//bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length(), tag));
			bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length()+1, tag));
		}			
	}
	log.debug("\n\n");
	return bemlocal;  	
  }
  

//--------------------- main method/testing -----------------------------------------------------
  
  //for testing purposes
  public static void main(String[] args)
  {
    try {    	
       //Usage check
       if (args.length < 1)
       {
         usage();
         return;
       }	
       
		//parsing inputs
       	boolean create = false;
       	String opt = args[0];
		if ((opt.startsWith("-create"))||(opt.startsWith("create"))) {
			create = true;
		}
      
       if(create){ //creates a model
       	  SigFilePredictor.createModel(args, "reply");       	
       }       
       else{ //prediction mode
          System.out.println("For details, set the verbosity level in config/log4j.properties\n");
          ReplyToAnnotator repto = new ReplyToAnnotator();

          for(int i=0; i< args.length; i++){
       	     System.out.println(args[i]);
         	 String message = LineProcessingUtil.readFile(args[i]);
     	 	 CharAnnotation[] onelist = repto.annotateString(message);
 		     //System.out.print(onelist.toString()); 
          }
       }    	
    } catch (Exception e) {
         usage();
         e.printStackTrace();
    }
  } 
  
  private static void usage()
  {
  	 System.out.println("usage: ReplyToAnnotator filename1 filename2 ...");
     System.out.println("OR");
     System.out.println("usage: ReplyToAnnotator -create filename1 filename2 ...");
     System.out.println("to create, use \"Signature and Reply Dataset\" annotation stile as in www.cs.cmu.edu/~vitor/codeAndData.html");
  }  
}