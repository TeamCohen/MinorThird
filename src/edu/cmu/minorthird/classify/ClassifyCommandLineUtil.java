/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.algorithms.knn.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.multi.*;
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
    private static final Set LEGAL_OPS = new HashSet(Arrays.asList(new String[]{"train","test","trainTest"}));

    private static Dataset safeToDataset(String s, boolean seq, int multi) {
	boolean sequential = seq;
	try {
	    if (s.startsWith("sample:")) return Expt.toDataset(s);
	    else if (sequential) return DatasetLoader.loadSequence(new File(s));
	    else if (multi > 0) return DatasetLoader.loadMulti(new File(s), multi);
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
    /** Parameters that all experiments have 
     *  Used so that main method only needs to check base
     */
    public static class BaseParams extends BasicCommandLineProcessor {
	public boolean sequential=false;
	public int multi=-1;
	public String op="train";
	public Dataset trainData=null;
	public String trainDataFilename = null;
	public boolean showData=false;
	public boolean showTestDetails;
	public boolean crossDim = false;
	public File saveAs;
	public String saveAsFilename;
	public Object resultToShow=null, resultToSave=null;
	public boolean showResult = false;

	public void seq() { 
	    sequential=true; 
	}
	public void multi(String dim) {
	    multi = new Integer(dim).intValue();
	}
	public void cross() {
	    if(multi <0)
		System.out.println("Warning: Cannot use crossdimensional classification without multiLabels!");
	    crossDim = true;
	}
	public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	public void showData() { 
	    showData=true; 
	}
	public void showResult() { showResult=true; }
	public void showTestDetails() { showTestDetails=true; }
	public void other(String s) {
	    Object o = this;
	    RefUtils.modify(o,s);
	}

	//for gui
	public boolean getSequential() {return sequential;}
	public void setSequential(boolean b) {sequential = b;}
	public int getMulti() { return multi;}
	public void setMulti(int multi) {this.multi = multi;}
	public boolean getCross() {return crossDim;}
	public void setCross(boolean cross) {this.crossDim = cross;}
	public String getSaveAsFilename() { return saveAsFilename; }
	public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
	public boolean getShowTestDetails() { return showTestDetails; }
	public void setShowTestDetails(boolean flag) { showTestDetails=flag; }
	public boolean getShowData() { return showData;}
	public void setShowData(boolean show) {showData = show;}
    }
    /** Generalized class for Leaner... contains classifierLearner and sequentialLearner */
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
    /**  Parameters for Train Classifier */
    public static class TrainParams extends BaseParams {
	public ClassifierLearner clsLearner=new NaiveBayes();
	public SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
	public Object resultToShow=null, resultToSave=null;
	public ClassifyCommandLineUtil.Learner.SequentialLnr seqLnr = new ClassifyCommandLineUtil.Learner.SequentialLnr();
	public ClassifyCommandLineUtil.Learner.ClassifierLnr clsLnr = new ClassifyCommandLineUtil.Learner.ClassifierLnr();
	public ClassifyCommandLineUtil.Learner lnr = clsLnr;     
	public ClassifyCommandLineUtil.BaseParams base;
	
	public void data(String s) { 
	    trainData = safeToDataset(s, sequential, multi);  
	    trainDataFilename = s; 
	}

	public void setSequential(boolean b){ 
	    sequential=b; 
	    if (b)
	    	lnr = seqLnr;
	    else 
		lnr = clsLnr;
	}
	public void learner(String s) { 
	    if (sequential) seqLnr.seqLearner = toSeqLearner(s);
	    else clsLnr.clsLearner = Expt.toLearner(s); 
	    System.out.println(clsLearner);
	}	

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, sequential, multi); 
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
    }
    /** Paramters for Test Classifier */ 
    public static class TestParams extends BaseParams {
	public Dataset testData=null;
	public String testDataFilename=null;
	public File loadFrom=null;
	public String loadFromFilename=null;
	public Object resultToShow=null, resultToSave=null;
	public ClassifyCommandLineUtil.BaseParams base;
	public Splitter splitter = new RandomSplitter(.7);;

	public void setBase(ClassifyCommandLineUtil.BaseParams b) {
	    base = b;
	    sequential = base.sequential;
	}
	public void setSequential(boolean b){ 
	    sequential=b; 
	    
	}
	public void classifierFile(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	}	
	public void loadFrom(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	}
	public void test(String s) {  
	    testData = safeToDataset(s, sequential, multi); 
	    testDataFilename = s;
	    Iterator it; 
	    if(sequential) 
		it = ((SequenceDataset)testData).sequenceIterator();
	    else if (multi > 0)
		it = ((MultiDataset)testData).multiIterator();
	    else 
		it = testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}
	public void splitter(String s) { splitter = Expt.toSplitter(s); }

	//for gui
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }
	public String getClassifierFilename() { return loadFromFilename; }
	public void setClassifierFilename(String s) { loadFromFilename=s; loadFrom=new File(s);}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, sequential, multi); testDataFilename=s; }
    }
    /** Paramters for Test Classifier */ 
    public static class TrainTestParams extends TrainParams {
	public Dataset testData=null;
	public String testDataFilename=null;
	public File loadFrom=null;
	public String loadFromFilename=null;
	public Object resultToShow=null, resultToSave=null;
	public ClassifyCommandLineUtil.BaseParams base;
	public Splitter splitter = new RandomSplitter(.7);;

	public void setBase(ClassifyCommandLineUtil.BaseParams b) {
	    base = b;
	    sequential = base.sequential;
	}
	public void setSequential(boolean b){ 
	    sequential=b; 
	    
	}
	public void classifierFile(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	}
	public void loadFrom(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	}
	public void test(String s) {  
	    testData = safeToDataset(s, sequential, multi); 
	    testDataFilename = s;
	    Iterator it; 
	    if(sequential) 
		it = ((SequenceDataset)testData).sequenceIterator();
	    else if (multi > 0)
		it = ((MultiDataset)testData).multiIterator();
	    else 
		it = testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}
	public void splitter(String s) { splitter = Expt.toSplitter(s); }

	//for gui
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }
	public String getClassifierFilename() { return loadFromFilename; }
	public void setClassifierFilename(String s) { loadFromFilename=s; loadFrom=new File(s);}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, sequential, multi); testDataFilename=s; }
    }

}