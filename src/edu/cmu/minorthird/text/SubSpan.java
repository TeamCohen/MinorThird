package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.gui.SpanViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/** A span that is a subset of another span
 *
 * @author Cameron Williams
 */

public class SubSpan extends BasicSpan 
{
    private int startIndex;

    public SubSpan(String documentId,TextToken[] textTokens,int loTextTokenIndex,int spanLen,String documentGroupId,int startIndex){
	super(documentId, textTokens, loTextTokenIndex, spanLen, documentGroupId);
	this.startIndex = startIndex;
    }
    
    public int getStartIndex() {
	return startIndex;
    }
}
