package iitb.MaxentClassifier;
import iitb.CRF.DataSequence;

import java.io.Serializable;
/**
 *
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */ 

public class DataRecord implements DataSequence, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4853002531871806868L;
	int label;
    float vals[];
    public DataRecord (int ncols) {
	vals = new float[ncols];
    }
    public DataRecord(DataRecord dr) {
	vals = new float[dr.vals.length];
	for (int i = 0; i < vals.length; vals[i] = dr.vals[i],i++);
	label = dr.label;
    }
    public DataRecord(float v[], int l) {
	vals = v;
	label = l;
    }
    public int length() {return 1;}
    public int y() {return label;}
    public int y(int i) {return label;}
    public Object x(int i) {return vals;}
    public void set_y(int i, int l) {label = l;}
    public float getColumn(int col) {return vals[col];}
    public void setColumn(int col, float val) {vals[col]=val;}
	public String toString() {
		String str="";
		for (int i = 0; i < vals.length; i++) {
			str += (vals[i] + " ");
		}
		str += label;
		return str;
	}
};
