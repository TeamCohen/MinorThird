package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * A no options loader.
 * It checks the given file/string and does one of two things:
 *  1) if Directory - load each file as a document assuming that labels are embedded
 *  2) if file - load each line as a document assuming the first word is a document name
 * @author ksteppe
 */
public class SimpleTextLoader
{
  private static Logger log = Logger.getLogger(SimpleTextLoader.class);

  public static TextLabels load(File file, boolean externalLabelFile)
  {
    TextBase base = null;
    MutableTextLabels tempLabels = null;
    MutableTextLabels labels = null;

    try
    {
      if (!file.isDirectory())
        base = TextBaseLoader.loadDocPerLine(file, false);
      else
        tempLabels = TextBaseLoader.loadDirOfTaggedFiles(file);

      if (base == null)
        base = tempLabels.getTextBase();
      labels = new BasicTextLabels(base);

      if (externalLabelFile)
      {
        String fileName = file.getName();
				if (fileName.lastIndexOf('.')>=0) fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        fileName += ".labels";
        File dir = file.getParentFile();
        File labelsFile = new File(dir, fileName);
        new TextLabelsLoader().importOps(labels, base, labelsFile);
      }
      else if (tempLabels != null)
        labels = tempLabels;

    }
    catch (Exception e)
    { log.error(e, e); }

    return labels;

  }

  public static TextLabels load(String fileName, boolean externalLabelFile)
  {
    return load(new File(fileName), externalLabelFile);
  }

//-------------------Instance stuff for the Wizard ----------------------------------------
  public boolean labelFile = true;
  public SimpleTextLoader()
  {}

  public TextLabels load(File file)
  { return load(file, labelFile); }

  public TextLabels load(String fileName)
  { return load(fileName, labelFile); }

  public boolean isLabelFile()
  { return labelFile; }

  public void setLabelFile(boolean labelFile)
  { this.labelFile = labelFile; }
//--------------------------------------------------------------------------------

}
