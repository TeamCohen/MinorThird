package LBJ2.IR;


/**
  * Abstract class for representing expressions that can stand alone as a
  * statement.
  *
  * @author Nick Rizzolo
 **/
public abstract class StatementExpression extends Expression
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
  StatementExpression(int line, int byteOffset) { super(line, byteOffset); }
}

