/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.util.Set;

/**
 * Some sample feature extractors.
 *
 * @author William Cohen
 */
public class SampleFE
{
	/** Simple bag of words feature extractor.
	 */
	public static final SpanFeatureExtractor BAG_OF_WORDS = new BagOfWordsFE();

	public static class BagOfWordsFE implements SpanFeatureExtractor
	{
		public Instance extractInstance(TextLabels labels, Span s)	{
			return extractInstance(s);
		}
		public Instance extractInstance(Span s){
			FeatureBuffer buf = new FeatureBuffer(s);
			SpanFE.from(s, buf).tokens().emit();
			return buf.getInstance();
		}
	}

	/** Simple bag of words feature extractor, with all tokens converted to lower case.
	 */
	public static final SpanFeatureExtractor BAG_OF_LC_WORDS = new BagOfLowerCaseWordsFE();

	public static class BagOfLowerCaseWordsFE implements SpanFeatureExtractor
	{
		public Instance extractInstance(TextLabels labels, Span s)	{
			return extractInstance(s);
		}
		public Instance extractInstance(Span s){
			FeatureBuffer buf = new FeatureBuffer(s);
			SpanFE.from(s, buf).tokens().eq().lc().emit();
			return buf.getInstance();
		}
	}

	/** A simple extraction-oriented feature extractor to apply to one-token spans, for extraction tasks. 
	 */
	public static final SpanFeatureExtractor makeExtractionFE(final int windowSize)
	{
		ExtractionFE fe = new ExtractionFE();
		fe.setWindowSize(windowSize);
		return fe;
	}

	/** A simple extraction-oriented feature extractor to apply to one-token spans, for extraction tasks. 
	 */
	public static class ExtractionFE implements SpanFeatureExtractor
	{
		private int windowSize=3;
		private boolean useCharType=false;
		private boolean useCompressedCharType=true;
		private String[] tokenPropertyFeatures=new String[0];

		public ExtractionFE() { this(3); }
		public ExtractionFE(int windowSize) { this.windowSize=windowSize; }

		//
		// getters and setters
		//

		public int getWindowSize() { return windowSize; }
		public void setWindowSize(int n) { windowSize=n; }

		public boolean getUseCharType() { return useCharType; } 
		public void setUseCharType(boolean flag) { useCharType=flag; } 

		public boolean getUseCompressedCharType() { return useCompressedCharType; } 
		public void setUseCompressedCharType(boolean flag) { useCompressedCharType=flag; } 

		public String getTokenPropertyFeatures() { return StringUtil.toString(tokenPropertyFeatures); }
		public void setTokenPropertyFeatures(String commaSeparatedTokenPropertyList) {
			tokenPropertyFeatures = commaSeparatedTokenPropertyList.split(",\\s*");
			//System.out.println("input: "+commaSeparatedTokenPropertyList);
			//System.out.println("tokenPropertyFeatures: "+StringUtil.toString(tokenPropertyFeatures));
		}
		public void setTokenPropertyFeatures(Set propertySet) {
			tokenPropertyFeatures = (String[])propertySet.toArray(new String[propertySet.size()]);
		}

		public Instance extractInstance(Span s)
		{
			return extractInstance(new EmptyLabels(), s);
		}
		public Instance extractInstance(TextLabels labels, Span s)
		{
			//System.out.println("tokenPropertyFeatures: "+StringUtil.toString(tokenPropertyFeatures));
			FeatureBuffer buf = new FeatureBuffer(labels,s);
			SpanFE.from(s,buf).tokens().eq().lc().emit();
			if (useCompressedCharType) {
				SpanFE.from(s,buf).tokens().eq().tr("[A-Z]+","X").tr("[a-z]+","x").tr("[0-9]+","9").emit();
			}
			if (useCharType) {
				SpanFE.from(s,buf).tokens().eq().tr("[A-Z]","A").tr("[a-z]","a").tr("[0-9]","0").emit();
			}
			for (int j=0; j<tokenPropertyFeatures.length; j++) {
				SpanFE.from(s,buf).tokens().prop(tokenPropertyFeatures[j]).emit();
			}
			for (int i=0; i<windowSize; i++) {
				SpanFE.from(s,buf).left().token(-i-1).eq().lc().emit();
				SpanFE.from(s,buf).right().token(i).eq().lc().emit();
				for (int j=0; j<tokenPropertyFeatures.length; j++) {
					SpanFE.from(s,buf).left().token(-i-1).prop(tokenPropertyFeatures[j]).emit();
				}
				for (int j=0; j<tokenPropertyFeatures.length; j++) {
					SpanFE.from(s,buf).right().token(i).prop(tokenPropertyFeatures[j]).emit();
				}
				if (useCompressedCharType) {
					SpanFE.from(s,buf).left().token(-i-1).eq().tr("[A-Z]+","X").tr("[a-z]+","x").tr("[0-9]+","9").emit();
				}
				if (useCharType) {
					SpanFE.from(s,buf).right().token(i).eq().tr("[A-Z]","A").tr("[a-z]","a").tr("[0-9]","0").emit();
				}
			}
			return buf.getInstance();
		}
	}

	/** Test case to try out the feature extractors
	 */
	public static void main(String[] args)
	{
		try {
			SpanFeatureExtractor fe = BAG_OF_LC_WORDS;
			TextBase base = new BasicTextBase();
			for (int i=0; i<SampleDatasets.posTrain.length; i++) {
				base.loadDocument("pos"+i, SampleDatasets.posTrain[i]);
			}
			for (int i=0; i<SampleDatasets.negTrain.length; i++) {
				base.loadDocument("neg"+i, SampleDatasets.negTrain[i]);
			}
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				String id = s.getDocumentId();
				ClassLabel label = ClassLabel.binaryLabel( id.startsWith("pos") ? +1 : -1 );
				dataset.add(new BinaryExample(fe.extractInstance(s), label));
			}
			ViewerFrame f = new ViewerFrame("Toy data", dataset.toGUI());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
