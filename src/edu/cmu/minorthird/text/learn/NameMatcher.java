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

import edu.cmu.minorthird.text.AbstractAnnotator;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.RegexTokenizer;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;
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
public class NameMatcher extends AbstractAnnotator{

	private String predType="_prediction";

	private String spanType="true_name";

	private static double threshold=16;

	private List<String> nameDict=new ArrayList<String>();

	private static final String DIV="@#!";

	private static final int WINDOW_SIZE=5;

	private static final int SIG_SIZE=2; // number of tokens at the end of e-mail

	// in search for signatures

	private List<String> lowRiskNameList=new ArrayList<String>();

	private List<String> highRiskNameList=new ArrayList<String>();

	private List<String> deletedNameList=new ArrayList<String>();

	private static MonotonicTextLabels postLabels=null;

	private static boolean Remove_Single_Tokens_Low_PFIDF=true;

	public NameMatcher(String spanType){
		this.spanType=spanType;
	}

	public NameMatcher(){
		;
	}
	
	public String getSpanType(){
		return spanType;
	}
	
	public void setSpanType(String spanType){
		this.spanType=spanType;
	}

	@Override
	protected void doAnnotate(MonotonicTextLabels labels){
		// create dictionary, sorted by names' length
		Set<String> allNames=new HashSet<String>();
		for(Iterator<Span> it=labels.instanceIterator(predType);it.hasNext();){
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

		FreqAnal fa=new FreqAnal(labels,predType);

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

		applyDictIncreaseRecall(labels);
		if(Remove_Single_Tokens_Low_PFIDF)
			applyDictIncreasePrecision(postLabels);
	}

	@Override
	public String explainAnnotation(TextLabels labels,Span span){
		return "No explanation implemented.";
	}

	private void applyDictIncreaseRecall(MonotonicTextLabels labels){
		int counter=0;

		for(Iterator<Span> i=labels.getTextBase().documentSpanIterator();i
				.hasNext();){
			Span docSpan=i.next();
			System.out.println(((float)++counter/labels.getTextBase().size()*100)+
					"% Working on "+docSpan.getDocumentId()+"...");

			for(int j=0;j<docSpan.size();j++){
				Span tokenWindow=
						docSpan.subSpan(j,Math.min(docSpan.size()-j,WINDOW_SIZE));
				Span nameMatch=dictLookup(lowRiskNameList,tokenWindow);
				if(nameMatch!=null){
					System.out.println("! Found: "+
							nameMatch.asString().replaceAll("[\r\n\\s]+"," ")+" matches "+
							tokenWindow.asString().replaceAll("[\r\n\\s]+"," "));
					labels.addToType(nameMatch,predType+"_updated");
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
					labels.addToType(nameMatch,predType+"_updated");
					j+=nameMatch.size()-1;
				}
			}
		}
		NameMatcher.postLabels=labels;
	}

	private void applyDictIncreasePrecision(MonotonicTextLabels labels){
		int counter=0;

		for(Iterator<Span> i=labels.getTextBase().documentSpanIterator();i.hasNext();){
			Span docSpan=i.next();
			System.out.println(((float)++counter/labels.getTextBase().size()*100)+
					"% Working on "+docSpan.getDocumentId()+"...");

			for(Iterator<Span> k=
					labels.instanceIterator(predType,docSpan.getDocumentId());k.hasNext();){
				Span span=k.next();
				if(span.size()==1){
					String token=span.getToken(0).getValue().toLowerCase();
					if(deletedNameList.contains(token)){
						labels.setProperty(span.getToken(0),"delete","t");
					}
				}
			}
		}
		NameMatcher.postLabels=labels;
	}

	private Span dictLookup(List<String> nameList,Span tokenWindow){
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
		List<String> al=new ArrayList<String>(new HashSet<String>(list));
		Collections.sort(al,new Comparator<String>(){
			@Override
			public int compare(String o1,String o2){
				return new Integer(o2.length()).compareTo(new Integer(o1.length()));
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

	private static void usage(){
		System.err
				.println("ExtractorNameMatcher: increase recall of a previously-learned extractor, ");
		System.err.println("applying a name matching scheme");
		System.err.println("Parameters:");
		System.err
				.println(" -loadFrom FILE     where to load a previously-learner extractor from");
		System.err
				.println(" -labels KEY        the key for the labels, in which names are to be extracted");
		System.err
				.println(" -spanType String   the span type of the true names. Usually, it is 'true_name'");
		System.err
				.println(" [-saveAs FILE]     a file to save the new post-name matching labels");
		System.err.println("");
		System.exit(1);
	}

	public static void main(String[] args) throws IOException{
		File fromFile=null;
		File saveAs=new File("NM_labels.env");
		String spanType="";
		MonotonicTextLabels textLabels=null;
		MonotonicTextLabels annLabels=null;
		ExtractorAnnotator ann=null;

		NameMatcher nameMatcher=new NameMatcher(spanType);

		// parse and load arguments
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-loadFrom")){
				fromFile=new File(args[i+1]);
			}else if(args[i].equals("-saveAs")){
				saveAs=new File(args[i+1]);
			}else if(args[i].equals("-labels")){
				textLabels=(MutableTextLabels)FancyLoader.loadTextLabels(args[i+1]);
			}else if(args[i].equals("-spanType")){
				spanType=args[i+1];
			}
		}
		if((fromFile==null)||(textLabels==null)||(spanType==null))
			usage();

		// load the annotator
		try{
			ann=(ExtractorAnnotator)IOUtil.loadSerialized(fromFile);
		}catch(IOException ex){
			throw new IllegalArgumentException("can't load annotator from "+fromFile+
					": "+ex);
		}
		annLabels=(MonotonicTextLabels)ann.annotatedCopy(textLabels);
		// TextBaseViewer.view(annLabels);

		nameMatcher.doAnnotate(annLabels);

		MixupProgram p=null;
		try{
			p=
					new MixupProgram(
							new String[]{"defTokenProp email:t = ~re'([\\.\\-\\w+]+\\@[\\.\\-\\w\\+]+)',1;"});
			p.addStatement("defSpanType email =: ... [email:t+R] ... ;");
			p
					.addStatement("defTokenProp predicted_name:1 =: ... [@_prediction_updated] ... || ... [@_prediction] ... ;");
			p
					.addStatement("defSpanType _prediction_updated_fixed =: ... [L <predicted_name:1, !email:t, !delete:t>+ R] ... ;");
		}catch(Exception e){
			System.out.println(e);
		}
		MixupInterpreter interp=new MixupInterpreter(p);
		interp.eval(postLabels);
		TextBaseViewer.view(postLabels);

		if(saveAs!=null){
			try{
				(new TextLabelsLoader()).saveTypesAsOps(postLabels,saveAs);
			}catch(IOException e){
				try{
					(new TextLabelsLoader()).saveTypesAsOps(postLabels,new File(
							"name-matching-labels.env"));
				}catch(Exception e2){
					System.out.println(e2);
				}
			}
		}

		// TextBaseViewer.view(nameMatcher.postLabels);

		SpanDifference sd;
		System.out
				.println("============================================================");
		System.out.println("Pre names-matching:");
		sd=
				new SpanDifference(NameMatcher.postLabels
						.instanceIterator(nameMatcher.predType),NameMatcher.postLabels
						.instanceIterator(spanType),NameMatcher.postLabels
						.closureIterator(spanType));
		System.out.println(sd.toSummary());
		System.out.println("Post names-matching:");
		SpanDifference finalSD=
				new SpanDifference(NameMatcher.postLabels
						.instanceIterator(nameMatcher.predType+"_updated_fixed"),
						NameMatcher.postLabels.instanceIterator(spanType),
						NameMatcher.postLabels.closureIterator(spanType));
		System.out.println(finalSD.toSummary());
	}

}
