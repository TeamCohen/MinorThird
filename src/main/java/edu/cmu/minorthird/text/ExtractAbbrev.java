package edu.cmu.minorthird.text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * The ExtractAbbrev class implements a simple algorithm for
 * extraction of abbreviations and their definitions from biomedical
 * text.  Abbreviations (short forms) are extracted from the input
 * file, and those abbreviations for which a definition (long form) is
 * found are printed out, along with that definition, one per line.
 *
 * See:
 * A Simple Algorithm for Identifying Abbreviation Definitions in Biomedical Text
 * A.S. Schwartz, M.A. Hearst; Pacific Symposium on Biocomputing 8:451-462(2003) 
 * for a detailed description of the algorithm. (http://biotext.berkeley.edu/papers/psb03.pdf)
 *
 * @author Ariel Schwartz and William Cohen
 * @version 03/12/03 and 11/04
 *
 */

public class ExtractAbbrev extends AbstractAnnotator
{
  private Logger log = Logger.getLogger(ExtractAbbrev.class);

  /** The annotation type provided by this annotator. */
  public static final String PROVIDED_ANNOTATION = "abbrev";
  /** The type asserted for extracted acronyms */
  public static final String SHORT_FORM_TYPE = "abbrevShort";
  /** The type asserted for expansions of extracted acronyms */
  public static final String LONG_FORM_TYPE = "abbrevLong";
  /** This property links an extracted acronym to its expansion */
  public static final String LONG_FORM_PROP = "expansion";
  /** This property links an expansion to its acronym */
  public static final String SHORT_FORM_PROP = "acronym";

  private Map<String,Vector<String>> mTestDefinitions = new HashMap<String,Vector<String>>();
  private int truePositives = 0, falsePositives = 0, falseNegatives = 0, trueNegatives = 0;
  private static final char DELIMITER = '\t';
  private boolean testMode = false;
  
  //
  // added by wcohen - implements the AbstractAnnotator interface
  //
  private List<StringSpan> accum = new ArrayList<StringSpan>();   // accumulator, used by Schwartz's code
  private boolean annotationMode = false; // flag, used by Schwartz's code
	@Override
	protected void doAnnotate(MonotonicTextLabels labels)
  {
    annotationMode = true;
    int k=0;
    for (Iterator<Span> i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
      accum.clear(); 
      Span doc = i.next();
      String s = doc.getDocumentContents();
      // call Schwartz's code to fill up accum with short,long pairs
      extractAbbrPairsFromString(s);
      // build annotations based on the contents of the accumulator
      for (Iterator<StringSpan> j=accum.iterator(); j.hasNext(); ) {
        StringSpan shortForm = j.next();
        StringSpan longForm = j.next();
        Span shortSpan = doc.charIndexSubSpan(shortForm.lo, shortForm.hi);
        log.debug("shortSpan["+shortForm.lo+".."+shortForm.hi+"] of doc: near '"+
                  doc.getDocumentContents().substring(shortForm.lo,shortForm.hi)+"'");
        log.debug("shortForm='"+shortForm.asString()+"' shortSpan='"+shortSpan.asString()+"'");


        Span longSpan = doc.charIndexSubSpan(longForm.lo, longForm.hi);
        labels.addToType(shortSpan, SHORT_FORM_TYPE);
        labels.addToType(longSpan, LONG_FORM_TYPE);
        k++;
        labels.setProperty( shortSpan, SHORT_FORM_PROP, Integer.toString(k));
        labels.setProperty( longSpan, LONG_FORM_PROP, Integer.toString(k));
      }
    }
    annotationMode = false;
    labels.setAnnotatedBy( PROVIDED_ANNOTATION );
  }
  @Override
	public String explainAnnotation(TextLabels labels,Span span)
  {
    return "No explanation implemented.";
  }

  private boolean isValidShortForm(String str) {
    return (hasLetter(str) && (Character.isLetterOrDigit(str.charAt(0)) || (str.charAt(0) == '(')));
  }

  private boolean hasLetter(String str) {
    for (int i=0; i < str.length() ; i++)
	    if (Character.isLetter(str.charAt(i)))
        return true;
    return false;
  }

  private boolean hasCapital(String str) {
    for (int i=0; i < str.length() ; i++)
	    if (Character.isUpperCase(str.charAt(i)))
        return true;
    return false;
  }

  private void loadTrueDefinitions(String inFile) {
    String abbrString, defnString, str = "";
    Vector<String> entry;
    Map<String,Vector<String>> definitions = mTestDefinitions;

    try {
	    BufferedReader fin = new BufferedReader(new FileReader (inFile));
	    while ((str = fin.readLine()) != null) {
        int j = str.indexOf(DELIMITER);
        abbrString = str.substring(0,j).trim();
        defnString = str.substring(j,str.length()).trim();		
        entry = definitions.get(abbrString);
        if (entry == null)
          entry = new Vector<String>();
        entry.add(defnString);
		    definitions.put(abbrString, entry);
	    }
    } catch (Exception e) {
	    e.printStackTrace();
	    System.out.println(str);
    }
  }
    
  private boolean isTrueDefinition(String shortForm, String longForm) {
    Vector<String> entry;
    Iterator<String> itr;

    entry = mTestDefinitions.get(shortForm);
    if (entry == null)
	    return false;
    itr = entry.iterator();
    while(itr.hasNext()){
	    if (itr.next().toString().equalsIgnoreCase(longForm))
        return true;
    }
    return false;
  }

  private void extractAbbrPairsFromFile(String inFile) 
  {
    try {
	    BufferedReader fin = new BufferedReader(new FileReader (inFile));
      String content = "";
      String line;
	    while ((line = fin.readLine()) != null) {
        content += line+" ";
      }
      extractAbbrPairsFromString(content);
    } catch (Exception ioe) {
	    ioe.printStackTrace();
    }
  }

  private void extractAbbrPairsFromString(String currString) 
  {
    String[] sentence = currString.split("\\.\\s{2}");
    int offset = 0;
    for (int i=0; i<sentence.length; i++) {
      extractAbbrPairsFromSentence(new StringSpan(currString,offset,offset+sentence[i].length()));
      offset += sentence[i].length()+3; // the +3 is for the period and the two following spaces
    }
  }

  /** Holds a substring of a longer string. */ 
  static private class StringSpan 
  {
    static public final StringSpan EMPTY = new StringSpan("",0,0);
    String base; 
    int lo,hi;
    String mySubstring; 
    public StringSpan(String b,int lo,int hi) 
    { 
      this.base=b; this.lo=lo; this.hi=hi; 
      mySubstring = base.substring(lo,hi);
    }
    public StringSpan(StringSpan ss,int lo,int hi) 
    { 
      this.base=ss.base; this.lo=ss.lo+lo; this.hi=ss.lo+hi; 
      this.mySubstring = this.base.substring(this.lo,this.hi);
    }
    //public int offset() { return lo; }
    public int length() { return hi-lo; }
    public char charAt(int i) { return mySubstring.charAt(i); }
    public int indexOf(char ch) { return mySubstring.indexOf(ch); }
    public int indexOf(char ch,int fromIndex) { return mySubstring.indexOf(ch,fromIndex); }
    public int indexOf(String s) { return mySubstring.indexOf(s); }
    public int lastIndexOf(String s) { return mySubstring.lastIndexOf(s); }
    public int lastIndexOf(String s,int fromIndex) { return mySubstring.lastIndexOf(s,fromIndex); }
    public String asString() { return mySubstring; }
    public StringSpan substring(int newLo,int newHi) { return new StringSpan(base,lo+newLo,lo+newHi); }
    public StringSpan substring(int newLo) { return new StringSpan(base,lo+newLo,hi); }
    public StringSpan trim()
    {
      StringSpan ss = new StringSpan(base,lo,hi);
      while (ss.lo<ss.hi && Character.isWhitespace(ss.base.charAt(ss.lo))) ss.lo++;
      while (ss.hi>ss.lo && Character.isWhitespace(ss.base.charAt(ss.hi-1))) ss.hi--;
      ss.mySubstring = ss.base.substring(ss.lo,ss.hi);
      return ss;
    }
  }

  private void extractAbbrPairsFromSentence(StringSpan currSentence) 
  {
    StringSpan longForm = StringSpan.EMPTY; 
    StringSpan shortForm = StringSpan.EMPTY;
    int openParenIndex, closeParenIndex = -1, sentenceEnd, newCloseParenIndex, tmpIndex = -1;
    StringTokenizer shortTokenizer;

    log.debug("finding pairs in '"+currSentence.asString()+"'");
    openParenIndex =  currSentence.indexOf(" (");
    do {
      if (openParenIndex > -1)
        openParenIndex++;
      sentenceEnd = Math.max(currSentence.lastIndexOf(". "), currSentence.lastIndexOf(", "));
      if ((openParenIndex == -1) && (sentenceEnd == -1)) {
        //Do nothing
      }
      else if (openParenIndex == -1) {
        currSentence = currSentence.substring(sentenceEnd + 2);
      } else if ((closeParenIndex = currSentence.indexOf(')',openParenIndex)) > -1){
        sentenceEnd = Math.max(currSentence.lastIndexOf(". ", openParenIndex), 
                               currSentence.lastIndexOf(", ", openParenIndex));
        if (sentenceEnd == -1)
          sentenceEnd = -2;
        //longForm = currSentence.substring(sentenceEnd + 2, openParenIndex);
        //shortForm = currSentence.substring(openParenIndex + 1, closeParenIndex);
        longForm = new StringSpan(currSentence,sentenceEnd+2,openParenIndex);
        shortForm = new StringSpan(currSentence,openParenIndex+1,closeParenIndex);
      }
      if (shortForm.length() > 0 || longForm.length() > 0) {
        if (shortForm.length() > 1 && longForm.length() > 1) {
          if ((shortForm.indexOf('(') > -1) && 
              ((newCloseParenIndex = currSentence.indexOf(')', closeParenIndex + 1)) > -1)){
            //shortForm = currSentence.substring(openParenIndex + 1, newCloseParenIndex);
            shortForm = new StringSpan(currSentence, openParenIndex + 1, newCloseParenIndex);
            closeParenIndex = newCloseParenIndex;
          }
          if ((tmpIndex = shortForm.indexOf(", ")) > -1)
            shortForm = shortForm.substring(0, tmpIndex);			    
          if ((tmpIndex = shortForm.indexOf("; ")) > -1)
            shortForm = shortForm.substring(0, tmpIndex);
          shortTokenizer = new StringTokenizer(shortForm.asString());
          if (shortTokenizer.countTokens() > 2 || shortForm.length() > longForm.length()) {
            // Long form in ( )
            tmpIndex = currSentence.lastIndexOf(" ", openParenIndex - 2);
            //tmpStr = currSentence.substring(tmpIndex + 1, openParenIndex - 1);
            StringSpan tmpStr = new StringSpan(currSentence,tmpIndex + 1, openParenIndex - 1);
            longForm = shortForm;
            shortForm = tmpStr;
            if (! hasCapital(shortForm.asString()))
              shortForm = StringSpan.EMPTY;
          }
          if (isValidShortForm(shortForm.asString())){
            extractAbbrPair(shortForm.trim(), longForm.trim());
          }
        }
        currSentence = currSentence.substring(closeParenIndex + 1);
      } else if (openParenIndex > -1) {
        if ((currSentence.length() - openParenIndex) > 200)
          // Matching close paren was not found
          currSentence = currSentence.substring(openParenIndex + 1);
        break; // Read next line
      }
      shortForm = StringSpan.EMPTY;
      longForm = StringSpan.EMPTY;
    } while ((openParenIndex =  currSentence.indexOf(" (")) > -1);
  }

  private StringSpan findBestLongForm(StringSpan shortForm, StringSpan longForm) 
  {
    int sIndex;
    int lIndex;
    char currChar;

    sIndex = shortForm.length() - 1;
    lIndex = longForm.length() - 1;
    for ( ; sIndex >= 0; sIndex--) {
	    currChar = Character.toLowerCase(shortForm.charAt(sIndex));
	    if (!Character.isLetterOrDigit(currChar))
        continue;
	    while (((lIndex >= 0) && (Character.toLowerCase(longForm.charAt(lIndex)) != currChar)) ||
             ((sIndex == 0) && (lIndex > 0) && (Character.isLetterOrDigit(longForm.charAt(lIndex - 1)))))
        lIndex--;
	    if (lIndex < 0)
        return null;
	    lIndex--;
    }
    lIndex = longForm.lastIndexOf(" ", lIndex) + 1;
    return longForm.substring(lIndex);
  }

  private void extractAbbrPair(StringSpan shortForm, StringSpan longForm) 
  {
    StringSpan bestLongForm;
    StringTokenizer tokenizer;
    int longFormSize, shortFormSize;

    log.debug("finding long form for '"+shortForm.asString()+"' and '"+longForm.asString()+"'");

    if (shortForm.length() == 1)
	    return;
    bestLongForm = findBestLongForm(shortForm, longForm);
    if (bestLongForm == null)
	    return;
    tokenizer = new StringTokenizer(bestLongForm.asString(), " \t\n\r\f-");
    longFormSize = tokenizer.countTokens();
    shortFormSize = shortForm.length();
    for (int i=shortFormSize - 1; i >= 0; i--)
	    if (!Character.isLetterOrDigit(shortForm.charAt(i)))
        shortFormSize--;
    if (bestLongForm.length() < shortForm.length() || 
        bestLongForm.indexOf(shortForm.asString() + " ") > -1 ||
        bestLongForm.asString().endsWith(shortForm.asString()) ||
        longFormSize > shortFormSize * 2 ||
        longFormSize > shortFormSize + 5 ||
        shortFormSize > 10)
	    return;

    // at this point we have bestLongForm as expansion of shortForm
    if (annotationMode) {
      accum.add( shortForm );
      accum.add( bestLongForm );
    }

    if (testMode) {
	    if (isTrueDefinition(shortForm.asString(), bestLongForm.asString())) {
        System.out.println(shortForm.asString() + DELIMITER + bestLongForm.asString() + DELIMITER + "TP");
        truePositives++;
	    }
	    else {
        falsePositives++;
        System.out.println(shortForm.asString() + DELIMITER + bestLongForm.asString() + DELIMITER + "FP");
	    }	
    } else if (!annotationMode) {
	    System.out.println(shortForm.asString() + DELIMITER + bestLongForm.asString());
    }
  }
    

  private static void usage() {
    System.err.println("Usage: ExtractAbbrev [-options] <filename>");
    System.err.println("       <filename> contains text from which abbreviations are extracted" );
    System.err.println("       -testlist <file> = list of true abbreviation definition pairs");
    System.err.println("       -usage or -help = this message");
    System.exit(1);
  }

  public static void main(String[] args) {
//    String shortForm, longForm, defnString, str;
    ExtractAbbrev extractAbbrev = new ExtractAbbrev();
    String filename =  null;
    String testList = null;
	
    //parse arguments
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-testlist")) {
        if (i == args.length - 1) {
          usage();
        }
        testList = args[++i];
        extractAbbrev.testMode = true;
      } else if (args[i].equals("-usage")) {
        usage();
      } else if (args[i].equals("-help")) {
        usage();
      } else {
        filename = args[i];
        // Must be last arg
        if (i != args.length - 1) {
          usage();
        }
      }
    }
    if (filename == null) {
      usage();
    }

    if (extractAbbrev.testMode)
	    extractAbbrev.loadTrueDefinitions(testList);
    extractAbbrev.extractAbbrPairsFromFile(filename);
    if (extractAbbrev.testMode)
	    System.out.println("TP: " + extractAbbrev.truePositives + " FP: " + extractAbbrev.falsePositives + 
                         " FN: " + extractAbbrev.falseNegatives + " TN: " + extractAbbrev.trueNegatives);
  }
}

