package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.TextBase;

import java.util.Set;

/**
 * This class...
 * @author ksteppe
 */
public class TestTextLabels extends BasicTextLabels
{
  public TestTextLabels(TextBase textBase)
  {
    super(textBase);
  }

  // get the set of spans with a given type in the given document
  public Set getTypeSet(String type, String documentId)
  {
    return super.getTypeSet(type, documentId);
  }
}
