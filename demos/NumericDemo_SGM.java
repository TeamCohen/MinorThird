import java.io.File;
import java.io.IOException;

import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.classify.relational.RealRelationalDataset;
import edu.cmu.minorthird.classify.relational.StackedBatchClassifierLearner;
import edu.cmu.minorthird.classify.relational.StackedGraphicalLearner;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Example of how to load relational data and run SGM learning
 *
 * There is a sample set of data under demo/SGMsample
 *
 *
 * @author Zhenzhen Kou
 */
public class NumericDemo_SGM{

	public static void main(String[] args){
		
		// usage check
		if(args.length<3||args.length>4){
			usage();
			return;
		}

		try{
			
			// acquire the files
			String datafl=args[0];
			String linkfl=args[1];
			String relTempfl=args[2];
			int stackingDepth=1;
			if(args.length==4){
				stackingDepth=Integer.parseInt(args[3]);
			}
			RealRelationalDataset data=new RealRelationalDataset();
			DatasetLoader.loadRelFile(new File(datafl),data);
			DatasetLoader.loadLinkFile(new File(linkfl),data);
			DatasetLoader.loadRelTempFile(new File(relTempfl),data);

			//Splitter<Example> splitter=new CrossValSplitter<Example>(5);
			Splitter<Example> splitter=new RandomSplitter<Example>(0.3);

			//Construct a  learner
			StackedBatchClassifierLearner learner=new StackedGraphicalLearner(stackingDepth);

			//Test and evaluate the learner:
			Evaluation eval=Tester.evaluate(learner,data,splitter,"stacked");

			//The constructor of ViewerFrame displays the frame
			//Classes which implement Visible have a toGUI() method which produces a Viewer component.
			//The ViewerFrame - obviously - displays the Viewer component
			new ViewerFrame("Stacked Learning Demo",eval.toGUI());

		}catch(IOException e){
			e.printStackTrace(); //To change body of catch statement use Options | File Templates.
		}

	}

	private static void usage(){
		System.out.println("usage: NumericDemo_SGM [data file] [link file] [relational template] [stacking depth]");
		System.out.println("usage: NumericDemo_SGM [data file] [link file] [relational template]");
		System.out.println("both files must be in standard SVM format");
	}
}
