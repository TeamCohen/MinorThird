package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;
import java.util.TreeSet;

import edu.cmu.minorthird.classify.ExampleSchema;

/**
 * @author William Cohen
*/

public class Segmentation
{
	private TreeSet<Segment> segments = new TreeSet<Segment>();
	private ExampleSchema schema;

	public Segmentation(ExampleSchema schema) { this.schema = schema; }
	public void add(Segment seg) { segments.add(seg); }
	public boolean contains(Segment seg) { return segments.contains(seg); }
	public Iterator<Segment> iterator() { return segments.iterator(); }
	public int size() { return segments.size(); }
	public String className(Segment seg)
	{
		String name = schema.getClassName(seg.y);
		return ExampleSchema.NEG_CLASS_NAME.equals(name) ? null : name;
	}
	@Override
	public String toString() { return "[Segmentation: "+segments+"]"; }

	static public class Segment implements Comparable<Segment>
	{
		public final int lo,hi,y;
		public Segment(int lo,int hi,int y) { this.lo=lo; this.hi=hi; this.y=y; }
		@Override
		public int compareTo(Segment b) {
			int cmp = lo - b.lo;
			if (cmp!=0) return cmp;
			cmp = hi - b.hi;
			if (cmp!=0) return cmp;
			return y - b.y;
		}
		@Override
		public String toString() { return "[Segment "+lo+".."+hi+";"+y+"]"; }
	}
}
