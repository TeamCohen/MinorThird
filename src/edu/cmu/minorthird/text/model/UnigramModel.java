package edu.cmu.minorthird.text.model;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/** Unigram Language Model
 *
 * @author William Cohen
*/

public class UnigramModel
{
	// avoid re-creating a zillion copies of Integer(1)
	final private static Double[] CACHED_DOUBLES = new Double[10];
	static {	for (int i=0; i<CACHED_DOUBLES.length; i++) CACHED_DOUBLES[i] = new Double(i); 	}

	//
	// private data
	//
	private Map freq = new HashMap();
	private double total = 0;

	/** Load a file where each line contains a <count,word> pair.
	 */
	public void load(File file) throws IOException,FileNotFoundException
	{
		LineNumberReader in = new LineNumberReader(new FileReader(file));
		String line;
		while ((line = in.readLine())!=null) {
			String[] words = line.trim().split("\\s+");
			if (words.length!=2) badLine(line,in);
			int n=0;
			try {
				n = Integer.parseInt(words[0]);
			} catch (NumberFormatException e) {
				badLine(line,in);
			}
			total += n;
			freq.put( words[1], getDouble(n) );
		}
	}
	private Double getDouble(int n)
	{
		if (n<CACHED_DOUBLES.length) return CACHED_DOUBLES[n];
		else return new Double(n);
	}
	private void badLine(String line,LineNumberReader in)
	{
		throw new IllegalStateException("bad input at line "+in.getLineNumber()+": "+line);
	}

	/** Return log Prob(span|model). 
	 * Assuming indendence, this is sum log Prob(tok_i|model).
	 */
	public double score(Span span)
	{
		double sum = 0;
		double prior = 0.1/total; // lower than any word we've seen
		for (int i=0; i<span.size(); i++) {
			int f = getFrequency( span.getToken(i).getValue() );
			sum += estimatedLogProb( f, total, prior, 1.0 );
		}
		return sum;
	}

	public double getTotalWordCount()
	{
		return total;
	}
	public int getFrequency(String s)
	{
		Double f = (Double)freq.get(s);
		if (f==null) return 0;
		else return f.intValue();
	}

	private double estimatedLogProb(double k, double n, double prior, double pseudoCounts) {
		return Math.log( (k+prior*pseudoCounts) / (n+pseudoCounts) );
	}


	static public void main(String[] args) 
	{
		try {
			UnigramModel model = new UnigramModel();
			model.load(new File(args[0]));
			TextBase base = new BasicTextBase();
			for (int i=1; i<args.length; i++) {
				base.loadDocument("argv."+i, args[i]);
			}
			for (Span.Looper j=base.documentSpanIterator(); j.hasNext(); ) {
				Span s = j.nextSpan();
				System.out.println(s.asString()+" => "+model.score(s));
				for (int k=0; k<s.size(); k++) {
					String w = s.getToken(k).getValue();
					System.out.print(" "+w+":"+model.getFrequency(w));
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: file span1 span2...");
		}
	}
}
