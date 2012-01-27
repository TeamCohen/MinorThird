/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
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
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/** A weighted combination of features.
 *
 * @author William Cohen
 */

public class Hyperplane extends BinaryClassifier implements Visible,
		Serializable{

	static private final long serialVersionUID=20080128L;

	/** 
	 * Weight for an invisible 'bias feature' which is considered to be
	 * present in every instance.  In otherwords, as a classifier, the
	 * score of a hyperplane h on an instance x is sum_{feature f}
	 * x.score(f)*h.featureScore(f) + h.featureScore(BIAS_TERM).
	 */
	public static final Feature BIAS_TERM=new Feature("_hyperplaneBias");

	transient protected TObjectDoubleHashMap hyperplaneWeights=
			new TObjectDoubleHashMap();

	transient private boolean ignoreWeights=false;

	// mystic incantations to implement a non-default serialization strategy
	private void writeObject(ObjectOutputStream out) throws IOException{
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			double w=featureScore(f);
			if(w!=0){
				out.writeObject(f);
				out.writeDouble(w);
				//System.out.println("wrote weight: "+f+" => "+w);
			}
		}
		out.writeObject(BIAS_TERM);
		out.writeDouble(0.0);
		out.writeBoolean(ignoreWeights);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException{
		try{
			//System.out.println("reading new object");
			double w=1;
			hyperplaneWeights=new TObjectDoubleHashMap();
			while(w!=0){
				Feature f=(Feature)in.readObject();
				w=in.readDouble();
				if(w!=0)
					hyperplaneWeights.put(f,w);
				//System.out.println("read weight: "+f+" => "+w);
			}
			ignoreWeights=in.readBoolean();
		}catch(StreamCorruptedException ex){
			//System.out.println("reading old object");
			in.defaultReadObject();
		}
	}

	public Hyperplane(){
		;
	}

	/** After this call is made, the hyperplane will assume that all
	 * feature weights are one in instances.  Specifically in calls to
	 * increment(instance,delta) or increment(hyperplane,delta), feature
	 * weights will be assumed to be one.  For backward compatibility
	 * with an old buggy version.
	 */
	public void startIgnoringWeights(){
		this.ignoreWeights=true;
	}

	/** Inner product of hyperplane and instance weights. */
	@Override
	public double score(Instance instance){
		double score=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			score+=instance.getWeight(f)*featureScore(f);
		}
		score+=featureScore(BIAS_TERM);
		return score;
	}

	/** Justify inner product of hyperplane and instance weights. */
	@Override
	public String explain(Instance instance){
		StringBuffer buf=new StringBuffer("");
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			if(buf.length()>0)
				buf.append("\n + ");
			else
				buf.append("   ");
			buf.append(f+"<"+instance.getWeight(f)+"*"+featureScore(f)+">");
		}
		buf.append("\n + bias<"+featureScore(BIAS_TERM)+">");
		buf.append("\n = "+score(instance));
		return buf.toString();
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=new Explanation.Node("Hyperplane Explanation");
		Explanation.Node tokens=new Explanation.Node("Tokens:");
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			Explanation.Node ftr=
					new Explanation.Node(f+"<"+instance.getWeight(f)+"*"+featureScore(f)+
							">");
			tokens.add(ftr);
		}
		Explanation.Node bias=
				new Explanation.Node("bias<"+featureScore(BIAS_TERM)+">");
		tokens.add(bias);
		top.add(tokens);
		Explanation.Node score=new Explanation.Node("\n = "+score(instance));
		top.add(score);
		Explanation ex=new Explanation(top);
		return ex;
	}

	/** Increment one feature from the hyperplane by delta */
	public void increment(Feature f,double delta){
		double d=hyperplaneWeights.get(f);
		hyperplaneWeights.put(f,d+delta);
	}

	/** Increment the bias term for the hyperplane by delta */
	public void incrementBias(double delta){
		increment(BIAS_TERM,delta);
	}

	/** Set the bias term for the hyperplane to delta */
	public void setBias(double delta){
		hyperplaneWeights.remove(BIAS_TERM);
		hyperplaneWeights.put(BIAS_TERM,delta);
	}

	/** Add the value of the features in the instance to this hyperplane. */
	public void increment(Instance instance,double delta){
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
			Feature f=i.next();
			double w=ignoreWeights?1:instance.getWeight(f);
			increment(f,w*delta);
		}
		incrementBias(delta);
	}

	/** Multiply all weights by a factor */
	public void multiply(double factor){
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			hyperplaneWeights.put(f,featureScore(f)*factor);
		}
	}

	/** Multiply one feature from the hyperplane by delta */
	public void multiply(Feature f,double delta){
		double d=hyperplaneWeights.get(f);
		hyperplaneWeights.put(f,d*delta);
	}

	/**Checks the presence of a feature in hyperplane */
	public boolean hasFeature(Feature feat){
		return hyperplaneWeights.containsKey(feat);
	}

	/** Add hyperplane b*delta to this hyperplane. */
	public void increment(Hyperplane b,double delta){
		for(TObjectDoubleIterator i=b.hyperplaneWeights.iterator();i.hasNext();){
			i.advance();
			Feature f=(Feature)i.key();
			double w=b.featureScore(f);
			increment(f,w*delta);
		}
	}

	/** Add hyperplane b to this hyperplane. */
	public void increment(Hyperplane b){
		increment(b,1.0);
	}

	/** Weight for a feature in the hyperplane. */
	public double featureScore(Feature feature){
		return hyperplaneWeights.get(feature);
	}

	/** Iterate over all features with non-zero weight. */
	public Iterator<Feature> featureIterator(){
		final TObjectDoubleIterator ti=hyperplaneWeights.iterator();
		Iterator<Feature> i=new Iterator<Feature>(){

			@Override
			public boolean hasNext(){
				return ti.hasNext();
			}

			@Override
			public Feature next(){
				ti.advance();
				return (Feature)ti.key();
			}

			@Override
			public void remove(){
				ti.remove();
			}
		};
		return i;
	}

	//
	// UI stuff
	//

	@Override
	public Viewer toGUI(){
		Viewer gui=new ControlledViewer(new MyViewer(),new HyperplaneControls());
		gui.setContent(this);
		return gui;
	}

	static private class HyperplaneControls extends ViewerControls{

		static final long serialVersionUID=20080128L;
		
		// how to sort
		//private JRadioButton absoluteValueButton;
		private JRadioButton valueButton;
		private JRadioButton nameButton;
		private JRadioButton treeButton;
		//private JRadioButton noneButton;

		@Override
		public void initialize(){
			ButtonGroup group=new ButtonGroup();
			;
			treeButton=addButton("Tree view",group,false);
			add(new JLabel("   OR  "));
			add(new JLabel("   Sort by"));
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
		
		static final long serialVersionUID=20080128L;

		private HyperplaneControls controls=null;

		private Hyperplane h=null;

		//private Vector features = new Vector();

		@Override
		public void applyControls(ViewerControls controls){
			this.controls=(HyperplaneControls)controls;
			setContent(h,true);
			revalidate();
		}

		@Override
		public boolean canReceive(Object o){
			return o instanceof Hyperplane;
		}

		public void createNodes(DefaultMutableTreeNode top,Object[][] data,
				int start,int len,int level){
			//DefaultMutableTreeNode node = top;
			int size=((Feature)data[start][0]).size();
			//String whole=((Feature)data[start][0]).toString();
			double max=((Double)data[start][1]).doubleValue();

			if(level<=size){
				String tok=((Feature)data[start][0]).getPart(level);

				int i=start+1;
				String nextTok=((Feature)data[i][0]).getPart(level);
				double weight=((Double)data[i][1]).doubleValue();
				int flag=0;
				while(nextTok.equals(tok)&&i<len-1){
					if(weight>max)
						max=weight;
					i++;
					nextTok=((Feature)data[i][0]).getPart(level);
					weight=((Double)data[i][1]).doubleValue();
					flag=1;
				}

				DefaultMutableTreeNode node=
						new DefaultMutableTreeNode(tok+" ("+max+")");
				top.add(node);
				if(flag==1){
					createNodes(node,data,start,i,level+1);
				}
				if(i<len-1){
					createNodes(top,data,i,len,level);
				}
			}
		}

		public JTree createTree(Object[][] data,int len){
			JTree tree;
			DefaultMutableTreeNode top=new DefaultMutableTreeNode("Features Tree");
			createNodes(top,data,0,len,0);
			tree=new JTree(top);

			return tree;
		}

		@Override
		public JComponent componentFor(Object o){
			h=(Hyperplane)o;
			Object[] keys=h.hyperplaneWeights.keys();
			Object[][] tableData=new Object[keys.length][2];
			int k=0;
			for(Iterator<Feature> i=h.featureIterator();i.hasNext();){
				Feature f=i.next();
				tableData[k][0]=f;
				tableData[k][1]=new Double(h.featureScore(f));
				k++;
			}
			if(controls!=null){
				Arrays.sort(tableData,new Comparator<Object[]>(){

					@Override
					public int compare(Object[] ra,Object[] rb){
						if(controls.nameButton.isSelected()||
								controls.treeButton.isSelected())
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

				JTree tree;
				if(controls.treeButton.isSelected()){
					tree=createTree(tableData,keys.length);
					return new JScrollPane(tree);
				}
			}
			String[] columnNames={"Feature Name","Weight"};
			JTable table=new JTable(tableData,columnNames);
			monitorSelections(table,0);
			return new JScrollPane(table);
		}
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("[Hyperplane:");
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append(" "+f+"="+featureScore(f));
		}
		buf.append("]");
		return buf.toString();
	}
}
