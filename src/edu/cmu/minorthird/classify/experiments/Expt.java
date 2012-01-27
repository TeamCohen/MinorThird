/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.experiments;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetClassifierTeacher;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/** Simple experiment on a classifier.
 *
 * @author William Cohen
 */

public class Expt implements CommandLineProcessor.Configurable
{
   private Dataset trainData=null, testData=null;
   private Splitter<Example> splitter=null;
   private ClassifierLearner learner=null;
   private String splitterArg=null,trainArg=null,testArg=null,learnerArg=null;

   private class MyCLP extends BasicCommandLineProcessor
   {
  	 /*
      public void train(String s) {
         try {
            trainData = toDataset(s);
            trainArg = s;
         } catch (IOException ex) {
            throw new IllegalArgumentException("Error loading "+s+": "+ex);
         }
      }
      public void test(String s) {
         try {
            testData = toDataset(s);
            splitter = new FixedTestSetSplitter<Example>(testData.iterator());
            testArg = s;
         } catch (IOException ex) {
            throw new IllegalArgumentException("Error loading "+s+": "+ex);
         }
      }
      public void splitter(String s) {
         splitterArg = s;
         splitter = toSplitter(s);
      }
      public void learner(String s) {
         learner = toLearner(s);
         learnerArg = s;
      }
      */
      @Override
			public void usage() {
         System.out.println("classify.Expt parameters:");
         System.out.println(" -train FILE              training data is in FILE");
         System.out.println(" [-test FILE]             test data is in FILE");
         System.out.println(" [-splitter SPLITTER]     do cross-validation study with the SPLITTER");
         System.out.println(" [-learner LEARNER]       use learner defined by bean-shell command");
         System.out.println();
      }
   }
   @Override
	public CommandLineProcessor getCLP() { return new MyCLP(); }

   public Expt(ClassifierLearner learner,Dataset trainData,Dataset testData)
   {
      this.learner = learner;
      this.trainData = trainData;
      this.testData = testData;
      this.splitter = null;
   }
   public Expt(ClassifierLearner learner,Dataset trainData,Splitter<Example> splitter)
   {
      this.learner = learner;
      this.trainData = trainData;
      this.splitter = splitter;
   }
   /** Convert a set of command-line arguments to an 'experiment'
    * Examples:
    *  -learn \"new NaiveBayes()\" -train sample:toy -split k10          (k-fold CV)
    *  -learn \"new PoissonLearner()\" -train sample:toy -split s10      (stratified s-fold CV)
    *  -learn \"new AdaBoost(new DecisionTreeLearner())\" -train file:foo.data -split r70
    *  -learn \"new AdaBoost()\" -train seqfile:foo.data -split r70
    */
   public Expt(String[] args) throws IOException
   {
      int pos = 0;
      while (pos<args.length) {
         String opt = args[pos++];
         if (opt.startsWith("-tr")) {
            trainData = toDataset(trainArg = args[pos++]);
         } else if (opt.startsWith("-te")) {
            if (splitter!=null) throw new IllegalArgumentException("only one of splitter, testData allowed");
            testData = toDataset(testArg = args[pos++]);
            splitter = new FixedTestSetSplitter<Example>(testData.iterator());
         } else if (opt.startsWith("-spl")) {
            if (splitter!=null) throw new IllegalArgumentException("only one of splitter, testData allowed");
            splitter = toSplitter(splitterArg = args[pos++]);
         } else if (opt.startsWith("-lea")) {
            learner = toLearner(learnerArg = args[pos++]);
         } else if (opt.startsWith("-")) {
            pos++;
         }
      }
      if (trainData==null || learner==null)
         throw new IllegalArgumentException("learner and trainData must be specified");
      if (testData==null && splitter==null)
         splitter = new FixedTestSetSplitter<Example>(trainData.iterator());
   }

   public Evaluation evaluation()
   {
      Evaluation v = Tester.evaluate(learner,trainData,splitter);
      v.setProperty("learner",learnerArg);
      v.setProperty("train",trainArg);
      if (splitterArg!=null) v.setProperty("splitter",splitterArg);
      if (testArg!=null) v.setProperty("test",testArg);
      return v;
   }
   public CrossValidatedDataset crossValidatedDataset(boolean saveTrain)
   {
      return new CrossValidatedDataset(learner,trainData,splitter,saveTrain);
   }
   public Classifier getClassifier()
   {
      return new DatasetClassifierTeacher(trainData).train(learner);
   }

   @Override
	public String toString()
   {
      return
          "[Expt:\n  learner:"+learner+"\n  splitter:"+splitter+"\n  train:\n"+trainData+"  test:\n"+testData+"Expt]";
   }


   /** Decode splitter names.
    */
   static public <T> Splitter<T> toSplitter(String splitterName,Class<T> clazz)
   {
      if (splitterName.charAt(0)=='k') {
         int folds = StringUtil.atoi(splitterName.substring(1,splitterName.length()));
         return new CrossValSplitter<T>(folds);
      }
      if (splitterName.charAt(0)=='r') {
         double pct = StringUtil.atoi(splitterName.substring(1,splitterName.length())) / 100.0;
         return new RandomSplitter<T>(pct);
      }
//      if (splitterName.charAt(0)=='s') {
//         int folds = StringUtil.atoi(splitterName.substring(1,splitterName.length()));
//         return new StratifiedCrossValSplitter(folds);
//      }
      if (splitterName.startsWith("l")) {
         return new LeaveOneOutSplitter<T>();
      }
      throw new IllegalArgumentException("illegal splitterName '"+splitterName+"'");
   }
   
   public static Splitter<Example> toSplitter(String splitterName){
  	 return toSplitter(splitterName,Example.class);
   }


   /** Decode dataset names.  Allowed names are:
    *
    *<ul>
    * <li>sample:foo,
    * <li>sample:foo.test
    * <li>sample:foo.train,
    * <li>file:bar
    * <li>seqfile:bar
    * <li>bar (bar is a filename)
    *</ul>
    */
   static public Dataset toDataset(String datasetName) throws IOException
   {
      String[] words = datasetName.split("\\:");
      if (words.length==1) {
         // file
         return DatasetLoader.loadFile(new File(words[0]));
      }
      if (words.length==2 && "file".equals(words[0])) {
         // file:bar
         return DatasetLoader.loadFile(new File(words[1]));
      }
      if (words.length==2 && "seqfile".equals(words[0])) {
         // file:bar
         return DatasetLoader.loadSequence(new File(words[1]));
      }
      if ("sample".equals(words[0])) {
         String[] parts = words[1].split("\\.");
         if (parts.length==1) {
            //sample:foo
            return SampleDatasets.sampleData(parts[0],false);
         } else if ("test".equals(parts[1])) {
            //sample:foo.test
            return SampleDatasets.sampleData(parts[0],true);
         } else if ("train".equals(parts[1])) {
            //sample:foo.train
            return SampleDatasets.sampleData(parts[0],false);
         }
      }
      throw new IllegalArgumentException("illegal datasetName: "+datasetName);
   }

   /**
    * Decode learner name, which should be a legitimate java constructor,
    * e.g. <code>new NaiveBayes()</code>.
    */
   static public ClassifierLearner toLearner(String learnerName)
   {
      try {
         bsh.Interpreter interp = new bsh.Interpreter();
         interp.eval("import edu.cmu.minorthird.classify.*;");
         interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
         interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
         interp.eval("import edu.cmu.minorthird.classify.algorithms.ranking.*;");
         interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
         interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
         interp.eval("import edu.cmu.minorthird.classify.transform.*;");
         interp.eval("import edu.cmu.minorthird.classify.semisupervised.*;");
         return (ClassifierLearner)interp.eval(learnerName);
      } catch (bsh.EvalError e) {
         throw new IllegalArgumentException("error parsing learnerName '"+learnerName+"':\n"+e);
      }
   }

   static public void main(String[] args)
   {
      try {
         Expt expt = new Expt(args);
         int pos = 0;
         Serializable toSave = null;
         File saveFile = null;
         while (pos<args.length) {
            String opt = args[pos++];
            if (opt.startsWith("-show")) {
               String what = args[pos++];
               if (what.startsWith("eval")) {
                  Evaluation v = expt.evaluation();
                  new ViewerFrame("Evaluation", v.toGUI());
               } else if (what.startsWith("all")) {
                  boolean saveTrain = "all+".equals(what);
                  CrossValidatedDataset cdv = expt.crossValidatedDataset(saveTrain);
                  new ViewerFrame("CrossValidatedDataset", cdv.toGUI());
               } else {
                  throw new IllegalArgumentException("can't show '"+what+"'");
               }
            } else if (opt.startsWith("-save")) {
               String what = args[pos++];
               if (what.startsWith("eval")) {
                  toSave = expt.evaluation();
               } else if (what.startsWith("cla")) {
                  toSave = (Serializable) expt.getClassifier();
               } else {
                  throw new IllegalArgumentException("can't save '"+what+"'");
               }
            } else if (opt.startsWith("-file")) {
               saveFile = new File(args[pos++]);
            } else if (opt.startsWith("-")) {
               pos++;
            }
         }
         if (saveFile!=null && toSave!=null)	{
            IOUtil.saveSerialized(toSave,saveFile);
         }
         if ((saveFile==null) != (toSave==null)) {
            throw new IllegalArgumentException("must specify -file FILE with -save WHAT");
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.out.println(
             "usage: -learn L -train D1 [-split S] [-test D] [-show eval|all|all+] [-save eval|classifier]");
      }
   }
}


