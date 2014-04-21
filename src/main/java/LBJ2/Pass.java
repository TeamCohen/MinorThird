package LBJ2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import LBJ2.IR.*;


/**
  * Abstract class from which all of LBJ2's analysis and optimization passes
  * are derived.
  *
  * @author Nick Rizzolo
 **/
public abstract class Pass
{
  /**
    * This flag gets set if an error occurs that should cause the LBJ2
    * compiler to stop executing after this pass finishes.
   **/
  public static boolean fatalError = false;
  /**
    * Errors and warnings are collected here so they can be printed in order
    * later.
   **/
  private static HashMap errorsAndWarnings = new HashMap();
  /**
    * Errors of the form "Cannot locate class ..." are only reported once;
    * this set remembers which have already been reported.
   **/
  private static HashSet missingClassErrors = new HashSet();
  /**
    * A global flag controlling whether or not errors and warnings can
    * currently be added.  When this flag is false, the
    * {@link #reportError(int,String)} and {@link #reportWarning(int,String)}
    * methods become no-ops.
   **/
  public static boolean canAddErrorsAndWarnings = true;


  /**
    * This method prints the given error message and sets the
    * <code>fatalError</code> variable.
    *
    * @param line     The line on which the error was recognized.
    * @param message  The error message.
   **/
  public static void reportError(int line, String message) {
    if (!canAddErrorsAndWarnings) return;
    if (message.startsWith("Cannot locate class")) {
      int start = message.indexOf('\'') + 1;
      int end = message.lastIndexOf('\'');
      String missingClass = message.substring(start, end);
      if (missingClassErrors.contains(missingClass)) return;
      missingClassErrors.add(missingClass);
    }

    String error = "Error on line " + (line + 1) + ":\n";

    String[] words = message.split("\\s+");
    for (int i = 0; i < words.length; ) {
      String s = " ";
      if (words[i].length() + 2 > 78) s += " " + words[i++];
      else
        for (; i < words.length && s.length() + words[i].length() <= 78; ++i)
          s += " " + words[i];
      error += s + "\n";
    }

    addErrorOrWarning(new Integer(line), error);
    fatalError = true;
  }


  /**
    * This method simply prints the given warning message.
    *
    * @param line     The line on which the warning was recognized.
    * @param message  The warning message.
   **/
  public static void reportWarning(int line, String message) {
    if (!canAddErrorsAndWarnings || Main.clean || Main.warningsDisabled)
      return;

    String warning = "Warning on line " + (line + 1) + ":\n";

    String[] words = message.split("\\s+");
    for (int i = 0; i < words.length; ) {
      String s = " ";
      if (words[i].length() + 2 > 78) s += " " + words[i++];
      else
        for (; i < words.length && s.length() + words[i].length() < 78; ++i)
          s += " " + words[i];
      warning += s + "\n";
    }

    addErrorOrWarning(new Integer(line), warning);
  }


  /** Prints the errors and warnings to STDERR sorted by line. */
  public static void printErrorsAndWarnings() {
    Map.Entry[] entries =
      (Map.Entry[]) errorsAndWarnings.entrySet().toArray(new Map.Entry[0]);
    Arrays.sort(entries,
                new Comparator() {
                  public int compare(Object o1, Object o2) {
                    Map.Entry e1 = (Map.Entry) o1;
                    Map.Entry e2 = (Map.Entry) o2;
                    return ((Integer) e1.getKey()).compareTo((Integer) e2.getKey());
                  }
                });

    for (int i = 0; i < entries.length; ++i)
      for (Iterator I = ((LinkedList) entries[i].getValue()).iterator();
           I.hasNext(); )
        System.err.print(I.next());

    errorsAndWarnings.clear();
  }


  /**
    * Adds a new entry into the {@link #errorsAndWarnings} multi-map.
    *
    * @param key    The key.
    * @param value  The value to be associated with <code>key</code>, which
    *               will be added to a list of values associated with
    *               <code>key</code>.
   **/
  private static void addErrorOrWarning(Object key, Object value) {
    LinkedList values = (LinkedList) errorsAndWarnings.get(key);
    if (values == null)
      errorsAndWarnings.put(key, values = new LinkedList());
    values.add(value);
  }


  /**
    * A reference to the root node of the AST over which this pass will
    * operate.
   **/
  protected ASTNode root;
  /**
    * Stores the same thing as <code>root</code>, but this variable is
    * declared as <code>AST</code>.
   **/
  protected AST ast;


  /** Default constructor. */
  public Pass() { root = ast = null; }

  /**
    * Initializing constructor.  This constructor initializes
    * <code>root</code>.
    *
    * @param r  The reference with which <code>root</code> will be
    *           initialized.
   **/
  public Pass(ASTNode r) { setRoot(r); }


  /**
    * Sets the <code>root</code> member variable.
    *
    * @param r  The reference with which <code>root</code> will be set.
   **/
  public void setRoot(ASTNode r) {
    root = r;
    if (r instanceof AST) ast = (AST) r;
    else ast = null;
  }


  /**
    * The main interface: call this method to apply the pass to the AST.  This
    * method simply calls the recursive helper method.  It uses the Visitor
    * pattern to ensure that the correct recursive helper method is called.
    * That way, the user can define a <code>run()</code> method with any
    * <code>ASTNode</code> argument type in a class that extends
    * <code>Pass</code>, and those methods will be called at the appropriate
    * times during the traversal of the AST.
   **/
  public void run() { root.runPass(this); }


  /**
    * This method supports derived passes that continue to descend down the
    * AST after operating on a particular type of node.
    *
    * @param node The node on whose children the pass should be run.
   **/
  public void runOnChildren(ASTNode node) {
    for (ASTNodeIterator I = node.iterator(); I.hasNext(); ) {
      ASTNode n = I.next();
      if (n != null) n.runPass(this);
    }
  }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(AST node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(PackageDeclaration node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ImportDeclaration node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(BinaryExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InstanceCreationExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ParameterSet node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InstanceofExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ArrayCreationExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ArrayInitializer node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Conditional node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(LearningClassifierExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(CastExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(IncrementExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Assignment node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Constant node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(UnaryExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Name node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(FieldAccess node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SubscriptVariable node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Argument node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Operator node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(NameList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstantList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(StatementList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ImportList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(DeclarationList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ExpressionList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstraintType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InferenceType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(NormalizerType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ReferenceType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ArrayType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(PrimitiveType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierReturnType node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierExpressionList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierAssignment node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierName node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ClassifierCastExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Conjunction node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(CodedClassifier node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(CompositeGenerator node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InferenceInvocation node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(VariableDeclaration node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(EmptyStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(LabeledStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(IfStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchBlock node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchGroupList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchGroup node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchLabelList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SwitchLabel node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(DoStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(WhileStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ForStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ExpressionStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ContinueStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ReturnStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SenseStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ThrowStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(SynchronizedStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(TryStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(CatchList node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(Block node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(CatchClause node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(AssertStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(BreakStatement node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(MethodInvocation node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(AtLeastQuantifierExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(AtMostQuantifierExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(BinaryConstraintExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstraintDeclaration node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstraintEqualityExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstraintInvocation node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ConstraintStatementExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(ExistentialQuantifierExpression node) {
    runOnChildren(node);
  }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InferenceDeclaration node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InferenceDeclaration.HeadFinder node) {
    runOnChildren(node);
  }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(InferenceDeclaration.NormalizerDeclaration node) {
    runOnChildren(node);
  }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(NegatedConstraintExpression node) { runOnChildren(node); }


  /**
    * One of the recursive "helper" methods for <code>run()</code>.  Simply in
    * charge of delegating its work to the children of the node passed to it.
    * Derived <code>Pass</code>es will override this method when there is
    * something useful to be done for the given node in that pass.  If there
    * isn't, it won't be overriden, and execution will continue to traverse
    * the AST.
    *
    * @param node A reference to the node currently being processed.
   **/
  public void run(UniversalQuantifierExpression node) { runOnChildren(node); }
}

