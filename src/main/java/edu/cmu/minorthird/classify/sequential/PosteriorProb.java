package edu.cmu.minorthird.classify.sequential;



// Compute posterior probabilities using Forward and Backward

public class PosteriorProb {
  Forward fwd;                  // result of the forward algorithm 
  Backward bwd;                 // result of the backward algorithm 
  private double logprob;

  PosteriorProb(Forward fwd, Backward bwd) {
    this.fwd = fwd; this.bwd = bwd;
    logprob = fwd.logprob();    // should equal bwd.logprob()
  }

  double posterior(int i, int k) // i=index into the seq; k=the HMM state
  { return Math.exp(fwd.f[i][k] + bwd.b[i][k] - logprob); }
}
