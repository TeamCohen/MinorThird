package edu.cmu.minorthird.classify.algorithms.trees;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.Serializable;

/** A decision tree.
 *
 * @author William Cohen
 */

/*package*/ abstract class DecisionTree extends BinaryClassifier implements Serializable,Visible
{
  static private final long serialVersionUID = 1;
  private final int CURRENT_SERIAL_VERSION = 1;

  /** Print routine */
  public String toString() 
  { 
    StringBuffer buf = new StringBuffer("");
    toString(buf,0);
    return buf.toString();
  }
  public void toString(StringBuffer buf,int tab) 
  { 
    for (int i=0; i<tab; i++) buf.append("|  ");
    if (this instanceof InternalNode) {
      InternalNode in = (InternalNode)this;
      buf.append(in.test+">="+in.threshold+":\n");
      in.getTrueBranch().toString(buf,tab+1);
      in.getFalseBranch().toString(buf,tab+1);
    } else {
      Leaf leaf = (Leaf)this;
      buf.append(leaf.getScore()+"\n");
    }
  }


  /** An internal node of a  decision tree.
   */
  public static class InternalNode extends DecisionTree implements Visible
  {
    static private final long serialVersionUID = 1;
    private final int CURRENT_SERIAL_VERSION = 1;
    private Feature test;
    private double threshold;
    private DecisionTree ifTrue,ifFalse;
    public InternalNode(Feature test,DecisionTree ifTrue,DecisionTree ifFalse) 
    { 
      this(test,0.5,ifTrue,ifFalse);
    }
    public InternalNode(Feature test,double threshold,DecisionTree ifTrue,DecisionTree ifFalse) 
    { 
      this.test=test; 
      this.threshold=threshold; 
      this.ifTrue = ifTrue;
      this.ifFalse = ifFalse;
    }
    public String explain(Instance instance)
    {
      if (instance.getWeight(test)>=threshold) {
	return test+"="+instance.getWeight(test)+">="+threshold+"\n"+ifTrue.explain(instance);
      } else {
	return test+"="+instance.getWeight(test)+"<"+threshold+"\n"+ifFalse.explain(instance);				
      }
    }
    public double score(Instance instance)
    {
      if (instance.getWeight(test)>=threshold)
	return ifTrue.score(instance);
      else 
	return ifFalse.score(instance);
    }
    public DecisionTree getTrueBranch()
    {
      return ifTrue;
    }
    public DecisionTree getFalseBranch()
    {
      return ifFalse;
    }
    public Viewer toGUI()
    {
      Viewer v = new TreeViewer();
      v.setContent(this);
      return v;
    }
  }

  /** A decision tree leaf.
   */
  public static class Leaf extends DecisionTree implements Visible
  {
    private double myScore;

    public Leaf(double myScore)
    { 
      this.myScore = myScore;
    }
    public String explain(Instance instance)
    {
      return "leaf: "+myScore;
    }
    public double score(Instance instance) 
    { 
      return myScore; 
    }
    public double getScore() 
    { 
      return myScore; 
    }
    public Viewer toGUI()
    {
      Viewer v = new TreeViewer();
      v.receiveContent(this);
      return v;
    }
  }

  public static class TreeViewer extends ComponentViewer
  {
    public JComponent componentFor(Object o)
    {
      DecisionTree dtree = (DecisionTree)o;
      DefaultMutableTreeNode top = createNodes(dtree);
      final JTree jtree = new JTree(top);
      jtree.addTreeSelectionListener(new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)jtree.getLastSelectedPathComponent();
            Object nodeInfo = node.getUserObject();
            if (nodeInfo instanceof InternalNode) {
              sendSignal(OBJECT_SELECTED, ((InternalNode)nodeInfo).test);
            }
          }
        });
      return new JScrollPane(jtree);
    }
    private DefaultMutableTreeNode createNodes(DecisionTree dtree)
    {
      if (dtree instanceof Leaf) {
        return new DefaultMutableTreeNode(dtree);
      } else {
        InternalNode internal = (InternalNode)dtree;
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(dtree);
        n.add( createNodes(internal.ifTrue) );
        n.add( createNodes(internal.ifFalse) );
        return n;
      }
    }
  }
}
