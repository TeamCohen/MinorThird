/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.MutableTextEnv;

import javax.swing.*;

/**
 * 
 * This class is responsible for... 
 *
 * @author $Author: ksteppe $
 * @version $Revision: 1.1 $
 */
public class SpanEditor_Features extends SpanEditor
{
    public SpanEditor_Features(
            edu.cmu.minorthird.text.TextEnv viewEnv,
            MutableTextEnv editEnv,
            JList documentList,
            SpanPainter spanPainter,
            StatusMessage statusMsg)
    {
        super(viewEnv, editEnv, documentList, spanPainter, statusMsg);
    }



}
