/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.linear;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.BatchClassifierLearner;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.algorithms.random.Estimate;
import edu.cmu.minorthird.classify.algorithms.random.Estimators;

/**
 * @author Edoardo Airoldi
 * Date: Nov 14, 2004
 */

public class KWayMixtureLearner extends BatchClassifierLearner{

	private double SCALE;

	private String MODEL; // "Naive-Bayes", "Binomial", "Poisson", "Negative-Binomial", "Mixture"

	private String PARAMETERIZATION;

	// constructors
	public KWayMixtureLearner(){
		this.SCALE=10.0;
		this.MODEL="Poisson";
		this.PARAMETERIZATION="default";
	}

	public KWayMixtureLearner(String model){
		this.SCALE=10.0;
		this.MODEL=model;
		this.PARAMETERIZATION="default";
	}

	public KWayMixtureLearner(String model,String parameterization){
		this.SCALE=10.0;
		this.MODEL=model;
		this.PARAMETERIZATION=parameterization;
	}

	public KWayMixtureLearner(String model,String parameterization,double scale){
		this.SCALE=scale;
		this.MODEL=model;
		this.PARAMETERIZATION=parameterization;
	}

	@Override
	public void setSchema(ExampleSchema schema){
		if(ExampleSchema.BINARY_EXAMPLE_SCHEMA.equals(schema)){
			throw new IllegalStateException("can only learn non-binary example data");
		}
	}

	@Override
	public ExampleSchema getSchema(){
		return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
	}

	@Override
	public Classifier batchTrain(Dataset dataset){
		// initialize stuff
		Classifier mc=new MultinomialClassifier();
		((MultinomialClassifier)mc).setScale(SCALE);
		ExampleSchema schema=dataset.getSchema();
		//System.out.println(schema);

		// retrieve valid class names and their sizes
		BasicFeatureIndex index=new BasicFeatureIndex(dataset);
		int numberOfClasses=schema.getNumberOfClasses();
		String[] classLabels=new String[numberOfClasses];
		int[] classSizes=new int[numberOfClasses];
		List<double[]> featureMatrix=new ArrayList<double[]>();
		List<double[]> exampleWeightMatrix=new ArrayList<double[]>();
		for(int i=0;i<numberOfClasses;i++){
			classLabels[i]=schema.getClassName(i);
			((MultinomialClassifier)mc).addValidLabel(new ClassLabel(classLabels[i]));
			classSizes[i]=index.size(classLabels[i]);
			//System.out.println(classSizes[i]); // DEBUG
			double[] featureCounts=new double[classSizes[i]];
			double[] exampleWeights=new double[classSizes[i]];
			featureMatrix.add(featureCounts);
			exampleWeightMatrix.add(exampleWeights);
		}

		// estimate parameters
		double numberOfExamples=(dataset.size());
		double numberOfFeatures=(index.numberOfFeatures());

		double[] countsGivenClass=new double[numberOfClasses];
		double[] examplesGivenClass=new double[numberOfClasses];
		int[] excounter=new int[numberOfClasses];
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example ex=i.next();
			//System.out.println("label="+ex.getLabel().bestClassName().toString());
			int idx=schema.getClassIndex(ex.getLabel().bestClassName().toString());
			int classIndex=((MultinomialClassifier)mc).indexOf(ex.getLabel());
			//System.out.println("classIndex="+classIndex+" idx"+idx);
			if(idx!=classIndex){
				System.out.println("Buzz! Error: incompatible class indeces ...");
				System.exit(1);
			}
			examplesGivenClass[idx]+=1.0;
			for(Iterator<Feature> j=index.featureIterator();j.hasNext();){
				Feature f=j.next();
				countsGivenClass[idx]+=ex.getWeight(f);
				(exampleWeightMatrix.get(idx))[excounter[idx]]+=
						ex.getWeight(f); // SCALE is HERE !!!
			}
			excounter[idx]+=1;
		}

		for(Iterator<Feature> floo=index.featureIterator();floo.hasNext();){
			int[] counter=new int[numberOfClasses];

			// load vector of counts (by class) for feature f
			Feature ft=floo.next();
			for(Iterator<Example> eloo=dataset.iterator();eloo.hasNext();){
				Example ex=eloo.next();
				int idx=schema.getClassIndex(ex.getLabel().bestClassName().toString());
				if(MODEL.equals("Naive-Bayes")){
					(featureMatrix.get(idx))[counter[idx]++]=
							Math.min(1.0,ex.getWeight(ft));
				}else{
					(featureMatrix.get(idx))[counter[idx]++]=ex.getWeight(ft);
				}
			}

			// select model and store parameters
			if(MODEL.equals("Naive-Bayes")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Naive-Bayes");
				for(int j=0;j<numberOfClasses;j++){
					double probabilityOfOccurrence=
							estimateClassProbMLE(1.0,numberOfClasses,
									examplesGivenClass[j],numberOfExamples);
					((MultinomialClassifier)mc).setClassParameter(j,
							probabilityOfOccurrence);

					if(PARAMETERIZATION.equals("default")|PARAMETERIZATION.equals("mean")){
						Estimate mean=
								Estimators.estimateNaiveBayesMean(1.0,numberOfFeatures,
										sum(featureMatrix.get(j)),examplesGivenClass[j]);
						((MultinomialClassifier)mc)
								.setFeatureGivenClassParameter(ft,j,mean);
					}else if(PARAMETERIZATION.equals("weighted-mean")){
						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate mean=
								Estimators.estimateNaiveBayesWeightedMean(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc)
								.setFeatureGivenClassParameter(ft,j,mean);
					}
				}
				((MultinomialClassifier)mc).setFeatureModel(ft,"Naive-Bayes");
			}else if(MODEL.equals("Binomial")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Binomial");
				for(int j=0;j<numberOfClasses;j++){
					double probabilityOfOccurrence=
							estimateClassProbMLE(1.0,numberOfClasses,
									examplesGivenClass[j],numberOfExamples);
					((MultinomialClassifier)mc).setClassParameter(j,
							probabilityOfOccurrence);

					if(PARAMETERIZATION.equals("default")|PARAMETERIZATION.equals("p/N")){
						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate pn=
								Estimators.estimateBinomialPN(countsFeatureGivenClass,
										countsGivenExample,1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,pn);
					}else if(PARAMETERIZATION.equals("mu/delta")){
						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate mudelta=
								Estimators.estimateBinomialMuDelta(countsFeatureGivenClass,
										countsGivenExample,1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								mudelta);
					}
				}
				((MultinomialClassifier)mc).setFeatureModel(ft,"Binomial");
			}else if(MODEL.equals("Poisson")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Poisson");
				for(int j=0;j<numberOfClasses;j++){
					double probabilityOfOccurrence;
					probabilityOfOccurrence=
							estimateClassProbMLE(1.0,numberOfClasses,
									examplesGivenClass[j],numberOfExamples);
					((MultinomialClassifier)mc).setClassParameter(j,
							probabilityOfOccurrence);

					if(PARAMETERIZATION.equals("default")|
							PARAMETERIZATION.equals("weighted-lambda")){
						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate lambda=
								Estimators.estimatePoissonWeightedLambda(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								lambda);
					}else if(PARAMETERIZATION.equals("lambda")){
						Estimate lambda=
								Estimators.estimatePoissonLambda(1.0/SCALE,numberOfFeatures,
										sum(featureMatrix.get(j)),countsGivenClass[j]/
												SCALE); // SCALE is HERE !!!
						//System.out.println(ft+" ["+j+"] ... prob="+((Double)lambda.getPms().get("lambda")).doubleValue());
						//System.out.println("#ft="+numberOfFeatures+" ft|class="+sum( (double[])featureMatrix.get(j) )+" tot|class="+countsGivenClass[j]+"\n");
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								lambda);
					}
				}
				((MultinomialClassifier)mc).setFeatureModel(ft,"Poisson");
			}else if(MODEL.equals("Negative-Binomial")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Negative-Binomial");
				for(int j=0;j<numberOfClasses;j++){
					double probabilityOfOccurrence=
							estimateClassProbMLE(1.0,numberOfClasses,
									examplesGivenClass[j],numberOfExamples);
					((MultinomialClassifier)mc).setClassParameter(j,
							probabilityOfOccurrence);

					if(PARAMETERIZATION.equals("default")|
							PARAMETERIZATION.equals("mu/delta")){
						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate mudelta=
								Estimators.estimateNegativeBinomialMuDelta(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								mudelta);
					}
				}
				((MultinomialClassifier)mc).setFeatureModel(ft,"Negative-Binomial");
			}else if(MODEL.equals("Mixture")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Mixture");

				for(int j=0;j<numberOfClasses;j++){
					double probabilityOfOccurrence;
					probabilityOfOccurrence=
							estimateClassProbMLE(1.0,numberOfClasses,
									examplesGivenClass[j],numberOfExamples);
					((MultinomialClassifier)mc).setClassParameter(j,
							probabilityOfOccurrence);

					double[] countsFeatureGivenClass=featureMatrix.get(j);
					double mean=Estimators.estimateMean(countsFeatureGivenClass);
					double var=Estimators.estimateVar(countsFeatureGivenClass);
					//double max=Estimators.Max(countsFeatureGivenClass);

					//
					// Select model for feature ft

					String model="";
					//if (max==1) { model="Naive-Bayes"; }
					//else if (mean>=var) { model="Binomial"; }
					if(mean>var){
						model="Binomial";
					} // Poisson is used in case d=0
					else if(mean<=var){
						model="Negative-Binomial";
					} // Poisson is used in case d=0
					//else if (mean==var) { model="Poisson"; }
					((MultinomialClassifier)mc).setFeatureModel(ft,model);

					// End Selector
					//

					if(model.equals("Naive-Bayes")){
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate m=
								Estimators.estimateNaiveBayesWeightedMean(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						//Estimate m = Estimators.estimateNaiveBayesMean( 1.0,numberOfFeatures,sum( (double[])featureMatrix.get(j) ),countsGivenClass[j] );
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,m);
					}else if(model.equals("Binomial")){
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						//Estimate pn = Estimators.estimateBinomialPN( countsFeatureGivenClass,countsGivenExample,1.0/numberOfFeatures,SCALE );
						//((MultinomialClassifier)mc).setFeatureGivenClassParameter( ft,j,pn );
						Estimate mudelta=
								Estimators.estimateBinomialMuDelta(countsFeatureGivenClass,
										countsGivenExample,1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								mudelta);
					}else if(model.equals("Poisson")){
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate lambda=
								Estimators.estimatePoissonWeightedLambda(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								lambda);
					}else if(model.equals("Negative-Binomial")){
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						Estimate mudelta=
								Estimators.estimateNegativeBinomialMuDelta(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								mudelta);
					}
				}
			}else if(MODEL.equals("Dirichlet-Poisson MCMC")){
				((MultinomialClassifier)mc).setPrior(1.0/numberOfFeatures);
				((MultinomialClassifier)mc).setUnseenModel("Dirichlet-Poisson MCMC");

				//double[] probabilityOfOccurrence = new double[ numberOfClasses ];
				double[] sumCountsFeatureGivenClass=new double[numberOfClasses];
				double[] sumCountsGivenExample=new double[numberOfClasses];
				Estimate[] lambda=new Estimate[numberOfClasses];

				if(PARAMETERIZATION.equals("default")|
						PARAMETERIZATION.equals("weighted-lambda")){
					for(int j=0;j<numberOfClasses;j++){
						double probabilityOfOccurrence=
								estimateClassProbMLE(1.0,numberOfClasses,
										examplesGivenClass[j],numberOfExamples);
						((MultinomialClassifier)mc).setClassParameter(j,
								probabilityOfOccurrence);

						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						lambda[j]=
								Estimators.estimatePoissonWeightedLambda(
										countsFeatureGivenClass,countsGivenExample,
										1.0/numberOfFeatures,SCALE);
						//System.out.println("lambda["+j+"] = "+lambda[j]);
						//((MultinomialClassifier)mc).setFeatureGivenClassParameter( ft,j,lambda );

						sumCountsFeatureGivenClass[j]=sum(countsFeatureGivenClass);
						sumCountsGivenExample[j]=sum(countsGivenExample);
					}

					double sigSD=
							(lambda[0].getPms().get("lambda")).doubleValue()+
									(lambda[1].getPms().get("lambda")).doubleValue();
					Estimate[] postLambdas=
							Estimators.mcmcEstimateDirichletPoissonTauSigma(lambda,
									new double[]{1e-7,1e-7},new double[]{1.0,150},
									sumCountsFeatureGivenClass[0],sumCountsFeatureGivenClass[1],
									sumCountsGivenExample[0],sumCountsGivenExample[1],
									new double[]{2.0,1.0},0.075,sigSD/10.0,100);

					for(int j=0;j<numberOfClasses;j++){
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,j,
								postLambdas[j]);
					}
				}else if(PARAMETERIZATION.equals("lambda")){
					for(int j=0;j<numberOfClasses;j++){
						double probabilityOfOccurrence=
								estimateClassProbMLE(1.0,numberOfClasses,
										examplesGivenClass[j],numberOfExamples);
						((MultinomialClassifier)mc).setClassParameter(j,
								probabilityOfOccurrence);

						double[] countsFeatureGivenClass=featureMatrix.get(j);
						double[] countsGivenExample=exampleWeightMatrix.get(j);
						lambda[j]=
								Estimators.estimatePoissonLambda(1.0,numberOfFeatures,
										sum(featureMatrix.get(j)),countsGivenClass[j]);
						//((MultinomialClassifier)mc).setFeatureGivenClassParameter( ft,j,lambda );

						sumCountsFeatureGivenClass[j]=sum(countsFeatureGivenClass);
						sumCountsGivenExample[j]=sum(countsGivenExample);
					}

					Estimate[] postLambdas=
							Estimators.mcmcEstimateDirichletPoissonTauSigma(lambda,
									new double[]{1e-7,1e-7},new double[]{1e-7,150},
									sumCountsFeatureGivenClass[0],sumCountsFeatureGivenClass[1],
									sumCountsGivenExample[0],sumCountsGivenExample[1],
									new double[]{2.0,1.0},0.1,0.5,100);

					for(int j=0;j<numberOfClasses;j++){
						((MultinomialClassifier)mc).setFeatureGivenClassParameter(ft,0,
								postLambdas[j]);
					}
				}
				((MultinomialClassifier)mc)
						.setFeatureModel(ft,"Dirichlet-Poisson MCMC");
			}
		}

		// return classifier
		return mc;
	}

	//
	// private methods
	//
	private double estimateClassProbMLE(double classPrior,double numberOfClasses,
			double observedCounts,double totalCounts){
		//return (classPrior+observedCounts)/(numberOfClasses+totalCounts);
		return (classPrior/numberOfClasses+observedCounts)/(1.0+totalCounts);
	}

	private double sum(double[] vec){
		double sum=0.0;
		for(int i=0;i<vec.length;i++){
			sum=sum+vec[i];
		}
		return sum;
	}

//	private TreeMap estimateNegBinMOME(double[] vCnt,double[] vWgt,double prior){
//		double m=0.0;
//		double d=0.0;
//
//		// compute mean
//		int N=vCnt.length;
//		double sumX=0.0;
//		double sumWgt=0.0;
//		double sumWgt2=0.0;
//		for(int i=0;i<N;i++){
//			sumX+=vCnt[i];
//			sumWgt+=vWgt[i];
//			sumWgt2+=Math.pow(vWgt[i],2);
//		}
//		m=(sumX+prior*1.0/SCALE)/(sumWgt+1.0/SCALE);
//		//m=(sumX+prior)/(sumWgt+1.0/SCALE);
//
//		/*StringBuffer str = new StringBuffer(""+vCnt[0]);
//		for(int i=1; i<vCnt.length; i++)
//		{
//		str.append(" "+vCnt[i]);
//		}
//		System.out.println("["+str+"]"); */
//		//System.out.println(". sumX="+sumX+",sumWgt="+sumWgt+",m="+m+",d="+d);
//		// compute intermediate
//		double r;
//		double v=0.0;
//		if(N<=1.0){
//			r=0.0;
//			v=0.0;
//		}else{
//			r=(sumWgt-sumWgt2/sumWgt)/(N-1.0);
//			for(int i=0;i<N;i++){
//				v+=(vWgt[i]*Math.pow(vCnt[i]/vWgt[i]-m,2))/(N-1.0);
//				//v += ( vWgt[i] * Math.pow( vCnt[i]/vWgt[i]-0.0,2 ) ) / (N-1.0);
//			}
//		}
//
//		// compute variance
//		d=Math.max(0.0,(v-m)/(r*m));
//		if(new Double(d).isNaN()){
//			d=0.0;
//		}
//
//		// package results
//		TreeMap mudelta=new TreeMap();
//		mudelta.put("mu",new Double(m));
//		mudelta.put("delta",new Double(d));
//		//System.out.println("m="+m+" d="+d);
//		return mudelta;
//
//	}

	//
	// Test SemiSupervisedNaiveBayesLearner
	//
	static public void main(String[] args){
		Dataset dataset=new BasicDataset();
		/*try {
		// load counts from file
		File fileOfCounts = new File("/Users/eairoldi/cmu.research/minorthird/apps/unlabeledDataset.3rd");
		dataset = DatasetLoader.loadFile(fileOfCounts);
		} catch (Exception e) {
		log.error(e, e);
		System.exit(1);
		}
		System.out.println( "DatasetLoader:\n" + dataset );*/
		dataset=SampleDatasets.sampleData("bayesUnlabeled",false);
		System.out.println("SampleDatasets (bayesUnlabeled):\n"+dataset);
		// do something here ...
	}

}
