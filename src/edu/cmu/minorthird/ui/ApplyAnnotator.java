package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Apply a serialized annotator.
 *
 * @author William Cohen
 */

public class ApplyAnnotator extends UIMain
{
    private static Logger log = Logger.getLogger(ApplyAnnotator.class);

    // private data needed to test a classifier

    private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
    private CommandLineUtil.LoadAnnotatorParams load = new CommandLineUtil.LoadAnnotatorParams();
    private CommandLineUtil.AnnotatorOutputParams output = new CommandLineUtil.AnnotatorOutputParams();
    private TextLabels annLabels = null;

    // for gui
    public CommandLineUtil.SaveParams getSaveParameters() { return save; }
    public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
    public CommandLineUtil.LoadAnnotatorParams getLoadAnnotatorParameters() { return load; }
    public void setLoadAnnotatorParameters(CommandLineUtil.LoadAnnotatorParams p) { load=p; }
    //public CommandLineUtil.AnnotatorOutputParams getAnnotatorOutputParams() { return output; }
    //public void setAnnotatorOutputParams(CommandLineUtil.AnnotatorOutputParams p) { output=p; }

    public String getApplyAnnotatorHelp() {
	return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/ApplyAnnotator%20Tutorial.htm\">ApplyAnnotator Tutorial</A></html>";
    }

    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,load,output});
    }

    //
    // load and test a classifier
    // 

    public void doMain()
    {
	// check that inputs are valid
	if (load.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

	// load the classifier
	Annotator ann = null;
	try {
	    ann = (Annotator)IOUtil.loadSerialized(load.loadFrom);
	} catch (IOException ex) {
	    throw new IllegalArgumentException("can't load annotator from "+load.loadFrom+": "+ex);
	}

	// do the annotation
	annLabels = ann.annotatedCopy(base.labels);

	// echo the annotated labels 
	if (base.showResult) {
	    new ViewerFrame("Annotated Textbase",new SmartVanillaViewer(annLabels));
	}
		
	if (save.saveAs!=null) {
	    try {
		if ("minorthird".equals(output.format)) {
		    new TextLabelsLoader().saveTypesAsOps( annLabels, save.saveAs );
		} else if ("strings".equals(output.format)) {
		    new TextLabelsLoader().saveTypesAsStrings( annLabels, save.saveAs, true );					
                } else if ("xml".equals(output.format)) {
                    new TextLabelsLoader().saveDocsWithEmbeddedTypes( annLabels, save.saveAs );
		} else {
		    throw new IllegalArgumentException("illegal output format "+output.format+" allowed values are "
						       +StringUtil.toString(output.getAllowedOutputFormatValues()));
		}
	    } catch (IOException e) {
		throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
	    }
	}
    }

    public Object getMainResult() { return annLabels; }

    public static void main(String args[])
    {
	new ApplyAnnotator().callMain(args);
    }
}
