package LBJ2.util;


/**
  * A collection of statistical methods supporting computations related to the
  * Student's T distribution.
  *
  * <table cellspacing=4>
  *   <tr>
  *     <th>Date:</th>
  *     <td>2002 as part of Fmath</td>
  *   </tr>
  *   <tr>
  *     <th>Amended:</th>
  *     <td>
  *       12 May 2003 Statistics separated out from Fmath as a new class
  *     </td>
  *   </tr>
  *   <tr>
  *     <th>Update:</th>
  *     <td>
  *       18 June 2005, 5 January 2006, 25 April 2006, 12, 21 November 2006,
  *       4 December 2006 (renaming of cfd and pdf methods - older version
  *       also retained), 31 December 2006, March 2007, 14 April 2007
  *     </td>
  *   </tr>
  * </table>
  *
  * <h4>Documentation</h4>
  * <p> See Michael Thomas Flanagan's Java library on-line web page:<br>
  * <a target=_top href="http://www.ee.ucl.ac.uk/~mflanaga/java/Stat.html">http://www.ee.ucl.ac.uk/~mflanaga/java/Stat.html</a>
  * <a target=_top href="http://www.ee.ucl.ac.uk/~mflanaga/java/">http://www.ee.ucl.ac.uk/~mflanaga/java/</a>
  *
  * <p> Copyright &copy; April 2004, June 2005, January 2006, December 2006,
  * April 2007
  *
  * <h4>Permission to Copy</h4>
  * <p> Permission to use, copy and modify this software and its documentation
  * for NON-COMMERCIAL purposes is granted, without fee, provided that an
  * acknowledgement to the author, Michael Thomas Flanagan at
  * <a target=_top href="http://www.ee.ucl.ac.uk/~mflanaga">www.ee.ucl.ac.uk/~mflanaga</a>,
  * appears in all copies.
  *
  * <p> Dr. Michael Thomas Flanagan makes no representations about the
  * suitability or fitness of the software for any or for a particular
  * purpose.  Michael Thomas Flanagan shall not be liable for any damages
  * suffered as a result of using, modifying or distributing this software or
  * its derivatives.
  *
  * @author Dr. Michael Thomas Flanagan
 **/
public class StudentT
{
  /**
    * A small number close to the smallest representable floating point
    * number.
   **/
  public static final double FPMIN = 1e-300;
  /** Lanczos Gamma Function approximation - N (number of coefficients -1) */
  private static int lgfN = 6;
  /** Lanczos Gamma Function approximation - Coefficients */
  private static double[] lgfCoeff = {1.000000000190015, 76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179E-2, -0.5395239384953E-5};
  /** Lanczos Gamma Function approximation - small gamma */
  private static double lgfGamma = 5.0;

  /** returns -1 if x &lt; 0 else returns 1 (double version) */
  public static double sign(double x){
    if (x<0.0){
      return -1.0;
    }
    else{
      return 1.0;
    }
  }

  /**
    * factorial of n.  Argument is of type double but must be, numerically, an
    * integer factorial returned as double but is, numerically, should be an
    * integer numerical rounding may makes this an approximation after n = 21
   **/
  public static double factorial(double n){
    if(n<0 || (n-Math.floor(n))!=0)throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?");
    double f = 1.0D;
    double iCount = 2.0D;
    while(iCount<=n){
      f*=iCount;
      iCount += 1.0D;
    }
    return f;
  }

  /**
    * log to base e of the factorial of n.  Argument is of type double but
    * must be, numerically, an integer log[e](factorial) returned as double
    * numerical rounding may makes this an approximation
   **/
  public static double logFactorial(double n){
    if(n<0 || (n-Math.floor(n))!=0)throw new IllegalArgumentException("\nn must be a positive integer\nIs a Gamma funtion [Fmath.gamma(x)] more appropriate?");
    double f = 0.0D;
    double iCount = 2.0D;
    while(iCount<=n){
      f+=Math.log(iCount);
      iCount += 1.0D;
    }
    return f;
  }

  /** Gamma function, Lanczos approximation (6 terms) */
  public static double gamma(double x){

    double xcopy = x;
    double first = x + lgfGamma + 0.5;
    double second = lgfCoeff[0];
    double fg = 0.0D;

    if(x>=0.0){
      if(x>=1.0D && x-(int)x==0.0D){
        fg = factorial(x)/x;
      }
      else{
        first = Math.pow(first, x + 0.5)*Math.exp(-first);
        for(int i=1; i<=lgfN; i++)second += lgfCoeff[i]/++xcopy;
        fg = first*Math.sqrt(2.0*Math.PI)*second/x;
      }
    }
    else{
      fg = -Math.PI/(x*gamma(-x)*Math.sin(Math.PI*x));
    }
    return fg;
  }

  /**
    * log to base e of the Gamma function, Lanczos approximation (6 terms).
    * Retained for backward compatibility.
   **/
  public static double logGamma(double x){
    double xcopy = x;
    double fg = 0.0D;
    double first = x + lgfGamma + 0.5;
    double second = lgfCoeff[0];

    if(x>=0.0){
      if(x>=1.0 && x-(int)x==0.0){
        fg = logFactorial(x)-Math.log(x);
      }
      else{
        first -= (x + 0.5)*Math.log(first);
        for(int i=1; i<=lgfN; i++)second += lgfCoeff[i]/++xcopy;
        fg = Math.log(Math.sqrt(2.0*Math.PI)*second/x) - first;
      }
    }
    else{
      fg = Math.PI/(gamma(1.0D-x)*Math.sin(Math.PI*x));

      if(fg!=1.0/0.0 && fg!=-1.0/0.0){
        if(fg<0){
          throw new IllegalArgumentException("\nThe gamma function is negative");
        }
        else{
          fg = Math.log(fg);
        }
      }
    }
    return fg;
  }

  /**
    * Incomplete fraction summation used in the method
    * {@link #regularisedBetaFunction(double,double,double)}.  modified
    * Lentz's method
   **/
  public static double contFract(double a, double b, double x){
    int maxit = 500;
    double eps = 3.0e-7;
    double aplusb = a + b;
    double aplus1 = a + 1.0D;
    double aminus1 = a - 1.0D;
    double c = 1.0D;
    double d = 1.0D - aplusb*x/aplus1;
    if(Math.abs(d)<FPMIN)d = FPMIN;
    d = 1.0D/d;
    double h = d;
    double aa = 0.0D;
    double del = 0.0D;
    int i=1, i2=0;
    boolean test=true;
    while(test){
      i2=2*i;
      aa = i*(b-i)*x/((aminus1+i2)*(a+i2));
      d = 1.0D + aa*d;
      if(Math.abs(d)<FPMIN)d = FPMIN;
      c = 1.0D + aa/c;
      if(Math.abs(c)<FPMIN)c = FPMIN;
      d = 1.0D/d;
      h *= d*c;
      aa = -(a+i)*(aplusb+i)*x/((a+i2)*(aplus1+i2));
      d = 1.0D + aa*d;
      if(Math.abs(d)<FPMIN)d = FPMIN;
      c = 1.0D + aa/c;
      if(Math.abs(c)<FPMIN)c = FPMIN;
      d = 1.0D/d;
      del = d*c;
      h *= del;
      i++;
      if(Math.abs(del-1.0D) < eps)test=false;
      if(i>maxit){
        test=false;
        System.out.println("Maximum number of iterations ("+maxit+") exceeded in Stat.contFract in Stat.incomplete Beta");
      }
    }
    return h;
  }

  /**
    * Regularised Incomplete Beta function.  Continued Fraction approximation
    * (see Numerical recipies for details of method)
   **/
  public static double regularisedBetaFunction(double z, double w, double x){
    if(x<0.0D || x>1.0D)throw new IllegalArgumentException("Argument x, "+x+", must be lie between 0 and 1 (inclusive)");
    double ibeta = 0.0D;
    if(x==0.0D){
      ibeta=0.0D;
    }
    else{
      if(x==1.0D){
        ibeta=1.0D;
      }
      else{
        // Term before continued fraction
        ibeta = Math.exp(logGamma(z+w) - logGamma(z) - logGamma(w) + z*Math.log(x) + w*Math.log(1.0D-x));
        // Continued fraction
        if(x < (z+1.0D)/(z+w+2.0D)){
          ibeta = ibeta*contFract(z, w, x)/z;
        }
        else{
          // Use symmetry relationship
          ibeta = 1.0D - ibeta*contFract(w, z, 1.0D-x)/w;
        }
      }
    }
    return ibeta;
  }

  // STUDENT'S T DISTRIBUTION

  /** Returns the Student's t cumulative distribution function probability */
  public static double studentTcdf(double tValue, int df){
    double ddf = (double)df;
    double x = ddf/(ddf+tValue*tValue);
    return 0.5D*(1.0D + (regularisedBetaFunction(ddf/2.0D, 0.5D, 1) - regularisedBetaFunction(ddf/2.0D, 0.5D, x))*sign(tValue));
  }


  /**
    * Computes the multiplier for the standard error of the mean when finding
    * a <i>(1 - alpha) * 100%</i> confidence interval.
    *
    * @param df     The degrees of freedom.
    * @param alpha  The fraction of the distribution to leave outside the
    *               interval.
    * @return <i>m</i> such that <i>mu +- m s</i> represents a
    *         <i>(1 - alpha) * 100%</i> confidence interval, where <i>mu</i>
    *         is the sample mean and <i>s</i> is the sample's standard
    *         deviation.
   **/
  public static double tTable(int df, double alpha) {
    double c = 1 - alpha / 2.0;
    double max = 700, min = -700;
    boolean same = false;

    while (!same) {
      double mid = (max + min) / 2.0;
      if (studentTcdf(mid, df) < c) {
        same = min == mid;
        min = mid;
      }
      else {
        same = max == mid;
        max = mid;
      }
    }

    return (max + min) / 2.0;
  }


  /**
    * Computes the confidence interval of the specified precision over a set
    * of data points.
    *
    * @param x      The data points.
    * @param alpha  The fraction of the distribution to leave outside the
    *               interval.
    * @return An array containing the mean of the elements in <code>x</code>
    *         and half of the size of the confidence interval over
    *         <code>x</code>.  If this array is named <code>r</code>, then the
    *         confidence interval can be stated as <code>r[0] +/- r[1]</code>.
   **/
  public static double[] confidenceInterval(double[] x, double alpha) {
    double mean = 0;
    // Compute the average.
    for (int i = 0; i < x.length; ++i) mean += x[i];
    mean /= (double) x.length;

    // Compute standard deviation and confidence interval.
    // s: the standard deviation of the testing results
    double s = 0.0;
    for (int i = 0; i < x.length; ++i) {
      double d = x[i] - mean;
      s += d * d;
    }
    s /= (double) (x.length - 1);
    s = Math.sqrt(s);

    // sem: estimated standard error of the mean
    double sem = s / Math.sqrt(x.length);
    double t = tTable(x.length - 1, alpha);
    return new double[]{ mean, t * sem };
  }
}

