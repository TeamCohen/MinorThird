package edu.cmu.minorthird.util;


/**
 * Indicates intermediate progress for a trainer, etc.
 *
 */
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;
import edu.cmu.minorthird.ui.*;

public class ProgressCounter
{
	static private Logger log = Logger.getLogger(ProgressCounter.class);
	static JProgressBar[] graphicContext = new JProgressBar[0];

	public static void setGraphicContext(JProgressBar[] context)
	{ 
		graphicContext = context;
	}
    
  public static void setOutputContext(JTextArea ea)
  {
    errorArea = ea;
  }
 
	public static void clearGraphicContext() 
	{ 
		graphicContext = new JProgressBar[0];
	} 

  // keep track of the global depth of progress counters
  static int currentDepth = 0;
	private static final int TIME_BTWN_OUTPUTS_IN_MS=1000;

	private String task,step;
	private int numSteps;
	private int depth;
	private long startTime;
	private long lastOutputTime;
	private int stepsCompleted;
  private JProgressBar graphicCounter;
  private static JTextArea errorArea = null;


	public ProgressCounter(String task,String step,int numSteps) {
		this.task = task;
		this.step = step;
		this.stepsCompleted = 0;
		this.numSteps = numSteps;
    this.depth = currentDepth++;
		this.startTime = this.lastOutputTime = System.currentTimeMillis();
		if (depth<graphicContext.length) {
      synchronized(graphicContext) {
        graphicCounter = graphicContext[depth];
        graphicCounter.setValue(0);
        if (numSteps>=0)
			    graphicCounter.setMaximum(numSteps);
        graphicCounter.setIndeterminate(numSteps<0);
        graphicCounter.setString(numSteps>=0 ? (" "+task+" for "+numSteps+" "+step+"s ") : task);
        graphicCounter.setStringPainted(true);
      }
		} else {
			graphicCounter = null;
		}
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
	public synchronized void progress() {
		stepsCompleted++;
		long time = System.currentTimeMillis();
		if (time - lastOutputTime > TIME_BTWN_OUTPUTS_IN_MS) {
      if (graphicCounter!=null) {
        synchronized(graphicContext) {
          graphicCounter.setValue(stepsCompleted);
        }
      }
      try{
				System.out.flush();
      } catch (Exception e) {
      }
      //if (errorArea == null) {
			for (int i=0; i<depth; i++) System.out.print("| ");
			if (numSteps >= 0) {
        try{
          System.out.flush();
        } catch (Exception e) {
        }
        System.out.println("Task "+task+": "+(100.0*stepsCompleted/numSteps)+"% in "+(time-startTime)/1000.0+" sec");
			} else {
        try{
          System.out.flush();
        } catch (Exception e) {
        }
        System.out.println("Task "+task+": "+stepsCompleted+" "+step+"(s) in "+(time-startTime)/1000.0+" sec");
			}
			lastOutputTime = time;
		}
	}

	/** Record this task as completed. */
	public void finished() 
  {
    if (graphicCounter != null && graphicCounter.isIndeterminate())
    {
      synchronized(graphicContext) {
        graphicCounter.setIndeterminate(false);
        graphicCounter.setMaximum(stepsCompleted);
        graphicCounter.setValue(stepsCompleted);
        graphicCounter.setStringPainted(false);
      }
    }
    currentDepth = depth;
	}
}
