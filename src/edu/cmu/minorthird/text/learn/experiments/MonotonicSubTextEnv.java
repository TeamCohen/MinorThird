package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.text.*;
import java.util.Set;

/** A subset of another TextEnv that can be added to.
 * Additions are propogated back to the underlying MonotonicTextEnv
 * passed in as an argument.
 *
 * @author William Cohen
*/

public class MonotonicSubTextEnv extends SubTextEnv implements MonotonicTextEnv
{
	private MonotonicTextEnv monotonicEnv;

	public MonotonicSubTextEnv(SubTextBase subBase,MonotonicTextEnv env) {
		super(subBase,env);
		this.monotonicEnv = env;
	}

	public void defineDictionary(String dictName,Set dict) {
		monotonicEnv.defineDictionary( dictName, dict );
	}
		
	public void setProperty(Token token,String prop,String value) {
		monotonicEnv.setProperty(token,prop,value);
	}

	public void setProperty(Span span,String prop,String value) {
		if (subBase.contains(span))	
			monotonicEnv.setProperty(span,prop,value);
	}

	public void addToType(Span span,String type) {
		if (subBase.contains(span))	
			monotonicEnv.addToType(span,type);
	}

	public void addToType(Span span,String type,Details details) {
		if (subBase.contains(span))	
			monotonicEnv.addToType(span,type,details);
	}

	public void declareType(String type) {
		monotonicEnv.declareType(type);
	}
	
}

