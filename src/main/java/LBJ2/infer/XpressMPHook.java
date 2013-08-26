package LBJ2.infer;

import java.util.*;
import com.dashoptimization.*;
import LBJ2.util.DVector;
import LBJ2.util.DVector2D;
import LBJ2.util.IVector;
import LBJ2.util.IVector2D;


/**
  * This interface to Xpress-MP from
  * <a href="http://www.dashoptimization.com">Dash Optimization</a> is
  * designed to work with Xpress-Optimizer 15.25.02.  Make sure the file
  * <code>xprs.jar</code> is on your <code>CLASSPATH</code> and that the
  * Xpress-MP libraries are installed appropriately on your system before
  * attempting to compile and use this class.
  *
  * <p>
  * <i><b>Note to users:</b></i> When invoking a classifier in your
  * application that calls this algorithm to do inference, files whose names
  * start with <code>xmp</code> and end with <code>.sol</code> or
  * <code>.glb</code> may be created in the directory from which you invoked
  * your application.  These files are created by the Xpress-MP library, and I
  * haven't found a way to avoid it.
  *
  * <p>
  * Xpress-MP represents an ILP problem as a coefficient matrix in which rows
  * represent constraints and columns represent variables.  In the
  * documentation below, the terms "row" and "constraint" are used
  * interchangeably, as are the terms "column" and "variable".
  *
  * @author Nick Rizzolo
 **/
public class XpressMPHook implements ILPSolver
{
  /**
    * The elements of this array are suitable for use as elements of the
    * <code>qrtype</code> array parameter of the <code>XPRSloadglobal</code>
    * method.
   **/
  private static final byte[] typeCodes = { 'L', 'E', 'G' };
  /** Index into the {@link #typeCodes} array representing "less than". */
  private static final int ROWLESSTHAN = 0;
  /** Index into the {@link #typeCodes} array representing "equal". */
  private static final int ROWEQUAL = 1;
  /** Index into the {@link #typeCodes} array representing "greater than". */
  private static final int ROWGREATERTHAN = 2;

  /** Keeps track of the next ID number for an instance of this class. */
  private static int nextID = 0;


  /** The ID of an instance is used in the name of the solution file. */
  private int problemID;
  /**
    * Verbosity level.  {@link ILPInference#VERBOSITY_NONE} produces no
    * incidental output.  If set to {@link ILPInference#VERBOSITY_LOW}, only
    * variable and constraint counts are reported on <code>STDOUT</code>.  If
    * set to {@link ILPInference#VERBOSITY_HIGH}, a textual representation of
    * the entire optimization problem is also generated on
    * <code>STDOUT</code>.
   **/
  protected int verbosity;
  /**
    * Xpress-MP's ILP problem representation is stored here for easy access to
    * the solution values of variables.
   **/
  private XPRSprob problem;
  /** The values of the primal variables after solving the problem. */
  private double[] x;
  /**
    * Remembers whether the objective function should be maximized or
    * minimzed.
   **/
  private boolean maximize;
  /** Stores the objective function's coefficients. */
  private DVector objective;
  /** Stores the types of each constraint. */
  private IVector rowTypes;
  /** Stores the constant bounds on each constraint. */
  private DVector rhs;
  /**
    * Contains one vector for each added variable representing a list of row
    * indexes at which the associated variable has a non-zero coefficient.
   **/
  private XMPIVector2D rowIndexes;
  /**
    * Contains one vector for each added variable representing a list of
    * non-zero coefficients associated with the variable in the various
    * constraints in the problem.  These coefficients correspond do the row
    * indexes at the same locations in the 2D {@link #rowIndexes} array.
   **/
  private XMPDVector2D coefficients;
  /**
    * Contains one vector for each SOS1 in the ILP problem representing the
    * list of columns involved in the set.
   **/
  private XMPIVector2D setColumns;


  /** Default constructor. */
  public XpressMPHook() { this(ILPInference.VERBOSITY_NONE); }

  /**
    * Use this constructor to control printing of the problem's representation
    * before its solution is carried out.
    *
    * @param v  Setting for the {@link #verbosity} level.
   **/
  public XpressMPHook(int v) {
    verbosity = v;

    if (nextID == 0) {
      try { XPRS.init(); }
      catch (Exception e) {
        System.err.println("Couldn't initialize Xpress-MP: " + e);
        System.exit(1);
      }

      Runtime.getRuntime().addShutdownHook(
          new Thread() {
            public void run() { XPRS.free(); }
          });
    }

    problemID = nextID++;
    reset();
  }


  /**
    * This method clears the all constraints and variables out of the ILP
    * solver's problem representation, bringing the <code>ILPSolver</code>
    * instance back to the state it was in when first constructed.
   **/
  public void reset() {
    if (problem != null) {
      problemID = nextID++;
      problem = null;
    }

    x = null;
    objective = new DVector();
    rowTypes = new IVector();
    rhs = new DVector();
    rowIndexes = new XMPIVector2D();
    coefficients = new XMPDVector2D();
    setColumns = new XMPIVector2D();
  }


  /**
    * Sets the direction of the objective function.
    *
    * @param d  <code>true</code> if the objective function is to be
    *           maximized.
   **/
  public void setMaximize(boolean d) { maximize = d; }


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
    int result = objective.size();
    objective.add(c);
    return result;
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
    int sosIndex = setColumns.size();
    int[] result = new int[c.length];
    int variables = objective.size(), sosSize = setColumns.size(sosIndex);

    for (int i = 0; i < c.length; ++i) {
      result[i] = variables + i;
      setColumns.set(sosIndex, sosSize + i, variables + i);
      objective.add(c[i]);
    }

    double[] a = new double[c.length];
    Arrays.fill(a, 1);
    addGreaterThanConstraint(result, a, 1);
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
    int sosIndex = setColumns.size();
    int[] result = new int[c.length];
    int variables = objective.size(), sosSize = setColumns.size(sosIndex);

    for (int i = 0; i < c.length; ++i) {
      result[i] = variables + i;
      setColumns.set(sosIndex, sosSize + i, variables + i);
      objective.add(c[i].score);
    }

    double[] a = new double[c.length];
    Arrays.fill(a, 1);
    addGreaterThanConstraint(result, a, 1);
    return result;
  }


  /**
    * Adds a new constraint to the problem with the specified type.  This
    * method is called by all the other <code>add*Constraint()</code> methods.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The new constraint will enforce (in)equality with this
    *           constant.
    * @param t  The type of linear inequality constraint to add.
   **/
  public void addConstraint(int[] i, double[] a, double b, int t) {
    int newRow = rowTypes.size();
    rowTypes.add(t);
    rhs.add(b);

    for (int j = 0; j < i.length; ++j) {
      int s = rowIndexes.size(i[j]);
      rowIndexes.set(i[j], s, newRow);
      coefficients.set(i[j], s, a[j]);
    }
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
    addConstraint(i, a, b, ROWEQUAL);
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
    addConstraint(i, a, b, ROWGREATERTHAN);
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
    addConstraint(i, a, b, ROWLESSTHAN);
  }


  /**
    * Solves the ILP problem, saving the solution internally.
    *
    * @return <code>true</code> iff a solution was found successfully.
   **/
  public boolean solve() throws Exception {
    int ncol = objective.size();
    int nrow = rowTypes.size();
    byte[] qrtype = new byte[nrow];
    for (int i = 0; i < nrow; ++i)
      qrtype[i] = typeCodes[rowTypes.get(i)];
    double[] b = rhs.toArray();
    double[] obj = objective.toArray();
    int[] mstart = rowIndexes.indexMap();
    int[] mrwind = rowIndexes.flatten();
    double[] dmatval = coefficients.flatten();
    double[] dlb = new double[ncol];
    double[] dub = new double[ncol];
    Arrays.fill(dub, 1);
    int nsets = setColumns.size();
    byte[] qgtype = new byte[ncol];
    int[] mgcols = new int[ncol];

    Arrays.fill(qgtype, (byte) 'B');
    for (int i = 0; i < ncol; ++i) mgcols[i] = i;

    byte[] qstype = new byte[nsets];
    Arrays.fill(qstype, (byte) '1');
    int[] msstart = setColumns.indexMap();
    int[] mscols = setColumns.flatten();
    double[] dref = new double[mscols.length];
    for (int i = 0; i < dref.length; ++i) dref[i] = i + 1;

    if (verbosity > ILPInference.VERBOSITY_NONE) {
      System.out.println(" variables: " + obj.length);
      System.out.println(" constraints: " + b.length);
    }

    if (verbosity == ILPInference.VERBOSITY_HIGH) {
      StringBuffer buffer = new StringBuffer();
      write(buffer);
      System.out.print(buffer);

      /*
      System.out.println(
          "\nloadGlobal(\n"
          + "  \"\",\n"
          + "  " + ncol + ",\n"
          + "  " + nrow + ",\n"
          + "  " + arrayToString(qrtype) + ",\n"
          + "  " + arrayToString(b) + ",\n"
          + "  null,\n"
          + "  " + arrayToString(obj) + ",\n"
          + "  " + arrayToString(mstart) + ",\n"
          + "  null,\n"
          + "  " + arrayToString(mrwind) + ",\n"
          + "  " + arrayToString(dmatval) + ",\n"
          + "  " + arrayToString(dlb) + ",\n"
          + "  " + arrayToString(dub) + ",\n"
          + "  " + ncol + ",\n"
          + "  " + nsets + ",\n"
          + "  " + arrayToString(qgtype) + ",\n"
          + "  " + arrayToString(mgcols) + ",\n"
          + "  null,\n"
          + "  " + arrayToString(qstype) + ",\n"
          + "  " + arrayToString(msstart) + ",\n"
          + "  " + arrayToString(mscols) + ",\n"
          + "  " + arrayToString(dref) + ")");
      */
    }

    problem = new XPRSprob();

    try {
      problem.setIntControl(XPRS.BAROUTPUT, 0);
      problem.setIntControl(XPRS.MIPLOG, 0);
      problem.setIntControl(XPRS.OUTPUTLOG, 0);
    }
    catch (Exception e) {
      System.err.println("Can't set Xpress-MP output control: " + e);
      System.err.println("Xpress-MP will probably be very verbose.");
    }

    String id = "" + problemID;
    while (id.length() < 5) id = "0" + id;
    id = "xmp" + id;
    problem.loadGlobal(id, ncol, nrow, qrtype, b, null, obj, mstart, null,
                       mrwind, dmatval, dlb, dub, ncol, nsets, qgtype, mgcols,
                       null, qstype, msstart, mscols, dref);

    if (maximize) problem.maxim("g");
    else problem.minim("g");

    int status = problem.getIntAttrib(XPRS.MIPSTATUS);
    return status == XPRS.MIP_OPTIMAL || status == XPRS.MIP_SOLUTION;
  }


  /**
    * Generates a string representation of the given array.
    *
    * @param a  The array to represent as a string.
    * @return A string representation of <code>a</code>.
   **/
  private static String arrayToString(byte[] a) {
    String result = "[";

    for (int i = 0; i < a.length; ++i) {
      result += " " + (char) a[i];
      if (i < a.length - 1) result += ",";
    }

    result += " ]";
    return result;
  }


  /**
    * Generates a string representation of the given array.
    *
    * @param a  The array to represent as a string.
    * @return A string representation of <code>a</code>.
   **/
  private static String arrayToString(int[] a) {
    String result = "[";

    for (int i = 0; i < a.length; ++i) {
      result += " " + a[i];
      if (i < a.length - 1) result += ",";
    }

    result += " ]";
    return result;
  }


  /**
    * Generates a string representation of the given array.
    *
    * @param a  The array to represent as a string.
    * @return A string representation of <code>a</code>.
   **/
  private static String arrayToString(double[] a) {
    String result = "[";

    for (int i = 0; i < a.length; ++i) {
      result += " " + a[i];
      if (i < a.length - 1) result += ",";
    }

    result += " ]";
    return result;
  }


  /**
    * Tests whether the problem represented by this <code>ILPSolver</code>
    * instance has been solved already.
   **/
  public boolean isSolved() { return problem != null; }


  /**
    * When the problem has been solved, use this method to retrieve the value
    * of any Boolean inference variable.  The result of this method is
    * undefined when the problem has not yet been solved.
    *
    * @param index  The index of the variable whose value is requested.
    * @return The value of the variable.
   **/
  public boolean getBooleanValue(int index) {
    if (x == null) {
      x = new double[objective.size()];
      try { problem.getSol(x, null, null, null); }
      catch (Exception e) {
        System.err.println("Couldn't get Xpress-MP problem solution: " + e);
        System.exit(1);
      }
    }

    return x[index] > 0.5;
  }


  /**
    * When the problem has been solved, use this method to retrieve the value
    * of the objective function at the solution.  The result of this method is
    * undefined when the problem has not yet been solved.  If the problem had
    * no feasible solutions, negative (positive, respectively) infinity will
    * be returned if maximizing (minimizing).
    *
    * @return The value of the objective function at the solution.
   **/
  public double objectiveValue() {
    int status = problem.getIntAttrib(XPRS.MIPSTATUS);
    if (status == XPRS.MIP_INFEAS)
      return maximize ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    if (status == XPRS.MIP_OPTIMAL || status == XPRS.MIP_SOLUTION)
      return problem.getDblAttrib(XPRS.MIPOBJVAL);
    return 0;
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

    int variables = objective.size();
    for (int i = 0; i < variables; ++i) {
      double c = objective.get(i);
      buffer.append(" ");
      if (c >= 0) buffer.append("+");
      buffer.append(c);
      buffer.append(" x_");
      buffer.append(i);
    }

    buffer.append("\n");

    int sosSets = setColumns.size();
    for (int i = 0; i < sosSets; ++i) {
      buffer.append(" atmost 1 of (x in {");
      int sosSize = setColumns.size(i);
      for (int j = 0; j < sosSize; ++j) {
        buffer.append("x_");
        buffer.append(setColumns.get(i, j));
        if (j + 1 < sosSize) buffer.append(", ");
      }
      buffer.append("}) (x > 0)\n");
    }

    int constraints = rowTypes.size();
    StringBuffer[] rowBuffers = new StringBuffer[constraints];
    for (int i = 0; i < rowBuffers.length; ++i) {
      rowBuffers[i] = new StringBuffer();
      rowBuffers[i].append(" ");
    }

    for (int i = 0; i < variables; ++i) {
      int rows = rowIndexes.size(i);
      for (int j = 0; j < rows; ++j) {
        int r = rowIndexes.get(i, j);
        double c = coefficients.get(i, j);
        rowBuffers[r].append(" ");
        if (c >= 0) rowBuffers[r].append("+");
        rowBuffers[r].append(c);
        rowBuffers[r].append(" x_");
        rowBuffers[r].append(i);
      }
    }

    for (int i = 0; i < constraints; ++i) {
      int t = rowTypes.get(i);
      if (typeCodes[t] == 'L') rowBuffers[i].append(" <= ");
      else if (typeCodes[t] == 'E') rowBuffers[i].append(" = ");
      else if (typeCodes[t] == 'G') rowBuffers[i].append(" >= ");
      rowBuffers[i].append(rhs.get(i));
    }

    for (int i = 0; i < rowBuffers.length; ++i) {
      buffer.append(rowBuffers[i]);
      buffer.append("\n");
    }
  }


  /**
    * Gives <code>DVector2D</code>s the ability to {@link #flatten() flatten}
    * and to produce {@link #indexMap() index maps} into the flattened
    * representation.
    *
    * @author Nick Rizzolo
   **/
  private static class XMPDVector2D extends DVector2D
  {
    /** Creates the vector. */
    public XMPDVector2D() { }


    /**
      * The array returned by this method contains indexes into the array
      * returned by {@link #flatten()} indicating where each subvector began
      * and ended before they were flattened.
     **/
    public int[] indexMap() {
      int size = sizes.size();
      int[] result = new int[size + 1];
      for (int i = 0; i < size; ++i)
        result[i + 1] = result[i] + sizes.get(i);
      return result;
    }


    /**
      * Returns all elements in all vectors in a single, one dimensional array
      * in the appropriate order.
     **/
    public double[] flatten() {
      int size = sizes.size();
      int totalElements = 0;
      for (int i = 0; i < size; ++i)
        totalElements += sizes.get(i);
      double[] result = new double[totalElements];

      totalElements = 0;
      for (int i = 0; i < size; ++i) {
        int s = sizes.get(i);
        System.arraycopy(vector[i], 0, result, totalElements, s);
        totalElements += s;
      }

      return result;
    }
  }


  /**
    * Gives <code>IVector2D</code>s the ability to {@link #flatten() flatten}
    * and to produce {@link #indexMap() index maps} into the flattened
    * representation.
    *
    * @author Nick Rizzolo
   **/
  private static class XMPIVector2D extends IVector2D
  {
    /** Creates the vector. */
    public XMPIVector2D() { }


    /**
      * The array returned by this method contains indexes into the array
      * returned by {@link #flatten()} indicating where each subvector began
      * and ended before they were flattened.
     **/
    public int[] indexMap() {
      int size = sizes.size();
      int[] result = new int[size + 1];
      for (int i = 0; i < size; ++i)
        result[i + 1] = result[i] + sizes.get(i);
      return result;
    }


    /**
      * Returns all elements in all vectors in a single, one dimensional array
      * in the appropriate order.
     **/
    public int[] flatten() {
      int size = sizes.size();
      int totalElements = 0;
      for (int i = 0; i < size; ++i)
        totalElements += sizes.get(i);
      int[] result = new int[totalElements];

      totalElements = 0;
      for (int i = 0; i < size; ++i) {
        int s = sizes.get(i);
        System.arraycopy(vector[i], 0, result, totalElements, s);
        totalElements += s;
      }

      return result;
    }
  }
}

