package edu.cmu.minorthird.text.gui;

import javax.swing.*;

/** A simple display tool for status messages.
 *
 * @author William Cohen
 */

public class StatusMessage extends JLabel
{
    public StatusMessage()
    {
        super("");
    }

    public void display(String msg)
    {
        setText(msg);
    }
}
