package edu.cmu.minorthird.text;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.gui.SpanViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/** Implements the Span interface.
 *
 * @author William Cohen
 */

public class BasicSpan implements Span,Serializable,Visible{

	static private Logger log=Logger.getLogger(BasicSpan.class);

	static private final boolean DEBUG=log.isDebugEnabled();

	static private final long serialVersionUID=20080303L;

	private String documentId;

	private String documentGroupId;

	public int loTextTokenIndex,loCharIndex=-1,hiCharIndex=-1;

	private int charOffset=0;

	private int spanLen; // The number of tokens in the span.

	private TextToken[] textTokens;

	private String text=null;

	/** Constructor assumes that the textTokens are all from the documentId.
	 */
	public BasicSpan(String documentId,TextToken[] textTokens,
			int loTextTokenIndex,int spanLen,String documentGroupId){
		this.documentId=documentId;
		this.textTokens=textTokens;
		this.loTextTokenIndex=loTextTokenIndex;
		this.spanLen=spanLen;
		this.documentGroupId=documentGroupId==null?documentId:documentGroupId;
	}

	@Override
	public String getDocumentId(){
		return documentId;
	}

	@Override
	public String getDocumentGroupId(){
		return documentGroupId;
	}

	@Override
	public String getDocumentContents(){
		if(textTokens.length==0)
			return "";
		else
			return textTokens[0].getDocument();
	}

	/** Returns the number of tokens in the span. */
	@Override
	public int size(){
		return spanLen;
	}

	/** Retrieves the ith TextToken in the span */
	@Override
	public TextToken getTextToken(int i){
		if(i<0||i>=spanLen)
			throw new IllegalArgumentException("out of range: "+i);
		return textTokens[loTextTokenIndex+i];
	}

	/** Retrieves the ith TextToken in the span */
	@Override
	public Token getToken(int i){
		//return getTextToken(i);
		if(i<0||i>=spanLen)
			throw new IllegalArgumentException("out of range: "+i);
		return textTokens[loTextTokenIndex+i];
	}

	/** Create a new BasicSpan, covering the indicated TextToken's. */
	@Override
	public Span subSpan(int start,int len){
		if(start<0||start+len>spanLen)
			throw new IllegalArgumentException("out of range: "+start+","+len);
		return new BasicSpan(documentId,textTokens,loTextTokenIndex+start,len,
				documentGroupId);
	}

	/** Create a SubSpan of this span, covering the indicated TextToken's. */
	public SubSpan subSpan(int startIndex,int start,int len){
		if(start<0||start+len>spanLen)
			throw new IllegalArgumentException("out of range: "+start+","+len);
		return new SubSpan(documentId,textTokens,loTextTokenIndex+start,len,
				documentGroupId,startIndex);
	}

	/** A larger span containing this span. */
	@Override
	public Span documentSpan(){
		return new BasicSpan(documentId,textTokens,0,textTokens.length,
				documentGroupId);
	}

	/** The index of this span in the home span. */
	@Override
	public int documentSpanStartIndex(){
		return loTextTokenIndex;
	}

	@Override
	public boolean contains(Span other){
		if(!other.getDocumentId().equals(getDocumentId()))
			return false;
		int myStart=documentSpanStartIndex();
		int otherStart=other.documentSpanStartIndex();
		int myEnd=documentSpanStartIndex()+size();
		int otherEnd=other.documentSpanStartIndex()+other.size();
		return(myStart<=otherStart&&myEnd>=otherStart&&myStart<=otherEnd&&myEnd>=otherEnd);
	}

	@Override
	public boolean overlaps(Span other){
		if(!other.getDocumentId().equals(getDocumentId()))
			return false;
		int myStart=documentSpanStartIndex();
		int otherStart=other.documentSpanStartIndex();
		int myEnd=documentSpanStartIndex()+size();
		int otherEnd=other.documentSpanStartIndex()+other.size();
		return(myStart<=otherStart&&myEnd>=otherStart // [ ... ( ... ] - partial containment 1
				||myStart<=otherEnd&&myEnd>=otherEnd // [ ... ) ... ] - partial containment 2
		||other.contains(this) // ( .. [ ... ] ... ) - containment 
		);
	}

	/** Find the string contained in a Span. */
	@Override
	public String asString(){
		if(size()<=0)
			return "";
		else if(text==null){

			TextToken lo=getTextToken(0);
			TextToken hi=getTextToken(size()-1);
			text=lo.getDocument().substring(lo.getLo(),hi.getHi());
		}
		return text;
	}

	/** A length-zero span for the left boundary */
	@Override
	public Span getLeftBoundary(){
		return new BasicSpan(documentId,textTokens,loTextTokenIndex,0,
				documentGroupId);
	}

	/** A length-zero span for the left boundary */
	@Override
	public Span getRightBoundary(){
		return new BasicSpan(documentId,textTokens,loTextTokenIndex+spanLen,0,
				documentGroupId);
	}

	// Implement comparable
	@Override
	public int compareTo(Span other){
		int cmp1=getDocumentId().compareTo(other.getDocumentId());
		if(cmp1!=0)
			return cmp1;
		int cmp2=documentSpanStartIndex()-other.documentSpanStartIndex();
		if(cmp2!=0)
			return cmp2;
		int cmp3=size()-other.size();
		if(cmp3!=0)
			return cmp3;
		return 0;
	}

	// for safe hashing
	@Override
	public int hashCode(){
		return documentId.hashCode()^loTextTokenIndex^spanLen;
	}

	@Override
	public boolean equals(Object o){
		return o instanceof BasicSpan&&compareTo((Span)o)==0;
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		buf.append("Span '"+asString()+"'");
		buf.append(" = tokens "+loTextTokenIndex+":"+(loTextTokenIndex+spanLen)+
				" in ");
		buf.append(documentId+"/"+documentGroupId);
		return buf.toString();
	}

	@Override
	public Span charIndexSubSpan(int lo,int hi){
		return charIndexSubSpan(lo,hi,false);
	}

	@Override
	public Span charIndexProperSubSpan(int lo,int hi){
		return charIndexSubSpan(lo,hi,true);
	}

	@Override
	public void setCharOffset(int charOffset){
		this.charOffset=charOffset;
	}

	@Override
	public int getCharOffset(){
		return charOffset;
	}

	/** Converts from a span in character offsets within a document
	 * Span to a token span for that document Span. */
	private Span charIndexSubSpan(int lo,int hi,boolean proper){
		loCharIndex=lo;
		hiCharIndex=hi;
		// find token that start & end closest to lo and hi
		int minStartDist=Integer.MAX_VALUE;
		int minEndDist=Integer.MAX_VALUE;
		int firstTextToken=-1,lastTextToken=-1;
		for(int i=0;i<size();i++){
			if(!proper){
				if(DEBUG)
					log.debug("considering token '"+getTextToken(i)+"' from lo="+
							getTextToken(i).getLo()+" to hi="+getTextToken(i).getHi());
				int startDist=distance(getTextToken(i).getLo(),lo);
				int endDist=distance(getTextToken(i).getHi(),hi);
				// <= prefers later start, end tokens
				if(startDist<=minStartDist){
					minStartDist=startDist;
					firstTextToken=i;
					if(DEBUG)
						log.debug("minStartDist => "+minStartDist+" for token "+
								getTextToken(i));
				}
				if(endDist<=minEndDist){
					minEndDist=endDist;
					lastTextToken=i;
					if(DEBUG)
						log
								.debug("minEndDist => "+minEndDist+" for token "+
										getTextToken(i));
				}
			}else{
				// The lo character offset may lie on a whitespace character before a token, at the 
				// boundry of a token, or in the middle of a token.  In any of these cases we want to 
				// make sure to include this token in the span. So simply check that the lo character 
				// offset is less than the hi index of the token. That is check that at least one 
				// character of the token is included in the char offsets.
				if(firstTextToken<0&&lo<=getTextToken(i).getHi()){
					firstTextToken=i;
					if(DEBUG)
						log.debug("firstTextToken => "+getTextToken(i));
				}
				/* OLD LOGIC HERE:
				           if (getTextToken(i).getLo()>=lo && firstTextToken<0) {
				           firstTextToken = i;
				           if (DEBUG) log.debug("firstTextToken => "+getTextToken(i));
				           }
				 */

				// The hi character offset may lie on a whitespace character after a token, at the 
				// boundry of a token, or in the middle of a token.  Again, we need to include this
				// token in the subspan in any of these cases.  So continually increas the last
				// included token index (lastTextToken) for each subsequent token that is included in
				// in the span.  Do this by simply checking that the hi character offset is greater
				// than the token's lo character index.
				if(hi>getTextToken(i).getLo()){
					lastTextToken=i;
					if(DEBUG)
						log.debug("lastTextToken => "+getTextToken(i));
				}

				/* OLD LOGIC HERE
				           if (getTextToken(i).getHi()<=hi) {
				           lastTextToken = i;
				           if (DEBUG) log.debug("lastTextToken => "+getTextToken(i));
				           }
				 */
			}
		}
		if(firstTextToken<0||lastTextToken<0){
			throw new IllegalArgumentException("no proper subspan for lo="+lo+" hi="+
					hi+" for: "+this);
		}

		return subSpan(loCharIndex,firstTextToken,lastTextToken-firstTextToken+1);
	}

	@Override
	public int getLoTextToken(){
		return loTextTokenIndex;
	}

	/** Returns how many characters are before the span in the document */
	@Override
	public int getLoChar(){
		return textTokens[loTextTokenIndex].getLo();
	}

	/** Returns how many characters there are up to and including the span */
	@Override
	public int getHiChar(){
		return textTokens[loTextTokenIndex+size()-1].getHi();
	}

	private int distance(int i,int j){
		return(i>=j?i-j:j-i);
	}

	@Override
	public Viewer toGUI(){
		return new SpanViewer.ControlledTextViewer(this);
		//return new SpanViewer.TextViewer(this);
	}
}
