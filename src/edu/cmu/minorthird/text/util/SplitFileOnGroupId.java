package edu.cmu.minorthird.text.util;

import java.io.*;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class takes a data file (document group tokens)
 * and splits it into multiple files, each with one of the original groups removed
 *
 * Basic algorithm:
 *  1) iterate through the input file
 *     1.a) Tokenize each line to get the group id (token #2)
 *     1.b) add group id to a list
 *
 *  2) generate n output files
 *  3) iterate through input file again
 *     3.a) output to n - 1 files, skipping the file with corresponding index
 *
 * @author ksteppe
 */
public class SplitFileOnGroupId
{
  /** file to be split */
  File file;

  /** files to write to */
  File[] outputFiles;

  /** dynamic list of the group ids found */
  List groupIds;

  /** base name for output files */
  private String outputBaseName;

  /** array of output writers */
  private PrintWriter[] writers;

  public SplitFileOnGroupId(File file, String outputBase)
  {
    this.file = file;
    this.outputBaseName = outputBase;
  }

  /** called via command line:
   * edu.cmu.minorthird.text.util.SplitFileOnGroupId [input file] [base name for output files]
   * @param args see above
   */
  public static void main(String[] args)
  {
    SplitFileOnGroupId splitter = new SplitFileOnGroupId(new File(args[0]), args[1]);
    try
    {
      splitter.split();
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    catch (IOException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
  }

  /**
   * splits input file according to algorithm above
   */
  private void split() throws IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(file));
    //interate through input file
    while (in.ready())
    {
      String line = in.readLine();
      //tokenize each line
      String groupID = getGroupID(line);
      //add group id toke to groupIds
      this.groupIds.add(groupID);

    }

    //create output files
    this.outputFiles = new File[groupIds.size()];
    this.writers = new PrintWriter[groupIds.size()];

    for (int i = 0; i < groupIds.size(); i++)
    {
      String fileName = this.outputBaseName + "-" + i;
      File outFile = new File(fileName);
      this.outputFiles[i] = outFile;
      PrintWriter writer = new PrintWriter(new FileWriter(outFile));
      this.writers[i] = writer;
    }

    //interate through input file
    in.close();
    in = new BufferedReader(new FileReader(file));
    while (in.ready())
    {
      String line = in.readLine();
      //tokenize line
      String groupId = getGroupID(line);
      //identify group id

      //send to all output files, except index of the group
      int skipFile = this.groupIds.indexOf(groupId);
      for (int i = 0; i < this.writers.length; i++)
      {
        if (i != skipFile)
        {
          //write to file
          PrintWriter writer = this.writers[i];
          writer.println(line);
        }
      }
    }

    for (int i = 0; i < writers.length; i++)
    {
      PrintWriter writer = writers[i];
      writer.close();
    }
  }

  private String getGroupID(String line)
  {
    StringTokenizer tokenizer = new StringTokenizer(line, " ", false);
    //skip document id
    tokenizer.nextToken();

    //return group id
    return tokenizer.nextToken();
  }

}
