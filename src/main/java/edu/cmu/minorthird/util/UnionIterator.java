package edu.cmu.minorthird.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnionIterator<E> implements Iterator<E>{
	
	private Iterator<E> a;
	private Iterator<E> b;
	
	public UnionIterator(Iterator<E> a,Iterator<E> b){
		this.a=a;
		this.b=b;
	}
	
	@Override
	public boolean hasNext(){ 
		return a.hasNext()||b.hasNext(); 
	}
	
	@Override
	public E next(){
		if(a.hasNext()){
			return a.next();
		}
		else if(b.hasNext()){
			return b.next();
		}
		else{
			return null;
		}
	}
	
	@Override
	public void remove(){
		System.err.println("remove() not implemented by "+this.getClass());
	}
	
	public static void main(String argv[]){
		List<String> list=new ArrayList<String>();
		for(int i=0;i<argv.length;i++){
			list.add(argv[i]);
		}
		for(Iterator<String> i=new UnionIterator<String>(list.iterator(),list.iterator());i.hasNext();System.out.println(i.next()));
	}
}
