package eairoldi.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.random.Estimators;
import edu.cmu.minorthird.classify.algorithms.random.Estimate;
import edu.cmu.minorthird.classify.transform.T1InstanceTransform;
import edu.cmu.minorthird.classify.transform.T1InstanceTransformLearner;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.classify.transform.InfoGainTransformLearner2;

import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * User: Edoardo M. Airoldi  (eairoldi@cs.cmu.edu)
 * Date: Feb 23, 2005
 */

public class EDA
{
  private Dataset data;
  private double SCALE = 10.0;
  private String MODEL;

  public EDA(Dataset data, String model)
  {
     this.data=data;
     this.MODEL = model;
  }

  public void AnalyzeFeatures(PrintStream out)
  {
     // initialize stuff
     ExampleSchema schema = data.getSchema();
     //System.out.println(schema);

     // retrieve valid class names and their sizes
     BasicFeatureIndex index = new BasicFeatureIndex(data);
     int numberOfClasses = schema.getNumberOfClasses();
     String[] classLabels = new String[numberOfClasses];
     int[] classSizes = new int[numberOfClasses];
     ArrayList featureMatrix = new ArrayList();
     ArrayList exampleWeightMatrix = new ArrayList();

     for (int i=0; i<numberOfClasses; i++)
     {
        classLabels[i]=schema.getClassName(i);
        classSizes[i] = index.size(classLabels[i]);
        //System.out.println(classSizes[i]); // DEBUG
        double[] featureCounts = new double[ classSizes[i] ];
        double[] exampleWeights = new double[ classSizes[i] ];
        featureMatrix.add(featureCounts);
        exampleWeightMatrix.add(exampleWeights);
     }

     // estimate parameters
     double numberOfExamples = ((double)data.size());
     double numberOfFeatures = ((double)index.numberOfFeatures());

     double[] countsGivenClass = new double[numberOfClasses];
     double[] examplesGivenClass = new double[numberOfClasses];
     int[] excounter = new int[numberOfClasses];
     for(Example.Looper i=data.iterator(); i.hasNext(); )
     {
        Example ex = i.nextExample();
        //System.out.println("label="+ex.getLabel().bestClassName().toString());
        int idx = schema.getClassIndex( ex.getLabel().bestClassName().toString() );
        //System.out.println("classIndex="+classIndex+" idx"+idx);
        examplesGivenClass[ idx ] += 1.0;
        for (Feature.Looper j=index.featureIterator(); j.hasNext();)
        {
           Feature f = j.nextFeature();
           countsGivenClass[ idx ] += ex.getWeight(f);
           ((double[])exampleWeightMatrix.get(idx))[ excounter[idx] ] += ex.getWeight(f); // SCALE is HERE !!!
        }
        excounter[idx] += 1;
     }

     double[][] del = new double[numberOfClasses][(int)numberOfFeatures];
     int[][] results = new int[numberOfClasses][3]; // 0: v<m, 1: v=m, 2: v>m

     int ftCnt = 0;
     for( Feature.Looper floo=index.featureIterator(); floo.hasNext(); )
     {
        int[] counter = new int[numberOfClasses];

        // load vector of counts (by class) for feature f
        Feature ft = floo.nextFeature();
        for( Example.Looper eloo=data.iterator(); eloo.hasNext(); )
        {
           Example ex = eloo.nextExample();
           int idx = schema.getClassIndex( ex.getLabel().bestClassName().toString() );
           if (MODEL.equals("Naive-Bayes"))
           {
              ((double[])featureMatrix.get(idx))[ counter[idx]++ ] = Math.min(1.0,ex.getWeight(ft));
           } else
           {
              ((double[])featureMatrix.get(idx))[ counter[idx]++ ] = ex.getWeight(ft);
           }
        }

        if (MODEL.equals("Negative-Binomial"))
        {
           for (int j=0; j<numberOfClasses; j++)
           {
              Estimate probabilityOfOccurrence = Estimators.estimateNaiveBayesMean( 1.0,(double)numberOfClasses,examplesGivenClass[j],numberOfExamples );
              double classPrior = ((Double) probabilityOfOccurrence.getPms().get("mean") ).doubleValue();

              double[] countsFeatureGivenClass = (double[])featureMatrix.get(j);
              double[] countsGivenExample = (double[])exampleWeightMatrix.get(j);
              Estimate mudelta = Estimators.estimateNegativeBinomialMuDelta( countsFeatureGivenClass,countsGivenExample,1.0/numberOfFeatures,SCALE );
              double delta = ((Double) mudelta.getPms().get("delta") ).doubleValue();

              // Estimate parameters
              double m = mean(countsFeatureGivenClass);
              double v = variance(countsFeatureGivenClass);
              //System.out.println(". mean="+m+" variance="+v);
              //System.out.println(". p="+(m-v)/m+" N="+Math.pow(m,2)/(m-v));

              del[j][ftCnt] = delta;

              if (v<m) { results[j][0] +=1; }
              else if (v==m) { results[j][1] +=1; } // sparsity :: sum of counts = 1 or 0
              else if (v>m) { results[j][2] +=1; }
           }
        }


        // next feature
        ftCnt += 1;
     }

     // report results
     try {
        for (int c=0; c<numberOfClasses; c++)
        {
           double isDeltaPos = 0.0;
           double docsInC = del[c].length;
           out.println("class :: "+schema.getClassName(c)+" has "+docsInC+" features");
           //StringBuffer buf = new StringBuffer("c(");
           for (int i=0; i<(docsInC-1); i++) {
              //buf.append(del[c][i]+",");
              if (del[c][i]>1e-10) { isDeltaPos+=1.0; }
           }
           //buf.append(del[c][del[c].length-1]+")");
           if (del[c][del[c].length-1]>1e-10) { isDeltaPos+=1.0; }
           //System.out.println(buf.toString());
           out.println("v<m:"+(results[c][0]/docsInC)+" v=m:"+(results[c][1]/docsInC)+" v>m:"+(results[c][2]/docsInC)+" d>0:"+(isDeltaPos/docsInC));
        }
     } catch (Exception x) {;}
  }

  private double mean(double[] vec) {
     double m = 0.0;
     double N = (double) vec.length;
     for (int i=0; i<vec.length; i++)
     {
        m += vec[i];
     }
     m = m/N;
     return m;
  }

  private double variance(double [] vec) {
     double m = 0.0;
     double m2 = 0.0;
     double N = (double) vec.length;
     for (int i=0; i<vec.length; i++)
     {
        m += vec[i];
        m2 += Math.pow(vec[i],2);
     }
     m = m/N;
     m2 = m2/N;
     double v = (m2 - Math.pow(m,2)) *N /(N-1);
     return v;
  }

  //
  // Performs EDA on selected dataset in m3rd format
  //

  static public void main(String[] argv) {

     // define file's locations here
     String path = "C:\\Archive-Projects\\Text-Models\\m3rd-data\\";
     String fout = path+"eda.txt";

     try {
        File outFile = new File(path+fout);
        PrintStream out = new PrintStream(new FileOutputStream(outFile));

        //
        // webmaster & info-gain
        //

        File dataFile = new File(path+"webmaster.3rd");
        int[] levels = new int[]{100,250,500,750,1000};

        for (int l=0; l<levels.length;l++)
        {
           Dataset data = DatasetLoader.loadFile( dataFile );
           out.println("# webmaster & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("webmaster.ig-"+levels[l]+".3rd"));

           EDA eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        Dataset data = DatasetLoader.loadFile( dataFile );
        BasicFeatureIndex fid = new BasicFeatureIndex(data);
        out.println("# webmaster & info-gain :: "+fid.numberOfFeatures());

        EDA eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // webmaster & delta^2 stat
        //

        dataFile = new File(path+"webmaster.3rd");
        levels = new int[]{100,250,500,750,1000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# webmaster & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("webmaster.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# webmaster & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // pr & info-gain
        //

        dataFile = new File(path+"pr_data.3rd");
        levels = new int[]{100,200,300,400,500,600,700};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# pr & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("pr_data.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# pr & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // pr & delta^2 stat
        //

        dataFile = new File(path+"pr_data.3rd");
        levels = new int[]{100,200,300,400,500,600,700};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# pr & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("pr_data.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# pr & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // online & info-gain
        //

        dataFile = new File(path+"online_data.3rd");
        levels = new int[]{25,50,75,100,125,150,175};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# online & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);

           data = t.transform(data);
           DatasetLoader.save(data,new File("online_data.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# online & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // online & delta^2 stat
        //

        dataFile = new File(path+"online_data.3rd");
        levels = new int[]{25,50,75,100,125,150,175};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# online & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("online_data.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# online & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // movies & info-gain
        //

        dataFile = new File(path+"movie-data-all.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,15000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# movie & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("movie-data-all.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# movie & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // movies & delta^2 stat
        //

        dataFile = new File(path+"movie-data-all.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,15000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# movie & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("movie-data-all.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# movie & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-fin & info-gain
        //

        dataFile = new File(path+"roy-data-fin.3rd");
        levels = new int[]{100,500,1000,2500,5000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-fin & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("roy-data-fin.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-fin & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-fin & delta^2 stat
        //

        dataFile = new File(path+"roy-data-fin.3rd");
        levels = new int[]{100,500,1000,2500,5000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-fin & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("roy-data-fin.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-fin & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-ma & info-gain
        //

        dataFile = new File(path+"roy-data-ma.3rd");
        levels = new int[]{100,500,1000,2500,5000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-ma & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("roy-data-ma.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-ma & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-ma & delta^2 stat
        //

        dataFile = new File(path+"roy-data-ma.3rd");
        levels = new int[]{100,500,1000,2500,5000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-ma & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("roy-data-ma.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-ma & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-mix & info-gain
        //

        dataFile = new File(path+"roy-data-mix.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-mix & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("roy-data-mix.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-mix & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // roy-data-ma & delta^2 stat
        //

        dataFile = new File(path+"roy-data-mix.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# roy-data-mix & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("roy-data-mix.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# roy-data-mix & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // spam assassin & info-gain
        //

        dataFile = new File(path+"spamAss2002-3cat.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# spamAss2002-3cat & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("spamAss2002-3cat.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# spamAss2002-3cat & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // spam assassin & delta^2 stat
        //

        dataFile = new File(path+"spamAss2002-3cat.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# spamAss2002-3cat & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("spamAss2002-3cat.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# spamAss2002-3cat & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // fraud detection & info-gain
        //

        dataFile = new File(path+"fraud-3cat.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# fraud-3cat & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("fraud-3cat.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# fraud-3cat & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // fraud detection & delta^2 stat
        //

        dataFile = new File(path+"fraud-3cat.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# fraud-3cat & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("fraud-3cat.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# fraud-3cat & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // 5 newsgroups & info-gain
        //

        dataFile = new File(path+"5news-nohead.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# 5news-nohead & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("5news-nohead.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# 5news-nohead & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // 5 newsgroups & delta^2 stat
        //

        dataFile = new File(path+"5news-nohead.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# 5news-nohead & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("5news-nohead.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# 5news-nohead & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // reuters & info-gain
        //

        dataFile = new File(path+"reuters21578.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# reuters21578 & info-gain :: "+levels[l]);

           InfoGainTransformLearner2 filter = new InfoGainTransformLearner2(levels[l]);
           InstanceTransform t = filter.batchTrain(data);
           data = t.transform(data);
           DatasetLoader.save(data,new File("reuters21578.ig-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# reuters21578 & info-gain :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");

        //
        // reuters & delta^2 stat
        //

        dataFile = new File(path+"reuters21578.3rd");
        levels = new int[]{100,500,1000,2500,5000,10000,20000,30000};

        for (int l=0; l<levels.length;l++)
        {
           data = DatasetLoader.loadFile( dataFile );
           out.println("# reuters21578 & delta^2 stat :: "+levels[l]);

           T1InstanceTransformLearner f = new T1InstanceTransformLearner();
           f.setREF_LENGTH(500.0);
           T1InstanceTransform t1stat = (T1InstanceTransform)f.batchTrain( data );
           t1stat.setALPHA(0.99);
           t1stat.setSAMPLE(10000);
           t1stat.setMAX_WORDS(levels[l]);
           data = t1stat.transform( data );
           DatasetLoader.save(data,new File("reuters21578.d2-"+levels[l]+".3rd"));

           eda = new EDA(data,"Negative-Binomial");
           eda.AnalyzeFeatures(out);
        }

        data = DatasetLoader.loadFile( dataFile );
        fid = new BasicFeatureIndex(data);
        out.println("# reuters21578 & delta^2 stat :: "+fid.numberOfFeatures());

        eda = new EDA(data,"Negative-Binomial");
        eda.AnalyzeFeatures(out);
        out.println("\n");
     }
     catch (IOException e) { e.printStackTrace(); }

     // it's all good  =:-)
     System.exit(0);
  }

}
