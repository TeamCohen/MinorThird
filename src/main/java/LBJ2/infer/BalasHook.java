package LBJ2.infer;

import LBJ2.util.IVector;
import LBJ2.util.Sort;


/**
  * This {@link ILPSolver} implements Egon Balas' zero-one ILP solving
  * algorithm.  It is a branch and bound algorithm that can return the best
  * solution found so far if stopped early.  For more information on the
  * original algorithm, see <br>
  *
  * <blockquote>
  * E. Balas. 1965. An Additive Algorithm for Solving Linear Programs with
  * Zero-One Variables. <i>Operations Research</i>, 13(4):517–546.
  * </blockquote>
  *
  * @author Nick Rizzolo
 **/
public class BalasHook extends ZeroOneILPProblem implements ILPSolver
{
  private static boolean debug = false;


  /**
    * Whether or not the algorithm will halt upon finding its first feasible
    * solution.
   **/
  protected boolean first;
  /**
    * Verbosity level.  {@link ILPInference#VERBOSITY_NONE} produces no
    * incidental output.  If set to {@link ILPInference#VERBOSITY_LOW}, only
    * variable and constraint counts are reported on <code>STDOUT</code>.  If
    * set to {@link ILPInference#VERBOSITY_HIGH}, a textual representation of
    * the entire optimization problem is also generated on
    * <code>STDOUT</code>.
   **/
  protected int verbosity;
  /** The solution to the optimization problem. */
  private int[] solution;
  /** The value of the objective function at {@link #solution}. */
  private double objectiveValue;
  /**
    * Each element is <code>true</code> iff the corresponding inference
    * variable's value in {@link #solution} has been negated (which happens
    * iff that variable initially had a negative objective function
    * coefficient).
   **/
  private boolean[] negated;

  /**
    * The current solution being evaluated in the intermediate stages of the
    * algorithm.
   **/
  private int[] x;
  /**
    * The current values that, when added to the left hand sides of the
    * corresponding constraints, cause all constraints to be satisfied at
    * equality during the intermediate stages of the algorithm.
   **/
  private double[] slack;
  /**
    * A set of variables which must retain their current settings in
    * <code>x</code> as the algorithm continues processing.
   **/
  private boolean[] cancelled;


  /** Default constructor. */
  public BalasHook() { this(ILPInference.VERBOSITY_NONE); }

  /**
    * Creates a new ILP solver with the specified verbosity.
    *
    * @param v  Setting for the {@link #verbosity} level.
   **/
  public BalasHook(int v) { this(false, v); }

  /**
    * Creates a new ILP solver that halts at the first feasible solution
    * found, if the parameter to this constructor is <code>true</code>.
    *
    * @param f  Whether or not to stop at the first feasible solution.
   **/
  public BalasHook(boolean f) {
    this(f, ILPInference.VERBOSITY_NONE);
  }

  /**
    * Creates a new ILP solver that halts at the first feasible solution
    * found, if the first parameter to this constructor is <code>true</code>.
    *
    * @param f  Whether or not to stop at the first feasible solution.
    * @param v  Setting for the {@link #verbosity} level.
   **/
  public BalasHook(boolean f, int v) {
    first = f;
    verbosity = v;
  }

  /**
    * Creates a new ILP solver with the problem represented in the named file
    * loaded and ready to solve.  The constraints in the problem are assumed
    * to all be "less than or equal to" constraints, and the actual
    * (in)equality symbol is ignored during parsing.
    *
    * @param name The name of the file containing the textual representation
    *             of a 0-1 ILP problem.
   **/
  public BalasHook(String name) { this(name, ILPInference.VERBOSITY_NONE); }

  /**
    * Creates a new ILP solver with the problem represented in the named file
    * loaded and ready to solve.  The constraints in the problem are assumed
    * to all be "less than or equal to" constraints, and the actual
    * (in)equality symbol is ignored during parsing.
    *
    * @param name The name of the file containing the textual representation
    *             of a 0-1 ILP problem.
    * @param f    Whether or not to stop at the first feasible solution.
   **/
  public BalasHook(String name, boolean f) {
    this(name, f, ILPInference.VERBOSITY_NONE);
  }

  /**
    * Creates a new ILP solver with the problem represented in the named file
    * loaded and ready to solve.  The constraints in the problem are assumed
    * to all be "less than or equal to" constraints, and the actual
    * (in)equality symbol is ignored during parsing.
    *
    * @param name The name of the file containing the textual representation
    *             of a 0-1 ILP problem.
    * @param v    Setting for the {@link #verbosity} level.
   **/
  public BalasHook(String name, int v) {
    this(name, false, v);
  }

  /**
    * Creates a new ILP solver with the problem represented in the named file
    * loaded and ready to solve.  The constraints in the problem are assumed
    * to all be "less than or equal to" constraints, and the actual
    * (in)equality symbol is ignored during parsing.
    *
    * @param name The name of the file containing the textual representation
    *             of a 0-1 ILP problem.
    * @param f    Whether or not to stop at the first feasible solution.
    * @param v    Setting for the {@link #verbosity} level.
   **/
  public BalasHook(String name, boolean f, int v) {
    super(name);
    first = f;
    verbosity = v;
  }


  /** Sets the value of {@link #first}. */
  public void setFirst(boolean f) { first = f; }


  /**
    * This method clears the all constraints and variables out of the ILP
    * solver's problem representation, bringing the <code>ILPSolver</code>
    * instance back to the state it was in when first constructed.
   **/
  public void reset() {
    super.reset();
    solution = x = null;
    negated = null;
    slack = null;
    objectiveValue = Double.POSITIVE_INFINITY;
  }


  /**
    * Simply overrides
    * {@link ZeroOneILPProblem#addConstraint(int[],double[],int,double)} so
    * that it calls
    * {@link ZeroOneILPProblem#addConstraint(int[],double[],double)} thereby
    * ignoring the constraint's type.  Overriding this method in this way
    * ensures that types are not stored when reading in a textual problem
    * representation, as happens when constructing an instance with
    * {@link #BalasHook(String)}.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param t  The type of comparison in this constraint.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  protected void addConstraint(int[] i, double[] a, int t, double b) {
    addConstraint(i, a, b);
  }


  /**
    * Adds a new fixed constraint to the problem.  The two array arguments
    * must be the same length, as their elements correspond to each other.
    * Variables whose coefficients are zero need not be mentioned.  Variables
    * that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a = b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  public void addEqualityConstraint(int[] i, double[] a, double b) {
    addLessThanConstraint(i, a, b);
    addGreaterThanConstraint(i, a, b);
  }


  /**
    * Adds a new lower bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &gt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param I  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The lower bound for the new constraint.
   **/
  public void addGreaterThanConstraint(int[] I, double[] a, double b) {
    for (int i = 0; i < a.length; ++i) a[i] = -a[i];
    addLessThanConstraint(I, a, -b);
  }


  /**
    * Adds a new upper bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &lt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The upper bound for the new constraint.
   **/
  public void addLessThanConstraint(int[] i, double[] a, double b) {
    addConstraint(i, a, b);
  }


  /**
    * Solves the ILP problem, saving the solution internally.
    *
    * @return <code>true</code> iff a solution was found successfully.
   **/
  public boolean solve() throws Exception {
    int variables = objectiveCoefficients.size();
    int constraints = Ac.size();

    if (verbosity > ILPInference.VERBOSITY_NONE) {
      System.out.println("  variables: " + variables);
      System.out.println("  constraints: " + constraints);
    }

    negated = new boolean[variables];

    for (int i = 0; i < variables; ++i) {
      double c = objectiveCoefficients.get(i);
      if (Math.abs(c) < ZeroOneILPProblem.TOLERANCE)
        objectiveCoefficients.set(i, 0);
      else {
        if (maximize) c = -c;
        if (c < 0) {
          c = -c;

          for (int j = 0; j < constraints; ++j) {
            int vIndex = Av.binarySearch(j, i);
            if (vIndex >= 0) {
              double coefficient = Ac.get(j, vIndex);
              bounds.set(j, bounds.get(j) - coefficient);
              Ac.set(j, vIndex, -coefficient);
            }
          }

          negated[i] = true;
        }

        objectiveCoefficients.set(i, c);
      }
    }

    if (verbosity == ILPInference.VERBOSITY_HIGH) {
      boolean saveMaximize = maximize;
      maximize = false;
      StringBuffer buffer = new StringBuffer();
      write(buffer);
      System.out.print(buffer);
      maximize = saveMaximize;
    }

    x = new int[variables];
    slack = slack(x);
    cancelled = new boolean[variables];
    boolean result = solve(evaluate(x));

    for (int i = 0; i < variables; ++i)
      if (negated[i]) {
        x[i] = 1 - x[i];
        objectiveValue -= objectiveCoefficients.get(i);
      }

    if (maximize) objectiveValue = -objectiveValue;

    return result;
  }


  /**
    * Given a potential solution, this method determines the values for the
    * slack violates that will satisfy our less-than constraints at equality.
    *
    * @param x  The current settings of the inference variables.
    * @return The resulting values of the slack variables.
   **/
  private double[] slack(int[] x) {
    final double[] result = new double[bounds.size()];

    for (int i = 0; i < Ac.size(); ++i) {
      double lhs = 0;
      for (int j = 0; j < Ac.size(i); ++j)
        lhs += x[Av.get(i, j)] * Ac.get(i, j);
      result[i] = bounds.get(i) - lhs;
      final double rounded = Math.round(result[i]);
      if (Math.abs(rounded - result[i]) < ZeroOneILPProblem.TOLERANCE)
        result[i] = rounded;
    }

    return result;
  }


  /**
    * Implements the meat of the Balas algorithm recursively.
    *
    * @param z  The value of the objective function with the current variable
    *           settings.
    * @return <code>true</code> iff a solution was found successfully.
   **/
  public boolean solve(final double z) {
    // The slack variables, which will also be used later, tell us whether any
    // constraints have been violated.  If none have, we know we have found
    // the optimal solution under the additional constraints that all
    // ineligible variables must take their current settings in x.
    final IVector violated = new IVector();
    for (int i = 0; i < slack.length; ++i)
      if (slack[i] < 0) violated.add(i);
    final int violatedSize = violated.size();

    if (violatedSize == 0) {
      solution = (int[]) x.clone();
      objectiveValue = z;

      if (debug) {
        final int[] xx = (int[]) x.clone();
        double f = objectiveValue;

        System.out.print("[");
        for (int i = 0; i < xx.length; ++i) {
          if (negated[i]) {
            xx[i] = 1 - xx[i];
            f -= objectiveCoefficients.get(i);
          }

          System.out.print(xx[i]);
          if (i + 1 < xx.length) System.out.print(", ");
        }

        if (maximize) f = -f;
        System.out.println("]: " + f);
      }

      return true;
    }

    final IVector eligible = getEligibleVariables(z, violated);

    // Constraints get closer to satisfaction when variables with negative
    // coefficients in those constraints get turned on.  If there are any
    // constraints in which the eligible variables cannot contribute enough in
    // negative coefficients to satisfy the constraint, then we'll need to
    // backtrack.  lhsNegative keeps track of the total negative coefficient
    // contribution possible in each constraint as we try turning eligible
    // variables on, then turning them off and making them ineligible after
    // backtracking.
    final int eligibles = eligible.size();
    if (eligibles == 0) return false;

    final IVector atEquality = new IVector();
    final double[] lhsNegative =
      constraintSatisfiability(violated, eligible, atEquality);
    if (lhsNegative == null) return false;

    // Now the search begins, setting eligible variables on and making
    // recursive calls.
    final IVector cancelledLocally = new IVector();
    int[] indexes = null;
    int bestIndex = 0;
    int ineligibles = 0;
    boolean result = false;

    for (boolean satisfiable = true; satisfiable; ) {
      if (atEquality.size() > 0) {
        result |= satisfyAll(atEquality, z, eligible);
        satisfiable = false;
      }
      else {
        // If there weren't any constraints satisfied at equality when their
        // negative coefficient variables are turned on, then we choose our
        // next eligible variable according to the metric proposed by Balas.
        if (indexes == null) indexes = sortVariablesByViolations(eligible);

        int bestVariable = eligible.get(indexes[bestIndex]);
        while (++bestIndex < eligibles && cancelled[bestVariable]) {
          bestVariable = eligible.get(indexes[bestIndex]);
          --ineligibles;
        }
        if (cancelled[bestVariable]) break;

        setVariableOn(bestVariable);
        cancelled[bestVariable] = true;
        cancelledLocally.add(bestVariable);

        final double oldValue = objectiveValue;
        result |= solve(z + objectiveCoefficients.get(bestVariable));
        if (first && result) break;

        setVariableOff(bestVariable);

        final IVector newlyIneligible = new IVector();
        newlyIneligible.add(bestVariable);

        if (oldValue != objectiveValue) {
          for (int k = bestIndex; k < eligibles; ++k) {
            final int j = eligible.get(k);
            if (!cancelled[j]
                && z + objectiveCoefficients.get(j)
                   >= objectiveValue - ZeroOneILPProblem.TOLERANCE) {
              newlyIneligible.add(j);
              cancelled[j] = true;
              cancelledLocally.add(j);
            }
          }

          ineligibles += newlyIneligible.size() - 1;
        }

        satisfiable = eligibles - bestIndex - ineligibles > 0;

        for (int i = 0; i < violatedSize && satisfiable; ++i) {
          final int cIndex = violated.get(i);

          for (int j = 0; j < newlyIneligible.size(); ++j) {
            final int vIndex =
              Av.binarySearch(cIndex, newlyIneligible.get(j));
            if (vIndex >= 0) {
              final double c = Ac.get(cIndex, vIndex);
              if (c < 0) lhsNegative[i] -= c;
            }
          }

          satisfiable =
            lhsNegative[i] - ZeroOneILPProblem.TOLERANCE <= slack[cIndex];
          if (satisfiable
              && Math.abs(slack[cIndex] - lhsNegative[i])
                 < ZeroOneILPProblem.TOLERANCE)
            atEquality.add(cIndex);
        }
      }
    }

    for (int i = 0; i < cancelledLocally.size(); ++i)
      cancelled[cancelledLocally.get(i)] = false;
    return result;
  }


  /**
    * Determines which variables have a chance both to improve on the
    * incumbunt solution and to bring the current x closer to feasibility.
    *
    * @param z        The value of the objective function with the current
    *                 variable settings.
    * @param violated The set of violated constraints.
    * @return A vector of variables as described above.
   **/
  private IVector getEligibleVariables(final double z,
                                       final IVector violated) {
    final IVector eligible = new IVector();
    final int violatedSize = violated.size();

    for (int j = 0; j < x.length; ++j)
      if (!cancelled[j]
          && z + objectiveCoefficients.get(j)
             < objectiveValue - ZeroOneILPProblem.TOLERANCE) {
        boolean good = false;
        for (int i = 0; i < violatedSize && !good; ++i) {
          final int cIndex = violated.get(i);
          final int vIndex = Av.binarySearch(cIndex, j);
          good = vIndex >= 0 && Ac.get(cIndex, vIndex) < 0;
        }
        if (good) eligible.add(j);
      }

    return eligible;
  }


  /**
    * This method <i>attempts</i> to satisfy all specified constraints by
    * turning on all eligible variables that have a negative coefficient in
    * any of them.
    *
    * <p> If there are constraints satisfied at equality when their negative
    * coefficient variables are turned on, we know the only chance to satisfy
    * them is to turn on all eligible variables with negative coefficients in
    * such constraints.  This method does just that before making the
    * recursive call to {@link #solve(double)}.
    *
    * @param atEquality A vector of constraints which need to be satisfied.
    * @param z          The value of the objective function with the current
    *                   variable settings.
    * @param eligible   A vector of variables which are eligible to be turned
    *                   on.
    * @return <code>true</code> iff turning on all eligible variables as
    *         described above lead to a feasible solution.
   **/
  private boolean satisfyAll(final IVector atEquality, double z,
                             final IVector eligible) {
    final IVector F = new IVector();
    final int constraints = atEquality.size();
    boolean result = false;

    for (int i = 0; i < constraints; ++i) {
      final int cIndex = atEquality.get(i);
      final int constraintSize = Ac.size(cIndex);
      for (int k = 0; k < constraintSize; ++k) {
        final int j = Av.get(cIndex, k);
        if (cancelled[j]) continue;
        final double c = Ac.get(cIndex, k);
        if (c < 0 && eligible.binarySearch(j) >= 0) {
          F.add(j);
          cancelled[j] = true;
          z += objectiveCoefficients.get(j);
        }
      }
    }

    final int FSize = F.size();

    if (z < objectiveValue - ZeroOneILPProblem.TOLERANCE) {
      for (int i = 0; i < FSize; ++i)
        setVariableOn(F.get(i));
      result = solve(z);
      for (int i = 0; i < FSize; ++i)
        setVariableOff(F.get(i));
    }

    for (int i = 0; i < FSize; ++i)
      cancelled[F.get(i)] = false;

    return result;
  }


  /**
    * For each violated constraint, this method determines whether it is
    * individually satisfiable given the eligible variables remaining.  If all
    * constraints are still satisfiable, the sums of the negative coefficients
    * on eligible variables for each constraint is returned.  Otherwise,
    * <code>null</code> is returned.
    *
    * <p> As a side effect, this method also determines which constraints can
    * only be satisfied exactly at equality and stores them in the given
    * vector.
    *
    * @param violated   The set of violated constraints.
    * @param eligible   The set of variables still eligible to be turned on.
    * @param atEquality A vector into which is stored the set of constraints
    *                   that can only be satisfied at equality.
   **/
  private double[] constraintSatisfiability(final IVector violated,
                                            final IVector eligible,
                                            final IVector atEquality) {
    final int violatedSize = violated.size();
    final double[] lhsNegative = new double[violatedSize];

    for (int i = 0; i < violatedSize; ++i) {
      final int index = violated.get(i);

      for (int j = 0; j < Ac.size(index); ++j) {
        final double c = Ac.get(index, j);
        if (c < 0 && eligible.binarySearch(Av.get(index, j)) >= 0)
          lhsNegative[i] += c;
      }

      if (lhsNegative[i] - ZeroOneILPProblem.TOLERANCE > slack[index])
        return null;
      if (Math.abs(slack[index] - lhsNegative[i])
          < ZeroOneILPProblem.TOLERANCE)
        atEquality.add(index);
    }

    return lhsNegative;
  }


  /**
    * Sets the given variable on and updates the slack variables.
    *
    * @param j  The variable to set on.
   **/
  private void setVariableOn(final int j) {
    x[j] = 1;

    for (int i = 0; i < slack.length; ++i) {
      final int vIndex = Av.binarySearch(i, j);
      if (vIndex >= 0) slack[i] -= Ac.get(i, vIndex);
    }
  }


  /**
    * Sets the given variable off and updates the slack variables.
    *
    * @param j  The variable to set on.
   **/
  private void setVariableOff(final int j) {
    x[j] = 0;

    for (int i = 0; i < slack.length; ++i) {
      final int vIndex = Av.binarySearch(i, j);
      if (vIndex >= 0) slack[i] += Ac.get(i, vIndex);
    }
  }


  /**
    * Computes a vector of indexes that, in effect, sorts the given variables
    * according to how violated the constraints would be if each were turned
    * on independently.  Ties are broken by giving precedence to variables
    * with smaller objective coefficients.
    *
    * @param eligible The variables to be sorted.
    * @return An array of <code>Integer</code> indexes pointing into the
    *         <code>eligible</code> vector.
   **/
  private int[] sortVariablesByViolations(final IVector eligible) {
    final int eligibles = eligible.size();
    final int[] indexes = new int[eligibles];
    final double[] violations = new double[eligibles];

    for (int k = 0; k < eligibles; ++k) {
      indexes[k] = k;
      final int j = eligible.get(k);

      for (int i = 0; i < slack.length; ++i) {
        final int vIndex = Av.binarySearch(i, j);
        final double aij = vIndex < 0 ? 0 : Ac.get(i, vIndex);
        violations[k] += Math.max(0, aij - slack[i]);
      }
    }

    Sort.sort(indexes,
        new Sort.IntComparator() {
          public int compare(int i1, int i2) {
            if (Math.abs(violations[i1] - violations[i2])
                < ZeroOneILPProblem.TOLERANCE) {
              double c1 = objectiveCoefficients.get(eligible.get(i1));
              double c2 = objectiveCoefficients.get(eligible.get(i2));
              if (Math.abs(c1 - c2) < ZeroOneILPProblem.TOLERANCE)
                return i1 - i2;
              if (c1 < c2) return -1;
              return 1;
            }

            if (violations[i1] < violations[i2]) return -1;
            return 1;
          }
        });

    return indexes;
  }


  /**
    * Tests whether the problem represented by this <code>ILPSolver</code>
    * instance has been solved already.
   **/
  public boolean isSolved() { return solution != null; }


  /**
    * When the problem has been solved, use this method to retrieve the value
    * of any Boolean inference variable.  The result of this method is
    * undefined when the problem has not yet been solved.
    *
    * @param index  The index of the variable whose value is requested.
    * @return The value of the variable.
   **/
  public boolean getBooleanValue(int index) { return solution[index] == 1; }


  /**
    * When the problem has been solved, use this method to retrieve the value
    * of the objective function at the solution.  The result of this method is
    * undefined when the problem has not yet been solved.  If the problem had
    * no feasible solutions, negative (positive, respectively) infinity will
    * be returned if maximizing (minimizing).
    *
    * @return The value of the objective function at the solution.
   **/
  public double objectiveValue() { return objectiveValue; }


  /**
    * Creates a textual representation of the ILP problem in an algebraic
    * notation.
    *
    * @param buffer The created textual representation will be appended here.
   **/
  public void write(StringBuffer buffer) {
    if (maximize) buffer.append("max");
    else buffer.append("min");

    int variables = objectiveCoefficients.size();
    for (int i = 0; i < variables; ++i) {
      double c = objectiveCoefficients.get(i);
      buffer.append(" ");
      if (c >= 0) buffer.append("+");
      buffer.append(c);
      buffer.append(" ");
      if (negated[i]) buffer.append("(");
      buffer.append("x_");
      buffer.append(i);
      if (negated[i]) buffer.append(")");
    }

    buffer.append("\n");

    int constraints = Ac.size();
    for (int i = 0; i < constraints; ++i) {
      int constraintSize = Ac.size(i);
      buffer.append(" ");

      for (int j = 0; j < constraintSize; ++j) {
        double c = Ac.get(i, j);
        buffer.append(" ");
        if (c >= 0) buffer.append("+");
        buffer.append(c);
        buffer.append(" x_");
        buffer.append(Av.get(i, j));
      }

      buffer.append(" <= ");
      buffer.append(bounds.get(i));
      buffer.append("\n");
    }
  }
}

