package LBJ2;


/**
  * This class holds configuration parameters determined by the
  * <code>configure</code> script.
  *
  * <p> <b>LBJ2/Configuration.java.  Generated from Configuration.java.in by configure.</b>
  *
  * @author Nick Rizzolo
 **/
public class Configuration
{
  /** The name of the Java compiler. */
  public static String javac = "javac";
  /** The name of the JVM executable. */
  public static String java = "java";
  /** Contains the version number of this software. */
  public static String packageVersion = "2.8.2";
  /** Whether GLPK is supported. */
  public static boolean GLPKLinked = "no" == "yes";
  /** Whether Xpress-MP is supported. */
  public static boolean XpressMPLinked = "no" == "yes";
  /** Whether Gurobi is supported. */
  public static boolean GurobiLinked = "no" == "yes";
  /** Whether WEKA is supported. */
  public static boolean WekaLinked = "no" == "yes";
  /** Whether liblinear is supported. */
  public static boolean LiblinearLinked = "no" == "yes";
  /** LBJ's web site. */
  public static String webSite = "http://cogcomp.cs.uiuc.edu/";
}

