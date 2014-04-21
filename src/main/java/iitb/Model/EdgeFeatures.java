package iitb.Model;
import iitb.CRF.DataSequence;
/**
 *
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */ 

public class EdgeFeatures extends FeatureTypes {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6098103393225258231L;
	transient protected EdgeIterator edgeIter = null;
	protected int edgeNum;
	transient boolean edgeIsOuter;
	Object labelNames[];
	public EdgeFeatures(FeatureGenImpl m, Object labels[]) {
		super(m);
		labelNames=labels;
	}
	public EdgeFeatures(FeatureGenImpl m) {
		this(m,null);
	}
	protected void setEdgeIter() {
	    edgeIter = model.edgeIterator();
	}
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
		if (prevPos < 0) {
			edgeNum = model.numEdges();
			return false;
		} else {
			edgeNum = 0;
			if (edgeIter == null) {
			    setEdgeIter();
			}
			if (edgeIter != null)
			    edgeIter.start();
			return hasNext();
		}
	}
	public boolean hasNext() {
		return (edgeIter != null) && (edgeIter.hasNext());
	}	
	public boolean lastEdgeWasOuter() {return edgeIsOuter;}
	public void next(FeatureImpl f) {
		edgeIsOuter = edgeIter.nextIsOuter();
		Edge e = edgeIter.next();
		Object name="";
		if (featureCollectMode()) {
			if (labelNames == null) {
				name = "E."+(edgeIsOuter?(""+model.label(e.start)):("I." + e.start));
			} else {
				name = labelNames[model.label(e.start)];
			}
		}
		if (edgeIsOuter) {
			setFeatureIdentifier(model.label(e.start)*model.numberOfLabels()+model.label(e.end) + model.numEdges(), e.end,name,f);
		} else {
			setFeatureIdentifier(edgeNum,e.end,name,f);
		}
		f.ystart = e.start;
		f.yend = e.end;
		f.val = 1;
		edgeNum++;
	}
	
	 public boolean fixedTransitionFeatures() {
	     return true;
	     /*((model.numStartStates()==model.numStates())&&
	             (model.numEndStates()==model.numEndStates()));
	             */
	 }
};
