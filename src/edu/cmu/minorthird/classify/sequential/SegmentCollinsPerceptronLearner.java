/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.sequential.Segmentation.Segment;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 *
 * @author William Cohen
 */

public class SegmentCollinsPerceptronLearner implements BatchSegmenterLearner,SequenceConstants
{
	private static Logger log = Logger.getLogger(SegmentCollinsPerceptronLearner.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	private int numberOfEpochs;
    private boolean updatedViterbi = false;
	public SegmentCollinsPerceptronLearner(int epochs) { this.numberOfEpochs = epochs; }
	public SegmentCollinsPerceptronLearner(int epochs, boolean updatedViterbi) { this(epochs); this.updatedViterbi=updatedViterbi;}
	public SegmentCollinsPerceptronLearner() { this.numberOfEpochs = 5; }

	@Override
	public void setSchema(ExampleSchema schema)	{	;	}

	//
	// accessors
	//
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	public int getHistorySize() { return 1; }

	//
	// training scheme
	//

	@Override
	public Segmenter batchTrain(SegmentDataset dataset)
	{
		int maxSegmentSize = dataset.getMaxWindowSize();
		ExampleSchema schema = dataset.getSchema();
		if (DEBUG) log.debug("schema: "+schema);
		CollinsPerceptronLearner.MultiClassVPClassifier c = new CollinsPerceptronLearner.MultiClassVPClassifier(schema);

		//if (DEBUG) log.debug("dataset:\n"+dataset);

		ProgressCounter pc =
			new ProgressCounter("training semi-markov voted-perceptron",
													"sequence",numberOfEpochs*dataset.getNumberOfSegmentGroups());
		if (updatedViterbi)
		    c.setVoteMode(true);    
		for (int epoch=0; epoch<numberOfEpochs; epoch++) 
		{
			// shuffling seems to lower performance by a lot - why?
			//dataset.shuffle();

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (Iterator<CandidateSegmentGroup> i=dataset.candidateSegmentGroupIterator(); i.hasNext(); ) 
			{
				CandidateSegmentGroup g = i.next();
				if (DEBUG) log.debug("classifier is: "+c);
				Segmentation viterbi = new ViterbiSearcher(c,schema,maxSegmentSize).bestSegments(g);
				if (DEBUG) log.debug("viterbi:\n"+viterbi);
				Segmentation correct = correctSegments(g,schema,maxSegmentSize);
				if (DEBUG) log.debug("correct segments:\n"+correct);

				boolean errorOnThisSequence = false;
//				Segmentation.Segment previousViterbiSeg = null;
				int fp = compareSegmentsAndRevise(c, schema, viterbi, correct, -1.0, g);
				if (fp>0) errorOnThisSequence = true;
				int fn = compareSegmentsAndRevise(c, schema, correct, viterbi, +1.0, g);
				if (fn>0) errorOnThisSequence = true;
				if (errorOnThisSequence) sequenceErrors++;
				
				transitionErrors += fp + fn;
				transitions += correct.size();

				c.completeUpdate(); 

				pc.progress();

			} // sequence i

			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
												 +" transitionErrors="+transitionErrors+"/"+transitions);

			if (transitionErrors==0) break;

		} // epoch
		pc.finished();
			
		c.setVoteMode(true);

		// construct the classifier
		return new ViterbiSegmenter(c,schema,maxSegmentSize);
	}

	/** Compare the target segments to the 'otherSegments', and update
	 * the classifier by sum_x [delta*x], for each example x
	 * corresponding to a target segment that's not in otherSegments.
	 */
	private int compareSegmentsAndRevise(
		CollinsPerceptronLearner.MultiClassVPClassifier classifier,ExampleSchema schema, 
		Segmentation segments,Segmentation otherSegments,double delta,CandidateSegmentGroup g)
	{
		int errors = 0;
		// first, work out the name of the previous class for each segment
		Map<Segment,String> map = previousClassMap(segments,schema);
		Map<Segment,String> otherMap = previousClassMap(otherSegments,schema);
		String[] history = new String[1];
		for (Iterator<Segment> j=segments.iterator(); j.hasNext(); ) {
			Segmentation.Segment seg = j.next();
			String previousClass =  map.get(seg);
			if (seg.lo>=0 && (!otherSegments.contains(seg) || !otherMap.get(seg).equals(previousClass))) {
				errors++;
				history[0] = previousClass;
				Instance instance = new InstanceFromSequence( g.getSubsequenceExample(seg.lo,seg.hi), history);
				if (DEBUG) log.debug("update "+delta+" for: "+instance.getSource());
				classifier.update( schema.getClassName( seg.y ), instance, delta ); 
			}
		}
		return errors;
	}

	/** Build a mapping from segment to string name of previous segment.
	 * This should let you look up segments which are logically
	 * equivalent, as well as ones which are pointer-equivalent (==)
	 */
	private Map<Segment,String> previousClassMap(Segmentation segments,ExampleSchema schema)
	{
		// use a treemap so that logically equivalent segments be mapped to same previousClass
		Map<Segment,String> map = new TreeMap<Segment,String>(); 
		Segmentation.Segment previousSeg = null;
		for (Iterator<Segment> j=segments.iterator(); j.hasNext(); ) {
			Segmentation.Segment seg = j.next();
			String previousClassName = previousSeg==null ? NULL_CLASS_NAME : schema.getClassName(previousSeg.y);
			map.put( seg, previousClassName);
			previousSeg = seg;
		}
		return map;
	}

	/** Collect the correct segments for this example.  These are defined as 
	 * all segments with non-NEGATIVE labels, and all unit-length negative labels
	 * not inside a positives label.
	 */
	private Segmentation correctSegments(CandidateSegmentGroup g,ExampleSchema schema,int maxSegmentSize)
	{
		Segmentation result = new Segmentation(schema);
		int pos, len;
		for (pos=0; pos<g.getSequenceLength(); ) {
			boolean addedASegmentStartingAtPos = false;
			for (len=1; !addedASegmentStartingAtPos && len<=maxSegmentSize; len++) {
				Instance inst = g.getSubsequenceInstance(pos,pos+len);
				ClassLabel label = g.getSubsequenceLabel(pos,pos+len);
				if (inst!=null && !label.isNegative()) {
					result.add( new Segmentation.Segment(pos,pos+len,schema.getClassIndex(label.bestClassName())) );
					addedASegmentStartingAtPos = true;
					pos += len;
				}
			}
			if (!addedASegmentStartingAtPos) {
//				Instance inst = g.getSubsequenceInstance(pos,pos+1);
//				ClassLabel label = g.getSubsequenceLabel(pos,pos+1);
				result.add( new Segmentation.Segment(pos,pos+1,schema.getClassIndex(ExampleSchema.NEG_CLASS_NAME)) );
				pos += 1;
			}
		}
		return result;
	}

	static public class ViterbiSearcher
	{
		private Classifier classifier;
		private ExampleSchema schema;
		private int maxSegmentSize;
		public ViterbiSearcher(Classifier classifier,ExampleSchema schema,int maxSegmentSize)
		{
			this.classifier = classifier;
			this.schema = schema;
			this.maxSegmentSize = maxSegmentSize;
		}
		public Segmentation bestSegments(CandidateSegmentGroup g) 
		{
			// for t=0..size, y=0 or 1, fty[t][y] is the highest score that
			// can be obtained with a segmentation of the tokens from 0..t
			// that ends with class y (where y=1 means "from dictionary", y=0
			// means "from null model")

			// initialize
			String[] history = new String[1];
			
			int seqLen = g.getSequenceLength();
			int ny = schema.getNumberOfClasses();
			int backgroundClass = schema.getClassIndex( ExampleSchema.NEG_CLASS_NAME );
			double[][] fty = new double[seqLen+1][ny];
			BackPointer[][] trace = new BackPointer[seqLen+1][ny];
			for (int t=0; t<seqLen+1; t++) {
				for (int y=0; y<ny; y++) {
					fty[t][y] = -99999; //could be -Double.MAX_VALUE;
					trace[t][y] = null;
				}
			}
			for (int y=0; y<ny; y++) fty[0][y] = 0;
		
			// fill the matrix fty[t][y] = score of maximal segmentation
			// from 0..t that ends in y
			for (int t=0; t<seqLen+1; t++) {
				for (int y=0; y<ny; y++) {
					for (int lastY=0; lastY<ny; lastY++) {
						int maxSegSizeForY = y==backgroundClass ? 1 : maxSegmentSize;
						for (int lastT=Math.max(0, t-maxSegSizeForY); lastT<t; lastT++) {
							// find the classifier's score for the subsequence from lastT to t
							// with label y and previous label lastY
							Instance segmentInstance = g.getSubsequenceInstance(lastT, t);
							if (segmentInstance!=null) {
								history[0] = schema.getClassName( lastY );
								InstanceFromSequence seqSegmentInstance = new InstanceFromSequence(segmentInstance,history);
								double segmentScore = classifier.classification(seqSegmentInstance).getWeight( schema.getClassName(y) );
								// store the max score (over all lastT,lastY) in fty
								if (segmentScore + fty[lastT][lastY] > fty[t][y]) {
									fty[t][y] = segmentScore + fty[lastT][lastY];
									trace[t][y] = new BackPointer(lastT,t,lastY);
								}
							}
						}
					}
				}
			}
		
			// use the back pointers to find the best segmentation that ends at t==documentSize
			int bestEndY = -1;
			double bestEndYScore = -Double.MAX_VALUE;
			for (int y=0; y<ny; y++) {
				if (fty[seqLen][y] > bestEndYScore) {
					bestEndYScore = fty[seqLen][y];
					bestEndY = y;
				}
			}
			Segmentation result = new Segmentation(schema);
			int y = bestEndY;
			for (BackPointer bp = trace[seqLen][y]; bp!=null; bp=trace[bp.lastT][bp.lastY]) {
				bp.onBestPath = true;
				result.add( new Segmentation.Segment(bp.lastT,bp.t,y) );
				y = bp.lastY;
			}
			if (DEBUG) dumpStuff(g,fty,trace);
			return result;
		}
	}
	private static class BackPointer {
		public int lastT, t,lastY;
		public boolean onBestPath;
		public BackPointer(int lastT, int t,int lastY) {
			this.lastT=lastT;
			this.t=t;
			this.lastY=lastY;
			this.onBestPath=false;
		}
	}

	private static void dumpStuff(CandidateSegmentGroup g, double[][] fty, BackPointer[][] trace)
	{
		Example nullExample = new Example(new MutableInstance("*NULL*"),new ClassLabel("*NULL*"));
		java.text.DecimalFormat format = new java.text.DecimalFormat("####.###");
		System.out.println("t.y\tf(t,y)\tt'.y'\tspan");
		for (int t=0; t<fty.length; t++) {
			for (int y=0; y<fty[t].length; y++) {
				BackPointer bp = trace[t][y];				
				Example ex = bp==null ? nullExample : g.getSubsequenceExample(bp.lastT,bp.t);
				if (bp==null) bp = new BackPointer(-1,-1,-1);
				String marker = bp.onBestPath? "<==" : "";
				System.out.println(t+"."+y+"\t"+format.format(fty[t][y])+"\t"+
													 bp.lastT+"."+bp.lastY+"\t'"+ex.getSource()+"' "+marker);
			}
		}
	}

	public static class ViterbiSegmenter implements Segmenter,Visible,Serializable
	{
		static private final long serialVersionUID = 20080207L;

		private Classifier c;
		private ExampleSchema schema;
		private int maxSegSize;

		public ViterbiSegmenter(Classifier c,ExampleSchema schema,int maxSegSize) 
		{
			this.c = c;
			this.schema = schema;
			this.maxSegSize = maxSegSize;
		}
		@Override
		public Segmentation segmentation(CandidateSegmentGroup g) 
		{
			return new ViterbiSearcher(c,schema,maxSegSize).bestSegments(g);
		}
		@Override
		public String explain(CandidateSegmentGroup g)
		{
			return "not implemented yet";
		}
		@Override
		public Viewer toGUI()
		{
			Viewer v = new ComponentViewer() {
				static final long serialVersionUID=20080207L;
					@Override
					public JComponent componentFor(Object o) {
						ViterbiSegmenter vs = (ViterbiSegmenter)o;
						JPanel mainPanel = new JPanel();
						mainPanel.setLayout(new BorderLayout());
						mainPanel.add(new JLabel("ViterbiSegmenter: maxSegSize="+vs.maxSegSize),BorderLayout.NORTH);
						Viewer subView = new SmartVanillaViewer(vs.c);
						subView.setSuperView(this);
						mainPanel.add(subView,BorderLayout.SOUTH);
						mainPanel.setBorder(new TitledBorder("ViterbiSegmenter"));
						return new JScrollPane(mainPanel);
					}
				};
			v.setContent(this);
			return v;
		}
	}
}

