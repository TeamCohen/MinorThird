package edu.cmu.minorthird.util.gui;

import java.awt.Font;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Implements the console window for the gui
 *
 * @author Cameron Williams
 */

public class Console{

	private PipedInputStream piOut;

	private PipedOutputStream poOut;

	//  private final PipedInputStream piErr = new PipedInputStream();
	private JTextArea errorArea;

	private JScrollPane scroller;

	private Thread mainThread,readerThread;

	private boolean doMainRunning;

	private PrintStream oldSystemOut;

	private JButton viewButton;

//	private Object ui;

	private Object task;

//	private boolean labels;

	public Console(Console.Task t){
		this.errorArea=new JTextArea(20,100);
		errorArea.setFont(new Font("monospaced",Font.PLAIN,12));
		scroller=new JScrollPane(errorArea);
		this.task=t;
	}

	/** Constructor initializes the task, whether labels are present, and the viewButton */
	public Console(Object t,boolean l,JButton vb){
		this.errorArea=new JTextArea(20,100);
		errorArea.setFont(new Font("monospaced",Font.PLAIN,12));
		scroller=new JScrollPane(errorArea);
		this.task=t;
		this.viewButton=vb;
//		this.labels=l;
	}

	/** The outermost component of the console. */
	public JComponent getMainComponent(){
		return scroller;
	}

	/** Start the task for the console. */
	public void start(){
		initThreads();
		try{
			oldSystemOut=System.out;
			piOut=new PipedInputStream();
			poOut=new PipedOutputStream(piOut);
			System.setOut(new PrintStream(poOut,true));
		}catch(java.io.IOException io){
			errorArea.append("Couldn't redirect output\n"+io.getMessage()+"\n");
		}catch(SecurityException se){
			errorArea.append("SE error"+se.getMessage()+"\n");
		}
		mainThread.start();
	}

	/** Append a string to the console window. */
	public void append(String s){
		errorArea.append(s);
		scrollDown();
	}

	/** Clear the console window. */
	public void clear(){
		errorArea.setText("");
	}

	/** Re-initialize the threads for the console */
	private void initThreads(){
		// mainthread - runs the main task with doMain
		mainThread=new Thread(){

			@Override
			public void run(){
				if(task instanceof Console.Task){
					viewButton.setEnabled(false);
					if(!((Console.Task)task).getLabels()){
						errorArea
								.append("\nYou need to specify the labeled data you're using!\n"
										+"Modify the 'labels' parameters under base parameters section\n"
										+"of the parameter modification window.\n");
					}else{
						long startTime=System.currentTimeMillis();
						doMainRunning=true;
						readerThread.start();
						((Console.Task)task).doMain();
						double elapsedTime=(System.currentTimeMillis()-startTime)/1000.0;
						System.out.println("\nTotal time for task: "+elapsedTime+" sec");
						viewButton.setEnabled(((Console.Task)task).getMainResult()!=null);
						doMainRunning=false; // signal reader to stop
						try{
							readerThread.join(); // wait for it to end
						}catch(InterruptedException ex){
							System.err.println("reader interrupted?");
						}
						scrollDown();
					}
				}else{
					errorArea.append("Error: Task is not an instance of Console.Task \n");
				}//end else
			} //end run
		}; // end thread

		// this thread traps output from mainThread and sticks it in
		// console window
		readerThread=new Thread(){

			@Override
			public void run(){
				try{
					byte[] buf=new byte[2048];
					int len=0;
					while(doMainRunning){
						// look for more output
						if((len=piOut.read(buf))>0){
							errorArea.append(new String(buf,0,len));
							scrollDown();
						}
						yield(); // let someone else in to execute
					}
					// clean up any output we might have missed
					// after exiting
					if((len=piOut.read(buf))>0){
						errorArea.append(new String(buf,0,len));
						scrollDown();
					}
					closePipes();
				}catch(IOException e){
					errorArea.append(e.getMessage());
					System.out.println(e.getMessage());
				}
			}
		}; //end reader Thread
	} // constructor

	// imperfect method to scroll to bottom
	private void scrollDown(){
		try{
			JScrollBar bar=scroller.getVerticalScrollBar();
			bar.setValue(bar.getMaximum());
		}catch(Exception ex){
			System.err.println("error scrolling: "+ex);
		}
	}

	private void closePipes(){
		try{
			poOut.close();
			piOut.close();
			System.setOut(oldSystemOut);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	/** Interface for objects that can be configured with command-line arguments.
	 * Configuration for x is done by calling <code>x.doMain().</code>
	 */
	public interface Task{

		public boolean getLabels();

		public void doMain();

		public Object getMainResult();
	}
}
