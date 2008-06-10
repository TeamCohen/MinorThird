
import java.io.File;

import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.gui.TextBaseEditor;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Example of how to label data with the text package.
 * Invoke this with arguments: DATADIR LABELFILE [mixupFile].
 *
 * For a demo, add demos\sampleMixup\ to your classpath
 * and invoke it with no arguments.
 * 
 * @author wcohen
 */
public class LabelerDemo
{
  public static void main(String[] args)
  {
    //Usage check
    if (args.length != 0 && args.length<2) {
      usage();
      return;
    }

    try
    {
			String[] myArgs = args.length>0 ? args : 
												new String[]{ "sampleData/seminar-subset", "human.labels", "sampleMixup/toyName.mixup" };

      File dataDir = new File(myArgs[0]);
      File labelFile = new File(myArgs[1]);
			File mixupFile = myArgs.length>2 ? new File(myArgs[2]) : null;
			
			// load the data
//			TextBaseLoader baseLoader = new TextBaseLoader();
//			TextBase base = new BasicTextBase();
			//This detects XML markup, and makes it available with
			//getFileMarkup().  If you don't have XML markup, use
			//"baseLoader.loadDir(base,dataDir)" instead.
//			baseLoader.loadTaggedFiles(base,dataDir);
			
			TextBaseLoader loader=new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,true);
			loader.load(dataDir);
			TextBase base=loader.getLabels().getTextBase();

			// load previous markup, if it exists
			TextLabelsLoader labelLoader = new TextLabelsLoader(); 
			labelLoader.setClosurePolicy(TextLabelsLoader.CLOSE_TYPES_IN_LABELED_DOCS);
			MutableTextLabels labels = new BasicTextLabels(base);
			if (labelFile.exists()) labelLoader.importOps(labels,base,labelFile);
      //NB: markup from files is tossed out??

			// apply mixup file to get candidates, if there is one
			if (mixupFile!=null) {
				try {
					MixupProgram p = new MixupProgram(mixupFile);
					MixupInterpreter interpreter=new MixupInterpreter(p);
					interpreter.eval(labels);
				} catch (Exception e) {
					System.out.println(
						"couldn't mixup load file - are you sure "+mixupFile.getName()+" is on your classpath?");
					System.out.println(
						"error was: "+e);
					labels.declareType("candidate");
				}
			} else {
				labels.declareType("candidate");
			}
			labels.declareType("corrected");
			
			TextBaseEditor editor = TextBaseEditor.edit(labels,labelFile);
			editor.getViewer().getGuessBox().setSelectedItem("candidate");
			editor.getViewer().getTruthBox().setSelectedItem("corrected");
		} catch (Exception e) {
			e.printStackTrace();
			usage();
		}
	}

  private static void usage()
  {
    System.out.println("usage: LabelerDemo directoryOfFilesToLabel labelFile [mixupFile]");
		System.out.println("       - for a demo, if you're in the minorthird/demos directory, try");
		System.out.println("java LabelerDemo sampleData/seminar-subset my.labels sampleMixup/toyName.mixup");
  }
}
