package edu.cmu.minorthird.ui;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.sequential.*;

/** 
 * Defines the list of classes that can be selected by an instance of UIMain. 
 */

/*package*/ class SelectableTypes
{			
	public static final Class[] CLASSES = new Class[]
	{
		//
		// bunches of parameters
		//
		CommandLineUtil.BaseParams.class, CommandLineUtil.SaveParams.class, 
		CommandLineUtil.ClassificationSignalParams.class, CommandLineUtil.TrainClassifierParams.class, 
		CommandLineUtil.TestClassifierParams.class, CommandLineUtil.TestExtractorParams.class, 
		CommandLineUtil.LoadAnnotatorParams.class, CommandLineUtil.SplitterParams.class,
		CommandLineUtil.ExtractionSignalParams.class, CommandLineUtil.TrainExtractorParams.class,
		CommandLineUtil.TestClassifierParams.class, CommandLineUtil.TrainTaggerParams.class,
		CommandLineUtil.TaggerSignalParams.class, CommandLineUtil.MixupParams.class,
		//
		// main routines
		//
		ApplyAnnotator.class, TestExtractor.class, TrainClassifier.class, 
		TrainExtractor.class,	TestClassifier.class, TrainTestClassifier.class, 
		TrainTestExtractor.class, TrainTestTagger.class,
		RunMixup.class,
		//
		// recommended classification learners
		//
		Recommended.KnnLearner.class, Recommended.NaiveBayes.class,
		Recommended.VotedPerceptronLearner.class,	Recommended.SVMLearner.class,
		Recommended.DecisionTreeLearner.class, Recommended.BoostedDecisionTreeLearner.class,
		Recommended.BoostedStumpLearner.class, Recommended.MaxEntLearner.class,
		//
		// recommended sequence learners
		//
		Recommended.VPTagLearner.class, 
		//
		// recommended annotator learners
		//
		Recommended.VPHMMLearner.class, Recommended.VPCMMLearner.class, 
		Recommended.MEMMLearner.class, Recommended.SVMLearner.class, 
		Recommended.VPSMMLearner.class, Recommended.VPSMMLearner2.class, 
		Recommended.VPCMMLearner.class, Recommended.MEMMLearner.class,
    Recommended.SVMCMMLearner.class, Recommended.CRFAnnotatorLearner.class,
    Recommended.SemiCRFAnnotatorLearner.class, 
		SequenceAnnotatorLearner.class, SegmentAnnotatorLearner.class,
    //
    // to make parameters visible...
    //
    SegmentCRFLearner.class, CRFLearner.class, 
    SegmentCollinsPerceptronLearner.class, CollinsPerceptronLearner.class,
		//
		// reductions from annotator-learning to tagging
		//
		InsideOutsideReduction.class, BeginContinueEndUniqueReduction.class,
		//
		// recommend feature extractors
		//
		Recommended.DocumentFE.class, Recommended.TokenFE.class, Recommended.MultitokenSpanFE.class,
		//
		// splitters
		//
		CrossValSplitter.class, RandomSplitter.class, 
	};
}
