package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.GenericCollinsLearner;
import edu.cmu.minorthird.classify.algorithms.knn.KnnLearner;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.svm.SVMLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.CollinsSequenceAnnotatorLearner;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.util.SimpleTextLoader;
import edu.cmu.minorthird.util.gui.*;
import jwf.WizardPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;


/**
 * Top-level GUI interface for the text annotation/learning stuff.
 *
 * @author William Cohen
 */

public class WizardUI
{
  private static Logger log = Logger.getLogger(WizardUI.class);

  /** A special type selector.
	 */
	private static class MyTypeSelector extends TypeSelector
	{
		private static Class[] myClasses = {
			// loaders
//			TextBaseLoader.class,
      SimpleTextLoader.class,
      DatasetLoader.class,
//      RepositoryLoader.class, <-- Added by PickLoader
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
      // anontotor extractors
      SampleFE.ExtractionFE.class,
      //AnnotatorLearners
      BatchFilteredFinderLearner.class,
      CMMAnnotatorLearner.class,
      CollinsSequenceAnnotatorLearner.class,
//      SequenceAnnotatorLearner.class
//      BatchInsideOutsideLearner.class,
//      BatchStartEndLengthLearner.class,
		};
		public MyTypeSelector(Class rootClass) { super(myClasses,rootClass); }
	}

  public static final String BINARY_TASK = "binary categorization task";
  public static final String TEXT_CAT_TASK = "text categorization task";
  public static final String TEXT_EXTRACT_TASK = "text extraction task";
  public static final String TASK_KEY = "task";
  private static final String FIXED_TEST = "fixed test";
  private static final String CROSS_VALIDATION = "cross validation";

//  private static PickLoader pickLoader;
  private static ExperimentWizard experimentWizard;
//  private Map viewerContext; //aka all the parameters so far

	//
	// define the steps of the wizard
	//

  /**
   * controls the flow of the wizard using the context to determine what
   * should be next
   * @return next Panel to display
   */
  public WizardPanel next(Map newData)
  {
    return null;
  }

  //-------------------PAGE 1: Choose a task -------------------------------//
	/** pick the task to perform. */
	private static class PickTask extends RadioWizard
	{
    public PickTask(Map viewerContext)
		{
			super(TASK_KEY, viewerContext, "Select Task","Please select a task:");
//      pickLoader = new PickLoader(viewerContext);
      experimentWizard = new ExperimentWizard(viewerContext);

      WizardPanel learner = new PickLearnerB(viewerContext).getWizardPanel();
//      WizardPanel annotatorLearner = new PickAnnotatorLearner(viewerContext).getWizardPanel();

      addButton("Perform a text categorization experiment", TEXT_CAT_TASK, learner, true);
			addButton("Perform an extraction experiment", TEXT_EXTRACT_TASK, learner, false);
	    addButton("Perform classification experiment on a dataset", BINARY_TASK, learner, false);
	  }
	}
  //-------------------PAGE 1: Choose a task -------------------------------//


  //-------------------PAGE 2: Choose a learner -------------------------------//
  private static class PickLearnerB extends SimpleViewerWizard
  {
    public PickLearnerB(Map viewerContext)
    {
      super("learner",viewerContext,
            "Configure Learner","Choose a classification learning algorithm:",
            new PickLoader(viewerContext).getWizardPanel());
          //            pickLoader.getWizardPanel());
//      PickLoader pickLoader = new PickLoader(viewerContext);
//      this.nextWizardPanel = pickLoader.getWizardPanel();
//      log.debug("next = " + pickLoader.getWizardPanel());
    }

    public void init()
    {
      //get context info
      MyTypeSelector selector;
      if (viewerContext.get(TASK_KEY) != null)
      {
        if (viewerContext.get(WizardUI.TASK_KEY).equals(WizardUI.TEXT_CAT_TASK))
        {
          selector = new MyTypeSelector(ClassifierLearner.class);
          selector.setContent(new NaiveBayes());
          addViewer(selector);

          log.debug("cat");
          MyTypeSelector feSelector = new MyTypeSelector(SpanFeatureExtractor.class);
          feSelector.setContent(new SampleFE.BagOfLowerCaseWordsFE());
          addViewer(feSelector, "fe");

        }
        else if (viewerContext.get(WizardUI.TASK_KEY).equals(WizardUI.TEXT_EXTRACT_TASK))
        {
          selector = new MyTypeSelector(AnnotatorLearner.class);
          selector.setContent(SampleLearners.CMM);
          addViewer(selector);

          log.debug("extract");
          MyTypeSelector feSelector = new MyTypeSelector(SampleFE.ExtractionFE.class);
          feSelector.setContent(new SampleFE.ExtractionFE());
          addViewer(feSelector, "fe");
        }
        else //binary numeric classifier
        {
          selector = new MyTypeSelector(BinaryClassifierLearner.class);
          selector.setContent(new NaiveBayes());
          addViewer(selector);
        }
      }
    }
  }
  //-------------------PAGE 2: Choose a learner -------------------------------//


  //-------------------PAGE 3: Choose a Loader -------------------------------//

  /** pick a TextBaseLoader. */
  private static class PickLoader extends SimpleViewerWizard
	{
		public PickLoader(Map viewerContext)
		{
			super("Loader",viewerContext,
						"Configure Loader","Define the data format being used:",
						new PickTrainData(viewerContext).getWizardPanel());

      log.debug("Loader: " + getWizardPanel());
		}

    /**
     * initialize the view -
     * places either datasetloader or simpletextloader depending
     * on task type into the chooser.
     */
    public void init()
    {
      MyTypeSelector selector = null;
      if (viewerContext.get(TASK_KEY) != null)
      {
        if (viewerContext.get(TASK_KEY).equals(BINARY_TASK))
        {
          selector = new MyTypeSelector(DatasetLoader.class);
          selector.setContent(new DatasetLoader());
        }
        else
        {
          selector = new MyTypeSelector(SimpleTextLoader.class);
          SimpleTextLoader loader = new SimpleTextLoader();
          if (viewerContext.get(TASK_KEY).equals(TEXT_EXTRACT_TASK))
          {
            loader.setLabelFile(false);
            selector.addClass(RepositoryLoader.class);
          }
          selector.setContent(loader);
        }
      }

      if (selector != null)
        addViewer(selector);

    }
	}
  //-------------------PAGE 3: Choose a Loader -------------------------------//

  //-------------------PAGE 4: Choose Training data -------------------------------//

  //Use the PickTrain Data class
  private static class PickTrainData extends PickData
  {
    public PickTrainData(Map viewerContext)
    {
      super(null, viewerContext, "Select Training Data",
          "Where is the training data", new PickTargetClass(viewerContext).getWizardPanel(),
          "trainData", "trainLabels");

    }
  }

	/** Pick a training-data file. */
  private static class PickData extends SimpleViewerWizard implements ActionListener, ItemListener
  {
    private boolean loaded = false;
    private String DATA_KEY;
    private String LABEL_KEY;
    private FileChooserViewer dataChooser;
    private FileChooserViewer labelChooser;
    private JComboBox bshCombo = null;

    public void actionPerformed(ActionEvent e)
    { loaded = false; }

    public PickData(String key, Map viewerContext, String title, String prompt,
                    SimpleViewerPanel next, String dataKey, String labelKey)
    {
      super(key, viewerContext, title,
          prompt, next);
      DATA_KEY = dataKey;
      LABEL_KEY = labelKey;

    }

    public SimpleViewerWizard.SimpleViewerPanel buildWizardPanel()
    { return new TrainDataPanel(this); }

    /**
     * Adds a file chooser for the training data
     * If using a TBL then another file chooser is added for the labels (users of TBL should "know what they are doing")
     * If using a SimpleTextLoader a button is added to look at the text base
     */
    public void init()
    {
      if (dataChooser == null)
      {
        dataChooser = new FileChooserViewer(this);
        dataChooser.addActionListener(this);

        labelChooser = new FileChooserViewer(this);
        labelChooser.addActionListener(this);
      }

      if (viewerContext.get("Loader") != null)
      {
        //Build the combo box of repository entries if a RepLoader
        if (viewerContext.get("Loader") instanceof RepositoryLoader)
        {
          RepositoryLoader loader = (RepositoryLoader)viewerContext.get("Loader");
          String[] beanShells = loader.getFileList();
          log.warn(beanShells);
          bshCombo = new JComboBox(beanShells);
          bshCombo.addItemListener(this);
          getWizardPanel().add(bshCombo);
        }
        else //add FileChooser for a SimpleTextLoader or TBL
          addFileChooser(dataChooser, DATA_KEY + "File");

        //set style based on loader?
        if (viewerContext.get("Loader") instanceof TextBaseLoader)
        {
          getWizardPanel().add(new JLabel("Choose annotation file if needed"));
          addFileChooser(labelChooser, LABEL_KEY);
        }

        //add a viewing button
        JButton viewButton = getViewDataButton();

        getWizardPanel().add(viewButton);
      }
    }

    public void itemStateChanged(ItemEvent e)
    { this.loaded = false; }


    /**
     * constructs a "view data" button
     * The button loads data if not already loaded.
     * If a label-key exists it displays labels and data via TextBaseViewer
     * other wise it displays nothing.
     * Note: this means that Datasets aren't displayed.
     */
    private JButton getViewDataButton()
    {
      JButton viewButton = new JButton("view data");
      viewButton.addActionListener(new ActionListener()
      { //displays the data and labels, loading first if needed
        public void actionPerformed(ActionEvent e)
        {
          try {
            if (!loaded)
              loadData();

            if (viewerContext.get(LABEL_KEY) != null)
            {
              TextLabels labels = (TextLabels)viewerContext.get(LABEL_KEY);
              TextBaseViewer.view(labels);
            }
            else if (viewerContext.get(DATA_KEY + "set") != null)
            {
              Dataset trainDataset = (Dataset)viewerContext.get(DATA_KEY + "set");
              ViewerFrame f = new ViewerFrame("Dataset", trainDataset.toGUI());
            }

          }
          catch (Exception ex)
          {
            JOptionPane.showMessageDialog(null, "Can not load data", "Load Failed", JOptionPane.ERROR_MESSAGE);
            log.error(ex, ex);
          }
        }
      });
      return viewButton;
    }

    /**
     * put the file chooser on the panel, selecting the current file
     * if it's there.
     */
    private void addFileChooser(FileChooserViewer chooser, String key)
    {
      addViewer(chooser, key);

      Object o = viewerContext.get(key);
      if (o != null && o instanceof File)
        chooser.setFile((File)viewerContext.get(key));

      //display file chooser
    }

    /**
     * Loads data into "trainLabels" on the context
     * exception is thrown if the data can't be read
     * sets this.loaded = true
     */
    protected void loadData() throws Exception, ParseException, IOException
    {
      File file = (File)viewerContext.get(DATA_KEY + "File");
      Object loadObj = viewerContext.get("Loader");
      TextLabels labels = null;
      //load the data
      if (loadObj instanceof DatasetLoader)
      {
        //load dataset
        log.debug("load via dataset");
        Dataset trainDataset = DatasetLoader.loadFile(file);
        viewerContext.put(DATA_KEY + "set", trainDataset);
      }
      else
      {
        labels = loadText(loadObj, file);
        log.debug("loaded: " + loadObj + "/" +file);
      }

      if (labels != null)
        viewerContext.put(LABEL_KEY, labels);

      this.loaded = true;
    }

    /**
     * Load text using the given loader object and the file location for data
     */
    private TextLabels loadText(Object loadObj, File file) throws Exception
    {
      TextLabels labels = null;

      if (loadObj instanceof SimpleTextLoader)
      {
        SimpleTextLoader loader = (SimpleTextLoader)loadObj;
        if (file != null)
          labels = loader.load(file);
        else
          throw new Exception("can't load null file!");
      }
      else if (loadObj instanceof RepositoryLoader)
      {
        RepositoryLoader loader = (RepositoryLoader)loadObj;
        if (bshCombo == null)
          log.fatal("Bean Shell combo not yet initialized!");
        labels = loader.load((String)bshCombo.getSelectedItem());
      }
      else  //TBL loader
      {
        TextBaseLoader loader = (TextBaseLoader)loadObj;
        TextBase base = loader.load(file);
        if (loader.isLabelsInFile())
          labels = loader.getLabels();
        else
        {
          File labelFile = new File("labelsFile"); //ks42 todo broken - TBL not implemented yet
          labels = new TextLabelsLoader().loadOps(base, labelFile);
        }

      }
      return labels;
    }


    /**
     * Panel to allows validation that data has been loaded.
     */
    public class TrainDataPanel extends SimpleViewerWizard.SimpleViewerPanel
    {
      public TrainDataPanel(SimpleViewerWizard parent)
      { super(parent); }

      /**
       * Ensures that data is loaded properly before moving on
       */
      public boolean validateNext(List list)
      {
        try
        {
          log.debug("loaded? " + loaded);
          log.debug(viewerContext.get("trainDataFile"));

          if (!loaded)
            loadData();
          return true;
        }
        catch (Exception e)
        {
          list.add("cannot load your data: " + e.getMessage());
          return false;
        }
      }

    }
  }

  //-------------------PAGE 4: Choose Training data -------------------------------//


  //-------------------PAGE 5: Choose Label to learn ------------------------------//
  /** pick a cross-validation scheme. */
  private static class PickTargetClass extends SimpleViewerWizard
  {
//    private JTextField textField;

    public PickTargetClass(Map viewerContext)
    { super(null, viewerContext, "Pick target", "What do you want to try to learn?", null); }

    public SimpleViewerWizard.SimpleViewerPanel buildWizardPanel()
    { return new TargetPanel(this); }

    private class TargetPanel extends SimpleViewerWizard.SimpleViewerPanel
    {
      private JComboBox combo;
      private WizardPanel next;
      private JTextField outputField = null;

      public TargetPanel(SimpleViewerWizard parent)
      {
        super(parent);
        next = new PickTestMethod(viewerContext);
      }

      public void init()
      {
        if (viewerContext.get(TASK_KEY) != null)
        {
          removeAll();
          super.init();

          outputField = null;
          combo = new JComboBox();
          //loop through the current labels
          TextLabels labels = (TextLabels)context.get("trainLabels");
//          log.debug("labels found: " + labels);

          if (labels != null)
          {
            Set types = labels.getTypes();
            for (Iterator it = types.iterator(); it.hasNext();)
            {
              String type = (String)it.next();
              combo.addItem(type);
            }
          }
          else //dataset
          {
            Dataset trainDataset = (Dataset)viewerContext.get("trainDataset");
            //ok, realisticly we've requested only binary classification through the wizard
            //however I can still get all the classes and list them
            String[] classes = trainDataset.getSchema().validClassNames();
            combo = new JComboBox(classes);
          }
          add(combo);

          if (context.get(TASK_KEY) != null && context.get(TASK_KEY).equals(TEXT_EXTRACT_TASK))
          {
            //need to get output label
            outputField = new JTextField(20);
            add(new JLabel("What label do you want to output?"));
            add(outputField);
          }
        }
      }

      public boolean validateNext(java.util.List list) {
        list.add("You need to pick an output label.");
        if (outputField != null)
          return outputField.getText().trim().length() > 0;
        else
          return true;
      }

      public boolean hasNext() { return true; }
      public WizardPanel next()
      {
        context.put("targetClass", combo.getSelectedItem()); // textField.getText().trim());
        if (outputField != null)
          context.put("outputLabel", outputField.getText().trim());

        ((PickTestMethod)next).init();
        return next;
      }
    }
  }

  //-------------------PAGE 5: Choose Label to learn ------------------------------//


  //-------------------PAGE 6: Choose Test Method ------------------------------//
	/** pick a test scheme algorithm. */
	private static class PickTestMethod extends RadioWizard
	{
    private boolean initialized = false;

    public PickTestMethod(Map viewerContext)
		{ super("testMethod",viewerContext,"Test method","Choose a testing scheme:"); }

    public void init()
    {
      if (!initialized)
      {
        addButton( "Cross validation or random split", CROSS_VALIDATION, new PickSplitter(viewerContext).getWizardPanel(), true );
        addButton( "Fixed test set", FIXED_TEST, new PickTestDataFile(viewerContext).getWizardPanel(), false );
        initialized = true;
      }

      if (viewerContext.get(TASK_KEY).equals(TEXT_EXTRACT_TASK))
        disableButton(FIXED_TEST);
      else
        enableButton(FIXED_TEST);
    }

  }


  /** pick a cross-validation scheme. */
  private static class PickSplitter extends SimpleViewerWizard
  {
//    private PickTargetClass classification = new PickTargetClass(viewerContext);
//    private PickLabelTargets extraction = new PickLabelTargets(viewerContext);

    public PickSplitter(Map viewerContext)
    {
      super("splitter",viewerContext,
            "Cross Validation","Choose a cross-validation scheme:",
            null);
    }

    public void init()
    {
      MyTypeSelector selector = new MyTypeSelector(Splitter.class);
      selector.setContent(new CrossValSplitter(5));
      addViewer(selector);
    }

    public SimpleViewerWizard.SimpleViewerPanel buildWizardPanel()
    { return new SplitterPanel(this); }

    private class SplitterPanel extends SimpleViewerPanel
    {
      public SplitterPanel(SimpleViewerWizard parent)
      { super(parent); }

      public WizardPanel next()
      {
        return experimentWizard;
      }
    }
  }

  private static class PickTestDataFile extends PickData
  {
    public PickTestDataFile(Map viewerContext)
    {
      super("testDataFile", viewerContext, "Select Testing Data",
          "Where is the test data", null,
          "testData", "testLabels");
    }

    public SimpleViewerWizard.SimpleViewerPanel buildWizardPanel()
    { return new TestDataPanel(this); }

    /** Pick a training-data file. */
    private class TestDataPanel extends PickData.TrainDataPanel
    {
      public TestDataPanel(SimpleViewerWizard parent)
      { super(parent); }

      public boolean validateNext(List list)
      {
        return super.validateNext(list);

//        if (super.validateNext(list))
//        {

  //      for text stuff will need to get the labels and ???
  //      if (testDataFile != null)
  //      {
  //        Dataset testDataset = DatasetLoader.loadFile(testDataFile);
  //        splitter = new FixedTestSetSplitter(testDataset.iterator());
  //      }

          //set the data and splitter
          //create splitter from the LABEL_KEY
          //not sure what to do for extraction

//          return true;
//        }
//        else
//          return false;
      }

      public WizardPanel next()
      {
        viewerContext.put("splitter", null);
        return experimentWizard;
      }
    }

  }

  //-------------------PAGE 6: Choose Test Method ------------------------------//


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
