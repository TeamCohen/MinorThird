package edu.cmu.minorthird.text;



/**
 * This class holds a single text 'document'.
 * It holds the real text, plus all the tokens.
 *
 * This is the only direct pointer to the documentText
 * @author ksteppe
 */
public class Document //implements Span //???
{
	private String documentId;

	/** single string version of the text */
	private String documentText;

	/** tokenized version */
	private TextToken[] tokens; //should this be TextTokens?

  public Document(String documentId, String documentText)
  {
    init(documentId, documentText);
  }

  private void init(String documentId, String documentText)
	{
		this.documentId = documentId;
		this.documentText = documentText;
	}

	public TextToken[] getTokens() { return tokens; }

	public String getText() { return this.documentText; }

  public void setTokens(TextToken[] tokenArray)
  { tokens = tokenArray; }

  public String getId()
  { return this.documentId; }
}
