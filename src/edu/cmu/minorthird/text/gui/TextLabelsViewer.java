package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


/** View the contents of TextLabels.
 *  This is not quite working yet.
 *
 * @author William Cohen
 */


public class TextLabelsViewer extends SplitViewer
{
	private TextLabels labels; 
	public TextLabelsViewer(TextLabels labels)
	{
		super(new SpanLooperViewer(labels,labels.getTextBase().documentSpanIterator()),
					new MarkupPlan(labels).toGUI());
		this.labels = labels;
		setHorizontal();
	}
	public void receiveContent(Object content) {
		viewer1.setContent((Span)content);
		viewer2.setContent(new MarkupPlan(labels));
	}
	public boolean canReceive(Object content)	{
		return (content instanceof Span);
	}
	protected void handle(int signal,Object argument,ArrayList senders) {
		((SpanLooperViewer)viewer1).applyMarkup((MarkupPlan)argument);
	}
	protected boolean canHandle(int signal,Object argument,ArrayList senders) {
		return (signal==OBJECT_UPDATED && (argument instanceof MarkupPlan));
	}
}
