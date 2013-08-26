/** NoEdgeModel.java
 *
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */ 
package iitb.Model;

public class NoEdgeModel extends CompleteModel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 8955183589954114429L;
	class EmptyEdgeIter implements EdgeIterator {
	public void start(){}
	public boolean hasNext(){return false;}
	public Edge next(){return null;}
	public boolean nextIsOuter() {
		return false;}
    };
    EmptyEdgeIter emptyIter;
    public NoEdgeModel(int nlabels) {
	super(nlabels);
	emptyIter = new EmptyEdgeIter();
	name = "NoEdge";
    }
    public int numEdges() {return 0;}
    public EdgeIterator edgeIterator() {
	return emptyIter;
    }
};
