package edu.cmu.minorthird.util.gui;

import jwf.NullWizardPanel;

import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.WizardUI;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.experiments.TextLabelsExperiment;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Wizard Panel for launching an experiment
 * @author ksteppe
 */
public class ExperimentWizard extends NullWizardPanel
{
  private Map viewerContext;
//  private Evaluation evaluation = null;
  private JButton resultButton;
  private JRadioButton someDetail,moreDetail,mostDetail;

  public ExperimentWizard(Map viewerContext)
  {
    this.viewerContext = viewerContext;
    setBorder(new TitledBorder("Run Experiment"));

    JPanel detailPanel = new JPanel();
    detailPanel.setBorder(new TitledBorder("Result Format"));
    ButtonGroup group = new ButtonGroup();
    someDetail = new JRadioButton("Some detail", true);
    moreDetail = new JRadioButton("More detail", false);
    mostDetail = new JRadioButton("Most detail", false);
    JRadioButton[] buttons = new JRadioButton[]{someDetail, moreDetail, mostDetail};
    for (int i = 0; i < buttons.length; i++)
    {
      detailPanel.add(buttons[i]);
      group.add(buttons[i]);
    }

    JPanel runPanel = new JPanel();
    runPanel.setBorder(new TitledBorder("Experiment Progress"));
    JProgressBar progressBar1 = new JProgressBar();
    JProgressBar progressBar2 = new JProgressBar();
    runPanel.add(new JButton(new AbstractAction("Start Experiment")
    {
      public void actionPerformed(ActionEvent ev)
      { new MyThread().start(); }
    }));
    ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1, progressBar2});
    runPanel.add(progressBar1);
    runPanel.add(progressBar2);

    resultButton = new JButton("View Results");
    resultButton.setEnabled(false);

    runPanel.add(resultButton);
    add(runPanel);

    add(detailPanel);

  }

  private class MyThread extends Thread
  {
    private Logger log = Logger.getLogger(this.getClass());
    private MutableTextLabels labels;
    private Splitter splitter;
    private ClassifierLearner cLearner;
    private AnnotatorLearner aLearner;
    private String targetClass;

    public void run()
    {
      try
      {
        log.setLevel(Level.DEBUG);
        log.debug("viewer context: " + viewerContext.toString());

//        String testMethod = (String)viewerContext.get("testMethod");

        //standard data file loading
        //won't handle directories :(
        TextBase base = new BasicTextBase();
        TextBase testBase = new BasicTextBase();
        loadData(base, testBase);

        //Feature settings
        targetClass = (String)viewerContext.get("targetClass");
        SpanFeatureExtractor fe = (SpanFeatureExtractor)viewerContext.get("fe");

        //Learner setting
        splitter = (Splitter)viewerContext.get("splitter");

        Dataset trainDataset = loadDataset(labels, targetClass, fe, "creating train dataset");

        File testDataFile = (File)viewerContext.get("testDataFile");
        if (testDataFile != null)
        {
          Dataset testDataset = loadDataset(labels, targetClass, fe, "creating test dataset");
          //this will over-ride the user selection
          splitter = new FixedTestSetSplitter(testDataset.iterator());
        }

        if (viewerContext.get(WizardUI.TASK_KEY).equals(WizardUI.TEXT_CAT_TASK))
        {
          cLearner = (ClassifierLearner)viewerContext.get("learner");
          runClassificationExperiment(trainDataset);
        }
        else if (viewerContext.get(WizardUI.TASK_KEY).equals(WizardUI.TEXT_EXTRACT_TASK))
        {
          aLearner = (AnnotatorLearner)viewerContext.get("annotator learner");
          String outputLabel = (String)viewerContext.get("outputLabel");
          runExtractionExperiment(targetClass, outputLabel); //NB: a panel to gather output label would be good
        }

        resultButton.setEnabled(true);
      }
      catch (IOException e)
      {
        //NO SWALLOWING EXCEPTIONS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        log.error(e, e);
      }
      catch (Throwable e)
      {
        log.fatal(e, e);
      }
    }

    private void runExtractionExperiment(String inputLabel, String outputLabel)
    {
      log.debug("extraction called");
      final TextLabelsExperiment expt = new TextLabelsExperiment
          (labels, splitter, aLearner, inputLabel, outputLabel);
      log.debug("experiment starting");
      expt.doExperiment();
      log.debug("experiment completed");
//      if (mostDetail.isSelected())
//        new TextLabelsLoader().saveTypesAsOps( expt.getTestLabels(), new File(saveFileName) );
      resultButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        { TextBaseViewer.view( expt.getTestLabels() ); log.debug("displayed");}
      });

    }

    /**
     * Runs and displays a text classification experiment according to the
     * detail settings of the Wizard
     * @param trainDataset
     */
    private void runClassificationExperiment(Dataset trainDataset)
    {

//      Expt expt = new Expt(learner, trainDataset, splitter);
      final Viewer resultsViewer;

      if (someDetail.isSelected())
      {
        final Evaluation e = Tester.evaluate(cLearner, trainDataset, splitter);
        resultsViewer = e.toGUI();
      }
      else
      {
        boolean saveTrainPartitions = mostDetail.isSelected();
        final CrossValidatedDataset xValDataset = new CrossValidatedDataset(cLearner, trainDataset, splitter, saveTrainPartitions);
        resultsViewer = xValDataset.toGUI();
      }

      resultButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        { new ViewerFrame("Result", resultsViewer); }
      });
    }

    private Dataset loadDataset(TextLabels labels, String targetClass, SpanFeatureExtractor fe, String action)
    {
      TextBase base = labels.getTextBase();
      Dataset data = new BasicDataset();
      ProgressCounter progressCounter = new ProgressCounter(action, "document", base.size());
      for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
      {
        Span s = i.nextSpan();
        double label = labels.hasType(s, targetClass) ? +1 : -1;
        data.add(new BinaryExample(fe.extractInstance(s), label));
        progressCounter.progress();
      }
      progressCounter.finished();
      return data;
    }

    /**
     * Load / initialize the two text bases
     * Assumes that the viewer Context is avaialbe to get file names from
     * @param base training set
     * @param testBase test set
     * @throws IOException if files aren't found
     */
    private void loadData(TextBase base, TextBase testBase) throws IOException
    {
      TextBaseLoader loader = (TextBaseLoader)viewerContext.get("textBaseLoader");
      File trainDataFile = (File)viewerContext.get("trainDataFile");
      if (trainDataFile.isDirectory())
        loader.loadTaggedFiles(base, trainDataFile);
      else
        loader.loadFile(base, trainDataFile);

      //standard test data file loading
      //skipped if no test file present
      File testDataFile = (File)viewerContext.get("testDataFile");
      if (testDataFile != null)
        loader.loadFile(testBase, testDataFile);

      //get the text labels
      File labelsFile = (File)viewerContext.get("labelsFile");
      if (trainDataFile.isDirectory())
        labels = loader.getLabels();
      else
        labels = new TextLabelsLoader().loadOps(base, labelsFile);


    }
  }
}
