package edu.cmu.minorthird.ui;

import jwf.NullWizardPanel;

import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.util.SimpleTextLoader;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.ui.WizardUI;
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
  private ActionListener action = null; //new ExtractionViewer();
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
    private TextLabels labels;
    private Splitter splitter;
    private ClassifierLearner cLearner;
    private AnnotatorLearner aLearner;
    private String targetClass;

    public void run()
    {
      try
      {
        log.setLevel(Level.DEBUG);
//        log.debug("viewer context: " + viewerContext.toString());

        Dataset trainDataset = null;
        File testDataFile = (File)viewerContext.get("testDataFile");

        //Feature settings
        targetClass = (String)viewerContext.get("targetClass");
        SpanFeatureExtractor fe = (SpanFeatureExtractor)viewerContext.get("fe");

        //Learner setting
        splitter = (Splitter)viewerContext.get("splitter");

        //Data should be loaded already here
        if (viewerContext.get("Loader") instanceof DatasetLoader)
          trainDataset = (Dataset)viewerContext.get("trainDataset");
        else
          labels = (TextLabels)viewerContext.get("trainLabels");

        //For a Classification test
        String task = (String)viewerContext.get(WizardUI.TASK_KEY);
        if (task.equals(WizardUI.TEXT_CAT_TASK)
              || task.equals(WizardUI.BINARY_TASK))
        {
          cLearner = (ClassifierLearner)viewerContext.get("learner");

          if (task.equals(WizardUI.TEXT_CAT_TASK))
          { //transform text into labels
            trainDataset = loadDataset(labels, targetClass, fe, "creating train dataset");
          }

          //if we don't have a splitter then the user wants a Fixed Test set
          //here we construct the FixedTestSetSplitter
          if (splitter == null)
          {
            Dataset testDataset = null;
            if (task.equals(WizardUI.TEXT_CAT_TASK))
            {
              TextLabels testLabels = (TextLabels)viewerContext.get("testLabels");
              log.debug("got test labels");
              testDataset = loadDataset(testLabels, targetClass, fe, "creating test dataset");
            }
            else //load fixed splitter for Datasets
              testDataset = (Dataset)viewerContext.get("testDataset");

            splitter = new FixedTestSetSplitter(testDataset.iterator());
          }

          runClassificationExperiment(trainDataset);

        }
        else if (viewerContext.get(WizardUI.TASK_KEY).equals(WizardUI.TEXT_EXTRACT_TASK))
        { //Extraction test
          aLearner = (AnnotatorLearner)viewerContext.get("learner");
          String outputLabel = (String)viewerContext.get("outputLabel");
          runExtractionExperiment(targetClass, outputLabel); //NB: a panel to gather output label would be good
        }

        resultButton.setEnabled(true);
      }
      catch (Throwable e)
      { log.fatal(e, e); }
    }

    private void runExtractionExperiment(String inputLabel, String outputLabel)
    {
      log.debug("extraction called");
      log.debug("learner: " + aLearner);
      final TextLabelsExperiment expt = new TextLabelsExperiment
          (labels, splitter, aLearner, inputLabel, outputLabel);
      log.debug("experiment starting");
      expt.doExperiment();
      log.debug("experiment completed");

      resultButton.removeActionListener(action);
      action = new ExtractionViewer();
      ((ExtractionViewer)action).setExperiment(expt);
      resultButton.addActionListener(action);

//      if (mostDetail.isSelected())
//        new TextLabelsLoader().saveTypesAsOps( expt.getTestLabels(), new File(saveFileName) );
    }

    private class ExtractionViewer implements ActionListener
    {
      private TextLabelsExperiment experiment;

      public void actionPerformed(ActionEvent e)
      { TextBaseViewer.view( experiment.getTestLabels() ); log.debug("displayed");}

      public void setExperiment(TextLabelsExperiment expt)
      { experiment = expt; }
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

//      log.debug("action: " + action);
      resultButton.removeActionListener(action);
      action = new ClassificationViewer();
      ((ClassificationViewer)action).setResultsViewer(resultsViewer);
      resultButton.addActionListener(action);
//      log.debug("action: " + action);

    }

    private class ClassificationViewer implements ActionListener
    {
      private Viewer resultsViewer;

      public void actionPerformed(ActionEvent e)
      { new ViewerFrame("Result", resultsViewer); }

      public void setResultsViewer(Viewer resultsViewer)
      { this.resultsViewer = resultsViewer; }
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

  }
}
