/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.*;
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
    private static Logger log = Logger.getLogger(Tokenizer.class);

    /** How to split tokens up */
    public static final String TOKEN_REGEX_PROP = "edu.cmu.minorthird.tokenRegex";
    public static final String TOKEN_REGEX_DEFAULT_VALUE = "\\s*([0-9]+|[a-zA-Z]+|\\W)\\s*";

    public static String standardTokenRegexPattern;
    static {
        Properties props = new Properties();
	try {
	    InputStream in = FancyLoader.class.getClassLoader().getResourceAsStream("token.properties");
	    if (in != null) {
		props.load(in);
		log.debug("loaded properties from stream "+in);
	    } else {
		log.info("no token.properties found on classpath");
	    }
        } catch (Exception ex) {
            log.debug("can't open token.properties:"+ex);
        }
        standardTokenRegexPattern = 
            props.getProperty(TOKEN_REGEX_PROP, System.getProperty(TOKEN_REGEX_PROP,TOKEN_REGEX_DEFAULT_VALUE));
        log.info("tokenization regex: "+standardTokenRegexPattern);
    }

    public final static int REGEX = 0;
    public final static int SPLIT = 1;
    public int parseType = REGEX;

    public String splitString = " ";
    public String regexPattern = standardTokenRegexPattern;

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

    /** Tokenize a string that is only part of a document... pass in the start pos of the string in the doc. */
    public TextToken[] splitIntoTokens(Document document, String string, int startPos)
    {
	List tokenList = new ArrayList();
	TextToken[] tokenArray;
	if(parseType == REGEX) {
	    Pattern pattern = Pattern.compile(regexPattern);
	    Matcher matcher = pattern.matcher(string);				
	    	    
	    while (matcher.find())  {
		tokenList.add( new TextToken(document, matcher.start(1)+startPos, matcher.end(1)-matcher.start(1)) );
	    }
	    tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
	}else if (parseType == SPLIT) {
	    int currentChar = 0;
	    String[] tokenValues = string.split(splitString);

	    for(int x=0; x<tokenValues.length; x++) {
		currentChar = string.indexOf(tokenValues[x],currentChar);
		tokenList.add( new TextToken(document, currentChar+startPos, tokenValues[x].length()) );
	    }	    
	    tokenArray = (TextToken[])tokenList.toArray(new TextToken[0]);
	} else tokenArray = null;
	return tokenArray;
    }

}
