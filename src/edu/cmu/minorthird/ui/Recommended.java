package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.algorithms.knn.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.transform.*;
import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Recommended configurations of objects like learners, feature extractors, etc.
 *
 * @author William Cohen
 */

public class Recommended
{
	private static Logger log = Logger.getLogger(Recommended.class);

	//
	// classifier learners
	//

	/** K-NN learning following Yang and Chute. This uses a TFIDF transformation
	 * of the features, and averages the 30 nearest neighbors.
	 */
	static public class KnnLearner extends TransformingBatchLearner 
	{
		public KnnLearner() 
		{ 
			super(new TFIDFTransformLearner(), 
						new BatchVersion(new edu.cmu.minorthird.classify.algorithms.knn.KnnLearner(30),1)); 
		}
	}
	
	/** Multinomial Naive bayes.
	 */
	static public class NaiveBayes extends edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes
	{
		public NaiveBayes() { super(); }
	}

	/** Voted perceptron learning following Freund & Schapire.  Iterates
	 * over the data 5 times.
	 */
	static public class VotedPerceptronLearner extends BatchVersion 
	{
		public VotedPerceptronLearner() 
		{
			super(new edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron(),5);
		}
	}

	/** SVM learner with no weighting and a linear kernel. */
	static public class SVMLearner extends edu.cmu.minorthird.classify.algorithms.svm.SVMLearner
	{
		public SVMLearner() { super(); }
	}

	/** Maximum entropy learner/linear regression */
	static public class MaxEntLearner extends edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner
	{
		public MaxEntLearner() { super("maxIters 50"); }
	}

	/** Default decision tree learner.  */
	static public class DecisionTreeLearner extends edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner
	{
		public DecisionTreeLearner() { super(); }
	}

	/** Boosts the default decision tree learner 10 times. */
	static public class BoostedDecisionTreeLearner extends AdaBoost
	{
		public BoostedDecisionTreeLearner() { super(new DecisionTreeLearner(),10); }
	}

	/** Boosts a decision stump 100 times. */
	static public class BoostedStumpLearner extends AdaBoost
	{
		public BoostedStumpLearner() 
		{ 
			super(new edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner(), 100); 
		}
	}

	//
	// annotator learners
	//

	/** Uses the voted perceptron algorithm to learn hidden semi-Markov model (SMM)
	 */
	static public class VPSMMLearner extends ConditionalSemiMarkovModel.CSMMLearner
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		/**	 Extracted entities must be of length 4 or less. */
		public VPSMMLearner() { super(20,4); }
		public VPSMMLearner(int maxLength) { super(20,maxLength); }
	}

	/** Uses the voted perceptron algorithm to learn hidden semi-Markov model (SMM)
	 */
	static public class VPSMMLearner2 extends SegmentAnnotatorLearner
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		/**	 Extracted entities must be of length 4 or less. */
		public VPSMMLearner2() { super(new SegmentCollinsPerceptronLearner(), new MultitokenSpanFE(), 4); }
		public VPSMMLearner2(int epochs,int maxLen) { super(new SegmentCollinsPerceptronLearner(epochs), new MultitokenSpanFE(), maxLen); }
	}
	
	/** Uses the voted perceptron algorithm to learn a hidden Markov model (HMM).
	 */
	static public class VPHMMLearner extends SequenceAnnotatorLearner
	{
		public VPHMMLearner() { 
			super(new CollinsPerceptronLearner(1,5), new Recommended.TokenFE());
		}
	}

	/** Uses the voted perceptron algorithm to learn a conditional Markov model (CMM).
	 */
	static public class VPCMMLearner extends SequenceAnnotatorLearner
	{
		public VPCMMLearner() { 
			super(new CMMLearner(new VotedPerceptronLearner(),1),new Recommended.TokenFE());
		}
	}

	/** Uses logistic regression to learn a condition Markov model
	 * (CMM), aka maxent Markov model (MEMM).
	 */
	static public class MEMMLearner extends SequenceAnnotatorLearner
	{
		public MEMMLearner() { 
			super(new CMMLearner(new MaxEntLearner(),	1), new Recommended.TokenFE());
		}
	}

	/** Uses probabilistic SVM to learn a condition Markov model (CMM).
	 */
	static public class SVMCMMLearner extends SequenceAnnotatorLearner
	{
		public SVMCMMLearner() { 
			super(new CMMLearner(new SVMLearner(), 1), new Recommended.TokenFE());
		}
	}

	/** Uses the voted perceptron algorithm to learn a hidden Markov model (HMM).
	 */
	static public class CRFAnnotatorLearner extends SequenceAnnotatorLearner
	{
		public CRFAnnotatorLearner() { 
			super(new CRFLearner(), new Recommended.TokenFE(), new BeginContinueEndUniqueReduction());
		}
	}

	/** Uses the voted perceptron algorithm to learn hidden semi-Markov model (SMM)
	 */
	static public class SemiCRFAnnotatorLearner extends SegmentAnnotatorLearner
	{
		/**	 Extracted entities must be of length 4 or less. */
		public SemiCRFAnnotatorLearner() 
    { 
      super(new SegmentCRFLearner(""), new MultitokenSpanFE(), 4); 
    }
		public SemiCRFAnnotatorLearner(int maxIters,int maxLen) 
    { 
      super(new SegmentCRFLearner("maxIters "+maxIters), new MultitokenSpanFE(), maxLen); 
    }
	}
	


	//
	// SequenceClassifierLearners
	//

	public static class VPTagLearner extends CollinsPerceptronLearner
	{
		public VPTagLearner() { super(1,5); }
	}

	//
	// feature extractors
	//

	/** Simple bag-of-words feature extractor, with words being but in lower case.
	 */
	abstract public static class TokenPropUsingFE extends SpanFE implements Serializable
	{
		protected String[] tokenPropertyFeatures=null;
		/** tokenProperties depend on the requiredAnnotation, so override
		 * default setRequiredAnnotation() method to reset the
		 * tokenPropertyFeatures to null when this changes.
		 */
		public void setRequiredAnnotation(String requiredAnnotation,String annotationProvider)
		{
			super.setRequiredAnnotation(requiredAnnotation,annotationProvider);
			tokenPropertyFeatures = null;
		}
		/** Specify the token properties from the TextLabels environment
		 * that will be used as features. A value of '*' or nul means to
		 * use all defined token properties. */
		public void setTokenPropertyFeatures(String commaSeparatedTokenPropertyList) 
		{
			if ("*".equals(commaSeparatedTokenPropertyList)) tokenPropertyFeatures = null;
			else tokenPropertyFeatures = commaSeparatedTokenPropertyList.split(",\\s*");
		}
		public String getTokenPropertyFeatures() { 
			return tokenPropertyFeatures==null ? "*" : StringUtil.toString(tokenPropertyFeatures); 
		}
		public void setTokenPropertyFeatures(Set propertySet) 
		{
			tokenPropertyFeatures = (String[])propertySet.toArray(new String[propertySet.size()]);
		}
		protected void setMyTokenPropertyList(TextLabels labels) {
			if (tokenPropertyFeatures==null) {
				log.info("tokenPropertyFeatures: "+labels.getTokenProperties());
				setTokenPropertyFeatures( labels.getTokenProperties() );
			}
		}
	}

	public static class DocumentFE extends TokenPropUsingFE implements Serializable {
		protected boolean foldCase=true;
		public void extractFeatures(TextLabels labels, Span s){
			requireMyAnnotation(labels);
			setMyTokenPropertyList(labels);
			if (foldCase) from(s).tokens().eq().lc().emit();
			else from(s).tokens().eq().emit();
			for (int j=0; j<tokenPropertyFeatures.length; j++) {
				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
		}
		public boolean getFoldCase() { return foldCase; }
		public void setFoldCase(boolean flag) { foldCase=flag; }	
	}


	/** An extraction-oriented feature extractor to apply to one-token spans.
	 */
	public static class TokenFE extends TokenPropUsingFE implements CommandLineProcessor.Configurable,Serializable
	{
		protected int windowSize=3;
		protected boolean useCharType=false;
		protected boolean useCharTypePattern=true;
		//
		// getters and setters, for gui-based configuration
		//
		/** Window size for features. */
		public void setFeatureWindowSize(int n) { windowSize=n; }
		public int getFeatureWindowSize() { return windowSize; }
		/** If produce features like "token.charTypePattern.Aaaa" for the word "Bill" */
		public void setUseCharType(boolean flag) { useCharType=flag; }
		public boolean getUseCharType() { return useCharType; }
		/** If true produce features like "token.charTypePattern.Aa+" for the word "Bill". */
		public void setUseCharTypePattern(boolean flag) { useCharTypePattern=flag; }
		public boolean getUseCharTypePattern() { return useCharTypePattern; }
		//
		// command-line based configuration
		//
		public CommandLineProcessor getCLP() 
		{
			return new MyCLP();
		}
		public class MyCLP extends BasicCommandLineProcessor {
			public void window(String s) { windowSize = StringUtil.atoi(s); System.out.println("window=>"+s);}
			public void charTypes() { useCharType = true; }
			public void noCharTypes() { useCharType = false; }
			public void charTypePattern() { useCharTypePattern = true; }
			public void noCharTypePattern() { useCharTypePattern = false; }
			public void tokenProps(String s) { setTokenPropertyFeatures(s); }
		}
		//
		// real code (i.e., not configuration code)
		//
		public void extractFeatures(TextLabels labels, Span s)
		{
			requireMyAnnotation(labels);
			setMyTokenPropertyList(labels);
			from(s).tokens().eq().lc().emit();
			if (useCharTypePattern) from(s).tokens().eq().charTypePattern().emit();
			if (useCharType) from(s).tokens().eq().charTypes().emit();
			for (int j=0; j<tokenPropertyFeatures.length; j++) {
				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
			for (int i=0; i<windowSize; i++) {
				from(s).left().token(-i-1).eq().lc().emit();
				from(s).right().token(i).eq().lc().emit();
				for (int j=0; j<tokenPropertyFeatures.length; j++) {
					from(s).left().token(-i-1).prop(tokenPropertyFeatures[j]).emit();
					from(s).right().token(i).prop(tokenPropertyFeatures[j]).emit();
				}
				if (useCharTypePattern) {
					from(s).left().token(-i-1).eq().charTypePattern().emit();
					from(s).right().token(i).eq().charTypePattern().emit();
				}
				if (useCharType) {
					from(s).left().token(-i-1).eq().charTypes().emit();
					from(s).right().token(i).eq().charTypes().emit();
				}
			}
		}
	}
	
	/** An extraction-oriented feature extractor to apply to multi-token spans. */
	public static class MultitokenSpanFE extends TokenFE implements CommandLineProcessor.Configurable,Serializable
	{
		private boolean useFirst=true,useLast=true,useLength=true,useInternal=true;
		//
		// getters/setters for gui configuration
		//
		/** Generate features for the first token of the span. */
		public void setUseFirst(boolean flag) { useFirst=flag; }
		public boolean getUseFirst() { return useFirst; }
		/** Generate features for the last token of the span. */
		public void setUseLast(boolean flag) { useLast=flag; }
		public boolean getUseLast() { return useLast; }
		/** Generate features for the length of the span. */
		public void setUseLength(boolean flag) { useLength=flag; }
		public boolean getUseLength() { return useLength; }
		/** Generate features for the span itself */
		public void setUseInternal(boolean flag) { useInternal=flag; }
		public boolean getUseInternal() { return useInternal; }
		//
		// command-line configuration
		//
		public CommandLineProcessor getCLP() 
		{
			return new JointCommandLineProcessor( new CommandLineProcessor[]{	super.getCLP(),new MyCLP() } );
		}
		public class MyCLP extends BasicCommandLineProcessor {
			public void first() { useFirst = true; }
			public void noFirst() { useFirst = false; }
			public void last() { useLast = true; }
			public void noLast() { useLast = false; }
			public void length() { useLength = true; }
			public void noLength() { useLength = false; }
			public void internal() { useInternal = true; }
			public void noInternal() { useInternal = false; }		
		}

		//
		// 'real' code
		//
		public void extractFeatures(TextLabels labels,Span span) 
		{
			super.extractFeatures(labels,span);
			// text of span & its charTypePattern
			if (useInternal) {
				from(span).eq().lc().emit();
				if (useCharType) from(span).eq().charTypes().emit();
				if (useCharTypePattern) from(span).eq().charTypePattern().emit();
			}
			// length properties of span
			if (useLength) {
				from(span).size().emit();
				from(span).exactSize().emit();
			}
			// first and last tokens
			if (useFirst) from(span).token(0).eq().lc().emit();
			if (useLast) from(span).token(-1).eq().lc().emit();
			if (useCharType) {
				if (useFirst) from(span).token(0).eq().charTypes().lc().emit();
				if (useLast) from(span).token(-1).eq().charTypes().lc().emit();
			}
			if (useCharTypePattern) {
				if (useFirst) from(span).token(0).eq().charTypePattern().lc().emit();
				if (useLast) from(span).token(-1).eq().charTypePattern().lc().emit();
			}
			// use marked properties of tokens for first & last tokens in span
			for (int i=0; i<tokenPropertyFeatures.length; i++) {
				String p = tokenPropertyFeatures[i];
				// first & last tokens
				if (useFirst) from(span).token(0).prop(p).emit();
				if (useLast) from(span).token(-1).prop(p).emit();
			}
		}
	}
}
