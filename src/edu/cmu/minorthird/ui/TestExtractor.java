package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;

import sun.java2d.pipe.SpanIterator;


/**
 * Do a train/test experiment for named-entity extractors.
 *
 * @author William Cohen
 */

public class TestExtractor extends UIMain
{
  private static Logger log = Logger.getLogger(TestExtractor.class);

    //private TextLabels annLabels;   //added by Einat

	// private data needed to train a extractor

    private MonotonicTextLabels[] subLabels;   //added - Einat
    private MonotonicTextLabels fullTestLabels;  //added - Einat
    private ExtractorAnnotator ann = null;
    private ExtractionEvaluation extractionEval = null;
    private static int testSplits = 10;

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ExtractionSignalParams signal = new CommandLineUtil.ExtractionSignalParams(base);
	private CommandLineUtil.TestExtractorParams test = new CommandLineUtil.TestExtractorParams();


	// for gui
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
	public CommandLineUtil.ExtractionSignalParams getSignalParameters() { return signal; } 
	public void setSignalParameters(CommandLineUtil.ExtractionSignalParams p) { signal=p; } 
	public CommandLineUtil.TestExtractorParams getAdditionalParameters() { return test; } 
	public void setAdditionalParameters(CommandLineUtil.TestExtractorParams p) { test=p; } 

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,test});
	}

	//
	// do the experiment
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (test.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

		// load the annotator
		try {
			ann = (ExtractorAnnotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom+": "+ex);
		}

		if (test.showExtractor) {
			Viewer vx = new SmartVanillaViewer();
			vx.setContent(ann);
			new ViewerFrame("Annotator",vx);
		}
		TextLabels annLabels = ann.annotatedCopy(base.labels);

        // added by Einat

        CrossValSplitter splitter = new CrossValSplitter(testSplits);
        splitter.split(annLabels.getTextBase().documentSpanIterator());

		Set allTestDocuments = new TreeSet();
		for (int i=0; i<splitter.getNumPartitions(); i++) {
			for (Iterator j=splitter.getTest(i); j.hasNext(); ) {
				//System.out.println("adding test case to allTestDocuments");
				allTestDocuments.add( j.next() );
			}
		}

        try {
			// for most splitters, the test set will be a subset of the original TextBase
			SubTextBase fullTestBase = new SubTextBase(annLabels.getTextBase(), allTestDocuments.iterator() );
			fullTestLabels = new NestedTextLabels( new SubTextLabels( fullTestBase, annLabels ) );
			subLabels = new MonotonicTextLabels[ splitter.getNumPartitions() ];
		} catch (SubTextBase.UnknownDocumentException ex) {
			if (annLabels==null) throw new IllegalArgumentException("exception: "+ex);
		}

        log.info("Creating test partition...");
			for (int i=0; i<splitter.getNumPartitions(); i++)
            {
               try {
				SubTextBase testBase = new SubTextBase(annLabels.getTextBase(), splitter.getTest(i) );
				subLabels[i] = new MonotonicSubTextLabels(testBase, fullTestLabels );
			    } catch (SubTextBase.UnknownDocumentException ex) {
				// do nothing since testLabels[i] is already set
			    }
            }

        extractionEval = new ExtractionEvaluation();

        System.out.println("Compare "+ann.getSpanType()+" to "+signal.spanType+":");
        log.info("Evaluating test partitions...");

        for (int i=0; i<10; i++){
            measurePrecisionRecall("TestPartition"+(i+1),subLabels[i],false);
        }
        measurePrecisionRecall("OverallTest",annLabels,true);

        extractionEval.printAccStats();

		// echo the labels after annotation
		if (base.showResult) {
			Viewer va = new SmartVanillaViewer();
			va.setContent(annLabels);
			new ViewerFrame("Annotated Textbase",va);
			new ViewerFrame("Performance Results", extractionEval.toGUI());
		}
        /**
		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)evaluation,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}**/

        if (save.saveAs!=null) {
			try {
				(new TextLabelsLoader()).saveTypesAsOps(annLabels, save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

    private void measurePrecisionRecall(String tag,TextLabels labels,boolean isOverallMeasure)
    {
       if (signal.spanType!=null) {
			// only need one span difference here
			SpanDifference sd =
				new SpanDifference(
					labels.instanceIterator(ann.getSpanType()),
					labels.instanceIterator(signal.spanType),
					labels.closureIterator(signal.spanType) );
			System.out.println("\n" + tag+":");
			System.out.println(sd.toSummary());
			extractionEval.extend(tag,sd,isOverallMeasure);
		}
       else
       {
        // not handled
       }
    }

	public Object getMainResult() { return extractionEval; }

	public static void main(String args[])
	{
		new TestExtractor().callMain(args);
	}
}
