package ksteppe.enron;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class...
 * @author ksteppe
 */
public class RandomFilePicker
{
  private static class Pick
  {
    int person;
    int email;

    public Pick(int person, int email)
    {
      this.email = email;
      this.person = person;
    }

    public String toString()
    {
      return "[" + person + "][" + email + "]";
    }

    public boolean equals(Object obj)
    {
      Pick p = (Pick)obj;
      if (this.person == p.person && this.email == p.email)
        return true;
      else
        return false;
    }

    public int hashCode()
    {
      String sRep = this.person + "/" + this.email;
      return sRep.hashCode();
    }
  }

  public static void main(String[] args)
  {
    if (args.length !=4)
      usage();

    File base= new File(args[2]);
    File dest = new File(args[3]);

    int numChunks = Integer.parseInt(args[0]);
    int numMsg = Integer.parseInt(args[1]);
    System.out.println("loading total of " + numChunks * numMsg + " messages");

//    HashMap selections = new HashMap(numChunks * numMsg, 0.95f);
    HashSet selections = new HashSet(numChunks * numMsg, 0.95f);
    HashSet exempted = new HashSet(160, 0.90f);

    //possible directories
    File[] dirs = base.listFiles();
    File[][] messages = new File[dirs.length][];
    for (int i = 0; i < dirs.length; i++)
    {
      //get all_documents
      File dir = new File(dirs[i], "all_documents");
      System.out.println(i + " - loading messages in " + dir);
      messages[i] = dir.listFiles();
      if (messages[i] == null)
      {
        System.out.println(dirs[i].getName() + " doesn't have all_documents");
        exempted.add(new Integer(i));
      }
    }

    String exemptFile = dest.getAbsoluteFile() + "exempted";
    File ef = new File(exemptFile);
    try
    {
      ef.createNewFile();
      PrintWriter out = new PrintWriter(new FileWriter(ef));
      for (Iterator it = exempted.iterator(); it.hasNext();)
      {
        out.println(dirs[((Integer)it.next()).intValue()].getName());
      }
      out.close();
    }
    catch (IOException e)
    { e.printStackTrace(); } //To change body of catch statement use Options | File Templates. }

    for (int chunk = 0; chunk < numChunks; chunk++)
    {
      int msg = 0;
      while (msg < numMsg)
      {
        int person = (int)Math.floor(Math.random() * dirs.length);
        if (exempted.contains(new Integer(person)))
        {
          System.out.println("exempted person (" + person + "), skipping");
          continue;
        }
        int email = (int)Math.floor(Math.random() * messages[person].length);

        System.out.println("selected: [" + person + "][" + email + "] - " + messages[person][email]);
        if (!selections.add(new Pick(person, email)))
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
        Pick p = (Pick)it.next();
        System.out.println("copy " + p);
        String newName = dirs[p.person].getName() + "_all_documents_" + messages[p.person][p.email].getName();
        File newFile = new File(newDir, newName);

        if (newFile.exists())
        { System.out.println("ERROR - file exists: " + newFile.getName()); }
        //try { com.ksteppe.fileUtils.FileTool.copy(messages[p.person][p.email], newFile); }
        //catch (IOException e)
        //{ e.printStackTrace(); } //To change body of catch statement use Options | File Templates.
      }
    }


  }

  private static void usage()
  {
    System.out.print(RandomFilePicker.class.getName() + " ");
    System.out.print("#chunks #msgPerChunk dir destinationStem");
  }
}
