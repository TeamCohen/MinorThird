package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;

/** Interactivly edit document spans.
 *
 * @author William Cohen
 */

public class SpanLabeler extends ViewerTracker
{
    private static final String EDITOR_PROP = SpanEditor.EDITOR_PROP;
	  // map top-level spans to type that labels them
	  private static final String LABEL_PROP = "userLabel";
    private static final String NULL_TYPE = "- pick label -";
    private static final String UNKNOWN_TYPE = "- unknown -";

    // internal state
    private String labelType = UNKNOWN_TYPE;

    // buttons
    final JLabel currentTypeLabel = new JLabel(UNKNOWN_TYPE);
    final JComboBox typeBox = new LabelChooserBox();
    final JButton addCurrentTypeButton = new JButton(new AddCurrentTypeAction("Accept class:"));
    final JTextField newTypeField = new JTextField(15);
    final JButton addNewTypeButton = new JButton(new AddNewTypeAction("New class:"));

    /**
     * @param viewEnv a superset of editEnv which may include some additional read-only information
     * @param editEnv the environment being modified
     * @param documentList the document Span being edited is associated with
     * the selected entry of the documentList.
     * @param spanPainter used to repaint documentList elements
     * @param statusLabel a JLabel used for status messages.
     */
    public SpanLabeler(
            TextEnv viewEnv,
            MutableTextEnv editEnv,
            JList documentList,
            SpanPainter spanPainter,
            StatusMessage statusMsg)
    {
        super(viewEnv, editEnv, documentList, spanPainter, statusMsg);
        setViewEntireDocument(true);
        newTypeField.addActionListener(addNewTypeButton.getAction());

				restoreLabelProps();

        //
        // layout stuff
        //
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        int col = 0;
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 2.0;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(currentTypeLabel, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(addCurrentTypeButton, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(typeBox, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(addNewTypeButton, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(newTypeField, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(upButton, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(downButton, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.gridx = ++col;
        gbc.gridy = 2;
        add(saveButton, gbc);
        //saveButton.setEnabled( saveAsFile!=null );

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = col;
        add(editorHolder, gbc);

        loadSpan(nullSpan());
    }

	  // if documentSpan s has type t, then setProperty(s,LABEL_PROP,t)
    private void restoreLabelProps()
	  {
			//System.out.println("restoring label properties");
			for (Span.Looper i=editEnv.getTextBase().documentSpanIterator(); i.hasNext(); )
			{
				Span s = i.nextSpan();
				for (Iterator j=editEnv.getTypes().iterator(); j.hasNext(); )
				{
					String t = (String)j.next();
					if (editEnv.hasType(s,t)) 
					{
						//System.out.println("restoring "+t+" for "+s);
						editEnv.setProperty(s,LABEL_PROP,t);
					}
				}
			}
		}

    protected void loadSpanHook()
    {
        String oldLabel = editEnv.getProperty(documentSpan, LABEL_PROP);
        if (oldLabel == null) { 
					currentTypeLabel.setText(UNKNOWN_TYPE);
				} else {
					currentTypeLabel.setText(oldLabel);
				}
    }


    /** Say where the viewer is.. */
    public void addViewer(TextBaseViewer viewer)
    {
        this.viewer = viewer;
    }

    private TextBaseViewer viewer = null;

    private class AddNewTypeAction extends AbstractAction
    {
        public AddNewTypeAction(String msg)
        {
            super(msg);
        }

        public void actionPerformed(ActionEvent event)
        {
            String type = newTypeField.getText().trim();
            if (!editEnv.isType(type))
            {
                typeBox.addItem(type);
                if (viewer != null)
                {
                    viewer.getGuessBox().addItem(type);
                    viewer.getTruthBox().addItem(type);
                    viewer.getDisplayedTypeBox().addItem(type);
                }
            }
            setDocumentType(type);
        }
    }

    private class AddCurrentTypeAction extends AbstractAction
    {
        public AddCurrentTypeAction(String msg)
        {
            super(msg);
        }

        public void actionPerformed(ActionEvent event)
        {
            String type = (String) typeBox.getSelectedItem();
            if (!NULL_TYPE.equals(type)) setDocumentType(type);
        }
    }

    private class LabelChooserBox extends JComboBox
    {
        public LabelChooserBox()
        {
            super();
            addItem(NULL_TYPE);
            for (Iterator i = editEnv.getTypes().iterator(); i.hasNext();)
            {
                addItem(i.next());
            }
            addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    String t = (String) getSelectedItem();
                    if (!NULL_TYPE.equals(t)) setDocumentType(t);
                }
            });
        }
    }

    private void setDocumentType(String type)
    {
			  statusMsg.display("setting type="+type+"for "+documentSpan);
        currentTypeLabel.setText(type);
        String oldLabel = editEnv.getProperty(documentSpan, LABEL_PROP);
        if (oldLabel != null)
        {
            // clear the old label
            editEnv.defineTypeInside(oldLabel, documentSpan, new BasicSpanLooper(Collections.EMPTY_SET.iterator()));
        }
        editEnv.setProperty(documentSpan, LABEL_PROP, type);
        editEnv.addToType(documentSpan, type);
        editEnv.closeTypeInside(type, documentSpan);
    }
}

