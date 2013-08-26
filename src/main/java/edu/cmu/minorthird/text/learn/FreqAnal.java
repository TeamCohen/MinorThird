package edu.cmu.minorthird.text.learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;

/**
 * Created by IntelliJ IDEA. User: rcwang Date: Mar 29, 2004 Time: 10:14:58 AM
 * 
 * @author Richard Wang <rcwang@cmu.edu>
 */
public class FreqAnal{

	private TextLabels labels=null;

	private String spanType="_prediction";

	private static final int TF=0; // term frequency

	private static final int DF=1; // document frequency

	private static final int PF=2; // predicted frequency

	private static final int HS=3; // heuristic score

	private static final int LAST=4;

	private static Map<String,Double[]> tokenHash=new HashMap<String,Double[]>();
	
	public FreqAnal(TextLabels labels,String spanType){

		this.labels=labels;
		this.spanType=spanType;
		TextBase base=labels.getTextBase();

		for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
			Span docSpan=i.next();
			List<String> TFList=new ArrayList<String>();
			List<String> PFList=new ArrayList<String>();
			for(int j=0;j<docSpan.size();j++){
				Span tokenSpan=docSpan.subSpan(j,1);
				// if (isEmail(tokenSpan,labels)) continue;
				if(isPredictedName(spanType,tokenSpan,labels))
					PFList.add(tokenSpan.asString().toLowerCase());
				TFList.add(tokenSpan.asString().toLowerCase());
			}
			List<String> DFList=uniqueList(TFList);
			updateHash(TFList,TF);
			updateHash(DFList,DF);
			updateHash(PFList,PF);
		}
		updateHScore(base.size(),HS);
	}
	
	public TextLabels getLabels(){
		return labels;
	}
	
	public void setLabels(TextLabels labels){
		this.labels=labels;
	}
	
	public String getSpanType(){
		return spanType;
	}
	
	public void setSpanType(String spanType){
		this.spanType=spanType;
	}

	public void print(){
		for(Iterator<String> it=tokenHash.keySet().iterator();it.hasNext();){
			String next=it.next();
			Double[] array=tokenHash.get(next);
			System.out.println(next+" "+array[0]+" "+array[1]+" "+array[2]+" "+
					array[3]);
		}
	}

	private static void updateHScore(int numDoc,int type){
		for(Iterator<String> i=tokenHash.keySet().iterator();i.hasNext();){
			String token=i.next();
			Double[] array=tokenHash.get(token);
			if(array==null){
				array=new Double[LAST];
				for(int j=0;j<array.length;j++)
					array[j]=new Double(0);
			}
			array[type]=TF_IDF(array,numDoc);
			System.out.println(token+" "+array[3]);
			tokenHash.put(token,array);
		}
	}

	private static Double TF_IDF(Double[] array,int numDoc){
		return new Double(array[PF].doubleValue()/(array[TF].doubleValue()+2)*
				Math.log((numDoc+0.5)/array[DF].doubleValue())/Math.log(numDoc+1)*100);
	}

	private static void updateHash(List<String> list,int type){
		for(Iterator<String> i=list.iterator();i.hasNext();){
			String token=i.next();
			Double[] array=tokenHash.get(token);
			if(array==null){
				array=new Double[LAST];
				for(int j=0;j<array.length;j++)
					array[j]=new Double(0);
			}
			array[type]=new Double(array[type].doubleValue()+1);
			tokenHash.put(token,array);
		}
	}

//	private static boolean isEmail(Span test,MutableTextLabels labels){
//		for(Iterator<Span> i=
//				labels.instanceIterator("extracted_email",test.getDocumentId());i
//				.hasNext();){
//			Span email=i.next();
//			if(email.contains(test))
//				return true;
//		}
//		return false;
//	}

	private static boolean isPredictedName(String spanType,Span test,
			TextLabels labels){
		for(Iterator<Span> i=labels.instanceIterator(spanType,test.getDocumentId());i
				.hasNext();){
			Span name=i.next();
			if(name.contains(test))
				return true;
		}
		return false;
	}

	public Double getHScore(String term){
		Double[] array=tokenHash.get(term);
		return (array!=null)?array[HS]:null;
	}

	private static List<String> uniqueList(List<String> list){
		return new ArrayList<String>(new HashSet<String>(list));
	}

}
