// Some algorithms for Hidden Markov Models (Chapter 3): Viterbi,
// Forward, Backward, Baum-Welch.  We compute with log probabilities.

// Notational conventions: 

// i     = 1,...,L           indexes x, the observed string, x_0 not a symbol
// k,ell = 0,...,hmm.nstate-1  indexes hmm.state(k)   a_0 is the start state

//Zhenzhen Kou

// Notational conventions: 

// i     = 1,...,L           indexes x, the observed string, x_0 not a symbol
// k,ell = 0,...,hmm.nstate-1  indexes hmm.state(k)   a_0 is the start state
package edu.cmu.minorthird.classify.sequential;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

// Some algorithms for Hidden Markov Models (Chapter 3): Viterbi,
// Forward, Backward, Baum-Welch.  We compute with log probabilities.

public class HMM {
  // State names and state-to-state transition probabilities
  int nstate;           // number of states (incl initial state)
  String[] state;       // names of the states
  double[][] amat;	// transition matrix
  double[][] loga;      // loga[k][ell] = log(P(k -> ell))

  // Emission names and emission probabilities
  int nesym;            // number of emission symbols
  Hashtable<String,String> esym = new Hashtable<String,String>();         // the emission symbols e1,...,eL (characters)
  Hashtable<String,String> esym_tok2idx;
  Hashtable<String,String> esym_idx2tok;

  double[][] emat;	// emision matrix
  double[][] loge;      // loge[k][ei] = log(P(emit ei in state k))

  // Input:
  // state = array of state names (except initial state)
  // amat  = matrix of transition probabilities (except initial state)
  // esym  = string of emission names
  // emat  = matrix of emission probabilities

  public HMM(String[] state, double[][] amat, 
             Hashtable<String,String> esym, double[][] emat) {
    if (state.length != amat.length)
      throw new IllegalArgumentException("HMM: state and amat disagree");
    if (amat.length != emat.length)
      throw new IllegalArgumentException("HMM: amat and emat disagree");
    for (int i=0; i<amat.length; i++) {
      if (state.length != amat[i].length)
        throw new IllegalArgumentException("HMM: amat non-square");
      if (esym.size() != emat[i].length)
        throw new IllegalArgumentException("HMM: esym and emat disagree");
    }      

    // Set up the transition matrix
    nstate = state.length + 1;
    this.state = new String[nstate];
    loga = new double[nstate][nstate];
    this.state[0] = "S";        // initial state
    // P(start -> start) = 0
    loga[0][0] = Double.NEGATIVE_INFINITY; // = log(0)
    // P(start -> other) = 1.0/state.length 
    double fromstart = Math.log(1.0/state.length);
    for (int j=1; j<nstate; j++)
      loga[0][j] = fromstart;
    for (int i=1; i<nstate; i++) {
      // Reverse state names for efficient backwards concatenation
      this.state[i] = new StringBuffer(state[i-1]).reverse().toString();
//      System.out.println("state["+i+"] is "+this.state[i]);
      // P(other -> start) = 0
      loga[i][0] = Double.NEGATIVE_INFINITY; // = log(0)
      for (int j=1; j<nstate; j++)
        loga[i][j] = Math.log(amat[i-1][j-1]);
    }
    
    this.esym = esym;
    esym_tok2idx = new Hashtable<String,String>();
    esym_idx2tok = new Hashtable<String,String>();
    int idx=0;
		  for ( Enumeration<String> e_keys = esym.keys(); e_keys.hasMoreElements();){
		    	String key = e_keys.nextElement();
					esym_tok2idx.put(key, String.valueOf(idx) );
					esym_idx2tok.put(String.valueOf(idx),key );
					idx ++;
     }				   

		  for ( Enumeration<String> e_keys = esym_tok2idx.keys(); e_keys.hasMoreElements();){
		    	String key = e_keys.nextElement();
					String val = esym_tok2idx.get(key );
					System.out.println("in esym_tok2idx: "+key+"<--->"+val);
     }				        
     
    // Set up the emission matrix
    nesym = esym.size();
    loge = new double[nstate][nesym];
    for (int b=0; b<nesym; b++) {
      loge[0][b] = Double.NEGATIVE_INFINITY; // = log(0)
      for (int k=0; k<emat.length; k++) 
        loge[k+1][b] = Math.log(emat[k][b]);
    }
  }

    /*public void print(Output out) 
    { printa(out); printe(out); }

  public void printa(Output out) {
    out.println("Transition probabilities:");
    for (int i=1; i<nstate; i++) {
      for (int j=1; j<nstate; j++) 
        out.print(fmtlog(loga[i][j]));
      out.println();
    }
    }*/

	public String[] convert_Ob_seq( String[] x)	{
		String[] y = new String[x.length];
		for( int i=0; i<x.length;i++){
			if( esym_tok2idx.containsKey( x[i] ) ){
				y[i]= esym_tok2idx.get( x[i] );
			}else{
				y[i]= esym_tok2idx.get( "UNSEEN" );
			}
				
			System.out.println("string "+x[i]+" corresponds to state idx "+y[i]);
		}
		return(y);
	}
	
	/*public void printe(Output out) {
  	
 	
    out.println("Emission probabilities:");
    for (int b=0; b<esym_idx2tok.size(); b++) 
      out.print((String)esym_idx2tok.get(String.valueOf(b)) + hdrpad);
    out.println();
        
    for (int i=1; i<loge.length; i++) {
      for (int b=0; b<nesym; b++) 
        out.print(fmtlog(loge[i][b]));
      out.println();
    }
    }*/

  private static DecimalFormat fmt = new DecimalFormat("0.000000 ");
//  private static String hdrpad     =                    "        ";

  public static String fmtlog(double x) {
    if (x == Double.NEGATIVE_INFINITY)
      return fmt.format(0);
    else
      return fmt.format(Math.exp(x));
  }

  // The Baum-Welch algorithm for estimating HMM parameters for a
  // given model topology and a family of observed sequences.
  // Often gets stuck at a non-global minimum; depends on initial guess.

  // xs    is the set of training sequences, here one training sequence is the sequence of index reprensenting tokens
  // state is the set of HMM state names
  // esym  is the set of emissible symbols

  public static HMM baumwelch(ArrayList<String[]> xs, String[] state, 
                              Hashtable<String,String> esym, final double threshold) {
    int nstate = state.length;
    int nseqs  = xs.size();
    int nesym  = esym.size();


    Forward[] fwds = new Forward[nseqs];
    Backward[] bwds = new Backward[nseqs];
    double[] logP = new double[nseqs];
    
    double[][] amat = new double[nstate][];
    double[][] emat = new double[nstate][];

    // Initially use random transition and emission matrices
    for (int k=0; k<nstate; k++) {
      amat[k] = randomdiscrete(nstate);
      emat[k] = randomdiscrete(nesym);
    }

    HMM hmm = new HMM(state, amat, esym, emat);
    double oldloglikelihood;

    // Compute Forward and Backward tables for the sequences
    double loglikelihood = fwdbwd(hmm, xs, fwds, bwds, logP);
    System.out.println("log likelihood = " + loglikelihood);
    // hmm.print(new SystemOut());
    do {
      oldloglikelihood = loglikelihood;
      // Compute estimates for A and E
      double[][] A = new double[nstate][nstate];
      double[][] E = new double[nstate][nesym];
      for (int s=0; s<nseqs; s++) {
        String[] x = xs.get(s);
        Forward fwd  = fwds[s];
        Backward bwd = bwds[s];
        int L = x.length;
        double P = logP[s];	// NOT exp.  Fixed 2001-08-20

        for (int i=0; i<L; i++) {
          for (int k=0; k<nstate; k++) 
            E[k][Integer.parseInt(x[i])] += exp(fwd.f[i+1][k+1] 
                                              + bwd.b[i+1][k+1] 
                                              - P);
        }
        for (int i=0; i<L-1; i++) 
          for (int k=0; k<nstate; k++) 
            for (int ell=0; ell<nstate; ell++) 
              A[k][ell] += exp(fwd.f[i+1][k+1] 
                               + hmm.loga[k+1][ell+1] 
                               + hmm.loge[ell+1][Integer.parseInt(x[i+1])] 
                               + bwd.b[i+2][ell+1] 
                               - P);
      }
      // Estimate new model parameters, i.e. normalize
      for (int k=0; k<nstate; k++) {
        double Aksum = 0;
        for (int ell=0; ell<nstate; ell++) 
          Aksum += A[k][ell];
        for (int ell=0; ell<nstate; ell++) 
          amat[k][ell] = A[k][ell] / Aksum;
        double Eksum = 0;
        for (int b=0; b<nesym; b++) 
          Eksum += E[k][b];
        for (int b=0; b<nesym; b++) 
          emat[k][b] = E[k][b] / Eksum;
      }
      // Create new model 
      hmm = new HMM(state, amat, esym, emat);
      loglikelihood = fwdbwd(hmm, xs, fwds, bwds, logP);
      System.out.println("log likelihood = " + loglikelihood);
      // hmm.print(new SystemOut());
    } while (Math.abs(oldloglikelihood - loglikelihood) > threshold);
    return hmm;
  }

  private static double fwdbwd(HMM hmm, ArrayList<String[]> xs, Forward[] fwds, 
                               Backward[] bwds, double[] logP) {
    double loglikelihood = 0;
    for (int s=0; s<xs.size(); s++) {
      fwds[s] = new Forward(hmm, xs.get(s));
      bwds[s] = new Backward(hmm, xs.get(s));
      logP[s] = fwds[s].logprob();
      loglikelihood += logP[s];
    }
    return loglikelihood;
  }

  public static double exp(double x) {
    if (x == Double.NEGATIVE_INFINITY)
      return 0;
    else
      return Math.exp(x);
  }

//  private static double[] uniformdiscrete(int n) {
//    double[] ps = new double[n];
//    for (int i=0; i<n; i++) 
//      ps[i] = 1.0/n;
//    return ps;
//  }    

  private static double[] randomdiscrete(int n) {
    double[] ps = new double[n];
    double sum = 0;
    // Generate random numbers
    for (int i=0; i<n; i++) {
      ps[i] = Math.random();
      sum += ps[i];
    }
    // Scale to obtain a discrete probability distribution
    for (int i=0; i<n; i++) 
      ps[i] /= sum;
    return ps;
  }
}
