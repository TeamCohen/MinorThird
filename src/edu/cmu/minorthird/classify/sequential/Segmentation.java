package edu.cmu.minorthird.classify.sequential;

import java.util.*;
import edu.cmu.minorthird.classify.*;

/**
 * @author William Cohen
*/

public class Segmentation
{
	private TreeSet segments = new TreeSet();
	private ExampleSchema schema;

	public Segmentation(ExampleSchema schema) { this.schema = schema; }
	public void add(Segment seg) { segments.add(seg); }
	public boolean contains(Segment seg) { return segments.contains(seg); }
	public Iterator iterator() { return segments.iterator(); }
	public int size() { return segments.size(); }
	public String className(Segment seg)
	{
		String name = schema.getClassName(seg.y);
		return ExampleSchema.NEG_CLASS_NAME.equals(name) ? null : name;
	}
	public String toString() { return "[Segmentation: "+segments+"]"; }

	static public class Segment implements Comparable
	{
		public final int lo,hi,y;
		public Segment(int lo,int hi,int y) { this.lo=lo; this.hi=hi; this.y=y; }
		public int compareTo(Object o) {
			Segment b = (Segment)o;
			int cmp = lo - b.lo;
			if (cmp!=0) return cmp;
			cmp = hi - b.hi;
			if (cmp!=0) return cmp;
			return y - b.y;
		}
		public String toString() { return "[Segment "+lo+".."+hi+";"+y+"]"; }
	}
}
