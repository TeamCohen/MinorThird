/* Copyright 2004, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.RegexTokenizer;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;

/**
 * A name matching scheme on top of a given extractor, fit for spanTypes
 * depicting personal names. This class applies a given annotator. Then, it uses
 * the output extractor's dictionary of predicted names and over-rides some of
 * the original predictions, using the NameMatcher scheme. This procedure
 * increases recall, at low cost of precision.
 * 
 * @author Richard Wang, edited by Einat Minkov
 */

// need to store the names lists in a sorted list (so the name would be matched
// from long to short)
public class ExtractorNameMatcher{

	private File fromFile=null;

	private File saveAs=null;

	private MonotonicTextLabels textLabels=null;

	private MonotonicTextLabels annLabels=null;

	private String predType="_prediction";

	private String spanType="";

	private static double threshold=16;

	private ExtractorAnnotator ann=null;

	private SpanDifference finalSD=null;

	private List<String> nameDict=new ArrayList<String>();

	private static final String DIV="@#!";

	private static final int WINDOW_SIZE=5;

	private static final int SIG_SIZE=2; // number of tokens at the end of e-mail

	// in search for signatures

// private static final File fixMixup=new File("fixEnv.mixup");

	private List<String> lowRiskNameList=new ArrayList<String>();

	private List<String> highRiskNameList=new ArrayList<String>();

	private List<String> deletedNameList=new ArrayList<String>();

	public double getTokenPrecision(){
		return finalSD.tokenPrecision();
	}

	public double getTokenRecall(){
		return finalSD.tokenRecall();
	}

	public ExtractorNameMatcher(MonotonicTextLabels labels){
		this.annLabels=labels;
	}
	
	public ExtractorNameMatcher(){}

	//
	// command-line processing
	//
	public class MyCLP extends BasicCommandLineProcessor{

		public void loadFrom(String s){
			fromFile=new File(s);
		}

		public void saveAs(String s){
			saveAs=new File(s);
		}

		public void labels(String s){
			textLabels=(MutableTextLabels)FancyLoader.loadTextLabels(s);
		}

		public void spanType(String s){
			spanType=s;
		}

		@Override
		public void usage(){
			for(int i=0;i<USAGE.length;i++)
				System.out.println(USAGE[i]);
		}
	}

	public CommandLineProcessor getCLP(){
		return new MyCLP();
	}

	static private final String[] USAGE=
			{
					"ExtractorNameMatcher: increase recall of a previously-learned extractor, "
							+"applying a name matching scheme",
					"",
					"Parameters:",
					" -loadFrom FILE     where to load a previously-learner extractor from",
					" -labels KEY        the key for the labels, in which names are to be extracted",
					" [-spanType String] the span type of the true names. The default is set to true_name",
					" [-saveAs FILE]     a file to save the new post-name matching labels",
					"",};

	public void doMain(){
		if(annLabels==null){
			if(fromFile==null)
				throw new IllegalStateException("need to specify -loadFrom");

			// load the annotator
			try{
				ann=(ExtractorAnnotator)IOUtil.loadSerialized(fromFile);
			}catch(IOException ex){
				throw new IllegalArgumentException("can't load annotator from "+
						fromFile+": "+ex);
			}

			annLabels=(MonotonicTextLabels)ann.annotatedCopy(textLabels);
		}

		// create dictionary, sorted by names' length
		Set<String> allNames=new HashSet<String>();
		for(Iterator<Span> it=annLabels.instanceIterator(predType);it.hasNext();){
			Span sp=it.next();
			allNames.add(sp.asString());
		}
		nameDict=new ArrayList<String>(allNames);
		Collections.sort(nameDict,new Comparator<String>(){

			@Override
			public int compare(String o1,String o2){
				return new Integer(o2.length()).compareTo(new Integer(o1.length()));
			}
		});

		FreqAnal fa=new FreqAnal(annLabels,predType);

		// transorm-extend dictionary per pre-defined personal name-specific
		// templates.
		// identify 'high-risk' names and eliminate them from the extended
		// dictionary.
		transformDict(fa);

		int counter=0;
		/**
		 * System.out.println("High Confidence Names:"); for (Iterator i =
		 * nameList.iterator(); i.hasNext();) System.out.println(++counter + ". " +
		 * i.next()); counter = 0;
		 */
		System.out.println("Low Risk Names:");
		for(Iterator<String> i=lowRiskNameList.iterator();i.hasNext();)
			System.out.println(++counter+". "+i.next());
		counter=0;
		System.out.println("High Risk Names:");
		for(Iterator<String> i=highRiskNameList.iterator();i.hasNext();)
			System.out.println(++counter+". "+i.next());
		counter=0;
		System.out.println("Deleted Names:");
		for(Iterator<String> i=deletedNameList.iterator();i.hasNext();)
			System.out.println(++counter+". "+i.next());

		applyDict();

		MixupProgram p=null;
		try{
			// BUG: THIS FILE DOES NOT EXIST AND THE WAY ITS ACCESSED IS WRONG
			p=new MixupProgram(new File("c:\\minorthird\\apps\\names\\fixEnv.mixup"));
		}catch(Exception e){
			System.out.println(e);
		}

		MixupInterpreter interp=new MixupInterpreter(p);
		interp.eval(annLabels);

		if(saveAs!=null){
			try{
				(new TextLabelsLoader()).saveTypesAsOps(annLabels,saveAs);
			}catch(IOException e){
				try{
					(new TextLabelsLoader()).saveTypesAsOps(annLabels,new File(
							"name-matching-labels.env"));
				}catch(Exception e2){
					System.out.println(e2);
				}
			}
		}

		// TextBaseViewer.view(annLabels);

		SpanDifference sd;
		System.out
				.println("============================================================");
		System.out.println("Pre names-matching:");
		sd=
				new SpanDifference(annLabels.instanceIterator(predType),annLabels
						.instanceIterator(spanType),annLabels.closureIterator(spanType));
		System.out.println(sd.toSummary());
		System.out.println("Post names-matching:");
		finalSD=
				new SpanDifference(annLabels
						.instanceIterator(predType+"_updated_fixed"),annLabels
						.instanceIterator(spanType),annLabels.closureIterator(spanType));
		System.out.println(finalSD.toSummary());

	}

	private void applyDict(){
		int counter=0;

		for(Iterator<Span> i=annLabels.getTextBase().documentSpanIterator();i
				.hasNext();){
			// if (counter==5) TextBaseViewer.view(annLabels);
			Span docSpan=i.next();
			System.out.println(((float)++counter/annLabels.getTextBase().size()*100)+
					"% Working on "+docSpan.getDocumentId()+"...");

			for(int j=0;j<docSpan.size();j++){
				Span tokenWindow=
						docSpan.subSpan(j,Math.min(docSpan.size()-j,WINDOW_SIZE));
				Span nameMatch=dictLookup(lowRiskNameList,tokenWindow);
				if(nameMatch!=null){
					System.out.println("! Found: "+
							nameMatch.asString().replaceAll("[\r\n\\s]+"," ")+" matches "+
							tokenWindow.asString().replaceAll("[\r\n\\s]+"," "));
					annLabels.addToType(nameMatch,predType+"_updated");
					j+=nameMatch.size()-1;
				}
			}

			// for signature detection
			for(int j=docSpan.size()-SIG_SIZE;j<docSpan.size();j++){
				Span tokenWindow=
						docSpan.subSpan(j,Math.min(docSpan.size()-j,WINDOW_SIZE));
				Span nameMatch=dictLookup(highRiskNameList,tokenWindow);
				if(nameMatch!=null){
					System.out.println("! Found: "+
							nameMatch.asString().replaceAll("[\r\n\\s]+"," ")+" matches "+
							tokenWindow.asString().replaceAll("[\r\n\\s]+"," "));
					annLabels.addToType(nameMatch,predType+"_updated");
					j+=nameMatch.size()-1;
				}
			}
		}

	}

	private Span dictLookup(List<String> nameList,Span tokenWindow){
		// old code created a BasicTextBase() and called splitIntoTokens(name)
		RegexTokenizer tokenizer=new RegexTokenizer();
		for(Iterator<String> i=nameList.iterator();i.hasNext();){
			String name=i.next();
			String tokens=tokenWindow.asString().replaceAll("[\r\n\\s]+"," ");
			if(tokens.toLowerCase().matches("(?i)(?s)^\\Q"+name+"\\E(\\W|$).*")){
				int numTokens=tokenizer.splitIntoTokens(name).length;
				return tokenWindow.subSpan(0,numTokens);
			}
		}
		return null;
	}

	private void transformDict(FreqAnal fa){
		for(Iterator<String> i=nameDict.iterator();i.hasNext();){
			List<String> transformedNames=transformName(i.next());
			for(Iterator<String> j=transformedNames.iterator();j.hasNext();){
				String tn=j.next();
				boolean lowRisk=(tn.indexOf(DIV)==-1);
				boolean highRisk=(tn.matches("(\\w"+DIV+")+"));
				tn=tn.replaceAll(DIV,"");
				Double hScore=fa.getHScore(tn);
				if(hScore!=null&&hScore.doubleValue()<threshold){
					deletedNameList.add(tn);
					continue;
				}
				if(lowRisk)
					lowRiskNameList.add(tn);
				else if(highRisk)
					highRiskNameList.add(tn);
			}
		}
		lowRiskNameList=uniqueSortedList(lowRiskNameList);
		highRiskNameList=uniqueSortedList(highRiskNameList);
		deletedNameList=uniqueSortedList(deletedNameList);
	}

	private List<String> transformName(String name){
		List<String> result=new ArrayList<String>();
		String str=name.toLowerCase().trim().replaceAll("[^a-zA-Z\\- ]+","");
		// if (str.trim().replaceAll("\\W", "").length() > 1) result.add(str);
		String s[]=str.split("[\\- ]+");
		Object[] array=new Object[0];

		if(s.length==1){
			int[][] order={{0}};
			array=transform(s,order);
		}else if(s.length==2){
			int[][] order={{0,1},{0}};
			array=transform(s,order);
		}else if(s.length==3){
			int[][] order={{0,1,2},{0,2},{2},{0}};
			array=transform(s,order);
		}else if(s.length==4){
			int[][] order={{0,1,2,3},{0,1,3},{0,3},{3},{0}};
			array=transform(s,order);
		}

		for(int i=0;i<array.length;i++){
			String temp=((String)array[i]).trim();
			if(temp.replaceAll("\\W","").length()<2)
				continue;
			if(temp.matches(".*-$"))
				continue;
			result.add(temp);
		}

		return result;
	}

	private Object[] transform(String[] s,int[][] order){
		List<Object> result=new ArrayList<Object>();
		Object[][] o=new Object[s.length][];

		for(int i=0;i<s.length;i++)
			o[i]=transformToken(s[i],(i==0),(i==s.length-1));

		for(int i=0;i<order.length;i++){
			int[] cur_order=order[i];

			if(cur_order.length==1)
				for(int j=0;j<o[cur_order[0]].length;j++)
					result.add(o[cur_order[0]][j]);
			else if(cur_order.length==2)
				for(int j=0;j<o[cur_order[0]].length;j++)
					for(int k=0;k<o[cur_order[1]].length;k++)
						result.add((String)o[cur_order[0]][j]+o[cur_order[1]][k]);
			else if(cur_order.length==3)
				for(int j=0;j<o[cur_order[0]].length;j++)
					for(int k=0;k<o[cur_order[1]].length;k++)
						for(int l=0;l<o[cur_order[2]].length;l++)
							result.add((String)o[cur_order[0]][j]+o[cur_order[1]][k]+
									o[cur_order[2]][l]);
			else if(cur_order.length==4)
				for(int j=0;j<o[cur_order[0]].length;j++)
					for(int k=0;k<o[cur_order[1]].length;k++)
						for(int l=0;l<o[cur_order[2]].length;l++)
							for(int m=0;m<o[cur_order[3]].length;m++)
								result.add((String)o[cur_order[0]][j]+o[cur_order[1]][k]+
										o[cur_order[2]][l]+o[cur_order[3]][m]);
		}

		return result.toArray();
	}

	private List<String> uniqueSortedList(List<String> list){
		Set<String> set=new HashSet<String>();
		for(Iterator<String> i=list.iterator();i.hasNext();){
			String str=i.next();
			set.add(str);
		}
		List<String> al=new ArrayList<String>(set);
		Collections.sort(al,new Comparator<String>(){
			@Override
			public int compare(String o1,String o2){
				return new Integer(o2.length()).compareTo(o1.length());
			}
		});
		return al;
	}

	private Object[] transformToken(String name,boolean first,boolean last){
		List<String> result=new ArrayList<String>();
		if(name.length()==0)
			return result.toArray();
		if(last)
			result.add(name);
		if(!last)
			result.add(name+" ");
		if(!last)
			result.add(name+"-");
		if(!last)
			result.add(name.substring(0,1)+". ");
		if(last)
			result.add(name.substring(0,1)+".");
		result.add(name.substring(0,1)+DIV);
		return result.toArray();
	}

	/**
	 */
	public static void main(String[] args){

		try{
			ExtractorNameMatcher nm=new ExtractorNameMatcher();
			nm.getCLP().processArguments(args);
			nm.doMain();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
