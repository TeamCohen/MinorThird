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
 * @author Cameron Williams
 */
public class ClassifyCommandLineUtil extends BasicCommandLineProcessor
{
    /*private static final Class[] SELECTABLE_TYPES = new Class[]{ 
	ClassifyCommandLineUtil.Learner.SeqLearner.class, ClassifyCommandLineUtil.Learner.ClassLearner.class,
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
	};*/

    private static final Set LEGAL_OPS = new HashSet(Arrays.asList(new String[]{"train","test","trainTest"}));

    private static Dataset safeToDataset(String s, boolean b)	
    {
	boolean sequential = b;
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
    private static SequenceClassifierLearner toSeqLearner(String s)
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
    public static class BaseParams extends BasicCommandLineProcessor {
	public boolean sequential=false;
	public String op="trainTest";
	public Dataset trainData=null;
	public String trainDataFilename = null;
	public boolean showData=false;

	public void seq() { sequential=true; }
	public void data(String s) { 
	    trainData = safeToDataset(s, sequential);  
	    trainDataFilename = s; 
	}
	public void op(String s) { 
	    if (LEGAL_OPS.contains(s)) op = s;
	    else throw new IllegalArgumentException("Illegal op "+s+": legal ops are "+LEGAL_OPS);
	}

	//for gui
	public String get_operation() { return op; } 		// underscore will sort this up to the top of the list
	public void set_operation(String s) { op=s; }
	public Object[] getAllowed_operationValues() { return LEGAL_OPS.toArray(); }	
	public void setSequentialMode(boolean flag) { sequential=flag; }
	public boolean getSequentialMode() { return sequential; }
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { trainData = safeToDataset(s, sequential); trainDataFilename=s; }
    }
    public static class Learner extends BasicCommandLineProcessor{
	public static class SequentialLnr extends ClassifyCommandLineUtil.Learner {
	    public SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
	    
	    public SequenceClassifierLearner getLearnerInSequentialMode() { return seqLearner; }
	    public void setLearnerInSequentialMode(SequenceClassifierLearner c) { seqLearner=c; }
	}
	public static class ClassifierLnr extends ClassifyCommandLineUtil.Learner {
	    public ClassifierLearner clsLearner = new NaiveBayes();
	    
	    public ClassifierLearner getLearner() { return clsLearner; }
	    public void setLearner(ClassifierLearner c) { clsLearner=c; }
	}
    }
    public static class TrainParams extends ClassifyCommandLineUtil {
	public ClassifierLearner clsLearner=new NaiveBayes();
	public SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
	public File saveAs = null;
	public String saveAsFilename=null;
	public boolean showData=false, showResult=false, showTestDetails=false;
	public Object resultToShow=null, resultToSave=null;
	public boolean sequential=false;
	public ClassifyCommandLineUtil.Learner.SequentialLnr seqLnr = new ClassifyCommandLineUtil.Learner.SequentialLnr();
	public ClassifyCommandLineUtil.Learner.ClassifierLnr classifierLnr = new ClassifyCommandLineUtil.Learner.ClassifierLnr();
	public ClassifyCommandLineUtil.Learner lnr = classifierLnr;       
	public Dataset trainData=null;
	public String trainDataFilename = null;
	public ClassifyCommandLineUtil.BaseParams base;

	public void setBase(ClassifyCommandLineUtil.BaseParams b) {
	    base = b;
	    sequential = base.sequential;
	}
	public void seq() { sequential=true; }
	public void data(String s) { 
	    trainData = safeToDataset(s, sequential);  
	    trainDataFilename = s; 
	    base.trainData = trainData;
	    base.trainDataFilename = trainDataFilename;
	}

	public void setSequential(boolean b){ 
	    sequential=b; 
	    if (b)
	    	lnr = seqLnr;
	    else 
		lnr = classifierLnr;
	}
	public void learner(String s) { 
	    if (sequential) seqLearner = toSeqLearner(s);
	    else clsLearner = Expt.toLearner(s); 
	}
	public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	public void showData() { 
	    showData=true; 
	    base.showData = true;
	}
	public void showResult() { showResult=true; }
	public void showTestDetails() { showTestDetails=true; }

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, sequential); 
	    trainDataFilename=s; 
	    base.trainData = trainData;
	    base.trainDataFilename = trainDataFilename;
	}
	public ClassifyCommandLineUtil.Learner get_LearnerParameters() { return lnr; }
	public void set_LearnerParameters(ClassifyCommandLineUtil.Learner learn) { 
	    lnr = learn; 
	    if(lnr instanceof ClassifyCommandLineUtil.Learner.SequentialLnr)
		setSequential(true);
	    else setSequential(false);
	}
	public String getSaveAsFilename() { return saveAsFilename; }
	public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
	public boolean getShowTestDetails() { return showTestDetails; }
	public void setShowTestDetails(boolean flag) { showTestDetails=flag; }
    }
    public static class TestParams extends ClassifyCommandLineUtil {
	public Dataset testData=null;
	public String testDataFilename=null;
	public File saveAs = null;
	public String saveAsFilename=null;
	public File loadFrom=null;
	public String loadFromFilename=null;
	public boolean showData=false, showResult=false, showTestDetails=false;
	public Object resultToShow=null, resultToSave=null;
	public boolean sequential=false;
	public Splitter splitter;
	public ClassifyCommandLineUtil.BaseParams base;

	public void setBase(ClassifyCommandLineUtil.BaseParams b) {
	    base = b;
	    sequential = base.sequential;
	}
	public void setSequential(boolean b){ 
	    sequential=b; 
	    
	}
	public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	public void classifierFile(String s) { loadFrom = new File(s); loadFromFilename=s; }
	public void showData() { 
	    showData=true; 
	    base.showData=true;
	}
	public void showResult() { showResult=true; }
	public void seq() { sequential=true; }
	public void showTestDetails() { showTestDetails=true; }
	public void test(String s) {  
	    testData = safeToDataset(s, sequential); 
	    testDataFilename = s;
	    Iterator it = sequential? ((SequenceDataset)testData).sequenceIterator(): testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}

	//for gui
	public String getSaveAsFilename() { return saveAsFilename; }
	public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
	public String getClassifierFilename() { return loadFromFilename; }
	public void setClassifierFilename(String s) { loadFromFilename=s; loadFrom=new File(s);}
	public boolean getShowTestDetails() { return showTestDetails; }
	public void setShowTestDetails(boolean flag) { showTestDetails=flag; }	
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, sequential); testDataFilename=s; }
    }
    public static class TrainTestParams extends ClassifyCommandLineUtil {
	public Dataset trainData = null, testData=null;
	public String trainDataFilename = null, testDataFilename=null;
	public Splitter splitter = new RandomSplitter(0.7);
	public ClassifierLearner clsLearner=new NaiveBayes();
	public SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
	public File saveAs = null;
	public String saveAsFilename=null;
	public File loadFrom=null;
	public String loadFromFilename=null;
	public boolean showData=false, showResult=false, showTestDetails=false;
	public Object resultToShow=null, resultToSave=null;
	public boolean sequential=false;
	public ClassifyCommandLineUtil.Learner.SequentialLnr seqLnr = new ClassifyCommandLineUtil.Learner.SequentialLnr();
	public ClassifyCommandLineUtil.Learner.ClassifierLnr classifierLnr = new ClassifyCommandLineUtil.Learner.ClassifierLnr();
	public ClassifyCommandLineUtil.Learner lnr = classifierLnr;
	public ClassifyCommandLineUtil.BaseParams base;

	public void setBase(ClassifyCommandLineUtil.BaseParams b) {
	    base = b;
	    sequential = base.sequential;
	}
	public void seq() { sequential=true; }
	public void data(String s) { 
	    trainData = safeToDataset(s, sequential);  
	    trainDataFilename = s;
	    base.trainData = trainData;
	    base.trainDataFilename = trainDataFilename;
	}
	public void setSequential(boolean b){ sequential=b; }
	public void splitter(String s) { splitter = Expt.toSplitter(s); }
	public void learner(String s) { 
	    if (sequential) seqLearner = toSeqLearner(s);
	    else clsLearner = Expt.toLearner(s); 
	}
	public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	public void classifierFile(String s) { loadFrom = new File(s); loadFromFilename=s; }
	public void showData() { 
	    showData=true; 
	    base.showData = true;
	}
	public void showResult() { showResult=true; }
	public void showTestDetails() { showTestDetails=true; }
	public void test(String s) {  
	    testData = safeToDataset(s, sequential); 
	    testDataFilename = s;
	    Iterator it = sequential? ((SequenceDataset)testData).sequenceIterator(): testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}

	//for gui
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }
	public ClassifyCommandLineUtil.Learner get_LearnerParameters() { return lnr; }
	public void set_LearnerParameters(ClassifyCommandLineUtil.Learner learn) { 
	    lnr = learn; 
	    if(lnr instanceof ClassifyCommandLineUtil.Learner.SequentialLnr)
		setSequential(true);
	    else setSequential(false);
	}
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, sequential); 
	    trainDataFilename=s; 
	    base.trainData = trainData;
	    base.trainDataFilename = trainDataFilename;
	}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, sequential); testDataFilename=s; }
	public String getSaveAsFilename() { return saveAsFilename; }
	public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
	public String getClassifierFilename() { return loadFromFilename; }
	public void setClassifierFilename(String s) { loadFromFilename=s; loadFrom=new File(s);}
	public boolean getShowTestDetails() { return showTestDetails; }
	public void setShowTestDetails(boolean flag) { showTestDetails=flag; }
	public void setTestDatasetFilename(String s) { testData = safeToDataset(s, sequential); testDataFilename=s; }
    }
}