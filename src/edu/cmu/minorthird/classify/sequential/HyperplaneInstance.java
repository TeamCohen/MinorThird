package edu.cmu.minorthird.classify.sequential;

import java.util.Collections;
import java.util.Iterator;

import javax.swing.JComponent;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;


/**
 * Wrap a hyperplane to that it supports the Instance interface.
 */

public class HyperplaneInstance implements Instance
{
	private Hyperplane hyperplane;
	private String subpopulationId;
	private Object source;
	public HyperplaneInstance(Hyperplane hyperplane,String subpopulationId,Object source) 
	{ 
		// compensate for automatic increment of bias term by linear learners
		// for some reason it seems to work better to have the bias be linear in length
		// than always zero
		hyperplane.incrementBias(-1.0);
		this.hyperplane = hyperplane; 
		this.subpopulationId = subpopulationId;
		this.source = source;
	}
	public Viewer toGUI() 
	{ 
		Viewer v = new ComponentViewer() {
			static final long serialVersionUID=20080202L;
				public JComponent componentFor(Object o) {
					HyperplaneInstance hi = (HyperplaneInstance)o;
					return hi.hyperplane.toGUI(); 
				}
			};
		v.setContent(this);
		return v;
	}
	public double getWeight(Feature f) { return hyperplane.featureScore(f); }
	public Iterator<Feature> binaryFeatureIterator() { return Collections.EMPTY_SET.iterator(); }
	public Iterator<Feature> numericFeatureIterator() { return hyperplane.featureIterator(); }
	public Iterator<Feature> featureIterator() { return hyperplane.featureIterator(); }
	public int numFeatures() { throw new UnsupportedOperationException();}
	public double getWeight() { return 1.0; }
	public Object getSource() { return source; }
	public String getSubpopulationId() { return subpopulationId; }
	// iterate over all hyperplane features except the bias feature
	// where is it used? - frank
//	private class MyIterator implements Iterator<Feature>
//	{
//		private Iterator<Feature> i;
//		private Feature myNext = null; // buffers the next nonbias feature produced by i
//		public MyIterator() { this.i = hyperplane.featureIterator(); advance(); }
//		private void advance() 
//		{
//			if (!i.hasNext()) myNext = null;
//			else { 
//				myNext = i.next();
//				if (myNext.equals(Hyperplane.BIAS_TERM)) advance();
//			}
//		}
//		public void remove() { throw new UnsupportedOperationException("can't remove"); }
//		public boolean hasNext() { return myNext!=null; }
//		public Feature next() { Feature result=myNext; advance(); return result; }
//	}
}

