/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.algorithms.knn.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import org.apache.log4j.*;

/**
 * Main UI program for the 'classify' package. 
 *
 * @author William Cohen
 */

public class UI 
{
  private static Logger log = Logger.getLogger(UI.class);

  private static final Class[] SELECTABLE_TYPES = new Class[]{
    DataClassificationTask.class, 
    KnnLearner.class, NaiveBayes.class, 
    VotedPerceptron.class,	SVMLearner.class,
    DecisionTreeLearner.class, AdaBoost.class,
    BatchVersion.class, TransformingBatchLearner.class,
    MaxEntLearner.class,
    // transformations
    FrequencyBasedTransformLearner.class, InfoGainTransformLearner2.class, 
    T1InstanceTransformLearner.class, TFIDFTransformLearner.class,
    // sequential learner
    CollinsPerceptronLearner.class, GenericCollinsLearner.class,
    // splitters
    CrossValSplitter.class, RandomSplitter.class, StratifiedCrossValSplitter.class,
  };

  private static final Set LEGAL_OPS = new HashSet(Arrays.asList(new String[]{"train","test","trainTest"}));

  public static class DataClassificationTask implements CommandLineProcessor.Configurable,Saveable
  {
    private Dataset trainData=null, testData=null;
    private String trainDataFilename=null, testDataFilename=null;
    private Splitter splitter=new RandomSplitter(0.7);
    private ClassifierLearner clsLearner=new NaiveBayes();
    private SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
    private File saveAs=null;
    private String saveAsFilename=null;
    private File loadFrom=null;
    private String loadFromFilename=null;
    private boolean sequential=false;
    private boolean showData=false, showResult=false, showTestDetails=false, useGUI=false;
    private String op="trainTest";
    private Object resultToShow=null, resultToSave=null;
    private BasicCommandLineProcessor clp = new MyCLP();

    public class MyCLP extends BasicCommandLineProcessor
    {
	    public void splitter(String s) { splitter = Expt.toSplitter(s); }
	    public void learner(String s) { 
        if (sequential) seqLearner = toSeqLearner(s);
        else clsLearner = Expt.toLearner(s); 
	    }
	    public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	    public void classifierFile(String s) { loadFrom = new File(s); loadFromFilename=s; }
	    public void showData() { showData=true; }
	    public void showResult() { showResult=true; }
	    public void seq() { sequential=true; }
	    public void showTestDetails() { showTestDetails=true; }
	    public void gui() { useGUI=true; }
	    public void data(String s) { 
        trainData = safeToDataset(s);  
        trainDataFilename = s; 
	    }
	    public void test(String s) {  
        testData = safeToDataset(s); 
        testDataFilename = s;
        Iterator it = sequential? ((SequenceDataset)testData).sequenceIterator(): testData.iterator();
        splitter = new FixedTestSetSplitter(it);
	    }
	    public void op(String s) { 
        if (LEGAL_OPS.contains(s)) op = s;
        else throw new IllegalArgumentException("Illegal op "+s+": legal ops are "+LEGAL_OPS);
	    }
	    public void usage()
	    {
        System.out.println("train a classifier: -op train -data INFILE [-learner LEARNER] [-saveAs OUTFILE]");
        System.out.println("  LEARNER is bean shell code that produces a ClassifierLearner.  Load a dataset");
        System.out.println("  from the INFILE and train the LEARNER on it, and optionally save the learned");
        System.out.println("  classifier in OUTFILE");
        System.out.println();
        System.out.println("test a classifier:  -op test -data INFILE1 -classifierFile INFILE2");
        System.out.println("  Load a dataset from INFILE1, and test the classifier stored in INFILE2 on it");
        System.out.println();				
        System.out.println("do an experiment:   -op trainTest -data INFILE [-learner LEARNER] [-splitter SPLITTER]");
        System.out.println("                    -op trainTest -data INFILE [-learner LEARNER] [-test INFILE2]");
        System.out.println("  Load a dataset from INFILE, and perform some sort of train-test experiment, either");
        System.out.println("  cross-validation with the specified splitter, or testing against the dataset in INFILE2.");
        System.out.println("  Sample SPLITTER's: k5 is 5-fold cross-validation, r70 is one 70% train/30% test split.");
        System.out.println();				
        System.out.println("other options:");
        System.out.println("  -showData:          interactively view the training data");
        System.out.println("  -showResult:        interactively view the result of the operation");
        System.out.println("  -showTestDetails:   show more test-set performance details for -op test or -op trainTest");
        System.out.println("  -saveAs:            save the classifier Evaluation for -op test or -op trainTest");
        System.out.println("  -gui:               use a graphical interface to change/set parameters");
        System.out.println("");
        System.out.println("  -seq:               sequential mode: use a sequential datasets and learners");
        System.out.println("                      This mode must be set BEFORE the -data or -learner options appear.");
	    }
    }
    public CommandLineProcessor getCLP() { return new MyCLP(); }
    private Dataset safeToDataset(String s)	
    {
	    try {
        if (s.startsWith("sample:")) return Expt.toDataset(s);
        else if (sequential) return DatasetLoader.loadSequence(new File(s));
        else return DatasetLoader.loadFile(new File(s));
	    } catch (IOException ex) {
        throw new IllegalArgumentException("Error loading '"+s+"': "+ex);
	    } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Error loading '"+s+"': "+ex);
	    }
    }
    private SequenceClassifierLearner toSeqLearner(String s)
    {
	    try {
        bsh.Interpreter interp = new bsh.Interpreter();
        interp.eval("import edu.cmu.minorthird.classify.*;");
        interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
        interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
        interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
        interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
        interp.eval("import edu.cmu.minorthird.classify.transform.*;");
        interp.eval("import edu.cmu.minorthird.classify.semisupervised.*;");
        interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
        return (SequenceClassifierLearner)interp.eval(s);
	    } catch (bsh.EvalError e) {
        throw new IllegalArgumentException("error parsing learnerName '"+s+"':\n"+e);
	    }
    }

    //
    // for gui update
    //
    public String get_operation() { return op; } 		// underscore will sort this up to the top of the list
    public void set_operation(String s) { op=s; }
    public Object[] getAllowed_operationValues() { return LEGAL_OPS.toArray(); }
    public Splitter getSplitter() { return splitter; }
    public void setSplitter(Splitter s) { splitter=s; }
    public ClassifierLearner getLearner() { return clsLearner; }
    public void setLearner(ClassifierLearner c) { clsLearner=c; }
    public SequenceClassifierLearner getLearnerInSequentialMode() { return seqLearner; }
    public void setLearnerInSequentialMode(SequenceClassifierLearner c) { seqLearner=c; }
    public String getSaveAsFilename() { return saveAsFilename; }
    public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
    public String getClassifierFilename() { return loadFromFilename; }
    public void setClassifierFilename(String s) { loadFromFilename=s; loadFrom=new File(s);}
    //public boolean getShowData() { return showData; }
    //public void setShowData(boolean flag) { showData=flag; }
    //public boolean getShowResult() { return showResult; }
    //public void setShowResult(boolean flag) { showResult=flag; }
    public boolean getShowTestDetails() { return showTestDetails; }
    public void setShowTestDetails(boolean flag) { showTestDetails=flag; }
    public void setSequentialMode(boolean flag) { sequential=flag; }
    public boolean getSequentialMode() { return sequential; }
    public String getDatasetFilename() { return trainDataFilename; }
    public void setDatasetFilename(String s) { trainData = safeToDataset(s); trainDataFilename=s; }
    public String getTestDatasetFilename() { return trainDataFilename; }
    public void setTestDatasetFilename(String s) { testData = safeToDataset(s); testDataFilename=s; }

    // main action
    public void doMain()
    {
	    if (trainData==null) {
        System.out.println("The training data needs to be specified with the -data option.");
        return;
	    }
	    if (sequential && (!(trainData instanceof SequenceDataset))) {
        System.out.println("The training data should be a sequence dataset");
        return;
	    }
	    if (showData) new ViewerFrame("Training data",trainData.toGUI());
	    if ("test".equals(op)) {
        try {
          if (loadFrom==null) {
            System.out.println("The classifier to test needs to be specified with -classifierFile option.");
            return;
          }
          Evaluation e = new Evaluation(trainData.getSchema());
          Object c;
          if (sequential) {
            c = IOUtil.loadSerialized(loadFrom);
            e.extend((SequenceClassifier)c, (SequenceDataset)trainData);
          } else {
            c = IOUtil.loadSerialized(loadFrom);
            e.extend((Classifier)c, trainData, 0);
          }
          e.summarize();
          resultToShow = resultToSave = e;
          if (showTestDetails) {
            if (sequential) {
              ClassifiedSequenceDataset cd = 
                new ClassifiedSequenceDataset((SequenceClassifier)c, (SequenceDataset)trainData);
              resultToShow = cd;
            } else {
              ClassifiedDataset cd = new ClassifiedDataset((Classifier)c, trainData);
              resultToShow = cd;
            }
          }
        } catch (IOException ex) {
          log.error("Can't load classifier from "+loadFromFilename+": "+ex);
          return;
        }
	    } else if ("train".equals(op)) {
        if (sequential) {
          DatasetSequenceClassifierTeacher teacher = new DatasetSequenceClassifierTeacher((SequenceDataset)trainData);
          SequenceClassifier c = teacher.train(seqLearner);
          resultToShow = resultToSave = c;
        } else {
          ClassifierTeacher teacher = new DatasetClassifierTeacher(trainData);
          Classifier c = teacher.train(clsLearner);
          resultToShow = resultToSave = c;
        }
	    } else if ("trainTest".equals(op)) {
        if (showTestDetails && sequential) {
          CrossValidatedSequenceDataset cvd 
            = new CrossValidatedSequenceDataset(seqLearner,(SequenceDataset)trainData,splitter);
          resultToShow = cvd;
          resultToSave = cvd.getEvaluation();
        } else if (!showTestDetails && sequential) {
          Evaluation e = Tester.evaluate(seqLearner,(SequenceDataset)trainData,splitter);
          resultToShow = resultToSave = e;
        } else if (showTestDetails && !sequential) {
          CrossValidatedDataset cvd = new CrossValidatedDataset(clsLearner, trainData, splitter);
          resultToShow = cvd;
          resultToSave = cvd.getEvaluation();
        } else if (!showTestDetails && !sequential) {
          Evaluation e = Tester.evaluate(clsLearner,trainData,splitter);
          resultToShow = resultToSave = e;
        }
        ((Evaluation)resultToSave).summarize();
        // attach all the command-line arguments to the resultToSave, as properties
        for (Iterator i=clp.propertyList().iterator(); i.hasNext(); ) {
          String prop = (String)i.next();
          ((Evaluation)resultToSave).setProperty(prop,clp.propertyValue(prop));
        }
	    } else {
        log.error("Illegal operation: "+op);
        return;
	    }
	    if (showResult) new ViewerFrame("Result", new SmartVanillaViewer(resultToShow));
	    if (saveAs!=null) {
        if (IOUtil.saveSomehow(resultToSave,saveAs)) {
          log.info("Result saved in "+saveAs);
        } else {
          log.error("Can't save "+resultToSave.getClass()+" to "+saveAs);
        }
	    }
    }
    public Object getMainResult()
    {
	    return resultToShow;
    }

    // 
    // implements Saveable
    // 
    public String[] getFormatNames() { return clp.getFormatNames(); }
    public String getExtensionFor(String format) { return clp.getExtensionFor(format); }
    public void saveAs(File file, String format) throws IOException { clp.saveAs(file,format); }
    public Object restore(File file) throws IOException
    {
	    DataClassificationTask task = new DataClassificationTask();
	    task.clp.config(file.getAbsolutePath());
	    return task;
    }

    // gui around main action
    public void callMain(final String[] args)
    {
	    try {
        clp.processArguments(args);
        if (!useGUI) {
          doMain();
        }
        else {
          final Viewer v = new ComponentViewer() {
              public JComponent componentFor(Object o) 
              {
                Viewer ts = new TypeSelector(SELECTABLE_TYPES, "selectableTypes.txt", DataClassificationTask.class); 
                ts.setContent(o);								
								
                // we'll put the type selector in a nice panel
                JPanel panel = new JPanel();
                panel.setBorder(new TitledBorder(StringUtil.toString(args,"Command line: ",""," ")));
                panel.setLayout(new GridBagLayout());
                GridBagConstraints gbc;
								
                // another panel to allow parameter modifications
                JPanel subpanel1 = new JPanel();
                subpanel1.setBorder(new TitledBorder("Parameter modification"));
                //subpanel1.add(new JLabel("Use the edit button to change the parameters given in the command line"));
                subpanel1.add( ts );
                gbc = Viewer.fillerGBC(); gbc.weighty=0; 
                panel.add( subpanel1, gbc  ); 
								
                // another panel for error messages and other outputs
                JPanel errorPanel = new JPanel();
                errorPanel.setBorder(new TitledBorder("Error messages and output"));
                final JTextArea errorArea = new JTextArea(20,100);
                errorArea.setFont( new Font("monospaced",Font.PLAIN,12) );
                errorPanel.add(new JScrollPane(errorArea));
								
                // a control panel for controls
                JPanel subpanel2 = new JPanel();
                subpanel2.setBorder(new TitledBorder("Execution controls"));
                // a button to show the results
                final JButton viewButton = new JButton(new AbstractAction("View results") {
                    public void actionPerformed(ActionEvent event) {
                      Viewer rv = new SmartVanillaViewer();
                      rv.setContent( getMainResult() );
                      ViewerFrame f = new ViewerFrame("Result", rv);
                    }
                  });
                viewButton.setEnabled(false);
                // a button to start this thread
                JButton goButton = new JButton(new AbstractAction("Start task") {
                    public void actionPerformed(ActionEvent event) {
                      Thread thread = new Thread() { 
                          public void run() { 
                            viewButton.setEnabled(false);
                            try {
                              PrintStream oldSystemOut = System.out;
                              ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                              System.setOut(new PrintStream(outBuffer));
                              doMain(); 
                              errorArea.append(outBuffer.toString());
                              System.setOut(oldSystemOut);
                            } catch (Exception e) {
                              System.out.println("Error:"+e.toString());
                              errorArea.append("Error: "+e.toString());
                            }
                            viewButton.setEnabled(getMainResult()!=null);
                          }
                        };
                      thread.start();
                    }
                  });
                // and a button to show the current labels
                JButton showLabelsButton = new JButton(new AbstractAction("Show train data") {
                    public void actionPerformed(ActionEvent ev) {
                      new ViewerFrame("Labeled TextBase", new SmartVanillaViewer(trainData));
                    }
                  });
                // and a button to clear the errorArea
                JButton clearButton = new JButton(new AbstractAction("Clear window") {
                    public void actionPerformed(ActionEvent ev) {
                      errorArea.setText("");
                    }
                  });
                // and a button for help
                JButton helpParamsButton = new JButton(new AbstractAction("Parameters") {
                    public void actionPerformed(ActionEvent ev) {
                      PrintStream oldSystemOut = System.out;
                      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                      System.setOut(new PrintStream(outBuffer));
                      clp.usage(); 
                      errorArea.append(outBuffer.toString());
                      System.setOut(oldSystemOut);
                    }
                  });
                subpanel2.add( goButton );
                subpanel2.add( viewButton );
                subpanel2.add( showLabelsButton );
                subpanel2.add( clearButton );
                subpanel2.add( new JLabel("Help:") );
                subpanel2.add( helpParamsButton );
                gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=1;
                panel.add(subpanel2, gbc );
								
                gbc = Viewer.fillerGBC(); gbc.weighty=1; gbc.gridy=2;
                panel.add(errorPanel, gbc);
								
                // now some progress bars
                JProgressBar progressBar1 = new JProgressBar();
                JProgressBar progressBar2 = new JProgressBar();
                JProgressBar progressBar3 = new JProgressBar();
                ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1, progressBar2,progressBar3});
                gbc = Viewer.fillerGBC();	gbc.weighty=0; gbc.gridy=3;
                panel.add(progressBar1, gbc);
                gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=4;
                panel.add(progressBar2, gbc);
                gbc = Viewer.fillerGBC(); gbc.weighty=0; gbc.gridy=5;
                panel.add(progressBar3, gbc);
								
                return panel;
              }
            };
          v.setContent(this);
          String className = this.getClass().toString().substring("class ".length());
          ViewerFrame f = new ViewerFrame(className,v);
        }
	    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Use option -help for help");
	    }
    }
  }

  public static void main(String[] args) {
    new DataClassificationTask().callMain(args);
  }
}


