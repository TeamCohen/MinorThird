package edu.cmu.minorthird.classify.sequential;

import java.io.Serializable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/*package*/ class TransformingSegmenter implements Segmenter,Serializable,Visible
{
  static private final long serialVersionUID = 20080207L;
  private InstanceTransform instanceTransform;
  private Segmenter segmenter;
  public TransformingSegmenter(InstanceTransform instanceTransform,Segmenter segmenter)
  {
    this.instanceTransform = instanceTransform;
    this.segmenter = segmenter;
  }
  @Override
	public Segmentation segmentation(CandidateSegmentGroup group)
  {
    return segmenter.segmentation( new SegmentTransform(instanceTransform).transform(group) );
  }
  @Override
	public String explain(CandidateSegmentGroup group)
  {
    return "not implemented";
  }
  @Override
	public Viewer toGUI()
  {
    Viewer v = new ComponentViewer() {
    	static final long serialVersionUID=20080207L;
        @Override
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

