package edu.cmu.minorthird;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The Loader interface was introduced to have a common base for both DatasetLoader
 * and TextBaseLoader.  The common base is needed for the GUI combo box, but makes
 * sense as they both fit the pattern.
 * @author ksteppe
 */
public interface Loader
{
  /**
   * Load the data in the provided file
   * The loading object should be configured previous to this call to load the file
   * properly.
   * @param f File to load - some implementations will load only files, some directories
   *    etc.
   */
//  public void load(File f) throws IOException;
//  public Object load(File f, FileChooser fc);
}
