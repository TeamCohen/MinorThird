package email;

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


/**
 *
 * Extracts the signature file lines email messages * 
 * 
 * It follows the description in "Learning to Extract Signature and Reply 
 * Lines from Email", 
 * V.R.Carvalho and W.W.Cohen, CEAS (Conference of Email and Anti-Span), 2004
 *  *
 * @author vitor|AT|cs.cmu.edu
 * May 2004
 *
 * OBS: this implementation assumes the incoming message has a sig file.
 *
 */
 
public class SigFilePredictor 
{
  private final static String tag = "sig";	
  private BinaryClassifier model; 
  private static Logger log = Logger.getLogger(SigFilePredictor.class);
   // serialization stuff
  static public final long serialVersionUID = 1;
  public final int CURRENT_VERSION_NUMBER = 1;
  
  
  //--------------------- Constructors -----------------------------------------------------
  
  public SigFilePredictor(){
  	try{
  		//File file = new File("/afs/cs.cmu.edu/user/vitor/VPsigPredictionModel");
  		File file = new File("apps/email/models/VPsigPredictionModel");
  		//File file = new File("apps/email/models/sigModel");
		model = (BinaryClassifier)IOUtil.loadSerialized(file);  		
  	}
  	catch (Exception e){
        e.printStackTrace();
    }
  }
  	
  public SigFilePredictor(File file){
   	try{
		model = (BinaryClassifier)IOUtil.loadSerialized(file);
  	}
  	catch (Exception e){
        e.printStackTrace();
    }
  }
  
 
  //--------------------- Methods -----------------------------------------------------
  
 /**
  * in case you decide you need a better model on the fly -
  * make sure to change the instance representation accordingly
  *
  * @param string with the filename of the model(classifier)
  */
  public void setClassifier(String newClassifier) 
  {
    try{
     	File file = new File(newClassifier);
  	    model = (BinaryClassifier)IOUtil.loadSerialized(file);
    }
    catch (Exception e){
    	e.printStackTrace();
    }

  }
  
 /**
  * Predicts the sig file lines in the email message.
  * @param email message as String
  * @return ArrayList with instances (set of features)
  */
  public ArrayList Predict(String wholeMessage){

  	WindowRepresentation windowRep = new WindowRepresentation(wholeMessage);
  	MutableInstance[] ins = windowRep.getInstances();
  	
  	ArrayList finalList = windowRep.ClassifyInstances(model, tag);
  	
  	return finalList;
  } 
  
  
  /**
  * Detects if there is a sig in the email message AND
  * predicts (extracts) the signature lines
  *.
  * @param email message as String
  * @return ArrayList with instances (set of features)g
  */
  public ArrayList DetectAndPredict (String wholeMessage){
  	
  	SigFileDetector det = new SigFileDetector();
  	boolean hasSig = det.hasSig(wholeMessage);
  	if(!hasSig) {return null;}  
  	
  	ArrayList temp = Predict(wholeMessage);
  	return temp;
  	
  }
    
      
 //--------------------- Inner Class -----------------------------------------------------   
 
 /**
  * Inner class to represent the message as a sequence of
  * features - using window features (neighbor lines)
  *
  */
 
public static class WindowRepresentation 
{
     private String wholeMessage;
     private String[] arrayOfLines;
     private MutableInstance[] instanceArray;  
     private int[] firstCharIndex;  
     private final int tail_lines = 10; 
     private final int Th = 0;
     
     public WindowRepresentation(String message){
       wholeMessage = message;
       String[] temp_arrayOfLines = LineProcessingUtil.getMessageLines(message);
       createArrayOfLines(temp_arrayOfLines);
     }
     
     public void createArrayOfLines(String[] temp_arrayOfLines){
       arrayOfLines = temp_arrayOfLines;
       int arraysize = temp_arrayOfLines.length;
	   instanceArray = new MutableInstance[arraysize];	   
	   firstCharIndex = new int[arraysize];
	   firstCharIndex[0] = 0;
	   for (int i=1;i<arraysize;i++){
	   	   firstCharIndex[i] = firstCharIndex[i-1] + arrayOfLines[i-1].length() + 1;
	   }
     }

     
    public MutableInstance[] getInstances()
	{		
		MutableInstance[] inst = processMailFile(arrayOfLines);
		return inst;		
	}
	
	public int[] getFirstCharIndex()
	{		
		return firstCharIndex;		
	}
	
	public String getWholeMessage()
	{		
		return wholeMessage;		
	}
	
	public String[] getArrayOfLines(){
		return arrayOfLines;
	}
	
    public ArrayList ClassifyInstances(BinaryClassifier model, String tag)
    {
  	
  	    ArrayList bemlocal = new ArrayList();
  	    int charBegin;
		for(int i=0; i<instanceArray.length;i++){
			
			boolean decision = (model.score(instanceArray[i])<Th)? false:true;
			
			if(decision){
				//System.out.println("                                POSITIVE = " +i);
				//System.out.println(instanceArray[i].toString());
				//System.out.println(arrayOfLines[i]);
				log.debug(arrayOfLines[i]);
				charBegin = wholeMessage.indexOf(arrayOfLines[i], firstCharIndex[i]-1);
				if(charBegin<0) charBegin = firstCharIndex[i]; //just in case
				bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length()+1, tag));
			}			
		}
		log.debug("\n\n");
  		return bemlocal;
    }
       
    
	/** 
	 * @param email message as a String[] of lines
	 * @return same msg represented as an MutableInstance[]
	 *
	 * DON'T change any of these feature functions - in case you do, 
	 * you'll need a new classifier (model) trained on the new feature set.
	 *
	*/
	private MutableInstance[] processMailFile(String[] arrayOfLines)
	{
	    int fromLine = findFromLine(arrayOfLines);
	    int size = arrayOfLines.length;
	    
		for(int i=0; i<size; i++){

			instanceArray[i]=new MutableInstance();
			//list of features:
		    //check first line feature
		    if(i==0){instanceArray[i].addBinary(new Feature("firstL"));}
		  	if(i==1){instanceArray[i].addBinary(new Feature("secondL"));}
			
			//check last line feature
		  	if(i==size-1){instanceArray[i].addBinary(new Feature("lastL"));}
		  	if(i==size-2){instanceArray[i].addBinary(new Feature("lastbutoneL"));}
		  	if(i==size-3){instanceArray[i].addBinary(new Feature("lastbutbutoneL"));}
			
			//header feature
			if (LineProcessingUtil.lineMatcher("^\\s?\\s?[\\w|\\-]+\\:", arrayOfLines[i]))
	 	    {
	 	    	if(!LineProcessingUtil.lineMatcher("^\\s?\\s?(http|HTTP|Phone|PHONE|phone|email|EMAIL|Internet|INTERNET|internet)+\\:", arrayOfLines[i])){
	 	    		instanceArray[i].addBinary(new Feature("header"));
	 	    		if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeheader"));
	 	    	}	 	  	       	 	     
	 	    }
	 	  
	 	    //blank line features
	 	    if(i>1){
				if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*$", arrayOfLines[i-2])){
					instanceArray[i].addBinary( new Feature("prevprevblankL"));
					if((size - i)<tail_lines+2) instanceArray[i].addBinary( new Feature("closeprevprevblankL"));
				}			  
			}
	 	    if(i>0){
				if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*$", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevblankL"));
					if((size - i)<tail_lines+1) instanceArray[i].addBinary( new Feature("closeprevblankL"));
				}			  
			}
			if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*$", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("blankL"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeblankL"));
			}
			else{
				instanceArray[i].addBinary( new Feature("notblankL"));
			}
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*$", arrayOfLines[i+1])){
					instanceArray[i].addBinary( new Feature("nextblankL"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextblankL"));
				}				  
			}
			if(i< size -2){
				if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*$", arrayOfLines[i+2])){
					instanceArray[i].addBinary( new Feature("nextnextblankL"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextnextblankL"));
				}				  
			}
			
			//sig marker feature
			if(i>0){
				if(LineProcessingUtil.lineMatcher("^[\\s]*---*[\\s]*$", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevsigMarker"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevsigMarker"));
				}				  
			}
			if(LineProcessingUtil.lineMatcher("^[\\s]*---*[\\s]*$", arrayOfLines[i])){			
				instanceArray[i].addBinary( new Feature("sigMarker"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closesigMarker"));
			}
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^[\\s]*---*[\\s]*$", arrayOfLines[i+1]))
				  instanceArray[i].addBinary( new Feature("nextsigMarker"));
				  if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextsigMarker"));
			}
			
			//trueSigMarker - post-addition
			if(i>3){
		  	 	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i-4])){
		  	 		instanceArray[i].addBinary( new Feature("prevprevprevprevtruesigMarker"));
		  	 		if((size - i)<tail_lines+4) instanceArray[i].addBinary( new Feature("prevprevprevprevclosetruesigMarker"));
		  	 	}		     		
		    }
			if(i>2){
		  	 	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i-3])){
		  	 		instanceArray[i].addBinary( new Feature("prevprevprevtruesigMarker"));
		  	 		if((size - i)<tail_lines+3) instanceArray[i].addBinary( new Feature("prevprevprevclosetruesigMarker"));
		  	 	}		     		
		    }
		  	if(i>1){
		  	 	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i-2])){
		  	 		instanceArray[i].addBinary( new Feature("prevprevtruesigMarker"));
		  	 		if((size - i)<tail_lines+2) instanceArray[i].addBinary( new Feature("prevprevclosetruesigMarker"));
		  	 	}
		     		
		    }
		  	if(i>0) {
		     	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i-1])){
		     		instanceArray[i].addBinary( new Feature("prevtruesigMarker"));
		     		if((size - i)<tail_lines+1) instanceArray[i].addBinary( new Feature("prevclosetruesigMarker"));
		     	}
		    }
		    if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i])){
		    	instanceArray[i].addBinary( new Feature("truesigMarker"));
		    	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closetruesigMarker"));
		    }
		     		
		     		
		    if(i< size -1) {
		     	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i+1])){
		     		instanceArray[i].addBinary( new Feature("nexttruesigMarker"));
		     		if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("nextclosetruesigMarker"));
		     	}
		    }
		    if(i< size -2) {
		     	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i+2])){
		     		instanceArray[i].addBinary( new Feature("nextnexttruesigMarker"));
		     		if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("nextnextclosetruesigMarker"));
		     	}
		    }
		    if(i< size -3) {
		     	if (LineProcessingUtil.lineMatcher("^[\\s]?[\\s]?---?[\\s]*$", arrayOfLines[i+3])){
		     		instanceArray[i].addBinary( new Feature("nextnextnexttruesigMarker"));
		     		if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("nextnextnextclosetruesigMarker"));
		     	}
		    }
			
			//other markers features		
			if(i>0){
				if(LineProcessingUtil.lineMatcher("^[\\s]*([\\*]|#|[\\+]|[\\^]|-|[\\~]|[\\&]|[////]|[\\$]|_|[\\!]|[\\/]|[\\%]|[\\:]|[\\=]){10,}[\\s]*$", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevotherMarkers"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevotherMarkers"));
				}				 
			}
			if(LineProcessingUtil.lineMatcher("^[\\s]*([\\*]|#|[\\+]|[\\^]|-|[\\~]|[\\&]|[////]|[\\$]|_|[\\!]|[\\/]|[\\%]|[\\:]|[\\=]){10,}[\\s]*$", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("otherMarkers"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeotherMarkers"));
			}
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^[\\s]*([\\*]|#|[\\+]|[\\^]|-|[\\~]|[\\&]|[////]|[\\$]|_|[\\!]|[\\/]|[\\%]|[\\:]|[\\=]){10,}[\\s]*$", arrayOfLines[i+1])){
					instanceArray[i].addBinary( new Feature("nextotherMarkers"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextotherMarkers"));
				}				  
			}
			
			//special works feature
			if(i>0){
				if(LineProcessingUtil.lineMatcher("Dept\\.|University|Corp\\.|Corporations?|College|Ave\\.|Laboratory|[D|d]isclaimer|Division|Professor|Laboratories|Institutes?|Services|Engineering|Director|Sciences?|Address|Fax|Office|Mobile|Phone|Manager|Street|St\\.|Avenue", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevspecWords"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevspecWords"));
				}				  
			}
			if(LineProcessingUtil.lineMatcher("Dept\\.|University|Corp\\.|Corporations?|College|Ave\\.|Laboratory|[D|d]isclaimer|Division|Professor|Laboratories|Institutes?|Services|Engineering|Director|Sciences?|Address|Fax|Office|Mobile|Phone|Manager|Street|St\\.|Avenue", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("specWords"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closespecWords"));
			}		
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("Dept\\.|University|Corp\\.|Corporations?|College|Ave\\.|Laboratory|[D|d]isclaimer|Division|Professor|Laboratories|Institutes?|Services|Engineering|Director|Sciences?|Address|Fax|Office|Mobile|Phone|Manager|Street|St\\.|Avenue", arrayOfLines[i+1])){
					instanceArray[i].addBinary( new Feature("nextspecWords"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextspecWords"));
				}				  
			}
			
			//email feature
			if(i>0){
				if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][\\w|\\+|\\.|\\_|\\-]+\\@[\\w|\\-|\\_|\\.]+\\.[a-zA-z]{2,5}[^(\\<|\\>)]", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevemail"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevemail"));
				}
				  
			}
			if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][\\w|\\+|\\.|\\_|\\-]+\\@[\\w|\\-|\\_|\\.]+\\.[a-zA-z]{2,5}[^(\\<|\\>)]", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("email"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeemail"));
			}
				

			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][\\w|\\+|\\.|\\_|\\-]+\\@[\\w|\\-|\\_|\\.]+\\.[a-zA-z]{2,5}[^(\\<|\\>)]", arrayOfLines[i+1])){
					instanceArray[i].addBinary( new Feature("nextemail"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextemail"));
				}
				  
			}
			if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][(\\w|\\+|\\_|\\-)]+\\@[(\\w|\\-|\\_)]+[\\.][a-zA-z]{2,5}", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("emailB"));//short emails
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeemailB"));
			}			
			
			//URL feature
			if(i>0){
				if(LineProcessingUtil.lineMatcher("[\\s](http\\:\\/\\/)*(www|web|w3)*(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)*[\\w]+", arrayOfLines[i-1])){
					instanceArray[i].addBinary( new Feature("prevurl"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevurl"));
				}				  
			}
			if(LineProcessingUtil.lineMatcher("[\\s](http\\:\\/\\/)*(www|web|w3)*(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)*[\\w]+", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("url"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeurl"));
			}						
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("[\\s](http\\:\\/\\/)*(www|web|w3)*(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)*[\\w]+", arrayOfLines[i+1])){
					instanceArray[i].addBinary( new Feature("nexturl"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("nextprevurl"));
				}
			}			
			
			//phone
			if(i>0){
				if(LineProcessingUtil.lineMatcher("(\\-?\\d)*\\d\\d\\s?\\-?\\s?\\d\\d\\d\\d", arrayOfLines[i-1]))
				  instanceArray[i].addBinary( new Feature("prevphone"));
			}
			if(LineProcessingUtil.lineMatcher("(\\-?\\d)*\\d\\d\\s?\\-?\\s?\\d\\d\\d\\d", arrayOfLines[i]))
				instanceArray[i].addBinary( new Feature("phone"));
				
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("(\\-?\\d)*\\d\\d\\s?\\-?\\s?\\d\\d\\d\\d", arrayOfLines[i+1]))
				  instanceArray[i].addBinary( new Feature("nextphone"));
			}
			
			//names like Vitor R. Carvalho or John F. Kennedy
			if(LineProcessingUtil.lineMatcher("[A-Z][a-z]+\\s\\s?[A-Z][\\.]?\\s\\s?[A-Z][a-z]+", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("namepat"));
				if((size - i)<tail_lines) instanceArray[i].addBinary(new Feature("closenamepat"));
			}
				
				
			//end-of-line quotes
			if(LineProcessingUtil.lineMatcher("\"$", arrayOfLines[i])){
				instanceArray[i].addBinary( new Feature("endQuote"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeendQuote"));
			}
				
					
			//FROM line feature
			if (fromLine > 0){
				if(SigFilePredictor.detectFromName(arrayOfLines[fromLine], arrayOfLines[i])){
					instanceArray[i].addBinary( new Feature("fromL"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closefromL"));
				}
			    if(i< size -1){
				    if(SigFilePredictor.detectFromName(arrayOfLines[fromLine], arrayOfLines[i])){
						instanceArray[i].addBinary( new Feature("nextfromL"));
						if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextfromL"));
					}
				}
			}
			
			//reply symbol
			if(i>0){
				if(LineProcessingUtil.lineMatcher("^\\>.*", arrayOfLines[i-1]))
				  instanceArray[i].addBinary( new Feature("prevreplySymbol"));
			}
			if(LineProcessingUtil.lineMatcher("^\\>.*", arrayOfLines[i]))
				instanceArray[i].addBinary( new Feature("replySymbol"));
	
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^\\>.*", arrayOfLines[i+1]))
				  instanceArray[i].addBinary( new Feature("nextreplySymbol"));
			}
			
			
			//other reply symbol             
			if(i>0){
				if(LineProcessingUtil.lineMatcher("^[\\=|\\:|\\#|\\:|\\-|\\+|\\&|\\%|\\}]\\s*\\w+.*", arrayOfLines[i-1]))
				  instanceArray[i].addBinary(new Feature("prevotherreplySymbol"));
			}
			if(LineProcessingUtil.lineMatcher("^[\\=|\\:|\\#|\\:|\\-|\\+|\\&|\\%|\\}]\\s*\\w+.*", arrayOfLines[i]))
				instanceArray[i].addBinary(new Feature("otherreplySymbol"));
			
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^[\\=|\\:|\\#|\\:|\\-|\\+|\\&|\\%|\\}]\\s*\\w+.*", arrayOfLines[i+1]))
				  instanceArray[i].addBinary(new Feature("nextotherreplySymbol"));
			}
			
			//punct starting and followed by ">"
			if(i>0){
				if(LineProcessingUtil.lineMatcher("^\\p{Punct}{1,2}\\>.*", arrayOfLines[i-1]))
				  instanceArray[i].addBinary(new Feature("prevpunct"));
			}
			if(LineProcessingUtil.lineMatcher("^\\p{Punct}{1,2}\\>.*", arrayOfLines[i]))
				instanceArray[i].addBinary(new Feature("punct"));
		
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher("^\\p{Punct}{1,2}\\>.*", arrayOfLines[i+1]))
				  instanceArray[i].addBinary(new Feature("nextpunct"));
			}
			
			//writes and wrote features
			if(i>0){
				if(LineProcessingUtil.lineMatcher(" writes:$", arrayOfLines[i-1]))
				  instanceArray[i].addBinary(new Feature("prevwrites"));
				if(LineProcessingUtil.lineMatcher(" wrote:$", arrayOfLines[i-1]))
				  instanceArray[i].addBinary(new Feature("prevwrote"));  
			}
			if(LineProcessingUtil.lineMatcher(" writes:$", arrayOfLines[i]))
				instanceArray[i].addBinary(new Feature("writes"));
			if(LineProcessingUtil.lineMatcher(" wrote:$", arrayOfLines[i]))
				instanceArray[i].addBinary(new Feature("wrote"));
		
			if(i< size -1){
				if(LineProcessingUtil.lineMatcher(" writes:$", arrayOfLines[i+1]))
				  instanceArray[i].addBinary(new Feature("nextwrites"));
				if(LineProcessingUtil.lineMatcher(" wrote:$", arrayOfLines[i+1]))
				  instanceArray[i].addBinary(new Feature("nextwrote"));
			}
			
			//same initial punct characters
			if((i>0)&&(LineProcessingUtil.startWithSameInitialPunctCharacters(arrayOfLines[i], arrayOfLines[i-1]))){
				instanceArray[i].addBinary(new Feature("prevsicline"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevsicline"));
			}
			if((i<size-1)&&(LineProcessingUtil.startWithSameInitialPunctCharacters(arrayOfLines[i], arrayOfLines[i+1]))){
				instanceArray[i].addBinary( new Feature("nextsicline"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextsicline"));
			}
			
			//number of leading tabs
	   		int ddd = LineProcessingUtil.indentNumber(arrayOfLines[i]);
		    if(ddd==1){instanceArray[i].addBinary( new Feature("indentUni"));
		               if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeindentUni"));}
		    if(ddd==2){instanceArray[i].addBinary( new Feature("indentBi"));
		    		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeindentBi"));}
		    if(ddd>=3){instanceArray[i].addBinary( new Feature("indentTri"));
		    		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeindentTri"));}
		    
		    if(i>0){
		    	ddd = LineProcessingUtil.indentNumber(arrayOfLines[i-1]);
		        if(ddd==1){instanceArray[i].addBinary( new Feature("previndentUni"));
		        		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevindentUni"));}
		        if(ddd==2){instanceArray[i].addBinary( new Feature("previndentBi"));
		        		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevindentBi"));}
		        if(ddd>=3){instanceArray[i].addBinary( new Feature("previndentTri"));
		        		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closeprevindentTri"));}		    	
		    }
		    
		    if(i<size-1){
		    	ddd = LineProcessingUtil.indentNumber(arrayOfLines[i+1]);
		        if(ddd==1){instanceArray[i].addBinary( new Feature("nextindentUni"));
		        		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextindentUni"));}
		        if(ddd==2){instanceArray[i].addBinary( new Feature("nextindentBi"));
		        		   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextindentBi"));}
		    	if(ddd>=3){instanceArray[i].addBinary( new Feature("nextindentTri"));
		    			   if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closenextindentTri"));}		    	
		    }
		    
		    //punctuation percentage
		    double temp = LineProcessingUtil.punctuationPercentage(arrayOfLines[i]);
		    if (temp>0.20){
		    	instanceArray[i].addBinary( new Feature("punctPerc20"));
		    	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc20"));
		    }
		    else {
		    	instanceArray[i].addBinary( new Feature("punctPerc0"));
		    	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc20"));
		    }
			if (temp>0.50){
				instanceArray[i].addBinary( new Feature("punctPerc50"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc50"));
			}
			if (temp>0.75){
				instanceArray[i].addBinary( new Feature("punctPerc75"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc75"));
			}
			if (temp>0.90){
				instanceArray[i].addBinary( new Feature("punctPerc90"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc90"));
			}
			
			if (i>0){
				temp = LineProcessingUtil.punctuationPercentage(arrayOfLines[i-1]);
		        if (temp>0.20){
		        	instanceArray[i].addBinary( new Feature("punctPerc20prev"));
		        	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc20prev"));
		        }
				if (temp>0.50){
					instanceArray[i].addBinary( new Feature("punctPerc50prev"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc50prev"));
				}
				if (temp>0.75){
					instanceArray[i].addBinary( new Feature("punctPerc75prev"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc75prev"));
				}
				if (temp>0.90){
					instanceArray[i].addBinary( new Feature("punctPerc90prev"));				
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc90prev"));
				}
			}
			if (i<size-1){
				temp = LineProcessingUtil.punctuationPercentage(arrayOfLines[i+1]);
		        if (temp>0.20){
		        	instanceArray[i].addBinary( new Feature("punctPerc20next"));
		        	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc20next"));
		        }
				if (temp>0.50){
					instanceArray[i].addBinary( new Feature("punctPerc50next"));
				   	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc50next"));
		        }
				if (temp>0.75){
					instanceArray[i].addBinary( new Feature("punctPerc75next"));
				  	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc75next"));
		        }
				if (temp>0.90){
					instanceArray[i].addBinary( new Feature("punctPerc90next"));				
				    if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closepunctPerc90next"));
		        }
			}
			
			//word characters percentage
			temp = LineProcessingUtil.wordCharactersPercentage(arrayOfLines[i]);
			if (temp<0.10){
				instanceArray[i].addBinary( new Feature("charPerc10"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc10"));
			}
			if (temp<0.30){
				instanceArray[i].addBinary( new Feature("charPerc30"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc30"));
			}
			if (temp<0.60){
				instanceArray[i].addBinary( new Feature("charPerc60"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc60"));
			}
			if (temp<0.90){
				instanceArray[i].addBinary( new Feature("charPerc90"));
				if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc90"));
			}
			
			if (i>0){
				temp = LineProcessingUtil.wordCharactersPercentage(arrayOfLines[i-1]);
			    if (temp<0.10){
			    	instanceArray[i].addBinary( new Feature("charPerc10prev"));
			       	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc10prev"));
		        }
				if (temp<0.30){
					instanceArray[i].addBinary( new Feature("charPerc30prev"));
				   	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc30prev"));
		        }
				if (temp<0.60){
					instanceArray[i].addBinary( new Feature("charPerc60prev"));
				   	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc60prev"));
		        }
				if (temp<0.90){
					instanceArray[i].addBinary( new Feature("charPerc90prev"));				
				   	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc90prev"));
		        }
			}
			if (i<size-1){
				temp = LineProcessingUtil.wordCharactersPercentage(arrayOfLines[i+1]);
			    if (temp<0.10){
			    	instanceArray[i].addBinary( new Feature("charPerc10next"));
			       	if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc10next"));
		        }
				if (temp<0.30){
					instanceArray[i].addBinary( new Feature("charPerc30next"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc30next"));
		        }
				if (temp<0.60){
					instanceArray[i].addBinary( new Feature("charPerc60next"));
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc60next"));
		        }
				if (temp<0.90){
					instanceArray[i].addBinary( new Feature("charPerc90next"));				
					if((size - i)<tail_lines) instanceArray[i].addBinary( new Feature("closecharPerc90next"));
		        }
			}

		}//end of for

		return instanceArray;
	}
		
	//find the line where the from: information is
	private int findFromLine(String[] arrayOfLines)
	{		
		int fromLine = -1;
		int size = arrayOfLines.length;
	 	for (int i = 0; i<size-1; i++){	 		
	 	   if(LineProcessingUtil.lineMatcher("^\\s?\\s?\\p{Punct}?[F|f][R|r][O|o][M|m]\\:", arrayOfLines[i])){
	 	   	 fromLine = i; 
	 	   	 break;
	 	   }	 
	 	}	 	   
		return fromLine;	
	}	     	 	  
} 
    
  
//--------------------- Main method / Test routines / Support methods -----------------------------------------------------
    /** 
     * From Line feature function: extracts a "name" from the fromLine of
     * an email message and attempts to match any of its components with
     * the words in the target line
     * In other words, if a piece of the sender's name is detected in this line, it
     * returns true. False, otherwise.
     *
     * @param fromLine in String format
     * @param testLine in String format
     * @return true, if any part of the sender's name is found. 
     *
     */
  public static boolean detectFromName(String tmp, String testLine)
	{
	    String inputStr = tmp;
	    
	    //try first pattern name first (Vitor R. Carvalho)
	    String patternStr = "([A-Z][a-z]+\\s\\s?[A-Z]?[\\.]?\\s\\s?([A-Z][a-z]+))";
	
	    // Compile and use regular expression
	    Pattern mypattern = Pattern.compile(patternStr);
	    Matcher matcher = mypattern.matcher(inputStr);
	    boolean matchFound = matcher.find();
	
	    if (matchFound)
	    {
	      int groupsize = matcher.groupCount() + 1;
	      String[] groupStr = new String[groupsize];	
	      for (int j = 0; j <= matcher.groupCount(); j++)
	      {
	        groupStr[j] = matcher.group(j);
	        if (LineProcessingUtil.lineMatcher(groupStr[j], testLine))
	        {
	          return true;
	        }
	      }
	    }
	    else
	    {
	      //try another string pattern (Vitor Carvalho)
	      patternStr = "([A-Z][a-z]+\\s\\s?([A-Z][a-z]+))";
	
	      // Compile and use regular expression
	      Pattern myPattern = Pattern.compile(patternStr);
	      Matcher matcher2 = myPattern.matcher(inputStr);
	      boolean newMatchFound = matcher2.find();
	
	      if (newMatchFound)
	      {
	        int groupsize = matcher.groupCount() + 1;
	        String[] groupStr = new String[groupsize];	
	        for (int j = 0; j <= matcher2.groupCount(); j++)
	        {
	          groupStr[j] = matcher2.group(j);
	          if (LineProcessingUtil.lineMatcher(groupStr[j], testLine))
	          {
	            return true;
	          }
	        }
	      }
   		}
	    
    //in case nothing was found
    return false;
  }


  static public void createModel(String[] args, String linetag) throws IOException{
  	String modelName = linetag+"Model";
  	Dataset dataset = new BasicDataset();
    
    //starts from position 1
    //args is teh file array, but the first position is not used here
  	for(int j=1; j< args.length; j++){
  		
  		//parse the message
                
        String message = LineProcessingUtil.readFile(args[j]);
        String[] strOfLines = LineProcessingUtil.getMessageLines(message);
        ClassLabel[] linelabel = new ClassLabel[strOfLines.length];
        
        for(int i=0; i<strOfLines.length; i++){
        	if(strOfLines[i].startsWith("#sig# ")){
        		strOfLines[i] = strOfLines[i].substring(6);
        		if(linetag.compareTo("sig")==0) linelabel[i] =  ClassLabel.binaryLabel(+1);
        		else linelabel[i] = ClassLabel.binaryLabel(-1);
        	}
   	        else if(strOfLines[i].startsWith("#reply#")){
      	     	strOfLines[i] = strOfLines[i].substring(7); 
        		if(linetag.compareTo("reply")==0) linelabel[i] =  ClassLabel.binaryLabel(+1);
        		else linelabel[i] =  ClassLabel.binaryLabel(-1);      	     	   	     	
    	    }
		    else{
		     	linelabel[i] =  ClassLabel.binaryLabel(-1);
		     }		      
        }        
        
     	WindowRepresentation windowRep = new SigFilePredictor.WindowRepresentation(message);
     	windowRep.createArrayOfLines(strOfLines);//to exclude #sig# from feature extraction 		
 		MutableInstance[] ins = windowRep.getInstances();
 		for(int i=0; i<strOfLines.length; i++){
 		Example example = new Example((Instance)ins[i], linelabel[i]);
 		dataset.add(example);
 		}
 		
    }
    System.out.println("dataset size = " +dataset.size());
    
    //just to compare with paper performance
    //ClassifierLearner learner2 = new AdaBoost(); 
    //new BatchVersion(new VotedPerceptron(), 15);
    //Splitter splitter = Expt.toSplitter("k5");
    //Evaluation eval = Tester.evaluate(learner2, dataset, splitter);
    //ViewerFrame frame = new ViewerFrame("numeric demo", eval.toGUI());
    
    System.out.println("training the Model...");
    ClassifierLearner learner = new BatchVersion(new VotedPerceptron(), 15); 
    Classifier cl = new DatasetClassifierTeacher(dataset).train(learner);
	System.out.println("saving model in file..."+modelName);
	IOUtil.saveSerialized((Serializable)cl, new File(modelName));
	return;
  }


  static public void main(String[] args) 
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
       	  SigFilePredictor.createModel(args, "sig");       	
       }       
       else{ //prediction mode
          System.out.println("For details, set the verbosity level in config/log4j.properties\n");
       	  //SigFilePredictor pred = new SigFilePredictor(new File("/afs/cs.cmu.edu/user/vitor/VPsigPredictionModel");
          SigFilePredictor pred = new SigFilePredictor();

          for(int i=0; i< args.length; i++){
       	     System.out.println(args[i]);
         	 String wholeMessage = LineProcessingUtil.readFile(args[i]);
     	 	 ArrayList onelist = pred.Predict(wholeMessage);
 		  // System.out.print(onelist.toString()); 
          }
       }        	
    } catch (Exception e) {
	  usage();
      e.printStackTrace();
    }
  } 
  
  private static void usage()
  {
  	 System.out.println("usage: SigFilePredictor filename1 filename2 ...");
     System.out.println("OR");
     System.out.println("usage: SigFilePredictor -create filename1 filename2 ...");
     System.out.println("PS: to create, use \"Signature and Reply Dataset\" annotation stile as in www.cs.cmu.edu/~vitor/codeAndData.html");
  }   
 	 	  
} 

