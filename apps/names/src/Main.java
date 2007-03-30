import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;

import java.util.*;
import java.io.*;

public class Main 
{
	static private int featureWindow=5,classWindow=3,epochs=6;
	static private Map propertyMap = new HashMap();
	static private String classToLearn = "name";
	static private Extraction2TaggingReduction reduction = new InsideOutsideReduction();
	//static private Extraction2TaggingReduction reduction = new BeginContinueEndUniqueReduction();

	static private String mixupFileStem="nameFeatures_v2";

	public static void main(String[] args)
	{
		int pos=0; 
		String 
			command=null,
			learnerName="new CollinsPerceptronLearner("+classWindow+","+epochs+")",
			labelsKey=null,
			saveFileName=null,
			splitterName="r50",
			show=null;

		while (pos<args.length) {
			String opt = args[pos++];
			if (opt.equals("-do")) command = args[pos++];
			else if (opt.startsWith("-lea")) learnerName = args[pos++];
			else if (opt.startsWith("-labels")) labelsKey = args[pos++];
			else if (opt.startsWith("-save")) saveFileName = args[pos++];
			else if (opt.startsWith("-split")) splitterName = args[pos++];
			else if (opt.startsWith("-mix")) mixupFileStem = args[pos++];
			else if (opt.startsWith("-show")) show = args[pos++];
			else if (opt.startsWith("-class")) classToLearn = args[pos++];
			else throw new IllegalArgumentException("illegal option "+opt);
		}

		propertyMap.put("featureWindow",Integer.toString(featureWindow));
		propertyMap.put("classWindow",Integer.toString(classWindow));
		propertyMap.put("epochs",Integer.toString(epochs));
		propertyMap.put("learner",learnerName);
		propertyMap.put("labels",labelsKey);
		propertyMap.put("splitter",splitterName);
		propertyMap.put("mixup",mixupFileStem);

		//System.out.println("saved annotator file: '"+saveFileName+"'");
		System.out.println("class: '"+classToLearn+"'");

		try {

			System.out.println("loading labels "+labelsKey);
			if (labelsKey==null) throw new IllegalArgumentException("-labels LABELS must be specified");
			MutableTextLabels labels = (MutableTextLabels)FancyLoader.loadTextLabels(labelsKey);
			System.out.println("loading "+mixupFileStem+".mixup...");
			MixupProgram program = new MixupProgram(new File(mixupFileStem+".mixup"));
                        MixupInterpreter interp = new MixupInterpreter(program);
			interp.eval(labels);

			if ("train".equals(command)) {
				SequenceClassifierLearner learner = SequenceAnnotatorExpt.toSeqLearner(learnerName);
				if (saveFileName==null) saveFileName = "out.ann";
				buildAnnotator(labels, learner, saveFileName);
			} else if ("test".equals(command)) {
				if (saveFileName==null) saveFileName = "out.ann";
				testAnnotator(labels, saveFileName);
			} else if ("expt".equals(command)) {
				SequenceClassifierLearner learner = SequenceAnnotatorExpt.toSeqLearner(learnerName);
				Splitter splitter = Expt.toSplitter(splitterName);
				doExpt(labels, splitter, learner, saveFileName, "all".equals(show));
			} else if ("printWords".equals(command)) {
				printAllWords(labels);
			} else {
				throw new IllegalArgumentException("unknown command '"+command+"'");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(
				"usage: -do train -labels KEY -mixup FILESTEM -learner LEARNER -save FILE");
			System.out.println(
				"       -do test -labels KEY -mixup FILESTEM -save FILE");
			System.out.println(
				"       -do expt -labels KEY -mixup FILESTEM -learner LEARNER -splitter SPLIT -save FILE [-show all]");
			System.out.println(
				"       also: -class name|date changes class to learn"); 
			System.out.println(
				"       also: -mixup mixupFileStem"); 

		}

	}

	/* do a train/test experiment */
	public static void doExpt(
		MutableTextLabels labels,
		Splitter splitter, 
		SequenceClassifierLearner learner,
		String outputFile,
		boolean explore)
	{
		try {

			System.out.println("teacher uses 'true_"+classToLearn+"'");
			//TextBaseEditor.edit(labels, null);

			AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher(labels,"true_"+classToLearn);
			SpanFeatureExtractor fe = fe(labels);
			SequenceDataset sequenceDataset = 
				SequenceAnnotatorLearner.prepareSequenceData(
					labels,"true_"+classToLearn,null,fe,classWindow,new InsideOutsideReduction());
			//ViewerFrame fd = new ViewerFrame("Name Learning Result",sequenceDataset.toGUI());

			DatasetIndex index = new DatasetIndex(sequenceDataset);
			System.out.println("Dataset: examples "+sequenceDataset.size()
												 +" features: "+index.numberOfFeatures()
												 +" avg features/examples: "+index.averageFeaturesPerExample());

			Evaluation e = null;
			if (!explore) {
				e = Tester.evaluate(learner,sequenceDataset,splitter);
				for (Iterator i=propertyMap.keySet().iterator(); i.hasNext(); ) {
					String prop = (String)i.next();
					e.setProperty( prop, (String)propertyMap.get(prop) );
				}
			} else {
				CrossValidatedSequenceDataset cvd = new CrossValidatedSequenceDataset( learner, sequenceDataset, splitter );
				ViewerFrame f = new ViewerFrame("Name Learning Result",cvd.toGUI());
				e = cvd.getEvaluation();
			} 
			String[] tags = e.summaryStatisticNames();
			double[] d = e.summaryStatistics();
			for (int i=0; i<d.length; i++) {
				System.out.println(tags[i]+": "+d[i]);
			}
			if (outputFile!=null) {
				e.save(new File(outputFile));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static SpanFeatureExtractor fe(TextLabels labels)
	{
		NameFE fe = new NameFE(); 
		Set props = labels.getTokenProperties();
		System.out.println("props: "+props);
		boolean useEq = true;
		fe.setWindowSize(featureWindow);
		fe.setTokenPropertyFeatures( props );
		fe.setUseEqOnNonAnchors(useEq);
		fe.setRequiredAnnotation(mixupFileStem);

		propertyMap.put("properties", props.toString() );
		propertyMap.put("useEqOnNonAnchors", Boolean.toString(useEq) );

		return fe;
	}

	/* train and save an annotator */
	public static void buildAnnotator(MutableTextLabels labels,SequenceClassifierLearner learner,String outputFile)
	{
		try {
			AnnotatorTeacher teacher = new TextLabelsAnnotatorTeacher(labels,"true_"+classToLearn);
			SpanFeatureExtractor fe = fe(labels);

			SequenceDataset sequenceDataset = 
				SequenceAnnotatorLearner.prepareSequenceData(
					labels,"true_"+classToLearn,null,fe,classWindow,new InsideOutsideReduction());

			//ViewerFrame fd = new ViewerFrame("Name Learning Result",sequenceDataset.toGUI());

			SequenceClassifier sequenceClassifier = 
				new DatasetSequenceClassifierTeacher(sequenceDataset).train(learner);
			Annotator annotator = 
				new SequenceAnnotatorLearner.SequenceAnnotator(sequenceClassifier,fe,"predicted_"+classToLearn);
			IOUtil.saveSerialized((Serializable)annotator,new File(outputFile));

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/* load and use an annotator */
	public static void testAnnotator(MutableTextLabels labels,String inFile)
	{
		try {
			Annotator annotator = (Annotator)IOUtil.loadSerialized(new File(inFile));
			annotator.annotate(labels);
			TextBaseEditor.edit(labels,new File("myCorrections.env"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** Use this to help find all name and non-name words in a text
	 */
	public static void printAllWords(MutableTextLabels labels)
	{
		try {
			MixupProgram prog = new MixupProgram(new String[]{
				"defTokenProp inTrueName:t =top: ... [@true_"+classToLearn+"] ..."});
                        MixupInterpreter interp = new MixupInterpreter(prog);
			interp.eval(labels);
			for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan(); 
				for (int j=0; j<s.size(); j++) {
					Token t = s.getToken(j);
					String tag = (labels.getProperty(t,"inTrueName")!=null) ? classToLearn : "word";
					System.out.println(tag + " " +t.getValue());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}

