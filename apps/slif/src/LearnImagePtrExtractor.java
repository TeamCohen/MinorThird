import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;

import java.util.*;
import java.io.*;

public class LearnImagePtrExtractor
{
	/** Heuristic used to find candidate image pointers */
	static public SpanFinder candidateFinder;

	/** Computes features used by learner */
	static public MixupProgram featureProgram;

	// initialize
	static {
		try {
			candidateFinder = new MixupFinder( new Mixup("... [eq('(') !eq(')'){1,15} eq(')')] ...") );
			featureProgram = 	new MixupProgram( new File("lib/features.mixup" ) );
		} catch (Exception e) {
			throw new IllegalStateException("mixup or io error: "+e);
		}
	}

	/** Feature extractor used by the learners */
	static public class ImgPtrFE implements SpanFeatureExtractor {
		private int windowSize=3;
		public Instance extractInstance(Span s)	{
			throw new UnsupportedOperationException("can't!");
		}
		public Instance extractInstance(TextEnv env, Span s)	{
			FeatureBuffer buf = new FeatureBuffer(env,s);
			SpanFE.from(s,buf).tokens().emit(); 
			for (int i=0; i<windowSize; i++) {
				SpanFE.from(s,buf).tokens().emit();
				SpanFE.from(s,buf).tokens().prop("cap").emit();
				SpanFE.from(s,buf).left().token(-i-1).emit(); 
				SpanFE.from(s,buf).left().token(-i-1).prop("cap").emit(); 
				SpanFE.from(s,buf).right().token(i).emit(); 
				SpanFE.from(s,buf).right().token(i).prop("cap").emit(); 
			}
			return buf.getInstance();
		}
	};

	/** Create the learner */
	static private BatchFilteredFinderLearner makeAnnotatorLearner(BinaryClassifierLearner classifierLearner)
	{
		BatchFilteredFinderLearner annotatorLearner = 
			new BatchFilteredFinderLearner( new ImgPtrFE(), classifierLearner, candidateFinder );
		return annotatorLearner;
	}
	
	static public String predictedClassName(String className)
	{
		return "predicted"+className.substring(0,1).toUpperCase()+className.substring(1);
	}

	/** Load the initial (labeled) environment */
	static public MutableTextEnv loadEnv() throws IOException,Mixup.ParseException
	{
		// load the data and labels
		TextBase base = new BasicTextBase();
		TextBaseLoader bloader = new TextBaseLoader();
		bloader.setFirstWordIsDocumentId(true);
		bloader.loadLines(base,new File("data/captions/lines.txt"));
		TextEnvLoader eloader = new TextEnvLoader();
		MutableTextEnv env = eloader.loadOps(base,new File("labels/imgptr.env"));
		return env;
	}

	static public void main(String argv[]) throws IOException,Mixup.ParseException
	{
		// load the environment and compute the features
		MutableTextEnv env = loadEnv();
		featureProgram.eval(env,env.getTextBase());

		if (argv.length>0 && "-expt".equals(argv[0])) {
			String className = argv.length>=2 ? argv[1] : "regional";
			String learnerName = argv.length>=3 ? argv[2] : "new AdaBoost()";
			BinaryClassifierLearner learner = (BinaryClassifierLearner)Expt.toLearner(learnerName);
			AnnotatorLearner annnotatorLearner = makeAnnotatorLearner(learner);
			String predClassName = predictedClassName(className);
			TextEnvExpt expt = new TextEnvExpt(env,new CrossValSplitter(10), annnotatorLearner, className, predClassName);
			expt.doExperiment();
			TextBaseViewer.view( expt.getTestEnv() );
		} else if (argv.length>0 && "-save".equals(argv[0])) {
			String[] classNames = new String[] { "regional","local" };
			for (int i=0; i<classNames.length; i++) {
				System.out.println("Training classifier for "+classNames[i]+" imgptrs");
				BinaryClassifierLearner learner = new AdaBoost();
				BatchFilteredFinderLearner annotatorLearner = makeAnnotatorLearner(learner);
				new TextEnvAnnotatorTeacher(env,classNames[i]).train(annotatorLearner);				
				Classifier classifier = annotatorLearner.getClassifier();
				IOUtil.saveSerialized((Serializable)classifier,new File("lib/"+classNames[i]+"Filter.ser"));
			}
		} else {
			System.out.println("usage: -expt className learner");
			System.out.println("usage: -save");
		}
	}
}
