import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;

import java.util.*;
import java.io.*;

/** A simple extraction-oriented feature extractor to apply to one-token spans, for extraction tasks. 
 */

public class NameFE implements SpanFeatureExtractor,Serializable
{
	private int windowSize=5;
	private boolean useCharType=true;
	private boolean useCompressedCharType=true;
	private boolean useEqOnNonAnchors=false;
	private String[] tokenPropertyFeatures=new String[0];
	
	//
	// getters and setters
	//
	
	public int getWindowSize() { return windowSize; }
	public void setWindowSize(int n) { windowSize=n; }
	
	public boolean getUseCharType() { return useCharType; } 
	public void setUseCharType(boolean flag) { useCharType=flag; } 
	
	public boolean getUseEqOnNonAnchors() { return useEqOnNonAnchors; }
	public void setUseEqOnNonAnchors(boolean flag) { useEqOnNonAnchors=flag; }

	public boolean getUseCompressedCharType() { return useCompressedCharType; } 
	public void setUseCompressedCharType(boolean flag) { useCompressedCharType=flag; } 
	
	public String getTokenPropertyFeatures() { return StringUtil.toString(tokenPropertyFeatures); }
	public void setTokenPropertyFeatures(String commaSeparatedTokenPropertyList) { 
		tokenPropertyFeatures = commaSeparatedTokenPropertyList.split(",\\s*");
		//System.out.println("input: "+commaSeparatedTokenPropertyList);
		//System.out.println("tokenPropertyFeatures: "+StringUtil.toString(tokenPropertyFeatures));
	}
	public void setTokenPropertyFeatures(Set propertySet) { 
		tokenPropertyFeatures = (String[])propertySet.toArray(new String[propertySet.size()]);
	}
	
	public Instance extractInstance(Span s)	
	{
		return extractInstance(new EmptyEnv(), s);
	}

	public Instance extractInstance(TextEnv env, Span s)
	{
		// need to run the nameFeatures.mixup file
		if (!env.isAnnotatedBy("nameFeatures_v2")) {
			System.out.println("env needs nameFeatures_v2 annotation - nameFE");
			Dependencies.runDependency((MonotonicTextEnv)env, "nameFeatures_v2", "nameFeatures.mixup");
		}

		FeatureBuffer buf = new FeatureBuffer(env,s);
		if (useEqOnNonAnchors) {
			SpanFE.from(s,buf).tokens().eq().lc().emit();
		} else {
			SpanFE.from(s,buf).tokens().hasProp("anchor").eq().lc().emit();
		}
		if (useCompressedCharType) {
			SpanFE.from(s,buf).tokens().eq().charTypePattern().emit();
		}
		if (useCharType) {
			SpanFE.from(s,buf).tokens().eq().charTypes().emit();
		}
		for (int j=0; j<tokenPropertyFeatures.length; j++) {
			SpanFE.from(s,buf).tokens().prop(tokenPropertyFeatures[j]).emit();
		}
		for (int i=0; i<windowSize; i++) {
			if (useEqOnNonAnchors) {
				SpanFE.from(s,buf).left().token(-i-1).eq().lc().emit();
				SpanFE.from(s,buf).right().token(i).eq().lc().emit();
			} else {
				SpanFE.from(s,buf).left().token(-i-1).hasProp("anchor").eq().lc().emit();
				SpanFE.from(s,buf).right().token(i).hasProp("anchor").eq().lc().emit();
			}
			for (int j=0; j<tokenPropertyFeatures.length; j++) {
				SpanFE.from(s,buf).left().token(i-1).prop(tokenPropertyFeatures[j]).emit();
			}
			for (int j=0; j<tokenPropertyFeatures.length; j++) {
				SpanFE.from(s,buf).right().token(i).prop(tokenPropertyFeatures[j]).emit();
			}
			if (useCompressedCharType) {
				SpanFE.from(s,buf).left().token(-i-1).eq().charTypePattern().emit();
			}
			if (useCharType) {
				SpanFE.from(s,buf).right().token(i).eq().charTypes().emit();
			}
		}
		return buf.getInstance();
	}
}

