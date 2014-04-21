package edu.cmu.minorthird.classify.algorithms.linear;

import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/**
 * Naive Bayes Poisson Classifier.
 *
 * @author Edoardo Airoldi
 */

/*
 * classify with maximum value of of Pr(class|instance)
 *
 * Pr(class|instance) = Pr(instance|class)*Pr(class)
 * = log Pr(instance|class)*Pr(class)
 * = log prod_f Pr(f|class)*Pr(class)
 * = sum_f log Pr(f|class) + log Pr(class)
 *
 * score is log odds = log( Pr(+|instance) / Pr(-|instance) )
 *  = sum_f log [Pr(f|+)-Pr(f|-)] + log Pr(+) - log Pr(-i)
 *  = sum_f { -mu(+) +mu(-) + f_counts * [ log mu(+) - log mu(-) ] } + log Pr(+) - log Pr(-)
 *
 * where:
 *       f_counts = {counts for feature f over all the examples}
 *     f_in_ex(.) = {counts for feature f in example ex of class .}
 *  MLE for mu(.) = [ sum_ex(.) f_in_ex(.) ] / [ sum_ex(.) sum_f f_in_ex(.) ]
 */

public class PoissonLearner extends BatchBinaryClassifierLearner{

//  static private Logger log = Logger.getLogger(PoissonLearner.class);

	private static final boolean LOG=true;

	private double SCALE;

	public PoissonLearner(){
		this.SCALE=10.0;
		reset();
	}

	public PoissonLearner(double scale){
		this.SCALE=scale;
		reset();
	}

	@Override
	public Classifier batchTrain(Dataset data){
		// temp-filter
		//int featureToKeep = 1000;  String model = "document"; // or "word"
		//System.out.println("Filter Features with Info-Gain");
		//InfoGainTransformLearner filter = new InfoGainTransformLearner( model );
		//InfoGainInstanceTransform infoGain = (InfoGainInstanceTransform)filter.batchTrain( data );
		//infoGain.setNumberOfFeatures( featureToKeep );
		//data = infoGain.transform( data );

		//System.out.println("in batchTrain");
		BasicFeatureIndex index=new BasicFeatureIndex(data);
		//System.out.println( "Dataset:\n # examples = "+data.size() );
		//System.out.println( " # features = "+index.numberOfFeatures() );
		PoissonClassifier c=new PoissonClassifier();
		c.setScale(SCALE);

		double numPos=0.0,numNeg=0.0;
		for(Iterator<Feature> floo=index.featureIterator();floo.hasNext();){
			Feature f=floo.next();
			for(int j=0;j<index.size(f);j++){
				Example ex=index.getExample(f,j);
				boolean isPos=ex.getLabel().isPositive();
				if(isPos)
					numPos+=ex.getWeight(f);
				else
					numNeg+=ex.getWeight(f);
			}
		}
		//System.out.println("n.Pos="+numPos+" n.Neg="+numNeg);
		double featurePrior=1.0/index.numberOfFeatures();
		//System.out.println("size=" + index.numberOfFeatures() + " prior=" +featurePrior);
		for(Iterator<Feature> i=index.featureIterator();i.hasNext();){
			Feature f=i.next();
			double ngp=index.getCounts(f,"POS");
			double ngn=index.getCounts(f,"NEG");
			//System.out.println("feature="+f+" n|Pos="+ngp+" n|Neg="+ngn);
			//System.out.println(c.getScale());
			double pweight=
					estimatedProb(ngp,numPos/c.getScale(),featurePrior,1.0/c.getScale());
			double nweight=
					estimatedProb(ngn,numNeg/c.getScale(),featurePrior,1.0/c.getScale());
			//System.out.println("w|Pos="+pweight+" w|Neg="+nweight);
			c.increment(f,-pweight+nweight);
			pweight=
					estimatedProb(ngp,numPos/c.getScale(),featurePrior,1.0/c.getScale(),
							LOG);
			nweight=
					estimatedProb(ngn,numNeg/c.getScale(),featurePrior,1.0/c.getScale(),
							LOG);
			//System.out.println("w|Pos="+pweight+" w|Neg="+nweight);
			c.increment(f,+pweight-nweight,LOG);
		}
		//System.out.println( "prior pos = "+estimatedProb(numPos, numPos+numNeg, 0.5, 1.0, LOG ) );
		//System.out.println( "prior neg = "+estimatedProb(numNeg, numPos+numNeg, 0.5, 1.0, LOG ) );
		c.incrementBias(+estimatedProb(numPos,numPos+numNeg,0.5,1.0,LOG));
		c.incrementBias(-estimatedProb(numNeg,numPos+numNeg,0.5,1.0,LOG));
		//System.out.println("out of batchTrain\n");
		return c;
	}

	private double estimatedProb(double k,double n,double prior,
			double pseudoCounts){
		//System.out.println("psudoCounts:" + k);
		return (k+prior*pseudoCounts)/(n+pseudoCounts);
	}

	private double estimatedProb(double k,double n,double prior,
			double pseudoCounts,boolean log){
		//.out.println("psudoCounts:" + k);
		return Math.log((k+prior*pseudoCounts)/(n+pseudoCounts));
	}

}
