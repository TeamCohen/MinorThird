package edu.cmu.minorthird.text.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

/** Unigram Language Model
 *
 * @author William Cohen
 */

public class UnigramModel{

	// avoid re-creating a zillion copies of Double(1), etc
	final private static Double[] CACHED_DOUBLES=new Double[10];
	static{
		for(int i=0;i<CACHED_DOUBLES.length;i++)
			CACHED_DOUBLES[i]=new Double(i);
	}

	//
	// private data
	//
	private Map<String,Double> freq=new HashMap<String,Double>();

	private double total=0;

	/** Load a file where each line contains a <count,word> pair.
	 */
	public void load(File file) throws IOException,FileNotFoundException{
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			String[] words=line.trim().split("\\s+");
			if(words.length!=2)
				badLine(line,in);
			int n=0;
			try{
				n=Integer.parseInt(words[0]);
			}catch(NumberFormatException e){
				badLine(line,in);
			}
			total+=n;
			freq.put(words[1],getDouble(n));
		}
		in.close();
	}

	private void badLine(String line,LineNumberReader in){
		throw new IllegalStateException("bad input at line "+in.getLineNumber()+
				": "+line);
	}

	/** Save a unigram model
	 */
	public void save(File file) throws IOException{
		PrintStream out=
				new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
		for(Iterator<Map.Entry<String,Double>> i=freq.entrySet().iterator();i
				.hasNext();){
			Map.Entry<String,Double> e=i.next();
			out.println(e.getValue().intValue()+" "+e.getKey());
		}
		out.close();
	}

	// routine to use cached doubles, rather than cons up new doubles
	private Double getDouble(int n){
		if(n<CACHED_DOUBLES.length)
			return CACHED_DOUBLES[n];
		else
			return new Double(n);
	}

	/** Return log Prob(span|model). 
	 * Assuming indendence, this is sum log Prob(tok_i|model).
	 */
	public double score(Span span){
		double sum=0;
		double prior=0.1/total; // lower than any word we've seen
		for(int i=0;i<span.size();i++){
			int f=getFrequency(span.getToken(i).getValue().toLowerCase());
			sum+=estimatedLogProb(f,total,prior,1.0);
		}
		return sum;
	}

	public double getTotalWordCount(){
		return total;
	}

	public int getFrequency(String s){
		String s1=s.toLowerCase();
		Double f=freq.get(s1);
		if(f==null)
			return 0;
		else
			return f.intValue();
	}

	public void incrementFrequency(String s){
		String s1=s.toLowerCase();
		freq.put(s1,getDouble(getFrequency(s1)+1));
	}

	private double estimatedLogProb(double k,double n,double prior,
			double pseudoCounts){
		return Math.log((k+prior*pseudoCounts)/(n+pseudoCounts));
	}

	static public void main(String[] args) throws IOException{
		if(args.length==0){
			System.out.println("usage 1: modelfile span1 span2...");
			System.out.println("usage 2: textbase modelfile");
		}
		if(args.length==2){
			UnigramModel model=new UnigramModel();
			TextBase base=FancyLoader.loadTextLabels(args[0]).getTextBase();
			for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
				Span s=i.next();
				for(int j=0;j<s.size();j++){
					model.incrementFrequency(s.getToken(j).getValue());
				}
			}
			model.save(new File(args[1]));
		}else{
			UnigramModel model=new UnigramModel();
			model.load(new File(args[0]));
			BasicTextBase base=new BasicTextBase();
			for(int i=1;i<args.length;i++){
				base.loadDocument("argv."+i,args[i]);
			}
			for(Iterator<Span> j=base.documentSpanIterator();j.hasNext();){
				Span s=j.next();
				System.out.println(s.asString()+" => "+model.score(s));
				for(int k=0;k<s.size();k++){
					String w=s.getToken(k).getValue();
					System.out.print(" "+w+":"+model.getFrequency(w));
				}
				System.out.println();
			}
		}
	}
}
