package edu.cmu.minorthird.classify.algorithms.linear;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.Visible;

/** A generative Model for word-counts based on the Poisson Distribution.
 *
 * @author Edoardo Airoldi
 */

class PoissonClassifier extends BinaryClassifier implements Visible,
		Serializable{

	static final long serialVersionUID=20080130L;
	
//	static private Logger log=Logger.getLogger(PoissonClassifier.class);

	private static final boolean LOG=true;

	private Hyperplane linear; // BIAS is the bias of linear

	private Hyperplane loglinear; // SCALE is the bias of loglinear

	public PoissonClassifier(){
		this.linear=new Hyperplane();
		this.loglinear=new Hyperplane();
	}

	/** Inner product of PoissonClassifier and instance weights. */
	@Override
	public double score(Instance instance){
		double score=0.0;
		double scoreLog=0.0;
		double total=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			total+=instance.getWeight(f);
			score+=featureScore(f);
			scoreLog+=instance.getWeight(f)*featureScore(f,LOG);
		}
		score=
				score*total/featureScore(Hyperplane.BIAS_TERM,LOG)+scoreLog+
						featureScore(Hyperplane.BIAS_TERM);
		// = sum_f { -w * [ +mu(+) -mu(-) ] + f_counts * [ log mu(+) - log mu(-) ] } + log Pr(+) - log Pr(-)
		return score;
	}

	/** Justify inner product of PoissonClassifier and instance weights. */
	@Override
	public String explain(Instance instance){
		StringBuffer buf=new StringBuffer("");
		double total=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			total+=instance.getWeight(f);
		}
		total=total/featureScore(Hyperplane.BIAS_TERM,LOG);

		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			if(buf.length()>0)
				buf.append(" + ");
			buf.append(f+"<"+instance.getWeight(f)+"*"+featureScore(f,LOG)+"+"+total+
					"*"+featureScore(f)+">");
		}
		buf.append(" + bias<"+featureScore(Hyperplane.BIAS_TERM)+">");
		buf.append(" = "+score(instance));
		return buf.toString();

	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=new Explanation.Node("PoisionClassifier Explanation");

		double total=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			total+=instance.getWeight(f);
		}
		total=total/featureScore(Hyperplane.BIAS_TERM,LOG);

		Explanation.Node features=new Explanation.Node("Features");
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			Explanation.Node fEx=
					new Explanation.Node(f+"<"+instance.getWeight(f)+"*"+
							featureScore(f,LOG)+"+"+total+"*"+featureScore(f)+">");
			features.add(fEx);
		}
		Explanation.Node bias=
				new Explanation.Node(" + bias<"+featureScore(Hyperplane.BIAS_TERM)+">");
		features.add(bias);
		top.add(features);
		Explanation.Node score=new Explanation.Node(" = "+score(instance));
		top.add(score);

		Explanation ex=new Explanation(top);
		return ex;
	}

	/** Increment the weight of one feature from the PoissonClassifier by delta */
	public void increment(Feature f,double delta){
		linear.increment(f,delta);
	}

	/** Increment the log-weight of one feature from the PoissonClassifier by delta */
	public void increment(Feature f,double delta,boolean log){
		loglinear.increment(f,delta);
	}

	/** Increment the bias term for the PoissonClassifier by delta */
	public void incrementBias(double delta){
		linear.incrementBias(delta);
	}

	/** Set the scale term for the PoissonClassifier to delta */
	public void setScale(double delta){
		loglinear.setBias(delta);
	}

	/** Weight for a feature in the PoissonClassifier. */
	public double getScale(){
		return featureScore(Hyperplane.BIAS_TERM,LOG);
	}

	/** Add the value of the features in the instance to this PoissonClassifier */
	public void increment(Instance instance,double delta,double log_delta){
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
			Feature f=i.next();
			increment(f,delta);
			increment(f,log_delta,LOG);
		}
		incrementBias(delta);
	}

	/** Add PoissonClassifier b*delta to this PoissonClassifier.
	 public void increment(PoissonClassifier b, double delta, double log_delta) {
	 for (Iterator i=b.hyperplaneWeights.keySet().iterator(); i.hasNext(); ) {
	 Feature f = (Feature)i.next();
	 double w = b.featureScore( f );
	 increment( f, w * delta );
	 }
	 for (Iterator i=b.hyperplaneLogWeights.keySet().iterator(); i.hasNext(); ) {
	 Feature f = (Feature)i.next();
	 double log_w = b.featureScore( f, LOG );
	 increment( f, log_w * log_delta, LOG );
	 }
	 incrementBias( b.featureScore(BIAS_TERM) * delta );
	 }*/

	/** Weight for a feature in the PoissonClassifier. */
	public double featureScore(Feature feature){
		return linear.featureScore(feature);
	}

	/** log-Weight for a feature in the PoissonClassifier. */
	public double featureScore(Feature feature,boolean log){
		return loglinear.featureScore(feature);
	}

	@Override
	public Viewer toGUI(){
		Viewer gui=new ControlledViewer(new MyViewer(),new PoissonControls());
		gui.setContent(this);
		return gui;
	}

	static private class PoissonControls extends ViewerControls{

		static final long serialVersionUID=20080130L;
		
		// how to sort
		//private JRadioButton absoluteValueButton;
		private JRadioButton valueButton;
		private JRadioButton nameButton;
		//private JRadioButton noneButton;

		@Override
		public void initialize(){
			add(new JLabel("Sort by"));
			ButtonGroup group=new ButtonGroup();
			;
			nameButton=addButton("name",group,true);
			valueButton=addButton("weight",group,false);
			//absoluteValueButton=addButton("|weight|",group,false);
		}

		private JRadioButton addButton(String s,ButtonGroup group,boolean selected){
			JRadioButton button=new JRadioButton(s,selected);
			group.add(button);
			add(button);
			button.addActionListener(this);
			return button;
		}
	}

	static private class MyViewer extends ComponentViewer implements Controllable{

		static final long serialVersionUID=20080130L;
		
		private PoissonControls controls=null;

		private PoissonClassifier h=null;

		@Override
		public void applyControls(ViewerControls controls){
			this.controls=(PoissonControls)controls;
			setContent(h,true);
			revalidate();
		}

		@Override
		public boolean canReceive(Object o){
			return o instanceof Hyperplane;
		}

		@Override
		public JComponent componentFor(Object o){
			h=(PoissonClassifier)o;
			//
			// Note: in this way only _hyperplaneBias of linear gets displayed.
			//       That one of loglinear does NOT.
			//
			Object[] keys=h.linear.hyperplaneWeights.keys();
			Object[][] tableData=new Object[keys.length][2];
			int k=0;
			for(Iterator<Feature> i=h.linear.featureIterator();i.hasNext();){
				Feature f=i.next();
				tableData[k][0]=f;
				tableData[k][1]=new Double(h.featureScore(f));
				k++;
			}
			if(controls!=null){
				Arrays.sort(tableData,new Comparator<Object[]>(){

					@Override
					public int compare(Object[] ra,Object[] rb){
						if(controls.nameButton.isSelected())
							return ra[0].toString().compareTo(rb[0].toString());
						Double da=(Double)ra[1];
						Double db=(Double)rb[1];
						if(controls.valueButton.isSelected())
							return MathUtil.sign(db.doubleValue()-da.doubleValue());
						else
							return MathUtil.sign(Math.abs(db.doubleValue())-
									Math.abs(da.doubleValue()));
					}
				});
			}
			String[] columnNames={"Feature Name","Weight"};
			JTable table=new JTable(tableData,columnNames);
			monitorSelections(table,0);
			return new JScrollPane(table);
		}
	}

	@Override
	public String toString(){
		String a=linear.toString();
		String b=loglinear.toString();
		return(a+"\n"+b);
	}
}
