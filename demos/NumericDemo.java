
import java.io.File;
import java.io.IOException;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 *
 * @author ksteppe
 */
public class NumericDemo
{
  public static void main(String[] args)
  {
    //Usage check
    if (args.length < 2)
    {
      usage();
      return;
    }

    try
    {
      //aquire the files
      File dataFile = new File(args[0]);
      File testFile = new File(args[1]);

      //Load numeric data stored in SVM format
      Dataset trainingDataset = DatasetLoader.loadSVMStyle(dataFile);
      Dataset testDataset = DatasetLoader.loadSVMStyle(testFile);

      //Construct a new learner (could be anything - this is just as a demo)
      ClassifierLearner learner = new NaiveBayes();

      //Test and evaluate the learner:
      //  Trains on the first parameter, tests on the second
      //  There are other versions which take Splitters to
      Evaluation eval = Tester.evaluate(learner, trainingDataset, testDataset);

      //The constructor of ViewerFrame displays the frame
      //Classes which implement Visible have a toGUI() method which produces a Viewer component.
      //The ViewerFrame - obviously - displays the Viewer component
      new ViewerFrame("numeric demo", eval.toGUI());

			eval.save(new File("demo.eval.gz"));
    }
    catch (IOException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }

  }

  private static void usage()
  {
    System.out.println("usage: NumericDemo [training file] [test file]");
    System.out.println("both files must be in standard SVM format");
  }
}
