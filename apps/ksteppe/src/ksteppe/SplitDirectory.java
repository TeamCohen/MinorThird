package ksteppe;

import java.io.File;
import java.io.IOException;

/**
 * This class...
 * @author ksteppe
 */
public class SplitDirectory
{
  private static String from;
  private static File fromDir;
  private static String to;
//  private static File toDir;
  private static int numFiles = 0;
  private static int numDirs = 0;

  /**
   * -from directory to be split
   * -to base name of directories to create
   * -numFiles or -Files
   *    number of files per new directory
   * -numDir or -Dir
   *    number of new directories (balance number of files)
   *
   * @param args
   */
  public static void main(String[] args)
  {
    int j = 0;
    for (int i = 0; i < 3; i++)
    {
      String command = args[j++];
      if (command.indexOf("fr") > -1)
      {
        from = args[j++];
        fromDir = new File(from);
      }
      else if (command.indexOf("to") > -1)
      {
        to = args[j++];
//        toDir = new File(to);
      }
      else if (command.indexOf("F") > -1)
        numFiles = Integer.parseInt(args[j++]);
      else if (command.indexOf("D") > -1)
        numDirs = Integer.parseInt(args[j++]);
      else
      {
        System.out.println("command not recognized: " + command);
        return;
      }
    }

    File[] fromList = fromDir.listFiles();

    if (numDirs > 0)
      splitByDirs(fromList);
    else
      splitByFiles(fromList);


  }

  private static void splitByFiles(File[] fromList)
  {
    int d = 0;
    int k = 0;
    while (k < fromList.length)
    {
      File destDir = new File(to + "_" + d);
      destDir.mkdir();

      for (int i = 0; i < numFiles & k < fromList.length; i++)
      { copyFile(destDir, fromList, k++); }

      d++;
    }
  }

  private static void splitByDirs(File[] fromList)
  {
    int k = 0;
    for (int i = 0; i < numDirs; i++)
    {
      File destDir = new File(to + "_" + i);
      destDir.mkdir();

      for (int j = 0; j < fromList.length / numDirs && k < fromList.length; j++)
      { copyFile(destDir, fromList, k++); }
    }
  }

  private static void copyFile(File destDir, File[] fromList, int fileIndex)
  {
    File copyTo = new File(destDir, fromList[fileIndex].getName());
    //try { com.ksteppe.fileUtils.FileTool.copy(fromList[fileIndex], copyTo); }
    //catch (IOException e)
    //{ e.printStackTrace(); } //To change body of catch statement use Options | File Templates.
  }
}
