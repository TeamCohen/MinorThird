package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.gui.SpanViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/** Implements the Span interface.
 *
 * @author William Cohen
*/

public class BasicSpan implements Span,Serializable,Visible
{
	static private Logger log = Logger.getLogger(BasicSpan.class);
	static private final boolean DEBUG = log.isDebugEnabled();

  static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private String documentId;
	private String documentGroupId;
 	private int loTextTokenIndex;
	private int spanLen;
	private TextToken[] textTokens;

	/** Constructor assumes that the textTokens are all from the documentId.
	 */
	public BasicSpan(
		String documentId,TextToken[] textTokens,int loTextTokenIndex,int spanLen,String documentGroupId)
	{
		this.documentId = documentId;
		this.textTokens = textTokens;
		this.loTextTokenIndex = loTextTokenIndex;
		this.spanLen = spanLen;
		this.documentGroupId = documentGroupId==null ? documentId : documentGroupId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public String getDocumentGroupId() {
		return documentGroupId;
	}

  public String getDocumentContents() {
    if (textTokens.length==0) return "";
    else return textTokens[0].getDocument();
  }

	public int size() { 
		return spanLen; 
	}

	public TextToken getTextToken(int i) {
		if (i<0 || i>=spanLen) throw new IllegalArgumentException("out of range: "+i);
		return textTokens[loTextTokenIndex+i]; 
	}

	public Token getToken(int i) {
	    //return getTextToken(i);
	    if (i<0 || i>=spanLen) throw new IllegalArgumentException("out of range: "+i);
	    return textTokens[loTextTokenIndex+i]; 
	}

	/** Create a subspan of this span, covering the indicated TextToken's. */
	public Span subSpan(int start,int len) {
		if (start<0 || start+len>spanLen) throw new IllegalArgumentException("out of range: "+start+","+len);
		return new BasicSpan(documentId,textTokens,loTextTokenIndex+start,len,documentGroupId);
	}

	/** A larger span containing this span. */
	public Span documentSpan() {
		return new BasicSpan(documentId,textTokens,0,textTokens.length,documentGroupId);
	}

	/** The index of this span in the home span. */
	public int documentSpanStartIndex() {
		return loTextTokenIndex;
	}

	public boolean contains(Span other) {
		if (!other.getDocumentId().equals(getDocumentId())) return false;
		int myStart = documentSpanStartIndex();
		int otherStart = other.documentSpanStartIndex();
		int myEnd = documentSpanStartIndex() + size();
		int otherEnd = other.documentSpanStartIndex() + other.size();
		return (myStart<=otherStart && myEnd>=otherStart && myStart<=otherEnd && myEnd>=otherEnd);
	}

	public boolean overlaps(Span other) {
		if (!other.getDocumentId().equals(getDocumentId())) return false;
		int myStart = documentSpanStartIndex();
		int otherStart = other.documentSpanStartIndex();
		int myEnd = documentSpanStartIndex() + size();
		int otherEnd = other.documentSpanStartIndex() + other.size();
		return (myStart<=otherStart && myEnd>=otherStart  // [ ... ( ... ] - partial containment 1
						|| myStart<=otherEnd && myEnd>=otherEnd   // [ ... ) ... ] - partial containment 2
						|| other.contains(this)                   // ( .. [ ... ] ... ) - containment 
			);
	}

	/** Find the string contained in a Span. */
	public String asString() {
		if (size()<=0) return "";
		else {
			TextToken lo = getTextToken(0);
			TextToken hi = getTextToken(size()-1);
			return lo.getDocument().substring( lo.getLo(), hi.getHi() );
		}
	}
	
	/** A length-zero span for the left boundary */
	public Span getLeftBoundary()	{
		return new BasicSpan(documentId,textTokens,loTextTokenIndex,0,documentGroupId);
	}
	
	/** A length-zero span for the left boundary */
	public Span getRightBoundary() {
		return new BasicSpan(documentId,textTokens,loTextTokenIndex+spanLen,0,documentGroupId);
	}


	

	// Implement comparable
	public int compareTo(Object o) {
		Span other = (Span) o;
		int cmp1 = getDocumentId().compareTo(other.getDocumentId());
		if (cmp1!=0) return cmp1;
		int cmp2 = documentSpanStartIndex() - other.documentSpanStartIndex();
		if (cmp2!=0) return cmp2;		
		int cmp3 = size() - other.size();
		if (cmp3!=0) return cmp3;
		return 0;
	}
	
	// for safe hashing
	public int hashCode() {
		return documentId.hashCode() ^ loTextTokenIndex ^ spanLen;
	}
	public boolean equals(Object o) {
		return compareTo(o)==0;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer("");
		buf.append("Span '"+asString()+"'");
		buf.append(" = tokens "+loTextTokenIndex+":"+(loTextTokenIndex+spanLen)+" in ");
		buf.append(documentId+"/"+documentGroupId);
		return buf.toString();
	}

	public Span charIndexSubSpan(int lo,int hi) {
		return charIndexSubSpan(lo,hi,false);
	}
	public Span charIndexProperSubSpan(int lo,int hi) {
		return charIndexSubSpan(lo,hi,true);		
	}

	/** Converts from a span in character offsets within a document
	 * Span to a token span for that document Span. */
	private Span charIndexSubSpan(int lo,int hi,boolean proper) 
	{
		// find token that start & end closest to lo and hi
		int minStartDist = Integer.MAX_VALUE;
		int minEndDist = Integer.MAX_VALUE;
		int firstTextToken = -1, lastTextToken = -1;
		for (int i=0; i<size(); i++) {
			if (!proper) {
				if (DEBUG) log.debug("considering token '"+getTextToken(i)
														 +"' from lo="+getTextToken(i).getLo()
														 +" to hi="+getTextToken(i).getHi());
				int startDist = distance( getTextToken(i).getLo(), lo );
				int endDist = distance( getTextToken(i).getHi(), hi );
				// <= prefers later start, end tokens
				if (startDist<=minStartDist) {
					minStartDist = startDist;
					firstTextToken = i;
					if (DEBUG) log.debug("minStartDist => "+minStartDist+" for token "+getTextToken(i));
				}
				if (endDist<=minEndDist) {
					minEndDist = endDist;
					lastTextToken = i;
					if (DEBUG) log.debug("minEndDist => "+minEndDist+" for token "+getTextToken(i));
				}
			} else {
				if (getTextToken(i).getLo()>=lo && firstTextToken<0) {
					firstTextToken = i;
					if (DEBUG) log.debug("firstTextToken => "+getTextToken(i));
				}
				if (getTextToken(i).getHi()<=hi) {
					lastTextToken = i;
					if (DEBUG) log.debug("lastTextToken => "+getTextToken(i));
				}
			}
		} 
		if (firstTextToken<0 || lastTextToken<0) {
			throw new IllegalArgumentException("no proper subspan for lo="+lo+" hi="+hi+" for: "+this);
		}

		//System.out.println("closest first ["+firstTextToken+"] "+getTextToken(firstTextToken));
		//System.out.println("closest last ["+lastTextToken+"] "+getTextToken(lastTextToken));
		return subSpan(firstTextToken,lastTextToken-firstTextToken+1);
	}

	private int distance(int i, int j) {
		return (i>=j ? i-j : j-i );
	}

	public Viewer toGUI() {
		return new SpanViewer.ControlledTextViewer(this);
		//return new SpanViewer.TextViewer(this);
	}
}

