package iitb.Model;

import java.io.Serializable;

public class Edge implements Comparable, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7286805255245543629L;
	public int start;
    public int end;
    Edge() {;}
    public Edge(int s, int e) {
	start = s;
	end = e;
    }
    String tostring() {
	return (start + " -> " + end);
    }
    public int compareTo(Object o) {
	Edge e = (Edge)o;
	return ((start != e.start)?(start - e.start):(end-e.end));
    }
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + end;
        result = PRIME * result + start;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Edge other = (Edge) obj;
        if (end != other.end)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
};
