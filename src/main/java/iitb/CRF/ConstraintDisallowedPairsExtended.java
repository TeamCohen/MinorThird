/*
 * Created on Dec 31, 2004
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public interface ConstraintDisallowedPairsExtended extends ConstraintDisallowedPairs {
    /*
     * This returns the canonical id with respect to which we can maintain the state for distint pairs
     * For eg. to ensure distinctiveness of labels encoded as BCEU, this would return "U" when the argument is any of the four labels.
     */
    int canonicalId(int label);
}
