/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * This implementation of the Tokenizer interface is used for re-tokenizing documents based on
 * a specified spantype.  All tokens inside the spantype are put together to create a single
 * "pseudotoken".  All other tokens remain as originally tokenized.
 *
 * @author Quinten Mercer
 */
public class SpanTypeTokenizer extends CompoundTokenizer{

	private static Logger log=Logger.getLogger(SpanTypeTokenizer.class);

	private String spanType;

	private TextLabels labels;

	public SpanTypeTokenizer(String s,TextLabels l){
		this.spanType=s;
		this.labels=l;
		this.parentTokenizer=l.getTextBase().getTokenizer();
	}

	public String getSpanType(){
		return spanType;
	}

	public TextLabels getTextLabels(){
		return labels;
	}

	/** Tokenize a string */
	@Override
	public String[] splitIntoTokens(String string){
		return parentTokenizer.splitIntoTokens(string);
	}

	/** Tokenize a document. */
	@Override
	public TextToken[] splitIntoTokens(Document document){

		//TextToken[] parentTokens;

		// If there is no document in the textbase related to the labels that we know, just use the base tokenizer
		if(labels.getTextBase().getDocument(document.getId())==null){
			log.warn("Labels for document with id: "+document.getId()+
					" are not available, will tokenize using base tokenizer.");
			return parentTokenizer.splitIntoTokens(document);
		}

		// If the document passed in has the same doc id as a document in our labels set, but has 
		// different actual text, then just use the parent tokenizer.
		if(!labels.getTextBase().getDocument(document.getId()).getText().equals(
				document.getText())){
			log
					.warn("Document with id: "+
							document.getId()+
							" differs from the document in the labels set with the same ID.  Will tokenize using base tokenizer.");
			return parentTokenizer.splitIntoTokens(document);
		}

		//  Get the tokens for the matching document in our labels set.
		TextToken[] parentTokens=
				labels.getTextBase().getDocument(document.getId()).getTokens();

		// Sort the old tokens to guarantee that we can access them in order.
		SortedSet<TextToken> sortedTokens=new TreeSet<TextToken>();
		for(int i=0;i<parentTokens.length;i++){
			sortedTokens.add(parentTokens[i]);
		}

		// Create the new list of tokens by copying all the old tokens outside of instances of spanType and
		//  combining all the old tokens inside each instance into a single new token.  The idea with this 
		//  implementation is that currOldToken holds the *next* token from the parent document to be looked at.
		//  This token will either be added as is, or skipped over so that a new custom token that includes its
		//  characters can be created in its place.
		List<TextToken> tokenList=new ArrayList<TextToken>();
		Iterator<TextToken> oldTokenIterator=sortedTokens.iterator();
		TextToken currOldToken=oldTokenIterator.next();
		Iterator<Span> typeIterator=labels.instanceIterator(spanType,document.getId());
		while(typeIterator.hasNext()){
			Span currSpan=typeIterator.next();
			// Copy over the tokens for the text that came before the current instance of spanType
			while(currOldToken.getLo()<currSpan.getTextToken(0).getLo()){
				tokenList.add(new TextToken(document,currOldToken.getLo(),currOldToken
						.getLength()));
				currOldToken=oldTokenIterator.next();
			}
			tokenList.add(new TextToken(document,currSpan.getTextToken(0).getLo(),
					currSpan.asString().length()));
			// Skip past the tokens in the iterator that correspond to this instance of spanType;
			for(int i=0;i<currSpan.size();i++){
				currOldToken=
						oldTokenIterator.hasNext()?(TextToken)oldTokenIterator.next():null;
			}
		}

		// Copy any remaining tokens.
		if(currOldToken!=null)
			tokenList.add(new TextToken(document,currOldToken.getLo(),currOldToken
					.getLength()));
		while(oldTokenIterator.hasNext()){
			currOldToken=oldTokenIterator.next();
			tokenList.add(new TextToken(document,currOldToken.getLo(),currOldToken
					.getLength()));
		}

		// Convert the ArrayList into a TextToken[] to return
		TextToken[] tokenArray=tokenList.toArray(new TextToken[0]);
		return tokenArray;
	}
}
