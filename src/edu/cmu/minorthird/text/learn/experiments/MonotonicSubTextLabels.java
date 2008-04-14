package edu.cmu.minorthird.text.learn.experiments;

import java.util.List;
import java.util.Set;

import edu.cmu.minorthird.text.AnnotatorLoader;
import edu.cmu.minorthird.text.Details;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.Token;
import edu.cmu.minorthird.text.Trie;

/**
 * A subset of another TextLabels that can be added to. Additions are propogated
 * back to the underlying MonotonicTextLabels passed in as an argument.
 * 
 * @author William Cohen
 */

public class MonotonicSubTextLabels extends SubTextLabels implements
		MonotonicTextLabels{

	private MonotonicTextLabels monotonicLabels;

	public void setAnnotatorLoader(AnnotatorLoader newLoader){
		monotonicLabels.setAnnotatorLoader(newLoader);
	}

	public AnnotatorLoader getAnnotatorLoader(){
		return monotonicLabels.getAnnotatorLoader();
	}

	public MonotonicSubTextLabels(SubTextBase subBase,MonotonicTextLabels labels){
		super(subBase,labels);
		this.monotonicLabels=labels;
	}

	public void defineDictionary(String dictName,Set<String> dict){
		monotonicLabels.defineDictionary(dictName,dict);
	}

	/** Associate a dictionary from this file */
	public void defineDictionary(String dictName,List<String> fileNames,
			boolean ignoreCase){
		monotonicLabels.defineDictionary(dictName,fileNames,ignoreCase);
	}

	/** Return a trie if defined */
	public Trie getTrie(){
		return monotonicLabels.getTrie();
	}

	/** Define a trie */
	public void defineTrie(List<String> phraseList){
		monotonicLabels.defineTrie(phraseList);
	}

	public void setProperty(Token token,String prop,String value){
		monotonicLabels.setProperty(token,prop,value);
	}

	public void setProperty(Token token,String prop,String value,Details details){
		monotonicLabels.setProperty(token,prop,value,details);
	}

	public void setProperty(Span span,String prop,String value){
		if(subBase.contains(span))
			monotonicLabels.setProperty(span,prop,value);
	}

	public void setProperty(Span span,String prop,String value,Details details){
		if(subBase.contains(span))
			monotonicLabels.setProperty(span,prop,value,details);
	}

	public void addToType(Span span,String type){
		if(subBase.contains(span))
			monotonicLabels.addToType(span,type);
	}

	public void addToType(Span span,String type,Details details){
		if(subBase.contains(span))
			monotonicLabels.addToType(span,type,details);
	}

	public void declareType(String type){
		monotonicLabels.declareType(type);
	}

	public void require(String annotationType,String fileToLoad){
		monotonicLabels.require(annotationType,fileToLoad);
	}

	public void require(String annotationType,String fileToLoad,
			AnnotatorLoader loader){
		monotonicLabels.require(annotationType,fileToLoad,loader);
	}

}
