package edu.cmu.minorthird.text.learn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Reducing an extraction task to tagging tokens as one of five
 * categories.  The categories are: a token beginning the type to
 * extract, a token ending the type to extract, a single token which
 * is the type to extract, otherwise inside the type to extract, or
 * outside the type to extract.
 *
 * @author William Cohen
 */

// to do - test with spanProp

public class BeginContinueEndUniqueReduction extends
		Extraction2TaggingReduction implements Serializable{

	private static final long serialVersionUID=1;

	// saves result of last reduction
	transient private NestedTextLabels taggedLabels;

	private String tokenProp="_entityPart";

	// all tag values that were used
	private Set<String> tagset=new HashSet<String>();

	private boolean useSpanType=true;

	@Override
	public void reduceExtraction2Tagging(AnnotationExample example){
		reduceDocument(example.getDocumentSpan(),example.getLabels(),example
				.getInputType(),example.getInputProp());
	}

	private void reduceDocument(Span doc,TextLabels labels,String spanType,
			String spanProp){
		useSpanType=spanType!=null;
		taggedLabels=new NestedTextLabels(labels);
		// label all tokens as NEG
		assignDefaultLabels(doc,taggedLabels,spanType,spanProp);
		// label the tokens inside a span to be extracted as POS, if there's just one
		// type to extract, or with the property value, otherwise.
		String id=doc.getDocumentId();
		Iterator<Span> i=
				useSpanType?taggedLabels.instanceIterator(spanType,id):taggedLabels
						.getSpansWithProperty(spanProp,id);
		while(i.hasNext()){
			Span span=i.next();
			String baseTag=
					useSpanType?spanType:taggedLabels.getProperty(span,spanProp);
			tagset.add(baseTag);
			if(span.size()==0){
				throw new IllegalStateException("empty span "+span);
			}
			if(span.size()==1){
				String tag=baseTag+"Unique";
				taggedLabels.setProperty(span.getToken(0),tokenProp,tag);
			}else{
				String beginTag=baseTag+"Begin";
				taggedLabels.setProperty(span.getToken(0),tokenProp,beginTag);
				String endTag=baseTag+"End";
				taggedLabels.setProperty(span.getToken(span.size()-1),tokenProp,endTag);
				if(span.size()>2){
					String contTag=baseTag+"Continue";
					for(int j=1;j<span.size()-1;j++){
						taggedLabels.setProperty(span.getToken(j),tokenProp,contTag);
					}
				}
			}
		}
	}

	@Override
	public String getTokenProp(){
		return tokenProp;
	}

	@Override
	public Set<String> getNonDefaultTagValues(){
		Set<String> result=new HashSet<String>();
		for(Iterator<String> i=tagset.iterator();i.hasNext();){
			String baseTag=i.next();
			result.add(baseTag+"Unique");
			result.add(baseTag+"Begin");
			result.add(baseTag+"End");
			result.add(baseTag+"Continue");
		}
		return result;
	}

	@Override
	public TextLabels getTaggedLabels(){
		return taggedLabels;
	}

	/** Return a TextLabels in which tagged tokens are used 
	 * to solve the extraction problem. */
	@Override
	public void extractFromTags(String output,MonotonicTextLabels taggedLabels){
		try{
			MixupProgram p=new MixupProgram();
			if(useSpanType){
				String baseTag=tagset.iterator().next();
				p.addStatement("defSpanType "+output+" =: "+makePattern(baseTag));
			}else{
				for(Iterator<String> i=tagset.iterator();i.hasNext();){
					String baseTag=i.next();
					p.addStatement("defSpanProp "+output+":"+baseTag+" =: "+
							makePattern(baseTag));
				}
			}
			MixupInterpreter interp=new MixupInterpreter(p);
			interp.eval(taggedLabels);
		}catch(Mixup.ParseException ex){
			throw new IllegalStateException("mixup error: "+ex);
		}
	}

	private String makePattern(String baseTag){
		String p=tokenProp+":"+baseTag;
		return "... ["+p+"Begin L "+p+"Continue* R "+p+"End] ... || ... ["+p+
				"Unique] ...";
	}
}
