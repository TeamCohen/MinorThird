package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

/** Creates a map between two textBases, parent and child.  Span in the child textBase are always contained by the parent. 
 *  To create a map, mapDocument much be called for each document when creating the childTextBase.
 *
 * @author Cameron Williams
 */

public class TextBaseMapper {
    private TextBase parent;
    private TextBase child;
    private HashMap parentToChildMap;
    private HashMap childToParentMap;

    public TextBaseMapper(TextBase parent) {
	this.parent = parent;
	parentToChildMap = new HashMap();
	childToParentMap = new HashMap();
    }

    public void setChildTextBase(TextBase child) {
	this.child = child;
    }

    /** Adds how to map the parent document to the child document and how to map the 
     *  child document back to the parent document to the index.  
     *  Note: this must be called for every document in each TextBase to create a full map */
    public void mapDocument(String parentDocId, Span childDoc, int childCharOffset) {
	//creates parent to child map
	DocInfo childInfo = new DocInfo(childDoc.getDocumentId(), childCharOffset, childDoc.getTextToken(childDoc.size()-1).getHi());
	parentToChildMap.put(parentDocId, childInfo);

	//create child to parent map
	Span parentDoc = parent.documentSpan(parentDocId);
	DocInfo parentInfo = new DocInfo(parentDocId, childCharOffset, parentDoc.getTextToken(parentDoc.size()-1).getHi());
	childToParentMap.put(childDoc.getDocumentId(), parentInfo);	
    }

    /** Returns the matching span in the child textBase.  Returns span of zero length if child is not included in parent.
     *  Note: spans indexed by characters not tokens. */
    public Span getMatchingChildSpan(Span parentSpan) {
	String parentDocId = parentSpan.getDocumentId();
	int parentStart = parentSpan.getTextToken(0).getLo();
	int parentEnd = parentSpan.getTextToken(parentSpan.size() - 1).getHi();

	DocInfo childInfo = (DocInfo)parentToChildMap.get(parentDocId);
	Span childDoc = child.documentSpan(childInfo.docId);
	int childStart = 0;
	int childEnd = childInfo.size;
	
	//check if child span is in parent
	if((parentEnd < childInfo.offset) || (parentStart > childInfo.offset+childInfo.size)) {
	    return childDoc.subSpan(0,0);
	}

	//find start and end chars in child span
	if (parentStart > childInfo.offset)
	    childStart = parentStart - childInfo.offset;
	if (parentEnd < childInfo.offset+childInfo.size)
	    childEnd = parentEnd - childInfo.offset;

	return childDoc.charIndexSubSpan(childStart, childEnd);
    }

    /** Returns the matching span in the parent textBase.  */
    public Span getMatchingParentSpan(Span childSpan) {
	String childDocId = childSpan.getDocumentId();
	DocInfo parentInfo = (DocInfo)childToParentMap.get(childDocId);
	Span parentDoc = parent.documentSpan(parentInfo.docId);
	int parentStart = parentInfo.offset + childSpan.getTextToken(0).getLo();
	int parentEnd = parentInfo.offset + childSpan.getTextToken(childSpan.size()-1).getHi();

	return parentDoc.charIndexSubSpan(parentStart, parentEnd);
    }
    
    public class DocInfo {
	public String docId;
	public int offset;
	public int size;
	public DocInfo (String docId, int offset, int size) { this.docId=docId; this.offset = offset; this.size=size; }
    }
}