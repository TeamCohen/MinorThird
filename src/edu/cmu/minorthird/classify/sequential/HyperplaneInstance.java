package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;


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
				public JComponent componentFor(Object o) {
					HyperplaneInstance hi = (HyperplaneInstance)o;
					return hi.hyperplane.toGUI(); 
				}
			};
		v.setContent(this);
		return v;
	}
	public double getWeight(Feature f) { return hyperplane.featureScore(f); }
	public Feature.Looper binaryFeatureIterator() { return new Feature.Looper(Collections.EMPTY_SET); }
	public Feature.Looper numericFeatureIterator() { return hyperplane.featureIterator(); }
	public Feature.Looper featureIterator() { return hyperplane.featureIterator(); }
	public double getWeight() { return 1.0; }
	public Object getSource() { return source; }
	public String getSubpopulationId() { return subpopulationId; }
	// iterate over all hyperplane features except the bias feature
	private class MyIterator implements Iterator
	{
		private Iterator i;
		private Object myNext = null; // buffers the next nonbias feature produced by i
		public MyIterator() { this.i = hyperplane.featureIterator(); advance(); }
		private void advance() 
		{
			if (!i.hasNext()) myNext = null;
			else { 
				myNext = i.next();
				if (myNext.equals(Hyperplane.BIAS_TERM)) advance();
			}
		}
		public void remove() { throw new UnsupportedOperationException("can't remove"); }
		public boolean hasNext() { return myNext!=null; }
		public Object next() { Object result=myNext; advance(); return result; }
	}
}

