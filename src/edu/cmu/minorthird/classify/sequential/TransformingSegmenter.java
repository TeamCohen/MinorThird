package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;
import java.io.*;

/*package*/ class TransformingSegmenter implements Segmenter,Serializable,Visible
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

