package LBJ2.infer;

import java.util.*;
import LBJ2.classify.*;
import LBJ2.jni.GLPKHook;


/**
  * Uses the GNU Linear Programming Kit library to perform Integer Linear
  * Programming over the variables, maximizing the sum of all
  * learner-object-value triples selected while respecting the constraints.
  * The difference between this implementation and <code>NaiveGLPK</code> is
  * that this implementation attempts to save the generation of a few
  * constraints by directly translating a broader variety of propositional
  * logic subexpressions into as few constraints as possible.  For example,
  * negated variables never need to be converted to positive variables with
  * the addition of a new variable and constraint, like <code>NaiveGLPK</code>
  * would generate.
  *
  * <p> This class assumes that the <code>constraint</code> variable inherited
  * from class <code>Inference</code> is of type
  * <code>FirstOrderConstraint</code>.
  *
  * @author     Nick Rizzolo
  * @deprecated As of LBJ release 2.0.12, it is preferrable to pass a
  *             {@link LBJ2.jni.GLPKHook} object to the
  *             {@link LBJ2.infer.ILPInference} constructor.
 **/
public class GLPK extends Inference
{
  /** Keeps the next ID number for objects of this class. */
  protected static int nextID = 0;


  /** The JNI to the GLPK library. */
  protected GLPKHook solver;
  /** Whether or not to write debug files when problems arise. */
  protected boolean writeStatusFiles;
  /** The identification number for this object, used in debug file names. */
  protected int ID;
  /** Whether or not to generate Gomory cuts. */
  protected boolean generateCuts;
  /** Debugging variable. */
  private int PRINT_ILP;

  /**
    * Used during ILP constraint generation.  When a propositional constraint
    * finishes generating any ILP constraints that may be associated with it,
    * it sets this variable to its own index.
   **/
  protected int returnIndex;
  /**
    * Used during ILP constraint generation.  This flag is set iff the
    * variable corresponding to <code>returnIndex</code> is negated in its
    * current context.
   **/
  protected boolean returnNegation;
  /**
    * Used during ILP constraint generation.  This map associates each
    * variable index with a representation of the expression whose value is
    * represented by the variable.  The keys associated with indexes of
    * variables that were originally part of the inference problem are
    * <code>PropositionalVariable</code> objects.  The keys associated with
    * indexes of temporary variables created during constraint translation are
    * strings.
   **/
  protected HashMap indexMap;
  /**
    * Used during ILP constraint generation.  Constraints are treated
    * differently if they are part of another constraint expression than if
    * they are a term in the top level conjunction.
   **/
  protected boolean topLevel;


  /**
    * Creates an instance with the specified verbosity level.
    *
    * @param v  Setting for the {@link #PRINT_ILP} variable.
   **/
  public GLPK(int v) { this(null, v); }

  /**
    * Creates an instance with the specified verbosity level.
    *
    * @param v  Setting for the {@link #PRINT_ILP} variable.
   **/
  public GLPK(Object h, int v) {
    this(h);
    PRINT_ILP = v;
  }

  /** Default constructor. */
  public GLPK() { this(null); }

  /**
    * Initializing constructor.
    *
    * @param g  Whether or not to generate cuts.
   **/
  public GLPK(boolean g) { this(null, g); }

  /**
    * Initializing constructor.
    *
    * @param g  Whether or not to generate cuts.
    * @param w  Whether or not to write debug files when problems arise.
   **/
  public GLPK(boolean g, boolean w) { this(null, g, w); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
   **/
  public GLPK(Object h) { this(h, true); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
    * @param g  Whether or not to generate cuts.
   **/
  public GLPK(Object h, boolean g) { this(h, g, false); }

  /**
    * Initializing constructor.
    *
    * @param h  The head object.
    * @param g  Whether or not to generate cuts.
    * @param w  Whether or not to write debug files when problems arise.
   **/
  public GLPK(Object h, boolean g, boolean w) {
    super(h);
    ID = nextID++;
    generateCuts = g;
    writeStatusFiles = w;

    if (!LBJ2.Configuration.GLPKLinked) {
      System.err.println(
          "LBJ ERROR: LBJ has not been configured correctly to invoke GLPK "
          + "inference.  Install GLPK 4.14 or better and re-configure.");
      System.exit(1);
    }
  }


  /**
    * Adds a constraint to the inference.
    *
    * @param c  The constraint to add.
   **/
  public void addConstraint(FirstOrderConstraint c) {
    solver = null;
    if (constraint == null) constraint = c;
    else
      constraint =
        new FirstOrderConjunction((FirstOrderConstraint) constraint, c);
  }


  /**
    * Uses the <code>lpx_intopt(LPX*)</code> C routine from the GLPK library
    * to solve the ILP proglem if it hasn't already been solved.
   **/
  protected void infer() throws Exception {
    if (solver != null) return;

    constraint.consolidateVariables(variables);
    indexMap = new HashMap();

    if (writeStatusFiles)
      solver = new GLPKHook("GLPKInference" + ID, generateCuts, PRINT_ILP);
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

        if (PRINT_ILP == ILPInference.VERBOSITY_HIGH)
          System.out.println(
              indexes[j] + "(" + scores[j].score + "): " + v.getClassifier()
              + "(" + v.getExample() + ") == " + scores[j].value);
      }

      solver.addEqualityConstraint(indexes, coefficients, 1);
    }

    PropositionalConstraint propositional =
      ((FirstOrderConstraint) constraint).propositionalize();

    if (propositional instanceof PropositionalConjunction)
      propositional =
        ((PropositionalConjunction) propositional).simplify(true);
    else propositional = propositional.simplify();

    if (propositional instanceof PropositionalConstant
        && !propositional.evaluate()) {
      System.err.println("GLPK ERROR: Unsatisfiable constraints!");
      solver.addEqualityConstraint(new int[]{ 0 }, new double[]{ 1 }, 2);
    }

    topLevel = true;
    propositional.runVisit(this);

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
    * Retrieves the value of the specified variable as identified by the
    * classifier and the object that produce that variable.
    *
    * @param c  The classifier producing the variable.
    * @param o  The object from which the variable is produced.
    * @return The current value of the requested variable.  If the variable
    *         does not exist in this inference, the result of the
    *         <code>Learner</code>'s <code>discreteValue(Object)</code> method
    *         applied to the <code>Object</code> is returned.
   **/
  public String valueOf(LBJ2.learn.Learner c, Object o) throws Exception {
    infer();
    return getVariable(new FirstOrderVariable(c, o)).getValue();
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
    if (!(o instanceof GLPK)) return false;
    return head == ((GLPK) o).head;
  }


  /**
    * Used during ILP constraint generation, this method creates a new
    * temporary propositional variable and adds the corresponding column to
    * the solver.
    *
    * @param d  A textual description of the subexpression whose value is
    *           represented by the new variable.
    * @return The index of the new variable created.
   **/
  protected int createNewVariable(String d) {
    int result = solver.addBooleanVariable(0);
    if (PRINT_ILP == ILPInference.VERBOSITY_HIGH)
      System.out.println(result + ": " + d);
    return result;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDoubleImplication c) {
    assert topLevel : "GLPK: PropositionalDoubleImplication encountered.";
    topLevel = false;

    int[] indexes = new int[2];
    double[] coefficients = new double[2];
    double bound = 0;

    c.left.runVisit(this);
    indexes[0] = returnIndex;
    if (returnNegation) {
      coefficients[0] = -1;
      --bound;
    }
    else coefficients[0] = 1;

    c.right.runVisit(this);
    indexes[1] = returnIndex;
    if (returnNegation) {
      coefficients[1] = 1;
      ++bound;
    }
    else coefficients[1] = -1;

    solver.addEqualityConstraint(indexes, coefficients, bound);

    topLevel = true;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalImplication c) {
    assert false : "GLPK: PropositionalImplication encountered.";
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

    int[] indexes = null;
    double[] coefficients = null;
    double bound;

    if (topLevel) {
      PropositionalConstraint[] variables =
        new PropositionalConstraint[children.length];
      int size = 0;
      for (int i = 0; i < children.length; ++i) {
        if (children[i] instanceof PropositionalVariable
            || children[i] instanceof PropositionalNegation)
          variables[size++] = children[i];
        else children[i].runVisit(this);
      }

      if (size > 0) {
        indexes = new int[size];
        coefficients = new double[size];
        bound = size;

        for (int i = 0; i < size; ++i) {
          variables[i].runVisit(this);
          indexes[i] = returnIndex;
          if (returnNegation) {
            coefficients[i] = -1;
            --bound;
          }
          else coefficients[i] = 1;
        }

        solver.addEqualityConstraint(indexes, coefficients, bound);
      }
    }
    else {
      indexes = new int[children.length + 1];
      coefficients = new double[children.length + 1];
      bound = 0;

      for (int i = 0; i < children.length; ++i) {
        children[i].runVisit(this);
        indexes[i] = returnIndex;
        if (returnNegation) {
          coefficients[i] = -1;
          --bound;
        }
        else coefficients[i] = 1;
      }

      String[] stringIndexes = new String[children.length];
      for (int i = 0; i < children.length; ++i)
        stringIndexes[i] = (coefficients[i] < 0 ? "!" : "") + indexes[i];
      Arrays.sort(stringIndexes);
      String key = stringIndexes[0];
      for (int i = 1; i < stringIndexes.length; ++i)
        key += "&" + stringIndexes[i];
      Integer I = (Integer) indexMap.get(key);

      if (I == null) {
        I = new Integer(createNewVariable(key));
        indexMap.put(key, I);

        indexes[children.length] = I.intValue();
        coefficients[children.length] = -children.length;
        solver.addGreaterThanConstraint(indexes, coefficients, bound);

        coefficients[children.length] = -1;
        solver.addLessThanConstraint(indexes, coefficients,
                                     bound + children.length - 1);
      }

      returnIndex = I.intValue();
      returnNegation = false;
    }
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

    int[] indexes = null;
    double[] coefficients = null;
    double bound = 0;

    if (topLevel) {
      int subConstraintIndex = -1;

      for (int i = 0; i < children.length && subConstraintIndex == -1; ++i) {
        if (children[i] instanceof PropositionalVariable
            || children[i] instanceof PropositionalNegation)
          continue;
        if (children[i] instanceof PropositionalConjunction)
          subConstraintIndex = i;
        else if (children[i] instanceof PropositionalAtLeast)
          subConstraintIndex = i;
      }

      if (subConstraintIndex > -1) {
        PropositionalConstraint[] subChildren =
          (PropositionalConstraint[])
          children[subConstraintIndex].getChildren();
        int multiplier =
          children[subConstraintIndex] instanceof PropositionalConjunction
          ? subChildren.length
          : ((PropositionalAtLeast) children[subConstraintIndex]).getM();

        indexes = new int[subChildren.length + children.length - 1];
        coefficients = new double[subChildren.length + children.length - 1];
        bound = multiplier;

        topLevel = false;

        int j = 0;
        for (int i = 0; i < children.length; ++i) {
          if (i == subConstraintIndex) continue;

          children[i].runVisit(this);
          indexes[j] = returnIndex;
          if (returnNegation) {
            coefficients[j] = -multiplier;
            bound -= multiplier;
          }
          else coefficients[j] = multiplier;

          ++j;
        }

        for (int i = 0; i < subChildren.length; ++i, ++j) {
          subChildren[i].runVisit(this);
          indexes[j] = returnIndex;
          if (returnNegation) {
            coefficients[j] = -1;
            --bound;
          }
          else coefficients[j] = 1;
        }

        topLevel = true;

        solver.addGreaterThanConstraint(indexes, coefficients, bound);
        return;
      }
    }

    if (topLevel) {
      indexes = new int[children.length];
      coefficients = new double[children.length];
      bound = 1;
    }
    else {
      indexes = new int[children.length + 1];
      coefficients = new double[children.length + 1];
    }

    boolean saveTopLevel = topLevel;
    topLevel = false;
    for (int i = 0; i < children.length; ++i) {
      children[i].runVisit(this);
      indexes[i] = returnIndex;
      if (returnNegation) {
        coefficients[i] = -1;
        --bound;
      }
      else coefficients[i] = 1;
    }

    topLevel = saveTopLevel;
    if (topLevel)
      solver.addGreaterThanConstraint(indexes, coefficients, bound);
    else {
      String[] stringIndexes = new String[children.length];
      for (int i = 0; i < children.length; ++i)
        stringIndexes[i] = (coefficients[i] < 0 ? "!" : "") + indexes[i];
      Arrays.sort(stringIndexes);
      String key = stringIndexes[0];
      for (int i = 1; i < stringIndexes.length; ++i)
        key += "|" + stringIndexes[i];
      Integer I = (Integer) indexMap.get(key);

      if (I == null) {
        I = new Integer(createNewVariable(key));
        indexMap.put(key, I);

        indexes[children.length] = I.intValue();
        coefficients[children.length] = -1;
        solver.addGreaterThanConstraint(indexes, coefficients, bound);

        coefficients[children.length] = -children.length;
        solver.addLessThanConstraint(indexes, coefficients, bound);
      }

      returnIndex = I.intValue();
      returnNegation = false;
    }
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

    int[] indexes = null;
    double[] coefficients = null;
    double bound = 0;

    if (topLevel) {
      indexes = new int[children.length];
      coefficients = new double[children.length];
      bound = c.getM();
    }
    else {
      indexes = new int[children.length + 1];
      coefficients = new double[children.length + 1];
    }

    boolean saveTopLevel = topLevel;
    topLevel = false;
    for (int i = 0; i < children.length; ++i) {
      children[i].runVisit(this);
      indexes[i] = returnIndex;
      if (returnNegation) {
        coefficients[i] = -1;
        --bound;
      }
      else coefficients[i] = 1;
    }

    topLevel = saveTopLevel;
    if (topLevel)
      solver.addGreaterThanConstraint(indexes, coefficients, bound);
    else {
      String[] stringIndexes = new String[children.length];
      for (int i = 0; i < children.length; ++i)
        stringIndexes[i] = (coefficients[i] < 0 ? "!" : "") + indexes[i];
      Arrays.sort(stringIndexes);
      String key = "atl" + c.getM() + "of" + stringIndexes[0];
      for (int i = 1; i < stringIndexes.length; ++i)
        key += "&" + stringIndexes[i];
      Integer I = (Integer) indexMap.get(key);

      if (I == null) {
        I = new Integer(createNewVariable(key));
        indexMap.put(key, I);

        indexes[children.length] = I.intValue();
        coefficients[children.length] = -c.getM();
        solver.addGreaterThanConstraint(indexes, coefficients, bound);

        coefficients[children.length] = -children.length;
        solver.addLessThanConstraint(indexes, coefficients,
                                     bound + c.getM() - 1);
      }

      returnIndex = I.intValue();
      returnNegation = false;
    }
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalNegation c) {
    assert c.constraint instanceof PropositionalVariable
      : "GLPK: Negation of a " + c.constraint.getClass().getName()
        + " encountered.";
    c.constraint.runVisit(this);
    returnNegation = true;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalVariable c) {
    returnIndex = ((Integer) indexMap.get(c)).intValue();
    returnNegation = false;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalConstant c) {
    assert false : "GLPK: Constant encountered. (" + c.evaluate() + ")";
  }
}

