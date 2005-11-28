package edu.cmu.minorthird.ui;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.sequential.*;

/** 
 * Defines the list of classes that can be selected by an instance of UIMain. 
 */

/*package*/ class advancedParameters
{
    private static String rk = new String("repositoryKey");
			
    public static final String[] NAMES = new String[]
	{
	    //
	    // bunches of parameters
	    //
	    "repositoryKey", "testKey", /*"candidateType", "spanProp",*/ "showTestDetails", "embeddedAnnotators", "featureExtractor", "mixup",
	    "showData",

	    // advanced Help Buttons
	    /*"spanPropHelp", "candidateTypeHelp",*/ "showDataHelp", "embeddedAnnotatorsHelp", "feHelp", "mixupHelp", "showTestDetailsHelp",
	    "trainTestClassifierHelp", "featureExtractorHelp"
	};
}
