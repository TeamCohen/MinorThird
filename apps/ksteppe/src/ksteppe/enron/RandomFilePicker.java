package ksteppe.enron;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class...
 * @author ksteppe
 */
public class RandomFilePicker
{
  public static void main(String[] args)
  {
    if (args.length !=4)
      usage();

    File base;
    File dest = new File(args[3]);

    base = new File(args[2]);

    int numChunks = Integer.parseInt(args[0]);
    int numMsg = Integer.parseInt(args[1]);
    System.out.println("loading total of " + numChunks * numMsg + " messages");

//    HashMap selections = new HashMap(numChunks * numMsg, 0.95f);
    HashSet selections = new HashSet(numChunks * numMsg, 0.95f);

    //possible directories
    File[] dirs = base.listFiles();
    File[][] messages = new File[dirs.length][];
    for (int i = 0; i < dirs.length; i++)
    {
      //get all_documents
      File dir = new File(dirs[i], "all_documents");
      System.out.println("loading messages in " + dir);
      messages[i] = dir.listFiles();
    }

    for (int chunk = 0; chunk < numChunks; chunk++)
    {
      int msg = 0;
      while (msg < numMsg)
      {
        int person = (int)Math.floor(Math.random() * dirs.length);
        int email = (int)Math.floor(Math.random() * messages[person].length);

        System.out.println("selected: [" + person + "][" + email + "] - " + messages[person][email]);
        if (!selections.add(messages[person][email]))
        {
          System.out.println("already have that selection!  try again");
          continue; //if it's already in the set, find another
        }
        else
          msg++;
      }
      System.out.println("completed chunk #" + chunk);
    }

    if (selections.size() != numChunks * numMsg)
    {
      System.out.println("wrong number of messages! - " + selections.size());
      return;
    }

    Iterator it = selections.iterator();
    for (int chunk = 0; chunk < numChunks; chunk++)
    {
      //make directory
      String name = dest.getAbsoluteFile() + "" + chunk;
      File newDir = new File(name);
      newDir.mkdir();

      for (int msg = 0; msg < numMsg; msg++)
      {
        File f = (File)it.next(); //get file from selections
        try { com.ksteppe.fileUtils.FileTool.copy(f, newDir); }
        catch (IOException e)
        { e.printStackTrace(); } //To change body of catch statement use Options | File Templates.
      }
    }


  }

  private static void usage()
  {
    System.out.print(RandomFilePicker.class.getName() + " ");
    System.out.print("#chunks #msgPerChunk dir destinationStem");
  }
}
