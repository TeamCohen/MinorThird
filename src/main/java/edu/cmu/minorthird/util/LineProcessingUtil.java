package edu.cmu.minorthird.util;

import java.io.*;
import java.util.regex.*;
import edu.cmu.minorthird.text.*;

/**
 * Line processing utilities.
 * Matcher for regular expressions, 
 * adding features to stringBuffer in svmformat, etc
 *
 * @author Vitor R. Carvalho (vitor [at] cs..cmu...)
 */
public class LineProcessingUtil
{
	/** Returns true if substring in input (or part of it) matches the pattern.
	 * @param patternStr regexp (in String format)
	 * @param tmpstr line to be matched to regexp (in String format)
	 * @return true (if pattern is matched) or false (otherwise)
	 * 
	 * */
	public static boolean lineMatcher(String patternStr, String tmpstr)
	{
        int strsize = tmpstr.length();
	    CharSequence tmp = tmpstr.subSequence(0, strsize);
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(tmp);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    /**
     * If the line substring matches the regexp, 
     * it adds a " featurename=1" to the string buffer
     *
     * It is useful for producing external datasets in Minorthird format
     *
     * @param line in String format
     * @param regexp in String format
     * @param featureName feature name to be added, in case the regexp matches the line substring
     * @param features_out StringBuffer to which the feature should be added
     * 
     **/
	public static void addFeature(String line, String regexp, String featureName, StringBuffer features_out)
	{
	   if (lineMatcher(regexp, line)){
	 	     features_out.append(" "+featureName+"=1");	 	     
	 	  }
    }
    
     /**
      * Returns the percentage of punctuation (\p{punct}) characters in a line
      * 
      * @param line in String format
      * @return a double with the percentage of characters
      **/
	public static double punctuationPercentage(String line)
	{
	   int linelength = line.length();
	   if(linelength==0) return 0;
	   int punctCount = 0;
	   for (int i=0; i<linelength; i++){
	   	  if(lineMatcher("\\p{Punct}",line.substring(i,i+1))){
	   	  	punctCount++;
	   	  }	   	
	   }
	   double perc = punctCount/(double)linelength;
	   return perc;
    }
    
    
    /**
     * Returns the percentage of A-Z or a-z characters in a line
     *
     * @param line in String format
     * @return the percentage of [a-z] or [A-Z] characters in the line
     **/
	public static double AtoZPercentage(String line)
	{
	   int linelength = line.length();
	   if(linelength==0) return 0;
	   int punctCount = 0;
	   for (int i=0; i<linelength; i++){
	   	  if(lineMatcher("a-zA-Z",line.substring(i,i+1))){
	   	  	punctCount++;
	   	  }	   	
	   }
	   double perc = punctCount/(double)linelength;
	   return perc;
    }
    
     /**
     * Returns the percentage characters [\w] in a line
     * 
     * @param line in String format
     * @return the percentage of "\w" characters in the line
     **/
	public static double wordCharactersPercentage(String line)
	{
	   int linelength = line.length();
	   if(linelength==0) return 0;
	   int punctCount = 0;
	   for (int i=0; i<linelength; i++){
	   	  if(lineMatcher("\\w",line.substring(i,i+1))){
	   	  	punctCount++;
	   	  }	   	
	   }
	   double perc = punctCount/(double)linelength;
	   return perc;
    }
    
   /**
     * returns the percentage of tabs in a line
     *
     * @param line in String format
     * @return the percentage of "\t" characters in the line
     **/
	public static double indentPercentage(String line)
	{
	   int linelength = line.length();
	   if(linelength==0) return 0;
	   int punctCount = 0;
	   for (int i=0; i<linelength; i++){
	   	  if(lineMatcher("\\t",line.substring(i,i+1))){
	   	  	punctCount++;
	   	  }	   	
	   }
	   double perc = punctCount/(double)linelength;
	   return perc;
    }
    
     /* 
      * Returns the number of indentations or tabs ("\t") in a line
     * 
     * @param line in String format
     * @return the number of "\t" characters in the line
     * 
     **/
	public static int indentNumber(String line)
	{
	   int linelength = line.length();
	   if(linelength==0) return 0;
	   int punctCount = 0;
	   for (int i=0; i<linelength; i++){
	   	  if(lineMatcher("\\t",line.substring(i,i+1))){
	   	  	punctCount++;
	   	  }	   	
	   }
	   return punctCount;
    }
    
	 /* 
	 * Returns the number of times a certain expression happened in a line
	 * 
	 * @param - the expression to be counted (for instance: "Would you")
	 * @param line in String format
	 * @return the number of times the expression happened in the line
	 * 
	 **/
	public static int numberOfMatches(String expression, String line) {
  		int linelength = line.length();
  		int exprelength = expression.length();
  		if ((linelength == 0)||(exprelength==0))
    		return 0;
  		int theCount = 0;
  		for (int i = 0; i < (linelength - exprelength); i++) {
    		if (lineMatcher(expression, line.substring(i, i + exprelength))) {
      		theCount++;
      		i +=exprelength;
    		}
  		}
  		//System.out.println("count = "+theCount);
  		return theCount;
	}
    
    
    /** 
     * detect a sequence of 2 lines starting with the same
     * punctuation (\p{Punct}) character
     *
     * @param tmp line1 in String format
     * @param tmp1 line2 in String format
     * @return true, if both lines start with same punctuation symbol    
     *
     */
    public static boolean startWithSameInitialPunctCharacters(String tmp, String tmp1){
    	if((tmp.length()>0)&&(tmp1.length()>0)){
        	String ind = tmp.substring(0,1);//get first character
    	    if (LineProcessingUtil.lineMatcher("\\p{Punct}",ind)){
    		   String ind2 = tmp1.substring(0,1);
    		   if(ind2.compareTo(ind)==0) {
    		   	  return true;
    		   }
    	    }       	
        }
        return false;    	
    }
    
      /**
      * Method to split a message (string format) into lines
      * @param tmp message as String
      * @return message lines in a String[]
      */ 
      public static String[] getMessageLines(String tmp){
    	String[] outL = tmp.split("\n");
    	return outL;
      }
  
    
     /**
      * Method to read a file and turn it into a string - based on rcwang's code
      *  
      * @param in String with the name of file
      * @return the original fine in a String format
      *
      */
	 public static String readFile(String in) throws IOException
	 {
          String line = null;
          StringBuffer content = new StringBuffer("");
          BufferedReader bReader = new BufferedReader(new FileReader(in));
          while ((line = bReader.readLine()) != null)
          {
              content.append(line + "\n");
          } 
          bReader.close();
          return content.toString(); //return the contents of the file in a string format
     }
          
    /** Writes the contents of a String Buffer to an output file  
     *
     * @param outputFileName output File name (as a String)
     * @param aux string buffer to be written to output file
     */
	public static void writeToOutputFile(String outputFileName, StringBuffer aux) throws IOException
	{
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(outputFileName));
		bWriter.write(aux.toString());
		bWriter.close();
	}
	
	
	//don't use this
	public static TextLabels readBsh(File dir, File envfile) throws Exception {
		System.out.println("reading data files");
		TextBaseLoader tbl = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, true);
		tbl.load(dir);
		TextLabels lala  = tbl.getLabels();
		TextBase basevitor = lala.getTextBase();
		
		TextLabelsLoader labelLoaderVitor = new TextLabelsLoader();
		System.out.println("reading env file...");
		labelLoaderVitor.importOps((MutableTextLabels)lala, basevitor, envfile);
		return lala;

	}

}

