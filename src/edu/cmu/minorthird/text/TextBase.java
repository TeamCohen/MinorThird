/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;



/** Maintains information about what's in a set of documents.
 * Specifically, this contains a set of character sequences (TextToken's)
 * from some sort of set of containing documents - typically found by
 * tokenization.
 *
 *<p>
 * Implementations of TextBase should also implement Serializable.
 *
 * @author William Cohen
*/

public interface TextBase
{
  /**
   * Load a text document (represented by the given string) and tokenize
   * according to group 1 of a given regex pattern
   */
  public void loadDocument(String documentId, String documentString, String regexPattern);

  /**
   * Load a text document (represented by the given string) and tokenize
   * using the default regex pattern
   */
  public void loadDocument(String documentId, String text);

	/** Tokenize a string. */
	public String[] splitIntoTokens(String string);

	/** The number of documents. */
	public int size();

	/** An iterator over all documents. */
	public Span.Looper documentSpanIterator();

	/** Find the document span for the given id.
	 * If no such document span exists, then return null.
	 */
	public Span documentSpan(String documentId);

	/** Set the group that a document belongs to */
	public void setDocumentGroupId(String documentId,String documentGroupId);
}
