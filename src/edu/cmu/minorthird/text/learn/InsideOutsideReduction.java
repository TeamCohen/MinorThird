package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Reducing an extraction task to tagging tokens as inside the type to extract,
 * or outside the type to extract.
 * 
 * @author William Cohen
 */

public class InsideOutsideReduction extends Extraction2TaggingReduction
		implements Serializable{

	private static final long serialVersionUID=1;

	// saves result of last reduction
	transient private NestedTextLabels taggedLabels;

	private String tokenProp="_inside";

	// all tag values that were used
	private Set<String> tagset=new HashSet<String>();

	@Override
	public void reduceExtraction2Tagging(AnnotationExample example){
		reduceDocument(example.getDocumentSpan(),example.getLabels(),example
				.getInputType(),example.getInputProp());
	}

	private void reduceDocument(Span doc,TextLabels labels,String spanType,String spanProp){
		taggedLabels=new NestedTextLabels(labels);
		assignDefaultLabels(doc,taggedLabels,spanType,spanProp);
		// label the tokens inside a span to be extracted as POS if there's just one
		// type to extract, or with the property value, otherwise.
		String id=doc.getDocumentId();
		Iterator<Span> i=
				spanType!=null?taggedLabels.instanceIterator(spanType,id):taggedLabels
						.getSpansWithProperty(spanProp,id);
		while(i.hasNext()){
			Span span=i.next();
			String tag=
					spanType!=null?ExampleSchema.POS_CLASS_NAME:taggedLabels.getProperty(
							span,spanProp);
			tagset.add(tag);
			for(int j=0;j<span.size();j++){
				taggedLabels.setProperty(span.getToken(j),tokenProp,tag);
			}
		}
	}

	@Override
	public String getTokenProp(){
		return tokenProp;
	}

	@Override
	public Set<String> getNonDefaultTagValues(){
		return tagset;
	}

	@Override
	public TextLabels getTaggedLabels(){
		return taggedLabels;
	}

	/**
	 * Return a TextLabels in which tagged tokens are used to solve the extraction
	 * problem.
	 */
	@Override
	public void extractFromTags(String output,MonotonicTextLabels taggedLabels){
		try{
			MixupProgram p=new MixupProgram();
			if(tagset.size()==1&&
					tagset.iterator().next().equals(ExampleSchema.POS_CLASS_NAME)){
				p.addStatement("defSpanType "+output+" =: "+
						makePattern(ExampleSchema.POS_CLASS_NAME));
			}else{
				for(Iterator<String> i=tagset.iterator();i.hasNext();){
					String tag=i.next();
					p.addStatement("defSpanProp "+output+":"+tag+" =: "+makePattern(tag));
				}
			}
			MixupInterpreter interp=new MixupInterpreter(p);
			interp.eval(taggedLabels);
		}catch(Mixup.ParseException ex){
			throw new IllegalStateException("mixup error: "+ex);
		}
	}

	private String makePattern(String val){
		return "... [L "+tokenProp+":"+val+"+ R] ...";
	}
}
