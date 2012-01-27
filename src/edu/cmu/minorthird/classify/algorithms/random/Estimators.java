package edu.cmu.minorthird.classify.algorithms.random;

import java.util.TreeMap;

/**
 * @author Edoardo Airoldi
 * Date: Dec 11, 2004
 */

public class Estimators
{

   public static Estimate estimateBinomialPN( double[] vCnt, double[] vWgt, double prior, double scale)
   {
      double p = 0.0;
      double N = 0.0;

      // compute mean
      double sumX = 0.0;
      double sumWgt = 0.0;
      double maxCnt=0;
      for (int i=0; i<vCnt.length; i++)
      {
         sumX += vCnt[i];
         sumWgt += vWgt[i];
         maxCnt=Math.max(vCnt[i],maxCnt);
      }
      p = (sumX+prior*1.0/scale)/(sumWgt+1.0/scale);

      double mean = estimateMean(vCnt);
      double var = estimateVar(vCnt);

      //p = (mean - var)/mean;
      N = Math.round( Math.max( maxCnt,Math.min( 25.0,Math.pow(mean,2)/(mean-var) ))) +1; // also use p instead of mean
      //N = Math.round( Math.max( maxCnt,Math.pow(mean,2)/(mean-var) )); // +1; also use p instead of mean
      //System.out.println("p="+p+" N="+N);
      //System.out.println("len="+wgt.length);

      // package results
      TreeMap<String,Double> pn = new TreeMap<String,Double>();
      pn.put( "p",new Double(p) );
      pn.put( "N",new Double(N) );
      //System.out.println("p="+p+" N="+N);
      return new Estimate("Binomial","p/N",pn);
   }

   public static Estimate estimateBinomialMuDelta( double[] vCnt, double[] vWgt, double prior, double scale)
   {
      double m = 0.0;
      double d = 0.0;

      // compute mean
      int N = vCnt.length;
      double sumX = 0.0;
      double sumWgt = 0.0;
      double sumWgt2 = 0.0;
      for (int i=0; i<N; i++)
      {
         sumX += vCnt[i];
         sumWgt += vWgt[i];
         sumWgt2 += Math.pow( vWgt[i],2 );
      }
      m=(sumX+prior*1.0/scale)/(sumWgt+1.0/scale);

      // compute intermediate
      double r;
      double v=0.0;
      if (N<=1.0)
      {
         r = 0.0;
         v = 0.0;
      }
      else
      {
         r = (sumWgt - sumWgt2/sumWgt) / (N-1.0);
         for(int i=0; i<N; i++)
         {
            v += ( vWgt[i] * Math.pow( vCnt[i]/vWgt[i]-m,2 ) ) / (N-1.0);
            //v += ( vWgt[i] * Math.pow( vCnt[i]/vWgt[i]-0.0,2 ) ) / (N-1.0);
         }
      }

      // compute variance
      d = Math.max( 0.0,(m-v)/(r*m) );
      if (new Double(d).isNaN()) { d=0.0; }

      // package results
      TreeMap<String,Double> mudelta = new TreeMap<String,Double>();
      mudelta.put( "mu",new Double(m) );
      mudelta.put( "delta",new Double(d) );
      //System.out.println("m="+m+" d="+d);
      return new Estimate("Binomial","mu/delta",mudelta);
   }

   public static Estimate estimateNegativeBinomialMuDelta( double[] vCnt, double[] vWgt, double prior, double scale)
   {
      double m = 0.0;
      double d = 0.0;

      // compute mean
      int N = vCnt.length;
      double sumX = 0.0;
      double sumWgt = 0.0;
      double sumWgt2 = 0.0;
      for (int i=0; i<N; i++)
      {
         sumX += vCnt[i];
         sumWgt += vWgt[i]/scale;
         sumWgt2 += Math.pow( vWgt[i]/scale,2 );
      }
      m=(sumX+prior*1.0/scale)/(sumWgt+1.0/scale);

      /*StringBuffer str = new StringBuffer(""+vCnt[0]);
      for(int i=1; i<vCnt.length; i++)
      {
      str.append(" "+vCnt[i]);
      }
      System.out.println("["+str+"]"); */
      //System.out.println(". sumX="+sumX+",sumWgt="+sumWgt+",m="+m+",d="+d);
      // compute intermediate
      double r;
      double v=0.0;
      if (N<=1.0)
      {
         r = 0.0;
         v = 0.0;
      }
      else
      {
         r = (sumWgt - sumWgt2/sumWgt) / (N-1.0);
         for(int i=0; i<N; i++)
         {
            v += ( (vWgt[i]/scale) * Math.pow( vCnt[i]/(vWgt[i]/scale)-m,2 ) ) / (N-1.0);
            //v += ( vWgt[i] * Math.pow( vCnt[i]/vWgt[i]-0.0,2 ) ) / (N-1.0);
         }
      }

      // compute variance
      d = Math.max( 0.0,(v-m)/(r*m) );
      if (new Double(d).isNaN()) { d=0.0; }
      if (d==0.0) { d=1e-7; }

      // package results
      TreeMap<String,Double> mudelta = new TreeMap<String,Double>();
      mudelta.put( "mu",new Double(m) );
      mudelta.put( "delta",new Double(d) );
      //System.out.println("m="+m+" d="+d);
      return new Estimate("Negative-Binomial","mu/delta",mudelta);
   }

   public static Estimate estimatePoissonLambda(double classPrior, double numberOfClasses, double observedCounts, double totalCounts)
   {
      //double lambda = (classPrior+observedCounts)/(numberOfClasses+totalCounts);
      double lambda = (classPrior/numberOfClasses+observedCounts)/(classPrior+totalCounts);
      // package results
      TreeMap<String,Double> tm = new TreeMap<String,Double>();
      tm.put( "lambda",new Double(lambda) );
      return new Estimate("Poisson","lambda",tm);
   }

   public static Estimate estimatePoissonWeightedLambda( double[] vCnt, double[] vWgt, double prior, double scale)
   {
      double lambda = 0.0;

      // compute mean
      int N = vCnt.length;
      double sumX = 0.0;
      double sumWgt = 0.0;
      for (int i=0; i<N; i++)
      {
         sumX += vCnt[i];
         sumWgt += vWgt[i] /scale;
      }
      lambda=(sumX+prior*1.0/scale)/(sumWgt+1.0/scale);

      // package results
      TreeMap<String,Double> tm = new TreeMap<String,Double>();
      tm.put( "lambda",new Double(lambda) );
      return new Estimate("Poisson","weighted-lambda",tm);
   }

   public static Estimate estimateNaiveBayesMean(double classPrior, double numberOfClasses, double observedCounts, double totalCounts)
   {
      //double mean = (classPrior+observedCounts)/(numberOfClasses+totalCounts); //
      double mean = (classPrior/numberOfClasses+observedCounts)/(1.0+totalCounts);
      // package results
      TreeMap<String,Double> tm = new TreeMap<String,Double>();
      tm.put( "mean",new Double(mean) );
      return new Estimate("Naive-Bayes","mean",tm);
   }

   public static Estimate estimateNaiveBayesWeightedMean( double[] vCnt, double[] vWgt, double prior, double scale)
   {
      double mean = 0.0;

      // compute mean
      int N = vCnt.length;
      double sumX = 0.0;
      double sumWgt = 0.0;
      for (int i=0; i<N; i++)
      {
         sumX += vCnt[i];
         sumWgt += vWgt[i] /scale;
      }
      mean=(sumX+prior*1.0/scale)/(sumWgt+1.0/scale);

      // package results
      TreeMap<String,Double> tm = new TreeMap<String,Double>();
      tm.put( "mean",new Double(mean) );
      return new Estimate("Naive-Bayes","weighted-mean",tm);
   }



   //
   // Probabilities for the Dirichlet-Poisson model (Airoldi, Cohen & Fienberg 2005)
   //

   public static Estimate [] mcmcEstimateDirichletPoissonTauSigma
       (Estimate[] lambdaEstimateVec, double[] vlow, double[] vup, double xr, double xp, double wr, double wp, double[] vbeta, double tauSD, double sigmaSD, int numIterations)
   {
      // initialize a Probability Factory
 //     ProbabilityFactory pr = new ProbabilityFactory();

      // initialize variables fo MCMC
      int K = lambdaEstimateVec.length;
      double[][] tau = new double[ K-1 ][ numIterations ];
      double[] sig = new double[ numIterations ];
      double[] tauAcceptRate = new double[ K-1 ];
      double sigAcceptRate = 0.0;

      // compute starting guesses from Lambda estimates
      String parameterization = lambdaEstimateVec[0].getParameterization();
      Estimate[] tauSigmaEstimate = ReparametrizeLambdas2TauSig(lambdaEstimateVec);
//      StringBuffer buf;
      //buf = new StringBuffer("initial values ::");
      for (int i=0; i<(K-1); i++)
      {
         tau[i][0] = (tauSigmaEstimate[i].getPms().get("tau") ).doubleValue();
         //buf.append(" tau["+i+"] = "+tau[i][0]+",");
      }
      sig[0] = (tauSigmaEstimate[0].getPms().get("sigma") ).doubleValue();
      //buf.append(" sigma = "+sig[0]);
      //System.out.println(buf);

      // iterate MCMC
      for (int it=1; it<numIterations; it++) //in 1:iter
      {
         // print current values to screen
         /*buf = new StringBuffer("chain :: ");
         for (int i=0; i<(K-1); i++)
         {
            buf.append( "tau="+tau[i][(it-1)]+" " );
         }
         buf.append("sigma="+sig[(it-1)]);
         System.out.println(buf);*/

         double candidate, u, p;
         for (int i=0; i<(K-1); i++)
         {
            // sample from tau
            candidate = Math.max( 1e-7,ProbabilityFactory.rnorm(1, tau[i][it-1],tauSD)[0] );
            u = ProbabilityFactory.runif(1, 0,1)[0];
            p = ProbabilityFactory.AlphaTau(tau[i][it-1], sig[it-1],vlow,vup,xr,xp,wr,wp,vbeta,tau[i][it-1],tauSD, candidate);
            //System.out.println("tau["+i+"]: candidate="+candidate+" u="+u+" p="+p);
            if( u <= p )
            {
               tau[i][it] = candidate;
               tauAcceptRate[i] += 1.0 / numIterations;
            }
            else
            {
               tau[i][it] = tau[i][it-1];
            }
         }

         // sample from sigma
         candidate = Math.max( 1e-7,ProbabilityFactory.rnorm(1, sig[it-1],sigmaSD)[0] );
         u = ProbabilityFactory.runif(1, 0,1)[0];
         p = ProbabilityFactory.AlphaSigma(sig[it-1], tau[0][it],vlow,vup,xr,xp,wr,wp,vbeta,sig[it-1],sigmaSD, candidate); // modify for K>2
         //System.out.println("sig: candidate="+candidate+" u="+u+" p="+p);
         if( u <= p )
         {
            sig[it] = candidate;
            sigAcceptRate += 1.0 / numIterations;
         }
         else
         {
            sig[it] = sig[it-1];
         }
      }

      // compute posterior means
      double[] tauPost = new double[ K-1 ];
      double sigmaPost = 0;

      //double perc = 0.95;  // controls % of iterations to return
      //int tail = (int) Math.floor(numIterations*(1-perc)); // return [,tail:iter]

      for (int i=0; i<(K-1); i++)
      {
         /*double[] v = new double[ tau[i].length-tail+1 ];
         for (int j=tail; j<tau[i].length; j++) { v[j]=tau[i][j-tail]; }
         tauPost[i] = estimateMean(v);*/
         tauPost[i] = estimateMean(tau[i]);
      }
      sigmaPost = estimateMean(sig);
      /*buf = new StringBuffer("final values ::");
      for (int i=0; i<(K-1); i++)
      {
         buf.append( " tau["+i+"] = "+tauPost[i]+"," );
      }
      buf.append("sigma = "+sigmaPost);
      System.out.println(buf);*/

      // report acceptance rates
      /*buf = new StringBuffer("acceptance rates ::");
      for (int i=0; i<(K-1); i++)
      {
         buf.append( " tau["+i+"] = "+tauAcceptRate[i]+"," );
      }
      buf.append(" sigma = "+sigAcceptRate);
      System.out.println(buf);*/

      // package results
      Estimate[] tauSigma = new Estimate[ K-1];
      for (int i=0; i<(K-1); i++)
      {
         TreeMap<String,Double> tm = new TreeMap<String,Double>();
         tm.put( "tau",new Double(tauPost[i]) );
         tm.put( "sigma",new Double(sigmaPost) );
         tauSigma[i] = new Estimate("Dirichlet-Poisson MCMC","tau/sigma",tm);
      }
      return ReparametrizeTauSig2Lambdas(tauSigma,parameterization);
   }

   private static Estimate[] ReparametrizeLambdas2TauSig(Estimate[] lambdaEstimateVec)
   {
      int K = lambdaEstimateVec.length;
      double[] lambda = new double[ K ];
      double[] tau = new double[ K ];
      double sigma = 0;
      double lambdaSum = 0;

      for (int i=0; i<K; i++)
      {
         lambda[i] = (lambdaEstimateVec[i].getPms().get("lambda") ).doubleValue();
         lambdaSum += lambda[i];
      }

      for (int i=0; i<K; i++)
      {
         tau[i] = lambda[i] / lambdaSum;
         sigma += lambda[i];
      }

      // package results
      Estimate[] tauSigma = new Estimate[ K ];
      for (int i=0; i<K; i++)
      {
         TreeMap<String,Double> tm = new TreeMap<String,Double>();
         tm.put( "tau",new Double(tau[i]) );
         tm.put( "sigma",new Double(sigma) );
         tauSigma[i] = new Estimate("Dirichlet-Poisson MCMC","tau/sigma",tm);
      }
      return tauSigma;
   }

   private static Estimate[] ReparametrizeTauSig2Lambdas(Estimate[] tauSigEstimateVec, String parameterization)
   {
      int K = tauSigEstimateVec.length;
      double[] lambda = new double[ K+1 ];
      double[] tau = new double[ K ];
      double sigma = 0;
      double tauSum = 0;

      for (int i=0; i<K; i++)
      {
         tau[i] = (tauSigEstimateVec[i].getPms().get("tau") ).doubleValue();
         tauSum =+ tau[i];
      }
      sigma = (tauSigEstimateVec[0].getPms().get("sigma") ).doubleValue();

      for (int i=0; i<K; i++)
      {
         lambda[i] = tau[i] * sigma;
      }
      lambda[K] = (1.0 - tauSum) * sigma;

      // package results
      Estimate[] lambdaEstimateVec = new Estimate[ K+1 ];
      for (int i=0; i<(K+1); i++)
      {
         TreeMap<String,Double> tm = new TreeMap<String,Double>();
         tm.put( "lambda",new Double(lambda[i]) );
         lambdaEstimateVec[i] = new Estimate("Dirichlet-Poisson MCMC",parameterization,tm);
      }
      return lambdaEstimateVec;
   }



   //
   // Auxiliary Functions
   //

   public static double estimateMean(double[] wgt)
   {
      double m=0;
      double N = wgt.length;
      for (int i=0; i<wgt.length; i++)
      {
         m += wgt[i];
      }
      m = m/N;
      return m;
   }

   public static double estimateVar(double[] wgt)
   {
      double m=0;
      double m2=0;
      double N = wgt.length;
      for (int i=0; i<wgt.length; i++)
      {
         m += wgt[i];
         m2 += Math.pow(wgt[i],2);
      }
      m = m/N;
      m2 = m2/N;
      double v = (m2 - Math.pow(m,2)) *N /(N-1);
      if (new Double(v).isNaN()) { v=0.0; } // if no variability v=NaN at this point
      return v;
   }

   public static double Sum(double[] wgt)
   {
      double s=0;
      //double N = (double)wgt.length;
      for (int i=0; i<wgt.length; i++)
      {
         s += wgt[i];
      }
      return s;
   }

   public static double Max (double[] v)
   {
      double maxCnt=0;
      for (int i=0; i<v.length; i++)
      {
         maxCnt=Math.max(v[i],maxCnt);
      }
      return maxCnt;
   }
}
