/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;

/**
 * Marker interface for SpanFeatureExtractor objects which allow one
 * to attach a type of required annotations that must be present
 * before feature extraction starts. This also allows one to attach to
 * a feature extractor an AnnotatorLoader, which helps find Annotators
 * to provide the required annotations.
 *
 * @author William Cohen
 */

public interface MixupCompatible
{
	/** A correct implementation of a MixupCompatible
	 * SpanFeatureExtractor will call
	 * <code>textLabels.require(annotation,null,loader)</code> before
	 * extracting features relative to textLabels.  A null annotation
	 * means that no <code>textLabels.require(...)</code> call will be made.
	 */
	public void setRequiredAnnotation(String annotation);

	/** Retrieve the annotation required by this SpanFeatureExtractor.
	 */
	public String getRequiredAnnotation();

	/** Attach an annotatorLoader to the SpanFeatureExtractor, which is
	 * used to find the required Annotation (and any other Annotations
	 * that that it might recursively require.)
	 */
	public void setAnnotatorLoader(AnnotatorLoader loader);
}
