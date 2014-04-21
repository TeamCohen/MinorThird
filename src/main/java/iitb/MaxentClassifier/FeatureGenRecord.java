package iitb.MaxentClassifier;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.FeatureGenerator;
/**
 *
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */ 

public class FeatureGenRecord implements FeatureGenerator {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8292490593455124019L;
	protected int numCols;
	protected int numLabels;
	DataRecord dataRecord;
	public int addBias=0;
	class FeatureColumn implements Feature {
		int colId;
		int _y;
		float val;
		void copy(FeatureColumn f) {
			colId = f.colId;
			_y = f._y;
			val = f.val;
		}
		public int index() {return colId + _y*(numCols+addBias);}
		public int y() {return _y;}
		public int yprev() {return -1;}
		public float value() {return val;}
		public int[] yprevArray() {return null;}
	};
	FeatureColumn feature, featureToReturn;
	protected FeatureGenRecord(int numColumns, int numYs) {
		numCols =  numColumns;
		numLabels = numYs;
		feature = new FeatureColumn();
		featureToReturn = new FeatureColumn(); 
	}
	public int numFeatures() {return (numCols+addBias)*numLabels;}
	public void startScanFeaturesAt(DataSequence data, int pos) {
		dataRecord = (DataRecord)data;
		assert (pos == 0);
		feature.colId = firstColumnId();
		feature._y = 0;
	}
	public boolean hasNext() {
		return (feature.y() < numLabels);
	}
	protected int lastColumnIdp1() {return numCols+addBias;}
	protected int firstColumnId() {return 0;}
	public Feature next() {
		featureToReturn.copy(feature);
		feature.colId++;
		if (feature.colId >= lastColumnIdp1()) {
			feature.colId = firstColumnId();
			feature._y++;
		}
		if ((addBias==1) && (featureToReturn.colId == 0))
			featureToReturn.val = 1;
		else
			featureToReturn.val = dataRecord.getColumn(featureToReturn.colId-addBias);
		return featureToReturn;
	}
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#featureName(int)
	 */
	public String featureName(int featureIndex) {
		return "ColumnId=" + (featureIndex % (numCols+addBias)) + " label="+(featureIndex/(numCols+addBias));
	}
	public int xFeatureIdCurrent() {
		return feature.colId;
	}
};
