package edu.cmu.minorthird.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.gui.ZoomingTextLabelsViewer;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Maintains assertions about 'types' and 'properties' of contiguous Spans of
 * these TextToken's.
 * 
 * @author William Cohen
 */

public class BasicTextLabels implements MutableTextLabels,Serializable,Visible,
		Saveable{

	static private final long serialVersionUID=20080303L;

	private static Logger log=Logger.getLogger(BasicTextLabels.class);

	private Map<Token,SortedMap<String,String>> textTokenPropertyMap=new HashMap<Token,SortedMap<String,String>>();

	private Set<String> textTokenPropertySet=new HashSet<String>();

	private Map<Span,SortedMap<String,String>> spanPropertyMap=new HashMap<Span,SortedMap<String,String>>();

	private Map<String,SortedSet<Span>> spansWithSomePropertyByDocId=new HashMap<String,SortedSet<Span>>();

	private Set<String> spanPropertySet=new HashSet<String>();

	private Map<String,SortedMap<String,SortedSet<Span>>> typeDocumentSetMap=new TreeMap<String,SortedMap<String,SortedSet<Span>>>();

	private Map<String,SortedMap<String,SortedSet<Span>>> closureDocumentSetMap=new HashMap<String,SortedMap<String,SortedSet<Span>>>();

	private Map<String,Set<String>> textTokenDictMap=new HashMap<String,Set<String>>();

	private Set<String> annotatedBySet=new HashSet<String>();

	private Map<ObjectStringKey<?>,Details> detailMap=new TreeMap<ObjectStringKey<?>,Details>();

	private AnnotatorLoader loader=new DefaultAnnotatorLoader();

	// for statementType = TRIE
	public Trie trie=null;

	// don't serialize this, it's too big!
	transient private TextBase textBase=null;

	/** Creates an empty TextLabels not associated with a TextBase */
	public BasicTextLabels(){
		this.textBase=null;
	}

	/** Creates an empty TextLabels associated with the specified TextBase */
	public BasicTextLabels(TextBase textBase){
		this.textBase=textBase;
	}

	/**
	 * Returns the TextBase associated with this labels set or NULL if it has not
	 * been set.
	 */
	@Override
	public TextBase getTextBase(){
		return textBase;
	}

	/** Returns whether this labels set knows about the specified dictionary */
	@Override
	public boolean hasDictionary(String dictionary){
		return textTokenDictMap.containsKey(dictionary);
	}

	/**
	 * Sets the TextBase associated with this labels set.
	 * 
	 * @throws java.lang.IllegalStateException
	 *           If the TextBase has already been set.
	 */
	@Override
	public void setTextBase(TextBase textBase){
		if(this.textBase!=null)
			throw new IllegalStateException("textBase already set");
		this.textBase=textBase;
	}

	/** A convenience method which creates empty labels containing a single string. */
	public BasicTextLabels(String s){
		this(new BasicTextBase());
		((BasicTextBase)getTextBase()).loadDocument("nullId",s);
	}

	//
	// methods used to maintain annotation history
	//

	/**
	 * Returns whether or not this labels set has been annotated to include the
	 * specified type.
	 */
	@Override
	public boolean isAnnotatedBy(String s){
		return annotatedBySet.contains(s);
	}

	/**
	 * Adds the specified type to the list of annotation types that this labels
	 * set has been annotated to contain.
	 */
	@Override
	public void setAnnotatedBy(String s){
		annotatedBySet.add(s);
	}

	/** Sets the loader used to locate annotators. */
	@Override
	public void setAnnotatorLoader(AnnotatorLoader newLoader){
		this.loader=newLoader;
	}

	/** Returns the current loader used to locate annotators. */
	@Override
	public AnnotatorLoader getAnnotatorLoader(){
		return loader;
	}

	@Override
	public void require(String annotationType,String fileToLoad){
		require(annotationType,fileToLoad,loader);
	}

	@Override
	public void require(String annotationType,String fileToLoad,AnnotatorLoader theLoader){
		doRequire(this,annotationType,fileToLoad,theLoader);
	}

	static public void doRequire(MonotonicTextLabels labels,String annotationType,String fileToLoad,AnnotatorLoader theLoader){
		// only annotate if not already done
		if(annotationType!=null&&!labels.isAnnotatedBy(annotationType)){
			if(theLoader==null){
				 // use current loader as default
				theLoader=labels.getAnnotatorLoader();
			}
			log.info("Trying load \""+annotationType+"\" from "+fileToLoad+" using "+theLoader);
			Annotator annotator=theLoader.findAnnotator(annotationType,fileToLoad);
			log.info("Loaded "+annotator);
			if(annotator==null){
				throw new IllegalArgumentException("Cannot find annotator "+annotationType+" (file: "+fileToLoad+")");
			}

			// annotate using theLoader for any recursively-required annotations,
			AnnotatorLoader savedLoader=labels.getAnnotatorLoader();
			labels.setAnnotatorLoader(theLoader);
			annotator.annotate(labels);
			labels.setAnnotatorLoader(savedLoader); // restore original loader

			// check that the annotationType is provided
			if(!labels.isAnnotatedBy(annotationType)){
				throw new IllegalStateException(annotator+" did not provide annotation type: "+annotationType);
			}
		}
	}

	@Override
	public void annotateWith(String annotationType,String fileToLoad){
		annotateWith(this,annotationType,fileToLoad);
	}

	static public void annotateWith(MonotonicTextLabels labels,
			String annotationType,String fileToLoad){
		AnnotatorLoader theLoader=labels.getAnnotatorLoader();
		Annotator annotator=theLoader.findAnnotator(annotationType,fileToLoad);
		annotator.annotate(labels);
	}

	//
	// maintain dictionaries
	//

	/** Returns true if the value of the Token is in the named dictionary. */
	@Override
	public boolean inDict(Token token,String dictName){
		if(token.getValue()==null)
			throw new IllegalArgumentException("null token.value?");
		Set<String> set=textTokenDictMap.get(dictName);
		if(set==null)
			throw new IllegalArgumentException("undefined dictionary "+dictName);
		return set.contains(token.getValue());
	}

	/** Associate a dictionary with this labeling. */
	@Override
	public void defineDictionary(String dictName,Set<String> dictionary){
		textTokenDictMap.put(dictName,dictionary);
		if(log.isDebugEnabled())
			log.debug("added to token dictionary: "+dictName+" values "+textTokenDictMap.get(dictName));
	}

	/** Associate a dictionary from this file */
	@Override
	public void defineDictionary(String dictName,List<String> fileNames,
			boolean ignoreCase){
		Set<String> wordSet=new HashSet<String>();
		AnnotatorLoader theLoader=this.getAnnotatorLoader();
		// We should use the same tokenizer that the text base associated with this
		// labels set uses for new docs.
		// RegexTokenizer tok = new RegexTokenizer();
		Tokenizer tok=this.getTextBase().getTokenizer();
		String[] currentEntryTokens;
		for(int i=0;i<fileNames.size();i++){
			String fileName=fileNames.get(i);
			InputStream stream=theLoader.findFileResource(fileName);
			try{
				LineNumberReader bReader=
						new LineNumberReader(new BufferedReader(new InputStreamReader(
								stream)));
				String s=null;
				while((s=bReader.readLine())!=null){
					s=s.trim(); // remove trailing blanks
					// Split the entry into tokens and add it to the set only if there is
					// a single token.
					// Otherwise give an warning and ignore the entry.
					currentEntryTokens=tok.splitIntoTokens(s);
					if(currentEntryTokens.length>1){
						log
								.warn("Ignoring entry: \'"+
										s+
										"\' because it contains more than 1 token.  Use a Trie to match against sequences of tokens.");
					}else{
						if(ignoreCase)
							s=s.toLowerCase();
						wordSet.add(s);
					}
				}
				bReader.close();
			}catch(IOException ioe){
				// parseError("Error when reading " + fileName.toString() + ": " + ioe);
				ioe.printStackTrace();
			}
		}
		defineDictionary(dictName,wordSet);
	}

	/** Return a trie if defined */
	@Override
	public Trie getTrie(){
		return trie;
	}

	/** Define a trie */
	@Override
	public void defineTrie(List<String> phraseList){
		trie=new Trie();
		// We should use the same tokenizer that the text base associated with this
		// labels set uses for new docs.
		// RegexTokenizer tokenizer = new RegexTokenizer();
		Tokenizer tokenizer=this.getTextBase().getTokenizer();
		for(int i=0;i<phraseList.size();i++){
			String[] toks=tokenizer.splitIntoTokens(phraseList.get(i));
			if(toks.length<=2||!"\"".equals(toks[0])||
					!"\"".equals(toks[toks.length-1])){
				trie.addWords("phrase#"+i,toks);
			}else{
				StringBuffer defFile=new StringBuffer("");
				for(int j=1;j<toks.length-1;j++){
					defFile.append(toks[j]);
				}
				AnnotatorLoader theLoader=this.getAnnotatorLoader();
				InputStream stream=theLoader.findFileResource(defFile.toString());
				try{
					LineNumberReader bReader=
							new LineNumberReader(new BufferedReader(new InputStreamReader(
									stream)));
					String s=null;
					int line=0;
					while((s=bReader.readLine())!=null){
						line++;
						String[] words=tokenizer.splitIntoTokens(s);
						trie.addWords(defFile+".line."+line,words);
					}
					bReader.close();
				}catch(IOException ioe){
					// parseError("Error when reading " + defFile.toString() + ": " +
					// ioe);
					ioe.printStackTrace();
				}
			} // file load
		} // each phrase
	}

	//
	// maintain assertions about properties of Tokens
	//

	/** Get the property value associated with this Token. */
	@Override
	public String getProperty(Token token,String prop){
		return getPropMap(token).get(prop);
	}

	/** Get a set of all properties. */
	@Override
	public Set<String> getTokenProperties(){
		return textTokenPropertySet;
	}

	/** Assert that Token textToken has the given value of the given property */
	@Override
	public void setProperty(Token textToken,String prop,String value){
		getPropMap(textToken).put(prop,value);
		textTokenPropertySet.add(prop);
	}

	/**
	 * Assert that Token textToken has the given value of the given property, and
	 * associate that with some detailed information
	 */
	@Override
	public void setProperty(Token textToken,String prop,String value,
			Details details){
		setProperty(textToken,prop,value);
		if(details!=null){
			detailMap.put(new TokenPropKey(textToken,prop),details);
		}
	}

	private SortedMap<String,String> getPropMap(Token textToken){
		SortedMap<String,String> map=textTokenPropertyMap.get(textToken);
		if(map==null){
			map=new TreeMap<String,String>();
			textTokenPropertyMap.put(textToken,map);
		}
		return map;
	}

	//
	// maintain assertions about properties of spans
	//

	/** Get the property value associated with this Span. */
	@Override
	public String getProperty(Span span,String prop){
		return getPropMap(span).get(prop);
	}

	/** Get a set of all properties. */
	@Override
	public Set<String> getSpanProperties(){
		return spanPropertySet;
	}

	/** Find all spans that have a non-null value for this property. */
	@Override
	public Iterator<Span> getSpansWithProperty(String prop){
		SortedSet<Span> accum=new TreeSet<Span>();
		for(Iterator<Span> i=spanPropertyMap.keySet().iterator();i.hasNext();){
			Span s=i.next();
			if(getProperty(s,prop)!=null){
				accum.add(s);
			}
		}
		return accum.iterator();
	}

	/** Find all spans that have a non-null value for this property. */
	@Override
	public Iterator<Span> getSpansWithProperty(String prop,String id){
		SortedSet<Span> set=spansWithSomePropertyByDocId.get(id);
		if(set==null)
			return Collections.EMPTY_SET.iterator();
		else{
			SortedSet<Span> accum=new TreeSet<Span>();
			for(Iterator<Span> i=set.iterator();i.hasNext();){
				Span s=i.next();
				if(getProperty(s,prop)!=null){
					accum.add(s);
				}
			}
			return accum.iterator();
		}
	}

	/** Assert that Span span has the given value of the given property */
	@Override
	public void setProperty(Span span,String prop,String value){
		getPropMap(span).put(prop,value);
		spanPropertySet.add(prop);
		SortedSet<Span> set=spansWithSomePropertyByDocId.get(span.getDocumentId());
		if(set==null)
			spansWithSomePropertyByDocId
					.put(span.getDocumentId(),(set=new TreeSet<Span>()));
		set.add(span);
	}

	@Override
	public void setProperty(Span span,String prop,String value,Details details){
		setProperty(span,prop,value);
		if(details!=null){
			detailMap.put(new SpanPropKey(span,prop),details);
		}
	}

	private SortedMap<String,String> getPropMap(Span span){
		SortedMap<String,String> map=spanPropertyMap.get(span);
		if(map==null){
			map=new TreeMap<String,String>();
			spanPropertyMap.put(span,map);
		}
		return map;
	}

	//
	// maintain assertions about types of Spans
	//
	@Override
	public boolean hasType(Span span,String type){
		return getTypeSet(type,span.getDocumentId()).contains(span);
	}

	@Override
	public void addToType(Span span,String type){
		if(type==null)
			throw new IllegalArgumentException("null type added");
		lookupTypeSet(type,span.getDocumentId()).add(span);
	}

	@Override
	public void addToType(Span span,String type,Details details){
		addToType(span,type);
		if(details!=null){
			detailMap.put(new SpanTypeKey(span,type),details);
		}
	}

	@Override
	public Set<String> getTypes(){
		return typeDocumentSetMap.keySet();
	}

	@Override
	public boolean isType(String type){
		return typeDocumentSetMap.get(type)!=null;
	}

	@Override
	public void declareType(String type){
		// System.out.println("BasicTextLabels: declareType: "+type);
		if(type==null)
			throw new IllegalArgumentException("null type declared");
		if(!isType(type))
			typeDocumentSetMap.put(type,new TreeMap<String,SortedSet<Span>>());
	}

	@Override
	public Iterator<Span> instanceIterator(String type){
		return new MyNestedSpanLooper(type,false);
	}

	@Override
	public Iterator<Span> instanceIterator(String type,String documentId){
		if(documentId!=null)
			return getTypeSet(type,documentId).iterator();
		else
			return instanceIterator(type);
	}

	@Override
	public void defineTypeInside(String type,Span s,Iterator<Span> i){
		if(type==null||s.getDocumentId()==null)
			throw new IllegalArgumentException("null type defined");
		// System.out.println("BTE type: "+type+" documentId: "+s.getDocumentId());
		Set<Span> set=lookupTypeSet(type,s.getDocumentId());
		// remove all spans currently inside set
		for(Iterator<Span> j=set.iterator();j.hasNext();){
			Span t=j.next();
			if(s.contains(t))
				j.remove();
		}
		// add spans from i to set
		while(i.hasNext())
			set.add(i.next());
		// close the type
		closeTypeInside(type,s);
	}

	@Override
	public Details getDetails(Span span,String type){
		SpanTypeKey key=new SpanTypeKey(span,type);
		Details details=detailMap.get(key);
		if(details!=null)
			return details;
		else
			return hasType(span,type)?Details.DEFAULT:null;
	}

	// get the set of spans with a given type in the given document
	// so that it can be modified
	protected Set<Span> lookupTypeSet(String type,String documentId){
		if(type==null||documentId==null)
			throw new IllegalArgumentException("null type?");
		SortedMap<String,SortedSet<Span>> documentsWithType=typeDocumentSetMap.get(type);
		if(documentsWithType==null){
			typeDocumentSetMap.put(type,documentsWithType=new TreeMap<String,SortedSet<Span>>());
		}
		// System.out.println("BTE type: "+type+" documentId: "+documentId+"
		// documentsWithType:" + documentsWithType);
		SortedSet<Span> set=documentsWithType.get(documentId);
		if(set==null){
			documentsWithType.put(documentId,(set=new TreeSet<Span>()));
		}
		return set;
	}

	// get the set of spans with a given type in the given document w/o changing
	// it
	@Override
	public Set<Span> getTypeSet(String type,String documentId){
		if(type==null||documentId==null)
			throw new IllegalArgumentException("null type?");
		SortedMap<String,SortedSet<Span>> documentsWithType=typeDocumentSetMap.get(type);
		if(documentsWithType==null)
			return Collections.EMPTY_SET;
		SortedSet<Span> set=documentsWithType.get(documentId);
		if(set==null)
			return Collections.EMPTY_SET;
		return set;
	}

	private class ObjectStringKey<T extends Comparable<T>> implements Comparable<ObjectStringKey<T>>{

		T obj;

		String str;

		public ObjectStringKey(T o,String s){
			this.obj=o;
			this.str=s;
		}

		@Override
		public int compareTo(ObjectStringKey<T> b){
			String bn=b.obj.getClass().toString();
			int tmp=obj.getClass().toString().compareTo(bn);
			if(tmp!=0)
				return tmp;
			tmp=obj.compareTo(b.obj);
			if(tmp!=0)
				return tmp;
			return str.compareTo(b.str);
		}
	}

	private class SpanTypeKey extends ObjectStringKey<Span>{

		public SpanTypeKey(Span span,String type){
			super(span,"type:"+type);
		}
	}

	private class SpanPropKey extends ObjectStringKey<Span>{

		public SpanPropKey(Span span,String prop){
			super(span,"prop:"+prop);
		}
	}

	private class TokenPropKey extends ObjectStringKey<String>{

		public TokenPropKey(Token token,String prop){
			super(token.getValue(),prop);
		}
	}

	//
	// maintain assertions about where the closed world assumption holds
	//

	@Override
	public Iterator<Span> closureIterator(String type){
		return new MyNestedSpanLooper(type,true);
	}

	@Override
	public Iterator<Span> closureIterator(String type,String documentId){
		if(documentId!=null){
			return getClosureSet(type,documentId).iterator();
		}
		else{
			return closureIterator(type);
		}
	}

	@Override
	public void closeTypeInside(String type,Span s){
		getClosureSet(type,s.getDocumentId()).add(s);
	}

	/**
	 * get the set of spans with a given type in the given document
	 */
	private Set<Span> getClosureSet(String type,String documentId){
		SortedMap<String,SortedSet<Span>> documentsWithClosure=closureDocumentSetMap.get(type);
		if(documentsWithClosure==null){
			closureDocumentSetMap.put(type,documentsWithClosure=new TreeMap<String,SortedSet<Span>>());
			//closureDocumentSetMap.put(type,documentsWithClosure=typeDocumentSetMap.get(type));
		}
		SortedSet<Span> set=documentsWithClosure.get(documentId);
		if(set==null){
			documentsWithClosure.put(documentId,set=new TreeSet<Span>());
		}
		return set;
	}

	/** iterate over all spans of a given type */
	private class MyNestedSpanLooper implements Iterator<Span>{

		private Iterator<Map.Entry<String,SortedSet<Span>>> documentIterator;

		private Iterator<Span> spanIterator;

		private Span nextSpan;

//		private int estimatedSize;

		// private boolean getClosures; // if false, get documents

		public MyNestedSpanLooper(String type,boolean getClosures){
			// System.out.println("building MyNestedSpanLooper for "+type+":
			// "+typeDocumentSetMap);
			Map<String,SortedSet<Span>> documentMap=getClosures?closureDocumentSetMap.get(type):typeDocumentSetMap.get(type);
			if(documentMap==null){
				nextSpan=null;
//				estimatedSize=0;
			}else{
				// iterator over the documents in the map
				documentIterator=documentMap.entrySet().iterator();
//				estimatedSize=documentMap.entrySet().size();
				spanIterator=null;
				advance();
			}
		}

		/**
		 * @return Number of documents with the given type
		 */
//		public int estimatedSize(){
//			return estimatedSize;
//		}

		@Override
		public boolean hasNext(){
			return nextSpan!=null;
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException("can't remove");
		}

		@Override
		public Span next(){
			Span result=nextSpan;
			advance();
			return result;
		}

//		public Span nextSpan(){
//			return (Span)next();
//		}

		private void advance(){
			if(spanIterator!=null&&spanIterator.hasNext()){
				// get next span in the current document
				nextSpan=spanIterator.next();
			}else if(documentIterator.hasNext()){
				// move to the next document
				Map.Entry<String,SortedSet<Span>> entry=documentIterator.next();
				spanIterator=entry.getValue().iterator();
				advance();
			}else{
				// nothing found
				nextSpan=null;
			}
		}
	}

	@Override
	public String toString(){
		return "[BasicTextLabels "+typeDocumentSetMap+"]";
	}

	/** Dump of all strings that have textTokenuence with the given property */
	@Override
	public String showTokenProp(TextBase base,String prop){
		StringBuffer buf=new StringBuffer();
		for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
			Span span=i.next();
			for(int j=0;j<span.size();j++){
				Token textToken=span.getToken(j);
				if(j>0)
					buf.append(" ");
				buf.append(textToken.getValue());
				String val=getProperty(textToken,prop);
				if(val!=null){
					buf.append(":"+val);
				}
			}
			buf.append("\n");
		}
		return buf.toString();
	}

	@Override
	public Viewer toGUI(){
		return new ZoomingTextLabelsViewer(this);
	}

	//
	// Implement Saveable interface.
	//
	static private final String FORMAT_NAME="Minorthird TextLabels";

	@Override
	public String[] getFormatNames(){
		return new String[]{FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String s){
		return ".labels";
	}

	@Override
	public void saveAs(File file,String format) throws IOException{
		if(!format.equals(FORMAT_NAME))
			throw new IllegalArgumentException("illegal format "+format);
		new TextLabelsLoader().saveTypesAsOps(this,file);
	}

	@Override
	public Object restore(File file) throws IOException{
		throw new UnsupportedOperationException("Cannot load TextLabels object");
	}

}
