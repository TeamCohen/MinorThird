package edu.cmu.minorthird.util.gui;

import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.XYSeries;
import org.jfree.data.XYSeriesCollection;

import javax.swing.*;

/**
 * Wraps JFreeChart's XY line graph capability.
 * 
 * @author William cohen
 */

public class LineCharter
{
	private XYSeriesCollection collection = new XYSeriesCollection();
	private XYSeries currentSeries = null;
	private int numPoints = 0;
	private StringBuffer buf = new StringBuffer("");

	/** Start a new curve with a given label. */
	public void startCurve(String label) 
	{
		if (currentSeries!=null) {
			collection.addSeries(currentSeries);
			buf.append("\n");
		}
		buf.append("#begin "+label+"\n");
		currentSeries = new XYSeries(label);
		numPoints = 0;
	}
	/** Add a point to the current curve.
	 * @param mayDuplicate - there may be points x,y1 and x,y2 in the curve, ie
	 * it might nont be a function.
	 */
	public void addPoint(double x,double y,boolean mayDuplicate) 
	{
		if (currentSeries==null) throw new IllegalStateException("need to start series before adding points");
		if (mayDuplicate) currentSeries.add(x+0.00001*numPoints,y);
		else currentSeries.add(x,y);
		numPoints++;
		buf.append(x+"\t"+y+"\n");
	}
	/** Add a point to the current curve. */
	public void addPoint(double x,double y)
	{
		addPoint(x,y,true);
	}
	/** Get a panel showing the curves, with the given axis labels and title. */
	public JPanel getPanel(String title,String xlabel,String ylabel)
	{
		if (currentSeries!=null) {
			collection.addSeries(currentSeries);
		}
		JPanel mainPanel = new JPanel();
		JTabbedPane tabbedPane = new JTabbedPane();
		JFreeChart chart = ChartFactory.createXYLineChart(
			title,xlabel,ylabel,collection,PlotOrientation.VERTICAL,
			true,true,false);
		tabbedPane.add("Graphics",new ChartPanel(chart));
		tabbedPane.add("Text", new JScrollPane(new JTextArea("#"+xlabel+"\t"+ylabel+"\n" + buf.toString(), 20, 30)));
		mainPanel.add(tabbedPane);
		return mainPanel;
	}
}
