package LBJ2.IR;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents the assignment of a classifier expression to a method
  * signature.
  *
  * @author Nick Rizzolo
 **/
public class ClassifierAssignment extends Declaration
{
  /**
    * This value is used in place of the field access which appears as an
    * argument to <code>cachedin</code> to indicate that in fact,
    * <code>cached</code> was used instead.
   **/
  public static final String mapCache = "!!!";


  /** (&not;&oslash;) The return type of the classifier. */
  public ClassifierReturnType returnType;
  /** (&not;&oslash;) The input specification of the classifier. */
  public Argument argument;
  /** (&not;&oslash;) The expression representing the classifier. */
  public ClassifierExpression expression;
  /**
    * (&oslash;) The expression representing the field to cache this
    * classifier's result in.
   **/
  public Name cacheIn;
  /**
    * Whether the classifier will have a single example feature vector cache.
   **/
  public boolean singleExampleCache;


  /**
    * Full constructor.  Line and byte offset information is taken from the
    * type.
    *
    * @param co A Javadoc comment associated with the declaration.
    * @param t  The return type of the classifier.
    * @param n  The classifier's name.
    * @param a  The input specification of the classifier.
    * @param e  The expression representing the classifier.
    * @param ca The expression representing the field to cache this
    *           classifier's result in.
    * @param sc Whether or not to make a single example cache.
   **/
  public ClassifierAssignment(String co, ClassifierReturnType t, Name n,
                              Argument a, ClassifierExpression e, Name ca,
                              boolean sc) {
    super(co, n, t.line, t.byteOffset);
    returnType = t;
    argument = a;
    expression = e;
    cacheIn = ca;
    singleExampleCache = sc;
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the type.
    *
    * @param t  The return type of the classifier.
    * @param i  The identifier token representing the classifier's name.
    * @param a  The input specification of the classifier.
    * @param e  The expression representing the classifier.
   **/
  public ClassifierAssignment(ClassifierReturnType t, TokenValue i,
                              Argument a, ClassifierExpression e) {
    this(null, t, new Name(i), a, e, null, false);
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the type.
    *
    * @param t  The return type of the classifier.
    * @param i  The identifier token representing the classifier's name.
    * @param a  The input specification of the classifier.
    * @param e  The expression representing the classifier.
    * @param c  The expression representing the field to cache this
    *           classifier's result in.
   **/
  public ClassifierAssignment(ClassifierReturnType t, TokenValue i,
                              Argument a, ClassifierExpression e, Name c) {
    this(null, t, new Name(i), a, e, c, false);
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the type.
    *
    * @param t  The return type of the classifier.
    * @param i  The identifier token representing the classifier's name.
    * @param a  The input specification of the classifier.
    * @param e  The expression representing the classifier.
    * @param sc Whether or not to make a single example cache.
   **/
  public ClassifierAssignment(ClassifierReturnType t, TokenValue i,
                              Argument a, ClassifierExpression e, boolean sc)
  {
    this(null, t, new Name(i), a, e, null, sc);
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the type.
    *
    * @param t  The return type of the classifier.
    * @param i  The identifier token representing the classifier's name.
    * @param a  The input specification of the classifier.
    * @param e  The expression representing the classifier.
    * @param c  The expression representing the field to cache this
    *           classifier's result in.
    * @param sc Whether or not to make a single example cache.
   **/
  public ClassifierAssignment(ClassifierReturnType t, TokenValue i,
                              Argument a, ClassifierExpression e, Name c,
                              boolean sc) {
    this(null, t, new Name(i), a, e, c, sc);
  }


  /**
    * Returns the type of the declaration.
    *
    * @return The type of the declaration.
   **/
  public Type getType() {
    return
      new ClassifierType(argument.getType(), returnType,
                         expression instanceof LearningClassifierExpression);
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I = new ASTNodeIterator(cacheIn == null ? 3 : 4);
    I.children[0] = returnType;
    I.children[1] = argument;
    I.children[2] = expression;
    if (cacheIn != null) I.children[3] = cacheIn;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    return
      new ClassifierAssignment(
            comment,
            (ClassifierReturnType) returnType.clone(),
            (Name) name.clone(),
            (Argument) argument.clone(),
            (ClassifierExpression) expression.clone(),
            (Name) (cacheIn == null ? null : cacheIn.clone()),
            singleExampleCache);
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
    returnType.write(buffer);
    buffer.append(' ');
    name.write(buffer);
    buffer.append('(');
    argument.write(buffer);
    buffer.append(") ");

    if (singleExampleCache) buffer.append("cached ");

    if (cacheIn != null) {
      buffer.append("cachedin");

      if (cacheIn.toString().equals(ClassifierAssignment.mapCache))
        buffer.append("map");
      else {
        buffer.append(" ");
        cacheIn.write(buffer);
      }

      buffer.append(' ');
    }

    buffer.append("<- ");
    expression.write(buffer);
  }
}

