package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.BasicTextEnv;
import edu.cmu.minorthird.text.TextBase;

import java.util.Set;

/**
 * This class...
 * @author ksteppe
 */
public class TestTextEnv extends BasicTextEnv
{
  public TestTextEnv(TextBase textBase)
  {
    super(textBase);
  }

  // get the set of spans with a given type in the given document
  protected Set getTypeSet(String type, String documentId)
  {
    return super.getTypeSet(type, documentId);
  }
}
