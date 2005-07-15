package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

/** Wrapper around MonotonicTextLabels that let you create and set different levels.
 *  A different level might entail a TextBase with a different tokenization or a SpanTypeTextBase
 *  where a new TextBase is created from one spanType in the original TextBase (such as bodies of
 *  emails).  Each method will be applied to the current level, not all the levels.
 *
 * @author Cameron Williams
 */


public class MultiLevelTextLabels implements MonotonicTextLabels, Serializable, Visible, Saveable
{
    private MonotonicTextLabels curLabels;
    private String curLevel;
    private HashMap levels = new HashMap(); // collection of TextLabels with different tokenizations

    public MultiLevelTextLabels(MonotonicTextLabels labels) {
	curLabels = labels;
	curLevel = "original";
	levels.put(curLevel,curLabels);
    }

    /** Returns the current level */
    public MonotonicTextLabels getLabels() {
	return curLabels;
    }

    /** Returns the original TextLabels */
    public MonotonicTextLabels getOriginal() {
	return (MonotonicTextLabels)levels.get("original");
    }

    /** Creates a new level */
    public void createLevel(String level, String tokType, String tokDef){
	TextBase newTextBase;
	TextLabels newLabels;
	if("pseudotoken".equals(tokType)) {
	    newLabels = curLabels.getTextBase().createPseudotokens(curLabels, tokDef);		    
	} else if("filter".equals(tokType)) {
	    newTextBase = new SpanTypeTextBase(curLabels, tokDef);
	    newLabels = newTextBase.importLabels((TextLabels)curLabels);
	} else {
	    Tokenizer baseTok;
	    if(tokType.equals("tokType")) {
		baseTok = new Tokenizer(Tokenizer.SPLIT, tokDef );
	    }else baseTok = new Tokenizer(Tokenizer.REGEX, tokDef); //split = re
	    newTextBase = curLabels.getTextBase().retokenize(baseTok);
	    newLabels = new BasicTextLabels(newTextBase); 		
	}
	levels.put(level, newLabels);  //Add
    }

    public void onLevel(String level) {
	if(levels.containsKey(level)) {
	    curLabels = (MonotonicTextLabels)levels.get(level);
	    curLevel = level;
	} else System.out.println("Level: " + level + " is not defined");
    }

    public void offLevel() {
	curLabels = (MonotonicTextLabels)levels.get("original");
	curLevel = "original";
    }

    public void importFromLevel(String importLevel, String oldType, String newType  ) {
	if(!levels.containsKey(importLevel))
	    System.out.println("Level: " + importLevel + " not defined for importFromLevel");
	TextLabels importLabels = (TextLabels)levels.get(importLevel);
	curLabels = (MonotonicTextLabels)(curLabels.getTextBase().importLabels(curLabels, importLabels, oldType, newType));
    }

    /** See if the TextLabels contains a particular type of annotation */
    public boolean isAnnotatedBy(String s) { return curLabels.isAnnotatedBy(s); }

    /** Ensure that this TextLabels contains a particular type of
     * annotation.  If the annotation is not present, then either load
     * it (if possible) or throw an error. */
    public void require(String annotationType,String fileToLoad) { curLabels.require(annotationType, fileToLoad); }

    /** Ensure that this TextLabels contains a particular type of
     * annotation.  If the annotation is not present, then either load
     * it (if possible) or throw an error. Use the provided
     * annotatorLoader to find annotators rather than the default
     * one. */
    public void require(String annotationType,String fileToLoad,AnnotatorLoader loader) {curLabels.require(annotationType, fileToLoad, loader); }

    /** Returns the TextBase which is annotated by this TextLabels, or null if that
     * isn't set yet. */
    public TextBase getTextBase() { return curLabels.getTextBase(); }

    /**
     *
     * @param dictionary String name of the dictionary
     * @return true if the dictionary is defined for these labels
     */
    public boolean hasDictionary(String dictionary) { return curLabels.hasDictionary(dictionary); }

    /** Returns true if the value of the Token is in the named dictionary. */
    public boolean inDict(Token token,String dict) { return curLabels.inDict(token, dict); }

    /** Get the property value associated with this TextToken.  */
    public String getProperty(Token token,String prop) { return curLabels.getProperty(token,prop); }

    /** Get a set of all properties.  */
    public Set getTokenProperties() { return curLabels.getTokenProperties(); }

    /** Get the value of the named property which has been associated with this Span.  */
    public String getProperty(Span span,String prop) { return curLabels.getProperty(span, prop); }

    /** Find all spans that have a non-null value for this property. */
    public Span.Looper getSpansWithProperty(String prop) { return curLabels.getSpansWithProperty(prop); }

    /** Find all spans in the named document that have a non-null value
     * for this property. */
    public Span.Looper getSpansWithProperty(String prop, String documentId) { return curLabels.getSpansWithProperty(prop, documentId); }

    /** Get a set of all previously-defined properties.  */
    public Set getSpanProperties() { return curLabels.getSpanProperties(); }

    /** Query if a span has a given type. */
    public boolean hasType(Span span,String type) { return curLabels.hasType(span, type); }

    /** Get all instances of a given type. */
    public Span.Looper instanceIterator(String type) { return curLabels.instanceIterator(type); }

    /** Get all instances of a given type. */
    public Span.Looper instanceIterator(String type,String documentId) { return curLabels.instanceIterator(type, documentId); }

    /** Return a set of all type names. */
    public Set getTypes() { return curLabels.getTypes(); }

    /** Return the Set of all Spans with a given type in a given document */
    public Set getTypeSet(String type,String documentId) { return curLabels.getTypeSet(type, documentId); }

    /** True if the given string names a type. */
    public boolean isType(String type) {return curLabels.isType(type); }

    /** Returns the spans s for in the given type is 'closed'. If type T
     * is close inside S, this means that one can apply the 'closed
     * world assumption' and assume that the known set of spans of type
     * T is complete, except for areas of the text that are not
     * contained by any closure span S.
     */
    public Span.Looper closureIterator(String type) {return curLabels.closureIterator(type); }

    /** Returns the spans S inside the given document in which the
     * given type is 'closed'.
     */
    public Span.Looper closureIterator(String type, String documentId) { return curLabels.closureIterator(type, documentId); }

    /** For debugging. Returns a dump of all strings that have tokens
     * with the given property. */
    public String showTokenProp(TextBase base, String prop) { return curLabels.showTokenProp(base, prop); }

    /** Retrieve additional information associated with an assertion
     * 'span S has type T'.  Returns null if the span doesn't have the
     * stated type. */
    public Details getDetails(Span span,String type) { return curLabels.getDetails(span, type); }

    /** Associate a dictionary with this labeling. */
    public void defineDictionary(String dictName, Set dictionary) { curLabels.defineDictionary(dictName, dictionary); }

    /** Assert that TextToken textToken has the given value of the given property. */
    public void setProperty(Token token,String prop,String value) { curLabels.setProperty(token, prop, value); }

    /** Assert that a token has a given property value, and associate that
     * with some detailed information.
     * If details==null, this should have the same effect as setProperty(span,prop,value).
     */
    public void setProperty(Token token,String prop,String value,Details details) { curLabels.setProperty(token, prop, value, details); }

    /** Assert that Span span has the given value of the given property */
    public void setProperty(Span span,String prop,String value) { curLabels.setProperty(span, prop, value); }

    /** Assert that Span span has the given value of the given property, 
     * and associate that with some detailed information
     */
    public void setProperty(Span span,String prop,String value,Details details) {curLabels.setProperty(span, prop, value, details); }


    /** Assert that a span has a given type. */
    public void addToType(Span span,String type) {curLabels.addToType(span, type); }

    /** Assert that a span has a given type, and associate that
     * assertion with some detailed information. 
     * If details==null, this should have the same effect as addToType(span,type).
     */
    public void addToType(Span span,String type,Details details) { curLabels.addToType(span, type, details); }

    /** Declare a new type, without asserting any spans as members. */
    public void declareType(String type) { curLabels.declareType(type); }

    /** Record that this TextLabels was annotated with some type of annotation. */
    public void setAnnotatedBy(String s) { curLabels.setAnnotatedBy(s); }

    /** Specify the AnnotatorLoader used to find Annotations when a 'require'
     * call is made. */
    public void setAnnotatorLoader(AnnotatorLoader loader) { curLabels.setAnnotatorLoader(loader); }

    /** Get the current AnnotatorLoader. */
    public AnnotatorLoader getAnnotatorLoader() { return curLabels.getAnnotatorLoader(); }

    /** Retokenizes the TextBase associated with the labels and imports the existing labels
	to the new retokenized TextBase.  Note:  Do not use this method the old labels cannot
	be translated to the new TextBase. */
    public MonotonicTextLabels retokenize(Tokenizer tok) { return curLabels.retokenize(tok); }

    public Viewer toGUI() { return new ZoomingTextLabelsViewer(curLabels); }

    //
    // Implement Saveable interface. 
    //
    static private final String FORMAT_NAME = "Minorthird MultiLevelTextLabels";
    public String[] getFormatNames() { return new String[] {FORMAT_NAME}; } 
    public String getExtensionFor(String s) { return ".labels"; }
    public void saveAs(File file,String format) throws IOException
    {
	if (!format.equals(FORMAT_NAME)) System.out.println("illegal format "+format);
	new TextLabelsLoader().saveTypesAsOps(this,file);
    }
    public Object restore(File file) throws IOException
    {
	System.out.println("Cannot load TextLabels object");
	return null;
    }
    
}