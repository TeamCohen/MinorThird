package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/** A group of related evaluations.
 *
 * @author William Cohen
 */

public class EvaluationGroup implements Visible,Serializable
{
  private final Map members = new HashMap();
  private Evaluation someEvaluation=null;

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

    // summary view
    Viewer summaryView = new ComponentViewer() {
      public JComponent componentFor(Object o) {
        final EvaluationGroup group = (EvaluationGroup)o;
				String[] columnHeads = new String[group.members.keySet().size()+1];
				columnHeads[0] = "Statistic";
				int k=1;
        for (Iterator i=group.members.keySet().iterator(); i.hasNext(); ) {
          Object key = i.next();
          columnHeads[k] = (String)key; // should be name of key
					k++;
				}
        String[] statNames = Evaluation.summaryStatisticNames();
        Object[][] table = new Object[statNames.length][columnHeads.length];
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
        JTable jtable = new JTable(table,columnHeads);
        final Transform keyTransform = new Transform() {
          public Object transform(Object key) { return group.members.get(key); }
        };
        monitorSelections(jtable,0,keyTransform);
        return new JScrollPane(jtable);
      }
    };

    main.addSubView( "Summary", new ZoomedViewer(summaryView, new Evaluation.PropertyViewer()) );

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
