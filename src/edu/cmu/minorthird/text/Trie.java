package edu.cmu.minorthird.text;

import java.util.*;

/** Efficient scheme for matching a rote list of sequences of tokens.
 *
 * @author William Cohen
*/

public class Trie 
{
	// a node in the Trie
	private static class TrieNode {
		public Map map = null;
		public List endIds = null;
		public TrieNode() {;}
		public String toString() { return "TrieNode(ends="+endIds+",map="+map+")"; }
	}

	// a match to something in the Trie
	private static class TrieMatch {
		public List endIds;
		public int start;
		public int length;
		public TrieMatch(List endIds,int start,int length) {
			this.endIds = endIds;
			this.start = start;
			this.length = length;
		}
	}

	private TrieNode root = new TrieNode();

	public Trie() {;}

	/** Lookup matches to the trie in the span */
	public ResultLooper lookup(Span span) {
		List accum = new ArrayList();
		for (int i=0; i<span.size(); i++) {
			lookup(accum,span,i);
		}
		return new MyResultLooper(span,accum);
	}
	private void lookup(List accum, Span span, int start) {
		TrieNode node = root;
		int depth = 0;
		while (node!=null) {
			if (node.endIds!=null) {
				// add a new match
				accum.add(new TrieMatch(node.endIds, start, depth));
			}
			// extend the trie
			if (node.map!=null && start+depth<span.size()) {
				node = (TrieNode)node.map.get(span.getToken(start+depth).getValue());
			} else {
				node = null;
			}
			depth++;
		}
	}

	/** Associate a sequence of words with a given id. */
	public void addWords(String id, String[] words) {
		TrieNode node = root;
		for (int i=0; i<words.length; i++) {
			if (node.map==null) node.map = new TreeMap();
			TrieNode kid = (TrieNode)node.map.get(words[i]);
			if (kid==null) node.map.put(words[i], (kid = new TrieNode()) );
			node = kid;
		}
		// associate this id with the last node in the chain
		if (node.endIds==null) node.endIds = new ArrayList();
		node.endIds.add(id);
	}

	/** Pretty-print the entire trie. */
	public String toString() {
		StringBuffer buf = new StringBuffer("");
		toString(buf,0,root);
		return buf.toString();
	}
	private void tab(StringBuffer buf, int level) {
		for (int tab=0; tab<level; tab++) buf.append("|  ");
	}
	private void toString(StringBuffer buf,int level,TrieNode node) {
		if (node.map==null) return;
		for (Iterator i=node.map.keySet().iterator(); i.hasNext(); ) {
			String w = (String)i.next();
			TrieNode kid = (TrieNode)node.map.get(w);
			tab(buf,level);
			buf.append("'"+w+"'");
			if (kid.endIds!=null) {
				buf.append(" IDS:");
				for (Iterator j=kid.endIds.iterator(); j.hasNext(); ) {
					buf.append(" "+j.next());
				}
			} 
			//buf.append("\t"+node);
			buf.append("\n");
			toString(buf, level+1, (TrieNode)node.map.get(w) );
		}
	}

	/** An extension of Span.Looper which also returns the ids associated with a
	 * Span.
	 */
	public static interface ResultLooper extends Span.Looper {
		/** Return a list of the ids associated with the span in the Trie */ 
		public List getAssociatedIds();
	}

	//
	// an implementation of ResultLooper
	//
	private static class MyResultLooper implements ResultLooper {
		private Iterator i;
		private Span span;
		private List lastIdList;
		private int estSize = -1;
		public MyResultLooper(Span span,Collection c) { this.span = span; this.i = c.iterator(); estSize=c.size(); }
		public boolean hasNext() { return i.hasNext(); }
		public Span nextSpan() {	return (Span)next();	}
		public void remove() { i.remove(); }
		public List getAssociatedIds() { return lastIdList; }
		public Object next() {
			TrieMatch match = (TrieMatch)i.next();
			lastIdList = match.endIds;
			return span.subSpan(match.start,match.length);
		}
		public int estimatedSize() { return estSize; }
	}

	public static void main(String[] argv)
	{
		TextBase base = new BasicTextBase();
		Trie trie = new Trie();
		for (int i=0; i<argv.length-1; i++) {
			trie.addWords( ("argv"+i), base.splitIntoTokens(argv[i]) );
		}
		System.out.println(trie.toString());
		base.loadDocument("span",argv[argv.length-1]);
		for (Span.Looper i=trie.lookup( base.documentSpan("span") ); i.hasNext(); ) {
			System.out.println("match: "+i.nextSpan().asString());
		}
	}
}
