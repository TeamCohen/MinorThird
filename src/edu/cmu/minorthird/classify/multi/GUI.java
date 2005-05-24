package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.awt.*;

/** Support routines for building GUI's to view datasets, instances, and
 * etc.
 *
 * @author William Cohen
 */

public class GUI 
{

    /** A JComponent holding a very concise rendering of an example. */
	public static JComponent conciseExampleRendererComponent(MultiExample e,int len,boolean emphasized)
	{
		if (e==null) return new JTextField("[null example]");
		String sourceString = e.getSource()==null ? "[null]" : StringUtil.truncate(len, e.getSource().toString());
		JTextField tf = new JTextField(e.getLabel()+" "+sourceString);
		Color c = (emphasized ? Color.blue: Color.black);
		tf.setBorder(BorderFactory.createLineBorder(c,2));
		return tf;
	}
    public static JComponent conciseMultiExampleRendererComponent(MultiExample e,int len,boolean emphasized)
	{
		if (e==null) return new JTextField("[null example]");
		String sourceString = e.getSource()==null ? "[null]" : StringUtil.truncate(len, e.getSource().toString());
		JTextField tf = new JTextField(e.getMultiLabel()+" "+sourceString);
		Color c = (emphasized ? Color.blue: Color.black);
		tf.setBorder(BorderFactory.createLineBorder(c,2));
		return tf;
	}

	/** Create and return a viewer which shows features of
	 * an example, plus it's source in another window. */
	public static Viewer newSourcedMultiExampleViewer()
	{
		ParallelViewer main = new ParallelViewer();
		main.addSubView("Features", new MultiExampleViewer() );
		main.addSubView("Source", new TransformedViewer(new SmartVanillaViewer()) {
				public Object transform(Object o) {
					return ((MultiExample)o).getSource();
				}
			});
		main.addSubView("Subpopulation", new TransformedViewer(new SmartVanillaViewer()) {
				public Object transform(Object o) {
					return "Subpopulation ID='"+((MultiExample)o).getSubpopulationId()+"'";
				}
			});
		return main;
	}

    /** A viewer for examples. */
    public static class MultiExampleViewer extends ComponentViewer
    {
	public MultiExampleViewer()
	{
	    super();
	}
	public MultiExampleViewer(MultiExample instance)
	{
	    super(instance);
	}
	public boolean canReceive(Object o) 
	{
	    return (o instanceof MultiExample);
	}
	public JComponent componentFor(Object o) 
	{
	    MultiExample e = (MultiExample)o;
	    JPanel p = new JPanel();
	    p.setLayout(new GridBagLayout());
	    GridBagConstraints gbc = fillerGBC();
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc = new GridBagConstraints();
	    p.add( new JLabel("Class label: "+e.getMultiLabel().toDetails()), gbc );
			
	    gbc = fillerGBC();
	    gbc.gridy = 1;
	    p.add( instanceComponent(e),  gbc);
	    return p;
	}
    }
    /** A viewer for instances */
	public static class InstanceViewer extends ComponentViewer
	{
		public InstanceViewer()	{	super();	}
		public InstanceViewer(Instance instance){	super(instance);	}
		public boolean canReceive(Object o) {	return (o instanceof Instance);	}
		public JComponent componentFor(Object o) 	{	return instanceComponent((Instance)o);}
	}
    private static JComponent instanceComponent(Instance instance)
    {
	int numRows = 0;
	for (Feature.Looper i=instance.featureIterator(); i.hasNext(); i.next()) {
	    numRows++;
	}
	Object[][] tableData = new Object[numRows][2];
	int k=0;
	for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
	    Feature f = i.nextFeature();
	    tableData[k][0] = f;
	    tableData[k][1] = new Double(instance.getWeight(f));
	    k++;
	}
	String[] columnNames = {"Feature Name", "Weight" };
	JTable table = new JTable(tableData,columnNames);
	return new JScrollPane(table);
    }

}