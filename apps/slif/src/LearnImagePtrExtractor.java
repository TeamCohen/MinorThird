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
    static { Mixup.maxNumberOfMatchesPerToken = 20; }

    /** Heuristic used to find candidate image pointers */
    static public SpanFinder candidateFinder;

    /** Computes features used by learner */
    static public MixupProgram featureProgram;

    // initialize
    static {
	try {
	    candidateFinder = new MixupFinder( new Mixup("... [L eq('(') !eq(')'){1,15}R eq(')') R] ...") );
	    featureProgram = 	new MixupProgram( new File("lib/features.mixup" ) );
	} catch (Exception e) {
	    throw new IllegalStateException("mixup or io error: "+e);
	}
    }

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

    /** Load the initial labels */
    static public MutableTextLabels loadLabels() throws IOException,Mixup.ParseException,java.text.ParseException
    {
	// load the data and labels
	//TextBase base = TextBaseLoader.loadDocPerLine(new File("data/captions/lines.txt"),true);
	TextBase base = new TextBaseLoader().load(new File("data/captions/caption-lines"));
	TextLabelsLoader eloader = new TextLabelsLoader();
	MutableTextLabels labels = eloader.loadOps(base,new File("labels/imgptr.env"));
	return labels;
    }

    static public void main(String argv[]) throws IOException,Mixup.ParseException,java.text.ParseException
    {
	// load the labels and compute the features
	MutableTextLabels labels = loadLabels();
	MixupInterpreter interp = new MixupInterpreter(featureProgram);
        interp.eval(labels);

	if (argv.length>0 && "-expt".equals(argv[0])) {
	    String className = argv.length>=2 ? argv[1] : "regional";
	    String learnerName = argv.length>=3 ? argv[2] : "new AdaBoost()";
	    BinaryClassifierLearner learner = (BinaryClassifierLearner)Expt.toLearner(learnerName);
	    AnnotatorLearner annnotatorLearner = makeAnnotatorLearner(learner);
	    String predClassName = predictedClassName(className);
	    TextLabelsExperiment expt = new TextLabelsExperiment(labels,new CrossValSplitter(10), annnotatorLearner, className, predClassName);
	    expt.doExperiment();
	    TextBaseViewer.view( expt.getTestLabels() );
	} else if (argv.length>0 && "-save".equals(argv[0])) {
	    String[] classNames = new String[] { "regional","local" };
	    for (int i=0; i<classNames.length; i++) {
		System.out.println("Training classifier for "+classNames[i]+" imgptrs");
		BinaryClassifierLearner learner = new AdaBoost();
		BatchFilteredFinderLearner annotatorLearner = makeAnnotatorLearner(learner);
		new TextLabelsAnnotatorTeacher(labels,classNames[i]).train(annotatorLearner);
		Classifier classifier = annotatorLearner.getClassifier();
		IOUtil.saveSerialized((Serializable)classifier,new File("lib/"+classNames[i]+"Filter.ser"));
	    }
	} else {
	    System.out.println("usage: -expt className learner");
	    System.out.println("usage: -save");
	}
    }
}
