/** DataSet.java
 * 
 * @author Sunita Sarawagi
 * @since 1.0
 * @version 1.3
 */
package iitb.MaxentClassifier;
import java.util.Vector;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;

public class DataSet implements DataIter {
    Vector allRecords;
    int currPos = 0;
    public DataSet(Vector recs) {allRecords = recs;}
    public void startScan() {currPos = 0;}
    public boolean hasNext() {return (currPos < allRecords.size());}
    public DataSequence next() {currPos++;return (DataRecord)allRecords.elementAt(currPos-1);}
};
