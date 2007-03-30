/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

public abstract class CompoundTokenizer implements Tokenizer
{
    protected Tokenizer parentTokenizer;

    // Concrete methods
    public Tokenizer getParent() { return parentTokenizer; }

    // Abstract methods to be implemented by subclasses
    public abstract String[] splitIntoTokens(String string);
    public abstract TextToken[] splitIntoTokens(Document document);
}
