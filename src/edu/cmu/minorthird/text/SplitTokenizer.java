/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.util.ArrayList;
import java.util.List;

public class SplitTokenizer implements Tokenizer{

	private String splitString;

	public SplitTokenizer(String s){
		this.splitString=s;
	}

	public String getSplitString(){
		return splitString;
	}

	@Override
	public String[] splitIntoTokens(String string){
		return string.split(splitString);
	}

	/** Tokenize a document */
	@Override
	public TextToken[] splitIntoTokens(Document document){
		List<TextToken> tokenList=new ArrayList<TextToken>();
		TextToken[] tokenArray;
		String documentText=document.getText();
		int currPos=0;

		// Split the document text by the specified split string.
		String[] tokenValues=documentText.split(splitString);

		// Create the tokens.
		for(int i=0;i<tokenValues.length;i++){
			// Skip upto the first char in the next token
			currPos=documentText.indexOf(tokenValues[i],currPos);
			// Create the token
			tokenList.add(new TextToken(document,currPos,tokenValues[i].length()));
			// Skip past the text in the token.
			currPos=currPos+tokenValues[i].length();
		}
		tokenArray=tokenList.toArray(new TextToken[0]);

		return tokenArray;
	}
}
