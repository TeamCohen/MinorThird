package iitb.CRF;

import java.util.Vector;
/**
 * Implements the CollinsVotedPerceptron training algorithm
 *
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */ 

class CollinsTrainer extends Trainer {
    int beamsize = 3;
    double beta = 0.05;
    boolean useUpdated = false;
    boolean voted = true;
    Soln solnPool[]; // for efficiency
    public CollinsTrainer(CrfParams p) {
        super(p);
        if (params.miscOptions.getProperty("beamSize") != null)
            beamsize = Integer.parseInt(params.miscOptions.getProperty("beamSize"));
        if (params.miscOptions.getProperty("beta") != null)
            beta = Double.parseDouble(params.miscOptions.getProperty("beta"));
        if (params.miscOptions.getProperty("UpdatedViterbi") != null) 
            useUpdated = params.miscOptions.getProperty("UpdatedViterbi").equalsIgnoreCase("true");
        if (params.miscOptions.getProperty("voted") != null) 
            voted = params.miscOptions.getProperty("voted").equalsIgnoreCase("true");
    }
    public void train(CRF model, DataIter data, double[] l, Evaluator eval) {
        init(model,data,l);
        double grad[] = gradLogli; // reusing parent's structures.
        Viterbi viterbiSearcher = model.getViterbi(beamsize); 
        for (int i = 0; i < lambda.length; i++)
            lambda[i] = grad[i] = 0;
        Vector<Soln> viterbiS = new Vector<Soln>();
        
        for (int t = 0; t < params.maxIters; t++) {
            int numErrs = 0;
            diter.startScan();
            for (int numRecord = 0; diter.hasNext(); numRecord++) {
                DataSequence dataSeq = (DataSequence)diter.next();
                viterbiSearcher.viterbiSearch(dataSeq,(useUpdated)?lambda:grad, false);
                Soln corrSoln = getCorrectSoln(dataSeq,(useUpdated)?lambda:grad);
                double corrScore = corrSoln.score;
                int maxNum = viterbiSearcher.numSolutions();
                viterbiS.clear();
                for (int k = 0; k < maxNum; k++) {
                    Soln viterbi  = viterbiSearcher.getBestSoln(k);
                    if (viterbi.score < corrScore*(1-beta))
                        break;
                    if ( !isCorrect(viterbi,corrSoln)) {
                        viterbiS.add(viterbi);
                        //System.out.println("adding " + viterbiS.size() + " " + viterbi.score + " " + corrScore + " grad " + norm(grad));
                    }
                }
                if (viterbiS.size() > 0) {
                    for (; corrSoln != null; corrSoln = corrSoln.prevSoln) {
                        boolean differenceAtI = false;
                        for (int s = 0; s < viterbiS.size(); s++) {
                            Soln viterbi = (Soln) viterbiS.elementAt(s);
                            if ((viterbi == null) || !corrSoln.equals(viterbi)) {
                                differenceAtI = true;
                                break;
                            }
                        }
                        if (differenceAtI) {
                            numErrs++;
                            updateWeights(corrSoln, 1.0, grad, dataSeq);
                            for (int s = 0; s < viterbiS.size(); s++) {
                                Soln viterbi = (Soln) viterbiS.elementAt(s);
                                // if (within current frontier, i.e. endpoint overlaps with current segment
                                for (;(viterbi != null) && (viterbi.pos > corrSoln.prevPos()); viterbi = viterbi.prevSoln) { 
                                    updateWeights(viterbi, -1.0/viterbiS.size(), grad, dataSeq);
                                }
                            }
                            /*System.out.println("gnorm at " + corrSoln.pos + " " + norm(grad));
                             for (int s = 0; s < viterbiS.size(); s++) {
                             Soln viterbi = (Soln) viterbiS.elementAt(s);
                             System.out.println(s + " viterbi " + viterbi.pos + " " + viterbi.label);
                             }*/
                            
                        }
                        // advance all viterbi solutions..
                        for (int s = 0; s < viterbiS.size(); s++) {
                            Soln viterbi = (Soln) viterbiS.elementAt(s);
                            // if (within current frontier, i.e. endpoint overlaps with current segment
                            for (;(viterbi != null) && (viterbi.pos > corrSoln.prevPos()); viterbi = viterbi.prevSoln);
                            viterbiS.set(s,viterbi);
                        }
                    }
                }
                // voted perceptron, so add.
                for (int f = 0; f < lambda.length; f++)
                    lambda[f] += grad[f];
                
            }  // all records.
            if (params.debugLvl > 0)
                Util.printDbg("Iteration " + t + " numErrs "+ numErrs);
            if (numErrs == 0)
                break;
        }
    }
    boolean isCorrect(Soln viterbi, Soln corr) { 
        for (; (viterbi != null) && (corr != null); corr = corr.prevSoln, viterbi = viterbi.prevSoln) { 
            if (!viterbi.equals(corr))
                return false;
        }
        return ((viterbi == null) && (corr == null));
    }
    
    int getSegmentEnd(DataSequence dataSeq, int ss) {
        return ss;
    }
    void startFeatureGenerator(FeatureGenerator _featureGenerator, DataSequence dataSeq, Soln soln) {
        _featureGenerator.startScanFeaturesAt(dataSeq, soln.pos);
    }
    void updateWeights(Soln soln, double wt, double grad[], DataSequence dataSeq) {
        startFeatureGenerator(featureGenerator,dataSeq,soln);
        while (featureGenerator.hasNext()) { 
            Feature feature = featureGenerator.next();
            int f = feature.index();
            int yp = feature.y();
            int yprev = feature.yprev();
            float val = feature.value();
            
            if ((soln.label == yp) && (((soln.prevPos() >= 0) && (yprev == soln.prevSoln.label)) || (yprev < 0))) {
                grad[f] += wt*val;
                /*		if (soln.prevPos() < 0)
                 System.out.println("Updating " + soln.label +  " ");
                 else
                 System.out.println("Updating " + soln.label +  " " + yprev + " " + soln.prevSoln.label);
                 */
            }
        }
    }
    Soln getCorrectSoln(DataSequence dataSeq, double grad[]) {
        int se = 0;
        Soln prevSoln = null;
        if ((solnPool == null) || solnPool.length < dataSeq.length()) {
            solnPool = new Soln[dataSeq.length()];
            for (int i = 0; i < dataSeq.length(); solnPool[i++] = new Soln(0,0));
        }
        for (int ss = 0; ss < dataSeq.length(); ss = se+1) {
            se = getSegmentEnd(dataSeq, ss); 
            Soln soln = solnPool[ss];
            soln.pos = se;
            soln.label = dataSeq.y(ss);
            soln.prevSoln = prevSoln;
            soln.score = (prevSoln == null)?0:prevSoln.score;
            startFeatureGenerator(featureGenerator,dataSeq,soln);
            while (featureGenerator.hasNext()) { 
                Feature feature = featureGenerator.next();
                int f = feature.index();
                int yp = feature.y();
                int yprev = feature.yprev();
                float val = feature.value();
                if ((soln.label == yp) && (((soln.prevPos() >= 0) && (yprev == soln.prevSoln.label)) || (yprev < 0))) {
                    soln.score += grad[f]*val;
                }
            }
            prevSoln = soln;
        }
        return prevSoln;
    }
};
