/*
 * Created on Feb 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package iitb.Model;

import java.util.regex.Pattern;

import iitb.CRF.DataSequence;

/**
 * @author imran
 *
 */
public class RegexCountFeatures extends FeatureTypes {

    String patternString[][] = {
    		{"isInitCapitalWord",     		"[A-Z][a-z]+"        },
    		{"isAllCapitalWord",      		"[A-Z][A-Z]+"                },
    		{"isAllSmallCase",      	"[a-z]+"                },
    		//{"isWord",           		"[a-zA-Z][a-zA-Z]+"     },
    		//{"isAlphaNumeric",      	"[a-zA-Z0-9]+"          },
    		{"singleCapLetter",  		"[A-Z]"  				},
            {"containsDashes",          ".*--.*"},
            {"containsDash",            ".*\\-.*"       },
    		//{"singlePunctuation", 		"\\p{Punct}"			},
    		{"singleDot", 				"[.]"			},
    		{"singleComma", 			"[,]"			},
            {"singleQuote",             "[']"           },
            {"isSpecialCharacter",      "[#;:\\-/<>'\"()&]"},
            {"fourDigits",                "\\d\\d\\d\\d"          },
            {"isDigits",                "\\d+"          },
    		{"containsDigit", 			".*\\d+.*"		},
            {"endsWithDot",             "\\p{Alnum}+\\."        }
    	};
    Pattern p[];
	int patternOccurence[], index, maxSegmentLength;
    /**
     * @param m
     */
    public RegexCountFeatures(FeatureGenImpl m, int maxSegmentLength) {
        this(m,maxSegmentLength,null);
    }
    public RegexCountFeatures(FeatureGenImpl m, int maxSegmentLength, String patternFile) {
        super(m);
        this.maxSegmentLength = maxSegmentLength;
        if ((patternFile != null)&& (patternFile.length()>0)) 
            patternString = ConcatRegexFeatures.getPatterns(patternFile);
        assert(patternString != null);
        p = new Pattern[patternString.length];
        for(int i = 0; i < patternString.length; i++)
            p[i] = Pattern.compile(patternString[i][1]);
        patternOccurence = new int[patternString.length];
        
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {        
        int i, j;
		for(j = 0; j < patternOccurence.length; j++)
		    patternOccurence[j] = 0;
		for(i = prevPos + 1; i <= pos; i++){
		    for(j = 0; j < p.length; j++){
		        if(p[j].matcher(data.x(i).toString()).matches())
		            patternOccurence[j]++;
		    }
		}
		index = -1;
        return advance();
    }

    protected boolean advance() {        
        while(++index < (patternOccurence.length) && patternOccurence[index] <= 0);
        return index < patternOccurence.length;
    }

    public boolean hasNext() {
        return index < patternOccurence.length;
    }

    public void next(FeatureImpl f) {
		f.val = 1;
		patternOccurence[index] = Math.min(maxSegmentLength,patternOccurence[index]);
		f.strId.id = maxSegmentLength * (index+1) + patternOccurence[index];
		f.ystart = -1;
		if(featureCollectMode()){
			f.strId.name = patternString[index][0] + "_Count_" + patternOccurence[index];
			//System.out.println((String)f.strId.name +" " +index + " " + f.strId.id);
		}
    	advance();
    }

    @Override
    public int labelIndependentId(FeatureImpl f) {
        return f.id;
    }

    @Override
    public int maxFeatureId() {
        return maxSegmentLength*(patternString.length+1);
    }

    @Override
    public String name() {
        return "RC";
    }
}
