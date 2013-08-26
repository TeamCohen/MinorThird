package LBJ2;

import LBJ2.IR.*;


/**
  * The <code>PrintAST</code> pass simply prints a text representation of the
  * parsed AST to standard output.
  *
  * @author Nick Rizzolo
 **/
public class PrintAST extends Pass
{
  /** The current amount of indentation to print. */
  private int indent;


  /**
    * Instantiates a pass that runs on an entire <code>AST</code>.
    *
    * @param ast  The program to run this pass on.
   **/
  public PrintAST(AST ast) { super(ast); }


  /**
    * Prints the given text preceded by the amount of indentation called for
    * in the <code>indent</code> member variable and followed by a new line.
    *
    * @param text The text to print.
   **/
  public void indentedPrintln(String text) {
    for (int i = 0; i < indent; ++i) System.out.print("  ");
    System.out.println(text);
  }


  /**
    * Prints the given text preceeded by the amount of indentation called for
    * in the <code>indent</code> member variable and followed by the line
    * number and byte offset information for the specified
    * <code>ASTNode</code> and a new line.
    *
    * @param text The text to print.
    * @param node The node for which to print line and byte offset
    *             information.
   **/
  public void indentedPrintln(String text, ASTNode node) {
    for (int i = 0; i < indent; ++i) System.out.print("  ");
    System.out.println(text + " (" + (node.line + 1) + ", " + node.byteOffset
                       + ")");
  }


  /**
    * The default routine for printing a non-terminal AST node is to first
    * print the name of the AST node's class with line and byte offset
    * information, and then recursively print its children at indentation
    * level one higher.
    *
    * @param text The text to print.
    * @param node The node for which to print line and byte offset
    *             information.
   **/
  public void nonTerminal(String text, ASTNode node) {
    indentedPrintln(text, node);
    ++indent;
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(AST node) {
    indent = 0;
    nonTerminal("AST", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(PackageDeclaration node) {
    nonTerminal("PackageDeclaration", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ImportDeclaration node) {
    nonTerminal("ImportDeclaration", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(Name n) {
    indentedPrintln("Name", n);
    ++indent;
    indentedPrintln("name: " + n);
    indentedPrintln("dimensions: " + n.dimensions);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(BinaryExpression node) {
    nonTerminal("BinaryExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InstanceCreationExpression node) {
    nonTerminal("InstanceCreationExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InstanceofExpression node) {
    nonTerminal("InstanceofExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ArrayCreationExpression node) {
    indentedPrintln("ArrayCreationExpression", node);
    ++indent;
    indentedPrintln("dimensions: " + node.dimensions);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ArrayInitializer node) {
    nonTerminal("ArrayInitializer", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Conditional node) { nonTerminal("Conditional", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(LearningClassifierExpression node) {
    nonTerminal("LearningClassifierExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(CastExpression node) {
    nonTerminal("CastExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(IncrementExpression node) {
    nonTerminal("IncrementExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Assignment node) { nonTerminal("Assignment", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Constant node) {
    indentedPrintln("Constant", node);
    ++indent;
    indentedPrintln("value: " + node.value);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(UnaryExpression node) {
    nonTerminal("UnaryExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ParameterSet node) {
    nonTerminal("ParameterSet", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(FieldAccess node) {
    indentedPrintln("FieldAccess", node);
    ++indent;
    indentedPrintln("name: " + node.name);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SubscriptVariable node) {
    nonTerminal("SubscriptVariable", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Argument node) {
    indentedPrintln("Argument", node);
    ++indent;
    if (node.getFinal()) indentedPrintln("final");
    indentedPrintln("name: " + node.getName());
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Operator node) { indentedPrintln(node.toString(), node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(NameList node) { nonTerminal("NameList", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ConstantList node) { nonTerminal("ConstantList", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(StatementList node) { nonTerminal("StatementList", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ExpressionList node) {
    nonTerminal("ExpressionList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierType node) {
    nonTerminal("ClassifierType", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ReferenceType node) { nonTerminal("ReferenceType", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ArrayType node) { nonTerminal("ArrayType", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(PrimitiveType node) {
    indentedPrintln("PrimitiveType", node);
    ++indent;
    indentedPrintln("type: " + node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierReturnType node) {
    indentedPrintln("ClassifierReturnType", node);
    ++indent;
    indentedPrintln("type: " + node.getTypeName());
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierAssignment node) {
    indentedPrintln("ClassifierAssignment", node);
    ++indent;
    indentedPrintln("comment: " + node.comment);
    indentedPrintln("name: " + node.name);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(VariableDeclaration node) {
    indentedPrintln("VariableDeclaration", node);
    ++indent;
    if (node.isFinal) indentedPrintln("final");
    node.type.runPass(this);
    node.names.runPass(this);

    indentedPrintln("ExpressionList", node.initializers);
    ++indent;
    ExpressionList.ExpressionListIterator I =
      node.initializers.listIterator();
    while (I.hasNext()) {
      Expression i = I.nextItem();
      if (i == null) indentedPrintln("null");
      else i.runPass(this);
    }

    indent -= 2;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(EmptyStatement node) {
    nonTerminal("EmptyStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(LabeledStatement node) {
    indentedPrintln("LabeledStatement", node);
    ++indent;
    indentedPrintln("label: " + node.label);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(IfStatement node) { nonTerminal("IfStatement", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchStatement node) {
    nonTerminal("SwitchStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchBlock node) { nonTerminal("SwitchBlock", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchGroupList node) {
    nonTerminal("SwitchGroupList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchGroup node) { nonTerminal("SwitchGroup", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchLabelList node) {
    nonTerminal("SwitchLabelList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SwitchLabel node) { nonTerminal("SwitchLabel", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(DoStatement node) { nonTerminal("DoStatement", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(WhileStatement node) {
    nonTerminal("WhileStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ForStatement node) { nonTerminal("ForStatement", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ExpressionStatement node) {
    nonTerminal("ExpressionStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ContinueStatement node) {
    indentedPrintln("ContinueStatement", node);
    ++indent;
    indentedPrintln("label: " + node.label);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ReturnStatement node) {
    nonTerminal("ReturnStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SenseStatement node) {
    nonTerminal("SenseStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ThrowStatement node) {
    nonTerminal("ThrowStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(SynchronizedStatement node) {
    nonTerminal("SynchronizedStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(TryStatement node) { nonTerminal("TryStatement", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(CatchList node) { nonTerminal("CatchList", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Block node) { nonTerminal("Block", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(CatchClause node) { nonTerminal("CatchClause", node); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(AssertStatement node) {
    nonTerminal("AssertStatement", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(BreakStatement node) {
    indentedPrintln("BreakStatement", node);
    ++indent;
    indentedPrintln("label: " + node.label);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(MethodInvocation node) {
    nonTerminal("MethodInvocation", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(DeclarationList node) {
    nonTerminal("DeclarationList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierCastExpression node) {
    nonTerminal("ClassifierCastExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierExpressionList node) {
    nonTerminal("ClassifierExpressionList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ClassifierName node) {
    nonTerminal("ClassifierName", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(CodedClassifier node) {
    nonTerminal("CodedClassifier", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(CompositeGenerator node) {
    nonTerminal("CompositeGenerator", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(Conjunction node) {
    nonTerminal("Conjunction", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ImportList node) {
    nonTerminal("ImportList", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(AtLeastQuantifierExpression node) {
    nonTerminal("AtLeastQuantifierExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(AtMostQuantifierExpression node) {
    nonTerminal("AtMostQuantifierExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(BinaryConstraintExpression node) {
    nonTerminal("BinaryConstraintExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ConstraintDeclaration node) {
    indentedPrintln("ConstraintDeclaration", node);
    ++indent;
    indentedPrintln("comment: " + node.comment);
    indentedPrintln("name: " + node.name);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ConstraintEqualityExpression node) {
    nonTerminal("ConstraintEqualityExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ConstraintInvocation node) {
    nonTerminal("ConstraintInvcation", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ConstraintStatementExpression node) {
    nonTerminal("ConstraintStatementExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(ExistentialQuantifierExpression node) {
    nonTerminal("ExistentialQuantifierExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InferenceDeclaration node) {
    indentedPrintln("InferenceDeclaration", node);
    ++indent;
    indentedPrintln("comment: " + node.comment);
    indentedPrintln("name: " + node.name);
    runOnChildren(node);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InferenceDeclaration.HeadFinder node) {
    nonTerminal("InferenceDeclaration.HeadFinder", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InferenceDeclaration.NormalizerDeclaration node) {
    nonTerminal("InferenceDeclaration.NormalizerDeclaration", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(InferenceInvocation node) {
    nonTerminal("InferenceInvocation", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(NegatedConstraintExpression node) {
    nonTerminal("NegatedConstraintExpression", node);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param node The node to process.
   **/
  public void run(UniversalQuantifierExpression node) {
    nonTerminal("UniversalQuantifierExpression", node);
  }
}

