package edu.cmu.minorthird.classify.sequential;



// The `Backward algorithm': find the probability of an observed sequence x

public class Backward extends HMMAlgo {
  double[][] b;               // the matrix used to find the decoding
                              // b[i][k] = b_k(i) = log(P(x(i+1)..xL, pi_i=k))

  public Backward(HMM hmm, String[] x) {
    super(hmm, x);
    int L = x.length;
    b = new double[L+1][hmm.nstate];
    for (int k=1; k<hmm.nstate; k++)
      b[L][k] = 0;              // = log(1)  // should be hmm.loga[k][0]
    for (int i=L-1; i>=1; i--)
      for (int k=0; k<hmm.nstate; k++) {
        double sum = Double.NEGATIVE_INFINITY; // = log(0)
        for (int ell=1; ell<hmm.nstate; ell++) 
          sum = logplus(sum, hmm.loga[k][ell] 
                             + hmm.loge[ell][Integer.parseInt(x[i])] 
                             + b[i+1][ell]);
        b[i][k] = sum;
      }
  }

  double logprob() {
    double sum = Double.NEGATIVE_INFINITY; // = log(0)
    for (int ell=0; ell<hmm.nstate; ell++) 
      sum = logplus(sum, hmm.loga[0][ell] 
                         + hmm.loge[ell][Integer.parseInt(x[0])] 
                         + b[1][ell]);
    return sum;
  }

    /*public void print(Output out) {
    for (int j=0; j<hmm.nstate; j++) {
      for (int i=0; i<b.length; i++)
        out.print(HMM.fmtlog(b[i][j]));
      out.println();
    }
    }*/
}
