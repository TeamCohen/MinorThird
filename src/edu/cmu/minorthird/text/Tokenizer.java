/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maintains information about what's in a set of documents.
 * Specifically, this contains a set of character sequences (TextToken's)
 * from some sort of set of containing documents - typically found by
 * tokenization.
 *
 *<p>
 * Implementations of TextBase should also implement Serializable.
 *
 * @author Cameron Williams
*/

public class Tokenizer
{
    public final static int REGEX = 0;
    public final static int SPLIT = 1;
    public int parseType = REGEX;

    public String splitString = " ";
    public String regexPattern = "\\s*([0-9]+|[a-zA-Z]+|\\W)\\s*";
    //public String regexPattern = "([^\\r])\\r";

    public Tokenizer() {}

    public Tokenizer(int type) 
    {
	this.parseType = type;
    }

    public Tokenizer(int type, String s) 
    {
	this.parseType = type;
	if(type == REGEX)
	    this.regexPattern = s;
	else if(type == SPLIT)
	    this.splitString = s;
    }

    /** Tokenize a string. */
    public String[] splitIntoTokens(String string)
    {
	ArrayList list = new ArrayList();
	Pattern pattern = Pattern.compile(regexPattern);
	Matcher matcher = pattern.matcher(string);
	while (matcher.find()) {
	    list.add( matcher.group(1) );
	}
	return (String[]) list.toArray( new String[list.size()] );
    }

    /** Tokenize a string. */
    public TextToken[] splitIntoTokens(Document document, String string)
    {
	TextToken[] tokenArray;
	
	if(parseType == REGEX) {
	    Pattern pattern = Pattern.compile(regexPattern);
	    Matcher matcher = pattern.matcher(string);				
	    
	    List tokenList = new ArrayList();
	    while (matcher.find())  {
		tokenList.add( new TextToken(document, matcher.start(1), matcher.end(1)-matcher.start(1)) );
	    }
	    //if (tokenList.size()==0) log.warn("empty document with id "+documentId);
	    
	    tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
	}else if (parseType == SPLIT) {
	    int currentChar = 0;
	    String[] tokenValues = string.split(splitString);

	    List tokenList = new ArrayList();
	    for(int x=0; x<tokenValues.length; x++) {
		currentChar = string.indexOf(tokenValues[x],currentChar);
		tokenList.add( new TextToken(document, currentChar, tokenValues[x].length()) );
	    }	    	    
	    tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
	} else tokenArray = null;

	return tokenArray;
    }

}