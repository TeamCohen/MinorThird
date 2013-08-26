package edu.cmu.minorthird.classify.algorithms.linear;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;

/**
 * Naive Bayes Negative-Binomial Classifier Learner.
 *
 * @author Edoardo Airoldi
 * Date: Jul 12, 2004
 */

public class NegativeBinomialLearner extends BatchBinaryClassifierLearner{

//	static private Logger log=Logger.getLogger(PoissonLearner.class);

//	private static final boolean LOG=true;

	private double SCALE;

	public NegativeBinomialLearner(){
		this.SCALE=10.0;
		reset();
	}

	public NegativeBinomialLearner(double scale){
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

		BasicFeatureIndex index=new BasicFeatureIndex(data);
		//System.out.println( "Dataset:\n # examples = "+data.size() );
		//System.out.println( " # features = "+index.numberOfFeatures() );
		NegativeBinomialClassifier c=new NegativeBinomialClassifier();
		c.setScale(SCALE);

		// J = number of examples
		int JNeg=index.size("NEG");
		int JPos=index.size("POS");
		double[] wgtNeg=new double[JNeg];
		double[] wgtPos=new double[JPos];

		// estimate example weights w(i)
		int exNeg=0,exPos=0;
		double numPos=0.0,numNeg=0.0;
		for(Iterator<Example> eloo=data.iterator();eloo.hasNext();){
			Example e=eloo.next();
			if(e.getLabel().bestClassName().equals("POS")){
				double wgtTot=0.0;
				for(Iterator<Feature> floo=e.featureIterator();floo.hasNext();){
					Feature f=floo.next();
					wgtTot+=e.getWeight(f);
				}
				wgtPos[exPos++]=wgtTot/SCALE;
				numPos+=wgtTot;
			}else if(e.getLabel().bestClassName().equals("NEG")){
				double wgtTot=0.0;
				for(Iterator<Feature> floo=e.featureIterator();floo.hasNext();){
					Feature f=floo.next();
					wgtTot+=e.getWeight(f);
				}
				wgtNeg[exNeg++]=wgtTot/SCALE;
				numNeg+=wgtTot;
			}else{
				System.out.println("error: no class found for example!\n "+e);
				System.exit(1);
			}
		}

		// prior for positive and negative classes, and for unseen features
		double featurePrior=1.0/index.numberOfFeatures(); // prior for unseen features
		c.setPriorPos(numPos,numPos+numNeg,0.5,1.0);
		c.setPriorNeg(numNeg,numPos+numNeg,0.5,1.0);

		// estimate parameters for all features
		double[] vNeg=new double[JNeg];
		double[] vPos=new double[JPos];
		for(Iterator<Feature> floo=index.featureIterator();floo.hasNext();){
			// find vector of counts for Feature f
			Feature f=floo.next();
			exNeg=0;
			exPos=0;
			for(Iterator<Example> eloo=data.iterator();eloo.hasNext();){
				Example e=eloo.next();
				if(e.getLabel().bestClassName().equals("POS")){
					vPos[exPos++]=e.getWeight(f);
				}else if(e.getLabel().bestClassName().equals("NEG")){
					vNeg[exNeg++]=e.getWeight(f);
				}else{
					System.out.println("error: no class found for example!\n "+e);
					System.exit(1);
				}
			}
			// estimate parameters for Feature f
			SortedMap<String,Double> mudeltaNeg=estimateNegBinMOME(vNeg,wgtNeg,featurePrior);
			SortedMap<String,Double> mudeltaPos=estimateNegBinMOME(vPos,wgtPos,featurePrior);
			c.setPmsNeg(f,mudeltaNeg);
			c.setPmsPos(f,mudeltaPos);
		}
		return c;
	}

	private SortedMap<String,Double> estimateNegBinMOME(double[] vCnt,double[] vWgt,double prior){
		double m=0.0;
		double d=0.0;

		// compute mean
		int N=vCnt.length;
		double sumX=0.0;
		double sumWgt=0.0;
		double sumWgt2=0.0;
		for(int i=0;i<N;i++){
			sumX+=vCnt[i];
			sumWgt+=vWgt[i];
			sumWgt2+=Math.pow(vWgt[i],2);
		}
		m=(sumX+prior*1.0/SCALE)/(sumWgt+1.0/SCALE);

		// compute intermediate
		double r;
		double v=0.0;
		if(N<=1.0){
			r=0.0;
			v=0.0;
		}else{
			r=(sumWgt-sumWgt2/sumWgt)/(N-1.0);
			for(int i=0;i<N;i++){
				v+=(vWgt[i]*Math.pow(vCnt[i]/vWgt[i]-m,2))/(N-1.0);
			}
		}

		// compute variance
		d=Math.max(0.0,(v-m)/(r*m));
		if(new Double(d).isNaN()){
			d=1e-7;
		}

		// package results
		SortedMap<String,Double> mudelta=new TreeMap<String,Double>();
		mudelta.put("mu",new Double(m));
		mudelta.put("delta",new Double(d));
		return mudelta;
	}
}
