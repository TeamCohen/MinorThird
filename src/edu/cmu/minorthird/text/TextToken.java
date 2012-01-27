package edu.cmu.minorthird.text;

import java.io.Serializable;

/** 
 * Identifies a particular substring of a particular document.
 *
 * @author William Cohen
 */

public class TextToken implements Comparable<TextToken>,Serializable,Token{

	static private final long serialVersionUID=20080305L;

	//	private final String documentId;
	private final Document document;

	private final int lo,len;

	private String value=null;

	public TextToken(Document document,int lo,int len){
		this.document=document;
		this.lo=lo;
		this.len=len;
	}

	public String getDocumentId(){
		return document.getId();
	}

	public String getDocument(){
		return document.getText();
	}

	public int getLo(){
		return lo;
	}

	public int getLength(){
		return len;
	}

	public int getHi(){
		return lo+len;
	}

	@Override
	public String getValue(){
		if(value==null)
			value=document.getText().substring(lo,lo+len);
		return value;
	}

	//
	// implements Comparable
	//
	@Override
	public int compareTo(TextToken other){
		int cmp1=this.getDocumentId().compareTo(other.getDocumentId());
		if(cmp1!=0)
			return cmp1;
		int cmp2=lo-other.lo;
		if(cmp2!=0)
			return cmp2;
		return len-other.len;
	}

	//
	// for safe hashing
	//
	@Override
	public int hashCode(){
		return getDocumentId().hashCode()^lo^len;
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof TextToken){
			return compareTo((TextToken)o)==0;
		}
		else{
			return false;
		}
	}

	public String asString(){
		return getDocumentId()+"$Substr/"+lo+","+len+":'"+getValue()+"'";
	}

	@Override
	public String toString(){
		return "[token '"+getValue()+"']";
	}
}
