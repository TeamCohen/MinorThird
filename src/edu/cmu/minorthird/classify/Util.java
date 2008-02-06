package edu.cmu.minorthird.classify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Util{
	
	public static <T extends Example> Iterator<Instance> toInstanceIterator(Iterator<T> it){
		List<Instance> list=new ArrayList<Instance>();
		for(list=new ArrayList<Instance>();it.hasNext();list.add(it.next()));
		return list.iterator();
	}

}
