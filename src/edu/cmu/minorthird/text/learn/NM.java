/* Copyright 2004, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.gui.SpanViewer.TextViewer;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.mixup.Mixup.ParseException;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.ExtractorNameMatcher;
import edu.cmu.minorthird.util.*;


import java.io.*;
import java.util.*;

import org.apache.log4j.*;

/** A name matching scheme on top of a given extractor, fit for spanTypes depicting
 * personal names.
 * This class applies a given annotator. Then, it uses the output extractor's dictionary
 * of predicted names and over-rides some of the original predictions, using the
 * NameMatcher scheme. This procedure increases recall, at low cost of precision.
 *
 * @author Richard Wang, edited by Einat Minkov
 */

 // need to store the names lists in a sorted list (so the name would be matched from long to short)

public class NM extends AbstractAnnotator
{
	static private Logger log = Logger.getLogger(edu.cmu.minorthird.text.learn.NM.class);

  private File saveAs=null;
  private String predType="_prediction";
  private String spanType="true_name";
  private static double threshold = 16;
  private SpanDifference finalSD = null;

  private ArrayList nameDict = new ArrayList();
  private static final String DIV = "@#!";
  private static final int WINDOW_SIZE = 5;
  private static final int SIG_SIZE = 2;  // number of tokens at the end of e-mail in search for signatures

  private ArrayList lowRiskNameList = new ArrayList();
  private ArrayList highRiskNameList = new ArrayList();
  private ArrayList deletedNameList = new ArrayList();


  public double getTokenPrecision(){
     return finalSD.tokenPrecision();
  }

  public double getTokenRecall(){
     return finalSD.tokenRecall();
  }

  public double getSpanPrecision(){
     return finalSD.spanPrecision();
  }

  public double getSpanRecall(){
     return finalSD.spanRecall();
  }


  public NM(String spanType) {
      this.spanType = spanType;
      //TextBaseViewer.view(annLabels);
  }

  public NM(){;}


  protected void doAnnotate(MonotonicTextLabels labels){
         //create dictionary, sorted by names' length
      Set allNames = new HashSet();
      for (Iterator it=labels.instanceIterator(predType);it.hasNext();){
          Span sp = (Span)it.next();
          allNames.add(sp.asString());
      }
      nameDict = new ArrayList(allNames);
      Collections.sort(nameDict, new Comparator() {
			public int compare (Object o1, Object o2) {
				return new Integer(((String) o2).length()).compareTo(new Integer(((String) o1).length()));
			}
		});

            FreqAnal fa = new FreqAnal(labels, predType);

      //transorm-extend dictionary per pre-defined personal name-specific templates.
      //identify 'high-risk' names and eliminate them from the extended dictionary.
      transformDict(fa);


        int counter = 0;
		/**
        System.out.println("High Confidence Names:");
		for (Iterator i = nameList.iterator(); i.hasNext();)
			System.out.println(++counter + ". " + i.next());
		counter = 0;
        **/
		System.out.println("Low Risk Names:");
		for (Iterator i = lowRiskNameList.iterator(); i.hasNext();)
			System.out.println(++counter + ". " + i.next());
		counter = 0;
		System.out.println("High Risk Names:");
		for (Iterator i = highRiskNameList.iterator(); i.hasNext();)
			System.out.println(++counter + ". " + i.next());
		counter = 0;
		System.out.println("Deleted Names:");
		for (Iterator i = deletedNameList.iterator(); i.hasNext();)
			System.out.println(++counter + ". " + i.next());


      applyDict(labels);

      MixupProgram p = null;
      try{
          p = new MixupProgram(	new String[]
                {"defTokenProp email:t = ~re'([\\.\\-\\w+]+\\@[\\.\\-\\w\\+]+)',1;"}) ;
          p.addStatement("defSpanType email =: ... [email:t+R] ... ;");
          p.addStatement("defTokenProp predicted_name:1 =: ... [@_prediction_updated] ... || ... [@_prediction] ... ;");
          p.addStatement("defSpanType _prediction_updated_fixed =: ... [L <predicted_name:1, !email:t>+ R] ... ;");
      }

      catch (Exception e){
         System.out.println(e);
      }


      p.eval(labels, labels.getTextBase());

      if (saveAs!=null) {
			try {
				(new TextLabelsLoader()).saveTypesAsOps(labels, saveAs);
			} catch (IOException e) {
                try{
				(new TextLabelsLoader()).saveTypesAsOps(labels, new File("name-matching-labels.env"));
                }
                catch (Exception e2){
                    System.out.println(e2);
                }
			}
		}

		 //TextBaseViewer.view(labels);

	  SpanDifference sd;
		System.out.println("============================================================");
		System.out.println("Pre names-matching:");
		sd = new SpanDifference(labels.instanceIterator(predType), labels.instanceIterator(spanType), labels.closureIterator(spanType));
		System.out.println(sd.toSummary());
		System.out.println("Post names-matching:");
		finalSD = new SpanDifference(labels.instanceIterator(predType + "_updated_fixed"), labels.instanceIterator(spanType), labels.closureIterator(spanType));
		System.out.println(finalSD.toSummary());
  }


    public String explainAnnotation(TextLabels labels,Span span)
  {
    return "No explanation implemented.";
  }

   private void applyDict(MonotonicTextLabels annLabels) {
    int counter = 0;

   for (Span.Looper i = annLabels.getTextBase().documentSpanIterator(); i.hasNext();) {
        //if (counter==5) TextBaseViewer.view(annLabels);
        Span docSpan = i.nextSpan();
        System.out.println(((float) ++counter / annLabels.getTextBase().size() * 100) + "% Working on " + docSpan.getDocumentId() + "...");

        for (int j = 0; j < docSpan.size(); j++) {
            Span tokenWindow = docSpan.subSpan(j, Math.min(docSpan.size() - j, WINDOW_SIZE));
            Span nameMatch = dictLookup(lowRiskNameList, tokenWindow);
            if (nameMatch != null) {
                System.out.println("! Found: " + nameMatch.asString().replaceAll("[\r\n\\s]+", " ") + " matches " + tokenWindow.asString().replaceAll("[\r\n\\s]+", " "));
                annLabels.addToType(nameMatch, predType + "_updated");
                j += nameMatch.size()-1;
            }
        }

        // for signature detection
        for (int j = docSpan.size() - SIG_SIZE; j < docSpan.size(); j++) {
            Span tokenWindow = docSpan.subSpan(j, Math.min(docSpan.size() - j, WINDOW_SIZE));
            Span nameMatch = dictLookup(highRiskNameList, tokenWindow);
            if (nameMatch != null) {
                System.out.println("! Found: " + nameMatch.asString().replaceAll("[\r\n\\s]+", " ") + " matches " + tokenWindow.asString().replaceAll("[\r\n\\s]+", " "));
                annLabels.addToType(nameMatch, predType + "_updated");
                j += nameMatch.size()-1;
            }
         }
      }
   }

    private Span dictLookup (ArrayList nameList, Span tokenWindow) {
        BasicTextBase base = new BasicTextBase();
		for (Iterator i = nameList.iterator(); i.hasNext();) {
			String name = (String) i.next();
			String tokens = tokenWindow.asString().replaceAll("[\r\n\\s]+", " ");
			if (tokens.toLowerCase().matches("(?i)(?s)^\\Q" + name + "\\E(\\W|$).*")) {
				int numTokens = base.splitIntoTokens(name).length;
				return tokenWindow.subSpan(0, numTokens);
			}
		}
		return null;
	}


   private void transformDict(FreqAnal fa) {
         for (Iterator i = nameDict.iterator(); i.hasNext();) {
			ArrayList transformedNames = transformName((String) i.next());
			for (Iterator j = transformedNames.iterator(); j.hasNext();) {
				String tn = (String) j.next();
				boolean lowRisk = (tn.indexOf(DIV) == -1);
				boolean highRisk = (tn.matches("(\\w" + DIV + ")+"));
				tn = tn.replaceAll(DIV, "");
				Double hScore = fa.getHScore(tn);
				if (hScore != null && hScore.doubleValue() < threshold) {
					deletedNameList.add(tn);
					continue;
				}
				if (lowRisk)
					lowRiskNameList.add(tn);
				else if (highRisk) highRiskNameList.add(tn);
			}
	   }
       lowRiskNameList = uniqueSortedList(lowRiskNameList);
	   highRiskNameList = uniqueSortedList(highRiskNameList);
	   deletedNameList = uniqueSortedList(deletedNameList);
   }

   private ArrayList transformName(String name) {
		ArrayList result = new ArrayList();
		String str = name.toLowerCase().trim().replaceAll("[^a-zA-Z\\- ]+", "");
		// if (str.trim().replaceAll("\\W", "").length() > 1) result.add(str);
		String s[] = str.split("[\\- ]+");
		Object[] array = new Object[0];

		if (s.length == 1) {
			int[][] order = {
				{0}
			};
			array = transform(s, order);
		} else if (s.length == 2) {
			int[][] order = {
				{0, 1},
				{0}
			};
			array = transform(s, order);
		} else if (s.length == 3) {
			int[][] order = {
				{0, 1, 2},
				{0, 2},
				{2},
				{0}
			};
			array = transform(s, order);
		} else if (s.length == 4) {
			int[][] order = {
				{0, 1, 2, 3},
				{0, 1, 3},
				{0, 3},
				{3},
				{0}
			};
			array = transform(s, order);
		}

		for (int i = 0; i < array.length; i++) {
			String temp = ((String) array[i]).trim();
			if (temp.replaceAll("\\W", "").length() < 2) continue;
			if (temp.matches(".*-$")) continue;
			result.add(temp);
		}

		return result;
	}

    private Object[] transform (String[] s, int[][] order) {
		ArrayList result = new ArrayList();
		Object[][] o = new Object[s.length][];

		for (int i = 0; i < s.length; i++)
			o[i] = transformToken(s[i], (i == 0), (i == s.length - 1));

		for (int i = 0; i < order.length; i++) {
			int[] cur_order = order[i];

			if (cur_order.length == 1)
				for (int j = 0; j < o[cur_order[0]].length; j++)
					result.add(o[cur_order[0]][j]);
			else if (cur_order.length == 2)
				for (int j = 0; j < o[cur_order[0]].length; j++)
					for (int k = 0; k < o[cur_order[1]].length; k++)
						result.add((String) o[cur_order[0]][j] + o[cur_order[1]][k]);
			else if (cur_order.length == 3)
				for (int j = 0; j < o[cur_order[0]].length; j++)
					for (int k = 0; k < o[cur_order[1]].length; k++)
						for (int l = 0; l < o[cur_order[2]].length; l++)
							result.add((String) o[cur_order[0]][j] + o[cur_order[1]][k] + o[cur_order[2]][l]);
			else if (cur_order.length == 4)
				for (int j = 0; j < o[cur_order[0]].length; j++)
					for (int k = 0; k < o[cur_order[1]].length; k++)
						for (int l = 0; l < o[cur_order[2]].length; l++)
							for (int m = 0; m < o[cur_order[3]].length; m++)
								result.add((String) o[cur_order[0]][j] + o[cur_order[1]][k] + o[cur_order[2]][l] + o[cur_order[3]][m]);
		}

		return result.toArray();
	}

    private ArrayList uniqueSortedList (ArrayList list) {
		HashMap hash = new HashMap();
		for (Iterator i = list.iterator(); i.hasNext();) {
			String str = (String) i.next();
			hash.put(str, null);
		}
		ArrayList al = new ArrayList(hash.keySet());
		Collections.sort(al, new Comparator() {
			public int compare (Object o1, Object o2) {
				return new Integer(((String) o2).length()).compareTo(new Integer(((String) o1).length()));
			}
		});
		return al;
	}


    private Object[] transformToken (String name, boolean first, boolean last) {
		ArrayList result = new ArrayList();
		if (name.length() == 0)
			return result.toArray();
		if (last) result.add(name);
		if (!last) result.add(name + " ");
		if (!last) result.add(name + "-");
		if (!last) result.add(name.substring(0, 1) + ". ");
		if (last) result.add(name.substring(0, 1) + ".");
		result.add(name.substring(0, 1) + DIV);
		return result.toArray();
	}


  private static void usage() {
    System.err.println("ExtractorNameMatcher: increase recall of a previously-learned extractor, ");
    System.err.println("applying a name matching scheme");
    System.err.println("Parameters:");
    System.err.println(" -loadFrom FILE     where to load a previously-learner extractor from");
    System.err.println(" -labels KEY        the key for the labels, in which names are to be extracted");
    System.err.println(" -spanType String   the span type of the true names. Usually, it is 'true_name'");
    System.err.println(" [-saveAs FILE]     a file to save the new post-name matching labels");
    System.err.println("");
    System.exit(1);
  }


  public static void main(String[] args) throws IOException
  {
      File fromFile = null;
      File saveAs = new File("NM_labels.env");
      String spanType = "";
      MonotonicTextLabels textLabels = null;
      MonotonicTextLabels annLabels = null;
      ExtractorAnnotator ann = null;

      NM nameMatcher = new NM(spanType);

      //parse and load arguments
      for (int i = 0; i < args.length; i++) {
          if (args[i].equals("-loadFrom")) {
             fromFile = new File(args[i+1]);
            }
          else if (args[i].equals("-saveAs")) {
             saveAs = new File(args[i+1]);
            }
          else if (args[i].equals("-labels")) {
             textLabels=(MutableTextLabels)FancyLoader.loadTextLabels(args[i+1]);
          }
          else if (args[i].equals("-spanType")){
            spanType = args[i+1];
          }
      }
      if ((fromFile==null) || (textLabels==null) || (spanType==null)) usage();


        // load the annotator
		try {
			ann = (ExtractorAnnotator)IOUtil.loadSerialized(fromFile);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+fromFile+": "+ex);
        }
       annLabels = (MonotonicTextLabels)ann.annotatedCopy(textLabels);

       nameMatcher.doAnnotate(annLabels);
   }

}
