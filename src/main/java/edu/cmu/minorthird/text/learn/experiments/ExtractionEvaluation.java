package edu.cmu.minorthird.text.learn.experiments;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Records results of evaluating an extraction-learning system.
 * 
 * @author William Cohen
 */

public class ExtractionEvaluation implements Visible,Serializable{

	// serialization stuff
	static final long serialVersionUID=20080314L;

	private Map<String,Stats> tagToStatsMap=new TreeMap<String,Stats>();

	private String overallTag=null;

	private accStats acc_s=new accStats();

	double totalTokens;

	private static class Stats implements Serializable{

		static final long serialVersionUID=20080314L;

		double tp,tr,tf1,sp,sr,sf1;
	}

	private static class accStats implements Serializable{

		static final long serialVersionUID=20080314L;

		MathUtil.Accumulator tp=new MathUtil.Accumulator();

		MathUtil.Accumulator tr=new MathUtil.Accumulator();

		MathUtil.Accumulator tf1=new MathUtil.Accumulator();

		MathUtil.Accumulator sp=new MathUtil.Accumulator();

		MathUtil.Accumulator sr=new MathUtil.Accumulator();

		MathUtil.Accumulator sf1=new MathUtil.Accumulator();
	}

	public double spanF1(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).sf1;
	}

	public double spanRecall(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).sr;
	}

	public double spanPrecision(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).sp;
	}

	public double tokenF1(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).tf1;
	}

	public double tokenRecall(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).tr;
	}

	public double tokenPrecision(){
		if(overallTag==null)
			throw new IllegalStateException("no overall measure stored");
		else
			return (tagToStatsMap.get(overallTag)).tp;
	}

	// get stdErr
	public MathUtil.Accumulator acc_sr(){
		return acc_s.sr;
	}

	public MathUtil.Accumulator acc_sp(){
		return acc_s.sp;
	}

	public MathUtil.Accumulator acc_sf1(){
		return acc_s.sf1;
	}

	public MathUtil.Accumulator acc_tr(){
		return acc_s.tr;
	}

	public MathUtil.Accumulator acc_tp(){
		return acc_s.tp;
	}

	public MathUtil.Accumulator acc_tf1(){
		return acc_s.tf1;
	}

	// count how many total tokens there are in the textBase
	public void measureTotalSize(TextBase base){
		totalTokens=0;
		for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
			totalTokens+=i.next().size();
		}
	}

	public void extend(String tag,SpanDifference sd,boolean isOverallMeasure){
		Stats s=new Stats();
		s.tp=sd.tokenPrecision();
		s.tr=sd.tokenRecall();
		s.tf1=f1(s.tp,s.tr);
		s.sp=sd.spanPrecision();
		s.sr=sd.spanRecall();
		s.sf1=f1(s.sp,s.sr);
		tagToStatsMap.put(tag,s);

		if(isOverallMeasure){
			overallTag=tag;
		}else{
			acc_s.tp.add(s.tp);
			acc_s.tr.add(s.tr);
			acc_s.tf1.add(s.tf1);
			acc_s.sp.add(s.sp);
			acc_s.sr.add(s.sr);
			acc_s.sf1.add(s.sf1);
		}
	}

	private double f1(double p,double r){
		if(Double.isNaN(p))
			return 0;
		else if(Double.isNaN(r))
			return 0;
		else if(p==0&&r==0)
			return 0;
		else
			return 2*p*r/(p+r);
	}

	// a simple display of stdErr for now
	public void printAccStats(){
		System.out.println("\n \n Test Partitions Statistics: \n");
		System.out.println("\t\t n \t stdErr");
		System.out.println("tokenPrecision \t"+acc_s.tp.numberOfValues()+"\t"+
				acc_s.tp.stdErr());
		System.out.println("tokenRecall \t"+acc_s.tr.numberOfValues()+"\t"+
				acc_s.tr.stdErr());
		System.out.println("tokenF1 \t"+acc_s.tf1.numberOfValues()+"\t"+
				acc_s.tf1.stdErr());
		System.out.println("spanPrecision \t"+acc_s.sp.numberOfValues()+"\t"+
				acc_s.sp.stdErr());
		System.out.println("spanRecall \t"+acc_s.sr.numberOfValues()+"\t"+
				acc_s.sr.stdErr());
		System.out.println("spanF1 \t\t"+acc_s.sf1.numberOfValues()+"\t"+
				acc_s.sf1.stdErr());
	}

	@Override
	public Viewer toGUI(){
		Viewer v=new ComponentViewer(){
			static final long serialVersionUID=20080314L;
			@Override
			public JComponent componentFor(Object o){
				ExtractionEvaluation e=(ExtractionEvaluation)o;
				Object[][] table=new Object[e.tagToStatsMap.keySet().size()][7];
				int row=0;
				for(Iterator<String> i=e.tagToStatsMap.keySet().iterator();i.hasNext();){
					String tag=i.next();
					table[row][0]=tag;
					Stats s=tagToStatsMap.get(tag);
					table[row][1]=new Double(s.tp);
					table[row][2]=new Double(s.tr);
					table[row][3]=new Double(s.tf1);
					table[row][4]=new Double(s.sp);
					table[row][5]=new Double(s.sr);
					table[row][6]=new Double(s.sf1);
					row++;
				}
				String[] colNames=
						new String[]{"Measurement Tag","Token Prec.","Token Recall",
								"Token F1","Span Prec.","Span Recall","Span F1"};
				return new JScrollPane(new JTable(table,colNames));
			}
		};
		v.setContent(this);
		return v;
	}

	static public void main(String args[]) throws IOException{
		if(args.length==0){
			System.out
					.println("usage: ExtractionEvaluation serialized-evaluation-file1 [serialized-evaluation-file2...]");
		}else{
			System.out.println("     \ttoken\t  \t      \tspan");
			System.out.println("recall\tprec\tF1\trecall\tprec\tF1\tfile");
			for(int i=0;i<args.length;i++){
				ExtractionEvaluation e=
						(ExtractionEvaluation)IOUtil.loadSerialized(new File(args[i]));
				java.text.DecimalFormat fmt=new java.text.DecimalFormat("###.00\t");
				System.out.print(fmt.format(e.tokenRecall()));
				System.out.print(fmt.format(e.tokenPrecision()));
				System.out.print(fmt.format(e.tokenF1()));
				System.out.print(fmt.format(e.spanRecall()));
				System.out.print(fmt.format(e.spanPrecision()));
				System.out.print(fmt.format(e.spanF1()));
				System.out.println(args[i]);
			}
		}
	}
}
