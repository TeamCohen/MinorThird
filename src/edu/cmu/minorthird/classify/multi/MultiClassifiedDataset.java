/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;

/** Pairs a dataset and a classifier, for easy inspection
 * of the actions of a classifier.
 *
 * @author Cameron Williams
 */

public class MultiClassifiedDataset implements Visible
{
	private MultiClassifier classifier;
	private MultiRandomAccessDataset dataset;
	private MultiDatasetIndex index;

	public MultiClassifiedDataset(MultiClassifier classifier,MultiDataset dataset)
	{
		this(classifier,dataset,new MultiDatasetIndex(dataset));
	}

	public MultiClassifiedDataset(MultiClassifier classifier,MultiDataset dataset,MultiDatasetIndex index)
	{
		this.classifier = classifier;
		if (dataset instanceof MultiRandomAccessDataset) {
			this.dataset = (MultiRandomAccessDataset)dataset;
		} else {
			this.dataset = new MultiRandomAccessDataset();
			for (MultiExample.Looper i=dataset.multiIterator(); i.hasNext(); ) {
				this.dataset.addMulti( i.nextMultiExample() );
			}
		}
		this.index = index;
	}

	public MultiClassifier getClassifier() { return classifier; }

	public MultiDataset getDataset() { return dataset; }

	public Viewer toGUI()
	{
		Viewer v = new MessageViewer(new MyViewer());
		v.setContent(this);
		return v;
	}

	/** A toolbar to govern how data is filtered.
	 */
	private static class DataControls extends ViewerControls
	{
		public JCheckBox filterOnFeatureBox;
		public Feature targetFeature;
		public JRadioButton correctButton, incorrectButton, allButton;

		public void initialize()
		{
			// indicates if we should filter on some feature
			filterOnFeatureBox = new JCheckBox();
			filterOnFeatureBox.setText("[none]");
			targetFeature = null;
			add(filterOnFeatureBox);

			ButtonGroup group = new ButtonGroup();
			correctButton = addButton("correct",false,group);
			incorrectButton = addButton("incorrect",false,group);
			allButton = addButton("all",true,group);

			addApplyButton();
		}
		private JRadioButton addButton(String s,boolean selected,ButtonGroup group)
		{
			JRadioButton button = new JRadioButton(s,selected);
			group.add(button);
			add(button);
			return button; 
		}
	}
	

	/** A toolbar-controlled viewer for data/classifications in a classified dataset
	 */
	static private class ControlledDataViewer extends ComponentViewer implements Controllable
	{
		// cached last display
		private MultiClassifiedDataset cd;

		// If true, only show example for which the classification is
		// correct (or incorrect) depending on targetCorrectness
		private boolean filterOnCorrectness = false;
		private boolean targetCorrectness = false;
		//* If true, only show example which contain the target feature 
		private boolean filterOnFeature = false;
		private Feature targetFeature = null;

		public void applyControls(ViewerControls controls)
		{
			DataControls dc = (DataControls)controls;
			if (dc.allButton.isSelected()) filterOnCorrectness = false;
			else {
				filterOnCorrectness = true;
				targetCorrectness = dc.correctButton.isSelected();
			}
			filterOnFeature = dc.filterOnFeatureBox.isSelected();
			targetFeature = dc.targetFeature;
			// setContent here is incorrect - we want to bypass
			// any caching and force an update
			receiveContent(cd);
			revalidate();
		}
		public JComponent componentFor(Object o)
		{
			cd = (MultiClassifiedDataset)o;
			JTable jtable = new JTable(new MyTableModel(filteredMultiClassifiedDataset()));
			jtable.setDefaultRenderer(MultiExample.class,new TableCellRenderer() {
					public Component getTableCellRendererComponent(
						JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
					{
						return GUI.conciseMultiExampleRendererComponent((MultiExample)value,60,isSelected);
					}
				});
			monitorSelections(jtable,1);
			JScrollPane scrollpane = new JScrollPane(jtable);
			scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			return scrollpane;
		}
		private MultiClassifiedDataset filteredMultiClassifiedDataset()
		{
			if (!filterOnCorrectness && !filterOnFeature) {
				return cd;
			} else {
				MultiRandomAccessDataset filteredData = new MultiRandomAccessDataset();
				ProgressCounter pc = new ProgressCounter("classifying for ClassifiedDataset","example",filteredData.size());
				for (MultiExample.Looper i=cd.dataset.multiIterator(); i.hasNext(); ) {
					MultiExample e = i.nextMultiExample();
					boolean pass1 = true;
					if (filterOnCorrectness) 
						pass1 = targetCorrectness==cd.classifier.multiLabelClassification(e).isCorrect(e.getMultiLabel());
					boolean pass2 = true;
					if (filterOnFeature) 
						pass2 = targetFeature==null || e.getWeight(targetFeature)>0;
					if (pass1 && pass2) {
						filteredData.addMulti(e);
					}
					pc.progress();
				}
				pc.finished();
				return new MultiClassifiedDataset(cd.classifier,filteredData,cd.index);
			}
		}
		/** models the data in the RandomAccessDataset of the ClassifiedDataset */
		private class MyTableModel extends AbstractTableModel 
		{
			private MultiClassifiedDataset cd;
			public MyTableModel(MultiClassifiedDataset cd) { this.cd = cd; }
			public int getRowCount() { return cd.dataset.size(); }
			public int getColumnCount() { return 2; } // predicted, actual, instance
			public Object getValueAt(int row,int col)
			{
				if (col==0) return cd.classifier.multiLabelClassification(cd.dataset.getMultiExample(row));
				else if (col==1) return cd.dataset.getMultiExample(row);
				else throw new IllegalArgumentException("illegal col "+col);
			}
			public String getColumnName(int col)
			{
				if (col==0) return "Prediction";
				else if (col==1) return "MultiExample";
				else throw new IllegalArgumentException("illegal col "+col);
			}
		}
	}

    static public class ExplanationViewer extends ComponentViewer
    {
	Explanation ex;

	public ExplanationViewer(Explanation ex) {
	    this.ex = ex;
	    setContent(ex);
	}
	
	public boolean canReceive(Object o) {	return o instanceof Explanation; }

	public JComponent componentFor(Object o) 
	{
	    ex = (Explanation)o;
	    JScrollPane p = new JScrollPane(ex.getExplanation());
	    return p;
	}

    }

	/** Viewer for a classified dataset
	 */
	static private class MyViewer extends ComponentViewer
	{
		private Viewer instanceViewer,classifierViewer,explanationViewer;
		private ControlledViewer dataViewer;
		private MultiClassifiedDataset cd;

		public JComponent componentFor(Object o)
		{
			cd = (MultiClassifiedDataset)o;
			JSplitPane left = new JSplitPane();
			left.setOrientation(JSplitPane.VERTICAL_SPLIT);
			left.setResizeWeight(0.75);
			dataViewer = new ControlledViewer(new ControlledDataViewer(), new DataControls());
			dataViewer.setContent(cd);
			left.setTopComponent(dataViewer);
			instanceViewer = GUI.newSourcedMultiExampleViewer();
			left.setBottomComponent(instanceViewer);
			dataViewer.setSuperView(this,"data");
			instanceViewer.setSuperView(this,"instance");

			JSplitPane right = new JSplitPane();
			right.setOrientation(JSplitPane.VERTICAL_SPLIT);
			right.setResizeWeight(0.75);
			classifierViewer = 
				(cd.classifier instanceof Visible)	
				?((Visible)cd.classifier).toGUI():new VanillaViewer(cd.classifier);
			right.setTopComponent(classifierViewer);
			explanationViewer = new ExplanationViewer(new Explanation("[explanation]"));
			right.setBottomComponent(explanationViewer);
			classifierViewer.setSuperView(this,"classifier");
			explanationViewer.setSuperView(this,"explanation");

			JSplitPane split = new JSplitPane();
			split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			split.setResizeWeight(0.50);
			split.setLeftComponent(left);
			split.setRightComponent(right);

			MultiEvaluation e = new MultiEvaluation(cd.dataset.getMultiSchema());
			e.extend(cd.classifier, cd.dataset);
			Viewer evalViewer = e.toGUI();

			JTabbedPane main = new JTabbedPane();
			main.add("Details", split);
			main.add("Evaluation", evalViewer);
			evalViewer.setSuperView(this,"evaluation");

			return main;
		}
		protected boolean canHandle(int signal,Object argument,ArrayList senders) 
		{
			return 
				(signal==OBJECT_SELECTED) && (argument instanceof MultiExample)
				|| (signal==OBJECT_SELECTED) && (argument instanceof Feature);
		}
		protected void handle(int signal,Object argument,ArrayList senders) 
		{
			if (argument instanceof MultiExample) {
				MultiExample example = (MultiExample)argument;
				instanceViewer.setContent(example);
				explanationViewer.setContent(cd.classifier.getExplanation(example));
				revalidate();
			} else if (argument instanceof Feature) {
				DataControls dc = (DataControls)dataViewer.getControls();
				dc.targetFeature = (Feature)argument;
				dc.filterOnFeatureBox.setText(argument.toString());
				sendSignal( TEXT_MESSAGE, featureSummary((Feature)argument,cd.index) );
			} 
		}
		private String featureSummary(Feature f,MultiDatasetIndex index)
		{
			StringBuffer buf = new StringBuffer(f.toString());
			buf.append(" appears in ");
			buf.append(index.size(f));
			buf.append(" examples:");
			Map map = new TreeMap();
			for (int i=0; i<index.size(f); i++) {
			    String label = index.getMultiExample(f,i).getMultiLabel().bestClassName().toString();
				Integer count = (Integer)map.get(label);
				if (count==null) map.put(label, (count=new Integer(0)));
				map.put(label, new Integer(count.intValue()+1));
			}
			for (Iterator j=map.keySet().iterator(); j.hasNext(); ) {
				String label = (String)j.next();
				Integer count = (Integer)map.get(label);
				buf.append(" "+count+":"+label);
			}
			return buf.toString();
		}
	}

	public static void main(String[] args)
	{
	    System.out.println("ClassifiedDataset");
	}
}

