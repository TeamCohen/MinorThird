package edu.cmu.minorthird.text;

import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.text.CharAnnotation;
import montylingua.JMontyLingua;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Adds part of speech tags to a TextLabels. 
 *
 * @author Richard Wang rcwang@cmu.edu
 */

public class POSTagger extends StringAnnotator
{
  private static Logger log = Logger.getLogger(POSTagger.class);
  private static JMontyLingua montyLingua = new JMontyLingua();

	public POSTagger()
	{
		// tell superclass what type of annotation is
		// being provided
		providedAnnotation = "pos";
	}

  /**
   * Returns char based stand-off annotations for pos in the given string
   *
   * This will not work with html/xml in the string!
   *
   * @param in String to tag
   * @return tagged String
   */
  protected CharAnnotation[] annotateString(String in)
  {
    String tagged = montyTag(in);

	  String strToken = null;
    String pos = null;
    String word = null;

//    StringBuffer XMLTagged = new StringBuffer("");

    int sep = 0;
//    int endPointer = 0;

    StringTokenizer tokeTagged = new StringTokenizer(tagged, "\n ", false);
    log.debug("\n" + in);

    //list of annotations
    List list = new ArrayList();

    int curLocation = 0;
    while (tokeTagged.hasMoreTokens())
    {
      strToken = tokeTagged.nextToken();

      sep = strToken.lastIndexOf("/"); //find the annotation location
      word = strToken.substring(0, sep); //get string rep of the actual word

      curLocation = in.indexOf(word, curLocation); //where in the original string is this word

      pos = strToken.substring(sep + 1);  //the POS tag
      if (pos.endsWith("$"))
        pos = pos.replace('$', 'S');

      //put into list
		  CharAnnotation ca = new CharAnnotation(curLocation, word.length(), pos);
      list.add(ca);

      log.debug("tag: " + strToken + " with " + ca);

      //adjust curLocation by word.length
      curLocation += word.length();
    }

    return (CharAnnotation[])list.toArray(new CharAnnotation[0]);
    // if (sent) XMLTagged.append("</S>");
//    return XMLTagged.toString();
  }

  private static void writeFile(File out, String content)
  {
    log.debug("Writing " + out);
    try
    {
      BufferedWriter bWriter = new BufferedWriter(new FileWriter(out));
      bWriter.write(content);
      bWriter.close();
    }
    catch (Exception ioe)
    {
      log.error("Error writing to " + out + ": " + ioe);
    }
  }

  public static String substFirst(String in, String find, String newStr, boolean case_sensitive)
  {
    char[] working = in.toCharArray();
    StringBuffer sb = new StringBuffer();
    // int startindex =  in.indexOf(find);
    int startindex = 0;
    if (case_sensitive)
      startindex = in.indexOf(find);
    else
      startindex = (in.toLowerCase()).indexOf(find.toLowerCase());
    if (startindex < 0) return in;
    int currindex = 0;
    for (int i = currindex; i < startindex; i++)
      sb.append(working[i]);
    currindex = startindex;
    sb.append(newStr);
    currindex += find.length();
    for (int i = currindex; i < working.length; i++)
      sb.append(working[i]);
    return sb.toString();
  }

  /* currently unused
  public static String substAll(String in, String find, String newStr, boolean case_sensitive)
  {
    char[] working = in.toCharArray();
    StringBuffer sb = new StringBuffer();
    int startindex = 0;
    if (case_sensitive)
      startindex = in.indexOf(find);
    else
      startindex = (in.toLowerCase()).indexOf(find.toLowerCase());
    if (startindex < 0) return in;
    int currindex = 0;
    while (startindex > -1)
    {
      for (int i = currindex; i < startindex; i++)
        sb.append(working[i]);
      currindex = startindex;
      sb.append(newStr);
      currindex += find.length();
      // startindex = in.indexOf(find, currindex);
      if (case_sensitive)
        startindex = in.indexOf(find, currindex);
      else
        startindex = (in.toLowerCase()).indexOf(find.toLowerCase(), currindex);
    }
    for (int i = currindex; i < working.length; i++)
      sb.append(working[i]);
    return sb.toString();
  }
  */

  private static String montyTag(String string)
  {
//    in = in.replaceAll("(?i)('d)\\b", " would ");
//    in = in.replaceAll("(?i)('ll)\\b", " will ");
//    in = in.replaceAll("(?i)('ve)\\b", " have ");
//    in = in.replaceAll("(?i)('re)\\b", " are ");
//    in = in.replaceAll("(?i)\\b(can't)\\b", "cannot");
//    in = in.replaceAll("(?i)\\b(won't)\\b", "will not");
//    in = in.replaceAll("(?i)\\b(shan't)\\b", "shall not");
//    in = in.replaceAll("(?i)(n't)\\b", " not");
//    in = in.replaceAll("(?i)\\b(i'm)\\b", " I am ");
//    in = in.replaceAll("(?i)\\b(here's)\\b", " here is ");
//    in = in.replaceAll("(?i)\\b(there's)\\b", " there is ");
//    in = in.replaceAll("(?i)\\b(that's)\\b", " that is ");
//    in = in.replaceAll("(?i)\\b(he's)\\b", " he is ");
//    in = in.replaceAll("(?i)\\b(she's)\\b", " she is ");
//    in = in.replaceAll("(?i)\\b(who's)\\b", " who is ");
//    in = in.replaceAll("(?i)\\b(it's)\\b", " it is ");
    string = string.replaceAll("<[^>]+>", "");	// removes HTML/XML tags

// Perform tagging and re-formatting
// JMontyTagger j = new JMontyTagger();
    return montyLingua.tag_text(string);
  }


  public static String POSTag(String in)
  {
    String tagged = montyTag(in);

    String strToken = null;
    String pos = null;
    String word = null;

    StringBuffer XMLTagged = new StringBuffer("");

    int sep = 0;
    int endPointer = 0;

    StringTokenizer tokeTagged = new StringTokenizer(tagged, "\n ", false);
//    log.debug(in);

    String workingString = new String(in);
    while (tokeTagged.hasMoreTokens())
    {
      strToken = tokeTagged.nextToken();
//      log.debug("token: " + strToken);
      sep = strToken.lastIndexOf("/");
      word = strToken.substring(0, sep);
      pos = strToken.substring(sep + 1);
      if (pos.endsWith("$"))
        pos = pos.replace('$', 'S');
//   if (pos.equals(".")) {
//    XMLTagged.append("</S>");
//    sent = false;
//  }

//      log.debug("Replacing: " + word + " ==> <" + pos + ">" + word + "</" + pos + "> ================================================>");
      // if (debug) System.err.println(">>> Buffer before:\n" + in);
      workingString = substFirst(workingString, word, "<" + pos + ">" + word + "</" + pos + ">", false);
      // if (debug) System.err.println(">>> Buffer after:\n" + in);
      endPointer = workingString.lastIndexOf("</" + pos + ">") + ("</" + pos + ">").length();
      // if (debug) System.err.println(">>> IN_PTR: " + in_ptr);
      // if (debug) System.err.println(in.substring(0, in_ptr));
      XMLTagged.append(workingString.substring(0, endPointer));
      workingString = workingString.substring(endPointer); //in_ptr, working.length()

//   XMLTagged.append("<"+pos+">"+word+"</"+pos+"> ");
//  if (pos.equals(".")) {
//    XMLTagged.append("\n<S>");
//    sent = true;
//  }
    }
// if (sent) XMLTagged.append("</S>");
    return XMLTagged.toString();
  }

  public String explainAnnotation(edu.cmu.minorthird.text.TextLabels labels, edu.cmu.minorthird.text.Span documentSpan)
  {
    return "no idea";
  }


  public static void main(String[] args) throws Exception
  {
    if (args.length != 2)
    {
			System.out.println(montyLingua.tag_text("hello"));
			System.out.println(montyLingua.tag_text("world"));

      log.info("Usage:\t1. java POSTagger [input_file] [output_file]\n\t2. java POSTagger [input_dir]  [output_dir]\n\t3. java POSTagger [input_file] [output_dir]");
      return;
    }
    //montyLingua = new JMontyLingua();


    File inFile = new File(args[0]);
    File outFile = new File(args[1]);
    if (!inFile.exists())
    {
      log.fatal("Error: File " + inFile + " could not be found!");
      return;
    }

    if (inFile.isFile())
    {
      if (outFile.isDirectory())
        outFile = new File(outFile.getPath() + File.separator + inFile.getName());

//      writeFile(outFile, POSTag(IOUtil.readFile(inFile), false));
      writeFile(new File(outFile.getName() + ".l"), POSTag(IOUtil.readFile(inFile)));
    }
    else if (inFile.isDirectory())
    {
      if (!outFile.exists())
        outFile.mkdir();

      File[] fileList = inFile.listFiles();
      for (int i = 0; i < fileList.length; i++)
        if (fileList[i].isFile())
        {
          File outTo = new File(outFile.getPath() + File.separator + fileList[i].getName());
          log.debug("tagging " + fileList[i]);
          writeFile(outTo, POSTag(IOUtil.readFile(fileList[i])));
        }
    }
  }


}
