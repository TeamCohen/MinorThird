package edu.cmu.minorthird.text;

import java.util.Iterator;
import java.util.Set;

/** 
 * Access assertions about 'types' and 'properties' of
 * contiguous Spans of these Seq's.  TextLabels's are immutable.
 *
 * @author William Cohen
*/

public interface TextLabels
{
    /** See if the TextLabels contains a particular type of annotation */
    public boolean isAnnotatedBy(String s);
    
    /** Ensure that this TextLabels contains a particular type of
     * annotation.  If the annotation is not present, then either load
     * it (if possible) or throw an error. */
    public void require(String annotationType,String fileToLoad);
    
    /** Annotate labels with annotator named fileToLoad */
    public void annotateWith(String annotationType, String fileToLoad);
    
    /** Ensure that this TextLabels contains a particular type of
     * annotation.  If the annotation is not present, then either load
     * it (if possible) or throw an error. Use the provided
     * annotatorLoader to find annotators rather than the default
     * one. */
    public void require(String annotationType,String fileToLoad,AnnotatorLoader loader);
    
    /** Returns the TextBase which is annotated by this TextLabels, or null if that
     * isn't set yet. */
    public TextBase getTextBase();
    
    /**
     *
     * @param dictionary String name of the dictionary
     * @return true if the dictionary is defined for these labels
     */
    public boolean hasDictionary(String dictionary);

    /** Returns true if the value of the Token is in the named dictionary. */
    public boolean inDict(Token token,String dict);
    
    /** Get the property value associated with this TextToken.  */
    public String getProperty(Token token,String prop);
    
    /** Get a set of all properties.  */
    public Set<String> getTokenProperties();
    
    /** Get the value of the named property which has been associated with this Span.  */
    public String getProperty(Span span,String prop);
    
    /** Find all spans that have a non-null value for this property. */
    public Iterator<Span> getSpansWithProperty(String prop);
    
    /** Find all spans in the named document that have a non-null value
     * for this property. */
    public Iterator<Span> getSpansWithProperty(String prop, String documentId);
    
    /** Get a set of all previously-defined properties.  */
    public Set<String> getSpanProperties();
    
    /** Query if a span has a given type. */
    public boolean hasType(Span span,String type);
    
    /** Get all instances of a given type. */
    public Iterator<Span> instanceIterator(String type);
    
    /** Get all instances of a given type. */
    public Iterator<Span> instanceIterator(String type,String documentId);
    
    /** Return a set of all type names. */
    public Set<String> getTypes();
    
    /** Return the Set of all Spans with a given type in a given document */
    public Set<Span> getTypeSet(String type,String documentId);
    
    /** True if the given string names a type. */
    public boolean isType(String type);
    
    /** Returns the spans s for in the given type is 'closed'. If type T
     * is close inside S, this means that one can apply the 'closed
     * world assumption' and assume that the known set of spans of type
     * T is complete, except for areas of the text that are not
     * contained by any closure span S.
     */
    public Iterator<Span> closureIterator(String type);
    
    /** Returns the spans S inside the given document in which the
     * given type is 'closed'.
     */
    public Iterator<Span> closureIterator(String type, String documentId);
    
    /** For debugging. Returns a dump of all strings that have tokens
     * with the given property. */
    public String showTokenProp(TextBase base, String prop);
    
    /** Retrieve additional information associated with an assertion
     * 'span S has type T'.  Returns null if the span doesn't have the
     * stated type. */
    public Details getDetails(Span span,String type);
}
