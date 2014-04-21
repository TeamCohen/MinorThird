package LBJ2.IR;

import LBJ2.Pass;


/**
  * LBJ supports every Java operator.  This class stores information about
  * those operators, such as their symbols and precedences, as well as
  * information about LBJ's new operators.  The static constant fields defined
  * in this class serve as indeces into its static array fields.
  *
  * <p>
  * The precedence values for Java operators were taken from
  * <a target=_top href="http://www.java-faq.com/java-operator-precedence.shtml">Java-FAQ.com</a>.
  * @author Nick Rizzolo
 **/
public class Operator extends ASTNode
{
  /** Value of the <code>operation</code> variable. */
  public static final int DOT = 0;
  /** Value of the <code>operation</code> variable. */
  public static final int PRE_INCREMENT = 1;
  /** Value of the <code>operation</code> variable. */
  public static final int POST_INCREMENT = 2;
  /** Value of the <code>operation</code> variable. */
  public static final int PRE_DECREMENT = 3;
  /** Value of the <code>operation</code> variable. */
  public static final int POST_DECREMENT = 4;
  /** Value of the <code>operation</code> variable. */
  public static final int BITWISE_NOT = 5;
  /** Value of the <code>operation</code> variable. */
  public static final int NOT = 6;
  /** Value of the <code>operation</code> variable. */
  public static final int NEW = 7;
  /** Value of the <code>operation</code> variable. */
  public static final int TIMES = 8;
  /** Value of the <code>operation</code> variable. */
  public static final int DIVIDE = 9;
  /** Value of the <code>operation</code> variable. */
  public static final int MOD = 10;
  /** Value of the <code>operation</code> variable. */
  public static final int PLUS = 11;
  /** Value of the <code>operation</code> variable. */
  public static final int MINUS = 12;
  /** Value of the <code>operation</code> variable. */
  public static final int LEFT_SHIFT = 13;
  /** Value of the <code>operation</code> variable. */
  public static final int SIGNED_RIGHT_SHIFT = 14;
  /** Value of the <code>operation</code> variable. */
  public static final int UNSIGNED_RIGHT_SHIFT = 15;
  /** Value of the <code>operation</code> variable. */
  public static final int LESS_THAN = 16;
  /** Value of the <code>operation</code> variable. */
  public static final int LESS_THAN_OR_EQUAL = 17;
  /** Value of the <code>operation</code> variable. */
  public static final int GREATER_THAN = 18;
  /** Value of the <code>operation</code> variable. */
  public static final int GREATER_THAN_OR_EQUAL = 19;
  /** Value of the <code>operation</code> variable. */
  public static final int INSTANCEOF = 20;
  /** Value of the <code>operation</code> variable. */
  public static final int EQUAL = 21;
  /** Value of the <code>operation</code> variable. */
  public static final int NOT_EQUAL = 22;
  /** Value of the <code>operation</code> variable. */
  public static final int BITWISE_AND = 23;
  /** Value of the <code>operation</code> variable. */
  public static final int XOR = 24;
  /** Value of the <code>operation</code> variable. */
  public static final int BITWISE_OR = 25;
  /** Value of the <code>operation</code> variable. */
  public static final int AND = 26;
  /** Value of the <code>operation</code> variable. */
  public static final int OR = 27;
  /** Value of the <code>operation</code> variable. */
  public static final int CONDITIONAL = 28;
  /** Value of the <code>operation</code> variable. */
  public static final int ASSIGN = 29;
  /** Value of the <code>operation</code> variable. */
  public static final int MULTIPLY_ASSIGN = 30;
  /** Value of the <code>operation</code> variable. */
  public static final int DIVIDE_ASSIGN = 31;
  /** Value of the <code>operation</code> variable. */
  public static final int MOD_ASSIGN = 32;
  /** Value of the <code>operation</code> variable. */
  public static final int PLUS_ASSIGN = 33;
  /** Value of the <code>operation</code> variable. */
  public static final int MINUS_ASSIGN = 34;
  /** Value of the <code>operation</code> variable. */
  public static final int LEFT_SHIFT_ASSIGN = 35;
  /** Value of the <code>operation</code> variable. */
  public static final int SIGNED_RIGHT_SHIFT_ASSIGN = 36;
  /** Value of the <code>operation</code> variable. */
  public static final int UNSIGNED_RIGHT_SHIFT_ASSIGN = 37;
  /** Value of the <code>operation</code> variable. */
  public static final int AND_ASSIGN = 38;
  /** Value of the <code>operation</code> variable. */
  public static final int OR_ASSIGN = 39;
  /** Value of the <code>operation</code> variable. */
  public static final int XOR_ASSIGN = 40;
  /** Value of the <code>operation</code> variable. */
  public static final int CONJUNCTION = 41;
  /** Value of the <code>operation</code> variable. */
  public static final int ARROW = 42;
  /** Value of the <code>operation</code> variable. */
  public static final int CONSTRAINT_EQUAL = 43;
  /** Value of the <code>operation</code> variable. */
  public static final int CONSTRAINT_NOT_EQUAL = 44;
  /** Value of the <code>operation</code> variable. */
  public static final int LOGICAL_CONJUNCTION = 45;
  /** Value of the <code>operation</code> variable. */
  public static final int LOGICAL_DISJUNCTION = 46;
  /** Value of the <code>operation</code> variable. */
  public static final int IMPLICATION = 47;
  /** Value of the <code>operation</code> variable. */
  public static final int DOUBLE_IMPLICATION = 48;

  /**
    * This array contains the text representations of every symbol that
    * objects of this class can represent.
    * <code>
    * {
    *   ".", "++", "++", "--", "--", "~", "!", "new", "*", "/", "%", "+", "-",
    *   "&lt;&lt;", "&gt;&gt;", "&gt;&gt;&gt;", "&lt;", "&lt;=", "&gt;",
    *   "&gt;=", "instanceof", "==", "!=", "&amp;", "^", "|", "&amp;&amp;",
    *   "||", "?", "=", "*=", "/=", "%=", "+=", "-=", "&lt;&lt;=",
    *   "&gt;&gt;=", "&gt;&gt;&gt;=", "&amp;=", "|=", "^=", "&amp;&amp;",
    *   "&lt;-", "::", "!:", "/\\", "\\/", "=&gt;", "&lt;=&gt;"
    * }</code>
   **/
  private static final String[] symbols =
    {
      ".", "++", "++", "--", "--", "~", "!", "new", "*", "/", "%", "+", "-",
      "<<", ">>", ">>>", "<", "<=", ">", ">=", "instanceof", "==", "!=", "&",
      "^", "|", "&&", "||", "?", "=", "*=", "/=", "%=", "+=", "-=", "<<=",
      ">>=", ">>>=", "&=", "|=", "^=", "&&", "<-", "::", "!:", "/\\", "\\/",
      "=>", "<=>"
    };

  /**
    * This array contains the precedences of every operator in the same order
    * that they appear in the <code>symbols</code> array.
    * <code>=
    * {
    *   1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7,
    *   8, 9, 10, 11, 12, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
    *   15, 16, 1, 1, 2, 3, 4, 5
    * }
    * </code>.
    * <br>
    * A lower value represents a more tightly binding operator.
   **/
  private static final int[] precedences =
    {
      1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 8,
      9, 10, 11, 12, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15,
      16, 1, 1, 2, 3, 4, 5
    };

  /**
    * Produces the name of an operator given its index.
    *
    * @param operation  The index of the operation.  (See the static member
    *                   variables.)
    * @return A String holding the name of the operator.
   **/
  public static String operatorSymbol(int operation) {
    return symbols[operation];
  }

  /**
    * Produces the precedence of an operator given its index.
    *
    * @param operation  The index of the operation.
    * @return The precedence of the corresponding operator.
   **/
  public static int operatorPrecedence(int operation) {
    return precedences[operation];
  }


  /** The index of the operation represented by this <code>Operator</code>. */
  public int operation;


  /**
    * Default constructor.  Line and byte offset information, having not been
    * specified, is set to -1.
    *
    * @param operation  The index of the operation.
   **/
  public Operator(int operation) { this(operation, -1, -1); }

  /**
    * Full constructor.
    *
    * @param operation  The index of the operation.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public Operator(int operation, int line, int byteOffset) {
    super(line, byteOffset);
    this.operation = operation;
  }


  /**
    * Produces the precedence of this operator.
    *
    * @return The precedence of this operator.
   **/
  public int getPrecedence() { return precedences[operation]; }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() { return operation + 1; }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof Operator)) return false;
    return ((Operator) o).operation == operation;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() { return new ASTNodeIterator(0); }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() { return new Operator(operation); }


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
    buffer.append(symbols[operation]);
  }
}

