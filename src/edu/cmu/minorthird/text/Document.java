package edu.cmu.minorthird.text;



/**
 * This class holds a single text 'document'.
 * It holds the real text, plus all the tokens.
 *
 * This is the only direct pointer to the documentText.
 * @author ksteppe
 */
class Document
{
	private String documentId;   // name of document
	private String documentText; // string version of text
	private TextToken[] tokens;  // tokenized version of text

  public Document(String documentId, String documentText)
  {
		this.documentId = documentId;
		this.documentText = documentText;
	}

	public String getText() { return this.documentText; }
  public void setTokens(TextToken[] tokenArray)  { this.tokens = tokenArray; }
  public String getId() { return this.documentId; }
	public TextToken[] getTokens() { return tokens; }
}
