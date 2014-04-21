package LBJ2.infer;

import java.util.Arrays;
import LBJ2.parse.LineByLine;
import LBJ2.util.IVector;
import LBJ2.util.IVector2D;
import LBJ2.util.DVector;
import LBJ2.util.DVector2D;
import LBJ2.util.Sort;


/**
  * Can be used to represent an ILP problem, assuming all variables are 0-1.
  *
  * @author Nick Rizzolo
 **/
public class ZeroOneILPProblem
{
  /** Represents the constraint type "equality". */
  public static final int EQUALITY = 0;
  /** Represents the constraint type "less than or equal to". */
  public static final int LESS_THAN = 1;
  /** Represents the constraint type "greater than or equal to". */
  public static final int GREATER_THAN = 2;
  /** Maps from the three constraint types to their operator symbols. */
  public static final String[] boundTypeSymbols = { "=", "<=", ">=" };

  /** Used to mitigate floating point error in (in)equality comparisons. */
  public static final double TOLERANCE = 1e-10;


  /**
    * Remembers whether the objective function should be maximized or
    * minimzed.
   **/
  protected boolean maximize;
  /**
    * Represents the coefficients of all inference variables in the objective
    * function.
   **/
  protected DVector objectiveCoefficients;
  /**
    * Half of a sparse matrix representation of the constraints; this half
    * contains the variable indexes corresponding to the coefficients in
    * {@link #Ac}.
   **/
  protected IVector2D Av;
  /**
    * Half of a sparse matrix representation of the constraints; this half
    * contains the coefficients on the variables whose indexes appear in
    * {@link #Av}.
   **/
  protected DVector2D Ac;
  /** Contains the types of the constraints. */
  protected IVector boundTypes;
  /** The vector of constraint bounds. */
  protected DVector bounds;


  /** Default constructor. */
  public ZeroOneILPProblem() { reset(); }


  /**
    * Reads a textual representation of a 0-1 ILP problem from the specified
    * file.
    *
    * @param name The name of the file from which to read the ILP problem's
    *             representation.
   **/
  public ZeroOneILPProblem(String name) {
    reset();
    LineByLine parser =
      new LineByLine(name) {
        public Object next() {
          String line = readLine();
          while (line != null && line.matches("\\s*")) line = readLine();
          return line;
        }
      };

    String line = (String) parser.next();
    String[] a = line.split(" ");
    maximize = a[0].startsWith("max");
    for (int i = 1; i < a.length; i += 2) {
      double c = Double.parseDouble(a[i]);
      int v = Integer.parseInt(a[i+1].substring(2));
      objectiveCoefficients.set(v, c);
    }

    line = (String) parser.next();
    if (line.indexOf("subject") != -1) line = (String) parser.next();

    for (int i = 0; line != null; line = (String) parser.next(), ++i) {
      a = line.substring(2).split(" ");
      int[] variables = new int[a.length / 2 - 1];
      double[] coefficients = new double[variables.length];

      for (int j = 0; j < a.length - 2; j += 2) {
        coefficients[j / 2] = Double.parseDouble(a[j]);
        variables[j / 2] = Integer.parseInt(a[j+1].substring(2));
      }

      int type = EQUALITY;
      if (a[a.length - 2].charAt(0) == '>') type = GREATER_THAN;
      else if (a[a.length - 2].charAt(0) == '<') type = LESS_THAN;
      double bound = Double.parseDouble(a[a.length - 1]);

      addConstraint(variables, coefficients, type, bound);
    }
  }


  /**
    * This method clears the all constraints and variables out of the problem
    * representation, bringing it back to the state it was in when first
    * constructed.
   **/
  public void reset() {
    maximize = false;
    objectiveCoefficients = new DVector();
    Av = new IVector2D();
    Ac = new DVector2D();
    boundTypes = new IVector();
    bounds = new DVector();
  }


  /**
    * Sets the direction of the objective function.
    *
    * @param d  <code>true</code> if the objective function is to be
    *           maximized.
   **/
  public void setMaximize(boolean d) { maximize = d; }
  /**
    * Returns <code>true</code> iff the objective function is to be maximized.
   **/
  public boolean getMaximize() { return maximize; }


  /** Returns the number of constraints in the ILP problem. */
  public int rows() { return bounds.size(); }
  /** Returns the number of variables in the ILP problem. */
  public int columns() { return objectiveCoefficients.size(); }


  /**
    * Sets the specified coefficient in the objective function.
    *
    * @param j  The index of the variable whose coefficient will be set.
    * @param cj The new value of the coefficient.
   **/
  public void setObjectiveCoefficient(int j, double cj) {
    objectiveCoefficients.set(j, cj);
  }
  /** Returns the specified objective coefficient. */
  public double getObjectiveCoefficient(int j) {
    return objectiveCoefficients.get(j);
  }


  /**
    * Sets the specified coefficient in the constraint matrix.
    *
    * @param i    The index of the constraint.
    * @param j    The index of the variable.
    * @param aij  The new value of the coefficient.
   **/
  public void setConstraintCoefficient(int i, int j, double aij) {
    int index = Av.binarySearch(i, j);

    if (index < 0) {
      index = -index - 1;

      for (int k = Av.size(i) - 1; k >= index; --k) {
        Av.set(i, k + 1, Av.get(i, k));
        Ac.set(i, k + 1, Ac.get(i, k));
      }

      Av.set(i, index, j);
    }

    Ac.set(i, index, aij);
  }

  /** Returns the specified constraint coefficient. */
  public double getConstraintCoefficient(int i, int j) {
    int index = Av.binarySearch(i, j);
    if (index < 0) return 0;
    return Ac.get(i, index);
  }


  /**
    * Sets the bound type for the specified constraint.
    *
    * @param i  The constraint whose bound type will be set.
    * @param t  The new type for the constraint's bound.
   **/
  public void setBoundType(int i, int t) { boundTypes.set(i, t); }
  /** Returns the type of the specified constraint's bound. */
  public int getBoundType(int i) { return boundTypes.get(i); }


  /**
    * Sets the bound on the specified constraint.
    *
    * @param i  The constraint whose bound will be set.
    * @param bi The new value for the bound.
   **/
  public void setConstraintBound(int i, double bi) { bounds.set(i, bi); }
  /** Returns the bound of the specified constraint. */
  public double getConstraintBound(int i) { return bounds.get(i); }


  /**
    * Determines whether all constraints are satisfied by the given solution.
    *
    * @param x  The settings of the variables.
    * @return <code>true</code> iff all constraints are satisfied.
   **/
  public boolean constraintsSatisfied(int[] x) {
    int constraints = Av.size();

    for (int i = 0; i < constraints; ++i) {
      double a = 0;
      int variables = Av.size(i);
      for (int j = 0; j < variables; ++j)
        a += Ac.get(i, j) * x[Av.get(i, j)];
      if (boundTypes.get(i) == EQUALITY && a != bounds.get(i)
          || boundTypes.get(i) == LESS_THAN && a > bounds.get(i)
          || boundTypes.get(i) == GREATER_THAN && a < bounds.get(i))
        return false;
    }

    return true;
  }


  /**
    * Adds a new Boolean variable (an integer variable constrained to take
    * either the value 0 or the value 1) with the specified coefficient in the
    * objective function to the problem.
    *
    * @param c  The objective function coefficient for the new Boolean
    *           variable.
    * @return The index of the created variable.
   **/
  public int addBooleanVariable(double c) {
    objectiveCoefficients.add(c);
    return objectiveCoefficients.size() - 1;
  }


  /**
    * Adds a general, multi-valued discrete variable, which is implemented as
    * a set of Boolean variables, one per value of the discrete variable, with
    * exactly one of those variables set <code>true</code> at any given time.
    *
    * @param c  The objective function coefficients for the new Boolean
    *           variables.
    * @return The indexes of the newly created variables.
   **/
  public int[] addDiscreteVariable(double[] c) {
    int s = objectiveCoefficients.size();
    int[] result = new int[c.length];

    for (int i = 0; i < c.length; ++i) {
      objectiveCoefficients.add(c[i]);
      result[i] = s + i;
    }

    double[] a = new double[c.length];
    Arrays.fill(a, 1);
    addEqualityConstraint(result, a, 1);
    return result;
  }


  /**
    * Adds a general, multi-valued discrete variable, which is implemented as
    * a set of Boolean variables, one per value of the discrete variable, with
    * exactly one of those variables set <code>true</code> at any given time.
    *
    * @param c  An array of {@link LBJ2.classify.Score}s containing the
    *           objective function coefficients for the new Boolean variables.
    * @return The indexes of the newly created variables.
   **/
  public int[] addDiscreteVariable(LBJ2.classify.Score[] c) {
    double[] d = new double[c.length];
    for (int i = 0; i < c.length; ++i) d[i] = c[i].score;
    return addDiscreteVariable(d);
  }


  /**
    * Adds a typeless constraint to the problem.  No need to waste space
    * storing the types of constraints if they are implied or assumed.
    * Otherwise, this method does the same thing as
    * {@link #addConstraint(int[],double[],int,double)}.
    *
    * @param I  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  protected void addConstraint(final int[] I, double[] a, double b) {
    int[] indexes = new int[I.length];
    for (int i = 0; i < I.length; ++i) indexes[i] = i;
    Sort.sort(indexes,
              new Sort.IntComparator() {
                public int compare(int i1, int i2) { return I[i1] - I[i2]; }
              });

    int s = Av.size();

    for (int i = 0; i < I.length; ++i) {
      Av.set(s, i, I[indexes[i]]);
      double c = a[indexes[i]];
      double rounded = Math.round(c);
      if (Math.abs(c - rounded) < TOLERANCE) c = rounded;
      Ac.set(s, i, c);
    }

    bounds.add(b);
  }


  /**
    * Adds a new constraint of the specified type to the problem.  The two
    * array arguments must be the same length, as their elements correspond to
    * each other.  Variables whose coefficients are zero need not be
    * mentioned.  Variables that are mentioned must have previously been added
    * via {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a ?= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code>,
    * <code>*</code> represents dot product, and ?= stands for the type of the
    * constraint.
    *
    * <p> This method is called by the other constraint adding methods in this
    * class.  It sorts the variables and their coefficients so that the
    * presence of a given variable can be determined with
    * {@link LBJ2.util.IVector2D#binarySearch(int,int)}.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param t  The type of comparison in this constraint.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  protected void addConstraint(int[] i, double[] a, int t, double b) {
    addConstraint(i, a, b);
    boundTypes.add(t);
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
    addConstraint(i, a, EQUALITY, b);
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
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The lower bound for the new constraint.
   **/
  public void addGreaterThanConstraint(int[] i, double[] a, double b) {
    addConstraint(i, a, GREATER_THAN, b);
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
    addConstraint(i, a, LESS_THAN, b);
  }


  /**
    * This method evaluates the objective function on a potential (not
    * necessarily feasible) solution.
    *
    * @param x  The current settings of the inference variables.
    * @return The value of the objective function with these variable
    *         settings.
   **/
  public double evaluate(int[] x) {
    double result = 0;
    for (int i = 0; i < x.length; ++i)
      result += x[i] * objectiveCoefficients.get(i);
    return result;
  }


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
      buffer.append(" x_");
      buffer.append(i);
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

      buffer.append(" ");
      buffer.append(boundTypeSymbols[boundTypes.get(i)]);
      buffer.append(" ");
      buffer.append(bounds.get(i));
      buffer.append("\n");
    }
  }


  /** Returns the representation created by {@link #write(StringBuffer)}. */
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    write(buffer);
    return buffer.toString();
  }
}

