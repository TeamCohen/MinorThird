package edu.cmu.minorthird.classify.algorithms.random;

/*=
 * Negative Binomial distribution; See the <A HREF="http://www.statlets.com/usermanual/glossary2.htm"> math definition</A>.
 * <p>
 * Instance methods operate on a user supplied uniform random number generator; they are unsynchronized.
 * <dt>
 * Static methods operate on a default uniform random number generator; they are synchronized.
 * <p>
 * <b>Implementation:</b> High performance implementation. Compound method. 
 * <dt>
 * This is a port of <tt>nbp.c</tt> from the <A HREF="http://www.cis.tu-graz.ac.at/stat/stadl/random.html">C-RAND / WIN-RAND</A> library.
 * C-RAND's implementation, in turn, is based upon
 * <p>
 * J.H. Ahrens, U. Dieter (1974): Computer methods for sampling from gamma, beta, Poisson and binomial distributions, Computing 12, 223--246.
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * @author Edoardo Airoldi
 */

public class NegativeBinomial {
    protected int n;
    protected double p;

    protected Gamma gamma;
    protected Poisson poisson;

    /*=
     * Constructs a Negative Binomial distribution.
     * Example: n=1, p=0.5.
     * @param n the number of trials.
     * @param p the probability of success.
     */
    public NegativeBinomial(int n, double p) {
        setNandP(n,p);
        this.gamma = new Gamma(n,1.0);
        this.poisson = new Poisson(0.0);
    }

    /*=
     * Returns a random number from the distribution.
     */
    public int nextInt() {
        return nextInt(n,p);
    }

    /*=
     * Returns a random number from the distribution; bypasses the internal state.
     */
    public int nextInt(int n, double p) {
        /*=****************************************************************
         *                                                                *
         *        Negative Binomial Distribution - Compound method        *
         *                                                                *
         ******************************************************************
         *                                                                *
         * FUNCTION:    - nbp  samples a random number from the Negative  *
         *                Binomial distribution with parameters r (no. of *
         *                failures given) and p (probability of success)  *
         *                valid for  r > 0, 0 < p < 1.                    *
         *                If G from Gamma(r) then K  from Poiss(pG/(1-p)) *
         *                is NB(r,p)--distributed.                        *
         * REFERENCE:   - J.H. Ahrens, U. Dieter (1974): Computer methods *
         *                for sampling from gamma, beta, Poisson and      *
         *                binomial distributions, Computing 12, 223--246. *
         * SUBPROGRAMS: - drand(seed) ... (0,1)-Uniform generator with    *
         *                unsigned long integer *seed                     *
         *              - Gamma(seed,a) ... Gamma generator for a > 0     *
         *                unsigned long *seed, double a                   *
         *              - Poisson(seed,a) ...Poisson generator for a > 0  *
         *                unsigned long *seed, double a.                  *
         *                                                                *
         ******************************************************************/

        double x = p /(1.0 - p);
        //double p1 = p;
        double y = x * this.gamma.nextDouble(n,1.0);
        return this.poisson.nextInt(y);
    }

    /*=
     * Sets the parameters number of trials and the probability of success.
     * @param n the number of trials
     * @param p the probability of success.
     */
    public void setNandP(int n, double p) {
        this.n = n;
        this.p = p;
    }

    /*=
     * Returns a String representation of the receiver.
     */
    @Override
		public String toString() {
        return this.getClass().getName()+"("+n+","+p+")";
    }
}
