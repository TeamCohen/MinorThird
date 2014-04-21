package LBJ2.IR;


/**
  * Abstract class representing either a scalar or a subscript variable.
  *
  * @author Nick Rizzolo
 **/
public abstract class VariableInstance extends Expression
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
  VariableInstance(int line, int byteOffset) { super(line, byteOffset); }
}

