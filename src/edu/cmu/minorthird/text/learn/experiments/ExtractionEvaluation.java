package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import org.apache.log4j.Logger;

/** Records results of evaluating an extraction-learning system.
 *
 * @author William Cohen
*/

public class ExtractionEvaluation implements Visible,Serializable
{
  // serialization stuff
  static private final long serialVersionUID = 1;
  private final int CURRENT_VERSION_NUMBER = 1;

	private Map tagToStatsMap = new TreeMap();
	private String overallTag = null;

	private static class Stats implements Serializable {
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		double tp,tr,tf1,sp,sr,sf1;
	}
	
	public double spanF1() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).sf1;
	}
	public double spanRecall() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).sr;
	}
	public double spanPrecision() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).sp;
	}
	public double tokenF1() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).tf1;
	}
	public double tokenRecall() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).tr;
	}
	public double tokenPrecision() 
	{ 
		if (overallTag==null) throw new IllegalStateException("no overall measure stored");
		else return ((Stats)tagToStatsMap.get(overallTag)).tp;
	}

	public void extend(String tag,SpanDifference sd,boolean isOverallMeasure)
	{
		Stats s = new Stats();
		s.tp = sd.tokenPrecision();
		s.tr = sd.tokenRecall();
		s.tf1 = f1(s.tp,s.tr);
		s.sp = sd.spanPrecision();
		s.sr = sd.spanRecall();
		s.sf1 = f1(s.sp,s.sr);
		tagToStatsMap.put(tag, s);
		if (isOverallMeasure) overallTag = tag;
	}
	private double f1(double p,double r)
	{
		if (Double.isNaN(p)) return 0;
		else if (Double.isNaN(r)) return 0;
		else if (p==0 && r==0) return 0;
		else return 2*p*r/(p+r);
	}

	public Viewer toGUI()
	{
		Viewer v = new ComponentViewer() {
				public JComponent componentFor(Object o) {
					ExtractionEvaluation e = (ExtractionEvaluation)o;
					Object[][] table = new Object[e.tagToStatsMap.keySet().size()][7];
					int row = 0;
					for (Iterator i = e.tagToStatsMap.keySet().iterator(); i.hasNext(); ) {
						String tag = (String)i.next(); 
						table[row][0] = tag;
						Stats s = (Stats)tagToStatsMap.get(tag);
						table[row][1] = new Double(s.tp);
						table[row][2] = new Double(s.tr);
						table[row][3] = new Double(s.tf1);
						table[row][4] = new Double(s.sp);
						table[row][5] = new Double(s.sr);
						table[row][6] = new Double(s.sf1);
						row++;
					}
					String[] colNames = 
						new String[]{"Measurement Tag", 
												 "Token Prec.","Token Recall","Token F1",
												 "Span Prec.","Span Recall","Span F1"};
					return new JScrollPane(new JTable(table,colNames));
				}
			};
		v.setContent(this);
		return v;
	}
}

