package edu.cmu.minorthird.util;


/**
 * Indicates intermediate progress for a trainer, etc.
 *
 */
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

public class ProgressCounter
{
	static JProgressBar[] graphicContext = new JProgressBar[0];

	public static void setGraphicContext(JProgressBar[] context)
	{ 
		graphicContext = context;
	} 
	public static void clearGraphicContext() 
	{ 
		graphicContext = new JProgressBar[0];
	} 

	private static List stack = new LinkedList();
	private static final int TIME_BTWN_OUTPUTS_IN_MS=1000;

	private String task,step;
	private int numSteps;
	private int depth;
	private long startTime;
	private long lastOutputTime;
	private int stepsCompleted;
	private JProgressBar graphicCounter;
	Logger log = Logger.getLogger(this.getClass() + ":" + task);


	public ProgressCounter(String task,String step,int numSteps) {
		this.task = task;
		this.step = step;
		this.stepsCompleted = 0;
		this.numSteps = numSteps;
		this.depth = stack.size();
		this.startTime = this.lastOutputTime = System.currentTimeMillis();
		if (depth<graphicContext.length) {
			graphicCounter = graphicContext[depth];
			graphicCounter.setValue(0);
			if (numSteps>=0)
        graphicCounter.setMaximum(numSteps);

			graphicCounter.setIndeterminate(numSteps<0);
			graphicCounter.setString(numSteps>=0 ? (" "+task+" for "+numSteps+" "+step+"s ") : task);
			graphicCounter.setStringPainted(true);
		} else {
			graphicCounter = null;
		}
		stack.add( this );
		log.debug("start");
	}
	public ProgressCounter(String task,int numSteps) {
		this(task,"step",numSteps);
	}
	public ProgressCounter(String task,String step) {
		this(task,step,-1);
	}
	public ProgressCounter(String task) {
		this(task,"step",-1);
	}
	
	/** Record one step of progress on the task */
	public void progress() {
		stepsCompleted++;
		if (graphicCounter!=null) {
			graphicCounter.setValue(stepsCompleted);
		}
		long time = System.currentTimeMillis();
		if (time - lastOutputTime > TIME_BTWN_OUTPUTS_IN_MS) {
			for (int i=0; i<depth; i++) System.out.print("| ");
			if (numSteps >= 0) {
				System.out.println(
					"Task "+task+": "+(100.0*stepsCompleted/numSteps)+"% in "+(time-startTime)/1000.0+" sec");
			} else {
				System.out.println(
					"Task "+task+": "+stepsCompleted+" "+step+"(s) in "+(time-startTime)/1000.0+" sec");
			}
			lastOutputTime = time;
		}
	}

	/** Record this task as completed. */
	public void finished() {
    if (graphicCounter != null && graphicCounter.isIndeterminate())
    {
      graphicCounter.setIndeterminate(false);
      graphicCounter.setMaximum(stepsCompleted);
      graphicCounter.setValue(stepsCompleted);
    }
    int k = stack.indexOf(this);
		if (k>=0) stack = stack.subList( 0, k );

    log.info("finished in " + stepsCompleted + " steps.");
	}
}
