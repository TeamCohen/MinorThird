package edu.cmu.minorthird.text;

import java.io.Serializable;

/**
 * This class holds a single text 'document'.
 * It holds the real text, plus all the tokens.
 *
 * This is the only direct pointer to the documentText.
 * @author ksteppe
 */
public class Document implements Serializable{
	
	static final long serialVersionUID=20080303L;

	private String documentId; // name of document

	private String documentText; // string version of text

	public int charOffset=0; // used when the document is a subdocument of another

	private TextToken[] tokens; // tokenized version of text

	public Document(String documentId,String documentText){
		this.documentId=documentId;
		this.documentText=documentText;
	}

	public Document(String documentId,String documentText,int charOffset){
		this.documentId=documentId;
		this.documentText=documentText;
		this.charOffset=charOffset;
	}

	public String getText(){
		return this.documentText;
	}

	public void setTokens(TextToken[] tokenArray){
		this.tokens=tokenArray;
	}

	public String getId(){
		return this.documentId;
	}

	public TextToken[] getTokens(){
		return tokens;
	}
}
