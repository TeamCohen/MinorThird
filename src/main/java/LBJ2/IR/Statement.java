package LBJ2.IR;


/**
  * Abstract class from which statements are derived.  LBJ2 currently has only
  * one type of statement: the assignment statement.
  *
  * @author Nick Rizzolo
 **/
public abstract class Statement extends ASTNode
{
  /**
    * Default constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  Statement(int line, int byteOffset) { super(line, byteOffset); }
}

