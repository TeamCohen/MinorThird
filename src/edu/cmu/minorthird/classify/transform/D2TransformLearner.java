package edu.cmu.minorthird.classify.transform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.WeightedSet;
import edu.cmu.minorthird.classify.algorithms.random.Estimate;
import edu.cmu.minorthird.classify.algorithms.random.Estimators;
import edu.cmu.minorthird.classify.algorithms.random.NegativeBinomial;
import edu.cmu.minorthird.classify.algorithms.random.Poisson;

/**
 * @author Edoardo Airoldi  (eairoldi@cs.cmu.edu)
 * Date: Mar 6, 2005
 */

public class D2TransformLearner implements InstanceTransformLearner{

//	static private Logger log=Logger.getLogger(D2TransformLearner.class);

	private ExampleSchema schema;

	// re-initialized at each call of batchTrain(Dataset)
	private Map<Feature,Double> T1values;

	private Map<Feature,Double> muPosExamples;

//	private Map<Feature,Double> deltaPosExamples;
//
//	private Map<Feature,Double> muNegExamples;
//
//	private Map<Feature,Double> deltaNegExamples;

	private Map<Feature,String> featurePdf; // model for f: can be "Poisson" or "Negative-Binomial"

	private Map<Feature,Double[][]> T1valuesMany;

	private List<Double> classParameters;

	private List<Object> featureGivenClassParameters;

	// defaults
	private double ALPHA; // tolerance level for the FDR in selecting features

	private int MIN_WORDS; // minimum number of features to keep, EVEN IF NOT all significant

	private int MAX_WORDS; // maximum number of features to keep, EVEN IF MORE are significant

	private int SAMPLE; // points sampled to estimate T1's PDF, and compute p-values

	private double REF_LENGTH; // word-length of the reference document

	private String PDF; // distribution for D^2 p-values

	private String WHAT_IF_MANY_CLASSES; // "max" or "sum", only relevant for multiple-class schemas

	private String APPROX; // "sample" or "delta-method" or "saddle-point"

	public D2TransformLearner(){
		this.ALPHA=0.05;
		this.MIN_WORDS=5; // corresponding to indices 0,1,2,3,4
		this.MAX_WORDS=Integer.MAX_VALUE; // that is, all features in Dataset
		this.SAMPLE=2500; // this is fast, but for rare features may be better 10000 or 100000

		this.PDF="Poisson";
		this.APPROX="sample";
		this.REF_LENGTH=100.0;
		this.WHAT_IF_MANY_CLASSES="max";
	}

	/** ExampleSchema not used here ... */
	@Override
	public void setSchema(ExampleSchema schema){
		;
	}

	/** Examine data, build an instance transformer */
	@Override
	public InstanceTransform batchTrain(Dataset dataset){
		InitReset();
		this.schema=dataset.getSchema();
		int numberOfClasses=schema.getNumberOfClasses();

		if(ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)){
			//
			// binary class case

			BasicFeatureIndex index=new BasicFeatureIndex(dataset);
			double featurePrior=1.0/index.numberOfFeatures();
			// loop features
			for(Iterator<Feature> i=index.featureIterator();i.hasNext();){
				Feature f=i.next();
				// fill array of <counts_ex(feature), length_ex>
				double[] x=new double[dataset.size()];
				double[] omega=new double[dataset.size()];
				int position=0;
				for(Iterator<Example> j=dataset.iterator();j.hasNext();){
					Example e=j.next();
					x[position]=e.getWeight(f);
					omega[position]=getLength(e); // / REF_LENGTH; // normalization happens in Estimators
					position+=1;
				}
				/* // fill array of <counts_ex(feature), length_ex> for POS class
				 double[] xPos = new double[ index.size(f,"POS") ];
				 double[] omegaPos = new double[ index.size(f,"POS") ];
				 int position=0;
				 for (int j=0; j<index.size(f); j++) {
				    Example e = index.getExample(f,j);
				    if ( "POS".equals( e.getLabel().bestClassName() ) ) {
				       xPos[position] = e.getWeight(f);
				       omegaPos[position] = getLength(e); // / REF_LENGTH; // normalization happens in Estimators
				       position += 1;
				    }
				 }
				 // fill array of <counts(example,feature), length(example)> for NEG class
				 double[] xNeg = new double[ index.size(f,"NEG") ];
				 double[] omegaNeg = new double[ index.size(f,"NEG") ];
				 position=0;
				 for (int j=0; j<index.size(f); j++) {
				    Example e = index.getExample(f,j);
				    if ( "NEG".equals( e.getLabel().bestClassName() ) ) {
				       xNeg[position] = e.getWeight(f);
				       omegaNeg[position] = getLength(e); // / REF_LENGTH; // normalization happens in Estimators
				       position += 1;
				    }
				 }*/
				// estimate Parameters for the two classes and update the T1-Filter
				setT1(f,T1((int)index.getCounts(f,"POS"),(int)index.getCounts(f,"NEG")));
				if(PDF.equals("Poisson")){
					// learn Poisson parameters
					Estimate est=
							Estimators.estimatePoissonWeightedLambda(x,omega,featurePrior,
									REF_LENGTH);
					double mu=(est.getPms().get("lambda")).doubleValue();
					setPosMu(f,mu); // overall rate of occurrence
					setFeaturePdf(f,"Poisson");

				}else if(PDF.equals("Negative-Binomial")){
					throw new UnsupportedOperationException("error: PDF \""+PDF+
							"\" is not implemented!");
				}
				/*if ( PDF.equals("Poisson") )
				{
				   // learn Poisson parameters
				   Estimate estPos = Estimators.estimatePoissonWeightedLambda( xPos,omegaPos,featurePrior,REF_LENGTH );
				   Estimate estNeg = Estimators.estimatePoissonWeightedLambda( xNeg,omegaNeg,featurePrior,REF_LENGTH );
				   double muPos = ((Double)estPos.getPms().get("lambda")).doubleValue();
				   double muNeg = ((Double)estNeg.getPms().get("lambda")).doubleValue();
				   //System.out.println("ft = "+f+" :: mu+ = "+muPos+", mu- = "+muNeg);
				   // update T1 Filter
				   setPosMu( f,muPos );
				   setNegMu( f,muNeg );
				   setFeaturePdf( f,"Poisson" );

				} else if ( PDF.equals("Negative-Binomial") )
				{
				   // learn Negative-Binomial parameters
				   Estimate estPos = Estimators.estimateNegativeBinomialMuDelta( xPos,omegaPos,featurePrior,REF_LENGTH );
				   Estimate estNeg = Estimators.estimateNegativeBinomialMuDelta( xNeg,omegaNeg,featurePrior,REF_LENGTH );
				   // update T1 Filter
				   setPosMu( f,((Double)estPos.getPms().get("mu")).doubleValue() );
				   setPosDelta( f,((Double)estPos.getPms().get("delta")).doubleValue() );
				   setNegMu( f,((Double)estNeg.getPms().get("mu")).doubleValue() );
				   setNegDelta( f,((Double)estNeg.getPms().get("delta")).doubleValue() );
				   setFeaturePdf( f,"Negative-Binomial");
				} */
			}

			// end of binary class case
			//
		}else{
			//
			// multiple-class case

			BasicFeatureIndex index=new BasicFeatureIndex(dataset);
			List<double[]> featureMatrix=new ArrayList<double[]>();
			List<double[]> exampleWeightMatrix=new ArrayList<double[]>();
			String[] classLabels=new String[numberOfClasses];
			int[] classSizes=new int[numberOfClasses];
			for(int i=0;i<numberOfClasses;i++){
				classLabels[i]=schema.getClassName(i);
				classSizes[i]=index.size(classLabels[i]);
				double[] featureCounts=new double[classSizes[i]];
				double[] exampleWeights=new double[classSizes[i]];
				featureMatrix.add(featureCounts);
				exampleWeightMatrix.add(exampleWeights);
			}
			// count occurrences of features given class & example weights given class
			double numberOfExamples=(dataset.size());
			double[] countsGivenClass=new double[numberOfClasses];
			double[] examplesGivenClass=new double[numberOfClasses];
			int[] excounter=new int[numberOfClasses];
			for(Iterator<Example> i=dataset.iterator();i.hasNext();){
				Example ex=i.next();
				//System.out.println("label="+ex.getLabel().bestClassName().toString());
				int idx=schema.getClassIndex(ex.getLabel().bestClassName().toString());
				examplesGivenClass[idx]+=1.0;
				for(Iterator<Feature> j=index.featureIterator();j.hasNext();){
					Feature f=j.next();
					countsGivenClass[idx]+=ex.getWeight(f);
					(exampleWeightMatrix.get(idx))[excounter[idx]]+=
							ex.getWeight(f); // SCALE is HERE !!!
				}
				excounter[idx]+=1;
			}
			// estimate parameters of each features given class
			for(Iterator<Feature> floo=index.featureIterator();floo.hasNext();){
				int[] counter=new int[numberOfClasses];
				double[] sums=new double[numberOfClasses];

				// load vector of counts (by class) for feature f
				Feature ft=floo.next();
				for(Iterator<Example> eloo=dataset.iterator();eloo.hasNext();){
					Example ex=eloo.next();
					int idx=
							schema.getClassIndex(ex.getLabel().bestClassName().toString());
					(featureMatrix.get(idx))[counter[idx]++]=ex.getWeight(ft);
				}

				if(PDF.equals("Poisson")){
					double featurePrior=1.0/index.numberOfFeatures();
					for(int j=0;j<numberOfClasses;j++){
						Estimate est=
								Estimators.estimateNaiveBayesMean(1.0,numberOfClasses,
										examplesGivenClass[j],numberOfExamples);
						double probabilityOfOccurrence=
								(est.getPms().get("mean")).doubleValue();
						setClassParameter(j,probabilityOfOccurrence);

						double[] countsFeatureGivenClass=featureMatrix.get(j);
						sums[j]=Estimators.Sum(countsFeatureGivenClass);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate lambda=
								Estimators.estimatePoissonWeightedLambda(
										countsFeatureGivenClass,countsGivenExample,featurePrior,
										REF_LENGTH);
						setFeatureGivenClassParameter(ft,j,lambda);
					}
					setFeaturePdf(ft,"Poisson");
				}else if(PDF.equals("Negative-Binomial")){
					double featurePrior=1.0/index.numberOfFeatures();
					for(int j=0;j<numberOfClasses;j++){
						Estimate est=
								Estimators.estimateNaiveBayesMean(1.0,numberOfClasses,
										examplesGivenClass[j],numberOfExamples);
						double probabilityOfOccurrence=
								(est.getPms().get("mean")).doubleValue();
						setClassParameter(j,probabilityOfOccurrence);

						double[] countsFeatureGivenClass=featureMatrix.get(j);
						sums[j]=Estimators.Sum(countsFeatureGivenClass);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate mudelta=
								Estimators.estimateNegativeBinomialMuDelta(
										countsFeatureGivenClass,countsGivenExample,featurePrior,
										REF_LENGTH);
						setFeatureGivenClassParameter(ft,j,mudelta);
					}
					setFeaturePdf(ft,"Negative-Binomial");
				}
				// store T1 values for each pair of indices
				for(int ci=0;ci<numberOfClasses;ci++){
					for(int cj=(ci+1);cj<numberOfClasses;cj++){
						double t1=T1((int)sums[ci],(int)sums[cj]);
						setT1many(ft,t1,ci,cj);
					}
				}
			}

			// end of multiple-class class case
			//

		}

		//
		// compute D^2 p-values

		List<Pair> pValue=new ArrayList<Pair>();

		if(ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)){
			//
			// binary class case

			if(APPROX.equals("sample")){
				BasicFeatureIndex index=new BasicFeatureIndex(dataset);
				// loop features
				for(Iterator<Feature> i=index.featureIterator();i.hasNext();){
					Feature f=i.next();
					double[] T1array=sampleT1Values(f); // Sample T1 values
					//double[] T1array = sampleT1Values2( f ); // Sample T1 values
					Pair p=computePValue(T1array,f); // compute p-value for Feature f
					pValue.add(p);
					//System.out.println( p );
				}

			}else if(APPROX.equals("delta-method")){
				throw new UnsupportedOperationException("error: Approximation \""+
						APPROX+"\" is not implemented!");
			}else if(APPROX.equals("saddle-point")){
				throw new UnsupportedOperationException("error: Approximation \""+
						APPROX+"\" is not implemented!");
			}else{
				throw new UnsupportedOperationException("error: Approximation \""+
						APPROX+"\" is not recognized!");
			}

			// end of binary class case
			//

		}else{

			//
			// multiple-class class case

			if(WHAT_IF_MANY_CLASSES.equals("max")&APPROX.equals("sample")){
				BasicFeatureIndex index=new BasicFeatureIndex(dataset);
				// loop features
				for(Iterator<Feature> i=index.featureIterator();i.hasNext();){
					Feature f=i.next();
					Pair maxPair=new Pair(1.0,f);
					for(int ci=0;ci<numberOfClasses;ci++){
						for(int cj=(ci+1);cj<numberOfClasses;cj++){
							double[] T1array=sampleT1Values(f,ci,cj); // Sample T1 values
							Pair p=computePValue(T1array,f,ci,cj); // compute p-value for Feature f
							if(p.value<maxPair.value){
								maxPair=p;
							}
						}
					}
					pValue.add(maxPair);
				}

			}else if(WHAT_IF_MANY_CLASSES.equals("sum")){
				throw new UnsupportedOperationException("error: D^2 extension \""+
						WHAT_IF_MANY_CLASSES+"\" is not implemented!");
			}else{
				throw new UnsupportedOperationException("error: D^2 extension \""+
						WHAT_IF_MANY_CLASSES+"\" is not recognized!");
			}

			// end of multiple-class class case
			//

		}

		// compute D^2 p-values
		//

		//
		// find relevent features using D^2 p-values and FDR correction

		final Comparator<Pair> VAL_COMPARATOR=new Comparator<Pair>(){
			@Override
			public int compare(Pair p1,Pair p2){
				if(p1.value<p2.value)
					return -1;
				else if(p1.value>p2.value)
					return 1;
				else
					return (p1.feature).compareTo(p2.feature);
			}
		};
		final SortedMap<Feature,Integer> availableFeatures=selectFeaturesViaFDR(pValue,VAL_COMPARATOR);

		// find relevent features using D^2 p-values and FDR correction
		//

		//
		// build an InstanceTransform that removes low-frequency features

		return new AbstractInstanceTransform(){

			@Override
			public Instance transform(Instance instance){
				return new MaskedInstance(instance,availableFeatures);
			}

			@Override
			public String toString(){
				return "[InstanceTransform: model = "+PDF+" by D^2]";
			}
		};
	}

	//
	// getters / setters
	//

	/** Set the PDF for feature f */
	public void setFeaturePdf(Feature f,String pdf){
		featurePdf.put(f,new String(pdf));
	}

	//
	// for binary case

	/** Set the value of T1 corresponding to feature f */
	private void setT1(Feature f,double delta){
		Double d=T1values.get(f);
		if(d==null)
			T1values.put(f,new Double(delta));
		else
			System.out.println("Warning: T1 value already set for feature "+
					f.toString()+"!");
	}

	/** Set mu corresponding to the Positive examples of feature f */
	private void setPosMu(Feature f,double delta){
		Double d=muPosExamples.get(f);
		if(d==null)
			muPosExamples.put(f,new Double(delta));
		else
			muPosExamples.put(f,new Double(d.doubleValue()+delta));
	}

	/** Get mu corresponding to the Positive examples of feature f */
	private double getPosMu(Feature f){
		Double d=muPosExamples.get(f);
		if(d==null)
			return 0.0;
		else
			return d.doubleValue();
	}

	/** Set mu corresponding to the Positive examples of feature f */
//	private void setNegMu(Feature f,double delta){
//		Double d=(Double)muNegExamples.get(f);
//		if(d==null)
//			muNegExamples.put(f,new Double(delta));
//		else
//			muNegExamples.put(f,new Double(d.doubleValue()+delta));
//	}

	/** Get mu corresponding to the Positive examples of feature f */
//	private double getNegMu(Feature f){
//		Double d=(Double)muNegExamples.get(f);
//		if(d==null)
//			return 0.0;
//		else
//			return d.doubleValue();
//	}

	/** Set mu corresponding to the Positive examples of feature f */
//	private void setPosDelta(Feature f,double delta){
//		Double d=(Double)deltaPosExamples.get(f);
//		if(d==null)
//			deltaPosExamples.put(f,new Double(delta));
//		else
//			deltaPosExamples.put(f,new Double(d.doubleValue()+delta));
//	}

	/** Get mu corresponding to the Positive examples of feature f */
//	private double getPosDelta(Feature f){
//		Double d=(Double)deltaPosExamples.get(f);
//		if(d==null)
//			return 0.0;
//		else
//			return d.doubleValue();
//	}

	/** Set mu corresponding to the Positive examples of feature f */
//	private void setNegDelta(Feature f,double delta){
//		Double d=(Double)deltaNegExamples.get(f);
//		if(d==null)
//			deltaNegExamples.put(f,new Double(delta));
//		else
//			deltaNegExamples.put(f,new Double(d.doubleValue()+delta));
//	}

	/** Get mu corresponding to the Positive examples of feature f */
//	private double getNegDelta(Feature f){
//		Double d=(Double)deltaNegExamples.get(f);
//		if(d==null)
//			return 0.0;
//		else
//			return d.doubleValue();
//	}

	//
	// for multi-class case

	/** Set the value of T1 corresponding to feature f and classes (i,j) */
	private void setT1many(Feature f,double delta,int i,int j){
		Double[][] d=T1valuesMany.get(f);
		if(d==null){
			int N=schema.getNumberOfClasses();
			d=new Double[N][N];
			d[i][j]=new Double(delta);
			T1valuesMany.put(f,d);
		}else if(d[i][j]==null){
			d[i][j]=new Double(delta);
		}else
			System.out.println("Warning: T1 value already set for feature "+
					f.toString()+"!");
	}

	/** Set probability of class j to d */
	public void setClassParameter(int j,double d){
		try{
			classParameters.get(j);
		}catch(Exception x){
			classParameters.add(j,new Double(d));
			//System.out.println(". added in "+j+" >> pi="+d);
		}
	}

	public void setFeatureGivenClassParameter(Feature f,int j,Estimate pms){
		Map<Feature,Estimate> hmap;
		try{
			hmap=(Map<Feature,Estimate>)featureGivenClassParameters.get(j);
			hmap.put(f,pms);
			featureGivenClassParameters.set(j,hmap);
		}catch(Exception NoHashMapforClassJ){
			hmap=null;
			hmap=new HashMap<Feature,Estimate>();
			hmap.put(f,pms);
			featureGivenClassParameters.add(j,hmap);
		}
	}

	//
	// convenience methods
	//

	/** Initializes or Resets the contents of D2 */
	private void InitReset(){
		this.T1values=new TreeMap<Feature,Double>();
		this.muPosExamples=new TreeMap<Feature,Double>();
//		this.muNegExamples=new TreeMap<Feature,Double>();
//		this.deltaPosExamples=new TreeMap<Feature,Double>();
//		this.deltaNegExamples=new TreeMap<Feature,Double>();
		this.featurePdf=new TreeMap<Feature,String>();

		this.classParameters=new ArrayList<Double>();
		this.T1valuesMany=new TreeMap<Feature,Double[][]>();
		this.featureGivenClassParameters=new ArrayList<Object>();
		this.featureGivenClassParameters.add(new WeightedSet<Feature>());
	}

	/** A class that we use to sort a TreeMap by values */
	private class Pair extends Object{

		double value;

		Feature feature;

		public Pair(double v,Feature f){
			this.value=v;
			this.feature=f;
		}

		@Override
		public String toString(){
			return "[ "+this.value+","+this.feature+" ]"; //this.key + " ]";
		}
	}

	private SortedMap<Feature,Integer> selectFeaturesViaFDR(List<Pair> pValue,
			final Comparator<Pair> VAL_COMPARATOR){
		SortedMap<Feature,Integer> availableFeatures=new TreeMap<Feature,Integer>();
		Collections.sort(pValue,VAL_COMPARATOR);
		int greatestIndexBeforeAccept=-1; // does not return any word at -1
		for(int j=1;j<=pValue.size();j++){
			double line=(j)*ALPHA/(pValue.size());
			if(line>pValue.get(j-1).value){
				greatestIndexBeforeAccept=j-1;
			}
		}
		//System.out.println("max index = "+greatestIndexBeforeAccept);
		//System.out.println("total words = "+pValue.size());
		greatestIndexBeforeAccept=
				Math.min(MAX_WORDS,Math.min(pValue.size()-1,Math.max(
						greatestIndexBeforeAccept,MIN_WORDS)));
		System.out.println("Retained "+(greatestIndexBeforeAccept+1)+
				" fetures, out of "+pValue.size());
		for(int j=0;j<=greatestIndexBeforeAccept;j++){
			//System.out.println("word="+((Pair)pValue.get(j)).feature+", p-value="+((Pair)pValue.get(j)).value);
			availableFeatures.put(pValue.get(j).feature,new Integer(1));
		}
		return availableFeatures;
	}

//	private double[] sampleT1Values2(Feature f){
//		double[] T1array=new double[SAMPLE];
//		String s=(String)featurePdf.get(f);
//		// Sample from PDF of Feature f
//		if(s.equals("Poisson")){
//			Poisson Xp=new Poisson(getPosMu(f));
//			Poisson Xn=new Poisson(getNegMu(f));
//			for(int cnt=0;cnt<SAMPLE;cnt++){
//				T1array[cnt]=T1(Xp.nextInt(),Xn.nextInt());
//			}
//		}else if(s.equals("Negative-Binomial")){
//			TreeMap npPos=mudelta2np(getPosMu(f),getPosDelta(f),1.0);
//			TreeMap npNeg=mudelta2np(getNegMu(f),getNegDelta(f),1.0);
//			NegativeBinomial Xp=
//					new NegativeBinomial(((Integer)(npPos.get("n"))).intValue(),
//							((Double)(npPos.get("p"))).doubleValue());
//			NegativeBinomial Xn=
//					new NegativeBinomial(((Integer)(npNeg.get("n"))).intValue(),
//							((Double)(npNeg.get("p"))).doubleValue());
//			for(int cnt=0;cnt<SAMPLE;cnt++){
//				T1array[cnt]=T1(Xp.nextInt(),Xn.nextInt());
//			}
//		}else{
//			throw new IllegalStateException("Error: PDF not implemented!");
//		}
//		return T1array;
//	}

	private double[] sampleT1Values(Feature f){
		double[] T1array=new double[SAMPLE];
		String s=featurePdf.get(f);
		// Sample from PDF of Feature f
		if(s.equals("Poisson")){
			Poisson Xp=new Poisson(getPosMu(f));
			Poisson Xn=new Poisson(getPosMu(f));
			for(int cnt=0;cnt<SAMPLE;cnt++){
				T1array[cnt]=T1(Xp.nextInt(),Xn.nextInt());
			}
		}else if(s.equals("Negative-Binomial")){
			throw new UnsupportedOperationException("error: PDF \""+PDF+
					"\" is not implemented!");
		}else{
			throw new IllegalStateException("Error: PDF not recognized!");
		}
		return T1array;
	}

	private double[] sampleT1Values(Feature f,int ci,int cj){
		double[] T1array=new double[SAMPLE];
		String s=featurePdf.get(f);
		// Sample from PDF of Feature f
		if(s.equals("Poisson")){
			Estimate esti=
					((Map<Feature,Estimate>)featureGivenClassParameters.get(ci)).get(f);
			Estimate estj=
					((Map<Feature,Estimate>)featureGivenClassParameters.get(cj)).get(f);
			SortedMap<String,Double> pmi=esti.getPms();
			SortedMap<String,Double> pmj=estj.getPms();
			double lambdai=(pmi.get("lambda")).doubleValue();
			double lambdaj=(pmj.get("lambda")).doubleValue();
			double pci=(classParameters.get(ci)).doubleValue();
			double pcj=(classParameters.get(cj)).doubleValue();
			double lambda=pci/(pci+pcj)*lambdai+pcj/(pci+pcj)*lambdaj;
			Poisson Xi=new Poisson(lambda);
			Poisson Xj=new Poisson(lambda);
			for(int cnt=0;cnt<SAMPLE;cnt++){
				T1array[cnt]=T1(Xi.nextInt(),Xj.nextInt());
			}
		}else if(s.equals("Negative-Binomial")){
			Estimate esti=
					((Map<Feature,Estimate>)featureGivenClassParameters.get(ci)).get(f);
			Estimate estj=
					((Map<Feature,Estimate>)featureGivenClassParameters.get(cj)).get(f);
			SortedMap<String,Double> pmi=esti.getPms();
			SortedMap<String,Double> pmj=estj.getPms();
			TreeMap<String,Number> npi=
					mudelta2np((pmi.get("mu")).doubleValue(),(pmi
							.get("delta")).doubleValue(),1.0);
			TreeMap<String,Number> npj=
					mudelta2np(((Double)pmj.get("mu")).doubleValue(),((Double)pmj
							.get("delta")).doubleValue(),1.0);
			NegativeBinomial Xi=
					new NegativeBinomial(((Integer)(npi.get("n"))).intValue(),
							((Double)(npi.get("p"))).doubleValue());
			NegativeBinomial Xj=
					new NegativeBinomial(((Integer)(npj.get("n"))).intValue(),
							((Double)(npj.get("p"))).doubleValue());
			for(int cnt=0;cnt<SAMPLE;cnt++){
				T1array[cnt]=T1(Xi.nextInt(),Xj.nextInt());
			}
		}else{
			throw new IllegalStateException("Error: PDF not implemented!");
		}
		return T1array;
	}

	private Pair computePValue(double[] t1array,Feature f){
		Arrays.sort(t1array);
		int newLength=0;
		for(int j=0;j<t1array.length;j++){
			if(new Double(t1array[j]).isNaN()){
				newLength=j;
				break;
			}else{
				newLength=t1array.length;
			}
		}
		int greatestIndexBeforeT1Observed=0;
		for(int j=0;j<t1array.length;j++){
			if(t1array[j]<((Double)T1values.get(f)).doubleValue())
				greatestIndexBeforeT1Observed=j;
		}
		Pair p=
				new Pair(((double)(newLength-greatestIndexBeforeT1Observed))/
						((double)newLength),f);
		return p;
	}

	private Pair computePValue(double[] t1array,Feature f,int ci,int cj){
		Arrays.sort(t1array);
		int newLength=0;
		for(int j=0;j<t1array.length;j++){
			if(new Double(t1array[j]).isNaN()){
				newLength=j;
				break;
			}else{
				newLength=t1array.length;
			}
		}
		int greatestIndexBeforeT1Observed=0;
		for(int j=0;j<t1array.length;j++){
			Double[][] observedT1=(Double[][])T1valuesMany.get(f);
			if(t1array[j]<observedT1[ci][cj].doubleValue())
				greatestIndexBeforeT1Observed=j;
		}
		Pair p=
				new Pair(((double)(newLength-greatestIndexBeforeT1Observed))/
						((double)newLength),f);
		return p;
	}

	public TreeMap<String,Number> mudelta2np(double mu,double delta,double omega){
		//System.out.println("mu="+mu +", delta="+delta);
		TreeMap<String,Number> np=new TreeMap<String,Number>();
		// from mu,delta to n
		int n=(int)Math.ceil(new Double(mu/delta).doubleValue());
		np.put("n",new Integer(n));
		// from mu,delta to p
		double p=omega*delta;
		np.put("p",new Double(p));
		//System.out.println("n="+n +", p="+p +", omega="+omega);
		return np;
	}

	/** Get the total number of words in an Example */
	public double getLength(Example e){
		double len=0.0;
		for(Iterator<Feature> i=e.featureIterator();i.hasNext();){
			Feature f=i.next();
			len+=e.getWeight(f);
		}
		return len;
	}

	/** Compute the T1 statistic corresponding to the counts in two texts */
	public double T1(int x1,int x2){
		double dx1=new Integer(x1).doubleValue();
		double dx2=new Integer(x2).doubleValue();
		double t=Math.pow((dx1-dx2),2)/(dx1+dx2);
		return t;
	}

	//
	// Accessory Methods
	//

	/** Set REF_LENGTH to the desired value */
	public void setREF_LENGTH(double desiredLength){
		REF_LENGTH=desiredLength;
	}

	/** Set PDF to the desired value */
	public void setPDF(String pdf){
		PDF=pdf;
	}

	/** Set MIN_WORDS to the desired number */
	public void setMIN_WORDS(int number){
		this.MIN_WORDS=number;
	}

	/** Set MAX_WORDS to the desired number */
	public void setMAX_WORDS(int number){
		this.MAX_WORDS=number;
	}

	/** Set SAMPLE SIZE to the desired level */
	public void setSAMPLE(int size){
		this.SAMPLE=size;
	}

	/** Set ALPHA to the desired level */
	public void setALPHA(double desiredLevel){
		this.ALPHA=desiredLevel;
	}

	//
	// Test D^2 statistics

	static public void main(String[] args){
		// define file's locations here
		String path="/Users/eairoldi/cmu.research/8.Text.Learning.Group/src.MISC/";
		String fout="eda-test2.txt";

		try{

			File outFile=new File(path+fout);
			new PrintStream(new FileOutputStream(outFile));

			//
			// CP data & delta^2 stat

			//File dataFile = new File(path+"CPdata1.m3rd");
			File dataFile=new File(path+"webmaster.3rd");
			Dataset data=DatasetLoader.loadFile(dataFile);

			D2TransformLearner f=new D2TransformLearner();
			f.setREF_LENGTH(100.0);
			f.setSAMPLE(10000);
			f.setALPHA(0.01);
			f.setMIN_WORDS(14);
			f.setMAX_WORDS(14);

			InstanceTransform d2=f.batchTrain(data);
			data=d2.transform(data);

			BasicFeatureIndex idx=new BasicFeatureIndex(data);
			for(Iterator<Feature> i=idx.featureIterator();i.hasNext();){
				Feature ft=i.next();
				System.out.println(ft);
			}

		}catch(Exception x){
			System.out.println("error:\n"+x);
		}
	}

}