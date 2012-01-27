package edu.cmu.minorthird.text;

/**
 * Represents a stand-off annotation by character ie offset, length, and type
 * The stand-off annotation is generally immutable
 * @author ksteppe
 */

public class CharAnnotation
{
  private int offset;
  private int length;
  private String type;

  /**
   * create the annotation
   * @param offset - start index in the string
   * @param length - # of chars covered
   * @param type - the name of the annotation applied
   */
  public CharAnnotation(int offset, int length, String type)
  {
    this.offset = offset;
    this.length = length;
    this.type = type;
  }

  public int getLength()
  { return length; }

  public void setLength(int length)
  { this.length = length; }

  public int getOffset()
  { return offset; }

  public void setOffset(int offset)
  { this.offset = offset; }

  public String getType()
  { return type; }

  public void setType(String type)
  { this.type = type; }

  @Override
	public String toString()
  {
    String string = "[CharAnnotation:";
    string += " offset=" + offset;
    string += " length=" + length;
    string += " type=" + type;
    string += "]";

    return string;
  }
}
