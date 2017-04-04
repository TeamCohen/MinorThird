package edu.cmu.minorthird.text.learn;

import java.util.Set;

import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;

/**
 * This class...
 * @author ksteppe
 */
public class TestTextLabels extends BasicTextLabels{

	static final long serialVersionUID=20080609L;

	public TestTextLabels(TextBase textBase){
		super(textBase);
	}

	// get the set of spans with a given type in the given document
	public Set<Span> getTypeSet(String type,String documentId){
		return super.getTypeSet(type,documentId);
	}
	
}
