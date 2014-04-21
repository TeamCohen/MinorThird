package edu.cmu.minorthird.classify.sequential;


public class HMMAlgo {
  HMM hmm;                      // the hidden Markov model
  String[] x;                     // the observed sequence of emissions

  public HMMAlgo(HMM hmm, String[] x) 
  { this.hmm = hmm; this.x = x; }

  // Compute log(p+q) from plog = log p and qlog = log q, using that
  // log (p + q) = log (p(1 + q/p)) = log p + log(1 + q/p) 
  // = log p + log(1 + exp(log q - log p)) = plog + log(1 + exp(logq - logp))
  // and that log(1 + exp(d)) < 1E-17 for d < -37.

  static double logplus(double plog, double qlog) {
    double max, diff;
    if (plog > qlog) {
      if (qlog == Double.NEGATIVE_INFINITY)
        return plog;
      else {
        max = plog; diff = qlog - plog;
      } 
    } else {
      if (plog == Double.NEGATIVE_INFINITY)
        return qlog;
      else {
        max = qlog; diff = plog - qlog;
      }
    }
    // Now diff <= 0 so Math.exp(diff) will not overflow
    return max + (diff < -37 ? 0 : Math.log(1 + Math.exp(diff)));
  }
}
