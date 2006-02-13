package edu.cmu.minorthird.classify.ranking;

import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;
import java.io.*;

/** 
 * Learn from examples a GraphSearcher that re-ranks examples based on
 * scores from a learned classifier.
 */

public class RankingExpt
{
    private String dataFileName = null;
    private BatchRankingLearner classifierLearner = new RankingPerceptron();
    private Splitter splitter = new CrossValSplitter();
    private String saveFile = null;
    private StringEncoder encoder = new StringEncoder('%',"$ \t\n");
    private boolean guiFlag = false;

    public CommandLineProcessor getCLP() { return new MyCLP(); }
    public class MyCLP extends BasicCommandLineProcessor 
    {
	public void data(String s) { dataFileName = s; }
	public void splitter(String s) { splitter = Expt.toSplitter(s);	}
	public void saveAs(String s) { saveFile = s; }
	public void learner(String s) { classifierLearner = (BatchRankingLearner)Expt.toLearner(s); }
	public void gui() { guiFlag = true; }
    }

    private RankingEvaluation doExpt() throws IOException,NumberFormatException
    {
	Dataset data = DatasetLoader.loadFile(new File(dataFileName));
	System.out.println("loaded "+data.size()+" examples");

	RankingEvaluation eval = new RankingEvaluation();
	Dataset.Split split = data.split(splitter);
	ProgressCounter pc = new ProgressCounter("train/test", "fold", split.getNumPartitions());
	for (int k=0; k<split.getNumPartitions(); k++) {
	    Dataset train = split.getTrain(k);
	    Dataset test = split.getTest(k);
	    DatasetClassifierTeacher teacher = new DatasetClassifierTeacher(data);
	    BinaryClassifier classifier = (BinaryClassifier)teacher.train(classifierLearner);
	    doTest( classifier, test, eval );
	    pc.progress();
	}
	pc.finished();
	return eval;
    }

    private void doTest( BinaryClassifier classifier, Dataset test, RankingEvaluation eval)
    {
	Map bySubpopMap = BatchRankingLearner.splitIntoRankings(test);
	for (Iterator i=bySubpopMap.keySet().iterator(); i.hasNext(); ) {
	    String subpop = (String)i.next();
	    ArrayList subdata = (ArrayList)bySubpopMap.get(subpop);
	    eval.extend( subpop, subdata, classifier ); 
	}
    }

    public static void main(String[] args) throws IOException,NumberFormatException
    {
	RankingExpt x = new RankingExpt();
	x.getCLP().processArguments(args);
	RankingEvaluation eval = x.doExpt();
	System.out.println(eval.toTable());
	if (x.guiFlag) new ViewerFrame("result", eval.toGUI());
	if (x.saveFile!=null) 
	    IOUtil.saveSomehow(eval, new File(x.saveFile),true);
    }
}
