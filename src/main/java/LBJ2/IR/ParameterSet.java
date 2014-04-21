package LBJ2.IR;

import java.util.Arrays;
import LBJ2.Pass;


/**
  * Represents a set of possible parameters, used when doing
  * parameter-tuning. The list of possible values for a
  * parameter is stored in {@link #parameterList} as an
  * <code>ExpressionList</code>.
  *
  * <p>The set can be defined either as a comma-separated list
  * of possible values, or it can be defined as a range in terms
  * of a start value, end value, and increment factor, which
  * can then be converted into the explicit list of possible values.
  *
  * <p>The LBJ syntax for defining the parameter set is to declare
  * the possible values inside of double curly braces, either as
  * an explicit list or as a range. The following two examples
  * are equivalent:
  * <ul>
  *  <li><code>{{2,3,4,5}}</code></li>
  *  <li><code>{{2-&gt;5:1}}</code></li>
  * </ul>
  *
  * @author Michael Paul
 **/
public class ParameterSet extends Expression
{
  /** Remembers how many instances of this class have been instantiated. */
  public static int count;


  /** The list of possible values for this parameter. */
  private ExpressionList parameterList;
  /**
    * The name of the parameter that will be printed in method signatures in
    * the generated code.
   **/
  private String parameterName;
  /** The start value for the range. */
  public Expression start;
  /** The end value for the range. */
  public Expression end;
  /** The factor to increment by. */
  public Expression increment;
  /** The most specific type for the values in this set. */
  public Type type;
  /**
    * <code>true</code> iff this parameter set appears inside the
    * <code>rounds</code> clause of a {@link LearningClassifierExpression}.
   **/
  public boolean inRounds;


  /**
    * Initializing constructor. Sets the list of possible parameter values.
    *
    * @param list       The list of possible values for the parameter
   **/
  public ParameterSet(ExpressionList list) { this(-1, -1, list); }

  /**
    * Full constructor. Sets the list of possible parameter values.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param list       The list of possible values for the parameter.
   **/
  public ParameterSet(int line, int byteOffset, ExpressionList list) {
    super(line, byteOffset);
    parameterList = list;
    parameterName = "a" + count++;
  }

  /**
    * Initializing constructor. Sets the range parameters.
    *
    * @param s   The start value.
    * @param e   The end value.
    * @param i   The increment factor.
   **/
  public ParameterSet(Expression s, Expression e, Expression i) {
    this(-1, -1, s, e, i);
  }

  /**
    * Full constructor. Sets the range parameters.
    *
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param s          The start value.
    * @param e          The end value.
    * @param i          The increment factor.
   **/
  public ParameterSet(int line, int byteOffset, Expression s, Expression e,
                      Expression i) {
    super(line, byteOffset);
    start = s;
    end = e;
    increment = i;
    parameterName = "a" + count++;
  }


  /** Returns the value of {@link #parameterName}. */
  public String getParameterName() { return parameterName; }


  /** <code>true</code> iff this parameter set was specified as a range. */
  public boolean isRange() { return start != null; }


  /** Returns a list iterator over {@link #parameterList}. */
  public ExpressionList.ExpressionListIterator listIterator() {
    return parameterList.listIterator();
  }


  /** Returns the first element of the list. */
  public Expression getFirst() {
    ExpressionList.ExpressionListIterator iterator = listIterator();
    return iterator.hasNext() ? iterator.nextItem() : null;
  }


  /**
    * Converts this parameter set's {@link #start}, {@link #end}, and
    * {@link #increment} expressions (which must represent {@link Constant}s
    * of a {@link PrimitiveType} other than <code>boolean</code>) into an
    * explicit list of values.  The {@link #type} field must be set
    * appropriately for before calling this method.
   **/
  public void convertRange() {
    if (start == null)
      throw
        new IllegalArgumentException(
            "Can't call ParameterSet.convertRange() when start == null");
    if (type == null)
      throw
        new IllegalArgumentException(
            "Can't call ParameterSet.convertRange() when type == null");
    if (!(type instanceof PrimitiveType))
      throw
        new IllegalArgumentException(
            "Can't call ParameterSet.convertRange() when type isn't "
            + "primitive");
    PrimitiveType pt = (PrimitiveType) type;
    if (pt.type == PrimitiveType.BOOLEAN)
      throw
        new IllegalArgumentException(
            "Can't call ParameterSet.convertRange() when type is boolean");

    parameterList = new ExpressionList();

    if (pt.type == PrimitiveType.CHAR) {
      int s = (int) ((Constant) start).value.charAt(1);
      int e = (int) ((Constant) end).value.charAt(1);
      int i = Integer.parseInt(((Constant) increment).value);
      int m = i / Math.abs(i);
      e *= m;

      for (int j = s; m*j <= e; j += i)
        parameterList.add(
            new Constant(this.line, this.byteOffset, "" + ((char) j)));
    }
    else {
      double s = Double.parseDouble(((Constant) start).value);
      double e = Double.parseDouble(((Constant) end).value);
      double i = Double.parseDouble(((Constant) increment).value);
      double m = i / Math.abs(i);
      e *= m;

      for (double a = s; m*a <= e; a += i) {
        String num = "";

        switch (pt.type) {
          case PrimitiveType.FLOAT: case PrimitiveType.DOUBLE:
            num += a;
            break;
          case PrimitiveType.BYTE: case PrimitiveType.SHORT:
          case PrimitiveType.INT: case PrimitiveType.LONG:
            num += Math.round(a);
            break;
        }

        parameterList.add(new Constant(this.line, this.byteOffset, num));
      }
    }

    // If we didn't add the end value, add it
    /*
    if (lastEntry != e) {
      Double num = new Double(e);
      list.add(new Constant(this.line, this.byteOffset, num.toString()));
    }
    */
  }


  /**
    * Parses integers out of every constant in the set and returns them in a
    * sorted array.  If this parameter set was specified as a range, it is
    * assumed that {@link #convertRange()} has already been called.
   **/
  public int[] toSortedIntArray() {
    int[] values = new int[parameterList.size()];
    ExpressionList.ExpressionListIterator I = listIterator();
    for (int i = 0; I.hasNext(); ++i)
      values[i] = Integer.parseInt(((Constant) I.next()).value);
    Arrays.sort(values);
    return values;
  }


  /**
    * Assuming that {@link #convertRange()} has already been called (if
    * necessary) and that every expression in {@link #parameterList} is a
    * {@link Constant}, this method produces an array of <code>String</code>s
    * containing the values of the constants.  The return type of the method
    * is <code>Object[]</code> so that its elements can be replaced by objects
    * of other types, which is convenient during parameter tuning.
   **/
  public Object[] toStringArray() {
    Object[] values = new Object[parameterList.size()];
    ExpressionList.ExpressionListIterator I = listIterator();
    for (int i = 0; I.hasNext(); ++i) values[i] = ((Constant) I.next()).value;
    return values;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    if (start != null) {
      ASTNodeIterator result = new ASTNodeIterator(3);
      result.children[0] = start;
      result.children[1] = end;
      result.children[2] = increment;
      return result;
    }

    return parameterList.iterator();
  }


  /**
    * Two parameter sets are equivalent when their constituent expressions are
    * the same.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof ParameterSet)) return false;
    ParameterSet p = (ParameterSet) o;
    return
      start == null ? parameterList.equals(p.parameterList)
                    : start.equals(p.start) && end.equals(p.end)
                      && increment.equals(p.increment);
  }


  /** A hash code based on the hash codes of the constituent expressions. */
  public int hashCode() {
    return
      start == null ? parameterList.hashCode()
                    : 31 * start.hashCode() + 23 * end.hashCode()
                      + 17 * increment.hashCode();
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    ParameterSet c =
      new ParameterSet(this.line, this.byteOffset,
                       (ExpressionList) parameterList.clone());
    c.start = start;
    c.end = end;
    c.increment = increment;
    return c;
  }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  public void runPass(Pass pass) { pass.run(this); }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer) {
    if (parenthesized) buffer.append("(");
    buffer.append("{{ ");
    if (start != null) {
      start.write(buffer);
      buffer.append(" -> ");
      end.write(buffer);
      buffer.append(" : ");
      increment.write(buffer);
    }
    else parameterList.write(buffer);
    buffer.append(" }}");
    if (parenthesized) buffer.append(")");
  }
}

