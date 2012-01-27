package edu.cmu.minorthird.text.learn;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.Details;
import edu.cmu.minorthird.text.MixupFinder;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanFinder;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Annotator based on classifiers for start, labels, and length.
 * 
 * @author William Cohen
 */

public class StartEndLengthAnnotator extends AbstractAnnotator implements
		ExtractorAnnotator{

	private double threshold=0.5;

	// finds single-token spans
	static private final SpanFinder tokenFinder;
	static{
		try{
			tokenFinder=new MixupFinder(new Mixup("...[any]..."));
		}catch(Mixup.ParseException e){
			throw new IllegalStateException("illegal tokenFinder");
		}
	}

	// scores lengths
	private LengthScorer lengthScorer;

	private SpanFeatureExtractor fe;

	private BinaryClassifier start,end;

	private String annotationType;

	/**
	 * Create an annotator.
	 */
	public StartEndLengthAnnotator(BinaryClassifier start,BinaryClassifier end,
			SpanFeatureExtractor fe,Map<Integer,Integer> lengthMap,int totalPosSpans,
			String annotationType){
		this.fe=fe;
		this.start=start;
		this.end=end;
		lengthScorer=new LengthScorer(lengthMap,totalPosSpans);
		this.annotationType=annotationType;
	}

	public void setThreshold(double t){
		this.threshold=t;
	}

	public double getThreshold(){
		return threshold;
	}

	@Override
	public String getSpanType(){
		return annotationType;
	}

	/** Return something that finds beginnings (for debugging). */
	public SpanFinder getStartFinder(){
		return new FilteredFinder(start,fe,tokenFinder);
	}

	/** Return something that finds ends (for debugging). */
	public SpanFinder getEndFinder(){
		return new FilteredFinder(end,fe,tokenFinder);
	}

	@Override
	protected void doAnnotate(MonotonicTextLabels labels){
		Iterator<Span> i=labels.getTextBase().documentSpanIterator();
		ProgressCounter pc=new ProgressCounter("annotate","document");
		while(i.hasNext()){
			Span document=i.next();
			// look for all start and end tokens
			double[] startPred=new double[document.size()];
			double[] endPred=new double[document.size()];
			for(int j=0;j<document.size();j++){
				Span tokenSpan=document.subSpan(j,1);
				Instance instance=fe.extractInstance(labels,tokenSpan);
				startPred[j]=start.score(instance);
				endPred[j]=end.score(instance);
				// System.out.println(document.getDocumentId()+" "+tokenSpan+" "+j+"
				// start:"+startPred[j]+" end: "+endPred[j]);
			}
			// look for nearby start-end pairs, score them
			for(int j=0;j<=document.size()-1;j++){
				double startScore=startPred[j];
				if(startScore<threshold)
					continue;
				// System.out.println("possible start "+j+" ["+startScore+"]
				// "+document.subSpan(0,j+1));
				for(int len=1;j+len<=document.size()&&len<=lengthScorer.maxLength();len++){
					double lengthScore=lengthScorer.score(len);
					if(lengthScore+startScore<threshold)
						continue;
					// System.out.println("possible length ["+lengthScore+"] "+len);
					double endScore=endPred[j+len-1];
					// System.out.println("possible end "+(j+len-1)+" ["+endScore+"]
					// "+document.subSpan(0,j+len));
					double finalScore=startScore+lengthScore+endScore;
					// show something
					String lContext=
							document.subSpan(Math.max(0,j-5),Math.min(5,j-Math.max(0,j-5)))
									.asString();
					String rContext=
							document.subSpan(j+len,Math.min(5,document.size()-j-len))
									.asString();
					String cContext=document.subSpan(j,len).asString();
					// System.out.println("possible start ["+startScore+"]
					// "+document.subSpan(0,j+1));
					// System.out.println("possible end ["+endScore+"]
					// "+document.subSpan(0,j+len));
					// System.out.println("possible length ["+lengthScore+"] "+len);
					if(finalScore>threshold){
						System.out.println("output ["+finalScore+"] "+lContext+"|"+
								cContext+"|"+rContext);
						// put a high-scoring combination in the labels
						Map<String,Double> m=new TreeMap<String,Double>();
						m.put("start",new Double(startPred[j]));
						m.put("end",new Double(endPred[j+len-1]));
						m.put("length",new Double(lengthScore));
						labels.addToType(document.subSpan(j,len),annotationType,
								new Details(finalScore,m));
					}
				}
			}
			pc.progress();
		}
		pc.finished();
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return "not implemented";
	}

	@Override
	public String toString(){
		return "[StartEndLen: "+start+";"+end+";"+lengthScorer+"]";
	}

	/**
	 * Scores lengths using a smoothed histogram.
	 */
	private static class LengthScorer{

		private Map<Integer,Integer> lengthFreqMap;

		private int numLengths;

		private double mixingFactor=0.1;

		private int maxLength=0;

		public LengthScorer(Map<Integer,Integer> lengthFreqMap,int totalPosSpans){
			this.lengthFreqMap=lengthFreqMap;
			this.numLengths=totalPosSpans;
			for(Iterator<Integer> i=lengthFreqMap.keySet().iterator();i.hasNext();){
				int len=i.next().intValue();
				maxLength=Math.max(maxLength,len);
			}
		}

		public int maxLength(){
			return maxLength;
		}

		/** Return Prob(len) */
		public double score(int len){
			Integer freq=lengthFreqMap.get(new Integer(len));
			double empiricalProb=freq==null?0:((double)freq.intValue())/numLengths;
			double smoothedProb=
					(mixingFactor/maxLength)+(1-mixingFactor)*empiricalProb;
			double odds=Math.log(smoothedProb/(1.0-smoothedProb));
			// System.out.println("odds of len="+len+": "+odds);
			return odds;
		}

		@Override
		public String toString(){
			return "[LengthScorer: "+maxLength+";"+lengthFreqMap+"]";
		}
	}
}
