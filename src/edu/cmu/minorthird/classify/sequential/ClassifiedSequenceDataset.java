/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.ClassifiedDataset;
import edu.cmu.minorthird.util.gui.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A SequenceDataset that has been classified with a
 * SequenceClassifier.
 *
 * @author William Cohen
 */

public class ClassifiedSequenceDataset implements Visible
{
	private SequenceClassifier sequenceClassifier;
	private SequenceDataset sequenceDataset;
	private Classifier adaptedClassifier;
	
	public ClassifiedSequenceDataset(SequenceClassifier sequenceClassifier,SequenceDataset sequenceDataset)
	{
		this.sequenceClassifier = sequenceClassifier;
		this.sequenceDataset = sequenceDataset;
		this.adaptedClassifier = new AdaptedSequenceClassifier(sequenceClassifier,sequenceDataset);
	}
	public Classifier getClassifier()
	{
		return adaptedClassifier;
	}
	public Viewer toGUI()
	{
		Viewer cdv = new ClassifiedDataset(adaptedClassifier,sequenceDataset).toGUI();
		Viewer v = new TransformedViewer(cdv) {
				public Object transform(Object o) {
					ClassifiedSequenceDataset csd = (ClassifiedSequenceDataset)o;
					return new ClassifiedDataset(csd.adaptedClassifier,csd.sequenceDataset);
				}
			};
		v.setContent(this);
		return v;
	}

	/** Classifies examples from the sequenceDataset, by (a) mapping an
	 * example to it position in the containing sequence (b) classifying the
	 * containing sequence - caching it if necessary.
	 */
	private static class AdaptedSequenceClassifier implements Classifier, Visible
	{
		private SequenceClassifier sequenceClassifier;
		private class Place {
			Example[] seq;
			int index;
			public Place(Example[] seq,int index) { this.seq=seq; this.index=index; }
		}
		private Map instanceToPlace = new HashMap();
		private Map classifiedSeq = new HashMap();
		private Map explainedSeq = new HashMap();

		public AdaptedSequenceClassifier(SequenceClassifier sequenceClassifier,SequenceDataset sequenceDataset)
		{
			this.sequenceClassifier = sequenceClassifier;
			for (Iterator i=sequenceDataset.sequenceIterator(); i.hasNext(); ) {
				Example[] seq = (Example[])i.next();
				for (int j=0; j<seq.length; j++) {
					instanceToPlace.put( seq[j].getSource(), new Place(seq,j) );
				}
			}
		}
		public ClassLabel classification(Instance instance)
		{
			Place place = (Place)instanceToPlace.get(instance.getSource());
			if (place==null) 
				throw new IllegalArgumentException("instance src"+instance.getSource()+" not in "+instanceToPlace);
			ClassLabel[] labelSeq = (ClassLabel[]) classifiedSeq.get(place.seq);
			if (labelSeq==null) {
				classifiedSeq.put(place.seq, (labelSeq=sequenceClassifier.classification(place.seq)) );
			}
			return labelSeq[place.index];
		}
		public String explain(Instance instance)
		{
			Place place = (Place)instanceToPlace.get(instance.getSource());
			if (place==null) 
				throw new IllegalArgumentException("no explanation available");
			String explanation = (String) explainedSeq.get(place.seq);
			if (explanation==null) {
				explainedSeq.put(place.seq, (explanation=sequenceClassifier.explain(place.seq)) );
			}
			return explanation;
		}
		public Viewer toGUI()
		{
			return new SmartVanillaViewer(sequenceClassifier);
		}
	}
}
