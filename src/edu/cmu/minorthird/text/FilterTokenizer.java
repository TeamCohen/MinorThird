/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * This implementation of the Tokenizer interface is used for filtering a text base based on
 * a specified spantype.  It is a trivial tokenizer in the sense that it takes a document from
 * the new text base, maps it to the old text base and copies over the tokens.  If the mapping
 * is not found (ie if the document being added is not in the parent text base) then the 
 * parent tokenizer is used.
 *
 * @author Quinten Mercer
 */
public class FilterTokenizer extends CompoundTokenizer
{
    private static Logger log = Logger.getLogger(FilterTokenizer.class);
    
    private TextBaseManager textBaseManager;
    private String parentLevelName;
    private String levelName;

    public FilterTokenizer(TextBaseManager tbMan, String levelName, String parentLevelName) { 
        this.textBaseManager = tbMan;
        this.levelName = levelName;
        this.parentLevelName = parentLevelName;
        this.parentTokenizer = tbMan.getTextBase(parentLevelName).getTokenizer();
    }

    /** Tokenize a string */
    @Override
		public String[] splitIntoTokens(String string) {
        return parentTokenizer.splitIntoTokens(string);
    }

    /** Tokenize a document. */
    @Override
		public TextToken[] splitIntoTokens(Document document) {
        Span matchingParentSpan = textBaseManager.getMatchingSpan(levelName, document.getId(), 0, document.getText().length(), parentLevelName);

        if (matchingParentSpan != null) {
            SortedSet<TextToken> tokens = new TreeSet<TextToken>();
            int currOffset = 0;
            for (int i=0;i<matchingParentSpan.size();i++) {
                // Get the next token in the matching parent span
                TextToken currParentToken = matchingParentSpan.getTextToken(i);
                // Figure out the offset in the doc where this token begins 
                currOffset = currParentToken.getLo() - matchingParentSpan.getTextToken(0).getLo();
                // Copy this token over to array of tokens for the new document.
                tokens.add(new TextToken(document, currOffset, currParentToken.getLength()));
            }
            return tokens.toArray(new TextToken[0]);
        }
        else {
            log.warn("Matching span could not be found for document span for doc: " + document);
            return parentTokenizer.splitIntoTokens(document);
        }
    }
}
