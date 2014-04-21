package edu.cmu.minorthird.classify;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;

/** 
 * Support routines for building GUI's to view datasets, instances, and
 * etc.
 *
 * @author William Cohen
 */

public class GUI{

	/** A JComponent holding a very concise rendering of an example. */
	public static JComponent conciseExampleRendererComponent(Example e,int len,
			boolean emphasized){
		if(e==null)
			return new JTextField("[null example]");
		String sourceString=
				e.getSource()==null?"[null]":StringUtil.truncate(len,e.getSource()
						.toString());
		JTextField tf=new JTextField(e.getLabel()+" "+sourceString);
		Color c=(emphasized?Color.blue:Color.black);
		tf.setBorder(BorderFactory.createLineBorder(c,2));
		return tf;
	}

	/** Create and return a viewer which shows features of
	 * an example, plus it's source in another window. */
	public static Viewer newSourcedExampleViewer(){
		ParallelViewer main=new ParallelViewer();
		main.addSubView("Features",new ExampleViewer());
		main.addSubView("Source",new TransformedViewer(new SmartVanillaViewer()){

			static final long serialVersionUID=20071015;

			@Override
			public Object transform(Object o){
				return ((Example)o).getSource();
			}
		});
		main.addSubView("Subpopulation",new TransformedViewer(
				new SmartVanillaViewer()){

			static final long serialVersionUID=20071015;

			@Override
			public Object transform(Object o){
				return "Subpopulation ID='"+((Example)o).getSubpopulationId()+"'";
			}
		});
		return main;
	}

	/** A viewer for examples. */
	public static class ExampleViewer extends ComponentViewer{

		static final long serialVersionUID=20071015;

		public ExampleViewer(){
			super();
		}

		public ExampleViewer(Example instance){
			super(instance);
		}

		@Override
		public boolean canReceive(Object o){
			return(o instanceof Example);
		}

		@Override
		public JComponent componentFor(Object o){
			Example e=(Example)o;
			JPanel p=new JPanel();
			p.setLayout(new GridBagLayout());
			GridBagConstraints gbc=fillerGBC();
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc=new GridBagConstraints();
			p.add(new JLabel("Class label: "+e.getLabel().toDetails()),gbc);

			gbc=fillerGBC();
			gbc.gridy=1;
			p.add(instanceComponent(e),gbc);
			return p;
		}
	}

	/** A viewer for instances */
	public static class InstanceViewer extends ComponentViewer{

		static final long serialVersionUID=20071015;

		public InstanceViewer(){
			super();
		}

		public InstanceViewer(Instance instance){
			super(instance);
		}

		@Override
		public boolean canReceive(Object o){
			return(o instanceof Instance);
		}

		@Override
		public JComponent componentFor(Object o){
			return instanceComponent((Instance)o);
		}
	}

	private static JComponent instanceComponent(Instance instance){
		int numRows=0;
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();i.next()){
			numRows++;
		}
		Object[][] tableData=new Object[numRows][2];
		int k=0;
		for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
			Feature f=i.next();
			tableData[k][0]=f;
			tableData[k][1]=new Double(instance.getWeight(f));
			k++;
		}
		String[] columnNames={"Feature Name","Weight"};
		JTable table=new JTable(tableData,columnNames);
		return new JScrollPane(table);
	}
}
