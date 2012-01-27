/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.EmptyLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Some sample feature extractors.
 * 
 * @author William Cohen
 */
public class SampleFE{

	/**
	 * Simple bag of words feature extractor.
	 */
	public static final AnnotatedSpanFE BAG_OF_WORDS=new BagOfWordsFE();

	public static class BagOfWordsFE extends AnnotatedSpanFE implements
	Serializable{
		static final long serialVersionUID=20080306L;
		@Override
		public void extractFeatures(TextLabels labels,Span s){
			from(s).tokens().emit();
		}
	}

	/**
	 * Simple bag of words feature extractor, with all tokens converted to lower
	 * case.
	 */
	public static final AnnotatedSpanFE BAG_OF_LC_WORDS=
		new BagOfLowerCaseWordsFE();

	public static class BagOfLowerCaseWordsFE extends AnnotatedSpanFE implements
	Serializable{
		static final long serialVersionUID=20080306L;
		@Override
		public void extractFeatures(TextLabels labels,Span s){
			from(s).tokens().eq().lc().emit();
		}
	}

	/**
	 * A simple extraction-oriented feature extractor to apply to one-token spans,
	 * for extraction tasks.
	 */
	public static final AnnotatedSpanFE makeExtractionFE(
			final int featureWindowSize){
		ExtractionFE fe=new ExtractionFE();
		fe.setFeatureWindowSize(featureWindowSize);
		return fe;
	}

	/**
	 * An extraction-oriented feature extractor to apply to one-token spans, for
	 * extraction tasks.
	 */
	public static class ExtractionFE extends AnnotatedSpanFE{

		static final long serialVersionUID=20080306L;

		protected int windowSize=5;

		protected boolean useCharType=true;

		protected boolean useCompressedCharType=true;

		protected String[] tokenPropertyFeatures=new String[0];

		public ExtractionFE(){
			this(3);
		}

		public ExtractionFE(int windowSize){
			this.windowSize=windowSize;
		}

		//
		// getters and setters
		//

		/**
		 * Specify the number of tokens on before and after the span to emit
		 * features for.
		 */
		public void setFeatureWindowSize(int n){
			windowSize=n;
		}

		public int getFeatureWindowSize(){
			return windowSize;
		}

		/**
		 * If set to true, produce features like "token.charTypePattern.Aaaa" for
		 * the word "Bill"
		 */
		public void setUseCharType(boolean flag){
			useCharType=flag;
		}

		public boolean getUseCharType(){
			return useCharType;
		}

		/**
		 * If set to true, produce features like "token.charTypePattern.Aa+" for the
		 * word "Bill".
		 */
		public void setUseCompressedCharType(boolean flag){
			useCompressedCharType=flag;
		}

		public boolean getUseCompressedCharType(){
			return useCompressedCharType;
		}

		/**
		 * Specify the token properties from the TextLabels environment that will be
		 * used as features. A value of '*' means to use all defined token
		 * properties.
		 */
		public void setTokenPropertyFeatures(String commaSeparatedTokenPropertyList){
			if("*".equals(commaSeparatedTokenPropertyList)){
				// System.out.println("setting properties to null");
				tokenPropertyFeatures=null;
			}else{
				tokenPropertyFeatures=commaSeparatedTokenPropertyList.split(",\\s*");
			}
		}

		public String getTokenPropertyFeatures(){
			return StringUtil.toString(tokenPropertyFeatures);
		}

		public void setTokenPropertyFeatures(Set<String> propertySet){
			tokenPropertyFeatures=
				propertySet.toArray(new String[propertySet.size()]);
		}

		@Override
		public void extractFeatures(Span s){
			extractFeatures(new EmptyLabels(),s);
		}

		@Override
		public void extractFeatures(TextLabels labels,Span s){
			requireMyAnnotation(labels);

			if(tokenPropertyFeatures==null){
				System.out.println("setTokenPropertyFeatures to the set "+
						labels.getTokenProperties());
				setTokenPropertyFeatures(labels.getTokenProperties());
			}

			// tokens in span
			from(s).tokens().eq().lc().emit();
			// simplified capitalization pattern
			if(useCompressedCharType){
				from(s).tokens().eq().charTypePattern().emit();
			}
			// exact capitalization pattern
			if(useCharType){
				from(s).tokens().eq().charTypes().emit();
			}
			// token properties
			for(int j=0;j<tokenPropertyFeatures.length;j++){
				from(s).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
			// window
			for(int i=0;i<windowSize;i++){
				from(s).left().token(-i-1).eq().lc().emit();
				from(s).right().token(i).eq().lc().emit();
				for(int j=0;j<tokenPropertyFeatures.length;j++){
					// System.out.println("Property: "+tokenPropertyFeatures[j]);
					from(s).left().token(-i-1).prop(tokenPropertyFeatures[j]).emit();
					from(s).right().token(i).prop(tokenPropertyFeatures[j]).emit();
				}
				if(useCompressedCharType){
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

	/**
	 * A feature extractor that pre-loads a mixup file or some other type of
	 * annotation.
	 */
	public static abstract class AnnotatedSpanFE extends SpanFE{
		
		static final long serialVersionUID=20081125L;
		
	}

	/**
	 * Test case to try out the feature extractors
	 */
	public static void main(String[] args){
		try{
			SpanFeatureExtractor fe=BAG_OF_LC_WORDS;
			BasicTextBase base=new BasicTextBase();
			for(int i=0;i<SampleDatasets.posTrain.length;i++){
				base.loadDocument("pos"+i,SampleDatasets.posTrain[i]);
			}
			for(int i=0;i<SampleDatasets.negTrain.length;i++){
				base.loadDocument("neg"+i,SampleDatasets.negTrain[i]);
			}
			Dataset dataset=new BasicDataset();
			for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
				Span s=i.next();
				String id=s.getDocumentId();
				ClassLabel label=ClassLabel.binaryLabel(id.startsWith("pos")?+1:-1);
				TextLabels textLabels=new EmptyLabels();
				dataset.add(new Example(fe.extractInstance(textLabels,s),label));
			}
			new ViewerFrame("Toy data",dataset.toGUI());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
