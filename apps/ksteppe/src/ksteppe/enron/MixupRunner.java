package ksteppe.enron;

import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.mixup.MixupProgram;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * This class...
 * @author ksteppe
 */
public class MixupRunner
{
  public static void main(String[] args) throws Exception, IOException, ParseException
  {
    MixupProgram program = new MixupProgram(new File(args[0]));
    TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,
                                                TextBaseLoader.FILE_NAME, false);
    MonotonicTextLabels labels = (MonotonicTextLabels)loader.load(new File(args[1]));

//    MonotonicTextLabels labels = (MonotonicTextLabels)SimpleTextLoader.load(args[1], false);

    program.eval(labels, labels.getTextBase());
    File outFile = new File(args[2]);
    new TextLabelsLoader().saveTypesAsOps(labels, outFile);

  }
}
