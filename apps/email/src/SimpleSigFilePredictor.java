package email;

import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Old version of SigFilePredictor: completely deterministic.  
 * Reasonable performance, even not using learning at all.  
 *
 * Reads in a file in String format, classify it as having or not a signature 
 * file and predicts where the Sig file starts
 * Use the tail_lines variable below to change Precision/Recall relation. 
 * See comments below. 
 *
 * @author vitor/at/cs/./cmu
 * Dec 2003
 *
 */
public class SimpleSigFilePredictor
{

  /** sets the number of lines to detect messages with sig
   *
   * in tests, tail -5 is observed a Prec/Recall about (95/95)%
   * using tail -10, we observed (89.7/98.6)%
   * using tail -3, we observed (97/90)%
   */
  private static int tail_lines = 5;
  /** to predict, use the last 15 lines */
  private static int prediction_tail_lines = 15;
  /** not all are used currently for detection and prediction */
  private static int numberOfFeatures = 10;
  /** //line where the feature was detected in file */
  private static int arrayOfFeatures[] = new int[numberOfFeatures];
//  static final boolean debugmode = false; //set to true in case you want to debug this program
  private static int totalNumberOfLines;
  private static String[] arrayOfLines;//input file, line by line in an array

  //window for search of features - usually last tail_lines lines
  private static int lastSearchLine = 100;
  private static int firstSearchLine = 0;
  private static int fromLine;//line where "From:" pattern is seen
  private static int startSigLine;// the prediction line
  private static int endOfHeaderLine = 0;//line where we believe is the end of email headers
  private static Logger log = Logger.getLogger(SimpleSigFilePredictor.class);

  /**
    Receive string or file from interface.  Preprocess message: get
    last lines of message and exclude header. Return a string.  For
    all features: try to detect feature regexp on the this string.  If
    so, set featureArray field to true.  After all features, output
    heuristic classification (using OR function). Given true, predict
    the line where the sig field starts.
  */
  public static int Predict(String wholeMessage)
  {
    int totalNumberOfCharactersOnMessage = wholeMessage.length();
    //spliting in lines - see in the readFile method that we insert \n
    //note that, if last lines of file are blank lines(not even spaces), they are
    //not split in new elements of the array arrayOfLines
    //however, if a line has \\s spaces, then all lines before it are split into the array!
    //arrayOfLines = wholeMessage.split("\n");
    arrayOfLines = wholeMessage.split("\n");
    resetArrayOfFeatures();
    preProcessMailMessage(arrayOfLines);
    boolean hasSig = processMailFile(arrayOfLines);

    if (hasSig)
    {
      resetArrayOfFeatures();
      //given that the message has sig, try to make a better prediction
      int begin = DetectBegginingOfSigFile(startSigLine, wholeMessage);
      log.debug("Prediction = " + begin);

      return begin;
    }
    else //TBD wait for Kevin's fix
    {
        log.debug("############################# SIG NOT FOUND");

//      return +1;//TBD: until Kevin's fix
      return -1;  //no sig detected, end of the game.
    }

  }


  //preprocess message: get last lines of message and exclude header. return a string
  private static void preProcessMailMessage(String[] arrayOfLines)
  {
    int totalNumberOfLines = arrayOfLines.length;

    //calculating the lastSearchLine
    //Let's disregard the blank lines at the end of file
    int numberOfBlankLines = 0;
    int temp1 = totalNumberOfLines - 1;
    while (myMatcher("^[\\s]*$", arrayOfLines[temp1]))
    {
      numberOfBlankLines++;
      temp1--;
    }

    log.debug("numberofBlanckLines =  " + numberOfBlankLines);
    lastSearchLine = totalNumberOfLines - 1 - numberOfBlankLines;
    log.debug("lastSearchLine =  " + lastSearchLine);

    //calculating the firstSearchLine
    //TBD: need an intelligent way to exclude header of email- a </body> tag would be the proper solution
    //for the moment, find the "From:" line and use the next line as the end of header
    //Even better than the From: solution: any line that starts with "[\\w]+:"
    fromLine = 0;
    int i = 0;
    // if(myMatcher("^From\\:", arrayOfLines[i])){
    while (i <= lastSearchLine)
    {
      if (myMatcher("^From\\:", arrayOfLines[i]))
      {
        fromLine = i;
        endOfHeaderLine = fromLine + 1;
      }
      if ((fromLine > 0) && (myMatcher("^[a-zA-Z][a-z|A-Z|\\-|\\_]+\\:", arrayOfLines[i])))
      {
        endOfHeaderLine = i + 1;
      }
      else if (fromLine > 0)
      {
        break;
      }
      i++;
    }


    log.debug("endOfHeaderLine =  " + endOfHeaderLine);
    firstSearchLine = lastSearchLine - tail_lines + 1;
    if (firstSearchLine < endOfHeaderLine)
    {
      firstSearchLine = endOfHeaderLine;
    }
    log.debug("FirstsearchLine =  " + firstSearchLine);

  }

  private static int DetectBegginingOfSigFile(int startSigLine, String wholeMessage)
  {
    String tempString = arrayOfLines[startSigLine];

    //the new logic will come here.
    int extendedStartSigLine = preProcessForPrediction();

    if (extendedStartSigLine < startSigLine)
    {
      tempString = arrayOfLines[extendedStartSigLine];
    }

    int charindex = wholeMessage.lastIndexOf(tempString);

    return charindex;
  }

  //simply update the firstSearchLine
  private static int preProcessForPrediction()
  {
    //just update firstSearchLine
    firstSearchLine = lastSearchLine - prediction_tail_lines + 1;
    if (firstSearchLine < endOfHeaderLine)
    {
      firstSearchLine = endOfHeaderLine;
    }
    //System.out.println("FirstsearchLine =  " + firstSearchLine);

    int temp = detectFeature0(arrayOfLines);
    if (temp > 0) return temp;//Detect the "^\s\s?--[-]*?$" pattern

    temp = detectFeature1(arrayOfLines);//detect weird symbols *#+^-~_/%  TBD: couldn't use regexp for "\"
    if (temp > 0) return temp;

    //otherwise, try to use sender's name on prediction
    temp = detectFeature7(arrayOfLines);
    if (temp > 0) return temp;

    //otherwise
    return startSigLine;

  }


  // Returns true if substring in input matches the pattern.
  public static boolean myMatcher(String patternStr, String tmpstr)
  {
    int strsize = tmpstr.length();
    CharSequence tmp = tmpstr.subSequence(0, strsize);
    Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(tmp);
    if (matcher.find())
    {
      return true;
    }
    return false;
  }


  public static boolean processMailFile(String[] arrayOfLines)
  {

    arrayOfFeatures[0] = detectFeature0(arrayOfLines);//Detect the "^\s\s--[-]*?$" pattern
    arrayOfFeatures[1] = detectFeature1(arrayOfLines);//detect weird symbols *#+^-~_/%  TBD: couldn't use regexp for "\"
    arrayOfFeatures[2] = detectFeature2(arrayOfLines);//Detect usual words in sig files,like College, Division, Avenue, Street
    arrayOfFeatures[3] = detectFeature3(arrayOfLines);//Detect email pattern
    arrayOfFeatures[4] = detectFeature4(arrayOfLines);//Detect URL pattern
    arrayOfFeatures[5] = detectFeature5(arrayOfLines);//Detect telephone number pattern
    arrayOfFeatures[6] = detectFeature6(arrayOfLines);//Names patterns like the last Vitor R. Carvalho or James H. Miller
    //this feature has been effective in predicting, but increases recall when detectin
//	      arrayOfFeatures[7] = detectFeature7(arrayOfLines);//Using the From: information

    //In a handout set using OR rule
    //the 2 features below were increasing the Recall, and not increasing Precision
    //Please uncomment if usage will use a learning algorith.
    //    arrayOfFeatures[8] = detectFeature8(arrayOfLines);//Names patterns like Vitor Rocha Carvalho or James Henry Miller
    //    arrayOfFeatures[9] = detectFeature9(arrayOfLines);//detect quoted text,i.e., "God doesn't play dice" - Albert Einstein


    //simple learning rule: OR
    //based on empirical observations, we'll ignore the last 3 features when using the OR rule
    int IGNORE = 3;
    boolean resultUsingOR = false;
    startSigLine = lastSearchLine;
    for (int i = 0; i < numberOfFeatures - IGNORE; i++)
    {
      //System.out.println("arrayOfFeatures =  " + arrayOfFeatures[i]);
      if (arrayOfFeatures[i] > 0)
      {
        resultUsingOR = true;
        if (arrayOfFeatures[i] < startSigLine) startSigLine = arrayOfFeatures[i];

      }
    }
    // System.out.println("\n");
    return (resultUsingOR);
  }


//Detect the "^\s\s--[-]*?$" pattern - very effective feature to detect the beggining of sig files
  public static int detectFeature0(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("^[\\s]*---*[\\s]*$", tmp[i]))
      {
        log.debug(tmp[i]);

        return i;
      }
    }
    return result;
  }


  //people love to draw,  be different in sig files.
  //Detect the "^[\\s]*([\\*]|#|[\\+]|[\\^]|-|~|_|\\|\/|%|:|[\\=]){$symbolrepetitionlength,}$" pattern
  public static int detectFeature1(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("^[\\s]*([\\*]|#|[\\+]|[\\^]|-|[\\~]|[\\&]|[////]|[\\$]|_|[\\!]|[\\/]|[\\%]|[\\:]|[\\=]){10,}[\\s]*$", tmp[i]))
      {
        log.debug(tmp[i]);
        return i;
      }
    }
    return result;
  }


  //Detect "Usual words in sig files,  like College, Division, Avenue, Street, etc" pattern
//In fact, each one of these words can be a different feature - TBD
  public static int detectFeature2(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("Dept\\.|University|Corp\\.|Corporations?|College|Ave\\.|Laboratory|Division|Professor|Laboratories|Institutes?|Services|Engineering|Director|Sciences?|Address|Manager|Street|St\\.|Avenue", tmp[i]))
      {
        log.debug(tmp[i]);

        result = i;
      }
    }
    return result; //last line found
  }


  //Detect email pattern
  public static int detectFeature3(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      //"([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})") another option!
      if (myMatcher("[^(\\<|\\>)][(\\w|\\+|\\.|\\_|\\-]+\\@[\\w|\\-|\\_|\\.)]+\\.[a-zA-z]{2,5}[^(\\<|\\>)]", tmp[i]))
      {
        log.debug(tmp[i]);

        result = i;
      }
    }
    return result;
  }


  //Detect URL pattern
  public static int detectFeature4(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("[\\s](http\\:\\/\\/)*(www|web|w3)*(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)\\.(\\w[\\w|\\-]+)*[\\w]+", tmp[i]))
      {
        log.debug(tmp[i]);
        result = i;
      }
    }
    return result;
  }


  //Detect telephone number pattern
  public static int detectFeature5(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("(\\-?\\d)*\\d\\d\\s?\\-?\\s?\\d\\d\\d\\d", tmp[i]))
      {
        log.debug(tmp[i]);
        result = i;
      }
    }
    return result;
  }


  //Names patterns like the last Vitor R. Carvalho or James H. Miller
  public static int detectFeature6(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("[A-Z][a-z]+\\s\\s?[A-Z][\\.]?\\s\\s?[A-Z][a-z]+", tmp[i]))
      {
        result = i;
        log.debug(tmp[i]);
      }
    }
    return result;
  }


  //From: line processing
  public static int detectFeature7(String[] tmp)
  {
    int result = 0;
    if (fromLine == 0) return result; //nothing can be done

    //String inputStr = "abbabcd";
    String inputStr = arrayOfLines[fromLine];
    //String patternStr = "(a(?:b*))+(c*)";
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
        //System.out.println("groupSTR = " + groupStr[j]);

        for (int i = lastSearchLine; i >= firstSearchLine; i--)
        {
          if (myMatcher(groupStr[j], tmp[i]))
          {
            log.debug(tmp[i]);

            return i;
          }
        }
      }
    }
    else
    {
      //another string pattern
      patternStr = "([A-Z][a-z]+\\s\\s?([A-Z][a-z]+))";

      // Compile and use regular expression
      Pattern myPattern = Pattern.compile(patternStr);
      Matcher myMatcher = myPattern.matcher(inputStr);
      matchFound = myMatcher.find();

      if (matchFound)
      {
        int groupsize = matcher.groupCount() + 1;
        String[] groupStr = new String[groupsize];

        for (int j = 0; j <= myMatcher.groupCount(); j++)
        {
          groupStr[j] = myMatcher.group(j);
          //System.out.println("groupSTR = " + groupStr[j]);

          for (int i = lastSearchLine; i >= firstSearchLine; i--)
          {
            if (myMatcher(groupStr[j], tmp[i]))
            {
              log.debug(tmp[i]);
              return i;
            }
          }
        }
      }
    }

    return result;

  }


  //Names patterns like Vitor Rocha Carvalho or James Henry Miller
  //not being used for the moment: tests showed it increases recall
  public static int detectFeature8(String[] tmp)
  {
    int result = 0;
    for (int i = firstSearchLine; i <= lastSearchLine; i++)
    {

      if (myMatcher("[A-Z][a-z]+\\s\\s?[A-Z][a-z]+\\s\\s?[A-Z][a-z]+", tmp[i]))
      {
        log.info(tmp[i]);
        result = i;
      }
    }
    return result;
  }


  //Tries to detect quoted text in this format "   ".
  //doesn't detect if one single quote is contained in one single line
  //TBD: needs more testing
  public static int detectFeature9(String[] tmp)
  {
    int result = 0;
    int numberOfQuotes = 0;
    for (int i = lastSearchLine; i <= firstSearchLine; i--)
    { //inverted search

      if (myMatcher("\"", tmp[i]))
      {
        log.debug(tmp[i]);
        numberOfQuotes++;
        //result = i;

        //bugfix , in case of simple words like "mojo"
        if (myMatcher("[\"][\\w|\\s]{1,15}[\"]", tmp[i]))
          numberOfQuotes--;
        else
          result = i;
      }
    }

    if (numberOfQuotes > 1)
      return result;
    else
      return 0;
  }

  //reset array of features
  private static void resetArrayOfFeatures()
  {
    for (int i = 0; i < numberOfFeatures; i++)
    {
      arrayOfFeatures[i] = 0;
    }
  }


} //end of sigfinder class


//the matcher method in the String class will return true only if
//all the string is matched with the regexp. Thus, if you're looking for matching the word
//capita, use the regexp ".*capita.*"
//  String xxx = "^\s\s--[-]?$";
//  if (tmp.matches(xxx))
//  {
//     System.out.println("Found a sig file " + i + " matches");
//  }


