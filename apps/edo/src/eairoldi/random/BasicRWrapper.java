/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package eairoldi.random;


/**
 * @author Edoardo Airoldi
 * Date: Dec 14, 2004
 */

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import edu.cmu.minorthird.classify.algorithms.random.Estimate;
import edu.cmu.minorthird.classify.algorithms.random.Estimators;
import edu.cmu.minorthird.classify.algorithms.random.RWrapper;

public class BasicRWrapper implements RWrapper
{
   private Rengine re;

   public BasicRWrapper()
   {
      String [] args = new String [] {"--no-save"};
      System.out.println("Creating Rengine (with arguments)");
      //this.re=new Rengine(args, true, new TextConsole());
      this.re=new Rengine(args, true, null);
      System.out.println("Rengine created, waiting for R");
      if (!re.waitForR()) {
         System.err.println("Cannot load R");
      }
      System.out.println("R is ready");
   }

   public void end()
   {
      re.end();
   }

   //
   // Random number generators
   //

   public double [] rnorm(int n, double mu, double sd)
   {
      String exp = "rnorm("+n+",mean="+mu+",sd="+sd+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v;
   }

   public double [] runif(int n, double min, double max)
   {
      String exp = "runif("+n+",min="+min+",max="+max+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v;
   }

   public double [] rbinom(int n, double mu, double delta)
   {
      double N = Math.round(mu/delta);
      double p = delta;
      String exp = "rbinom("+n+",size="+N+",prob="+p+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v;
   }

   public double [] rnbinom(int n, double mu, double delta)
   {
      double N = Math.round(mu/delta);
      double p = 1.0/(1.0+delta);
      //String exp = "rnbinom("+n+",size="+N+",mu="+mu+")";
      String exp = "rnbinom("+n+",size="+N+",prob="+p+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v;
   }

   //
   // Densities
   //

   public double [] dnorm(double[] t, double mu, double sd, String string)
   {
      // string must be TRUE or FALSE
      if (!string.equals("TRUE") & !string.equals("FALSE") & !string.equals(""))
      {
         System.out.println("Error: String must be \"TRUE\", \"FALSE\" or \"\"!");
         System.exit(1);
      }

      long xp1 = re.rniPutDoubleArray(t);
      re.rniAssign("t", xp1, 0);

      String exp = "dnorm(t, mean="+mu+", sd="+sd+", log = "+string+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v;
   }

   public double dnorm(double t, double mu, double sd, String string)
   {
      // string must be TRUE or FALSE
      if (!string.equals("TRUE") & !string.equals("FALSE") & !string.equals(""))
      {
         System.out.println("Error: String must be \"TRUE\", \"FALSE\" or \"\"!");
         System.exit(1);
      }

      String exp = "dnorm("+t+", mean="+mu+", sd="+sd+", log="+string+")";

      long e=re.rniParse(exp, 1);
      long r=re.rniEval(e, 0);
      REXP x=new REXP(re, r);

      double [] v = x.asDoubleArray();
      return v[0];
   }

   //
   // Gamma related functions
   //


   //
   // Test RNG and corresponding Estimators
   //

   public  static void main(String[] args)
   {
      BasicRWrapper rw = new BasicRWrapper();

      // test negative-binomial
      double [] v = rw.rnbinom(10000,7.0,0.15);
      double [] w = new double[10000];
      for (int i=0; i<v.length; i++)
      {
         w[i]=1.0;
      }
      Estimate e = Estimators.estimateNegativeBinomialMuDelta(v,w,0.0,1.0);
      System.out.println(e.toTableInViewer());

      // test binomial
      v = rw.rbinom(10000,5.0,0.5);
      w = new double[10000];
      for (int i=0; i<v.length; i++)
      {
         w[i]=1.0;
      }
      e = Estimators.estimateBinomialMuDelta(v,w,0.0,1.0);
      System.out.println(e.toTableInViewer());

      // test densities
      double t = 0.5;
      double d = rw.dnorm(t,0,1,"TRUE");
      System.out.println(t+" = "+d);

      double[] tt = new double[]{-3,-2,-1,0,1,2,3};
      double[] dd = rw.dnorm(tt,0,1,"TRUE");
      for (int i=0; i<dd.length; i++)
      {
         System.out.println(tt[i]+" = "+dd[i]);
      }

      /* test Probability Factory
      ProbabilityFactory pr = new ProbabilityFactory();
      double[] c = new double[1000];
      for(int i=0; i<1000; i++)
      {
         double mu=0;
         double sd=1;
         c[i] = ProbabilityFactory.rnorm(1,mu,sd)[0];
         //System.out.println("value="+c[i]);
      }
      System.out.println("mean estimate="+Estimators.estimateMean(c));
      c = ProbabilityFactory.rnorm(1000,0,1);
      System.out.println("mean estimate="+Estimators.estimateMean(c));*/

      rw.end();
      System.exit(0);
   }
}
