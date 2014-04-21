package LBJ2.IR;

import java.util.HashSet;


/**
  * A quantified constraint expression is a compact way to specify a
  * constraint as a function of every object in a given collection.
  *
  * @author Nick Rizzolo
 **/
public abstract class QuantifiedConstraintExpression
                extends ConstraintExpression
{
  /**
    * (&not;&oslash;) The variable specified by this argument is set to each
    * of the objects from the collection in turn and used throughout the
    * quantified constraint.
   **/
  public Argument argument;
  /**
    * (&not;&oslash;) The objects to iterate through; it must evaluate to a
    * Java <code>Collection</code>.
   **/
  public Expression collection;
  /** (&not;&oslash;) The quantified constraint. */
  public ConstraintExpression constraint;
  /**
    * Filled in by <code>SemanticAnalysis</code>, this flag is set if
    * <code>collection</code> contains any quantified variables.
   **/
  public boolean collectionIsQuantified;


  /**
    * Full constructor.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param a          The quantification variable specification.
    * @param c          Evaluates to the collection of objects.
    * @param co         The quantified constraint.
   **/
  public QuantifiedConstraintExpression(int line, int byteOffset, Argument a,
                                        Expression c, ConstraintExpression co)
  {
    super(line, byteOffset);
    argument = a;
    collection = c;
    constraint = co;
  }


  /**
    * Returns a set of <code>Argument</code>s storing the name and type of
    * each variable that is a subexpression of this expression.  This method
    * cannot be run before <code>SemanticAnalysis</code> runs.
   **/
  public HashSet getVariableTypes() {
    HashSet result = collection.getVariableTypes();
    result.addAll(constraint.getVariableTypes());
    return result;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(3);
    I.children[0] = argument;
    I.children[1] = collection;
    I.children[2] = constraint;
    return I;
  }
}

