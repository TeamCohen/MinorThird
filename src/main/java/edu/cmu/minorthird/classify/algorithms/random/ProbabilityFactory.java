/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.random;

/**
 * @author Edoardo Airoldi
 * Date: Jan 10, 2005
 */

public class ProbabilityFactory
{
    private static int FLAG = 0;
    private static RWrapper rw;
    private static Object rwShadow;

    public ProbabilityFactory()
    {
        if (FLAG == 0)
        {
            try {
                rwShadow = Class.forName("eairoldi.random.BasicRWrapper");
                rw = (RWrapper) ((Class<?>)rwShadow).newInstance(); // Note: initialized once at the first call!
                FLAG += 1;
            } catch (Exception x) {
                System.out.println("error: R in Minorthird is not properly installed!");
                System.out.println("       See R_quickstart.txt in apps/edo/doc/ for more information.");
                System.exit(-99);
            }
        }
    }


    //
    // Random Number Generators
    //

    public static double [] rnorm(int n, double mu, double sd)
    {
        return rw.rnorm(n,mu,sd);
    }

    public static double [] runif(int n, double min, double max)
    {
        return rw.runif(n,min,max);
    }



    //
    // Probabilities for the Dirichlet-Poisson model (Airoldi, Cohen & Fienberg 2005)
    //

    // P(tau|sigma,X)
    public static double ProbTauGivenSigmaX
            (double tau, double sig, double[] vlow, double[] vup, double xr, double xp, double wr, double wp, double[] vbeta)
    {
        double prob=0;
        double b1 = vbeta[0];
        double b2 = vbeta[1];
        if (tau<vlow[0] | sig<vlow[1] | tau>vup[0] | sig>vup[1]) {
            prob = Double.NEGATIVE_INFINITY;
            //System.out.println(". out of bounds!");
            //System.out.println(". "+vlow[0]+"<"+tau+"<"+vup[0]+" "+vlow[1]+"<"+sig+"<"+vup[1]);
        } else {
            prob = -wr*tau*sig -wp*(1-tau)*sig +xr*Math.log(wr*tau*sig)
                    +xp*Math.log(wp*(1-tau)*sig) +(b1+b2*sig-1)*Math.log(tau*(1-tau));
        }
        return prob;
    }

    // probability of acceptance for tau - Metropolis step //// CHECK p1 UNUSED ??
    public static double AlphaTau
            (double tau, double sig, double[] vlow, double[] vup, double xr, double xp, double wr, double wp, double[] vbeta,  double p1, double p2, double candidate)
    {
        double prob=0;
        //System.out.println("p(cand|s,x)="+ProbTauGivenSigmaX(candidate,sig,vlow,vup,xr,xp,wr,wp,vbeta)+" p(tau|s,x)="+ProbTauGivenSigmaX(tau,sig,vlow,vup,xr,xp,wr,wp,vbeta)+" pi(tau)="+rw.dnorm(tau, candidate,p2,"TRUE")+" pi(cand)="+rw.dnorm(candidate, tau,p2,"TRUE"));
        prob = Math.exp(ProbTauGivenSigmaX(candidate,sig,vlow,vup,xr,xp,wr,wp,vbeta) -ProbTauGivenSigmaX(tau,sig,vlow,vup,xr,xp,wr,wp,vbeta) +rw.dnorm(tau, candidate,p2,"TRUE") -rw.dnorm(candidate, tau,p2,"TRUE"));
        if (new Double(prob).isNaN()) { prob=0; }
        double alpha = Math.min( 1,prob );
        return alpha;
    }

    // P(sigma|tau,X)
    public static double ProbSigmaGivenTauX
            (double sig, double tau, double[] vlow, double[] vup, double xr, double xp, double wr, double wp, double[] vbeta)
    {
        double prob=0;
        double b1 = vbeta[0];
        double b2 = vbeta[1];
        if (tau<vlow[0] | sig<vlow[1] | tau>vup[0] | sig>vup[1]) {
            prob = Double.NEGATIVE_INFINITY;
            //System.out.println(". out of bounds!");
            //System.out.println(". "+vlow[0]+"<"+tau+"<"+vup[0]+" "+vlow[1]+"<"+sig+"<"+vup[1]);
        } else {
            prob = -wr*tau*sig -wp*(1-tau)*sig +xr*Math.log(wr*tau*sig) +xp*Math.log(wp*(1-tau)*sig) +(b1+b2*sig-1)*Math.log(tau*(1-tau)) +Arithmetic.logGamma(2*(b1+b2*sig)) - 2*Arithmetic.logGamma(b1+b2*sig);
        }
        return prob;
    }

    // probability of acceptance for sig - Metropolis step //// CHECK p1 UNUSED ??
    public static double AlphaSigma
            (double sig, double tau, double[] vlow, double[] vup, double xr, double xp, double wr, double wp, double[] vbeta,  double p1, double p2, double candidate)
    {
        double prob=0;
        //System.out.println("p(cand|t,x)="+ProbSigmaGivenTauX(candidate,tau,vlow,vup,xr,xp,wr,wp,vbeta)+" p(sig|t,x)="+ProbSigmaGivenTauX(sig,tau,vlow,vup,xr,xp,wr,wp,vbeta)+" pi(sig)="+rw.dnorm(sig, candidate,p2,"TRUE")+" pi(cand)="+rw.dnorm(candidate, sig,p2,"TRUE"));
        prob = Math.exp( ProbSigmaGivenTauX(candidate,tau,vlow,vup,xr,xp,wr,wp,vbeta) -ProbSigmaGivenTauX(sig,tau,vlow,vup,xr,xp,wr,wp,vbeta) +rw.dnorm(sig, candidate,p2,"TRUE") -rw.dnorm(candidate, sig,p2,"TRUE") );
        if (new Double(prob).isNaN()) { prob=0; }
        double alpha = Math.min( 1,prob );
        return alpha;
    }

}
