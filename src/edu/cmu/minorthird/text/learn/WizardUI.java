package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.text.gui.CollinsSequenceAnnotatorLearner;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.Loader;
import jwf.NullWizardPanel;
import jwf.WizardPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Top-level GUI interface for the text annotation/learning stuff.
 *
 * @author William Cohen
 */

public class WizardUI
{

	/** A special type selector.
	 */
	private static class MyTypeSelector extends TypeSelector
	{
		private static Class[] myClasses = {
			// loaders
			TextBaseLoader.class,
      DatasetLoader.class,
			// learners
			NaiveBayes.class,
			BBMira.class,
			VotedPerceptron.class,
			PoissonLearner.class,
			LogisticRegressor.class,
			AdaBoost.class,
			DecisionTreeLearner.class,
			SVMLearner.class,
			KnnLearner.class,
			BatchVersion.class,
			StackedLearner.class,
			// splitters
			RandomSplitter.class,
			CrossValSplitter.class,
			// feature extractors
			SampleFE.BagOfWordsFE.class,
			SampleFE.BagOfLowerCaseWordsFE.class,
      //AnnotatorLearners
      BatchFilteredFinderLearner.class,
      CMMAnnotatorLearner.class,
      CollinsSequenceAnnotatorLearner.class
//      BatchInsideOutsideLearner.class,
//      BatchStartEndLengthLearner.class,
		};
		public MyTypeSelector(Class rootClass) { super(myClasses,rootClass); }
	}

  public static final String TEXT_CAT_TASK = "text categorization task";
  public static final String TEXT_EXTRACT_TASK = "text extraction task";
  public static final String TASK_KEY = "task";
  private static final String FIXED_TEST = "fixed test";
  private static final String CROSS_VALIDATION = "cross validation";

  private static PickLoader pickLoader;
	//
	// define the steps of the wizard
	//

	/** pick the task to perform. */
	private static class PickTask extends RadioWizard
	{
    public PickTask(Map viewerContext)
		{
			super(TASK_KEY, viewerContext, "Select Task","Please select a task:");
      pickLoader = new PickLoader(viewerContext);

      WizardPanel classifyLearner = new PickLearner(viewerContext).getWizardPanel();
      WizardPanel annotatorLearner = new PickAnnotatorLearner(viewerContext).getWizardPanel();

      addButton("Perform a text categorization experiment", TEXT_CAT_TASK, classifyLearner, true);
			addButton("Perform an extraction experiment", TEXT_EXTRACT_TASK, annotatorLearner, false);
		}
	}


  /** pick an annotator algorithm. */
  private static class PickAnnotatorLearner extends SimpleViewerWizard
  {
    public PickAnnotatorLearner(Map viewerContext)
    {
      super("annotator learner",viewerContext,
            "Configure Annotator Learner","Choose a annotation learner:",
            pickLoader.getWizardPanel());
      MyTypeSelector selector = new MyTypeSelector(AnnotatorLearner.class);
      selector.setContent(SampleLearners.CMM);
      addViewer(selector);
    }
  }

	/** pick a learning algorithm. */
	private static class PickLearner extends SimpleViewerWizard
	{
		public PickLearner(Map viewerContext)
		{
			super("learner",viewerContext,
						"Configure Learner","Choose a classification learning algorithm:",
						pickLoader.getWizardPanel());
			MyTypeSelector selector = new MyTypeSelector(ClassifierLearner.class);
			selector.setContent(new AdaBoost());
			addViewer(selector);
		}
	}


	/** pick a TextBaseLoader. */
	private static class PickLoader extends SimpleViewerWizard
	{
		public PickLoader(Map viewerContext)
		{
			super("Loader",viewerContext,
						"Configure Loader","Define the data format being used:",
						new PickTrainDataFile(viewerContext));
			MyTypeSelector selector = new MyTypeSelector(Loader.class);
			selector.setContent(new TextBaseLoader());
			addViewer(selector);
		}
	}

	/** Pick a training-data file. */
	private static class PickTrainDataFile extends FileChooserWizard
	{
		public PickTrainDataFile(Map viewerContext)
		{
			super("trainDataFile",viewerContext,
						true,"Select Training Data","Where is the training data?");
      this.nextWizardPanel = new PickLabelFile(viewerContext);
		}

    public boolean validateNext(java.util.List list)
    {
      if (((File)viewerContext.get("trainDataFile")).isDirectory())
      {
        if (viewerContext.get("Loader") instanceof DatasetLoader)
        {
          list.add("can not load directories with DatasetLoader");
          return false;
        }
        else
          return true;
      }
      else
        return super.validateNext(list);
    }
	}

	/** Pick a label file. */
	private static class PickLabelFile extends FileChooserWizard
	{
    private WizardPanel fePanel = new PickFeatureExtractor(viewerContext).getWizardPanel();

    public PickLabelFile(Map viewerContext)
		{
			super("labelsFile",viewerContext,
						true,"Select Annotation File","Where are annotations stored?");
      this.nextWizardPanel = fePanel;
//      this.nextWizardPanel = new PickFeatureExtractor(viewerContext).getWizardPanel();
		}

    public boolean validateNext(java.util.List list)
    {
//ks nb should we really enforce choosing a label file?  probably not
      return true;

      //don't need to check for loader type, since that is checked one page earlier.
//      if (((File)viewerContext.get("trainDataFile")).isDirectory())
//        return true;
//      else if (viewerContext.get("Loader") instanceof DatasetLoader)
//        return true;
//      else
//        return super.validateNext(list);
    }

    public WizardPanel next()
    {
      if (viewerContext.get("Loader") instanceof DatasetLoader)
        return new PickTestMethod(viewerContext);
      else
        return fePanel;
    }

	}

	/** pick a feature extractor. */
	private static class PickFeatureExtractor extends SimpleViewerWizard
	{
		public PickFeatureExtractor(Map viewerContext)
		{
			super("fe",viewerContext,
						"Configure Feature Extractor","Choose a featore extraction scheme:",
						new PickTestMethod(viewerContext));
			MyTypeSelector selector = new MyTypeSelector(SpanFeatureExtractor.class);
			selector.setContent(new SampleFE.BagOfLowerCaseWordsFE());
			addViewer(selector);
		}
	}

	/** pick a test scheme algorithm. */
	private static class PickTestMethod extends RadioWizard
	{
    public PickTestMethod(Map viewerContext)
		{
			super("testMethod",viewerContext,"Test method","Choose a testing scheme:");
			addButton( "Cross validation or random split", CROSS_VALIDATION, new PickSplitter(viewerContext).getWizardPanel(), true );
			// adding this button causes a stack overflow, why?
			addButton( "Fixed test set", FIXED_TEST, new PickTestDataFile(viewerContext), false );
		}
	}

	/** Pick a training-data file. */
	private static class PickTestDataFile extends FileChooserWizard
	{

		public PickTestDataFile(Map viewerContext)
		{
			super("testDataFile",viewerContext,
						true,"Select Testing Data","Where is the test data?");
      this.nextWizardPanel = new PickTargetClass(viewerContext);
		}
	}


	/** pick a cross-validation scheme. */
	private static class PickSplitter extends SimpleViewerWizard
	{
    private PickTargetClass classification = new PickTargetClass(viewerContext);
    private PickLabelTargets extraction = new PickLabelTargets(viewerContext);

		public PickSplitter(Map viewerContext)
		{
			super("splitter",viewerContext,
						"Cross Validation","Choose a cross-validation scheme:",
						null);
      this.nextWizardPanel = classification;
			MyTypeSelector selector = new MyTypeSelector(Splitter.class);
			selector.setContent(new CrossValSplitter(5));
			addViewer(selector);
		}

    public WizardPanel buildWizardPanel()
    { return new MyPanel(); }

    protected class MyPanel extends SimpleViewerPanel
    {
      public WizardPanel next()
      {
        if (viewerContext.get(TASK_KEY).equals(TEXT_EXTRACT_TASK))
          return extraction;
        else
          return classification;
      }
    }
	}

	/** pick a cross-validation scheme. */
	private static class PickTargetClass extends NullWizardPanel
	{
		protected Map viewerContext;
		private JTextField textField;

    public PickTargetClass(Map viewerContext)
		{
			this.viewerContext = viewerContext;
			setBorder(new TitledBorder("Target Class"));
			add(new JLabel("What class do you want to try to learn?"));
			textField = new JTextField(10);
			add(textField);

		}
		public boolean validateNext(java.util.List list) {
			list.add("You need to pick a target class.");
			return textField.getText().trim().length() > 0;
		}
		public boolean hasNext() { return true; }
		public WizardPanel next()
		{
			viewerContext.put("targetClass",textField.getText().trim());
			return new ExperimentWizard(viewerContext);
		}
	}

  private static class PickLabelTargets extends PickTargetClass
  {
    private JTextField outputField = new JTextField(10);

    public PickLabelTargets(Map viewerContext)
    {
      super(viewerContext);
      add(new JLabel("What label do you want to output?"));
      add(outputField);
    }

    public WizardPanel next()
    {
      viewerContext.put("outputLabel", outputField.getText().trim());
      return super.next();
    }
  }

	//
	// main program to kick off the wizard
	//

	/** The entry point to the wizard. */
	public static void main(String args[])
	{
		Map viewerContext = new HashMap();
		new edu.cmu.minorthird.util.gui.WizardFrame("Text Learning Wizard",new PickTask(viewerContext));
	}
}
