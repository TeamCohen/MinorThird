/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * This class uses a SpanFeaturesListModel pane
 *
 * @author $Author: ksteppe $
 * @version $Revision: 1.1 $
 */
public class TextBaseEditor_Features extends TextBaseEditor
{
    SpanFeaturesListModel spanFeatures;

    public TextBaseEditor_Features(String[] args)
    {
        super(args);
        spanFeatures = new SpanFeaturesListModel(this.viewerTracker.documentSpan);
        viewer.getDocumentList().addListSelectionListener(spanFeatures);
    }

    public void initializeLayout()
    {
        setPreferredSize(new Dimension(800, 600));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        viewer.setMinimumSize(new Dimension(200, 200));
        viewerTracker.setMinimumSize(new Dimension(200, 50));

        JList featureList = new JList(spanFeatures);
        JScrollPane scroll = new JScrollPane(featureList);
        JSplitPane lowerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewerTracker, scroll);
        lowerSplit.setDividerLocation(650);
//        lowerSplit.setDividerLocation(0.95d);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, viewer, lowerSplit);
        splitPane.setDividerLocation(400);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(splitPane, gbc);

    }

    public static void main(String[] args)
    {
        try
        {
					  //org.apache.log4j.xml.DOMConfigurator.configure(args[args.length - 1]);
            TextBaseEditor editor = new TextBaseEditor_Features(args);
            editor.initializeLayout();
            editor.buildFrame();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Error e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }
}
