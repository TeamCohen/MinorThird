package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Implementation of the methods described in:
 * K. Nigam, A. McCallum, S. Thrun and T. Mitchell.  Text Classifiaction from
 * labeled and unlabeled documents using EM.  W. Choen editor,  Machine Learning,
 * 1999.
 *
 * @author Edoardo Airoldi
 * Date: Mar 13, 2004
 */

public class SemiSupervisedNaiveBayesLearner extends SemiSupervisedBatchClassifierLearner
{
   static private Logger log = Logger.getLogger(SemiSupervisedNaiveBayesLearner.class);

   private int MAX_ITER = 1000;
   private Instance.Looper iteratorOverUnlabeled;

   // constructors
   public SemiSupervisedNaiveBayesLearner() {;}
   public SemiSupervisedNaiveBayesLearner(int iterations) { this.MAX_ITER=iterations; }


   public void setSchema(ExampleSchema schema) { ; }
   public void setInstancePool(Instance.Looper i) { this.iteratorOverUnlabeled=i; }

   public Classifier batchTrain(SemiSupervisedDataset dataset)
   {
      Classifier mc = new MultinomialClassifier();
      //System.out.println(dataset);

      // 1. retrieve valid class names
      int numberOfClasses = 0;
      for (Example.Looper i=dataset.iterator(); i.hasNext(); )
      {
         Example ex = i.nextExample();
         if ( !((MultinomialClassifier)mc).isPresent(ex.getLabel()) )
         {
            ((MultinomialClassifier)mc).addValidLabel(ex.getLabel());
            numberOfClasses += 1;
         }
      }

      // 2. estimate parameters using labeled examples
      BasicFeatureIndex index = new BasicFeatureIndex(dataset);
      double[] countsGivenClass = new double[numberOfClasses];
      double[] examplesGivenClass = new double[numberOfClasses];;
      // 2.1. toStal counts
      double numberOfExamples = ((double)dataset.size());
      double numberOfFeatures = ((double)index.numberOfFeatures());
      for(Example.Looper i=dataset.iterator(); i.hasNext(); )
      {
         Example ex = i.nextExample();
         int classIndex = ((MultinomialClassifier)mc).indexOf(ex.getLabel());
         //System.out.println("cllassIndex="+classIndex);
         examplesGivenClass[ classIndex ] += 1.0;
         for (Feature.Looper j=index.featureIterator(); j.hasNext();)
         {
            Feature f = j.nextFeature();
            countsGivenClass[ classIndex ] += ex.getWeight(f);
         }
      }
      for (int j=0; j<numberOfClasses; j++)
      {
         //System.out.println("classes="+numberOfClasses+" ex|class="+examplesGivenClass[j]+" examples="+numberOfExamples);
         double probabilityOfOccurrence = estimateClassProbMLE( 1.0,(double)numberOfClasses,examplesGivenClass[j],numberOfExamples );
         ((MultinomialClassifier)mc).setClassParameter( j,probabilityOfOccurrence );
         //System.out.println("classP="+probabilityOfOccurrence);
      }
      // 2.2. loop features
      for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
         Feature f = i.nextFeature();
         // 2.2.1. retrieve counts by feature
         double[] countsFeatureGivenClass = new double[numberOfClasses];
         for (int j=0; j<index.size(f); j++)
         {
            Example ex = index.getExample(f,j);
            int classIndex = ((MultinomialClassifier)mc).indexOf(ex.getLabel());
            countsFeatureGivenClass[ classIndex ] += ex.getWeight(f);
         }
         // 2.2.2. estimate parameters of MultinomialClassifier for a certain (feature,class)
         for (int j=0; j<numberOfClasses; j++)
         {
            //System.out.println("feature="+f+" class="+j+" label="+((MultinomialClassifier)mc).getLabel(j));
            //System.out.println("features="+numberOfFeatures+" fCnt|class="+countsFeatureGivenClass[j]+" cnt|class="+countsGivenClass[j]);
            double probabilityOfOccurrence =
                estimateFeatureProbMLE( 1.0,numberOfFeatures,countsFeatureGivenClass[j],countsGivenClass[j] );
            ((MultinomialClassifier)mc).setFeatureGivenClassParameter( f,j,probabilityOfOccurrence );
            //System.out.println("prob="+probabilityOfOccurrence);
         }
         ((MultinomialClassifier)mc).setFeatureModel( f,"Binomial" );
      }

      // 3. assign lables using classifier
      Dataset unlabeledDataset = new BasicDataset();
      Instance.Looper il = new Instance.Looper( iteratorOverUnlabeled );
      for (Instance.Looper i=il; i.hasNext();)
      {
         Instance mi = i.nextInstance();
         System.out.println(mi);
         ClassLabel estimatedClassLabel = mc.classification(mi);
         unlabeledDataset.add( new Example(mi,estimatedClassLabel) );
      }
      //System.out.println(unlabeledDataset);

      // 4. initialize log-likelihood
      double logLik = Double.NEGATIVE_INFINITY;
      double previousLogLik;

      // 5. loop until convergence
      int iter = 0;
      boolean hasConverged = false;
      while ( iter<MAX_ITER & !hasConverged )
      {
         // 5.1. initialization
         previousLogLik = logLik;
         logLik = 0.0;
         //Example.Looper el = new Example.Looper( dataset.iterator() );
         Dataset combinedDataset = new BasicDataset();
         for (Example.Looper i=dataset.iterator(); i.hasNext();)
         {
            combinedDataset.add( i.nextExample() );
         }
         //el = new Example.Looper( unlabeledDataset.iterator() );
         for (Example.Looper i=unlabeledDataset.iterator(); i.hasNext();)
         {
            combinedDataset.add( i.nextExample() );
         }
         //System.out.println(combinedDataset);

         // 5.2. estimates parameters using all examples
         ((MultinomialClassifier)mc).reset();
         index = new BasicFeatureIndex(combinedDataset);
         countsGivenClass = new double[numberOfClasses];
         examplesGivenClass = new double[numberOfClasses];;
         // 5.2.1. toStal counts
         numberOfExamples = ((double)combinedDataset.size());
         numberOfFeatures = ((double)index.numberOfFeatures());
         //el = new Example.Looper( dataset.iterator() );
         for(Example.Looper i=dataset.iterator(); i.hasNext(); )
         {
            Example ex = i.nextExample();
            int classIndex = ((MultinomialClassifier)mc).indexOf(ex.getLabel());
            //System.out.println("cllassIndex="+classIndex);
            examplesGivenClass[ classIndex ] += 1.0;
            for (Feature.Looper j=index.featureIterator(); j.hasNext();)
            {
               Feature f = j.nextFeature();
               countsGivenClass[ classIndex ] += ex.getWeight(f);
            }
         }
         for (int j=0; j<numberOfClasses; j++)
         {
            //System.out.println("classes="+numberOfClasses+" ex|class="+examplesGivenClass[j]+" examples="+numberOfExamples);
            double probabilityOfOccurrence = estimateClassProbMLE( 1.0,(double)numberOfClasses,examplesGivenClass[j],numberOfExamples );
            ((MultinomialClassifier)mc).setClassParameter( j,probabilityOfOccurrence );
            //System.out.println("classP="+probabilityOfOccurrence);
         }
         // 5.2.2. loop features
         for (Feature.Looper i=index.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();
            // 5.2.2.1. retrieve counts by feature
            double[] countsFeatureGivenClass = new double[numberOfClasses];
            for (int j=0; j<index.size(f); j++)
            {
               Example ex = index.getExample(f,j);
               int classIndex = ((MultinomialClassifier)mc).indexOf(ex.getLabel());
               countsFeatureGivenClass[ classIndex ] += ex.getWeight(f);
            }
            // 5.5.2.2. estimate parameters of MultinomialClassifier for a certain (feature,class)
            for (int j=0; j<numberOfClasses; j++)
            {
               //System.out.println("feature="+f+" class="+j+" label="+((MultinomialClassifier)mc).getLabel(j));
               //System.out.println("features="+numberOfFeatures+" fCnt|class="+countsFeatureGivenClass[j]+" cnt|class="+countsGivenClass[j]);
               double probabilityOfOccurrence =
                   estimateFeatureProbMLE( 1.0,numberOfFeatures,countsFeatureGivenClass[j],countsGivenClass[j] );
               ((MultinomialClassifier)mc).setFeatureGivenClassParameter( f,j,probabilityOfOccurrence );
               //System.out.println("prob="+probabilityOfOccurrence);
            }
            ((MultinomialClassifier)mc).setFeatureModel( f,"Binomial" );
         }

         // 5.3. re-assign labels using current value of parameters
         //unlabeledDataset = new BasicDataset();
         il = new Instance.Looper( iteratorOverUnlabeled );
         for (Instance.Looper i=il; i.hasNext();)
         {
            Instance mi = i.nextInstance();
            System.out.println(mi);
            ClassLabel estimatedClassLabel = mc.classification(mi);
            unlabeledDataset.add( new Example(mi,estimatedClassLabel) );
         }

         // 5.4. compute the log-lik of complete data
         logLik=0.0;
         for (Example.Looper eloo=combinedDataset.iterator(); eloo.hasNext();)
         {
            Example example = eloo.nextExample();
            logLik += ((MultinomialClassifier)mc).getLogLikelihood(example);
         }

         // 5.5. check convergence and iterate
         if ( EMconverged(logLik, previousLogLik, 1e-7, true) )
         {
            hasConverged=true;
            System.out.println("EM converged!");
         }
         else
         {
            System.out.println("iteration="+(iter+1)+" log-likelihood="+logLik);
         }
         iter += 1;
      }

      // 6. return classifier
      return mc;
   }


   //
   // private methods
   //
   private double estimateClassProbMLE(double classPrior, double numberOfClasses, double observedCounts, double totalCounts)
   {
      return (classPrior+observedCounts)/(numberOfClasses+totalCounts);
   }

   private double estimateFeatureProbMLE(double featurePrior, double numberOfFeatures, double observedCounts, double totalCounts)
   {
      return (featurePrior+observedCounts)/(numberOfFeatures+totalCounts);
   }


   /* We have converged if the slope of the log-likelihood function falls below 'threshold',
   * i.e., |f(t) - f(t-1)| / avg < threshold, where avg = (|f(t)| + |f(t-1)|)/2 and
   * f(t) is log lik at iteration t.  'threshold' defaults to 1e-4.
   *
   * This stopping criterion is from Numerical Recipes in C p423
   *
   * Note: If we are doing MAP estimation (using priors), the likelihood can decrase, even
   * though the mode of the posterior is increasing.
   */
   private boolean EMconverged(double loglik, double previousLoglik, double threshold, boolean checkIncreased)
   {
      double epsilon = 2.2204e-16;
      boolean converged = false;
      if (checkIncreased)
      {
         if (loglik - previousLoglik < -1e-3) // allow for a little imprecision
         {
            System.out.println("******likelihood decreased from "+previousLoglik+" to "+loglik);
         }
      }
      double deltaLoglik = Math.abs(loglik - previousLoglik);
      double avgLoglik = (Math.abs(loglik) + Math.abs(previousLoglik) + epsilon)/2;
      if ((deltaLoglik/avgLoglik) < threshold)
      {
         converged = true;
      }
      return converged;
   }


   //
   // Test SemiSupervisedNaiveBayesLearner
   //
   static public void main(String[] args)
   {
      Dataset dataset = new BasicDataset();
      /*try {
      // load counts from file
      File fileOfCounts = new File("/Users/eairoldi/cmu.research/minorthird/apps/unlabeledDataset.3rd");
      dataset = DatasetLoader.loadFile(fileOfCounts);
      } catch (Exception e) {
      log.error(e, e);
      System.exit(1);
      }
      System.out.println( "DatasetLoader:\n" + dataset );*/
      dataset = SampleDatasets.sampleData("bayesUnlabeled",false);
      System.out.println( "SampleDatasets (bayesUnlabeled):\n" + dataset );

   }

}
