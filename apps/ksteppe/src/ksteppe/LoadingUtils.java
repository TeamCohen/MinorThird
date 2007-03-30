package ksteppe;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.ProgressCounter;

import java.io.IOException;
import java.io.File;

/**
 * This class...
 * @author ksteppe
 */
public class LoadingUtils
{
   TextBaseLoader loader = new TextBaseLoader();

   public BasicTextBase base = new BasicTextBase();
   public BasicTextBase testBase = new BasicTextBase();
   public BasicTextLabels labels;

   public static void main(String[] args)
   {
      LoadingUtils loader = new LoadingUtils();
      loader.makeDatasetFiles();
      loader.testDataset();
   }

   private void testDataset()
   {
      try
      {
         Dataset aDS = DatasetLoader.loadFile(new File("demos/SampleData/webmasterDataset.dat"));
         DatasetLoader.save(aDS, new File("demos/SampleData/webmasterDataset-2.dat"));
      }
      catch (Exception e)
      {
         e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
   }

   private void makeDatasetFiles()
   {
      try
      {
	  //loader.setFirstWordIsDocumentId(true);

//      labels.setTextBase(base);
         loadDataTBL(new File("demos/SampleData/webmasterCommands.txt"),
             null,
             new File("demos/SampleData/webmasterCommandTypes.labels"));

         Dataset aDataset = this.TextBaseToDataset(labels, "deleteFromDatabaseCommand", SampleFE.BAG_OF_WORDS);
         DatasetLoader.save(aDataset, new File("demos/SampleData/webmasterDataset.dat"));
      }
      catch (IOException e)
      {
         e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
   }

   /**
    * Load / initialize the two text bases
    * Assumes that the viewer Context is avaialbe to get file names from
    * @param trainFile training set
    * @param testFile test set
    * @throws java.io.IOException if files aren't found
    */
   public void loadDataTBL(File trainFile, File testFile, File labelsFile) throws IOException
   {

       //if (trainFile.isDirectory())
         loader.load(trainFile);
	 //else
         //loader.loadFile(base, trainFile);

      //standard test data file loading
      //skipped if no test file present
      if (testFile != null)
         loader.load(testFile);

      //get the text labels
      if (trainFile.isDirectory())
         labels = (BasicTextLabels)loader.getLabels();
      else
         labels = (BasicTextLabels)new TextLabelsLoader().loadOps(base, labelsFile);
   }

   public Dataset TextBaseToDataset(TextLabels labels, String targetClass, SpanFeatureExtractor fe)
   {
      TextBase base = labels.getTextBase();
      Dataset data = new BasicDataset();
      ProgressCounter progressCounter = new ProgressCounter("loading", "document", base.size());
      for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
      {
         Span s = i.nextSpan();
         //double label = labels.hasType(s, targetClass) ? +1 : -1;
         //data.add(new BinaryExample(fe.extractInstance(s), label));
         String str = labels.hasType(s, targetClass) ? "POS" : "NEG";
         ClassLabel label = new ClassLabel(str);
         data.add(new Example(fe.extractInstance(labels, s), label));
         progressCounter.progress();
      }
      progressCounter.finished();
      return data;
   }


}
