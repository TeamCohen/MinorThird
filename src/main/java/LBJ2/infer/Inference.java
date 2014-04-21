package LBJ2.infer;

import java.util.LinkedHashMap;

import LBJ2.learn.IdentityNormalizer;
import LBJ2.learn.Learner;
import LBJ2.learn.Normalizer;


/**
  * An object of this class keeps track of all the information necessary to
  * perform inference.  Once that inference has been performed, constrained
  * classifiers access the results through this class's interface to determine
  * what their constrained predictions are.
  *
  * @author Nick Rizzolo
 **/
public abstract class Inference
{
  /**
    * Produces a string representation of an example object.  This method is
    * used mainly for debugging messages.
    *
    * @param example  The example object, which may be an array.
    * @return A string representation of <code>example</code>.
   **/
  public static String exampleToString(Object example) {
    String result = "";

    if (example instanceof Object[]) {
      Object[] array = (Object[]) example;
      result += "[";
      if (array.length > 0) result += array[0];
      for (int k = 1; k < array.length; ++k) result += ", " + array[k];
      result += "]";
    }
    else result += example;

    return result;
  }


  /**
    * The values of this map are the variables we perform inference over; they
    * are the actual <code>FirstOrderVariable</code> objects found in this
    * inference's constraints.  The keys are also objects of type
    * <code>FirstOrderVariable</code>, but they are not necessarily the actual
    * objects found in the constraints.  This map is populated by the first
    * evaluation of the constraints.
   **/
  protected LinkedHashMap variables;
  /** The constraints which must be satisfied by the inference algorithm. */
  protected Constraint constraint;
  /** Objects of this class are differentiated by their "head" objects. */
  protected Object head;


  /** Default constructor. */
  public Inference() { this(null); }

  /**
    * Initializes the head object.
    *
    * @param h  The head object.
   **/
  public Inference(Object h) {
    head = h;
    variables = new LinkedHashMap();
  }


  /** Retrieves the head object. */
  public Object getHead() { return head; }


  /**
    * Derived classes implement this method to perform the inference, setting
    * the values of the variables such that they maximize the objective
    * function while satisfying the constraints.  When implementing this
    * method in a derived class <code>Foo</code>, it may be assumed that the
    * <code>constraint</code> member field has already been filled in
    * appropriately, since the LBJ compiler will generate a class extending
    * <code>Foo</code> whose constructor does so.
   **/
  abstract protected void infer() throws Exception;


  /**
    * Retrieves the value of the specified variable as identified by the
    * classifier and the object that produce that variable.
    *
    * @param c  The classifier producing the variable.
    * @param o  The object from which the variable is produced.
    * @return The current value of the requested variable.
   **/
  abstract public String valueOf(Learner c, Object o) throws Exception;


  /**
    * Returns the normalization function associated with the given classifier
    * in this inference.  Derived classes that implement an inference
    * algorithm for use in an LBJ source file are required to call this method
    * to normalize the scores produced by classifiers before making use of
    * those scores.  By default, this method returns the
    * <code>IdentityNormalizer</code>.
    *
    * @param c  The classifier.
    * @return The normalization function associated with the classifier.
   **/
  public Normalizer getNormalizer(Learner c) {
    return new IdentityNormalizer();
  }


  /**
    * Returns the fully qualified name of the type of the head object for this
    * inference.  By default, this method returns
    * <code>"java.lang.Object"</code>.  It should be overridden by derived
    * classes.
   **/
  public String getHeadType() { return "java.lang.Object"; }


  /**
    * Returns the fully qualified names of the types of objects for which head
    * finder methods have been defined.  This method must be overridden by
    * derived classes, since by default it returns a 0-length array and every
    * <code>Inference</code> is required to have at least one head finder.
    * <code>Inference</code> classes written by the compiler automatically
    * override this method appropriately.
   **/
  public String[] getHeadFinderTypes() { return new String[0]; }


  /**
    * Determines if the constraints are satisfied by the current variable
    * assignments.
   **/
  public boolean satisfied() { return constraint.evaluate(); }


  /**
    * Retrieves the requested variable, creating it first if it doesn't yet
    * exist.
    *
    * @param v  A variable containing the same classifier, object, and
    *           prediction value as the desired variable.
    * @return The Boolean variable corresponding to the event
    *         <i>classifier(object) == prediction</i>.
   **/
  public PropositionalVariable getVariable(PropositionalVariable v) {
    PropositionalVariable variable = (PropositionalVariable) variables.get(v);

    if (variable == null) {
      variable = (PropositionalVariable) v.clone();
      variables.put(variable, variable);
    }

    return variable;
  }


  /**
    * Retrieves the requested variable, creating it first if it doesn't yet
    * exist.
    *
    * @param v  A variable containing the same classifier and object as the
    *           desired variable.
    * @return The variable corresponding to the application of the classifier
    *         on the object.
   **/
  public FirstOrderVariable getVariable(FirstOrderVariable v) {
    FirstOrderVariable variable = (FirstOrderVariable) variables.get(v);

    if (variable == null) {
      variable = (FirstOrderVariable) v.clone();
      variables.put(variable, variable);
    }

    return variable;
  }


  /**
    * The default method for visiting a constraint simply visits that
    * constraint's children.
   **/
  public void visitAll(Constraint c) {
    Constraint[] children = c.getChildren();
    for (int i = 0; i < children.length; ++i) children[i].runVisit(this);
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderDoubleImplication c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderImplication c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderConjunction c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderDisjunction c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderEqualityTwoValues c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderEqualityWithValue c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderEqualityWithVariable c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderNegation c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(FirstOrderConstant c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(UniversalQuantifier c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(ExistentialQuantifier c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(AtLeastQuantifier c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(AtMostQuantifier c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(QuantifiedConstraintInvocation c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDoubleImplication c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalImplication c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalConjunction c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDisjunction c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalAtLeast c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalConstant c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalNegation c) { visitAll(c); }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalVariable c) { visitAll(c); }
}

