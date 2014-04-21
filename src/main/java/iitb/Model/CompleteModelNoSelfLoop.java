/** CompleteModelNoSelfLoop.java
 * Created on Jan 29, 2008
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
package iitb.Model;

import java.util.BitSet;
import java.util.StringTokenizer;

/*
 * No self loop in any label except "other"
 */
public class CompleteModelNoSelfLoop extends GenericModel {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8650040567397288593L;

	public CompleteModelNoSelfLoop(String spec, int numLabels) throws Exception {
        super(numLabels,0);
        StringTokenizer tokens = new StringTokenizer(spec,":");
        tokens.nextToken(); // this is the name of the model.
        BitSet dupLabels = new BitSet(numLabels);
        while (tokens.hasMoreTokens()) {
            dupLabels.set(Integer.parseInt(tokens.nextToken()));
        }
        _edges = new Edge[numLabels*numLabels-numLabels+dupLabels.cardinality()];
        startStates = new int[numLabels];
        for (int i = 0; i < numLabels; i++) {
                startStates[i] = i;
        }
        endStates = new int[numLabels];
        for (int i = 0; i < endStates.length; i++) {
            endStates[i] = i;
        }
        edgeStart = new int[numLabels];
        for (int i = 0, edgeNum=0; i < numLabels; i++) {
            edgeStart[i] = edgeNum;
            for (int j = 0; j < numLabels; j++) {
                if (!dupLabels.get(i) && (i == j)) 
                    continue;
                Edge edge = new Edge(i,j);
                _edges[edgeNum++] = edge;
            }
        }
    }
}
