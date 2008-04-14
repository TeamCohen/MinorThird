package edu.cmu.minorthird.text;

/**
 * A span that is a subset of another span
 * 
 * @author Cameron Williams
 */

public class SubSpan extends BasicSpan{
	
	static final long serialVersionUID=20080305L;

	private int startIndex;

	public SubSpan(String documentId,TextToken[] textTokens,int loTextTokenIndex,
			int spanLen,String documentGroupId,int startIndex){
		super(documentId,textTokens,loTextTokenIndex,spanLen,documentGroupId);
		this.startIndex=startIndex;
	}

	public int getStartIndex(){
		return startIndex;
	}
}
