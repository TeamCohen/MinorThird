package LBJ2.IR;


/**
  * Used to iterate though the children of an AST node.  It is assumed that
  * the <code>children</code> array never contains a <code>null</code>
  * reference.
  *
  * @author Nick Rizzolo
 **/
public class ASTNodeIterator
{
  /** The nodes iterated through by this iterator. */
  public ASTNode[] children;
  /** Index into the <code>children</code> array. */
  protected int index;


  /** Initializes <code>index</code>, but not <code>children</code>. */
  public ASTNodeIterator() { index = 0; }

  /**
    * The <code>children</code> array will have the specified length.
    *
    * @param l  The number of children to iterate through.
   **/
  public ASTNodeIterator(int l) {
    this();
    children = new ASTNode[l];
  }


  /**
    * Determines whether there are any child nodes left to be accessed.
    *
    * @return <code>true</code> iff there are child nodes remaining.
   **/
  public boolean hasNext() {
    return children != null && index < children.length;
  }


  /**
    * Returns the next child AST node.
    *
    * @return The next child AST node.
   **/
  public ASTNode next() {
    return children == null || index == children.length
           ? null : children[index++];
  }


  /** Restarts the iterator. */
  public void reset() { index = 0; }
}

