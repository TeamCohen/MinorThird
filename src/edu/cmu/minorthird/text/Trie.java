package edu.cmu.minorthird.text;

import java.util.*;

/**
 * Efficient scheme for matching a rote list of sequences of tokens.
 * 
 * @author William Cohen
 */

public class Trie{

	// a node in the trie
	private static class TrieNode{

		public Map<String,TrieNode> map=null;

		public List<String> endIds=null;

		@Override
		public String toString(){
			return "TrieNode(ends="+endIds+",map="+map+")";
		}
	}

	// a match to something in the Trie
	private static class TrieMatch{

		public List<String> endIds;

		public int start;

		public int length;

		public TrieMatch(List<String> endIds,int start,int length){
			this.endIds=endIds;
			this.start=start;
			this.length=length;
		}
	}

	private TrieNode root;
	
	public Trie(){
		root=new TrieNode();
	}

	/** Lookup matches to the trie in the span */
	public ResultIterator lookup(Span span){
		List<TrieMatch> accum=new ArrayList<TrieMatch>();
		for(int i=0;i<span.size();i++){
			lookup(accum,span,i);
		}
		return new MyResultIterator(span,accum);
	}

	private void lookup(List<TrieMatch> accum,Span span,int start){
		TrieNode node=root;
		int depth=0;
		while(node!=null){
			if(node.endIds!=null){
				// add a new match
				accum.add(new TrieMatch(node.endIds,start,depth));
			}
			// extend the trie
			if(node.map!=null&&start+depth<span.size()){
				node=node.map.get(span.getToken(start+depth).getValue());
			}else{
				node=null;
			}
			depth++;
		}
	}

	/** Associate a sequence of words with a given id. */
	public void addWords(String id,String[] words){
		TrieNode node=root;
		for(int i=0;i<words.length;i++){
			if(node.map==null)
				node.map=new TreeMap<String,TrieNode>();
			TrieNode kid=node.map.get(words[i]);
			if(kid==null)
				node.map.put(words[i],(kid=new TrieNode()));
			node=kid;
		}
		// associate this id with the last node in the chain
		if(node.endIds==null)
			node.endIds=new ArrayList<String>();
		node.endIds.add(id);
	}

	/** Pretty-print the entire trie. */
	@Override
	public String toString(){
		StringBuilder b=new StringBuilder();
		toString(b,0,root);
		return b.toString();
	}

	private void tab(StringBuilder b,int level){
		for(int tab=0;tab<level;tab++)
			b.append("|  ");
	}

	private void toString(StringBuilder b,int level,TrieNode node){
		if(node.map==null)
			return;
		for(Iterator<String> i=node.map.keySet().iterator();i.hasNext();){
			String w=i.next();
			TrieNode kid=node.map.get(w);
			tab(b,level);
			b.append("'").append(w).append("'");
			if(kid.endIds!=null){
				b.append(" IDS:");
				for(Iterator<String> j=kid.endIds.iterator();j.hasNext();){
					b.append(" ").append(j.next());
				}
			}
			// buf.append("\t"+node);
			b.append("\n");
			toString(b,level+1,node.map.get(w));
		}
	}

	/**
	 * An extension of Span.Looper which also returns the ids associated with a
	 * Span.
	 */
	public static interface ResultIterator extends Iterator<Span>{

		/** Return a list of the ids associated with the span in the Trie */
		public List<String> getAssociatedIds();
	}

	//
	// an implementation of ResultLooper
	//
	private static class MyResultIterator implements ResultIterator{

		private Iterator<TrieMatch> i;

		private Span span;

		private List<String> lastIdList;

//		private int estSize=-1;

		public MyResultIterator(Span span,Collection<TrieMatch> c){
			this.span=span;
			this.i=c.iterator();
//			estSize=c.size();
		}

		@Override
		public boolean hasNext(){
			return i.hasNext();
		}

		@Override
		public void remove(){
			i.remove();
		}

		@Override
		public List<String> getAssociatedIds(){
			return lastIdList;
		}

		@Override
		public Span next(){
			TrieMatch match=i.next();
			lastIdList=match.endIds;
			return span.subSpan(match.start,match.length);
		}

//		public int estimatedSize(){
//			return estSize;
//		}
	}

	public static void main(String[] argv){
		BasicTextBase base=new BasicTextBase();
		Trie trie=new Trie();
		for(int i=0;i<argv.length-1;i++){
			trie.addWords(("argv"+i),base.getTokenizer().splitIntoTokens(argv[i]));
		}
		System.out.println(trie.toString());
		base.loadDocument("span",argv[argv.length-1]);
		for(Iterator<Span> i=trie.lookup(base.documentSpan("span"));i.hasNext();){
			System.out.println("match: "+i.next().asString());
		}
	}
}
