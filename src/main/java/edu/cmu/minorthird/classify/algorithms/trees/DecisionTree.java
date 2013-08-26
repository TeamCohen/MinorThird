package edu.cmu.minorthird.classify.algorithms.trees;

import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * A decision tree.
 * 
 * @author William Cohen
 */

/* package */abstract class DecisionTree extends BinaryClassifier implements
		Serializable,Visible{

	static final long serialVersionUID=20080609L;

	/** Print routine */
	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		toString(buf,0);
		return buf.toString();
	}

	public void toString(StringBuffer buf,int tab){
		for(int i=0;i<tab;i++)
			buf.append("|  ");
		if(this instanceof InternalNode){
			InternalNode in=(InternalNode)this;
			buf.append(in.test+">="+in.threshold+":\n");
			// in.getTrueBranch().toString(buf,tab+1);
			// in.getFalseBranch().toString(buf,tab+1);
		}else{
			Leaf leaf=(Leaf)this;
			buf.append(leaf.getScore()+"\n");
		}
	}

	/**
	 * An internal node of a decision tree.
	 */
	public static class InternalNode extends DecisionTree implements Visible{

		static final long serialVersionUID=20080609L;

		private Feature test;

		private double threshold;

		private DecisionTree ifTrue,ifFalse;

		public InternalNode(Feature test,DecisionTree ifTrue,DecisionTree ifFalse){
			this(test,0.5,ifTrue,ifFalse);
		}

		public InternalNode(Feature test,double threshold,DecisionTree ifTrue,
				DecisionTree ifFalse){
			this.test=test;
			this.threshold=threshold;
			this.ifTrue=ifTrue;
			this.ifFalse=ifFalse;
		}

		@Override
		public String explain(Instance instance){
			if(instance.getWeight(test)>=threshold){
				return test+"="+instance.getWeight(test)+">="+threshold+"\n"+
						ifTrue.explain(instance);
			}else{
				return test+"="+instance.getWeight(test)+"<"+threshold+"\n"+
						ifFalse.explain(instance);
			}
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=new Explanation.Node("DecisionTree Explanation");
			if(instance.getWeight(test)>=threshold){
				Explanation.Node node=
						new Explanation.Node(test+"="+instance.getWeight(test)+">="+
								threshold);
				Explanation.Node childEx=ifTrue.getExplanation(instance).getTopNode();
				node.add(childEx);
				top.add(node);
			}else{
				Explanation.Node node=
						new Explanation.Node(test+"="+instance.getWeight(test)+"<"+
								threshold);
				Explanation.Node childEx=ifFalse.getExplanation(instance).getTopNode();
				node.add(childEx);
				top.add(node);
			}
			Explanation ex=new Explanation(top);
			return ex;
		}

		@Override
		public double score(Instance instance){
			if(instance.getWeight(test)>=threshold)
				return ifTrue.score(instance);
			else
				return ifFalse.score(instance);
		}

		public DecisionTree getTrueBranch(){
			return ifTrue;
		}

		public DecisionTree getFalseBranch(){
			return ifFalse;
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new TreeViewer();
			v.setContent(this);
			return v;
		}
	}

	/**
	 * A decision tree leaf.
	 */
	public static class Leaf extends DecisionTree implements Visible{

		static final long serialVersionUID=20080609L;

		private double myScore;

		public Leaf(double myScore){
			this.myScore=myScore;
		}

		@Override
		public String explain(Instance instance){
			return "leaf: "+myScore;
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=new Explanation.Node("leaf: "+myScore);
			Explanation ex=new Explanation(top);
			return ex;
		}

		@Override
		public double score(Instance instance){
			return myScore;
		}

		public double getScore(){
			return myScore;
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new TreeViewer();
			v.receiveContent(this);
			return v;
		}
	}

	public static class TreeViewer extends ComponentViewer{

		static final long serialVersionUID=20080609L;
		
		@Override
		public JComponent componentFor(Object o){
			DecisionTree dtree=(DecisionTree)o;
			DefaultMutableTreeNode top=createNodes(dtree);
			final JTree jtree=new JTree(top);
			jtree.addTreeSelectionListener(new TreeSelectionListener(){

				@Override
				public void valueChanged(TreeSelectionEvent e){
					DefaultMutableTreeNode node=
							(DefaultMutableTreeNode)jtree.getLastSelectedPathComponent();
					Object nodeInfo=node.getUserObject();
					if(nodeInfo instanceof InternalNode){
						sendSignal(OBJECT_SELECTED,((InternalNode)nodeInfo).test);
					}
				}
			});
			return new JScrollPane(jtree);
		}

		private DefaultMutableTreeNode createNodes(DecisionTree dtree){
			if(dtree instanceof Leaf){
				return new DefaultMutableTreeNode(dtree);
			}else{
				InternalNode internal=(InternalNode)dtree;
				DefaultMutableTreeNode n=new DefaultMutableTreeNode(internal);
				n.add(createNodes(internal.ifTrue));
				n.add(createNodes(internal.ifFalse));
				return n;
			}
		}
	}
}
