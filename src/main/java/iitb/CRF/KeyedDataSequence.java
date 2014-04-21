/*
 * Created on Dec 29, 2007
 * @author sunita
 */
package iitb.CRF;

public interface KeyedDataSequence extends DataSequence {
    public int getKey();
    public void setKey(int key);
}
