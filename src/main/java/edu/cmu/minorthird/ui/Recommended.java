package edu.cmu.minorthird.ui;

import java.io.Serializable;
import java.util.Set;

import edu.cmu.minorthird.classify.BatchVersion;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.sequential.CMMLearner;
import edu.cmu.minorthird.classify.sequential.CRFLearner;
import edu.cmu.minorthird.classify.sequential.CollinsPerceptronLearner;
import edu.cmu.minorthird.classify.sequential.HMMLearner;
import edu.cmu.minorthird.classify.sequential.SegmentCRFLearner;
import edu.cmu.minorthird.classify.sequential.SegmentCollinsPerceptronLearner;
import edu.cmu.minorthird.classify.transform.TFIDFTransformLearner;
import edu.cmu.minorthird.classify.transform.TransformingBatchLearner;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.BeginContinueEndUniqueReduction;
import edu.cmu.minorthird.text.learn.ConditionalSemiMarkovModel;
import edu.cmu.minorthird.text.learn.InsideOutsideReduction;
import edu.cmu.minorthird.text.learn.SegmentAnnotatorLearner;
import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.text.learn.SpanFE;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.StringUtil;

/**
 * In Minorthird it is possible to build up constructs like learners,
 * feature extractors, and so on compositionally, out of simpler
 * pieces.  This class contains pre-configured "recommended" instances
 * of some common learners, feature extractors, etc.
 *
 * @author William Cohen
 */

public class Recommended{

	//
	// classifier learners
	//

	/** K-NN learner following Yang and Chute. This uses a TFIDF
	 * transformation of the features, and averages the scores 30
	 * nearest neighbors, with score weighted by TFIDF distance to the
	 * instance being classifier. Unlike many of the learners in the
	 * 'recommended' package, this is a non-binary (multi-class)
	 * learner.  
	 *
	 * <p>Training for this learner is very fast, but classification
	 * time is rather slow.
	 *
	 * <p>Reference: Y. Yang and C.G. Chute, <i>An example-based mapping
	 * method for text classification and retrieval</i>,
	 * ACM Transactions on Information Systems, 3(12), 1994.
	 */
	static public class KnnLearner extends TransformingBatchLearner{

		public KnnLearner(){
			super(new TFIDFTransformLearner(),new BatchVersion(
					new edu.cmu.minorthird.classify.algorithms.knn.KnnLearner(30),1));
		}
	}

	/** Multinomial Naive Bayes, as in McCallum's Rainbow package.
	 *
	 *<p>This is one of the fastest learners, but because of the strong
	 * independence assumptions, it often has a higher-than-necessary
	 * error rate.
	 *
	 * <p>References: Andrew McCallum and Kamal Nigam, <i>A comparison
	 * of event models for naive bayes text classification</i>, AAAI-98
	 * Workshop on Learning for Text Categorization; Kamal Nigam et al,
	 * <i>Text classification from labeled and unlabeled documents using
	 * EM</i>, Machine Learning, 39(2/3), 2000.
	 */
	static public class NaiveBayes extends
			edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes{
		static final long serialVersionUID=20080517L;
		public NaiveBayes(){
			super();
		}
	}

	/** A Tweaked Learner, with an optimization of the precision vs. recall
	 * 
	 * @author Giora Unger
	 *
	 * A learner whose score was optimized according to an F_beta() function,
	 * for a given beta. This optimization is used to fine-tune the precision
	 * vs. recall for the underlying classification algorithm.
	 * Values of beta<1.0 favor precision over recall, while values of
	 * beta>1.0 favor recall over precision. beta=1.0 grants equal weight 
	 * to both precision and recall.  
	 * 
	 * Note:
	 * - Currently, in a hard-coded manner, the leaner takes a NaiveBayse class
	 *   as its inner learner and a value of beta=1.0
	 *  
	 * <p>Reference: Jason D. M. Rennie,
	 * <i>Derivation of the F-Measure</i>,
	 * http://people.csail.mit.edu/jrennie/writing/fmeasure.pdf
	 */
	static public class TweakedLearner extends
			edu.cmu.minorthird.classify.TweakedLearner{

		public TweakedLearner(){
			super(new NaiveBayes(),1.0);
		}
	}

	/** Voted perceptron learning following Freund & Schapire.  This is
	 * a simple learning method which, like SVMs, has a bias towards
	 * large-margin linear classifiers.
	 * 
	 * <p>Reference: Yoav Freund and Robert E. Schapire,
	 * <i>Large Margin Classification Using the Perceptron Algorithm</i>,
	 * Computational Learning Theory, 1998.
	 */
	static public class VotedPerceptronLearner extends BatchVersion{

		public VotedPerceptronLearner(){
			super(
					new edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron(),5);
		}
	}

	/** A simple SVM learner with a linear kernel. 
	 */
	static public class SVMLearner extends
			edu.cmu.minorthird.classify.algorithms.svm.SVMLearner{

		public SVMLearner(){
			super();
		}
	}

	/** A maximum entropy learner.
	 * 
	 * <p>This is a wrapper around the CRF learner, which is built on
	 * the IIT CRF implementation.  Iterations of the optimization
	 * method are limited to 50, by default. 
	 */
	static public class MaxEntLearner extends
			edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner{

		public MaxEntLearner(){
			super("maxIters 50");
		}
	}

	static public class OneVsAllLearner extends
			edu.cmu.minorthird.classify.OneVsAllLearner{

		public OneVsAllLearner(){
			super(new Recommended.MaxEntLearner());
		}
	}

	static public class MostFrequentFirstLearner extends
			edu.cmu.minorthird.classify.MostFrequentFirstLearner{

		public MostFrequentFirstLearner(){
			super(new Recommended.MaxEntLearner());
		}
	}

	static public class CascadingBinaryLearner extends
			edu.cmu.minorthird.classify.CascadingBinaryLearner{

		public CascadingBinaryLearner(){
			super(new Recommended.MaxEntLearner());
		}
	}

	/** A simple decision tree learner.  
	 *
	 * <p>
	 * This has no pruning, and limits decision trees to a depth of 5.
	 * The splitting criterion is modelled after the one used in
	 * Cohen & Singer's SLIPPER system---it is designed to optimize
	 * performance of the metric being optimized by AdaBoost.
	 * 
	 * <p>Related references: William W. Cohen and Yoram Singer, <i>A
	 * Simple, Fast, and Effective Rule Learner</i>, Proceedings of the
	 * Sixteenth National Conference on Artificial Intelligence
	 * (AAAI-99); J. Ross Quinlan, <i>C4.5: programs for machine
	 * learning</i>, Morgan Kaufmann, 1994.
	 */
	static public class DecisionTreeLearner extends
			edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner{

		public DecisionTreeLearner(){
			super();
		}
	}

	/** Uses AdaBoost to boosts the default decision tree learner 10 times. 
	 *
	 * <p>Reference: Yoav Freund and Robert E. Schapire,
	 * <i>Experiments with a New Boosting Algorithm</i>,
	 * Proc. of International Conference on Machine Learning,
	 * 1996.
	 */
	static public class BoostedDecisionTreeLearner extends AdaBoost{

		public BoostedDecisionTreeLearner(){
			super(new DecisionTreeLearner(),10);
		}
	}

	/** Uses AdaBoost to boosts a two-level decision tree learner 100
	 * times. 
	 *
	 * <p>Reference: Yoav Freund and Robert E. Schapire,
	 * <i>Experiments with a New Boosting Algorithm</i>,
	 * Proc. of International Conference on Machine Learning,
	 * 1996.
	 */
	static public class BoostedStumpLearner extends AdaBoost{

		public BoostedStumpLearner(){
			super(
					new edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner(),
					100);
		}
	}

	//
	// annotator learners
	//

	/** 
	 * Uses the voted perceptron algorithm to learn the parameters for a
	 * hidden semi-Markov model (SMM).
	 *
	 * <p>This is a somewhat more expensive version of the VPHMMLearner,
	 * which allows features to describe properties of multi-token
	 * spans, rather than only properties of single tokens.  This
	 * implements the training algorithm described in the <i>initial</i>
	 * draft of Cohen & Saragi's KDD paper.  This implementation is less
	 * memory-intensive than the VPSMMLearner2 package below, but
	 * slower, since the feature-extraction step is iterated many times.
	 *
	 * <p>Reference: William W. Cohen and Sunita Sarawagi, <i>Exploiting
	 * Dictionaries in Named Entity Extraction: Combining Semi-Markov
	 * Extraction Processes and Data Integration Methods</i>,
	 * Proceedings of the Tenth ACM SIGKDD International Conference on
	 * Knowledge Discovery and Data Mining (KDD-2004).
	 */
	static public class VPSMMLearner extends
			ConditionalSemiMarkovModel.CSMMLearner{

		static private final long serialVersionUID=1;

		/**	 Extracted entities must be of length 4 or less. */
		public VPSMMLearner(){
			super(20,4);
		}

		public VPSMMLearner(int maxLength){
			super(20,maxLength);
		}
	}

	/** 
	 * Uses the voted perceptron algorithm to learn the parameters for a
	 * hidden semi-Markov model (SMM).
	 *
	 * <p>This is a somewhat more expensive version of the VPHMMLearner,
	 * which allows features to describe properties of multi-token
	 * spans, rather than only properties of single tokens.  This
	 * implements the training algorithm described in the final draft of
	 * Cohen & Saragi's KDD paper.  This implementation is more
	 * memory-intensive than the VPSMMLearner2 package below, but
	 * faster, since the feature-extraction step is only performed once.
	 *
	 * <p>I generally prefer thus method to the (older) VPHMMLearner.
	 *
	 * <p>Reference: William W. Cohen and Sunita Sarawagi, <i>Exploiting
	 * Dictionaries in Named Entity Extraction: Combining Semi-Markov
	 * Extraction Processes and Data Integration Methods</i>,
	 * Proceedings of the Tenth ACM SIGKDD International Conference on
	 * Knowledge Discovery and Data Mining (KDD-2004).
	 */
	static public class VPSMMLearner2 extends SegmentAnnotatorLearner{

		static private final long serialVersionUID=1;

		/**	 Extracted entities must be of length 4 or less. */
		public VPSMMLearner2(){
			super(new SegmentCollinsPerceptronLearner(),new MultitokenSpanFE(),4);
		}

		public VPSMMLearner2(int epochs,int maxLen){
			super(new SegmentCollinsPerceptronLearner(epochs),new MultitokenSpanFE(),
					maxLen);
		}
	}

	/** Uses the voted perceptron algorithm to learn a parameters of a
	 * hidden Markov model (HMM).  This method is similar to a CRF, but
	 * often less expensive to train.
	 *
	 * <p>This iterates over the data only 5 times.  Subsequent
	 * experiments suggest that iterating 20, 50, or 100 times often
	 * gives better performance.
	 *
	 * <p>Reference: Michael Collins, <i>Discriminative Training Methods
	 * for Hidden Markov Models: Theory and Experiments with Perceptron
	 * Algorithms</i>, Empirical Methods in Natural Language Processing (EMNLP),
	 * 2002.
	 * 
	 */
	static public class VPHMMLearner extends SequenceAnnotatorLearner{

		public VPHMMLearner(){
			super(new CollinsPerceptronLearner(1,5),new Recommended.TokenFE());
		}
	}

	/** Uses logistic regression/Maximum entropy to learn a condition
	 * Markov model (CMM), aka "maxent Markov model" (MEMM).
	 *
	 * <p>Reference: Andrew McCallum and Dayne Freitag and Fernando Pereira,
	 * <i>Maximum Entropy Markov Models for Information Extraction and Segmentation</i>,
	 * Proceedings of the International Conference on Machine Learning (ICML-2000).
	 */
	static public class MEMMLearner extends SequenceAnnotatorLearner{

		public MEMMLearner(){
			super(new CMMLearner(new MaxEntLearner(),1),new Recommended.TokenFE());
		}
	}

	/** Uses the voted perceptron algorithm to learn a "conditional
	 * Markov model" (CMM).  This is analogous to an MEMM learner, and
	 * often (surprisingly!) competitive in terms of performance.
	 */
	static public class VPCMMLearner extends SequenceAnnotatorLearner{

		public VPCMMLearner(){
			super(new CMMLearner(new VotedPerceptronLearner(),1),
					new Recommended.TokenFE());
		}
	}

	/** Uses probabilistic SVM to learn a condition Markov model (CMM).
	 * This is analogous to an MEMM learner.
	 */
	static public class SVMCMMLearner extends SequenceAnnotatorLearner{

		public SVMCMMLearner(){
			super(new CMMLearner(new SVMLearner(),1),new Recommended.TokenFE());
		}
	}

	/** Implements the CRF algorithm. Based on the IIT CRF
	 * implementation, in which optimization is performed using the
	 * limited-memory BFGS technique of Liu and Nocedal (following Sha &
	 * Pereira's recommendation.)
	 *
	 *<p>References: John Lafferty and Andrew McCallum and Fernando Pereira,
	 * <i>Conditional Random Fields: Probabilistic Models for Segmenting and Labeling Sequence Data</i>
	 * Proc. 18th International Conf. on Machine Learning, 2001; F. Sha and F. Pereira,
	 * <i>Shallow parsing with conditional random fields</i>, Proceedings of HLT-NAACL,
	 * 2003.
	 */
	static public class CRFAnnotatorLearner extends SequenceAnnotatorLearner{

		public CRFAnnotatorLearner(){
			super(new CRFLearner(),new Recommended.TokenFE(),
					new BeginContinueEndUniqueReduction());
		}
	}

	/** Learns a semi-Markovian extension of CRFs.  Like the
	 * voted-perceptron SMM classes, this allows features to describe
	 * properties of multi-token spans, rather than only properties of
	 * single tokens.
	 *
	 * <p>Reference: Sunita Sarawagi and William W. Cohen,
	 * <i>Semi-Markov Conditional Random Fields for Information Extraction</i>,
	 * in Neural Information Proceedings Systems (NIPS) 2004.
	 */
	static public class SemiCRFAnnotatorLearner extends SegmentAnnotatorLearner{

		/**	 Extracted entities must be of length 4 or less. */
		public SemiCRFAnnotatorLearner(){
			super(new SegmentCRFLearner(""),new MultitokenSpanFE(),4);
		}

		public SemiCRFAnnotatorLearner(int maxIters,int maxLen){
			super(new SegmentCRFLearner("maxIters "+maxIters),new MultitokenSpanFE(),
					maxLen);
		}
	}

	/** a hidden Markov model (HMM), by zkou
	 */
	static public class HMMAnnotatorLearner extends SequenceAnnotatorLearner{

		public HMMAnnotatorLearner(){
			super(new HMMLearner(),new Recommended.HMMTokenFE(),
					new InsideOutsideReduction());
		}
	}

	public static class HMMTokenFE extends TokenPropUsingFE implements
			CommandLineProcessor.Configurable,Serializable{
		
		static final long serialVersionUID=20080517L;
		
		protected boolean useCharType=false;

		protected boolean useCharTypePattern=true;

		//
		// getters and setters, for gui-based configuration
		//
		/** Window size for features. */
		/** If produce features like "token.charTypePattern.Aaaa" for the word "Bill" */
		public void setUseCharType(boolean flag){
			useCharType=flag;
		}

		public boolean getUseCharType(){
			return useCharType;
		}

		/** If true produce features like "token.charTypePattern.Aa+" for the word "Bill". */
		public void setUseCharTypePattern(boolean flag){
			useCharTypePattern=flag;
		}

		public boolean getUseCharTypePattern(){
			return useCharTypePattern;
		}

		//
		// command-line based configuration
		//
		@Override
		public CommandLineProcessor getCLP(){
			return new MyCLP();
		}

		public class MyCLP extends BasicCommandLineProcessor{

			public void charTypes(){
				useCharType=true;
			}

			public void noCharTypes(){
				useCharType=false;
			}

			public void charTypePattern(){
				useCharTypePattern=true;
			}

			public void noCharTypePattern(){
				useCharTypePattern=false;
			}

			public void tokenProps(String s){
				setTokenPropertyFeatures(s);
			}
		}

		//
		// real code (i.e., not configuration code)
		//
		@Override
		public void extractFeatures(TextLabels labels,Span s){
			requireMyAnnotation(labels);
			setMyTokenPropertyList(labels);
			//			from(s).tokens().eq().lc().emit();
			from(s).tokens().eq().emit();
			//			if (useCharTypePattern) from(s).tokens().eq().charTypePattern().emit();
			//			if (useCharType) from(s).tokens().eq().charTypes().emit();
			//			for (int j=0; j<tokenPropertyFeatures.length; j++) {
			//				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			//			}
		}
	}

	//
	// SequenceClassifierLearners
	//

	public static class VPTagLearner extends CollinsPerceptronLearner{

		public VPTagLearner(){
			super(1,5);
		}
	}

	//
	// feature extractors
	//

	/** A simple bag-of-words feature extractor, with words being put in
	 * lower case.
	 */
	abstract public static class TokenPropUsingFE extends SpanFE implements
			Serializable{
		
		static final long serialVersionUID=20081125L;

		protected String[] tokenPropertyFeatures=null;

		/** 
		 * tokenProperties depends on the requiredAnnotation, so override
		 * default setRequiredAnnotation() method to reset the
		 * tokenPropertyFeatures to null when this changes.
		 */
		@Override
		public void setRequiredAnnotation(String requiredAnnotation,
				String annotationProvider){
			super.setRequiredAnnotation(requiredAnnotation,annotationProvider);
			tokenPropertyFeatures=null;
		}

		/** 
		 * Specify the token properties from the TextLabels environment
		 * that will be used as features. A value of '*' or null means to
		 * use all defined token properties.
		 */
		public void setTokenPropertyFeatures(String commaSeparatedTokenPropertyList){
			if("*".equals(commaSeparatedTokenPropertyList))
				tokenPropertyFeatures=null;
			else
				tokenPropertyFeatures=commaSeparatedTokenPropertyList.split(",\\s*");
		}

		public String getTokenPropertyFeatures(){
			return tokenPropertyFeatures==null?"*":StringUtil
					.toString(tokenPropertyFeatures);
		}

		/** Specify the token properties from the TextLabels environment
		 * that will be used as features. */
		public void setTokenPropertyFeatures(Set<String> propertySet){
			tokenPropertyFeatures=
					propertySet.toArray(new String[propertySet.size()]);
		}

		protected void setMyTokenPropertyList(TextLabels labels){
			if(tokenPropertyFeatures==null){
				System.out.println("tokenPropertyFeatures: "+labels.getTokenProperties());
				setTokenPropertyFeatures(labels.getTokenProperties());
			}
		}
	}

	/** A simple bag-of-words feature extractor.
	 */
	public static class DocumentFE extends TokenPropUsingFE implements
			Serializable{

		static final long serialVersionUID=20080517L;
		
		protected boolean foldCase=true;

		@Override
		public void extractFeatures(TextLabels labels,Span s){
			requireMyAnnotation(labels);
			setMyTokenPropertyList(labels);
			if(foldCase)
				from(s).tokens().eq().lc().emit();
			else
				from(s).tokens().eq().emit();
			for(int j=0;j<tokenPropertyFeatures.length;j++){
				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
		}

		public boolean getFoldCase(){
			return foldCase;
		}

		/** If foldCase is true, then words will be converted to lower
		 * case before being used as features. */
		public void setFoldCase(boolean flag){
			foldCase=flag;
		}
	}

	/** An extraction-oriented feature extractor, which should be
	 * applied to one-token spans.  By default this extracts features
	 * for: the lower-case version of the single word inside that span;
	 * lexical properties of the word; and analogous features for tokens
	 * in a small window to either side of the word.
	 */
	public static class TokenFE extends TokenPropUsingFE implements
			CommandLineProcessor.Configurable,Serializable{

		static final long serialVersionUID=20080517L;
		
		protected int windowSize=3;

		protected boolean useCharType=false;

		protected boolean useCharTypePattern=true;

		protected boolean useTokenValues=true;

		//
		// getters and setters, for gui-based configuration
		//
		/** Window size for features. */
		public void setFeatureWindowSize(int n){
			windowSize=n;
		}

		public int getFeatureWindowSize(){
			return windowSize;
		}

		/** If true, produce features like "token.charTypePattern.Aaaa" for the word "Bill" */
		public void setUseCharType(boolean flag){
			useCharType=flag;
		}

		public boolean getUseCharType(){
			return useCharType;
		}

		/** If true, produce features like "token.charTypePattern.Aa+" for the word "Bill". */
		public void setUseCharTypePattern(boolean flag){
			useCharTypePattern=flag;
		}

		public boolean getUseCharTypePattern(){
			return useCharTypePattern;
		}

		/** If true, produce features like "token.lc.bill" for the word "Bill". */
		public void setUseTokenValues(boolean flag){
			useTokenValues=flag;
		}

		public boolean getUseTokenValues(){
			return useTokenValues;
		}

		//
		// command-line based configuration
		//
		@Override
		public CommandLineProcessor getCLP(){
			return new MyCLP();
		}

		public class MyCLP extends BasicCommandLineProcessor{

			public void window(String s){
				windowSize=StringUtil.atoi(s);
				System.out.println("window=>"+s);
			}

			public void charTypes(){
				useCharType=true;
			}

			public void noCharTypes(){
				useCharType=false;
			}

			public void charTypePattern(){
				useCharTypePattern=true;
			}

			public void noCharTypePattern(){
				useCharTypePattern=false;
			}

			public void noTokenValues(){
				useTokenValues=false;
			}

			public void tokenProps(String s){
				setTokenPropertyFeatures(s);
			}
		}

		//
		// real code (i.e., not configuration code)
		//
		@Override
		public void extractFeatures(TextLabels labels,Span s){
			requireMyAnnotation(labels);
			setMyTokenPropertyList(labels);
			if(useTokenValues)
				from(s).tokens().eq().lc().emit();
			if(useCharTypePattern)
				from(s).tokens().eq().charTypePattern().emit();
			if(useCharType)
				from(s).tokens().eq().charTypes().emit();
			for(int j=0;j<tokenPropertyFeatures.length;j++){
				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
			for(int i=0;i<windowSize;i++){
				if(useTokenValues)
					from(s).left().token(-i-1).eq().lc().emit();
				if(useTokenValues)
					from(s).right().token(i).eq().lc().emit();
				for(int j=0;j<tokenPropertyFeatures.length;j++){
					from(s).left().token(-i-1).prop(tokenPropertyFeatures[j]).emit();
					from(s).right().token(i).prop(tokenPropertyFeatures[j]).emit();
				}
				if(useCharTypePattern){
					from(s).left().token(-i-1).eq().charTypePattern().emit();
					from(s).right().token(i).eq().charTypePattern().emit();
				}
				if(useCharType){
					from(s).left().token(-i-1).eq().charTypes().emit();
					from(s).right().token(i).eq().charTypes().emit();
				}
			}
		}
	}

	/** An extraction-oriented feature extractor to apply to multi-token
	 * spans.  By default this extracts features for: the lower-case
	 * version of the phrase inside that span; lexical properties of the
	 * phrase; the length of the span; features for tokens in a small
	 * window to either side of the phrase, analogous to those extracted
	 * by TokenFE; features for the first and last tokens of the phrase,
	 * analogous to those extracted by TokenFE.
	 * 
	 */
	public static class MultitokenSpanFE extends TokenFE implements
			CommandLineProcessor.Configurable,Serializable{

		static final long serialVersionUID=20080517L;
		
		private boolean useFirst=true,useLast=true,useLength=true,useInternal=true;

		//
		// getters/setters for gui configuration
		//

		/** Generate features for the first token of the span. */
		public void setUseFirst(boolean flag){
			useFirst=flag;
		}

		public boolean getUseFirst(){
			return useFirst;
		}

		/** Generate features for the last token of the span. */
		public void setUseLast(boolean flag){
			useLast=flag;
		}

		public boolean getUseLast(){
			return useLast;
		}

		/** Generate features for the length of the span. */
		public void setUseLength(boolean flag){
			useLength=flag;
		}

		public boolean getUseLength(){
			return useLength;
		}

		/** Generate features for the span itself */
		public void setUseInternal(boolean flag){
			useInternal=flag;
		}

		public boolean getUseInternal(){
			return useInternal;
		}

		//
		// command-line configuration
		//
		@Override
		public CommandLineProcessor getCLP(){
			return new JointCommandLineProcessor(new CommandLineProcessor[]{
					super.getCLP(),new MyCLP()});
		}

		public class MyCLP extends BasicCommandLineProcessor{

			public void first(){
				useFirst=true;
			}

			public void noFirst(){
				useFirst=false;
			}

			public void last(){
				useLast=true;
			}

			public void noLast(){
				useLast=false;
			}

			public void length(){
				useLength=true;
			}

			public void noLength(){
				useLength=false;
			}

			public void internal(){
				useInternal=true;
			}

			public void noInternal(){
				useInternal=false;
			}
		}

		//
		// 'real' code
		//
		@Override
		public void extractFeatures(TextLabels labels,Span span){
			super.extractFeatures(labels,span);
			// text of span & its charTypePattern
			if(useInternal){
				from(span).eq().lc().emit();
				if(useCharType)
					from(span).eq().charTypes().emit();
				if(useCharTypePattern)
					from(span).eq().charTypePattern().emit();
			}
			// length properties of span
			if(useLength){
				from(span).size().emit();
				from(span).exactSize().emit();
			}
			// first and last tokens
			if(useFirst)
				from(span).token(0).eq().lc().emit();
			if(useLast)
				from(span).token(-1).eq().lc().emit();
			if(useCharType){
				if(useFirst)
					from(span).token(0).eq().charTypes().lc().emit();
				if(useLast)
					from(span).token(-1).eq().charTypes().lc().emit();
			}
			if(useCharTypePattern){
				if(useFirst)
					from(span).token(0).eq().charTypePattern().lc().emit();
				if(useLast)
					from(span).token(-1).eq().charTypePattern().lc().emit();
			}
			// use marked properties of tokens for first & last tokens in span
			for(int i=0;i<tokenPropertyFeatures.length;i++){
				String p=tokenPropertyFeatures[i];
				// first & last tokens
				if(useFirst)
					from(span).token(0).prop(p).emit();
				if(useLast)
					from(span).token(-1).prop(p).emit();
			}
		}
	}
}
