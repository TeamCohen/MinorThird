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

	// private data needed to train a extractor
    private ExtractorAnnotator ann = null;
    private ExtractionEvaluation extractionEval = null;
    // for test set splitting
    boolean doSplit = true;
    private static int num_partitions = 5;
    private MonotonicTextLabels[] subLabels;

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

    public String getTestExtractorHelp() {
	return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TestExtractor%20Tutorial.htm\">TestExtractor Tutorial</A></html>";
    }

    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,save,signal,test});
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
        TextLabels annFullLabels = ann.annotatedCopy(base.labels);

        extractionEval = new ExtractionEvaluation();

        log.info("Evaluating test partitions...");

        // split to partitions and evaluate
        if (num_partitions<2) doSplit=false;
        if (doSplit){
            log.info("Creating test partition...");
            CrossValSplitter splitter = new CrossValSplitter(num_partitions);
            splitter.split(annFullLabels.getTextBase().documentSpanIterator());

            subLabels = new MonotonicTextLabels[ splitter.getNumPartitions() ];
            for (int i=0; i<splitter.getNumPartitions(); i++) {
		try {
		    SubTextBase testBase = new SubTextBase(annFullLabels.getTextBase(), splitter.getTest(i) );
		    subLabels[i] = new MonotonicSubTextLabels(testBase, (MonotonicTextLabels)annFullLabels );
		} catch (SubTextBase.UnknownDocumentException ex) {
		    // do nothing since testLabels[i] is already set
		}
		measurePrecisionRecall("TestPartition"+(i+1),subLabels[i],false);
            }
        }

        measurePrecisionRecall("OverallTest",annFullLabels,true);

        // sample statistics
        if (doSplit) extractionEval.printAccStats();

	// echo the labels after annotation
	if (base.showResult) {
	    Viewer va = new SmartVanillaViewer();
	    va.setContent(annFullLabels);
	    new ViewerFrame("Annotated Textbase",va);
	    new ViewerFrame("Performance Results", extractionEval.toGUI());
	}


        if (save.saveAs!=null) {
	    try {
		(new TextLabelsLoader()).saveTypesAsOps(annFullLabels, save.saveAs);
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
	else{
	    // will need one span difference for each possible property value
	    Set propValues = new HashSet();
	    for (Span.Looper i=labels.getSpansWithProperty(signal.spanProp); i.hasNext(); ) {
		Span s = i.nextSpan();
		propValues.add( labels.getProperty(s,signal.spanProp) );
	    }
	    SpanDifference[] sd = new SpanDifference[propValues.size()];
	    int k = 0;
	    for (Iterator i=propValues.iterator(); i.hasNext(); k++) {
		String val = (String)i.next();
		sd[k] = new SpanDifference( propertyIterator(labels,ann.getSpanType(),val),															
					    propertyIterator(labels,signal.spanProp,val),
					    labels.getTextBase().documentSpanIterator());
		String tag1 = tag+" for "+signal.spanProp+":"+val;
		System.out.println(tag1+":");
		System.out.println(sd[k].toSummary());
		extractionEval.extend(tag1,sd[k],false);
		}
	}
    }
    private Span.Looper propertyIterator(TextLabels labels,String prop,String value)
    {
	List accum = new ArrayList();
	for (Span.Looper i=labels.getSpansWithProperty(prop); i.hasNext(); ) {
	    Span s = i.nextSpan();
	    if (value==null || value.equals(labels.getProperty(s,prop))) {
		accum.add(s );
	    }
	}
	return new BasicSpanLooper(accum);
    }

	public Object getMainResult() { return extractionEval; }

	public static void main(String args[])
	{
		new TestExtractor().callMain(args);
	}
}
