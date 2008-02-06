/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.semisupervised;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Instance;

/**
 * A set of semisupervised examples available for semi-supervised learning.
 *
 * @author Edoardo Airoldi
 * Date: Mar 15, 2004
 */

public interface SemiSupervisedActions
{
  /** Add a new semisupervised example to the dataset. */
  public void addUnlabeled(Instance instance);

  /** Return an iterator over all the semisupervised examples.  This iterator
   *  must always return examples in the order in which they were added,
   *  unless the data has been shuffled.
   */
  public Iterator<Instance> iteratorOverUnlabeled();

  /** Return the number of semisupervised examples. */
  public int sizeUnlabeled();

  /** Return whether the Dataset contains semisupervised examples available
   *  for semi-supervisedd learning. */
  public boolean hasUnlabeled();
}
