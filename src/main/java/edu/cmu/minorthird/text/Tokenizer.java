/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

public interface Tokenizer 
{
    /** Tokenize a string. */
    public String[] splitIntoTokens(String string);

    /** Tokenize a document. */
    public TextToken[] splitIntoTokens(Document document);
}
