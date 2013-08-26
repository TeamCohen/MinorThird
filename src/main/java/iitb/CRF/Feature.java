package iitb.CRF;
/**
 * A single feature returned by the FeatureGenerator needs to support this interface.
 * @author Sunita Sarawagi
 *
 */ 

public interface Feature {
    int index(); /** the index of this feature from 0..numFeatures-1. */
    int y();  /** has to be a label index from 0..numLabels-1 */
    int yprev(); /** can be -1 if the feature is a state, rather than an edge feature */
    float value(); /** any real value, don't return anything if 0 for efficiency */
    int[] yprevArray(); /** for history of length greater than 1, return array of prev values, will be ignore of history of length 1*/
  };
