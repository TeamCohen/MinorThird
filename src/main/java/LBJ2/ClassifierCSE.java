package LBJ2;

import java.util.HashMap;

import LBJ2.IR.AST;
import LBJ2.IR.ClassifierAssignment;
import LBJ2.IR.ClassifierCastExpression;
import LBJ2.IR.ClassifierExpression;
import LBJ2.IR.ClassifierExpressionList;
import LBJ2.IR.ClassifierName;
import LBJ2.IR.CodedClassifier;
import LBJ2.IR.CompositeGenerator;
import LBJ2.IR.Conjunction;
import LBJ2.IR.ConstraintDeclaration;
import LBJ2.IR.InferenceDeclaration;
import LBJ2.IR.InferenceInvocation;
import LBJ2.IR.LearningClassifierExpression;


/**
  * This pass performs common subexpression elimination on classifier
  * expressions except for {@link ClassifierName}s and
  * {@link LearningClassifierExpression}s.
  *
  * @author Nick Rizzolo
 **/
public class ClassifierCSE extends Pass
{
  /**
    * Maps each classifier expression to the cannonical name that will
    * represent it.
   **/
  private HashMap expressionToName;


  /**
    * Instantiates a pass that runs on an entire <code>AST</code>.
    *
    * @param ast  The program to run this pass on.
   **/
  public ClassifierCSE(AST ast) { super(ast); }


  /**
    * Looks up the given expression in {@link #expressionToName}, returning
    * a new {@link ClassifierName} if there was a name associated with it or
    * <code>null</code> otherwise.  In the case that a name was not already
    * associated with the given expression, its own name is set up in
    * association with it, with the following exceptions.
    * {@link ClassifierName}s are excluded from the map since they are already
    * merely names.  {@link LearningClassifierExpression}s are also excluded,
    * since each learning classifier expression should represent a separate
    * and independent learned function even if its specification is identical
    * to some other learning classifier.
    *
    * @param ce The expression to look up.
   **/
  private ClassifierName lookup(ClassifierExpression ce) {
    ClassifierName cached = (ClassifierName) expressionToName.get(ce);

    if (cached == null) {
      if (!(ce instanceof ClassifierName)
          && !(ce instanceof ClassifierCastExpression)
          && !(ce instanceof LearningClassifierExpression)) {
        cached =
          new ClassifierName(ce.name.toString(), ce.line, ce.byteOffset);
        cached.name = cached.referent;
        cached.returnType = ce.returnType;
        cached.argument = ce.argument;
        cached.singleExampleCache = ce.singleExampleCache;
        expressionToName.put(ce, cached);
      }

      return null;
    }

    ClassifierName result = (ClassifierName) cached.clone();
    result.line = cached.line;
    result.byteOffset = cached.byteOffset;
    result.returnType = cached.returnType;
    result.argument = cached.argument;
    result.singleExampleCache = cached.singleExampleCache;
    return result;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast) {
    expressionToName = new HashMap();
    runOnChildren(ast);
    expressionToName = null;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ca The node to process.
   **/
  public void run(ClassifierAssignment ca) {
    ca.expression.runPass(this);
    ClassifierName name = lookup(ca.expression);
    if (name != null) {
      name.name = ca.expression.name;
      name.returnType = ca.expression.returnType;
      name.singleExampleCache = ca.expression.singleExampleCache;
      ca.expression = name;
      SemanticAnalysis.representationTable.put(name.name.toString(), name);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cce  The node to process.
   **/
  public void run(ClassifierCastExpression cce) {
    cce.expression.runPass(this);
    ClassifierName name = lookup(cce.expression);
    if (name != null) {
      if (cce.expression.name.toString().indexOf("$$") == -1) {
        name.name = cce.expression.name;
        name.returnType = cce.expression.returnType;
        name.singleExampleCache = cce.expression.singleExampleCache;
        SemanticAnalysis.representationTable.put(name.name.toString(), name);
      }
      else SemanticAnalysis.representationTable.remove(cce.name.toString());

      cce.expression = name;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) {
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg) {
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) {
      ClassifierExpression ce = I.nextItem();
      ce.runPass(this);
      ClassifierName name = lookup(ce);
      if (name != null) {
        SemanticAnalysis.representationTable.remove(ce.name.toString());
        I.set(name);
      }
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c) {
    c.left.runPass(this);
    ClassifierName name = lookup(c.left);
    if (name != null) {
      SemanticAnalysis.representationTable.remove(c.left.name.toString());
      c.left = name;
    }

    c.right.runPass(this);
    name = lookup(c.right);
    if (name != null) {
      SemanticAnalysis.representationTable.remove(c.right.name.toString());
      c.right = name;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceInvocation i) {
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    ClassifierName name;
    if (lce.labeler != null) {
      lce.labeler.runPass(this);
      name = lookup(lce.labeler);
      if (name != null) {
        SemanticAnalysis.representationTable
          .remove(lce.labeler.name.toString());
        lce.labeler = name;
      }
    }

    lce.extractor.runPass(this);
    name = lookup(lce.extractor);
    if (name != null) {
      SemanticAnalysis.representationTable
        .remove(lce.extractor.name.toString());
      lce.extractor = name;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(ConstraintDeclaration c) {
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceDeclaration i) {
  }
}

