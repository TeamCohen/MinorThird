package edu.cmu.minorthird.classify;

import java.util.Iterator;

/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */

public class BasicFeatureIndex extends DatasetIndex implements FeatureIndex{

	static final long serialVersionUID=20071015L;

	public BasicFeatureIndex(){
		super();
	}

	public BasicFeatureIndex(Dataset data){
		this();
		for(Iterator<Example> i=data.iterator();i.hasNext();){
			addExample(i.next());
		}
	}

	/** Number of examples with label l containing non-zero values for feature f. */
	public int size(Feature f,String label){
		int size=0;
		for(int j=0;j<size(f);j++){
			if(label.equals(getExample(f,j).getLabel().bestClassName())){
				size+=1;
			}
		}
		return size;
	}

	/** Get counts of feature f in i-th example containing feature f */
	@Override
	public double getCounts(Feature f,int i){
		return (featureIndex(f).get(i)).getWeight(f);
	}

	/** Get counts of feature f in examples with label l */
	public double getCounts(Feature f,String label){
		double total=0.0;
		for(int j=0;j<size(f);j++){
			//System.out.println( getExample(f,j).getLabel().bestClassName() );
			if(label.equals(getExample(f,j).getLabel().bestClassName())){
				total+=(featureIndex(f).get(j)).getWeight(f);
			}
		}
		return total;
	}

	/** Get number of documents which contain feature f with label l */
	public double getDocCounts(Feature f,String label){
		double total=0.0;
		for(int j=0;j<size(f);j++){
			//System.out.println( getExample(f,j).getLabel().bestClassName() );
			if(label.equals(getExample(f,j).getLabel().bestClassName())){
				total+=1.0;
			}
		}
		return total;
	}

	@Override
	public String toString(){
		StringBuilder buf=new StringBuilder("[index");
		for(Iterator<Feature> i=featureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append("\n"+f+":");
			for(int j=0;j<size(f);j++){
				buf.append("\n\t"+getExample(f,j).toString());
				//buf.append("\n\t"+"feature:"+f+" counts:"+getCounts(f,j));
				//buf.append(" label:"+getExample(f,j).getLabel().bestClassName());
			}
			buf.append("\n\t"+"feature:"+f+" posCounts:"+getCounts(f,"POS")+" negCouns:"+getCounts(f,"NEG"));
		}
		buf.append("\nindex]");
		return buf.toString();
	}

	public static void main(String[] args){
		System.out.println(new BasicFeatureIndex(SampleDatasets.sampleData("bayes",false)));
	}

}
