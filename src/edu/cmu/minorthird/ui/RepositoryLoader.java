package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.Loader;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Wraps the Fancy Loader for the Wizard
 * @author ksteppe
 */
public class RepositoryLoader implements Loader, FilenameFilter
{
  public String[] getFileList()
  {
    String dirName = FancyLoader.getProperty(FancyLoader.SCRIPTDIR_PROP);
    File dir = new File(dirName);
    String[] files = dir.list(this);
    return files;
  }

  public TextLabels load(String scriptName)
  {
    return FancyLoader.loadTextLabels(scriptName);
  }

  /**
   * Currently accepts all files
   */
  public boolean accept(File dir, String name)
  {
//    if (name.endsWith(".bsh"))
      return true;
//    else
//      return false;
  }
}
