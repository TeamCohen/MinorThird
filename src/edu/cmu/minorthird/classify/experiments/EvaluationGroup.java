package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.awt.*;
import java.awt.event.*;

/** A group of related evaluations.
 *
 * @author William Cohen
 */

public class EvaluationGroup implements Visible,Serializable
{
  private final Map members = new HashMap();
  private Evaluation someEvaluation=null;
    private Object[][] table;
    private String[] columnHeads;
    private JTable jtable;

  /** Add an evaluation to the group */
  public void add(String name,Evaluation evaluation)
  {
    if (someEvaluation==null) someEvaluation=evaluation;
    members.put(name,evaluation);
  }
  public String toString()
  {
    return members.toString();
  }

  public Viewer toGUI()
  {
    if (members.keySet().size()==0)
      return new VanillaViewer("empty EvaluationGroup");

    ParallelViewer main = new ParallelViewer();

    class SummaryControls extends ViewerControls
    {
	// how to sort
	private JRadioButton exportButton;
	public void initialize()
	{
	    ButtonGroup group = new ButtonGroup();;
	    exportButton = addButton("Export to file",group);			
	}
	private JRadioButton addButton(String s,ButtonGroup group)
	{
	    JRadioButton button = new JRadioButton(s);
	    group.add(button);
	    add(button);
	    button.addActionListener(this);
	    return button;
	}
    };

    // summary view
    class SummaryViewer extends ComponentViewer implements Controllable{
	private SummaryControls controls = null;
	private EvaluationGroup group = null;
	    
	public void applyControls(ViewerControls controls)	
	{	
	    this.controls = (SummaryControls)controls;	
	    setContent(group, true);
	    revalidate();
	}
	public JComponent componentFor(Object o) {
	    group = (EvaluationGroup)o;
	    columnHeads = new String[group.members.keySet().size()+1];
	    columnHeads[0] = "Statistic";
	    int k=1;
	    for (Iterator i=group.members.keySet().iterator(); i.hasNext(); ) {
		Object key = i.next();
		columnHeads[k] = (String)key; // should be name of key
		k++;
	    }
	    String[] statNames = Evaluation.summaryStatisticNames();
	    table = new Object[statNames.length][columnHeads.length];
	    for (int j=0; j<statNames.length; j++) {
		table[j][0] = statNames[j];
	    }
	    k=1;
	    for (Iterator i=group.members.keySet().iterator(); i.hasNext(); ) {
		Object key = i.next();
		Evaluation v = (Evaluation)group.members.get(key);
		double[] ss = v.summaryStatistics();
		for (int j=0; j<ss.length; j++) {
		    table[j][k] = new Double(ss[j]);
		}
		k++;
	    }
	    jtable = new JTable(table,columnHeads);

	    if(controls!=null) {
		if(controls.exportButton.isSelected()) {
		    try {
			String name1 = columnHeads[1].substring(0,columnHeads[1].indexOf("."));
			String name2 = columnHeads[2].substring(0,columnHeads[2].indexOf("."));
			File evaluation = new File(name1 + "_" + name2 + ".txt");
			FileWriter out = new FileWriter(evaluation);
	    
			for(int i=0; i<columnHeads.length; i++) {
			    out.write(columnHeads[i]);
			    out.write(", ");
			}
			out.write("\n");
			int columns = jtable.getColumnCount();
			int rows = jtable.getRowCount();
			for(int x=0; x<rows; x++) {
			    for(int y=0; y<columns; y++) {
				out.write(table[x][y].toString());
				out.write("\t ");		   
			    }
			    out.write("\n");
			}
			out.flush();
			out.close();
		    } catch (Exception e) {
			System.out.println("Error Opening Excel File");
			e.printStackTrace();
		    }

		}
	    }

	    final Transform keyTransform = new Transform() {
		    public Object transform(Object key) { return group.members.get(key); }
		};
	    monitorSelections(jtable,0,keyTransform);
	    JScrollPane scroll = new JScrollPane(jtable);	
	    return scroll;
	}
	/*public void actionPerformed(ActionEvent event) {
	    // Create Excel Spreadsheet
	    // create a new file
	    System.out.println("Action Performed!!!");
	    
	    }*/
	
    };

    main.addSubView( "Summary", new ControlledViewer(new SummaryViewer(), new SummaryControls()) );

    // for zooming in to a particular experiment
    Viewer indexViewer = new IndexedViewer() {
      public Object[] indexFor(Object o) {
        EvaluationGroup group = (EvaluationGroup)o;
        return  group.members.keySet().toArray();
      }
    };
    TransformedViewer evaluationKeyViewer = new TransformedViewer()	{
      public Object transform(Object o) {
        return members.get(o);
      }
    };
    evaluationKeyViewer.setSubView( someEvaluation.toGUI() );
    ZoomedViewer zooomer = new ZoomedViewer(indexViewer, evaluationKeyViewer);
    zooomer.setHorizontal();

    Viewer prViewer = new ComponentViewer() {
      public JComponent componentFor(Object o) {
        EvaluationGroup group = (EvaluationGroup)o;
        LineCharter lc = new LineCharter();
        for (Iterator i=group.members.keySet().iterator(); i.hasNext(); ) {
          Object key = i.next();
          Evaluation e = (Evaluation)group.members.get(key);
          double[] p = e.elevenPointPrecision();
          lc.startCurve( key.toString() );
          for (int j=0; j<=10; j++) {
            lc.addPoint(j/10.0, p[j]);
          }
        }
        return lc.getPanel("11Pt Interpolated Precision vs Recall", "Recall", "Precision");
      }
    };
    main.addSubView("Interpolated Precision/Recall",prViewer);

    main.addSubView( "Details", zooomer );
    main.setContent(this);
    return main;
  }

  //
  // test routine
  //
  static public void main(String[] args)
  {
    try {
      EvaluationGroup group = new EvaluationGroup();
      for (int i=0; i<args.length; i++) {
        Evaluation v = Evaluation.load(new File(args[i]));
        group.add( args[i], v );
      }
      ViewerFrame f = new ViewerFrame("From file "+args[0], group.toGUI());
    } catch (Exception e) {
      System.out.println("usage: EvaluationGroup serializedFile1 serializedFile2 ...");
      e.printStackTrace();
    }
  }
}
