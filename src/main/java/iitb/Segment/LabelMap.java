/** LabelMap.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3
 */

package iitb.Segment;

/**
 * 
 * @author Sunita Sarawagi
 * 
 */

public class LabelMap {

	/**
	 * Decrements the parameter by one.
	 * @param l Value to be decremented.
	 * @return l - 1
	 */
	public int map(int l) {
		return l - 1;
	}

	/**
	 * Increments the parameter by one.
	 * @param l Value to be incremented.
	 * @return l + 1
	 */
	public int revMap(int l) {
		return l + 1;
	}
}

class BinaryLabelMap extends LabelMap {
	
	int posClass;

	BinaryLabelMap(int sel) {
		posClass = sel;
	}

	public int map(int el) {
		return (posClass == el) ? 1 : 0;
	}

	public int revMap(int label) {
		return (label == 1) ? posClass : 0;
	}
}
