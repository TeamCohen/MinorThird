package edu.cmu.minorthird.classify.algorithms.random;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Edoardo Airoldi
 * Date: Dec 11, 2004
 */

public class Estimate{

	private String model;

	private String parameterization;

	private SortedMap<String,Double> pms;

	public Estimate(String mod,String param,SortedMap<String,Double> pms){
		this.model=mod;
		this.parameterization=param;
		this.pms=pms;
	}

	public String getModel(){
		return this.model;
	}

	public String getParameterization(){
		return this.parameterization;
	}

	public SortedMap<String,Double> getPms(){
		return this.pms;
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("[ ");
		buf.append("model="+model+", "+"parameterization="+parameterization+" : ");
		Set<String> names=pms.keySet();
		for(Iterator<String> i=names.iterator();i.hasNext();){
			String key=i.next();
			double value=pms.get(key);
			buf.append(key+"="+value+", ");
		}
		buf.delete(buf.length()-2,buf.length());
		buf.append(" ]");
		return buf.toString();
	}

	public String toTableInViewer(){
		StringBuffer buf=new StringBuffer("");
		buf.append(model);
		Set<String> names=pms.keySet();
		for(Iterator<String> i=names.iterator();i.hasNext();){
			String key=i.next();
			double value=pms.get(key);
			buf.append(", "+key+"="+value);
		}
		return buf.toString();
	}

	// Test Estimate
	static public void main(String[] args){
		SortedMap<String,Double> mudelta=new TreeMap<String,Double>();
		mudelta.put("mu",new Double(0.661));
		mudelta.put("delta",new Double(0.035));
		Estimate theta=new Estimate("Binomial","mu/delta",mudelta);
		System.out.println(theta.toString());
		System.out.println("|"+theta.toTableInViewer()+"|");
		SortedMap<String,Double> pn=new TreeMap<String,Double>();
		pn.put("p",new Double(0.661));
		pn.put("N",new Double(11));
		Estimate gamma=new Estimate("Binomial","p/N",pn);
		System.out.println(gamma.toString());
		System.out.println("|"+gamma.toTableInViewer()+"|");
	}
}
