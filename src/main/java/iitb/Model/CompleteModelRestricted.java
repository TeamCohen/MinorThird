/** CompleteModelRestricted.java
 * Created on Jan 29, 2008
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
package iitb.Model;

import java.util.HashSet;
import java.util.StringTokenizer;

public class CompleteModelRestricted extends GenericModel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3151215795713987849L;

	public CompleteModelRestricted(String spec, int numLabels) throws Exception {
        super(numLabels,0);
        StringTokenizer tokens = new StringTokenizer(spec,":");
        tokens.nextToken(); // this is the name of the model.
        HashSet<Edge> followEdges = new HashSet<Edge>();
        HashSet<Integer> followLabels = new HashSet<Integer>();
        int startLabel=-1;
        while (tokens.hasMoreTokens()) {
            int parent = Integer.parseInt(tokens.nextToken());
            int child = Integer.parseInt(tokens.nextToken());
            if (parent >= 0) {
                followEdges.add(new Edge(parent,child));
                followLabels.add(child);
            } else {
                startLabel = child;
            }
        }
        _edges = new Edge[numLabels*numLabels-followEdges.size()*(numLabels-2)-((startLabel>=0)?1:0)*(numLabels-1)];
        
        startStates = new int[numLabels-followEdges.size()];
        for (int i = 0, st = 0; i < numLabels; i++) {
            if (!followLabels.contains(i))
                startStates[st++] = i;
        }
        endStates = new int[numLabels];
        for (int i = 0; i < endStates.length; i++) {
            endStates[i] = i;
        }
        
        edgeStart = new int[numLabels];
        for (int i = 0, edgeNum=0; i < numLabels; i++) {
            edgeStart[i] = edgeNum;
            for (int j = 0; j < numLabels; j++) {
                if ((j==startLabel) && (i != j)) 
                    continue;
                Edge edge = new Edge(i,j);
                if ((i != j) && followLabels.contains(j) && !followEdges.contains(edge)) {
                    continue;
                }
                _edges[edgeNum++] = edge;
            }
        }
    }
}
