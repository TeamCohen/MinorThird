package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.*;

/**
 *
 * @author William Cohen
 */

public class LeaveOneOutDictVersion implements BatchSegmenterLearner
{
	private String[] featurePattern;
	private BatchSegmenterLearner innerLearner;
    private String distanceNames;

	public LeaveOneOutDictVersion(String[] featurePattern, BatchSegmenterLearner innerLearner, String distanceNames)
	{
		this.featurePattern = featurePattern;
		this.innerLearner = innerLearner;
		this.distanceNames = distanceNames;
	}

	public LeaveOneOutDictVersion(BatchSegmenterLearner innerLearner, String distanceNames)
	{
	    this(LeaveOneOutDictTransformLearner.DEFAULT_PATTERN,innerLearner,distanceNames);
	}

    public LeaveOneOutDictVersion(BatchSegmenterLearner innerLearner) {
	this(LeaveOneOutDictTransformLearner.DEFAULT_PATTERN,innerLearner,"Jaccard");
    }
	public void setSchema(ExampleSchema schema)
	{
		;
	}

	public Segmenter batchTrain(SegmentDataset dataset)
	{
		LeaveOneOutDictTransformLearner transformLearner = new LeaveOneOutDictTransformLearner(featurePattern, distanceNames);
		InstanceTransform transform = transformLearner.batchTrain(dataset);
		SegmentTransform segmentTransform = new SegmentTransform(transform);
		SegmentDataset transformedDataset = segmentTransform.transform(dataset);
		//new ViewerFrame("transformedDataset", new SmartVanillaViewer(transformedDataset));
		Segmenter segmenter = innerLearner.batchTrain( transformedDataset );
		return new TransformingSegmenter( transform, segmenter ); 
	}

	static public class TransformingSegmenter implements Segmenter,Serializable,Visible
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		private InstanceTransform instanceTransform;
		private Segmenter segmenter;
		public TransformingSegmenter(InstanceTransform instanceTransform,Segmenter segmenter)
		{
			this.instanceTransform = instanceTransform;
			this.segmenter = segmenter;
		}
		public Segmentation segmentation(CandidateSegmentGroup group)
		{
			return segmenter.segmentation( new SegmentTransform(instanceTransform).transform(group) );
		}
		public String explain(CandidateSegmentGroup group)
		{
			return "not implemented";
		}
		public Viewer toGUI()
		{
			Viewer v = new ComponentViewer() {
					public JComponent componentFor(Object o) {
						TransformingSegmenter ts = (TransformingSegmenter)o;
						JPanel mainPanel = new JPanel();
						mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
						mainPanel.setBorder(new TitledBorder("TransformingSegmenter"));
						JLabel subView1 = new JLabel(ts.instanceTransform.toString());
						Viewer subView2 = new SmartVanillaViewer(ts.segmenter);
						//subView1.setSuperView(this);
						subView2.setSuperView(this);
						mainPanel.add(subView1);
						mainPanel.add(subView2);
						return new JScrollPane(mainPanel);
					}
				};
			v.setContent(this);
			return v;
		}

	}

	static private class SegmentTransform
	{
		private InstanceTransform innerTransform;

		public SegmentTransform(InstanceTransform innerTransform) { this.innerTransform = innerTransform; }

		public SegmentDataset transform(SegmentDataset dataset)
		{ 
			ProgressCounter pc = 
				new ProgressCounter("adding dictionary distances","segment group",dataset.getNumberOfSegmentGroups());
			SegmentDataset transformed = new SegmentDataset();
			for (SegmentDataset.Looper i = dataset.candidateSegmentGroupIterator(); i.hasNext(); ) {
				transformed.addCandidateSegmentGroup( transform(i.nextCandidateSegmentGroup()) );
				pc.progress();
			}
			pc.finished();
			return transformed;
		}
		public CandidateSegmentGroup transform(CandidateSegmentGroup g) 
		{ 
			CandidateSegmentGroup result = new CandidateSegmentGroup(g.getMaxWindowSize(), g.getSequenceLength());
			for (int lo=0; lo<g.getSequenceLength(); lo++) {
				for (int len=1; len<=g.getMaxWindowSize(); len++) {
					Instance gInstance = g.getSubsequenceInstance(lo,lo+len);
					if (gInstance!=null) {
						Instance tInstance = innerTransform.transform(gInstance);
						//Feature f = new Feature("distToSome POS");
						//if (tInstance.getWeight(f)!=0) System.out.println("useful instance: "+tInstance);
						result.setSubsequence(lo, lo+len, tInstance, g.getSubsequenceLabel(lo,lo+len));
					}
				}
			}
			return result;
		}
	}
}

