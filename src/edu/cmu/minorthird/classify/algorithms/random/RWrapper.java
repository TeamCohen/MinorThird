package edu.cmu.minorthird.classify.algorithms.random;

/**
 * User: Edoardo M. Airoldi  (eairoldi@cs.cmu.edu)
 * Date: Feb 22, 2005
 */

public interface RWrapper {

    public void end();

    //
    // Random Number Generators
    //

    public double [] rnorm(int n, double mu, double sd);
    public double [] runif(int n, double min, double max);
    public double [] rbinom(int n, double mu, double delta);
    public double [] rnbinom(int n, double mu, double delta);

    //
    // Densities
    //

    public double [] dnorm(double[] t, double mu, double sd, String string);
    public double dnorm(double t, double mu, double sd, String string);
}
