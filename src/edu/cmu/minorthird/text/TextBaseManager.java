package edu.cmu.minorthird.text;

/**
 *  Manages the mappings between TextBases.
 *
 * @author Quinten Mercer
 */

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class TextBaseManager {

    private HashMap textBases = new HashMap();
    private HashMap textBaseMappers = new HashMap();
    
    /** Creates an empty TextBaseManager */
    public TextBaseManager() {;}

    /**
     * Creates a new TextBaseManager using the specified textbase as the root textbase
     * and "root" as the name to identify it.
     */
    public TextBaseManager(TextBase rootBase) {
        textBases.put("root", new TextBaseEntry("root", rootBase, 0, null));
    }

    /**
     * Creates a new TextBaseManager using the specified textbase as the root textbase and
     * the specified name is used in place of "root" to identify it.
     */
    public TextBaseManager(String rootBaseName, TextBase rootBase) {
        textBases.put(rootBaseName, new TextBaseEntry(rootBaseName, rootBase, 0, null));
    }

    /** Returns a boolean indicating whether or not this manager has a level with the specified name */
    public boolean containsLevel(String levelName) {
        return textBases.containsKey(levelName);
    }

    /** Returns the textbase identified by name. */
    public TextBase getTextBase(String name) {
        TextBaseEntry entry = (TextBaseEntry)textBases.get(name);  
        return entry.getTextBase();
    }

    /** 
     * Adds a textbase to the manager that is a child of parentName.  Null parentName
     * creates a new root textbase.  Note that a single manager can maintain multiple 
     * sets of textbases by adding multiple root textbases.
     */
    private void addTextBase(String parentName, String childName, TextBase childTextBase, TextBaseMapper mapper) {
        TextBaseEntry parentEntry = null;

        // Make sure that there is not a textbase being managed with the desired child name.
        if (textBases.get(childName) != null)
            throw new IllegalArgumentException("TextBase already exists with name: " + childName);

        // Get the entry of the parent
        if (parentName != null)
            parentEntry = (TextBaseEntry)textBases.get(parentName);
        
        // Add the new text base and it's mapper to the store of text bases and mappers        
        if (parentEntry != null) {  // There's a parent so create a child level
            int parentLevel = parentEntry.getLevel();
            textBases.put(childName, new TextBaseEntry(childName, childTextBase, parentLevel+1, parentEntry));
            textBaseMappers.put(childName, mapper);
        }
        else { // There is no parent so create a root level.
            textBases.put(childName, new TextBaseEntry(childName, childTextBase, 0, null));
            textBaseMappers.put(childName, mapper);
        }
    }

    /**
     * Sometimes you may not have a source span, but rather only have a char offset in the source doc.  There
     * are two scenarios where this could happen.  First, it may be the case that you really just want to map
     * some char offset of an existing document.  In this case this method will simply get the documentSpan 
     * for the doc, use Span.charIndexSubSpan to create a span to map, and then forward the call to the 
     * getMatchingSpan method that takes a source Span instance.  The other situation is where you may need to
     * map sequences of chars before the document is actually in a TextBase.  For instance, FilterTokenizer 
     * needs to map char sequences in order to tokenize a document.  This works because you can create maps 
     * between documents in two text bases even if the destination document doesn't yet exist in the TextBase.
     * To make it happed, this method first maps the char offset to a span in it's parent, then calls 
     * getMatchingSpan to propagate the mapping down to the destination textbase.
     */
    public Span getMatchingSpan(String srcName, String srcDocId, int srcOffset, int length, String dstName) {
        TextBaseEntry srcEntry = (TextBaseEntry)textBases.get(srcName);        
        if (srcEntry == null)
            throw new IllegalArgumentException("There is no text base named: " + srcName + " in this manager.");

        // First try to get the document span for the source document
        Span srcDocSpan = srcEntry.getTextBase().documentSpan(srcDocId);
        Span srcSpan;

        if (srcDocSpan != null) {
            return this.getMatchingSpan(srcDocSpan.charIndexSubSpan(srcOffset, srcOffset+length), srcName, dstName);
        }

        // If the document is unavailable then get the mapper that maps between the source and dest text bases.
        TextBaseMapper mapper = (TextBaseMapper)textBaseMappers.get(srcEntry.getName());
        if (mapper == null)
            return null;
        // Get the mapping for the char index sequence in the source document to its parent.
        TextBaseMapper.MapEntry mapping = mapper.getChildMapping(srcDocId, srcOffset, length);
        // If no mapping could be found just return null
        if (mapping == null)
            return null;
        
        // Extract the info from the mapping to create a span in the parent document
        String parentDocId = mapping.dstDocId;
        int parentOffset = mapping.dstOffset + (srcOffset - mapping.srcOffset);
        
        // Get the span in the parent document that corresponds to this char index sequence
        Span parentSpan = srcEntry.getParent().getTextBase().documentSpan(parentDocId).charIndexSubSpan(parentOffset, parentOffset+length);
        
        // Finally, map this span to the destination level using the normal mechanisms.
        return this.getMatchingSpan(parentSpan, srcEntry.getParent().getName(), dstName);

        //Span srcSpan = srcEntry.getTextBase().documentSpan(srcDocId).charIndexSubSpan(srcOffset, srcOffset+length);
        //return this.getMatchingSpan(srcSpan, srcName, dstName);
    }


    /**
     * Finds a mapping path from the source text base to the destination textbase and translates
     * the specified span through each successive mapping until the coresponding span in the 
     * destination text base is located.
     */
    public Span getMatchingSpan(Span span, String srcName, String dstName) {
        TextBaseEntry srcEntry = (TextBaseEntry)textBases.get(srcName);
        TextBaseEntry dstEntry = (TextBaseEntry)textBases.get(dstName);
        if (srcEntry == null)
            throw new IllegalArgumentException("There is no text base named: " + srcName + " in this manager.");
        if (dstEntry == null)
            throw new IllegalArgumentException("There is no text base named: " + dstName + " in this manager.");
        if (srcEntry.getTextBase().getDocument(span.getDocumentId()) == null)
            throw new IllegalArgumentException("The document that the specified span refers to is not in the source text base.");

        // Lists to store the path from both text bases to one that is common between them
        ArrayList srcMapperList = new ArrayList();
        ArrayList dstMapperList = new ArrayList();

        // Generate a path of mappers that links from the src text base to the dst text base
        TextBaseEntry currSrcEntry = (TextBaseEntry)textBases.get(srcName);
        TextBaseEntry currDstEntry = (TextBaseEntry)textBases.get(dstName);
        while (currSrcEntry.getLevel() != currDstEntry.getLevel()) {
            if (currSrcEntry.getLevel() > currDstEntry.getLevel()) {
                srcMapperList.add(textBaseMappers.get(currSrcEntry.getName()));
                currSrcEntry = currSrcEntry.getParent();
            }
            else {
                dstMapperList.add(textBaseMappers.get(currDstEntry.getName()));
                currDstEntry = currDstEntry.getParent();
            }
        }
        while (currSrcEntry != currDstEntry) {
            srcMapperList.add(textBaseMappers.get(currSrcEntry.getName()));
            currSrcEntry = currSrcEntry.getParent();
            dstMapperList.add(textBaseMappers.get(currDstEntry.getName()));
            currDstEntry = currDstEntry.getParent();
        }

        // Now follow that path from src to dst mapping the span to each intermediate text base 
        // until we ultimately end up with the span in the dst text base.  If at anytime we
        // encounter a null value for a mapped span, this indicates that there is no mapping 
        // for this span between the source and destination text bases so return null
        Span matchingSpan = span;
        Iterator srcIterator = srcMapperList.iterator();
        while (srcIterator.hasNext()) {
            TextBaseMapper currMapper = (TextBaseMapper)srcIterator.next();
            matchingSpan = currMapper.getMappedParentSpan(matchingSpan);
            if (matchingSpan == null)
                return null;
        }
        Iterator dstIterator = dstMapperList.iterator();
        while (dstIterator.hasNext()) {
            TextBaseMapper currMapper = (TextBaseMapper)dstIterator.next();
            matchingSpan = currMapper.getMappedChildSpan(matchingSpan);
            if (matchingSpan == null)
                return null;
        }

        return matchingSpan;
    }

    public MutableTextBase retokenize(Tokenizer newTokenizer, String parentLevelName, String  newLevelName) {

        TextBaseEntry parentEntry = (TextBaseEntry)textBases.get(parentLevelName);
        if (parentEntry == null)
            throw new IllegalArgumentException("There is no text base named: " + parentLevelName + " in this manager.");

        BasicTextBase newTextBase = new BasicTextBase(newTokenizer);
        TextBaseMapper newMapper =  new TextBaseMapper(parentEntry.getTextBase(), newTextBase); 
        addTextBase(parentLevelName, newLevelName, newTextBase, newMapper);

        Span.Looper docsLooper = ((TextBaseEntry)textBases.get(parentLevelName)).getTextBase().documentSpanIterator();
        while (docsLooper.hasNext()) {
            Span currDocSpan = docsLooper.nextSpan();
            newTextBase.loadDocument(currDocSpan.getDocumentId(), currDocSpan.getDocumentContents());

            // Retokenizing does NOT change the underlying document structure so all we need to do is add a single 
            // map entry that maps position 0 from the parent text base to position 0 in the child text base.  Also
            // the documentIds don't change in the new textbase.
            newMapper.mapPlace(currDocSpan.getDocumentId(), 0, currDocSpan.getDocumentId(), 0);
        }
        return newTextBase;
    }

    public TextBase filter(String parentLevelName, TextLabels parentLabels, String newLevelName, String spanType) {

        BasicTextBase newTextBase = new BasicTextBase(new FilterTokenizer(this, newLevelName, parentLevelName));
        TextBaseMapper newMapper = new TextBaseMapper(parentLabels.getTextBase(), newTextBase);
        addTextBase(parentLevelName, newLevelName, newTextBase, newMapper);

        Span.Looper typeInstances = parentLabels.instanceIterator(spanType);
        String prevDocId = ""; //useful for checking whether the next span is in the same doc
        int docNum = 0; //counts how many spans have the type in each document
        while( typeInstances.hasNext() ) {
            Span currInstance = typeInstances.nextSpan();
            String curDocId = currInstance.getDocumentId();

            // This code assumes that the TextBase.instanceIterator method returns the spans ordered
            // by document ID.  This method makes NO guarantee that this will be true.
            if(curDocId.equals(prevDocId))
                docNum++;
            else docNum = 0;

            String newDocID = "childTB" + docNum + "-" + curDocId;

            // Map the doc span in the old text base to the correct document in the new text base.  No offset 
            // is required in the new doc since it we are just chopping up the original doc into pieces.
            newMapper.mapPlace(curDocId, currInstance.getLoChar(), newDocID, 0);

            prevDocId = curDocId;
            String newDocText = currInstance.asString();
            int startIndex = currInstance.getLoChar();
            newTextBase.loadDocument(newDocID, newDocText, startIndex);
        }
        return newTextBase;
    }

    // 
    // Used internally to help manage the set of TextBases
    //
    private class TextBaseEntry {
        private String entryName;
        private TextBase textBase;
        private TextBaseEntry parent;
        private int level;
        
        public TextBaseEntry(String newEntryName, TextBase newTextBase, int newLevel, TextBaseEntry newParent) {
            entryName = newEntryName;
            textBase = newTextBase;
            level = newLevel;
            parent = newParent;
        }
        public String getName() { return entryName; }
        public TextBase getTextBase() { return textBase; }
        public int getLevel() { return level; }
        public TextBaseEntry getParent() { return parent; }
    }


    //
    // Used internally to create the map between two textBases.
    //
    private class TextBaseMapper {
        private TextBase parent;
        private TextBase child;
        private HashMap parentToChildMap;
        private HashMap childToParentMap;
        
        public TextBaseMapper(TextBase parent, TextBase child) {
            this.parent = parent;
            this.child = child;
            this.parentToChildMap = new HashMap();
            this.childToParentMap = new HashMap();
        }
        
        public void mapPlace(String parentDocId, int parentOffset, String childDocId, int childOffset) {
            TreeSet parentEntry = (TreeSet)parentToChildMap.get(parentDocId);
            if (parentEntry == null) {
                parentEntry = new TreeSet();
                parentToChildMap.put(parentDocId, parentEntry);
            }
            parentEntry.add(new MapEntry(parentDocId, parentOffset, childDocId, childOffset));

            TreeSet childEntry = (TreeSet)childToParentMap.get(childDocId);
            if (childEntry == null) {
                childEntry = new TreeSet();
                childToParentMap.put(childDocId, childEntry);
            }
            childEntry.add(new MapEntry(childDocId, childOffset, parentDocId, parentOffset));
        }

        public MapEntry getParentMapping(String parentDocId, int parentOffset, int length) {
            TreeSet parentDocMap = (TreeSet)parentToChildMap.get(parentDocId);
            if (parentDocMap == null)
                throw new IllegalArgumentException("Document containing parent char sequence has no mappings.");
            
            // Iterate through this document's map entries until we find the entry that contains the entire parent span.
            // If there is no entry that contains the parent span, then give an error.  The entry is found by finding the
            // first entry whose offset is greater than both the start and end of the parent, then the previous entry has
            // the info we need.
            Iterator it = parentDocMap.iterator();
            MapEntry curr = null, parentEntry = null;
            while (it.hasNext()) {
                curr = (MapEntry)it.next();
                // If the current entry is before the start of the parent span update the parentEntry
                if (curr.srcOffset <= parentOffset) {
                    parentEntry = curr;
                }
                else if (curr.srcOffset < (parentOffset+length)) {
                    return null;
                }
            }
            return parentEntry;
        }

        public MapEntry getChildMapping(String childDocId, int childOffset, int length) {
            TreeSet childDocMap = (TreeSet)childToParentMap.get(childDocId);
            if (childDocMap == null)
                throw new IllegalArgumentException("Document containing child char sequence has no mappings.");

            // Iterate through this document's map entries until we find the entry that contains the entire parent span.
            // If there is no entry that contains the parent span, then give an error.  The entry is found by finding the
            // first entry whose offset is greater than both the start and end of the parent, then the previous entry has
            // the info we need.
            Iterator it = childDocMap.iterator();
            MapEntry curr = null, childEntry = null;
            while (it.hasNext()) {
                curr = (MapEntry)it.next();

                // If the current entry is before the start of the parent span update the childEntry
                if (curr.srcOffset <= childOffset) {
                    childEntry = curr;
                }
                else if (curr.srcOffset < (childOffset+length)) {
                    return null;
                }
            }
            return childEntry;
        }

        public Span getMappedChildSpan(Span parentSpan) {
            if (parent.getDocument(parentSpan.getDocumentId()) == null)
                throw new IllegalArgumentException("Document containing parent span not in the child text base of this mapper.");
                        
            int parentLo = parentSpan.getTextToken(0).getLo();
            int parentHi = parentSpan.getTextToken(parentSpan.size()-1).getHi();
            
            MapEntry parentEntry = this.getParentMapping(parentSpan.getDocumentId(), parentLo, parentHi-parentLo);
            
            // If no approptiate entry was found that maps the parent span, then there is no mapping for this
            // span between these two text bases so just return null.
            if (parentEntry == null)
                return null;
                        
            // Otherwise compute the index offsets for the new (mapperd) span as follows:
            // lo index: the mapped offset (destination) from the entry
            return child.documentSpan(parentEntry.dstDocId).charIndexSubSpan(parentEntry.dstOffset + (parentLo-parentEntry.srcOffset), 
                                                                             parentEntry.dstOffset + (parentHi-parentEntry.srcOffset));        
        }
        
        public Span getMappedParentSpan(Span childSpan) {
            if (child.getDocument(childSpan.getDocumentId()) == null)
                throw new IllegalArgumentException("Document containing child span not in the parent text base of this mapper.");
                        
            int childLo = childSpan.getTextToken(0).getLo();
            int childHi = childSpan.getTextToken(childSpan.size()-1).getHi();

            MapEntry childEntry = this.getChildMapping(childSpan.getDocumentId(), childLo, childHi-childLo);
                        
            // If no approptiate entry was found that maps the parent span, then there is no mapping for this
            // span between these two text bases so just return null.
            if (childEntry == null)
                return null;
            
            // Otherwise compute the index offsets for the new (mapped) span as follows:
            // lo index: the mapped offset (destination) from the entry
            return parent.documentSpan(childEntry.dstDocId).charIndexSubSpan(childEntry.dstOffset + (childLo-childEntry.srcOffset), 
                                                                             childEntry.dstOffset + (childHi-childEntry.srcOffset));
        }


        /**
         * Used for debugging purposes.
         */
        public void printMap() {
            System.out.println("****************************************************");
            System.out.println("*** Mapper Between Parent: " + parent + " and Child: " + child + " ***");
            System.out.println("***                                              ***");
            System.out.println("*** Parent To Child mappings:                    ***");            

            Iterator keyIterator = parentToChildMap.keySet().iterator();
            while (keyIterator.hasNext()) {
                String currKey = (String)keyIterator.next();
                TreeSet currDocMapings = (TreeSet)parentToChildMap.get(currKey);
                Iterator mappingsIterator = currDocMapings.iterator();
                while (mappingsIterator.hasNext()) {
                    System.out.println("*** " + (MapEntry)mappingsIterator.next() + " ***");
                }
            }
            System.out.println("***                                              ***");            
            System.out.println("*** Child To Parent mappings:                    ***");            

            keyIterator = childToParentMap.keySet().iterator();
            while (keyIterator.hasNext()) {
                String currKey = (String)keyIterator.next();
                TreeSet currDocMapings = (TreeSet)childToParentMap.get(currKey);
                Iterator mappingsIterator = currDocMapings.iterator();
                while (mappingsIterator.hasNext()) {
                    System.out.println("*** " + (MapEntry)mappingsIterator.next() + " ***");
                }
            }
            System.out.println("****************************************************\n\n");
        }

        public class MapEntry implements Comparable {
            public String srcDocId;
            public int srcOffset;
            public String dstDocId;
            public int dstOffset;
            
            public MapEntry(String sid, int sos, String did, int dos) {
                srcDocId = sid;
                srcOffset = sos;
                dstDocId = did;
                dstOffset = dos;
            }
            public int compareTo(Object o) {
                int res = srcDocId.compareTo(((MapEntry)o).srcDocId);
                if (res == 0)
                    res = srcOffset - ((MapEntry)o).srcOffset;
                return res;
                
            }
            public String toString() {
                return srcDocId + ":" + srcOffset + " -> " + dstDocId + ":" + dstOffset;
            }
        }
    }
}
