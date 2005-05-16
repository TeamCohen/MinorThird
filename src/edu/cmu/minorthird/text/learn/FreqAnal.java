package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rcwang
 * Date: Mar 29, 2004
 * Time: 10:14:58 AM
 * @author Richard Wang <rcwang@cmu.edu>
 */
public class FreqAnal {

	private TextLabels labels = null;
    private String spanType = "_prediction";

    private static final int TF = 0;    // term frequency
	private static final int DF = 1;    // document frequency
	private static final int PF = 2;    // predicted frequency
	private static final int HS = 3;    // heuristic score
	private static final int LAST = 4;

	private static HashMap tokenHash = new HashMap();

	public FreqAnal (TextLabels labels, String spanType) {

        this.labels = labels;
        this.spanType = spanType;
        TextBase base = labels.getTextBase();

        for (Span.Looper i = base.documentSpanIterator(); i.hasNext();) {
			Span docSpan = i.nextSpan();
			ArrayList TFList = new ArrayList();
			ArrayList PFList = new ArrayList();
			for (int j = 0; j < docSpan.size(); j++) {
				Span tokenSpan = docSpan.subSpan(j, 1);
				//if (isEmail(tokenSpan,labels)) continue;
				if (isPredictedName(spanType, tokenSpan, labels))
					    PFList.add(tokenSpan.asString().toLowerCase());
				TFList.add(tokenSpan.asString().toLowerCase());
			}
			ArrayList DFList = uniqueList(TFList);
			updateHash(TFList, TF);
			updateHash(DFList, DF);
			updateHash(PFList, PF);
		}
		updateHScore(base.size(), HS);
	}

    public void print(){
        for (Iterator it=tokenHash.keySet().iterator();it.hasNext();){
            String next = (String) it.next();
            Double[] array = (Double[])tokenHash.get(next);
            System.out.println(next +
                    " "+array[0]+" "+array[1]+" "+ array[2]+" "+array[3]);
        }
    }


	private static void updateHScore (int numDoc, int type) {
		for (Iterator i = tokenHash.keySet().iterator(); i.hasNext();) {
			String token = (String) i.next();
			Double[] array = (Double[]) tokenHash.get(token);
			if (array == null) {
				array = new Double[LAST];
				for (int j = 0; j < array.length; j++)
					array[j] = new Double(0);
			}
			array[type] = TF_IDF(array, numDoc);
            System.out.println(token + " " + array[3]);
			tokenHash.put(token, array);
		}
	}

    private static Double TF_IDF (Double[] array, int numDoc) {
		return new Double(array[PF].doubleValue() / (array[TF].doubleValue() + 2) * Math.log((numDoc + 0.5) / array[DF].doubleValue()) / Math.log(numDoc + 1) * 100);
	}

	private static void updateHash (ArrayList list, int type) {
		for (Iterator i = list.iterator(); i.hasNext();) {
			String token = (String) i.next();
			Double[] array = (Double[]) tokenHash.get(token);
			if (array == null) {
				array = new Double[LAST];
				for (int j = 0; j < array.length; j++)
					array[j] = new Double(0);
			}
			array[type] = new Double(array[type].doubleValue() + 1);
			tokenHash.put(token, array);
		}
	}

	private static boolean isEmail (Span test, MutableTextLabels labels) {
		for (Span.Looper i = labels.instanceIterator("extracted_email", test.getDocumentId()); i.hasNext();) {
			Span email = i.nextSpan();
			if (email.contains(test))
				return true;
		}
		return false;
	}

	private static boolean isPredictedName (String spanType, Span test, TextLabels labels) {
		for (Span.Looper i = labels.instanceIterator(spanType, test.getDocumentId()); i.hasNext();) {
			Span name = i.nextSpan();
			if (name.contains(test))
				return true;
		}
		return false;
	}

	public Double getHScore (String term) {
		Double[] array = (Double[]) tokenHash.get(term);
		return (array != null) ? array[HS] : null;
	}


	private static ArrayList uniqueList (ArrayList list) {
		HashMap hash = new HashMap();
		for (Iterator i = list.iterator(); i.hasNext();) {
			String str = (String) i.next();
			hash.put(str, null);
		}
		return new ArrayList(hash.keySet());
	}

}
