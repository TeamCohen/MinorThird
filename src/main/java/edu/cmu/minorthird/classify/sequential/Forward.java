package edu.cmu.minorthird.classify.sequential;



// The `Forward algorithm': find the probability of an observed sequence x

public class Forward extends HMMAlgo {
  double[][] f;                 // the matrix used to find the decoding
                                // f[i][k] = f_k(i) = log(P(x1..xi, pi_i=k))
  private int L; 

  public Forward(HMM hmm, String[] x) {
    super(hmm, x);
    L = x.length;
    
    f = new double[L+1][hmm.nstate];
    f[0][0] = 0;                // = log(1)
    for (int k=1; k<hmm.nstate; k++)
      f[0][k] = Double.NEGATIVE_INFINITY; // = log(0)
          
    for (int i=1; i<=L; i++)
      f[i][0] = Double.NEGATIVE_INFINITY; // = log(0)

    for (int i=1; i<=L; i++)
      for (int ell=1; ell<hmm.nstate; ell++) {
        double sum = Double.NEGATIVE_INFINITY; // = log(0)
        for (int k=0; k<hmm.nstate; k++) 
        {
        	  sum = logplus(sum, f[i-1][k] + hmm.loga[k][ell]);
        }
        f[i][ell] = hmm.loge[ell][Integer.parseInt(x[i-1])] + sum;
      }
  }

  double logprob() {
    double sum = Double.NEGATIVE_INFINITY; // = log(0)
    for (int k=0; k<hmm.nstate; k++) 
      sum = logplus(sum, f[L][k]);
    return sum;
  }

    /*public void print(Output out) {
    for (int j=0; j<hmm.nstate; j++) {
      for (int i=0; i<f.length; i++)
        out.print(HMM.fmtlog(f[i][j]));
      out.println();
    }
    }*/
}
