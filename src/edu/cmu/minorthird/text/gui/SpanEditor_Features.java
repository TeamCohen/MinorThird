/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.MutableTextLabels;

import javax.swing.*;

/**
 * 
 * This class is responsible for... 
 *
 * @author $Author: ksteppe $
 * @version $Revision: 1.2 $
 */
public class SpanEditor_Features extends SpanEditor
{
    public SpanEditor_Features(
            edu.cmu.minorthird.text.TextLabels viewLabels,
            MutableTextLabels editLabels,
            JList documentList,
            SpanPainter spanPainter,
            StatusMessage statusMsg)
    {
        super(viewLabels, editLabels, documentList, spanPainter, statusMsg);
    }



}
