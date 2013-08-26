package LBJ2.IR;

import java.util.Iterator;
import java.util.LinkedList;

import LBJ2.Pass;
import LBJ2.frontend.TokenValue;


/**
  * Represents an inference specification.
  *
  * @author Nick Rizzolo
 **/
public class InferenceDeclaration extends Declaration
                                  implements LBJ2.CodeGenerator
{
  /** If no inference algorithm is specified, this algorithm is used. */
  public static final InstanceCreationExpression defaultInferenceConstructor =
    new InstanceCreationExpression(
        new Name("ILPInference"),
        new ExpressionList(
          new InstanceCreationExpression(
            new Name("GLPKHook"),
            new ExpressionList(),
            -1, -1)),
        -1, -1);


  /**
    * (&not;&oslash;) A specification of the object from which all variables
    * can be found.
    **/
  public Argument head;
  /**
    * (&not;&oslash;) The methods used to find the head object given objects
    * of different types.
   **/
  public HeadFinder[] headFinders;
  /**
    * (&not;&oslash;) Declarations describing how the scores produced by
    * various learning classifiers should be normalized.
   **/
  public NormalizerDeclaration[] normalizerDeclarations;
  /**
    * (&not;&oslash;) The constraint that must be respected during
    * optimization.
   **/
  public ConstraintDeclaration constraint;
  /**
    * Counts the number of <code>subjectto</code> clauses for error detection.
   **/
  public int subjecttoClauses;
  /** (&oslash;) A constructor for the inference algorithm to use. */
  public InstanceCreationExpression algorithm;
  /** Counts the number of <code>with</code> clauses for error detection. */
  public int withClauses;


  /**
    * Full constructor.
    *
    * @param com        A Javadoc comment associated with the declaration.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
    * @param n          The inference's name.
    * @param h          The specification of the head object.
    * @param f          An array of methods used to find the head object.
    * @param d          An array of normalizer declarations.
    * @param con        The constraint this inference must respect.
    * @param a          A constructor for the inference algorithm.
   **/
  public InferenceDeclaration(String com, int line, int byteOffset, Name n,
                              Argument h, HeadFinder[] f,
                              NormalizerDeclaration[] d,
                              ConstraintDeclaration con,
                              InstanceCreationExpression a) {
    super(com, n, line, byteOffset);

    head = h;
    headFinders = f;
    if (headFinders == null) headFinders = new HeadFinder[0];
    normalizerDeclarations = d;
    if (normalizerDeclarations == null)
      normalizerDeclarations = new NormalizerDeclaration[0];

    if (con == null) {
      constraint =
        new ConstraintDeclaration(
            null, -1, -1, new Name(name + "$subjectto"), h,
            new Block(
              new StatementList(
                new ExpressionStatement(
                  new ConstraintStatementExpression(
                    new ConstraintEqualityExpression(
                      new Operator(Operator.CONSTRAINT_EQUAL),
                      new Constant("true"),
                      new Constant("true")))))));
    }
    else constraint = con;

    subjecttoClauses = 1;
    algorithm = a;
    withClauses = algorithm == null ? 0 : 1;
  }

  /**
    * Parser's constructor.  Line and byte offset information is taken from
    * the first token.
    *
    * @param t  The first token indicates line and byte offset information.
    * @param i  The identifier token representing the classifier's name.
    * @param h  The specification of the head object.
    * @param c  A list of clauses from the body of the declaration.
   **/
  public InferenceDeclaration(TokenValue t, TokenValue i, Argument h,
                              LinkedList c) {
    this(null, t.line, t.byteOffset, new Name(i), h, null, null, null, null);
    subjecttoClauses = 0;
    LinkedList finders = new LinkedList();
    LinkedList normalizers = new LinkedList();

    for (Iterator I = c.iterator(); I.hasNext(); ) {
      Clause clause = (Clause) I.next();
      if (clause.type == Clause.HEAD_FINDER) finders.add(clause.argument);
      else if (clause.type == Clause.SUBJECTTO) {
        Block b = (Block) clause.argument;
        constraint =
          new ConstraintDeclaration(null, b.line, b.byteOffset,
                                    new Name(name + "$subjectto"), h, b);
        ++subjecttoClauses;
      }
      else if (clause.type == Clause.WITH) {
        algorithm = (InstanceCreationExpression) clause.argument;
        ++withClauses;
      }
      else if (clause.type == Clause.NORMALIZER_DECLARATION)
        normalizers.add(clause.argument);
    }

    headFinders =
      (HeadFinder[]) finders.toArray(new HeadFinder[finders.size()]);
    normalizerDeclarations =
      (NormalizerDeclaration[])
      normalizers.toArray(new NormalizerDeclaration[normalizers.size()]);
  }


  /**
    * Returns <code>true</code> iff at least one of the normalizer
    * declarations is specific to a given type.
   **/
  public boolean containsTypeSpecificNormalizer() {
    for (int i = 0; i < normalizerDeclarations.length; ++i)
      if (normalizerDeclarations[i].learner != null) return true;
    return false;
  }


  /**
    * Returns the type of the declaration.
    *
    * @return The type of the declaration.
   **/
  public Type getType() {
    return new InferenceType(head.getType(), headFinders);
  }


  /** Returns the name of the <code>InferenceDeclaration</code>. */
  public String getName() { return name.toString(); }


  /**
    * Returns the line number on which this AST node is found in the source
    * (starting from line 0).  This method exists to fulfull the
    * implementation of <code>CodeGenerator</code>.
    * @see LBJ2.CodeGenerator
   **/
  public int getLine() { return line; }


  /**
    * Returns a shallow textual representation of this AST node.  The
    * difference between the result of this method and the result of
    * <code>write(StringBuffer)</code> is that this method omits the
    * <code>subjectto</code> clause.
   **/
  public StringBuffer shallow() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("inference ");
    name.write(buffer);
    buffer.append(" head ");
    head.write(buffer);
    buffer.append(" { ");

    for (int i = 0; i < headFinders.length; ++i) {
      headFinders[i].write(buffer);
      buffer.append(" ");
    }

    for (int i = 0; i < normalizerDeclarations.length; ++i) {
      normalizerDeclarations[i].write(buffer);
      buffer.append(" ");
    }

    if (algorithm != null) {
      buffer.append(" with ");
      algorithm.write(buffer);
    }

    buffer.append(" }");
    return buffer;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    int total = headFinders.length + normalizerDeclarations.length + 3;
    if (algorithm != null) ++total;

    ASTNodeIterator I = new ASTNodeIterator(total);
    I.children[0] = head;

    for (int i = 0; i < headFinders.length; ++i)
      I.children[i + 1] = headFinders[i];
    for (int i = 0; i < normalizerDeclarations.length; ++i)
      I.children[i + 1 + headFinders.length] = normalizerDeclarations[i];

    I.children[headFinders.length + normalizerDeclarations.length + 1] =
      constraint;
    if (algorithm != null)
      I.children[headFinders.length + normalizerDeclarations.length + 2] =
        algorithm;

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
      new InferenceDeclaration(
          comment, -1, -1, (Name) name.clone(),
          (Argument) head.clone(),
          (HeadFinder[]) headFinders.clone(),
          (NormalizerDeclaration[]) normalizerDeclarations.clone(),
          (ConstraintDeclaration) constraint.clone(),
          algorithm == null ? null
                            : (InstanceCreationExpression) algorithm.clone());
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
    buffer.append("inference ");
    name.write(buffer);
    buffer.append(" head ");
    head.write(buffer);
    buffer.append(" { ");

    for (int i = 0; i < headFinders.length; ++i) {
      headFinders[i].write(buffer);
      buffer.append(" ");
    }

    for (int i = 0; i < normalizerDeclarations.length; ++i) {
      normalizerDeclarations[i].write(buffer);
      buffer.append(" ");
    }

    buffer.append("subjectto ");
    constraint.body.write(buffer);

    if (algorithm != null) {
      buffer.append(" with ");
      algorithm.write(buffer);
    }

    buffer.append(" }");
  }


  /**
    * A head finder is a method that finds the head object for an inference
    * given another object.  <code>HeadFinder</code> objects are only
    * constructed by the <code>InferenceDeclaration</code> constructor and
    * only stored in <code>InferenceDeclaration</code> objects.
    *
    * @author Nick Rizzolo
   **/
  public static class HeadFinder extends ASTNode
  {
    /** (&not;&oslash;) Input specification of the head finder method. */
    public Argument argument;
    /** (&not;&oslash;) Body of the head finder method. */
    public Block body;


    /**
      * Full constructor.  Line and byte offset information are taken from the
      * argument.
      *
      * @param a  The argument to the head finder method.
      * @param b  The body of the head finder method.
     **/
    public HeadFinder(Argument a, Block b) {
      super(a.line, a.byteOffset);
      argument = a;
      body = b;
    }


    /**
      * Returns an iterator used to successively access the children of this
      * node.
      *
      * @return An iterator used to successively access the children of this
      *         node.
     **/
    public ASTNodeIterator iterator() {
      ASTNodeIterator I = new ASTNodeIterator(2);
      I.children[0] = argument;
      I.children[1] = body;
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
        new HeadFinder((Argument) argument.clone(), (Block) body.clone());
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
      argument.write(buffer);
      buffer.append(" ");
      body.write(buffer);
    }
  }


  /**
    * A normalizer declaration is a clause of an inference declaration that
    * specifies a normalizer to be used in association with a particular
    * learning classifier or in general.  <code>NormalizerDeclaration</code>
    * objects are only constructed by the <code>InferenceDeclaration</code>
    * constructor and only stored in <code>InferenceDeclaration</code>
    * objects.
    *
    * @author Nick Rizzolo
   **/
  public static class NormalizerDeclaration extends ASTNode
  {
    /** (&oslash;) The name of the learner to be normalized. */
    public Name learner;
    /** (&not;&oslash;) Constructs the normalizer to use. */
    public InstanceCreationExpression normalizer;


    /**
      * Full constructor.
      *
      * @param line       The line on which the source code represented by
      *                   this node is found.
      * @param byteOffset The byte offset from the beginning of the source
      *                   file at which the source code represented by this
      *                   node is found.
      * @param l          The name of the learner.
      * @param n          Constructs the normalizer.
     **/
    public NormalizerDeclaration(int line, int byteOffset, Name l,
                                 InstanceCreationExpression n) {
      super(line, byteOffset);
      learner = l;
      normalizer = n;
    }

    /**
      * Parser's constructor.  Line and byte offset information are taken from
      * the token.
      *
      * @param t  The token containing line and byte offset information.
      * @param l  The name of the learner.
      * @param n  Constructs the normalizer.
     **/
    public NormalizerDeclaration(TokenValue t, Name l,
                                 InstanceCreationExpression n) {
      super(t.line, t.byteOffset);
      learner = l;
      normalizer = n;
    }


    /**
      * Returns an iterator used to successively access the children of this
      * node.
      *
      * @return An iterator used to successively access the children of this
      *         node.
     **/
    public ASTNodeIterator iterator() {
      ASTNodeIterator I = new ASTNodeIterator(learner == null ? 1 : 2);
      if (learner != null) I.children[0] = learner;
      I.children[I.children.length - 1] = normalizer;
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
        new NormalizerDeclaration(
            -1, -1, learner == null ? null : (Name) learner.clone(),
            (InstanceCreationExpression) normalizer.clone());
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
      if (learner != null) {
        learner.write(buffer);
        buffer.append(" ");
      }

      buffer.append("normalizedby ");
      normalizer.write(buffer);
    }
  }


  /**
    * An intermediate class used during parsing to represent the various
    * clauses of an inference declaration.
    *
    * @author Nick Rizzolo
   **/
  public static class Clause
  {
    /** Value of the <code>type</code> variable. */
    public static final int HEAD_FINDER = 0;
    /** Value of the <code>type</code> variable. */
    public static final int SUBJECTTO = 1;
    /** Value of the <code>type</code> variable. */
    public static final int WITH = 2;
    /** Value of the <code>type</code> variable. */
    public static final int NORMALIZER_DECLARATION = 3;
    /** String representations of the type names. */
    public static final String[] typeNames =
      new String[]{ "", "subjectto", "with", "" };


    /** The type of the clause. */
    public int type;
    /** The argument of the clause. */
    public ASTNode argument;


    /**
      * Full constructor.
      *
      * @param t  The type.
      * @param a  The argument node.
    **/
    public Clause(int t, ASTNode a) {
      type = t;
      argument = a;
    }


    /**
      * Creates a new object with the same primitive data, and recursively
      * creates new member data objects as well.
      *
      * @return The clone node.
    **/
    public Object clone() {
      return new Clause(type, (ASTNode) argument.clone());
    }


    /**
      * Debugging utility method.
      *
      * @return A textual representation of this expression.
    **/
    public String toString() {
      if (type == HEAD_FINDER || type == NORMALIZER_DECLARATION)
        return argument.toString();
      return typeNames[type] + " " + argument;
    }
  }
}

