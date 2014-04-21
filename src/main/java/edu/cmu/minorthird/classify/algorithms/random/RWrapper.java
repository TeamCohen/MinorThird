package edu.cmu.minorthird.classify.algorithms.random;

/**
 * User: Edoardo M. Airoldi  (eairoldi@cs.cmu.edu)
 * Date: Feb 22, 2005
 */

public interface RWrapper {

    /** Teminates the REngine */
    public void end();

    //
    // Random Number Generators
    //

    /** Genrates a vector of n doubles distributed according to a
     *  Gaussian with: mean = mu and standard deviation = sd */
    public double [] rnorm(int n, double mu, double sd);
    /** Genrates a vector of n doubles distributed according to a
     *  Uniform with range: min = min and max = max */
    public double [] runif(int n, double min, double max);
    /** Genrates a vector of n doubles distributed according to a
     *  Binomial with parameters: mu = mu and delta = delta */
    public double [] rbinom(int n, double mu, double delta);
    /** Genrates a vector of n doubles distributed according to a
     *  Negative-Binomial with parameters: mu = mu and delta = delta */
    public double [] rnbinom(int n, double mu, double delta);

    //
    // Densities
    //

    /** Evasluates a Gaussian density (mean = mu, standard deviation = sd)
     *  at each element of the vector t */
    public double [] dnorm(double[] t, double mu, double sd, String string);
    /** Evasluates a Gaussian density (mean = mu, standard deviation = sd)
     *  at t */
    public double dnorm(double t, double mu, double sd, String string);
}
