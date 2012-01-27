package edu.cmu.minorthird.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.util.BasicCommandLineProcessor;

/** 
 * Compares two sets of spans.
 *
 * @author William Cohen
 */

public class SpanDifference{

	static public Logger log=Logger.getLogger(SpanDifference.class);

	/** Max value of an status indicator, eg FALSE_POS, FALSE_NEG, etc */
	static public final int MAX_STATUS=4;

	/** Indicates a false positive span. Specificially, indicates part of
	 * document inside a 'guess' span, but not inside a 'truth' span,
	 * where the set of truth spans for this area is known to be
	 * complete. */
	static public final int FALSE_POS=1;

	/** Indicates a false negative span. Specificially, indicates part of
	 * document inside a truth span but not inside a guess span. */
	static public final int FALSE_NEG=2;

	/** Indicates a true positive negative span. Specificially, indicates
	 * part of document inside a truth span and also inside a guess
	 * span. */
	static public final int TRUE_POS=3;

	/** Indicates something inside a guess span which may or may not
	 * be inside a truth span. */
	static public final int UNKNOWN_POS=4;

	static private final int UNMARKED=0;

	static private final int LEFT=1;

	static private final int RIGHT=2;

	static private final int GUESS=1;

	static private final int TRUTH=2;

	static private final int CLOSURE=3;

	// for debugging, map guess, truth, closure to strings
	static private final String[] strCode=new String[]{"?","G","T","C"};

	// caches differences
	private List<DiffedSpan> diffedSpans=null;

	private SortedSet<Span> guessSet;

	private SortedSet<Span> truthSet;

	private SortedMap<String,Set<Span>> closureMap;

	// caches performance measures
	double tokenFalsePos;

	double tokenFalseNeg;

	double tokenTruePos;

	double spanFalsePos;

	double spanFalseNeg;

	double spanTruePos;

	// indicates if tokenFalseNeg,etc are valid
	boolean performanceCacheIsValid;

	/**
	 * Create an aggregation of the results in several SpanDifference's.
	 */
	public SpanDifference(SpanDifference[] spanDifferences){
		SortedSet<DiffedSpan> accum=new TreeSet<DiffedSpan>();
		tokenFalsePos=
				tokenFalseNeg=tokenTruePos=spanFalsePos=spanFalseNeg=spanTruePos=0;
		for(int i=0;i<spanDifferences.length;i++){
			SpanDifference sd=spanDifferences[i];
			for(Iterator<DiffedSpan> j=sd.diffedSpans.iterator();j.hasNext();){
				accum.add(j.next());
			}
			tokenFalsePos+=sd.tokenFalsePos;
			tokenFalseNeg+=sd.tokenFalseNeg;
			tokenTruePos+=sd.tokenTruePos;
			spanFalsePos+=sd.spanFalsePos;
			spanFalseNeg+=sd.spanFalseNeg;
			spanTruePos+=sd.spanTruePos;
		}
		diffedSpans=new ArrayList<DiffedSpan>(accum.size());
		for(Iterator<DiffedSpan> i=accum.iterator();i.hasNext();){
			diffedSpans.add(i.next());
		}
		// make sure we don't recompute this!
		performanceCacheIsValid=true;
	}

	/** Create machinery to analyze the differences between the two sets
	 * of spans.  It is assume that the first argument is a complete
	 * list of all guess spans and the second argument is a complete
	 * list of all truth spans. 
	 */
	public SpanDifference(Iterator<Span> guess,Iterator<Span> truth){
		this(guess,truth,null);
	}

	/** Create machinery to analyze the differences between the two sets
	 * of spans.  It is assumed that the first argument is a complete
	 * list of all guess spans, the second argument is a partial list of
	 * all truth spans, and the third argument is the set of spans S
	 * for which all truth spans contained by S are known. 
	 */
	public SpanDifference(Iterator<Span> guess,Iterator<Span> truth,Iterator<Span> closures){
		guessSet=new TreeSet<Span>();
		truthSet=new TreeSet<Span>();
		closureMap=new TreeMap<String,Set<Span>>();

		// load up all the boundaries into a sorted set
		SortedSet<ChangeBoundary> changes=new TreeSet<ChangeBoundary>();
		//System.out.println("adding boundaries...");
		while(guess.hasNext()){
			Span s=guess.next();
			changes.add(new ChangeBoundary(s.getLeftBoundary(),LEFT,GUESS,s));
			changes.add(new ChangeBoundary(s.getRightBoundary(),RIGHT,GUESS,s));
			guessSet.add(s);
		}
		while(truth.hasNext()){
			Span s=truth.next();
			changes.add(new ChangeBoundary(s.getLeftBoundary(),LEFT,TRUTH,null));
			changes.add(new ChangeBoundary(s.getRightBoundary(),RIGHT,TRUTH,null));
			truthSet.add(s);
		}
		while(closures!=null&&closures.hasNext()){
			Span s=closures.next();
			changes.add(new ChangeBoundary(s.getLeftBoundary(),LEFT,CLOSURE,null));
			changes.add(new ChangeBoundary(s.getRightBoundary(),RIGHT,CLOSURE,null));
			Set<Span> closuresForId=closureMap.get(s.getDocumentId());
			if(closuresForId==null)
				closureMap.put(s.getDocumentId(),closuresForId=new TreeSet<Span>());
			closuresForId.add(s);
		}

		// go thru the boundaries and create a list of differences
		//System.out.println("creating differences...");
		diffedSpans=new ArrayList<DiffedSpan>();
		performanceCacheIsValid=false;
		int state=UNMARKED;
		// if there is an explicit list of things which are 'closed', then
		// use it, and otherwise assume that the closed world assumption holds
		// everywhere.
		boolean insideClosure=closures==null;
		ChangeBoundary fpLeft=null,tpLeft=null,fnLeft=null;
		for(Iterator<ChangeBoundary> i=changes.iterator();i.hasNext();){
			ChangeBoundary cb=i.next();
			//System.out.println("state = "+state+" cb = "+cb);
			if(cb.guessTruthClosure==CLOSURE)
				insideClosure=cb.isLeft;
			else if(state==UNMARKED&&cb.isLeft&&cb.guessTruthClosure==TRUTH){
				state=FALSE_NEG; // truth starts, start false neg
				fnLeft=cb;
			}else if(state==UNMARKED&&cb.isLeft&&cb.guessTruthClosure==GUESS){
				state=FALSE_POS; // guess starts, start false pos
				fpLeft=cb;
			}else if(state==FALSE_POS&&cb.isLeft&&cb.guessTruthClosure==TRUTH){
				state=TRUE_POS; // truth starts, false pos -> true pos
				if(cb.point.compareTo(fpLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,FALSE_POS,fpLeft,cb));
				tpLeft=cb;
			}else if(state==FALSE_POS&&!cb.isLeft&&cb.guessTruthClosure==GUESS){
				state=UNMARKED; // guess ends, end false pos
				if(cb.point.compareTo(fpLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,FALSE_POS,fpLeft,cb));
				fpLeft=null;
			}else if(state==FALSE_NEG&&cb.isLeft&&cb.guessTruthClosure==GUESS){
				state=TRUE_POS; // guess starts, false neg -> true pos
				if(cb.point.compareTo(fnLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,FALSE_NEG,fnLeft,cb));
				tpLeft=cb;
			}else if(state==FALSE_NEG&&!cb.isLeft&&cb.guessTruthClosure==TRUTH){
				state=UNMARKED; // truth ends, end false neg 
				if(cb.point.compareTo(fnLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,FALSE_NEG,fnLeft,cb));
				fnLeft=null;
			}else if(state==TRUE_POS&&!cb.isLeft&&cb.guessTruthClosure==TRUTH){
				state=FALSE_POS; // truth ends, true pos->false pos
				if(cb.point.compareTo(tpLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,TRUE_POS,tpLeft,cb));
				fpLeft=cb;
			}else if(state==TRUE_POS&&!cb.isLeft&&cb.guessTruthClosure==GUESS){
				state=FALSE_NEG; // guess ends, true pos->false neg
				if(cb.point.compareTo(tpLeft.point)!=0)
					diffedSpans.add(new DiffedSpan(insideClosure,TRUE_POS,tpLeft,cb));
				fnLeft=cb;
			}
		}
		//System.out.println("span diff complete");
		//for (Iterator i=diffedSpans.iterator(); i.hasNext(); ) System.out.println(i.next());
	}

	public Looper differenceIterator(){
		return new Looper(diffedSpans);
	}

	/** Return the percentage of tokens in 'guess' spans that are true
	 * positives (ignoring tokens that are UNKNOWN_POS). */
	public double tokenPrecision(){
		if(!performanceCacheIsValid)
			cachePerformance();
		if(tokenTruePos+tokenFalsePos==0)
			return 0.0;
		else
			return tokenTruePos/(tokenTruePos+tokenFalsePos);
	}

	/** Return the percentage of tokens in true positive spans that are in guess
	 * spans (ignoring tokens that are UNKNOWN_POS). */
	public double tokenRecall(){
		if(!performanceCacheIsValid)
			cachePerformance();
		if(tokenTruePos+tokenFalseNeg==0)
			return 0.0;
		else
			return tokenTruePos/(tokenTruePos+tokenFalseNeg);
	}

	/** Return the percentage of 'guess' spans that are also 'truth'
	 * spans, ignoring non-truth spans that are not inside closure spans. */
	public double spanPrecision(){
		if(!performanceCacheIsValid)
			cachePerformance();
		if(spanTruePos+spanFalsePos==0)
			return 0.0;
		else
			return spanTruePos/(spanTruePos+spanFalsePos);
	}

	/** Return the percentage of 'truth' spans that are also 'guess'
	 * spans */
	public double spanRecall(){
		if(!performanceCacheIsValid)
			cachePerformance();
		if(spanTruePos+spanFalseNeg==0)
			return 0.0;
		else
			return spanTruePos/(spanTruePos+spanFalseNeg);
	}

	private void cachePerformance(){
		tokenFalsePos=tokenFalseNeg=tokenTruePos=0;
		for(Iterator<DiffedSpan> i=diffedSpans.iterator();i.hasNext();){
			DiffedSpan s=i.next();
			int numTokens=s.diffSpan.size();
			int status=s.status;
			if(status==FALSE_POS)
				tokenFalsePos+=numTokens;
			else if(status==FALSE_NEG)
				tokenFalseNeg+=numTokens;
			else if(status==TRUE_POS)
				tokenTruePos+=numTokens;
		}
		spanFalsePos=spanFalseNeg=spanTruePos=0;
		//System.out.println("guessSet: "+guessSet);
		//System.out.println("truthSet: "+truthSet);
		for(Iterator<Span> i=truthSet.iterator();i.hasNext();){
			Span s=i.next();
			if(!guessSet.contains(s)){
				spanFalseNeg++;
				log.debug("fn: "+s);
			}
		}
		for(Iterator<Span> i=guessSet.iterator();i.hasNext();){
			Span s=i.next();
			if(truthSet.contains(s)){
				spanTruePos++;
				log.debug("tp: "+s);
			}else{
				Set<Span> closuresForId=closureMap.get(s.getDocumentId());
				if(closuresForId!=null){
					for(Iterator<Span> j=closuresForId.iterator();j.hasNext();){
						Span t=j.next();
						if(t.contains(s)){
							log.debug("fp: "+s);
							spanFalsePos++;
							break;
						}
					}
				}//closuresForId!=null
			}//guess not in truthSet
		}// for guess
		performanceCacheIsValid=true;
	}

	/** A Span.Looper which also passes out two additional types
	 * of information about each returned span s:
	 * <ol>
	 * <li>if s is a FALSE_POS, FALSE_NEG, or TRUE_POS,
	 * relative to the original spans.
	 * <li>the true span and/or guess spans associated with s.
	 *</ol> 
	 */
	public static class Looper implements Iterator<Span>{

		private DiffedSpan last;

		private Iterator<DiffedSpan> i;

		private int estSize=-1;

		public Looper(Collection<DiffedSpan> c){
			this.i=c.iterator();
			estSize=c.size();
		}

		public Looper(Iterator<DiffedSpan> i){
			this.i=i;
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public boolean hasNext(){
			return i.hasNext();
		}

		@Override
		public Span next(){
			last=i.next();
			return last.diffSpan;
		}

		/** Return status of the last span returned - does it indicate a false positive,
		 * false negative, or true positive area? */
		public int getStatus(){
			return last.status;
		}

		/** Guess span that support this difference. */
		public Span getOriginalGuessSpan(){
			return last.originalGuessSpan;
		}

		public int estimatedSize(){
			return estSize;
		}
	}

	/** Indicates a point at which the document changes from
	 * being false positive, false negative, true positive,
	 * or neither. */
	private static class ChangeBoundary implements Comparable<ChangeBoundary>{

		public Span point; // empty span

		public boolean isLeft;

		public int guessTruthClosure;

		public Span originalGuessSpan;

		public ChangeBoundary(Span point,int leftRight,int guessTruthClosure,
				Span originalGuessSpan){
			this.point=point;
			this.isLeft=leftRight==LEFT;
			this.guessTruthClosure=guessTruthClosure;
			this.originalGuessSpan=originalGuessSpan;
		}

		@Override
		public String toString(){
			return "["+point.toString()+";"+(isLeft?"L":"R")+";"+
					strCode[guessTruthClosure]+"]";
		}

		@Override
		public int compareTo(ChangeBoundary cb){
			int tmp=point.compareTo(cb.point);
			if(tmp!=0)
				return tmp;
			if(originalGuessSpan!=null&&cb.originalGuessSpan==null)
				return -1;
			if(originalGuessSpan==null&&cb.originalGuessSpan!=null)
				return +1;
			if(!isLeft&&cb.isLeft)
				return -1;
			if(isLeft&&!cb.isLeft)
				return +1;
			return guessTruthClosure-cb.guessTruthClosure;
		}
	}

	/** A difference between the guess and truth spans. */
	private static class DiffedSpan implements Comparable<DiffedSpan>{

		private Span diffSpan;

		private int status;

		private Span originalGuessSpan;

		public DiffedSpan(boolean insideClosure,int statusCWA,
				ChangeBoundary leftBoundary,ChangeBoundary rightBoundary){
			if(!insideClosure&statusCWA==FALSE_POS)
				this.status=UNKNOWN_POS;
			else
				status=statusCWA;
			if(!leftBoundary.point.getDocumentId().equals(
					rightBoundary.point.getDocumentId())){
				throw new IllegalArgumentException("error diffing "+leftBoundary.point+
						" to "+rightBoundary.point);
			}
			int lo=leftBoundary.point.documentSpanStartIndex();
			int len=rightBoundary.point.documentSpanStartIndex()-lo;
			diffSpan=leftBoundary.point.documentSpan().subSpan(lo,len);
			originalGuessSpan=leftBoundary.originalGuessSpan;
			if(originalGuessSpan==null)
				originalGuessSpan=rightBoundary.originalGuessSpan;
		}

		@Override
		public String toString(){
			return "[Diff "+status+" "+diffSpan+"]";
		}

		@Override
		public int compareTo(DiffedSpan o){
			return diffSpan.compareTo(o.diffSpan);
		}
	}

	@Override
	public String toString(){
		return "[SpanDiff: token p/r="+tokenPrecision()+"/"+tokenRecall()+
				" span p/r="+spanPrecision()+"/"+spanRecall()+"]";
	}

	/** Return a string containing all the summary statistics
	 * printed moderately neatly on two lines. */

	public String toSummary(){
		double tokenF=
				2*tokenPrecision()*tokenRecall()/(tokenPrecision()+tokenRecall());
		double spanF=2*spanPrecision()*spanRecall()/(spanPrecision()+spanRecall());
		return "TokenPrecision: "+fmt(tokenPrecision())+" TokenRecall: "+
				fmt(tokenRecall())+" F: "+fmt(tokenF)+"\n"+"SpanPrecision:  "+
				fmt(spanPrecision())+" SpanRecall:  "+fmt(spanRecall())+" F: "+
				fmt(spanF);
		//+"\n"+"Token TP,FP,FN: "+tokenTruePos+","+tokenFalsePos+","+tokenFalseNeg
		//+"\n"+"Span  TP,FP,FN: "+spanTruePos+","+spanFalsePos+","+spanFalseNeg;
	}

	private String fmt(double d){
		if(Double.isNaN(d))
			return fmt(0);
		else
			return new java.text.DecimalFormat("0.0000").format(d);
	}

	static public class Invoker extends BasicCommandLineProcessor{

		public TextLabels textLabels;

		public String predictedType="_predicted",actualType=null;

		public void labels(String s){
			textLabels=FancyLoader.loadTextLabels(s);
		}

		public void predicted(String s){
			predictedType=s;
		}

		public void actual(String s){
			actualType=s;
		}
	}

	public static void main(String[] args){
		Invoker inv=new Invoker();
		inv.processArguments(args);
		if(inv.textLabels==null)
			throw new IllegalArgumentException("-labels must be set");
		if(inv.actualType==null)
			throw new IllegalArgumentException("-actual must be set");
		Iterator<Span> guess=inv.textLabels.instanceIterator(inv.predictedType);
		Iterator<Span> truth=inv.textLabels.instanceIterator(inv.actualType);
		Iterator<Span> closure=inv.textLabels.closureIterator(inv.actualType);
		if(guess==null)
			throw new IllegalArgumentException("spanType '"+inv.predictedType+
					"' not found");
		if(truth==null)
			throw new IllegalArgumentException("spanType '"+inv.actualType+
					"' not found");
		SpanDifference sd=new SpanDifference(guess,truth,closure);
		System.out.println(sd.toString());
		System.out.println(sd.toSummary());
	}
}
