package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Edoardo Airldi
 * Date: Nov 21, 2003
 */

public class FilterInstanceTransform implements FeatureFilter {
    // example: discard fetures that contain punctuation
    static final String PATTERN = "[\\W\\d]+";

    /** Should I retain feature f in the given instance. */
    @Override
		public boolean match(Feature f, Instance instance){
        boolean retain = false;
        Pattern p = Pattern.compile(PATTERN);
        Matcher m = p.matcher( f.toString() );
        if ( !m.find() ) {
            retain = true;
        }
        return retain;
    }

}
