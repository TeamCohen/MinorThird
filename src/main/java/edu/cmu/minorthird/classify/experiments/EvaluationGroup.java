package edu.cmu.minorthird.classify.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.IndexedViewer;
import edu.cmu.minorthird.util.gui.LineCharter;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * A group of related evaluations.
 * 
 * @author William Cohen
 */

public class EvaluationGroup implements Visible,Serializable,Saveable{

	static final long serialVersionUID=20080130L;
	
	private final SortedMap<String,Evaluation> members=new TreeMap<String,Evaluation>();

	private Evaluation someEvaluation=null;

	private SummaryViewer sv;

	/** Add an evaluation to the group */
	public void add(String name,Evaluation evaluation){
		if(someEvaluation==null)
			someEvaluation=evaluation;
		members.put(name,evaluation);
	}

	/** Return an iterator for the names of evaluations in the group. */
	public Iterator<String> evalNameIterator(){
		return members.keySet().iterator();
	}

	/** Return the evaluation associated with this name. */
	public Evaluation getEvaluation(String name){
		return members.get(name);
	}

	@Override
	public String toString(){
		return members.toString();
	}

	// summary view
	class SummaryViewer extends ComponentViewer{
		
		static final long serialVersionUID=20080130L;

		private EvaluationGroup group=null;

		public Object[][] table;

		public String[] columnHeads;

		public JTable jtable;

		@Override
		public JComponent componentFor(Object o){
			group=(EvaluationGroup)o;
			columnHeads=new String[group.members.keySet().size()+1];
			columnHeads[0]="Statistic";
			int k=1;
			for(Iterator<String> i=group.members.keySet().iterator();i.hasNext();){
				columnHeads[k]=i.next(); // should be name of key
				k++;
			}
			// set number of summary statistics
			ExampleSchema schema=someEvaluation.getSchema();
			int statNumber=0;
			if(someEvaluation.isBinary()){
				statNumber=10+2*schema.getNumberOfClasses();
			}else{
				statNumber=3+2*schema.getNumberOfClasses();
			}
			// get summary statistics
			table=new Object[statNumber][columnHeads.length];
			k=1;
			for(Iterator<String> i=group.members.keySet().iterator();i.hasNext();){
				Evaluation v=group.members.get(i.next());
				String[] statNames=v.summaryStatisticNames();
				double[] ss=v.summaryStatistics();
				for(int j=0;j<ss.length;j++){
					table[j][0]=statNames[j];
					table[j][k]=new Double(ss[j]);
				}
				k++;
			}
			jtable=new JTable(table,columnHeads);

			final Transform keyTransform=new Transform(){

				@Override
				public Object transform(Object key){
					return group.members.get(key);
				}
			};
			monitorSelections(jtable,0,keyTransform);
			JScrollPane scroll=new JScrollPane(jtable);
			return scroll;
		}

	};

	@Override
	public Viewer toGUI(){
		if(members.keySet().size()==0)
			return new VanillaViewer("empty EvaluationGroup");

		ParallelViewer main=new ParallelViewer();
		sv=new SummaryViewer();
		main.addSubView("Summary",new ZoomedViewer(sv,
				new Evaluation.PropertyViewer()));

		// for zooming in to a particular experiment
		Viewer indexViewer=new IndexedViewer(){
			static final long serialVersionUID=20080130L;
			@Override
			public Object[] indexFor(Object o){
				EvaluationGroup group=(EvaluationGroup)o;
				return group.members.keySet().toArray();
			}
		};
		TransformedViewer evaluationKeyViewer=new TransformedViewer(){
			static final long serialVersionUID=20080130L;
			@Override
			public Object transform(Object o){
				return members.get(o);
			}
		};
		evaluationKeyViewer.setSubView(someEvaluation.toGUI());
		ZoomedViewer zooomer=new ZoomedViewer(indexViewer,evaluationKeyViewer);
		zooomer.setHorizontal();

		// precision recall
		Viewer prViewer=new ComponentViewer(){
			static final long serialVersionUID=20080130L;
			@Override
			public JComponent componentFor(Object o){
				EvaluationGroup group=(EvaluationGroup)o;
				LineCharter lc=new LineCharter();
				for(Iterator<String> i=group.members.keySet().iterator();i.hasNext();){
					String key=i.next();
					Evaluation e=group.members.get(key);
					double[] p=e.elevenPointPrecision();
					lc.startCurve(key);
					for(int j=0;j<=10;j++){
						lc.addPoint(j/10.0,p[j]);
					}
				}
				return lc.getPanel("11Pt Interpolated Precision vs Recall","Recall",
						"Precision");
			}
		};
		Viewer avgPRViewer=new ComponentViewer(){
			static final long serialVersionUID=20080130L;
			@Override
			public JComponent componentFor(Object o){
				EvaluationGroup group=(EvaluationGroup)o;
				LineCharter lc=new LineCharter();
				double[] sum=new double[11];
				int n=0;
				for(Iterator<String> i=group.members.keySet().iterator();i.hasNext();){
					String key=i.next();
					Evaluation e=group.members.get(key);
					double[] p=e.elevenPointPrecision();
					for(int j=0;j<=10;j++)
						sum[j]+=p[j];
					n++;
				}
				lc.startCurve("Average of all "+n+" 11pt P-R Curves");
				for(int j=0;j<=10;j++){
					lc.addPoint(j/10.0,sum[j]/n);
				}
				return lc.getPanel("Averaged 11Pt Interpolated Precision vs Recall",
						"Recall","Precision");
			}
		};
		main.addSubView("11Pt Precision/Recall - Details",prViewer);
		main.addSubView("11Pt Precision/Recall - Average",avgPRViewer);
		main.addSubView("Details",zooomer);
		main.setContent(this);
		return main;
	}

	//
	// Implement Saveable interface.
	//
	private static final String FORMAT_NAME="Evaluation Group";

	@Override
	public String[] getFormatNames(){
		return new String[]{FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String s){
		return ".xls";
	}

	@Override
	public void saveAs(File file,String format) throws IOException{
		if(!format.equals(FORMAT_NAME))
			throw new IllegalArgumentException("illegal format "+format);
		try{
//			String name1=
//					sv.columnHeads[1].substring(0,sv.columnHeads[1].indexOf("."));
//			String name2=
//					sv.columnHeads[2].substring(0,sv.columnHeads[2].indexOf("."));
			// File evaluation = new File(name1 + "_" + name2 + ".txt");
			// FileWriter out = new FileWriter(evaluation);
			PrintStream out=new PrintStream(new FileOutputStream(file));

			for(int i=0;i<sv.columnHeads.length;i++){
				out.print(sv.columnHeads[i]);
				out.print("\t");
			}
			out.print("\n");
			int columns=sv.jtable.getColumnCount();
			int rows=sv.jtable.getRowCount();
			for(int x=0;x<rows;x++){
				for(int y=0;y<columns;y++){
					out.print(sv.table[x][y].toString());
					out.print("\t ");
				}
				out.print("\n");
			}
			out.flush();
			out.close();
		}catch(Exception e){
			System.out.println("Error Opening Excel File");
			e.printStackTrace();
		}
	}

	@Override
	public Object restore(File file) throws IOException{
		throw new UnsupportedOperationException(
				"Cannot load EvaluationGroup object");
	}

	//
	// test routine
	//
	static public void main(String[] args){
		try{
			EvaluationGroup group=new EvaluationGroup();
			for(int i=0;i<args.length;i++){
				try{
					Evaluation v=Evaluation.load(new File(args[i]));
					group.add(args[i],v);
				}catch(IOException ex){
					try{
						Evaluation v=(Evaluation)IOUtil.loadSerialized(new File(args[i]));
						group.add(args[i],v);
					}catch(Exception ex2){
						System.out
								.println("usage: EvaluationGroup serializedFile1 serializedFile2 ...");
						ex2.printStackTrace();
					}
				}
			}
			new ViewerFrame("From file "+args[0],group.toGUI());
		}catch(Exception e){
			System.out
					.println("usage: EvaluationGroup serializedFile1 serializedFile2 ...");
			e.printStackTrace();
		}
	}
}
