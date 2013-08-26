package edu.cmu.minorthird.text;

import java.util.Iterator;



/** A very simple stemming algorithm.
 *
 * @author William Cohen
*/

public class BoneheadStemmer 
{
	public final static String STEM_PROP = "stem";

	public BoneheadStemmer() {;}

	public void stem(TextBase base,MonotonicTextLabels labels)
	{
		for (Iterator<Span> i = base.documentSpanIterator(); i.hasNext(); ) {
			Span span = i.next();
			for (int j=0; j<span.size(); j++) {
				Token token = span.getToken(j);
				labels.setProperty(token,STEM_PROP,stem(token.getValue()));
			}
		}
	}
	public String stem(String s)
	{
		String lc = s.toLowerCase();
		if (lc.length()<4) return lc;
		else if (lc.endsWith("tion")) return lc.substring(0,lc.length()-4);
		else if (lc.endsWith("ed")) return lc.substring(0,lc.length()-2);
		else if (lc.endsWith("es")) return lc.substring(0,lc.length()-2);
		else if (lc.endsWith("ly")) return lc.substring(0,lc.length()-2);
		else if (lc.endsWith("s")) return lc.substring(0,lc.length()-1);
		else return lc;
	}

	static public void main(String[] args)
	{
		BoneheadStemmer stemmer = new BoneheadStemmer();
		for (int i=0; i<args.length; i++) {
			System.out.println("stem of '"+args[i]+"' = '"+stemmer.stem(args[i])+"'");
		}
	}
}
