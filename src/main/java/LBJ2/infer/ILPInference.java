package LBJ2.infer;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import LBJ2.classify.Score;
import LBJ2.classify.ScoreSet;


/**
  * This class employs an {@link ILPSolver} to solve a constrained inference
  * problem.  When constructing an instance of this class in an LBJ source
  * file, use one of the constructors that does <i>not</i> specify a head
  * object.  The generated code will fill in the head object automatically.
  * The other constructor parameters are used to specify the ILP algorithm and
  * enable textual output of ILP variable descriptions to <code>STDOUT</code>.
  * Textual output of the ILP problem itself is controlled by the
  * {@link ILPSolver}.
  *
  * @author Nick Rizzolo
 **/
public class ILPInference extends Inference
{
  /** A possible setting for {@link #verbosity}. */
  public static final int VERBOSITY_NONE = 0;
  /** A possible setting for {@link #verbosity}. */
  public static final int VERBOSITY_LOW = 1;
  /** A possible setting for {@link #verbosity}. */
  public static final int VERBOSITY_HIGH = 2;

  /** Keeps the next ID number for objects of this class. */
  protected static int nextID = 0;


  /** The identification number for this object, used in debug file names. */
  protected int ID;

  /** The ILP algorithm. */
  protected ILPSolver solver;
  /** This flag is set if the constraints turn out to be true in all cases. */
  protected boolean tautology;
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
    * Verbosity level.  {@link ILPInference#VERBOSITY_NONE} produces no
    * incidental output.  If set to {@link ILPInference#VERBOSITY_LOW}, only
    * timing information is printed on <code>STDOUT</code>.  If set to
    * {@link ILPInference#VERBOSITY_HIGH}, information mapping the generated
    * ILP variables to the first order variables they were generated from and
    * their settings in the ILP problem's solution is printed to
    * <code>STDOUT</code>.
   **/
  protected int verbosity;


  /** Don't use this constructor, since it doesn't set an ILP algorithm. */
  public ILPInference() { this(null); }

  /**
    * Initializes the ILP algorithm, but not the head object.
    *
    * @param a  The ILP algorithm.
   **/
  public ILPInference(ILPSolver a) { this(null, a); }

  /**
    * Initializes the ILP algorithm, but not the head object.
    *
    * @param a  The ILP algorithm.
    * @param v  Sets the value of {@link #verbosity}.
   **/
  public ILPInference(ILPSolver a, int v) { this(null, a, v); }

  /** Don't use this constructor, since it doesn't set an ILP algorithm. */
  public ILPInference(Object h) { this(h, null); }

  /**
    * Sets the head object and the ILP algorithm.
    *
    * @param h  The head object.
    * @param a  The ILP algorithm.
   **/
  public ILPInference(Object h, ILPSolver a) { this(h, a, VERBOSITY_NONE); }

  /**
    * Sets the head object and the ILP algorithm.
    *
    * @param h  The head object.
    * @param a  The ILP algorithm.
    * @param v  Sets the value of {@link #verbosity}.
   **/
  public ILPInference(Object h, ILPSolver a, int v) {
    super(h);
    solver = a;
    verbosity = v;
    ID = nextID++;
  }


  /**
    * Adds a constraint to the inference.
    *
    * @param c  The constraint to add.
   **/
  public void addConstraint(FirstOrderConstraint c) {
    solver.reset();
    if (constraint == null) constraint = c;
    else
      constraint =
        new FirstOrderConjunction((FirstOrderConstraint) constraint, c);
  }


  /**
    * Uses the provided ILP algorithm to solve the ILP proglem if it hasn't
    * already been solved.
   **/
  protected void infer() throws Exception {
    if (tautology || solver.isSolved()) return;

    solver.setMaximize(true);
    constraint.consolidateVariables(variables);
    indexMap = new HashMap();

    if (verbosity > VERBOSITY_NONE)
      System.out.println("variables: (" + new Date() + ")");

    for (Iterator I = variables.values().iterator(); I.hasNext(); ) {
      FirstOrderVariable v = (FirstOrderVariable) I.next();
      ScoreSet ss = getNormalizer(v.getClassifier()).normalize(v.getScores());
      Score[] scores = null;
      if (ss != null) scores = ss.toArray();

      if (scores == null || scores.length == 0) {
        System.err.println(
            "LBJ ERROR: Classifier " + v.getClassifier()
            + " did not return any scores.  ILP inference cannot be "
            + "performed.");
        System.exit(1);
      }

      int[] indexes = solver.addDiscreteVariable(scores);

      for (int j = 0; j < scores.length; ++j) {
        indexMap.put(
            new PropositionalVariable(v.getClassifier(), v.getExample(),
                                      scores[j].value),
            new Integer(indexes[j]));

        if (verbosity >= VERBOSITY_HIGH) {
          StringBuffer toPrint = new StringBuffer();
          toPrint.append("x_");
          toPrint.append(indexes[j]);
          while (toPrint.length() < 8) toPrint.insert(0, ' ');
          toPrint.append(" (");
          toPrint.append(scores[j].score);
          toPrint.append("): ");
          toPrint.append(v.getClassifier());
          toPrint.append("(");
          toPrint.append(Inference.exampleToString(v.getExample()));
          toPrint.append(") == ");
          toPrint.append(scores[j].value);
          System.out.println(toPrint);
        }
      }
    }

    if (verbosity > VERBOSITY_NONE)
      System.out.println("propositionalization: (" + new Date() + ")");
    PropositionalConstraint propositional =
      ((FirstOrderConstraint) constraint).propositionalize();

    if (verbosity > VERBOSITY_NONE)
      System.out.println("simplification: (" + new Date() + ")");
    if (propositional instanceof PropositionalConjunction)
      propositional =
        ((PropositionalConjunction) propositional).simplify(true);
    else propositional = propositional.simplify();

    if (propositional instanceof PropositionalConstant) {
      if (propositional.evaluate()) {
        tautology = true;
        return;
      }
      else {
        System.err.println("ILP ERROR: Unsatisfiable constraints!");
        solver.addEqualityConstraint(new int[]{ 0 }, new double[]{ 1 }, 2);
      }
    }

    if (verbosity > VERBOSITY_NONE)
      System.out.println("translation: (" + new Date() + ")");
    topLevel = true;
    propositional.runVisit(this);

    if (verbosity > VERBOSITY_NONE)
      System.out.println("solution: (" + new Date() + ")");
    if (!solver.solve()) throw new InferenceNotOptimalException(solver, head);
    int variableIndex = 0;
    if (verbosity > VERBOSITY_NONE)
      System.out.println("variables set true in solution: (" + new Date()
                         + ")");

    for (Iterator I = variables.values().iterator(); I.hasNext(); ) {
      FirstOrderVariable v = (FirstOrderVariable) I.next();
      Score[] scores = v.getScores().toArray();
      for (int j = 0; j < scores.length; ++j, ++variableIndex)
        if (solver.getBooleanValue(variableIndex)) {
          v.setValue(scores[j].value);

          if (verbosity >= VERBOSITY_HIGH) {
            StringBuffer toPrint = new StringBuffer();
            toPrint.append("x_");
            toPrint.append(variableIndex);
            while (toPrint.length() < 8) toPrint.insert(0, ' ');
            toPrint.append(": ");
            toPrint.append(v);
            System.out.println(toPrint);
          }
        }
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
    if (!(o instanceof ILPInference)) return false;
    return head == ((ILPInference) o).head;
  }


  /**
    * Simply returns the <code>head</code>'s hash code.
    *
    * @see java.lang.Object#hashCode()
   **/
  public int hashCode() { return head.hashCode(); }


  /**
    * Creates a new Boolean variable to represent the value of a subexpression
    * of some constraint.
    *
    * @param d  A textual description of the subexpression whose value is
    *           represented by the new variable.
    * @return The index of the new variable.
   **/
  protected int createVariable(String d) {
    int result = solver.addBooleanVariable(0);
    if (verbosity >= VERBOSITY_HIGH) System.out.println(result + ": " + d);
    return result;
  }


  /**
    * Derived classes override this method to do some type of processing on
    * constraints of the parameter's type.
    *
    * @param c  The constraint to process.
   **/
  public void visit(PropositionalDoubleImplication c) {
    assert topLevel : "ILP: PropositionalDoubleImplication encountered.";
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
    assert false : "ILP: PropositionalImplication encountered.";
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
        I = new Integer(createVariable(key));
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
        I = new Integer(createVariable(key));
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
        I = new Integer(createVariable(key));
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
      : "ILP: Negation of a " + c.constraint.getClass().getName()
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
    assert false : "ILP: Constant encountered. (" + c.evaluate() + ")";
  }
}

