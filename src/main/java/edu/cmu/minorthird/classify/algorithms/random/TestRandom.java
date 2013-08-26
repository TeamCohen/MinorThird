package edu.cmu.minorthird.classify.algorithms.random;

/**
 * Tests Poisson, Gamma, and Negative-Binomial random numbers.
 *
 * @author Edoardo Airoldi
 *
 */

public class TestRandom {

    private static final double N = 10000.0;
    private static final int N_int = 10000;

    static public void main(String[] argv) {

        // Test Poisson
        Poisson X = new Poisson(5.0);
        Poisson Y = new Poisson(15.0);
        Poisson Z = new Poisson(20.0);

        double[][] mat = new double[N_int][3];
        double[] mean = new double[3];
        double[] var = new double[3];

        for (int k=0; k < N_int; k++) {
            mat[k][0] = X.nextInt();
            mat[k][1] = Y.nextInt();
            mat[k][2] = Z.nextInt();
            mean[0] += mat[k][0];
            mean[1] += mat[k][1];
            mean[2] += mat[k][2];
            //System.out.println( mat[k][0] + " " + mat[k][1] + " " + mat[k][2] );
        }
        mean[0] /= N;
        mean[1] /= N;
        mean[2] /= N;

        System.out.println( mean[0] + " " + mean[1] + " " + mean[2] );

        for (int k=0; k < N_int; k++) {
            var[0] += Math.pow(mat[k][0] - mean[0], 2);
            var[1] += Math.pow(mat[k][1] - mean[1], 2);
            var[2] += Math.pow(mat[k][2] - mean[2], 2);
        }
        var[0] /= (N-1);
        var[1] /= (N-1);
        var[2] /= (N-1);

        System.out.println( var[0] + " " + var[1] + " " + var[2] );

        // Test Gamma
        Gamma G = new Gamma(1.0,1.0);
        //double[] vec = new double[N_int];

        for (int k=0; k < N_int; k++) {
            double g = G.nextDouble();
            System.out.println(g);
        }

        // Test Negative-Binomial
        NegativeBinomial NB = new NegativeBinomial(10,0.5);

        for (int k=0; k < N_int; k++) {
            int nb = NB.nextInt();
            System.out.println(nb);
        }

        //System.out.println( X.toString() );
        //System.out.println( G.toString() );
        //System.out.println( NB.toString() );
    }

}
