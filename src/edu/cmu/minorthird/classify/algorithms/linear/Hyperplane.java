/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.linear;
import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.*;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import javax.swing.*;
import javax.swing.JTree;
import javax.swing.tree.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/** A weighted combination of features.
 *
 * @author William Cohen
 */

public class Hyperplane extends BinaryClassifier implements Visible, Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_SERIAL_VERSION = 1;

	/** 
	 * Weight for an invisible 'bias feature' which is considered to be
	 * present in every instance.  In otherwords, as a classifier, the
	 * score of a hyperplane h on an instance x is sum_{feature f}
	 * x.score(f)*h.featureScore(f) + h.featureScore(BIAS_TERM).
	 */
	public static final Feature BIAS_TERM = new Feature("_hyperplaneBias");
	protected TObjectDoubleHashMap hyperplaneWeights = new TObjectDoubleHashMap();
	private boolean ignoreWeights = false;

	public Hyperplane() { ; }

	/** After this call is made, the hyperplane will assume that all
	 * feature weights are one in instances.  Specifically in calls to
	 * increment(instance,delta) or increment(hyperplane,delta), feature
	 * weights will be assumed to be zero.  For backward compatibility
	 * with an old buggy version.
	 */
	public void startIgnoringWeights() { this.ignoreWeights=true; }

	/** Inner product of hyperplane and instance weights. */
	public double score(Instance instance) 
	{
		double score = 0.0;
		for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
			Feature f = j.nextFeature();
			score += instance.getWeight(f) * featureScore(f) ;
		}
		score += featureScore( BIAS_TERM );
		return score;
	}

	/** Justify inner product of hyperplane and instance weights. */
	public String explain(Instance instance) 
	{
		StringBuffer buf = new StringBuffer("");
		for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
			Feature f = j.nextFeature();
			if (buf.length()>0) buf.append("\n + ");
			else buf.append("   ");
			buf.append( f+"<"+instance.getWeight(f)+"*"+featureScore(f)+">");
		}
		buf.append( "\n + bias<"+featureScore( BIAS_TERM )+">" );
		buf.append("\n = "+score(instance) );
		return buf.toString();
	}


	/** Increment one feature from the hyperplane by delta */
	public void increment(Feature f, double delta) {
		double d = hyperplaneWeights.get(f);
		hyperplaneWeights.put(f, d+delta);
	}

	/** Increment the bias term for the hyperplane by delta */
	public void incrementBias(double delta) {
		increment(BIAS_TERM, delta);
	}

	/** Set the bias term for the hyperplane to delta */
	public void setBias(double delta) {
		hyperplaneWeights.remove(BIAS_TERM);
		hyperplaneWeights.put(BIAS_TERM, delta);
	}

	/** Add the value of the features in the instance to this hyperplane. */
	public void increment(Instance instance, double delta) {
		for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			double w = ignoreWeights? 1: instance.getWeight(f);
			increment( f, w * delta );
		}
		incrementBias( delta );
	}

	/** Multiply all weights by a factor */
	public void multiply(double factor) 
	{
		for (Feature.Looper i=featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			hyperplaneWeights.put( f, featureScore(f) * factor );
		}
	}

	/** Add hyperplane b*delta to this hyperplane. */
	public void increment(Hyperplane b, double delta) {
		for (TObjectDoubleIterator i=b.hyperplaneWeights.iterator(); i.hasNext(); ) {
			i.advance();
			Feature f = (Feature)i.key();
			double w = b.featureScore( f );
			increment( f, w * delta );
		}
	}

	/** Add hyperplane b to this hyperplane. */
	public void increment(Hyperplane b) {
		increment(b,1.0);
	}

	/** Weight for a feature in the hyperplane. */
	public double featureScore( Feature feature ) {
		return  hyperplaneWeights.get(feature);
	}

	/** Iterate over all features with non-zero weight. */
	public Feature.Looper featureIterator()	
	{
		final TObjectDoubleIterator ti = hyperplaneWeights.iterator();
		Iterator i = new Iterator() {
				public boolean hasNext() { return ti.hasNext(); }
				public Object next() { ti.advance(); return ti.key(); }
				public void remove() { ti.remove(); }
			};
		return new Feature.Looper(i);
	}

	//
	// UI stuff
	//

	public Viewer toGUI()
	{
		Viewer gui = new ControlledViewer(new MyViewer(), new HyperplaneControls());
		gui.setContent(this);
		return gui;
	}

	static private class HyperplaneControls extends ViewerControls
	{
		// how to sort
	    private JRadioButton absoluteValueButton,valueButton,nameButton,treeButton,noneButton;
		public void initialize()
		{
		        ButtonGroup group = new ButtonGroup();;
		        treeButton = addButton("Tree view",group,false);
			add(new JLabel("   OR  "));
			add(new JLabel("   Sort by"));			
			nameButton = addButton("name",group,true);
			valueButton = addButton("weight",group,false);
			absoluteValueButton = addButton("|weight|",group,false);			
		}
		private JRadioButton addButton(String s,ButtonGroup group,boolean selected)
		{
			JRadioButton button = new JRadioButton(s,selected);
			group.add(button);
			add(button);
			button.addActionListener(this);
			return button;
		}
	}

	static private class MyViewer extends ComponentViewer implements Controllable
	{
		private HyperplaneControls controls = null;
		private Hyperplane h = null;
	    //private Vector features = new Vector();

		public void applyControls(ViewerControls controls)	
		{	
			this.controls = (HyperplaneControls)controls;	
			setContent(h,true);
			revalidate();
		}
		public boolean canReceive(Object o) {	return o instanceof Hyperplane;	}	    	    

	    public void createNodes(DefaultMutableTreeNode top, Object[][] data, int start, int len, int level) {
		//DefaultMutableTreeNode node = top;
		int size = ((Feature)data[start][0]).size();
		String whole = ((Feature)data[start][0]).toString();
		double max = ((Double)data[start][1]).doubleValue();
	       
		if(level <= size) {		    		    
		    String tok = ((Feature)data[start][0]).getPart(level);

		    int i = start+1;
		    String nextTok = ((Feature)data[i][0]).getPart(level);
		    double weight = ((Double)data[i][1]).doubleValue();
		    int flag = 0;
		    while(nextTok.equals(tok) && i<len-1) {
			if(weight > max)
			    max = weight;
			i++;
			nextTok = ((Feature)data[i][0]).getPart(level);			
			weight = ((Double)data[i][1]).doubleValue();
			flag = 1;
		    }
		    
		    DefaultMutableTreeNode node = new DefaultMutableTreeNode(tok+ " (" + max + ")");
		    top.add(node);
		    if (flag == 1) {
			createNodes(node, data, start, i, level+1);
		    }
		    if(i<len-1) {
			createNodes(top, data, i, len, level);
		    }
		}
	    }

	    public JTree createTree(Object[][] data, int len) {
		    JTree tree;
		    DefaultMutableTreeNode top = new DefaultMutableTreeNode("Features Tree");
		    createNodes(top, data, 0, len, 0);
		    tree = new JTree(top);		    

		    return tree;
		}
		    
		public JComponent componentFor(Object o) 
		{
			h = (Hyperplane)o;
			Object[] keys = h.hyperplaneWeights.keys();
			Object[][] tableData = new Object[keys.length][2];
			int k=0;
			for (Feature.Looper i=h.featureIterator(); i.hasNext(); ) {
				Feature f = i.nextFeature();
				tableData[k][0] = f;
				tableData[k][1] = new Double( h.featureScore( f ) );
				k++;
			}
			if (controls!=null) {
				Arrays.sort(
					tableData, 
					new Comparator() {
						public int compare(Object a,Object b) {
							Object[] ra = (Object[])a;
							Object[] rb = (Object[])b;
							if (controls.nameButton.isSelected() || controls.treeButton.isSelected()) 
								return ra[0].toString().compareTo(rb[0].toString());
							Double da = (Double)ra[1];
							Double db = (Double)rb[1];
							if (controls.valueButton.isSelected())
								return MathUtil.sign( db.doubleValue() - da.doubleValue() );
							else
								return MathUtil.sign( Math.abs(db.doubleValue()) - Math.abs(da.doubleValue()) );
						}
					});
			
			JTree tree;
			if(controls.treeButton.isSelected()) {
			    tree = createTree(tableData, keys.length);
			    return new JScrollPane(tree);
			}
			}
			String[] columnNames = {"Feature Name", "Weight" };
			JTable table = new JTable(tableData,columnNames);
			monitorSelections(table,0);
			return new JScrollPane(table);
		}
	}


	public String toString() 
	{ 
		StringBuffer buf = new StringBuffer("[Hyperplane:");
		for (Feature.Looper i=featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			buf.append(" " + f + "=" + featureScore(f));
		}
		buf.append("]");
		return buf.toString();
	}
}

