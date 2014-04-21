package edu.cmu.minorthird.classify.algorithms.trees;

import java.io.Serializable;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;

// java doesn't have structs..so lets try to compact memory by hand. ICK
public class CompactDecisionTree extends BinaryClassifier implements
		Serializable //,Visible
{

	static final long serialVersionUID=20080609L;

	// do we need this?
	private final int initialVectorSize=10;

	private int currentElt=0;

	private int rootNode=-1;

	public void setRoot(int root){
		rootNode=root;
	}

	// the data
	// ---------------------------------------------------------

	private boolean[] isLeaf=new boolean[initialVectorSize];

	private Feature[] feature=new Feature[initialVectorSize];

	private double[] threshold=new double[initialVectorSize];

	private int[] trueBranch=new int[initialVectorSize];

	private int[] falseBranch=new int[initialVectorSize];

	private double[] score=new double[initialVectorSize];

	/** Print routine */
	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		toString(buf,0);
		return buf.toString();
	}

	public void toString(StringBuffer buf,int tab){
		/*
		for (int i=0; i<tab; i++) buf.append("|  ");
		if (this instanceof InternalNode) {
		  InternalNode in = (InternalNode)this;
		  buf.append(in.test+">="+in.threshold+":\n");
		  //in.getTrueBranch().toString(buf,tab+1);
		  //in.getFalseBranch().toString(buf,tab+1);
		} else {
		  Leaf leaf = (Leaf)this;
		  buf.append(leaf.getScore()+"\n");
		}
		 */
	}

	public void compactStorage(){

// 1.6 code
//     isLeaf     = Arrays.copyOf(isLeaf,      currentElt);
//     score      = Arrays.copyOf(score,       currentElt);
//     feature    = Arrays.copyOf(feature,     currentElt);
//     threshold  = Arrays.copyOf(threshold,   currentElt);
//     trueBranch = Arrays.copyOf(trueBranch,  currentElt);
//     falseBranch= Arrays.copyOf(falseBranch, currentElt);

// Icky 1.5 code

		boolean[] isLeafTmp=new boolean[currentElt];
		Feature[] featureTmp=new Feature[currentElt];
		double[] thresholdTmp=new double[currentElt];
		int[] trueBranchTmp=new int[currentElt];
		int[] falseBranchTmp=new int[currentElt];
		double[] scoreTmp=new double[currentElt];

		System.arraycopy(isLeaf,0,isLeafTmp,0,currentElt);
		System.arraycopy(feature,0,featureTmp,0,currentElt);
		System.arraycopy(threshold,0,thresholdTmp,0,currentElt);
		System.arraycopy(trueBranch,0,trueBranchTmp,0,currentElt);
		System.arraycopy(falseBranch,0,falseBranchTmp,0,currentElt);
		System.arraycopy(score,0,scoreTmp,0,currentElt);

		isLeaf=isLeafTmp;
		score=scoreTmp;
		feature=featureTmp;
		threshold=thresholdTmp;
		trueBranch=trueBranchTmp;
		falseBranch=falseBranchTmp;

	}

	private void expandStorage(){
		int currentSize=isLeaf.length;

		// 1.6 code
//     isLeaf     = Arrays.copyOf(isLeaf,      currentSize * 2);
//     score      = Arrays.copyOf(score,       currentSize * 2);

//     feature    = Arrays.copyOf(feature,     currentSize * 2);
//     threshold  = Arrays.copyOf(threshold,   currentSize * 2);
//     trueBranch = Arrays.copyOf(trueBranch,  currentSize * 2);
//     falseBranch= Arrays.copyOf(falseBranch, currentSize * 2);

		// 1.5 code

		boolean[] isLeafTmp=new boolean[currentSize*2];
		Feature[] featureTmp=new Feature[currentSize*2];
		double[] thresholdTmp=new double[currentSize*2];
		int[] trueBranchTmp=new int[currentSize*2];
		int[] falseBranchTmp=new int[currentSize*2];
		double[] scoreTmp=new double[currentSize*2];

		System.arraycopy(isLeaf,0,isLeafTmp,0,currentElt);
		System.arraycopy(feature,0,featureTmp,0,currentElt);
		System.arraycopy(threshold,0,thresholdTmp,0,currentElt);
		System.arraycopy(trueBranch,0,trueBranchTmp,0,currentElt);
		System.arraycopy(falseBranch,0,falseBranchTmp,0,currentElt);
		System.arraycopy(score,0,scoreTmp,0,currentElt);

		isLeaf=isLeafTmp;
		score=scoreTmp;
		feature=featureTmp;
		threshold=thresholdTmp;
		trueBranch=trueBranchTmp;
		falseBranch=falseBranchTmp;

	}

	public int addInternalNode(Feature test,int ifTrue,int ifFalse){
		return addInternalNode(test,0.5,ifTrue,ifFalse);
	}

	public int addInternalNode(Feature test,double thresh,int ifTrue,int ifFalse){
		if(currentElt==isLeaf.length){
			expandStorage();
		}

		isLeaf[currentElt]=false;
		score[currentElt]=0.0;

		feature[currentElt]=test;
		threshold[currentElt]=thresh;
		trueBranch[currentElt]=ifTrue;
		falseBranch[currentElt]=ifFalse;

		currentElt++;
		return currentElt-1;
	}

	public int addLeafNode(double myScore){
		if(currentElt==isLeaf.length){
			expandStorage();
		}

		isLeaf[currentElt]=true;
		score[currentElt]=myScore;

		feature[currentElt]=null;
		threshold[currentElt]=0;
		trueBranch[currentElt]=0;
		falseBranch[currentElt]=0;

		currentElt++;
		return currentElt-1;
	}

	@Override
	public double score(Instance instance){
		return score(instance,rootNode);
	}

	public double score(Instance instance,int index){
		if(isLeaf[index]){
			return score[index];
		}else if(instance.getWeight(feature[index])>=threshold[index]){
			return score(instance,trueBranch[index]);
		}else{
			return score(instance,falseBranch[index]);
		}
	}

	@Override
	public String explain(Instance instance){
		return "";
		/*
		  if (instance.getWeight(test)>=threshold) {
		return test+"="+instance.getWeight(test)+">="+threshold+"\n"+ifTrue.explain(instance);
		  } else {
		  return test+"="+instance.getWeight(test)+"<"+threshold+"\n"+ifFalse.explain(instance);
		  }*/
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=new Explanation.Node("DecisionTree Explanation");
		/*
		if (instance.getWeight(test)>=threshold) {
		  Explanation.Node node = new Explanation.Node( test+"="+instance.getWeight(test)+">="+threshold );
		  Explanation.Node childEx = ifTrue.getExplanation(instance).getTopNode();
		  node.add(childEx);
		  top.add(node);
		} else {
		  Explanation.Node node = new Explanation.Node(test+"="+instance.getWeight(test)+"<"+threshold);
		  Explanation.Node childEx = ifFalse.getExplanation(instance).getTopNode();
		  node.add(childEx);
		  top.add(node);			
		}
		 */
		Explanation ex=new Explanation(top);
		return ex;
	}

	/*
	public Viewer toGUI() {
	  Viewer v = new TreeViewer();
	  v.setContent(this);
	  return v;
	}
	 */
	/*
	public Explanation getExplanation(Instance instance) {
	  Explanation.Node top = new Explanation.Node("leaf: " +myScore);
	  Explanation ex = new Explanation(top);
	  return ex;
	}
	 */

	/*
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
	      DefaultMutableTreeNode n = new DefaultMutableTreeNode(internal);
	      n.add( createNodes(internal.ifTrue) );
	      n.add( createNodes(internal.ifFalse) );
	      return n;
	    }
	  }
	}

	 */
}
