/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SimpleFeatureExtractor;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class is responsible for...
 *
 * @author $Author: ksteppe $
 * @version $Revision: 1.1 $
 */
public class SpanFeaturesListModel implements ListModel, ListSelectionListener
{
    Logger log = Logger.getLogger(this.getClass());

    Span span;
    SimpleFeatureExtractor extractor;
    Instance curInstance;
    private List listeners;
    private List featureList;

    public SpanFeaturesListModel(Span span)
    {
        log.debug("Span = " + span);
        this.span = span;
        extractor = new SimpleFeatureExtractor();
        listeners = new ArrayList();
        featureList = new ArrayList();
        prepareList();
    }

    public int getSize()
    {
        return featureList.size();
    }

    public Object getElementAt(int index)
    {
        return featureList.get(index);
    }

    public void addListDataListener(ListDataListener l)
    {
        listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l)
    {
        listeners.remove(l);
    }

    public void valueChanged(ListSelectionEvent e)
    {
        log.debug("received change");
        JList jList = (JList) e.getSource();
        synchronized (jList)
        {
            this.span = (Span) jList.getSelectedValue();
            log.debug("got new span: " + span);
        }

        prepareList();
        for (int i = 0; i < listeners.size(); i++)
        {
            ListDataListener listDataListener = (ListDataListener) listeners.get(i);
            log.debug("sent change to: " + listDataListener);
            listDataListener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0,
                    this.getSize() - 1));
        }
    }

    private void prepareList()
    {
        curInstance = extractor.extractInstance(span);
        featureList.clear();
        Feature.Looper it = curInstance.featureIterator();
        while (it.hasNext())
        {
            Feature feature = it.nextFeature();
            double weight = curInstance.getWeight(feature);
            String featureDescription = weight + " : " + feature.toString();

            featureList.add(featureDescription);
        }
    }
}

