package LBJ2.infer;

import java.util.*;
import LBJ2.classify.*;
import LBJ2.jni.GLPKHook;


/**
  * Uses the GNU Linear Programming Kit library to perform Integer Linear
  * Programming over the variables, maximizing the sum of all
  * learner-object-value triples selected while respecting the constraints.
  * This code implements the most straight-forward algorithm for translating
  * FOL constraints to linear inequalities.  First, the FOL constraints are
  * propositionalized and conjuncted together to arrive at a single
  * propositional expression representing all the constraints.  Then, each
  * type of propositional subexpression recursively translates its children,
  * replacing each with an unnegated variable before translating itself into
  * two (or one, in the case of negation) linear inequalities.
  *
  * <p> This class assumes that the <code>constraint</code> variable inherited
  * from class <code>Inference</code> is of type
  * <code>FirstOrderConstraint</code>.
  *
  * @author Nick Rizzolo
  * @deprecated As of LBJ release 2.0.12, it is preferrable to pass a
  *             {@link LBJ2.jni.GLPKHook} object to the
  *             {@link LBJ2.infer.ILPInference} constructor.
 **/
public class NaiveGLPK extends GLPK
{
  /** Debugging variable. */
  private static final int PRINT_ILP = ILPInference.VERBOSITY_NONE;


  /** Default constructor. */
  public NaiveGLPK() { this(false); }

  /**
    * Initializing constructor.
    *
    * @param g  Whether or not to generate cuts.
   **/
  public NaiveGLPK(boolean g) { super(g); }

  /**
    * Initializing constructor.
    *
    * @param g  Whether or not to generate cuts.
    * @param w  Whether or not to write debug files when problems arise.
   **/
  public NaiveGLPK(boolean g, boolean w) { super(g, w); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
   **/
  public NaiveGLPK(Object h) { this(h, false); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
    * @param g  Whether or not to generate cuts.
   **/
  public NaiveGLPK(Object h, boolean g) { super(h, g); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
    * @param g  Whether or not to generate cuts.
    * @param w  Whether or not to write debug files when problems arise.
   **/
  public NaiveGLPK(Object h, boolean g, boolean w) { super(h, g, w); }


  /**
    * Uses the <code>lpx_intopt(LPX*)</code> C routine from the GLPK library
    * to solve the ILP proglem if it hasn't already been solved.
   **/
  protected void infer() throws Exception {
    if (solver != null) return;

    constraint.consolidateVariables(variables);
    indexMap = new HashMap();

    if (writeStatusFiles)
      solver =
        new GLPKHook("NaiveGLPKInference" + ID, generateCuts, PRINT_ILP);
    else solver = new GLPKHook(generateCuts, PRINT_ILP);
    solver.setMaximize(true);

    for (Iterator I = variables.values().iterator(); I.hasNext(); ) {
      FirstOrderVariable v = (FirstOrderVariable) I.next();
      ScoreSet ss = getNormalizer(v.getClassifier()).normalize(v.getScores());
      Score[] scores = null;
      if (ss != null) scores = ss.toArray();

      if (scores == null || scores.length == 0) {
        System.err.println(
            "LBJ ERROR: Classifier " + v.getClassifier()
            + " did not return any scores.  GLPK Inference cannot be "
            + "performed.");
        System.exit(1);
      }

      int[] indexes = new int[scores.length];
      double[] coefficients = new double[scores.length];
      Arrays.fill(coefficients, 1);

      for (int j = 0; j < scores.length; ++j) {
        indexes[j] = solver.addBooleanVariable(scores[j].score);
        indexMap.put(
            new PropositionalVariable(v.getClassifier(), v.getExample(),
                                      scores[j].value),
            new Integer(indexes[j]));

        if (PRINT_ILP == ILPInference.VERBOSITY_HIGH) {
          System.out.println(
              indexes[j] + "(" + scores[j].score + "): " + v.getClassifier()
              + "(" + v.getExample() + ") == " + scores[j].value);
        }
      }

      solver.addEqualityConstraint(indexes, coefficients, 1);
    }

    PropositionalConstraint propositional =
      ((FirstOrderConstraint) constraint).propositionalize();
    propositional = propositional.simplify();
    propositional.runVisit(this);
    solver.addEqualityConstraint(
        new int[]{ returnIndex }, new double[]{ 1 }, 1);

    if (PRINT_ILP == ILPInference.VERBOSITY_HIGH) {
      StringBuffer buffer = new StringBuffer();
      solver.write(buffer);
      System.out.println(buffer);
    }

    if (!solver.solve()) throw new InferenceNotOptimalException(solver, head);
    int variableIndex = 0;

    for (Iterator I = variables.values().iterator(); I.hasNext(); ) {
      FirstOrderVariable v = (FirstOrderVariable) I.next();
      Score[] scores = v.getScores().toArray();
      for (int j = 0; j < scores.length; ++j, ++variableIndex)
        if (solver.getBooleanValue(variableIndex))
          v.setValue(scores[j].value);
    }
  }


  /**
    * Two <code>Inference</code> objects are equal when they have the same
    * run-time type and store the same head object.  I.e., the <code>==</code>
    * operator must return <code>true</code> when comparing the two head
    * objects for this method to return <code>true</code>.
    *
    * @param o  The object to compare to this object.
    * @return <code>true</code> iff this object equals the argument object as
    *         defined above.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof NaiveGLPK)) return false;
    return head == ((NaiveGLPK) o).head;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDoubleImplication c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    visit(
        new PropositionalConjunction(
          new PropositionalImplication(children[0], children[1]),
          new PropositionalImplication(children[1], children[0])));
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalImplication c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    visit(
        new PropositionalDisjunction(
          new PropositionalNegation(children[0]), children[1]));
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalConjunction c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();

    int[] indexes = new int[children.length + 1];
    double[] coefficients = new double[children.length + 1];

    for (int i = 0; i < children.length; ++i) {
      children[i].runVisit(this);
      indexes[i] = returnIndex;
      coefficients[i] = 1;
    }

    indexes[children.length] = Integer.MAX_VALUE;
    Arrays.sort(indexes);
    String key = "" + indexes[0];
    for (int i = 1; i < children.length; ++i) key += "&" + indexes[i];
    Integer I = (Integer) indexMap.get(key);

    if (I == null) {
      I = new Integer(createNewVariable(key));
      indexMap.put(key, I);

      indexes[children.length] = I.intValue();
      coefficients[children.length] = -1;
      solver.addLessThanConstraint(indexes, coefficients,
                                   children.length - 1);

      coefficients = new double[]{ 1, -1 };
      for (int i = 0; i < children.length; ++i)
        solver.addGreaterThanConstraint(
            new int[]{ indexes[i], indexes[children.length] },
            coefficients, 0);
    }

    returnIndex = I.intValue();
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDisjunction c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();

    int[] indexes = new int[children.length + 1];
    double[] coefficients = new double[children.length + 1];

    for (int i = 0; i < children.length; ++i) {
      children[i].runVisit(this);
      indexes[i] = returnIndex;
      coefficients[i] = 1;
    }

    indexes[children.length] = Integer.MAX_VALUE;
    Arrays.sort(indexes);
    String key = "" + indexes[0];
    for (int i = 1; i < children.length; ++i) key += "|" + indexes[i];
    Integer I = (Integer) indexMap.get(key);

    if (I == null) {
      I = new Integer(createNewVariable(key));
      indexMap.put(key, I);

      indexes[children.length] = I.intValue();
      coefficients[children.length] = -1;
      solver.addGreaterThanConstraint(indexes, coefficients, 0);

      coefficients = new double[]{ 1, -1 };
      for (int i = 0; i < children.length; ++i)
        solver.addLessThanConstraint(
            new int[]{ indexes[i], indexes[children.length] },
            coefficients, 0);
    }

    returnIndex = I.intValue();
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalAtLeast c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();

    int[] indexes = new int[children.length + 1];
    double[] coefficients = new double[children.length + 1];

    for (int i = 0; i < children.length; ++i) {
      children[i].runVisit(this);
      indexes[i] = returnIndex;
      coefficients[i] = 1;
    }

    indexes[children.length] = Integer.MAX_VALUE;
    Arrays.sort(indexes);
    String key = "atl" + c.getM() + "of" + indexes[0];
    for (int i = 1; i < children.length; ++i)
      key += "&" + indexes[i];
    Integer I = (Integer) indexMap.get(key);

    if (I == null) {
      I = new Integer(createNewVariable(key));
      indexMap.put(key, I);

      indexes[children.length] = I.intValue();
      coefficients[children.length] = -c.getM();
      solver.addGreaterThanConstraint(indexes, coefficients, 0);

      coefficients[children.length] = -children.length;
      solver.addLessThanConstraint(indexes, coefficients, c.getM() - 1);
    }

    returnIndex = I.intValue();
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalNegation c) {
    PropositionalConstraint[] children =
      (PropositionalConstraint[]) c.getChildren();
    children[0].runVisit(this);

    int index = returnIndex;
    String key = "!" + index;
    Integer I = (Integer) indexMap.get(key);

    if (I == null) {
      I = new Integer(createNewVariable(key));
      indexMap.put(key, I);

      int[] indexes = new int[]{ index, I.intValue() };
      double[] coefficients = new double[]{ 1, 1 };
      solver.addEqualityConstraint(indexes, coefficients, 1);
    }

    returnIndex = I.intValue();
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalVariable c) {
    returnIndex = ((Integer) indexMap.get(c)).intValue();
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalConstant c) {
    assert false : "NaiveGLPK: Constraint contains a constant.";
  }
}

