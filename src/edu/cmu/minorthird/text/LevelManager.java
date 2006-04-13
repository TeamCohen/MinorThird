package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

/** Creates and keeps track of all levels and their corresponding TextLabels and TextBaseMappers.  The TextBaseMappers
 *  are created for each parent-child pair and contain the information on how to map spans from the parent to the child
 *  and vise versa.
 *
 * @author Cameron Williams
 */

public class LevelManager {
    static private Logger log = Logger.getLogger(LevelManager.class);

    private MonotonicTextLabels root;
    private HashMap levels = new HashMap(); // collection of TextLabels with TextBases with different tokenizations
    private HashMap mappers = new HashMap(); //stores all the TextBaseMappers for each child

    public LevelManager(MonotonicTextLabels rootLabels) {
	this.root = rootLabels;
	levels.put("root", rootLabels);
    }
    
    /** Creates a new level and imports labels from parent.  Where levelType defines whether to create a retokenized, a spanType, or
     *  a pseudotoken textBase and pattern is the regular expression or spanType to use to create the new TextBase. */
    public void createLevel(String newLevelName, String levelType, String pattern){
	TextBase newTextBase = root.getTextBase();
	MonotonicTextLabels newLabels = root;
	TextBaseMapper tbmapper = new TextBaseMapper(root.getTextBase());
	if("pseudotoken".equals(levelType)) { // creates a textBase where spans of a certain type are combined into a sigle token 
	    newLabels = newTextBase.createPseudotokens(root, pattern, tbmapper);
	    newTextBase = newLabels.getTextBase();
	} else if("filter".equals(levelType)) { // creates a textBase which filters out all spans with a certain spanType
	    newTextBase = new SpanTypeTextBase(root, pattern, tbmapper);
	    newLabels = new BasicTextLabels(newTextBase);
	} else if("re".equals(levelType) || "split".equals(levelType)){
	    Tokenizer baseTok;
	    if(levelType.equals("split")) {  // creates a tokenizer that splits the textBase at a certain token (e.g. split at ".")
		baseTok = new Tokenizer(Tokenizer.SPLIT, pattern );
	    }else baseTok = new Tokenizer(Tokenizer.REGEX, pattern); //split = regular expression		
	    newTextBase = root.getTextBase().retokenize(baseTok, tbmapper);	    
	    newLabels = new BasicTextLabels(newTextBase);
	} else {
	    log.warn("No level type: " + levelType + " new level created with old textBase and Labels");
	}
	tbmapper.setChildTextBase(newTextBase);	
	importParentLabels(root, newLabels, tbmapper);
	mappers.put(newLevelName, tbmapper);
	levels.put(newLevelName, newLabels);
    }

    /** Imports labels from parent */
    public void importParentLabels(MonotonicTextLabels parentLabels, MonotonicTextLabels childLabels, TextBaseMapper mapper) {
	Span.Looper docIterator = parentLabels.getTextBase().documentSpanIterator();
	Set types = parentLabels.getTypes();     
	while(docIterator.hasNext()) {
	    Span docSpan = docIterator.nextSpan();
	    String docID = docSpan.getDocumentId();
	    Iterator typeIterator = types.iterator();
	    while(typeIterator.hasNext()) {
		String type = (String)typeIterator.next();
		// bug: can't cast to BasicTextLabels!
		Set spansWithType = parentLabels.getTypeSet(type, docID);
		Iterator spanIterator = spansWithType.iterator();
		while(spanIterator.hasNext()) {
		    Span s = (Span)spanIterator.next();
		    Span childSpan = mapper.getMatchingChildSpan(s);
		    childLabels.addToType(childSpan, type);
		}
	    }
	}
    }

    /** Returns the TextBaseMapper for the parent-child textbase pair.
     *  Note Mappers are indexed by child because every child can only have one parent */
    public TextBaseMapper getTextBaseMapper(String child){
	return (TextBaseMapper)mappers.get(child);
    }

    /** Returns whether level has been created */
    public boolean containsLevel(String level) {
	return levels.containsKey(level);
    }
    
    /** Returns the textLabels associated with the level */
    public MonotonicTextLabels getLevel(String level) { 
	return (MonotonicTextLabels)levels.get(level); 
    }
}

