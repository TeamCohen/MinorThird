package edu.cmu.minorthird.ui;

import java.io.File;
import java.io.IOException;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * A simple UI for training and testing classifiers
 *
 * @author frank
 */

public class SimpleClassifierUI{

	public static void main(String[] args){

		if(args.length<4||args.length>5){
			usage();
			return;
		}

		try{

			// load parameters
			File dataFile=new File(args[0]);
			String learnerName=args[1];
			int cvFolds=Integer.parseInt(args[2]);
			boolean gui=Boolean.parseBoolean(args[3]);
			String saveFile=null;
			if(args.length==5){
				saveFile=args[4];
			}

			// load dataset
			Dataset data=DatasetLoader.loadFile(dataFile);

			// create learner
			ClassifierLearner learner=(ClassifierLearner)CommandLineUtil.newObjectFromBSH(learnerName,ClassifierLearner.class); 

			// evaluate
			Evaluation eval=Tester.evaluate(learner,data,new CrossValSplitter<Example>(cvFolds));

			// view results
			if(gui){
				new ViewerFrame("Evaluation Results for "+dataFile.getName(),eval.toGUI());
			}
			else{
				System.out.println("Error Rate: "+eval.errorRate());
			}

			// save evaluation results
			if(saveFile!=null){
				eval.save(new File(saveFile+".eval.gz"));
			}

		}catch(IOException e){
			e.printStackTrace();
		}

	}

	private static void usage(){
		System.out.println("Usage:");
		System.out.println(" SimpleClassifierUI TRAIN_FILE CLASSIFIER CV_FOLDS GUI_OP [EVAL_SAVE_FILE]");
		System.out.println(" GUI_OP=[true|false]");
	}
}
