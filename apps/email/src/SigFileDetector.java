package email;

import java.io.*; 
import java.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import java.util.regex.*;
import java.math.*;
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
 * Classify an incoming email message as having or not a Signature File.
 * 
 * It follows the description in "Learning to Extract Signature and Reply Lines from Email", 
 * V.R.Carvalho and W.W.Cohen, CEAS (Conference of Email and Anti-Span), 2004
 *
 * Uses the last 10 last lines of email message.
 *
 * @author Vitor R. Carvalho  -  May 2004
 */
public class SigFileDetector 
{
	
  private BinaryClassifier classifier;
  
 // serialization stuff
  static public final long serialVersionUID = 1;
  public final int CURRENT_VERSION_NUMBER = 1;
 
  
  //--------------------- Constructors -----------------------------------------------------
  
  public SigFileDetector()
  {
  	try
  	{
  		//File file = new File("models/sigDetectionAdaBoostModel");
  		File file = new File("apps/email/models/VPsigDetectionModel");
        //File file = new File("apps/email/models/AB99sigDetectionModel");
		classifier = (BinaryClassifier)IOUtil.loadSerialized(file);  		
  	}
  	catch (Exception e)
    {
        e.printStackTrace();
    }
  }
  	
  public SigFileDetector(File file) 
  {
   	try
  	{
		classifier = (BinaryClassifier)IOUtil.loadSerialized(file);
  	}
  	catch (Exception e)
    {
        e.printStackTrace();
    }
  }
  
 
  //--------------------- Methods -----------------------------------------------------
  
 /**
  * in case you decide you need a better model -
  * make sure to change the instance representation accordingly
  */
  public void setClassifier(String newClassifier) 
  {
    try
    {
     	File file = new File(newClassifier);
  	    classifier = (BinaryClassifier)IOUtil.loadSerialized(file);
    }
    catch (Exception e)
    {
    	e.printStackTrace();
    }

  }
  
 /**
  * Detects if there is a sig in the email message.
  * @param email message as String
  * @return boolean - true, the msg has a sig. False, otherwise.
  */
  public boolean hasSig(String wholeMessage){

  	// detect binary features on message and
  	// returns a single instance representing this message
  	SigDetectorByLine byLine = new SigDetectorByLine();
  	Instance instance = byLine.getInstance(wholeMessage);

  	//apply the classifier to the instance
  	boolean decision = (classifier.score(instance)<0)? false:true;  	
  	return decision;
  } 
  
      
 //--------------------- Inner Class -----------------------------------------------------   
 
 /**
  * Inner class to help extracting the features 
  * from last line of message.
  */
public class SigDetectorByLine 
{
     //window for search of features - usually last tail_lines lines
     private final int tail_lines = 10;	          
     private int firstSearchLine = 0;
     private int fromLine = 0;
     private int lastSearchLine;
     private MutableInstance instance;
     
     public SigDetectorByLine(){
       firstSearchLine = 0;
	   fromLine = 0;
	   instance = new MutableInstance();
     }
     
    private MutableInstance getInstance(String wholeMessage)
	{		
		clear();
		String[] strArray = preProcessMailMessage(wholeMessage);
		MutableInstance inst = processMailFile(strArray);
		return inst;		
	}

	//reset parameters, in case a the same object is called to several messages
	private void clear()
	{
	  firstSearchLine = 0;
	  fromLine = 0;
	  instance = new MutableInstance();
	}

	/**
	 * Detects the presence of any of the features in the last tail_lines
	 * lines of mail file 
	 *
	 * @param the String[] with the message lines
	 * @return the instance (set of features) representing the message
	 *
	 * OBS: DON'T change any of these feature functions - in case you do, 
	 * you'll need a new classifier trained on the new feature set.
	 *
	*/
	private MutableInstance processMailFile(String[] arrayOfLines)
	{
		int ind; double temp = 0;
		if(lastSearchLine<=firstSearchLine){
			throw new IllegalStateException("ERROR parsing message");
	    }
	    
		for(int i=lastSearchLine; i>=firstSearchLine; i--){
			
			ind = lastSearchLine - i;//line index, for instance, the 3rd to last line
			//list of features
			if(LineProcessingUtil.lineMatcher("^[\\s]*---*[\\s]*$", arrayOfLines[i])){
				instance.addBinary( new Feature("sigMarker"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("^[\\s|\\t]?[\\s|\\t]?---?[\\s|\\t]*$", arrayOfLines[i])){
				instance.addBinary( new Feature("sigBeginMarker"+ind));
			}

			if(LineProcessingUtil.lineMatcher("^[\\s|\\t]*([\\*]|#|[\\+]|[\\^]|-|[\\~]|[\\&]|[////]|[\\$]|_|[\\!]|[\\/]|[\\%]|[\\:]|[\\=]){10,}[\\s]*$", arrayOfLines[i])){
				instance.addBinary( new Feature("otherMarkers"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("Dept\\.|University|Corp\\.|Corporations?|College|Ave\\.|Laboratory|[D|d]isclaimer|Division|Professor|Laboratories|Institutes?|Services|Engineering|Director|Sciences?|Address|Manager|Fax|Office|Mobile|Phone|Street|St\\.|Avenue", arrayOfLines[i])){
				instance.addBinary( new Feature("specWords"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][\\w|\\+|\\.|\\_|\\-]+\\@[\\w|\\-|\\_|\\.]+\\.[a-zA-z]{2,5}[^(\\<|\\>)]", arrayOfLines[i])){
				instance.addBinary( new Feature("emailA"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("[^(\\<|\\>)][(\\w|\\+|\\_|\\-)]+\\@[(\\w|\\-|\\_)]+[\\.][a-zA-z]{2,5}", arrayOfLines[i])){
				instance.addBinary( new Feature("emailB"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("[\\s|\\t](http\\:\\/\\/)*(www|web|w3)*(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)*[\\w]+", arrayOfLines[i])){
				instance.addBinary( new Feature("url"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("(\\-?\\d)*\\d\\d\\s?\\-?\\s?\\d\\d\\d\\d", arrayOfLines[i])){
				instance.addBinary( new Feature("phone"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("[A-Z][a-z]+\\s\\s?[A-Z][\\.]?\\s\\s?[A-Z][a-z]+", arrayOfLines[i])){
				instance.addBinary( new Feature("completeName"+ind));
			}
	
			if(LineProcessingUtil.lineMatcher("\"$", arrayOfLines[i])){
				instance.addBinary( new Feature("endQuote"+ind));
			}
			
			if(LineProcessingUtil.lineMatcher("^[\\w|\\-]+\\:", arrayOfLines[i])){
				instance.addBinary( new Feature("header"+ind));
			}
			//vitor
			if (LineProcessingUtil.lineMatcher("^[\\s|\\t]*$",arrayOfLines[i])){
    	 	    instance.addBinary( new Feature("BlankL"+ind));
    	    }
    	    else{
    	    	instance.addBinary( new Feature("elseBlankL"+ind));
    	    }	

		    int ddd = LineProcessingUtil.indentNumber(arrayOfLines[i]);
		    if(ddd==1){instance.addBinary( new Feature("indentUni"+ind));}
		    else if(ddd==2){instance.addBinary( new Feature("indentBi"+ind));}
		    else if(ddd>=3){instance.addBinary( new Feature("indentTri"+ind));}
		    
		    temp = LineProcessingUtil.punctuationPercentage(arrayOfLines[i]);
		    if (temp>0.20) instance.addBinary( new Feature("punctPerc20"+ind));
		    else instance.addBinary( new Feature("punctPerc0"+ind));
			if (temp>0.50) instance.addBinary( new Feature("punctPerc50"+ind));
			if (temp>0.75) instance.addBinary( new Feature("punctPerc75"+ind));
			if (temp>0.90) instance.addBinary( new Feature("punctPerc90"+ind));
		
			//2 lines starting with same punctuation symbol
			if((i>0)&&(LineProcessingUtil.startWithSameInitialPunctCharacters(arrayOfLines[i], arrayOfLines[i-1]))){
				instance.addBinary( new Feature("prevsicline"+ind));
			}
			
			//extract proper name from FROM field and match it 
			if (fromLine > 0){
				if(SigFilePredictor.detectFromName(arrayOfLines[fromLine], arrayOfLines[i])){
					instance.addBinary( new Feature("fromL"+ind));
				}
			}
		}
		return instance;
	}
		
	/**
	 * Splits the message in lines and calculates the last line and the
	 * first line to be searched. Also detects the "From:" line and excludes header.
	 *
	 * @param incoming message as a String
	 * @return String[] with message lines.
	 */
	private String[] preProcessMailMessage(String wholeMessage)
	{		
		//spliting in lines - see in the readFile method that we insert \n 
		//note that, if last lines of file are blank lines(not even spaces), they are
		//not split in new elements of the array arrayOfLines
		//however, if a line has \\s spaces, then all lines before it are split into the array!
		String[] arrayOfLines = wholeMessage.split("\n");
	 	int arraylength = arrayOfLines.length;	 	
	 	
	 	//calculating the lastSearchLine
	 	//Let's disregard the blank lines at the end of file
	 	int numberOfBlankLines = 0;
	 	int temp1 = arraylength -1;
	 	while(LineProcessingUtil.lineMatcher("^[\\s]*$", arrayOfLines[temp1])){
	 	   numberOfBlankLines++;
	 	   temp1--;
	 	}	 	
	 	
	 	lastSearchLine = arraylength - 1 - numberOfBlankLines;
	
	 	//calculating the firstSearchLine
	 	//TBD: need an intelligent way to exclude header of email- a </body> tag would be the proper solution
	 	//for the moment, find the "From:" line and use the next line as the end of header
	 	//Even better than the From: solution: any line that starts with "[\\w]+:"
	 	int tempfromLine = 0;
	 	int endOfHeaderLine = 0;
	 	int i = 0;
	 	while(i<=lastSearchLine){
	 	
	 	   if(LineProcessingUtil.lineMatcher("^\\s?\\s?From\\:", arrayOfLines[i])){tempfromLine = i; endOfHeaderLine = tempfromLine + 1;}
	 	   if((tempfromLine>0)&&(LineProcessingUtil.lineMatcher("^\\s?\\s?[a-zA-Z][a-z|A-Z|\\-|\\_]+\\:", arrayOfLines[i]))){
	 	       endOfHeaderLine = i + 1;
	 	   }
	 	   else if(tempfromLine>0) {break;}
	 	   i++;  	     
	 	}
	 	
	 	fromLine = tempfromLine;
	 	firstSearchLine = lastSearchLine - tail_lines + 1;
	 	if (firstSearchLine < endOfHeaderLine) {firstSearchLine = endOfHeaderLine;}
	 	
	 	if ((lastSearchLine<= firstSearchLine)||(lastSearchLine - firstSearchLine > (tail_lines +1))){
	 		lastSearchLine = arraylength - 1;
	 		firstSearchLine = lastSearchLine - tail_lines +1;
	 		if (firstSearchLine<0) firstSearchLine =0;
	 	}
	 	return arrayOfLines;		
	}	     	 	  
} 

  private void createSigModel(TextLabels labels) throws IOException{
  	  	
  	  	//SigDetectorByLine byLine = new SigDetectorByLine();
  	    SigDetectorByLine byLine = new SigDetectorByLine();
  		edu.cmu.minorthird.text.TextBase textBase = labels.getTextBase();
  		Dataset dataset = new BasicDataset();
 
		for (edu.cmu.minorthird.text.Span.Looper it = textBase.documentSpanIterator(); it.hasNext();)
		{
			ClassLabel myLabel = new ClassLabel();
			edu.cmu.minorthird.text.Span span = it.nextSpan();
			String spanString = span.asString();	
			
			//parse the message into a set of features		
			MutableInstance  myInst = byLine.getInstance(spanString);
			
			//get the labels
			if (labels.hasType(span, "sig")){
				myLabel = ClassLabel.binaryLabel(+1);
			}
			else{
				myLabel = ClassLabel.binaryLabel(-1);
			}
			
			//build the dataset
			Example ex = new Example((Instance)myInst, myLabel);
			dataset.add(ex);
		}
		
		System.out.println("dataset size = " +dataset.size());
    
   		//just to compare with paper performance
    	//ClassifierLearner learner2 = new AdaBoost();new BatchVersion(new VotedPerceptron(), 5);
    	//Splitter splitter = Expt.toSplitter("k5");
    	//Evaluation eval = Tester.evaluate(learner2, dataset, splitter);
    	//ViewerFrame frame = new ViewerFrame("numeric demo", eval.toGUI());
    	
    	//train and save the model
        String modelName = "mysigDetectionModel";
   		System.out.println("training the Model...");
    	ClassifierLearner learner = new BatchVersion(new VotedPerceptron(), 15);
    	//ClassifierLearner learner = new AdaBoost();
    	Classifier cl = new DatasetClassifierTeacher(dataset).train(learner);
		System.out.println("saving model in file..."+modelName);
		IOUtil.saveSerialized((Serializable)cl, new File(modelName));
		return;  	
  }
    
  
//--------------------- Main method / Test routine -----------------------------------------------------


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

       if(create){ //create model mode
       	  TextLabels labels = FancyLoader.loadTextLabels(args[1]);//parse the .bsh file
       	  SigFileDetector det = new SigFileDetector();
       	  det.createSigModel(labels);
       }       
       else{ //detect mode
          SigFileDetector det = new SigFileDetector();
          for(int i=0; i< args.length; i++){
             String wholeMessage = LineProcessingUtil.readFile(args[i]);
     	     boolean isSig = det.hasSig(wholeMessage);
     	     if(isSig){
     	        System.out.println(args[i]+" has Signature");	
     	     }
     	     else{
     	 	    System.out.println(args[i]+" has NOT Signature");
     	     }
          }       	
       }              
    	
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 
  
  private static void usage(){
  	 System.out.println("Usage: SigFileDetector filename1 filename2 ...");
  	 System.out.println(" OR...");
  	 System.out.println("SigFileDetector -create yourfile.bsh");
  	 /*
  	  *in .env file, annotations follow:
  	  *
  	  *
  	  *addToType filename1 0 -1 sig
  	  *addToType filename2 0 -1 sig
  	  *addToType filename3 0 -1 notsig
  	  *
  	  */  	
  }
} 

 
