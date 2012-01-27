/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.semisupervised;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.WeightedSet;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.Visible;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

/**
 * @author Edoardo Airoldi
 * Date: Mar 15, 2004
 */

public class MultinomialClassifier implements SemiSupervisedClassifier,
		Classifier,Visible,Serializable{
	
	static final long serialVersionUID=20080207L;

	static Logger log=Logger.getLogger(MultinomialClassifier.class);

	private double SCALE; // set by learner if needed

	private List<String> classNames;

	private List<Double> classParameters;

	private Map<Feature,String> featureModels;

	private List<WeightedSet<Feature>> featureGivenClassParameters;

	private double featurePrior;

	private String unseenModel;

	// constructor
	public MultinomialClassifier(){
		this.classNames=new ArrayList<String>();
		this.classParameters=new ArrayList<Double>();
		this.featureModels=new HashMap<Feature,String>();
		this.featureGivenClassParameters=new ArrayList<WeightedSet<Feature>>();
		this.featureGivenClassParameters.add(new WeightedSet<Feature>());
		this.featurePrior=0.0;
		this.unseenModel=null;
	}

	//
	// methods in Classifier interface
	//
	@Override
	public ClassLabel classification(Instance instance){
		double[] score=score(instance); // implement smoothing for *unseen* features
		//System.out.println("size="+score.length);
		int maxIndex=0;
		for(int i=0;i<score.length;i++){
			//System.out.println("i="+i+" score="+score[i]);
			if(score[i]>score[maxIndex]){
				maxIndex=i;
			}
		}
		//System.out.println( classNames.get(0)+","+score[0]+" "+classNames.get(1)+","+score[1]);
		return new ClassLabel(classNames.get(maxIndex));
	}

	public double[] score(Instance instance){
		//System.out.println(instance);
		//System.out.println( "class="+classNames.get(0)+" counts="+featureGivenClassParameters.get(0) );
		//System.out.println( "class="+classNames.get(1)+" counts="+featureGivenClassParameters.get(1) );
		double total=0.0;
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			total+=instance.getWeight(f);
		}

		double[] score=new double[classNames.size()];
		for(int i=0;i<classNames.size();i++){
			score[i]=0.0;
			//System.out.println("instance="+instance);
			for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
				Feature f=j.next();
				double featureCounts=instance.getWeight(f);
				double featureProb=
						featureGivenClassParameters.get(i).getWeight(f);
				//System.out.println("feature="+f+" counts="+featureCounts+" prob="+featureProb+" class="+classProb);
				String model=getFeatureModel(f);
				//System.out.println("feature="+f+" model="+model);
				if(model.equals("Poisson")){
					score[i]+=
							-featureProb*total/SCALE+featureCounts*Math.log(featureProb);
				}else if(model.equals("Binomial")){
					score[i]+=featureCounts*Math.log(featureProb);
				}else if(model.equals("unseen")){
					score[i]+=0.0;
				}else{
					System.out.println("error: model "+model+" not found!");
					System.exit(1);
				}
			}
			double classProb=(classParameters.get(i)).doubleValue();
			score[i]+=Math.log(classProb);
		}
		return score;
	}

	@Override
	public String explain(Instance instance){
		StringBuffer buf=new StringBuffer("");
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
//			Feature f=j.next();
			if(buf.length()>0)
				buf.append("\n + ");
			else
				buf.append("   ");
			//buf.append( f+"<"+instance.getWeight(f)+"*"+featureScore(f)+">");
		}
		//buf.append( "\n + bias<"+featureScore( BIAS_TERM )+">" );
		buf.append("\n = "+score(instance));
		return buf.toString();
	}

	@Override
	public Explanation getExplanation(Instance instance){
		Explanation.Node top=
				new Explanation.Node("MultinomialClassifier Explanation");
		Explanation.Node features=new Explanation.Node("Features");
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			Explanation.Node featureEx=
					new Explanation.Node(f+"<"+instance.getWeight(f));
			features.add(featureEx);
		}
		Explanation.Node bias=new Explanation.Node("bias");
		features.add(bias);
		top.add(features);
		Explanation.Node score=new Explanation.Node("\n = "+score(instance));
		top.add(score);
		Explanation ex=new Explanation(top);
		return ex;
	}

	//
	// Get, Set, Check
	//
	public void setScale(double value){
		this.SCALE=value;
	}
	
	public double getPrior(){
		return featurePrior;
	}

	public void setPrior(double pi){
		this.featurePrior=pi;
	}
	
	public String getUnseenModel(){
		return unseenModel;
	}

	public void setUnseenModel(String str){
		this.unseenModel=str;
	}

	public double getLogLikelihood(Example example){
		//System.out.println( example );
		int idx=-1;
		for(int i=0;i<classNames.size();i++){
			if(classNames.get(i).equals(example.getLabel().bestClassName())){
				idx=i;
				break;
			}
		}
		//System.out.println( "class="+classNames.get(idx) );
		Instance instance=example.asInstance();
		double loglik=0.0;
		//System.out.println("instance="+instance);
		for(Iterator<Feature> j=instance.featureIterator();j.hasNext();){
			Feature f=j.next();
			double featureCounts=instance.getWeight(f);
			double featureProb=featureGivenClassParameters.get(idx).getWeight(f);
//			double classProb=((Double)classParameters.get(idx)).doubleValue();
			//System.out.println("feature="+f+" counts="+featureCounts+" prob="+featureProb+" class="+classProb);
			String model=getFeatureModel(f);
			if(model.equals("Poisson")){
				loglik+=-featureProb+featureCounts*Math.log(featureProb);
			}else if(model.equals("Binomial")){
				loglik+=featureCounts*Math.log(featureProb);
			}else if(model.equals("unseen")){
				System.out.println("unseen: "+f);
			}else{
				System.out.println("error: model "+model+" not found!");
				System.exit(1);
			}
		}
		return loglik;
	}

	public void reset(){
		this.classParameters=new ArrayList<Double>();
		this.featureGivenClassParameters=new ArrayList<WeightedSet<Feature>>();
		//this.featureGivenClassParameters.add( new WeightedSet() );
	}

	public boolean isPresent(ClassLabel label){
		boolean isPresent=false;
		for(int i=0;i<classNames.size();i++){
			if(classNames.get(i).equals(label.bestClassName())){
				isPresent=true;
			}
		}
		return isPresent;
	}

	public void addValidLabel(ClassLabel label){
		classNames.add(label.bestClassName());
	}

	public ClassLabel getLabel(int i){
		return new ClassLabel(classNames.get(i));
	}

	public int indexOf(ClassLabel label){
		return classNames.indexOf(label.bestClassName());
	}

	public void setFeatureGivenClassParameter(Feature f,int j,
			double probabilityOfOccurrence){
		WeightedSet<Feature> wset;
		try{
			wset=featureGivenClassParameters.get(j);
			wset.add(f,probabilityOfOccurrence);
			featureGivenClassParameters.set(j,wset);
		}catch(Exception t){
			wset=null;
			wset=new WeightedSet<Feature>();
			wset.add(f,probabilityOfOccurrence);
			featureGivenClassParameters.add(j,wset);
		}
	}

	public void setClassParameter(int j,double probabilityOfOccurrence){
		classParameters.add(j,new Double(probabilityOfOccurrence));
	}

	public void setFeatureModel(Feature feature,String model){
		featureModels.put(feature,model);
	}

	public String getFeatureModel(Feature feature){
		try{
			String model=featureModels.get(feature).toString();
			return model;
		}catch(NullPointerException x){
			return "unseen";
		}
	}

	public Iterator<Feature> featureIterator(){
		// 1. create a new WeightedSet with all features
		TObjectDoubleHashMap map=new TObjectDoubleHashMap();
		for(int i=0;i<classNames.size();i++){
			WeightedSet<Feature> wset=featureGivenClassParameters.get(i);
			for(Iterator<Feature> j=wset.iterator();j.hasNext();){
				Feature f=j.next();
				double w=wset.getWeight(f);
				map.put(f,w);
			}
		}
		// 2. create global feature iterator
		final TObjectDoubleIterator ti=map.iterator();
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

	public Object[] keys(){
		TObjectDoubleHashMap map=new TObjectDoubleHashMap();
		for(int i=0;i<classNames.size();i++){
			WeightedSet<Feature> wset=featureGivenClassParameters.get(i);
			for(Iterator<Feature> j=wset.iterator();j.hasNext();){
				Feature f=j.next();
				double w=wset.getWeight(f);
				map.put(f,w);
			}
		}
		return map.keys();
	}

	//
	// GUI related stuff
	//
	@Override
	public Viewer toGUI(){
		Viewer gui=
				new ControlledViewer(new MyViewer(),new MultinomialClassifierControls());
		gui.setContent(this);
		return gui;
	}

	static private class MultinomialClassifierControls extends ViewerControls{

		static final long serialVersionUID=20080207L;
		
		// how to sort
//		private JRadioButton absoluteValueButton;
		private JRadioButton valueButton;
		private JRadioButton nameButton;
//		private JRadioButton noneButton;

		@Override
		public void initialize(){
			add(new JLabel("Sort by"));
			ButtonGroup group=new ButtonGroup();
			;
			nameButton=addButton("name",group,true);
			valueButton=addButton("weight",group,false);
//			absoluteValueButton=addButton("|weight|",group,false);
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
		
		static final long serialVersionUID=20080207L;

		private MultinomialClassifierControls controls=null;

		private MultinomialClassifier h=null;

		@Override
		public void applyControls(ViewerControls controls){
			this.controls=(MultinomialClassifierControls)controls;
			setContent(h,true);
			revalidate();
		}

		@Override
		public boolean canReceive(Object o){
			return o instanceof MultinomialClassifier;
		}

		@Override
		public JComponent componentFor(Object o){
			h=(MultinomialClassifier)o;
			Object[] keys=h.keys();
			Object[][] tableData=new Object[keys.length][(h.classNames.size()+1)];
			int k=0;
			for(Iterator<Feature> i=h.featureIterator();i.hasNext();){
				Feature f=i.next();
				tableData[k][0]=f;
				for(int l=0;l<h.classNames.size();l++){
					tableData[k][(l+1)]=
							new Double(h.featureGivenClassParameters.get(l).getWeight(f));
					;
				}
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
			String[] columnNames=new String[(h.classNames.size()+1)];
			columnNames[0]="Feature Name";
			for(int i=0;i<h.classNames.size();i++){
				columnNames[(i+1)]="Wgt "+h.classNames.get(i);
			}
			JTable table=new JTable(tableData,columnNames);
			monitorSelections(table,0);
			return new JScrollPane(table);
		}
	}

	@Override
	public String toString(){
		return null;
	}

}
