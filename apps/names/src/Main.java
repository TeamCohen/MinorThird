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
	static private int featureWindow=3,classWindow=5,epochs=6;
	static private String mixupFileStem="nameFeatures_v2";

	public static void main(String[] args)
	{
		int pos=0; 
		String 
			command=null,
			learnerName="new CollinsPerceptronLearner("+classWindow+","+epochs+")",
			envKey=null,
			saveFileName=null,
			splitterName="r50",
			show=null;

		while (pos<args.length) {
			String opt = args[pos++];
			if (opt.equals("-do")) command = args[pos++];
			else if (opt.startsWith("-lea")) learnerName = args[pos++];
			else if (opt.startsWith("-env")) envKey = args[pos++];
			else if (opt.startsWith("-save")) saveFileName = args[pos++];
			else if (opt.startsWith("-split")) splitterName = args[pos++];
			else if (opt.startsWith("-mix")) mixupFileStem = args[pos++];
			else if (opt.startsWith("-show")) show = args[pos++];
		}

		try {

			System.out.println("loading environment "+envKey);
			if (envKey==null) throw new IllegalArgumentException("-env ENV must be specified");
			MutableTextEnv env = (MutableTextEnv)FancyLoader.loadTextEnv(envKey);
			System.out.println("loading "+mixupFileStem+".mixup...");
			MixupProgram program = new MixupProgram(new File(mixupFileStem+".mixup"));
			program.eval(env, env.getTextBase());

			if ("train".equals(command)) {
				SequenceClassifierLearner learner = SequenceAnnotatorExpt.toSeqLearner(learnerName);
				if (saveFileName==null) saveFileName = "out.ann";
				buildAnnotator(env, learner, saveFileName);
			} else if ("test".equals(command)) {
				if (saveFileName==null) saveFileName = "out.ann";
				testAnnotator(env, saveFileName);
			} else if ("expt".equals(command)) {
				SequenceClassifierLearner learner = SequenceAnnotatorExpt.toSeqLearner(learnerName);
				Splitter splitter = Expt.toSplitter(splitterName);
				doExpt(env, splitter, learner, saveFileName, "all".equals(show));
			} else {
				throw new IllegalArgumentException("unknown command '"+command+"'");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(
				"usage: -do train -env KEY -mixup FILESTEM -learner LEARNER -save FILE");
			System.out.println(
				"       -do test -env KEY -mixup FILESTEM -save FILE");
			System.out.println(
				"       -do expt -env KEY -mixup FILESTEM -learner LEARNER -splitter SPLIT -save FILE [-show all]");
		}

	}

	/* do a train/test experiment */
	public static void doExpt(
		MutableTextEnv env,
		Splitter splitter, 
		SequenceClassifierLearner learner,
		String outputFile,
		boolean explore)
	{
		try {

			AnnotatorTeacher teacher = new TextEnvAnnotatorTeacher(env,"true_name");
			SpanFeatureExtractor fe = fe(env);

			SequenceAnnotatorLearner dummy = new SequenceAnnotatorLearner(fe,classWindow) {
					public Annotator getAnnotator() { return null; }
				};
			teacher.train(dummy);
			SequenceDataset sequenceDataset = dummy.getSequenceDataset();
			//ViewerFrame fd = new ViewerFrame("Name Learning Result",sequenceDataset.toGUI());

			Evaluation e = null;
			if (explore) {
				CrossValidatedSequenceDataset cvd = new CrossValidatedSequenceDataset( learner, sequenceDataset, splitter );
				ViewerFrame f = new ViewerFrame("Name Learning Result",cvd.toGUI());
				if (outputFile!=null) {
					e = cvd.getEvaluation();
				}
			} 
				
			if (outputFile!=null) {
				if (e==null) e = Tester.evaluate(learner,sequenceDataset,splitter);
				e.setProperty("learner",learner.toString());
				e.setProperty("splitter",splitter.toString());
				e.save(new File(outputFile));
			}
			System.out.println("learner: "+learner.toString());
			System.out.println("splitter: "+splitter.toString());
			String[] tags = e.summaryStatisticNames();
			double[] d = e.summaryStatistics();
			for (int i=0; i<d.length; i++) {
				System.out.println(tags[i]+": "+d[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static SpanFeatureExtractor fe(TextEnv env)
	{
		NameFE fe = new NameFE(); 
		Set props = env.getTokenProperties();
		System.out.println("props: "+props);
		fe.setWindowSize(featureWindow);
		fe.setTokenPropertyFeatures( props );
		fe.setUseEqOnNonAnchors(true);
		fe.setRequiredAnnotation(mixupFileStem);
		return fe;
	}

	/* train and save an annotator */
	public static void buildAnnotator(MutableTextEnv env,SequenceClassifierLearner learner,String outputFile)
	{
		try {
			AnnotatorTeacher teacher = new TextEnvAnnotatorTeacher(env,"true_name");
			SpanFeatureExtractor fe = fe(env);

			SequenceAnnotatorLearner dummy = new SequenceAnnotatorLearner(fe,classWindow) {
					public Annotator getAnnotator() { return null; }
				};
			teacher.train(dummy);
			SequenceDataset sequenceDataset = dummy.getSequenceDataset();
			//ViewerFrame fd = new ViewerFrame("Name Learning Result",sequenceDataset.toGUI());

			SequenceClassifier sequenceClassifier = 
				new DatasetSequenceClassifierTeacher(sequenceDataset).train(learner);
			Annotator annotator = new SequenceAnnotatorLearner.SequenceAnnotator(sequenceClassifier,fe,"predicted_name");
			IOUtil.saveSerialized((Serializable)annotator,new File(outputFile));

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/* load and use an annotator */
	public static void testAnnotator(MutableTextEnv env,String inFile)
	{
		try {
			Annotator annotator = (Annotator)IOUtil.loadSerialized(new File(inFile));
			annotator.annotate(env);
			TextBaseEditor.edit(env,new File("myCorrections.env"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** Use this to help find all name and non-name words in a text
	 */
	public static void printAllWords()
	{
		try {
			MutableTextEnv env = (MutableTextEnv) FancyLoader.loadTextEnv("cspace.bsh");
			MixupProgram prog = new MixupProgram(new String[]{"defTokenProp inTrueName:t =top: ... [@true_name] ..."});
			prog.eval(env,env.getTextBase());
			for (Span.Looper i=env.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan(); 
				for (int j=0; j<s.size(); j++) {
					Token t = s.getToken(j);
					String tag = (env.getProperty(t,"inTrueName")!=null) ? "name" : "word";
					System.out.println(tag + " " +t.getValue());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

