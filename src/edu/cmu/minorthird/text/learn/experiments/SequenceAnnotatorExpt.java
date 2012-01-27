package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.sequential.BatchSequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.CrossValidatedSequenceDataset;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.text.learn.TextLabelsAnnotatorTeacher;
import edu.cmu.minorthird.ui.Recommended;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Run an annotation-learning experiment based on pre-labeled text , using a
 * sequence learning method, and showing the result of evaluation of the
 * sequence-classification level.
 * 
 * @author William Cohen
 */

public class SequenceAnnotatorExpt{

	private TextLabels labels;

	private Splitter<Example[]> splitter;

	private SequenceClassifierLearner learner;

	private String inputLabel;

	private String tokPropFeats;

	private SequenceDataset sequenceDataset;

	public SequenceAnnotatorExpt(TextLabels labels,Splitter<Example[]> splitter,
			SequenceClassifierLearner learner,String inputLabel){
		this(labels,splitter,learner,inputLabel,null);
	}

	public SequenceAnnotatorExpt(TextLabels labels,Splitter<Example[]> splitter,
			SequenceClassifierLearner learner,String inputLabel,String tokPropFeats){
		this.labels=labels;
		this.splitter=splitter;
		this.learner=learner;
		this.inputLabel=inputLabel;
		this.tokPropFeats=tokPropFeats;
		AnnotatorTeacher teacher=new TextLabelsAnnotatorTeacher(labels,inputLabel);
		Recommended.TokenFE fe=new Recommended.TokenFE();
		if(tokPropFeats!=null)
			fe.setTokenPropertyFeatures(tokPropFeats);
		final int size=learner.getHistorySize();
		BatchSequenceClassifierLearner dummyLearner=
				new BatchSequenceClassifierLearner(){

					@Override
					public void setSchema(ExampleSchema schema){
					}

					@Override
					public SequenceClassifier batchTrain(SequenceDataset dataset){
						return null;
					}

					@Override
					public int getHistorySize(){
						return size;
					}
				};
		SequenceAnnotatorLearner dummy=
				new SequenceAnnotatorLearner(dummyLearner,fe){

					@Override
					public Annotator getAnnotator(){
						return null;
					}
				};
		teacher.train(dummy);
		sequenceDataset=dummy.getSequenceDataset();
	}
	
	public TextLabels getLabels(){
		return labels;
	}
	
	public String getInputLabel(){
		return inputLabel;
	}

	public String getTokPropFeats(){
		return tokPropFeats;
	}

	public CrossValidatedSequenceDataset crossValidatedDataset(){
		return new CrossValidatedSequenceDataset(learner,sequenceDataset,splitter);
	}

	public Evaluation evaluation(){
		Evaluation e=Tester.evaluate(learner,sequenceDataset,splitter);
		return e;
	}

	static public SequenceClassifierLearner toSeqLearner(String learnerName){
		try{
			bsh.Interpreter interp=new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			return (SequenceClassifierLearner)interp.eval(learnerName);
		}catch(bsh.EvalError e){
			throw new IllegalArgumentException("error parsing learnerName '"+
					learnerName+"':\n"+e);
		}
	}

	public static void main(String[] args){
		TextLabels labels=null;
		Splitter<Example[]> splitter=new RandomSplitter<Example[]>();
		SequenceClassifierLearner learner=null;
		String inputLabel=null;
		String tokPropFeats=null;
		String toShow="eval";
		try{
			int pos=0;
			while(pos<args.length){
				String opt=args[pos++];
				if(opt.startsWith("-lab")){
					labels=FancyLoader.loadTextLabels(args[pos++]);
				}else if(opt.startsWith("-sp")){
					splitter=Expt.toSplitter(args[pos++],Example[].class);
				}else if(opt.startsWith("-lea")){
					learner=toSeqLearner(args[pos++]);
				}else if(opt.startsWith("-i")){
					inputLabel=args[pos++];
				}else if(opt.startsWith("-p")){
					tokPropFeats=args[pos++];
				}else if(opt.startsWith("-sh")){
					toShow=args[pos++];
				}else{
					usage();
				}
			}
			if(labels==null||learner==null||splitter==null||inputLabel==null)
				usage();
			SequenceAnnotatorExpt expt=
					new SequenceAnnotatorExpt(labels,splitter,learner,inputLabel,
							tokPropFeats);
			Visible v=null;
			if(toShow.startsWith("ev"))
				v=expt.evaluation();
			else if(toShow.startsWith("all"))
				v=expt.crossValidatedDataset();
			else
				usage();
			new ViewerFrame("Evaluation",v.toGUI());
		}catch(Exception e){
			e.printStackTrace();
			usage();
		}
	}

	private static void usage(){
		System.out
				.println("usage: -labels labelsKey -learn learner -input inputLabel -split splitter -show all|eval");
	}
}
