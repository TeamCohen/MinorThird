package edu.cmu.minorthird.text.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.text.AnnotatorLoader;
import edu.cmu.minorthird.text.EmptyLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.StopWords;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.Token;

/**
 * A Feature Extractor which converts a Span to an Instance.
 * 
 * <p>
 * Typical use of this would be something like the following: <code><pre>
 * SpanFE fe=new SpanFE(labels){
 * 
 * 	public void extractFeatures(Span span){
 * 		from(span).tokens().emit();
 * 		from(span).left().subSpan(-2,2).emit();
 * 		from(span).right().subSpan(0,2).emit();
 * 		from(span).right().contains(&quot;obj&quot;).emit();
 * 	}
 * };
 * 
 * Instance inst=fe.extractInstance(span);
 * </pre></code> Generally, to use this class, one subclasses it and implements the
 * extractFeatures method, using a chain of feature-extracting actions which
 * starts with 'from' and ends with 'emit'.
 * <p>
 * The methods tokens(), subSpan(), and so on are defined in subclasses of
 * SpanFE.Result, and are summarized here.
 * <ul>
 * <li> result.trace() - prints some stuff to stdout by called
 * SpanFE.trace(result). SpanFE.trace can be overloaded for different behavior.
 * <li> result.emit() - ends a feature extraction pipeline by calling
 * SpanFE(result), which can be overloaded.
 * <li> result.left() - if result contains a single span, find the left context
 * of that span (a span containing all tokens before it).
 * <li> result.right() - if result contains a single span, find the right
 * context of that span (all tokens after it).
 * <li> result.contains(String type) - if result contains a single span, find
 * the set of all spans of given type contained by that span.
 * <li> result.subSpan(int lo,int len) - if result contains a single span, find
 * the appropriate subspan of that span.
 * <li> result.tokens() - if result contains a single span, find the set of all
 * tokens contained in that span (a 'bag of words'. Extends to a set of spans as
 * well.
 * <li> result.token(int i) - if result contains a single span, construct the
 * set containing the i-th token only.
 * <li> result.first(), result.last() - return the first/last element of a set
 * of Spans.
 * <li> result.eq() - for a set of tokens, construct a set of features of the
 * form 'x y z eq v' where v is the value of the token and 'x y z' is the path
 * of feature extraction steps needed to get to set of tokens.
 * </ul>
 * 
 * 
 * @author William Cohen
 */

abstract public class SpanFE implements SpanFeatureExtractor,MixupCompatible,
		Serializable{

	// for serialization
	static private final long serialVersionUID=20080306L;

	/**
	 * Store features as binary, whenever possible, even if occurence counts are
	 * ignored.
	 */
	static public final int STORE_AS_BINARY=1;

	/** Store features as numeric counts, whenever possible */
	static public final int STORE_AS_COUNTS=2;

	/**
	 * Store features as binary or counts, trying to reduce storage while
	 * maintaining information.
	 */
	static public final int STORE_COMPACTLY=3;

	private int featureStoragePolicy=STORE_AS_COUNTS;

	// buffers for intermediate results & inputs in feature extraction
	transient protected MutableInstance instance;

	transient private TextLabels textLabels=new EmptyLabels();

	protected String requiredAnnotation=null;

	protected String requiredAnnotationFileToLoad=null;

	protected AnnotatorLoader annotatorLoader=null;

	/** Create a feature extractor */
	public SpanFE(){
	}

	//
	// getters and setters
	// 

	/**
	 * Set the policy for creating features.
	 * 
	 * @param p
	 *          should be one of SpanFE.STORE_AS_BINARY, SpanFE.STORE_AS_COUNTS,
	 *          SpanFE.STORE_COMPACTLY
	 */
	public void setFeatureStoragePolicy(int p){
		this.featureStoragePolicy=p;
	}

	/**
	 * Simultaneously specify an annotator to run before feature generation and a
	 * mixup file or class that generates it.
	 */
	public void setRequiredAnnotation(String requiredAnnotation,
			String annotationProvider){
		setRequiredAnnotation(requiredAnnotation);
		setAnnotationProvider(annotationProvider);
	}

	//
	// simpler getter-setter interface, e.g. for GUI configuration
	//

	/** Specify an annotator to run before feature generation. */
	@Override
	public void setRequiredAnnotation(String requiredAnnotation){
		this.requiredAnnotation=requiredAnnotation;
	}

	@Override
	public String getRequiredAnnotation(){
		return requiredAnnotation==null?"":requiredAnnotation;
	}

	/**
	 * Specify a mixup file or java class to use to provide the annotation.
	 */
	public void setAnnotationProvider(String classNameOrMixupFileName){
		this.requiredAnnotationFileToLoad=classNameOrMixupFileName;
	}

	public String getAnnotationProvider(){
		return requiredAnnotationFileToLoad==null?"":requiredAnnotationFileToLoad;
	}

	@Override
	public void setAnnotatorLoader(AnnotatorLoader newLoader){
		this.annotatorLoader=newLoader;
	}

	//
	// preprocessing for extraction
	//

	/** Make sure the required annotation is present. */
	public void requireMyAnnotation(TextLabels labels){
		labels.require(requiredAnnotation,requiredAnnotationFileToLoad,
				annotatorLoader);
	}

	//
	// extraction
	//

//	/** @deprecated Use extractInstance(TextLabels labels,Span s) */
//	final public Instance extractInstance(Span span){
//		instance=new MutableInstance(span,span.getDocumentGroupId());
//		extractFeatures(span);
//		return instance;
//	}

	/** Extract an Instance from a span */
	@Override
	final public Instance extractInstance(TextLabels labels,Span span){
		instance=new MutableInstance(span,span.getDocumentGroupId());
		textLabels=labels;
		extractFeatures(labels,span);
		return instance;
	}

	/**
	 * Starts a 'pipeline' of extraction steps, and adds the resulting features to
	 * the instance being built.
	 * <p>
	 * As an example: <code>fe.from(s).tokens(s).eq().emit()</code> adds
	 * bag-of-words type features.
	 */
	final public SpanResult from(Span s){
		return new SpanResult(new String[0],this,s);
	}

	/**
	 * Starts a 'pipeline' of extraction steps, and adds the resulting features to
	 * the instance being built.
	 * 
	 * <p>
	 * This is intended to be used as an alternative to using the SpanFE class to
	 * build an Span2Instance converter, eg
	 * 
	 * <pre><code>
	 * fe=new Span2Instance(){
	 * 
	 * 	public extractInstance(Span s){
	 * 		FeatureBuffer buf=new FeatureBuffer(s);
	 * 		SpanFE.from(s,buf).tokens().emit();
	 * 		SpanFE.from(s,buf).left().subspan(-2,2).emit();
	 * 		SpanFE.from(s,buf).right().subspan(0,2).emit();
	 * 		buf.getInstance();
	 * 	}
	 * }
	 * </code></pre>
	 * 
	 */
	final static public SpanResult from(Span s,FeatureBuffer buffer){
		return new SpanResult(new String[0],buffer,s);
	}

	/**
	 * Called by some SpanFE.Result subclasses when a 'pipeline' of extraction
	 * steps is ended with a StringBagResult.
	 */

	public void emit(StringBagResult result){
		for(Iterator<String> i=result.asBag().iterator();i.hasNext();){
			String s=i.next();
			Feature f=new Feature(result.extend(s));
			if(featureStoragePolicy==STORE_AS_BINARY){
				instance.addBinary(f);
			}else{
				int c=result.asBag().getCount(s);
				if(featureStoragePolicy==STORE_COMPACTLY&&c==1)
					instance.addBinary(f);
				else
					instance.addNumeric(f,c);
			}
		}
	}

	/**
	 * Called by some SpanFE.Result subclass when a 'pipeline' of extraction steps
	 * is ended with a TokenSetResult.
	 */

	public void emit(TokenSetResult result){
		emit(result.eq());
	}

	/**
	 * Called by some SpanFE.Result subclass when a 'pipeline' of extraction steps
	 * is ended with a SpanSetResult.
	 */
	public void emit(SpanSetResult result){
		emit(result.tokens());
	}

	/**
	 * Called by some SpanFE.Result subclass when a 'pipeline' of extraction steps
	 * is ended with a SpanResult.
	 */
	public void emit(SpanResult result){
		emit(result.tokens());
	}

	/**
	 * Implement this with a specific set of SpanFE 'pipelines'. Each pipeline
	 * will typically start with 'start(span)' and end with 'emit()'.
	 * 
	 */
	public void extractFeatures(Span span){
		throw new IllegalStateException(
				"you probably meant to use extractFeatures(labels,span) instead");
	}

	/**
	 * Implement this with a specific set of SpanFE 'pipelines'. Each pipeline
	 * will typically start with 'start(span)' and end with 'emit()'.
	 */
	abstract public void extractFeatures(TextLabels labels,Span span);

	/** Subclass this to change the tracing behavior. */
	public void trace(Result result){
		String[] name=result.getName();
		for(int i=0;i<name.length;i++)
			System.out.print(" "+name[i]);
		System.out.println(" -> "+result);
	}

	//
	// SpanFE.Result classes
	//

	/** Encodes an intermediate result of the SpanFE process. */
	static abstract public class Result{

		protected String[] name;

		protected SpanFE fe;

		public Result(String[] name,SpanFE fe){
			this.name=name;
			this.fe=fe;
			if(fe==null)
				throw new IllegalArgumentException("null fe");
		}

		// extend the name
		public String[] extend(String addition){
			return extend(name,addition);
		}

		public String[] extend(String[] partial,String addition){
			String[] extension=new String[partial.length+1];
			for(int i=0;i<partial.length;i++)
				extension[i]=partial[i];
			extension[partial.length]=addition;
			return extension;
		}

		// for traces
		protected Result doTrace(){
			fe.trace(this);
			return this;
		}

		public String[] getName(){
			return name;
		}

		/** Terminates a feature extraction pipeline by actually emitting features. */
		abstract public void emit();
	}

	/**
	 * An intermediate result of a SpanFE process where the object being operated
	 * on is a Set of something.
	 */
	abstract static public class SetResult<T> extends Result{

		protected SortedSet<T> set;

		public SetResult(String[] name,SpanFE fe,SortedSet<T> set){
			super(name,fe);
			this.set=set;
			if(this.set==null)
				throw new IllegalArgumentException("null set");
		}

		/**
		 * Convert to a plain old set.
		 */
		public Set<T> asSet(){
			return set;
		}

		/**
		 * Filter the set using a user-defined filter.
		 */
		protected SortedSet<T> applyFilter(Filter f){
			SortedSet<T> s=new TreeSet<T>();
			for(Iterator<T> i=set.iterator();i.hasNext();){
				T o=i.next();
				if(f.match(o))
					s.add(o);
			}
			return s;
		}

		/**
		 * Modify each element in the set using a user-defined function.
		 */
		protected SortedSet<T> mapFunction(Function f){
			SortedSet<T> s=new TreeSet<T>();
			for(Iterator<T> i=set.iterator();i.hasNext();){
				s.add(f.apply(i.next()));
			}
			return s;
		}
	}

	/**
	 * An intermediate result of an SpanFE process where a span is being
	 * processed.
	 */
	static public class SpanResult extends Result{

		private Span s;

		public SpanResult(String[] name,SpanFE fe,Span s){
			super(name,fe);
			this.s=s;
		}

		public SpanResult trace(){
			return (SpanResult)doTrace();
		}

		@Override
		public void emit(){
			fe.emit(this);
		}

		@Override
		public String toString(){
			return "[SpanResult: "+s+"]";
		}

		public Span getSpan(){
			return s;
		}

		/**
		 * Move to the span consisting of all tokens in the same document that
		 * precede the current span.
		 */
		public SpanResult left(){
			Span lSpan=s.documentSpan().subSpan(0,s.documentSpanStartIndex());
			return new SpanResult(extend("left"),fe,lSpan);
		}

		/**
		 * Move to the span consisting of all tokens in the same document that
		 * follow the current span.
		 */
		public SpanResult right(){
			Span rSpan=
					s.documentSpan().subSpan(s.documentSpanStartIndex()+s.size(),
							s.documentSpan().size()-s.documentSpanStartIndex()-s.size());
			return new SpanResult(extend("right"),fe,rSpan);
		}

		/**
		 * Move to the document containing this span.
		 */
		public SpanResult doc(){
			Span docSpan=s.documentSpan();
			return new SpanResult(extend("doc"),fe,docSpan);
		}

		/**
		 * Move to a set of all spans of the named type that are contained by the
		 * current span.
		 */
		public SpanSetResult contains(String type){
			SortedSet<Span> set=new TreeSet<Span>();
			for(Iterator<Span> i=
					fe.textLabels.instanceIterator(type,s.getDocumentId());i.hasNext();){
				Span other=i.next();
				if(s.contains(other)){
					set.add(other);
				}
			}
			return new SpanSetResult(extend("contains_"+type),fe,set);
		}

		/**
		 * Move to the specified subspan of the current span. Invalid indices will
		 * be trimmed to a valid size. Negative indices mean to extract a subspan
		 * from the end of the current span, e.g., subSpan(-2,2) means to extract a
		 * span containing the last two tokens.
		 */
		public SpanResult subSpan(int lo,int len){
			if(s.size()==0)
				return this;
			if(lo>=0){
				lo=Math.min(lo,s.size()-1);
				len=Math.min(s.size()-lo,len);
				return new SpanResult(extend("subspan_"+lo+"_"+len),fe,s
						.subSpan(lo,len));
			}else if(lo<0){
				lo=Math.max(s.size()+lo,0);
				len=Math.min(s.size()-lo,len);
				return new SpanResult(extend("subspanNeg_"+lo+"_"+len),fe,s.subSpan(lo,
						len));
			}else{
				throw new IllegalArgumentException("illegal subSpan indices "+lo+", "+
						len);
			}
		}

		/** Move to the set of all tokens contained by this span. */
		public TokenSetResult tokens(){
			SortedSet<Token> set=new TreeSet<Token>();
			for(int i=0;i<s.size();i++){
				set.add(s.getToken(i));
			}
			return new TokenSetResult(extend("tokens"),fe,set);
		}

		/**
		 * Move to the specified token inside the span. A negative index means to
		 * count from the end. An invalid index will result in an empty
		 * TokenSetResult.
		 */
		public TokenSetResult token(int index){
			String namex;
			int index1;
			if(index<0){
				index1=s.size()+index;
				namex="tokenNeg_"+(-index);
			}else{
				index1=index;
				namex="token_"+index;
			}
			SortedSet<Token> set=new TreeSet<Token>();
			if(index1>=0&&index1<s.size()){
				set.add(s.getToken(index1));
			}
			return new TokenSetResult(extend(namex),fe,set);
		}

		/** Move to the string value of the span. */
		public StringBagResult eq(){
			Bag<String> stringBag=new Bag<String>();
			stringBag.add(s.asString());
			return new StringBagResult(extend("eq"),fe,stringBag);
		}

		/**
		 * Make length of the span a feature. Eg feature is #tokens=3 for a 3-token
		 * span.
		 */
		public StringBagResult size(){
			Bag<String> stringBag=new Bag<String>();
			stringBag.add("#tokens",s.size());
			return new StringBagResult(name,fe,stringBag);
		}

		/**
		 * Make exact length of span a feature. Eg, feature is #tokens.3=1 for a
		 * 3-token span, #tokens_2=1 for a two-token span.
		 */
		public StringBagResult exactSize(){
			Bag<String> stringBag=new Bag<String>();
			stringBag.add("#tokens_"+s.size());
			return new StringBagResult(name,fe,stringBag);
		}
	}

	/**
	 * An intermediate result of a SpanFE process where the object being operated
	 * on is a set of spans.
	 */
	static public class SpanSetResult extends SetResult<Span>{

		public SpanSetResult(String[] name,SpanFE fe,SortedSet<Span> set){
			super(name,fe,set);
		}

		public SpanSetResult trace(){
			return (SpanSetResult)doTrace();
		}

		@Override
		public void emit(){
			fe.emit(this);
		}

		@Override
		public String toString(){
			return "[SpanSetResult: "+set+"]";
		}

		/**
		 * Move to the first span in the set.
		 */
		public SpanSetResult first(){
			SortedSet<Span> newSet=new TreeSet<Span>();
			if(set.size()>0)
				newSet.add(set.first());
			return new SpanSetResult(extend("first"),fe,newSet);
		}

		/**
		 * Move to the last span in the set.
		 */
		public SpanSetResult last(){
			SortedSet<Span> newSet=new TreeSet<Span>();
			if(set.size()>0)
				newSet.add(set.last());
			return new SpanSetResult(extend("last"),fe,newSet);
		}

		/**
		 * Find the set of all tokens contained by any span in the set.
		 */
		public TokenSetResult tokens(){
			SortedSet<Token> accum=new TreeSet<Token>();
			for(Iterator<Span> i=set.iterator();i.hasNext();){
				SpanResult r=new SpanResult(name,fe,i.next());
				accum.addAll(r.tokens().asSet());
			}
			return new TokenSetResult(extend("tokens"),fe,accum);
		}

		/** Move a set of all string values of spans in the set */
		public StringBagResult eq(){
			Bag<String> stringBag=new Bag<String>();
			for(Iterator<Span> i=set.iterator();i.hasNext();){
				stringBag.add(i.next().asString());
			}
			return new StringBagResult(extend("eq"),fe,stringBag);
		}

		/** Filter out spans that don't match the filter. */
		public SpanSetResult filter(Filter f){
			return new SpanSetResult(extend("filter_"+f.getName()),fe,applyFilter(f));
		}

		public SpanSetResult map(Function f){
			return new SpanSetResult(extend("map_"+f.getName()),fe,mapFunction(f));
		}
	}

	/**
	 * An intermediate result of a SpanFE process where the object being operated
	 * on is a set of tokens.
	 */
	static public class TokenSetResult extends SetResult<Token>{

		public TokenSetResult(String[] name,SpanFE fe,SortedSet<Token> set){
			super(name,fe,set);
		}

		public TokenSetResult trace(){
			return (TokenSetResult)doTrace();
		}

		@Override
		public void emit(){
			fe.emit(this);
		}

		@Override
		public String toString(){
			return "[TokenSetResult: "+set+"]";
		}

		/** Find all values of a token in this set. */
		public StringBagResult eq(){
			Bag<String> stringBag=new Bag<String>();
			for(Iterator<Token> i=set.iterator();i.hasNext();){
				Token token=i.next();
				stringBag.add(token.getValue());
			}
			return new StringBagResult(extend("eq"),fe,stringBag);
		}

		/** Find the value of some given property. */
		public StringBagResult prop(String property){
			Bag<String> stringBag=new Bag<String>();
			for(Iterator<Token> i=set.iterator();i.hasNext();){
				Token token=i.next();
				String value=fe.textLabels.getProperty(token,property);
				if(value!=null){
					stringBag.add(value);
				}
			}
			return new StringBagResult(extend(property),fe,stringBag);
		}

		/** Filter out tokens that have some property set to a non-null value. */
		public TokenSetResult hasProp(String property){
			SortedSet<Token> filteredSet=new TreeSet<Token>();
			for(Iterator<Token> i=set.iterator();i.hasNext();){
				Token token=i.next();
				String value=fe.textLabels.getProperty(token,property);
				if(value!=null)
					filteredSet.add(token);
			}
			return new TokenSetResult(extend("hasProp_"+property),fe,filteredSet);
		}

		/**
		 * Filter out tokens that have a property set to some particular value. A
		 * targetValue of 'null' will filter out tokens with null values of the
		 * property.
		 */
		public TokenSetResult hasProp(String property,String targetValue){
			SortedSet<Token> filteredSet=new TreeSet<Token>();
			for(Iterator<Token> i=set.iterator();i.hasNext();){
				Token token=i.next();
				String value=fe.textLabels.getProperty(token,property);
				if((targetValue==null&&value==null)||
						(targetValue!=null&&targetValue.equals(value)))
					filteredSet.add(token);
			}
			String targetValueTag=(targetValue==null)?"NULL":targetValue;
			return new TokenSetResult(extend("hasProp_"+property+"_"+targetValueTag),
					fe,filteredSet);
		}

	}

	/**
	 * An intermediate result of a SpanFE process where the object being operated
	 * on is a set of strings.
	 */
	static public class StringBagResult extends SetResult<String>{

		private Bag<String> bag;

		public StringBagResult(String[] name,SpanFE fe,Bag<String> bag){
			super(name,fe,bag.asSet());
			this.bag=bag;
		}

		@Override
		public void emit(){
			fe.emit(this);
		}

		public StringBagResult trace(){
			return (StringBagResult)doTrace();
		}

		@Override
		public String toString(){
			return "[StringBagResult: "+bag+"]";
		}

		public Bag<String> asBag(){
			return bag;
		}

		public StringBagResult lc(){
			Bag<String> lcBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				lcBag.add(str.toLowerCase(),n);
			}
			return new StringBagResult(extend("lc"),fe,lcBag);
		}

		public StringBagResult toConst(String replacement){
			Bag<String> trBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				trBag.add(replacement,n);
			}
			return new StringBagResult(extend("toConst"),fe,trBag);
		}

		public StringBagResult tr(String regex,String replacement){
			Bag<String> trBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				trBag.add(str.replaceAll(regex,replacement),n);
			}
			return new StringBagResult(extend("tr/"+regex+"/"+replacement),fe,trBag);
		}

		public StringBagResult charTypes(){
			Bag<String> trBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				String charTypes=
						str.replaceAll("[A-Z]","A").replaceAll("[a-z]","a").replaceAll(
								"[0-9]","0");
				int n=bag.getCount(str);
				trBag.add(charTypes,n);
			}
			return new StringBagResult(extend("charTypes"),fe,trBag);
		}

		public StringBagResult charTypePattern(){
			Bag<String> trBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				String pattern=
						str.replaceAll("[A-Z]+","X+").replaceAll("[a-z]+","x+").replaceAll(
								"[0-9]+","9+");
				int n=bag.getCount(str);
				trBag.add(pattern,n);
			}
			return new StringBagResult(extend("charTypePattern"),fe,trBag);
		}

		// Removes punctuation and numbers
		public StringBagResult punk(){
			Bag<String> punkBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				Pattern p=Pattern.compile("[\\W\\d]+");
				Matcher m=p.matcher(str);
				if(!m.find()){
					punkBag.add(str,n);
				}
			}
			return new StringBagResult(extend("punk"),fe,punkBag);
		}

		// Use or Remove words in String Array
		public StringBagResult stopwords(String action){
			String[] wordArray=StopWords.LONG; // change with SHORT
			Bag<String> swBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				if(action.equalsIgnoreCase("use")){
					// "use" words as sole features
					for(int j=0;j<wordArray.length;j++){
						if(wordArray[j].equals(str)){
							swBag.add(str,n);
						}
					}
				}else if(action.equalsIgnoreCase("remove")){
					// "remove" words from retieved features
					boolean isAbsent=true;
					for(int j=0;j<wordArray.length;j++){
						if((wordArray[j].equals(str))){
							isAbsent=false;
						}
					}
					if(isAbsent){
						swBag.add(str,n);
					}
				}else{
					throw new IllegalArgumentException("Error: action is missing!");
				}
			}
			return new StringBagResult(extend("stopwords-"+action),fe,swBag);
		}

		/** Use ONLY words in Dictionary File. */
		public StringBagResult usewords(String filename) throws IOException{
			Bag<String> uwBag=new Bag<String>();
			for(Iterator<String> i=bag.iterator();i.hasNext();){
				String str=i.next();
				int n=bag.getCount(str);
				File dictFile=new File(filename);
				FileReader fr=new FileReader(dictFile);
				BufferedReader in=new BufferedReader(fr);
				String line;
				while((line=in.readLine())!=null){
					line=line.trim();
					// Check whether str is in Dictionary File
					if(line.equals(str)){
						uwBag.add(str,n);
					}
				}
			}
			return new StringBagResult(extend("usewords"),fe,uwBag);
		}
	}

	/**
	 * An abstract class that can be used to filter SpanSetResults.
	 */
	static public abstract class Filter{

		/**
		 * A short name, used to help construct feature names associated with this
		 * filter.
		 */
		abstract public String getName();

		/**
		 * Should return true for all items that will be accepted by the filter.
		 */
		abstract public boolean match(Object o);
	}

	/**
	 * An abstract class that can be used to change SpanSets
	 */
	static public abstract class Function{

		/**
		 * A short name, used to help construct feature names associated with this
		 * filter.
		 */
		abstract public String getName();

		/** Should return the modified object. */
		abstract public <T>T apply(T o);
	}
}
