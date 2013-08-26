package iitb.Model;
import iitb.CRF.DataSequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * ConcatRegexFeatures generates features by matching the token with the character patterns.
 * Character patterns are regular expressions for checking whether the token is capitalized word, 
 * a number, small case word, whether the token contains any special characters and like.
 * It uses regular expression to match a sequence of character pattern and generates features 
 * accordingly.
 * <P> 
 * The feature generated here is whether a sequence of tokens has a particular sequence of given pattern or not.
 * For example, if a pattern is to mathc a capital word, then for two token context window, various features 
 * generated are weither two token (bigram) sequence is having any of the following pattern or not: 
 * 	(1) Capital, Capital 
 *	(2) Capital, Non-Capital 
 *	(3) Non-capital, Capital.
 *
 * You can use any window around the current token (segment) for creating regular expression based features.
 * Also, you can define your own patterns, by writing down the regular expression in a file, 
 * whose format is specified below.
 * </p>
 * <p> 
 * The object of this class should be wrap around {@link FeatureTypesEachLabel} as follows:
 * <pre>
 *	 new FeatureTypesEachLabel(model, new ConcreteConcatRegexFeatures(model,relSegmentStart, relSegmentEnd, maxMemory, patternFile));
 * </pre>
 * </p>
 * A token in a token sequence has a index relative to the current token index, which is described below:
 * <pre>
 	x0 x1 x2 x3 x4 x5 x6 x7 .... xn
	-4 -3 -2 -1 0  0  0  1 2 ...  
 * </pre>
 * <p>
 * In above example, the current segment is from postion 4 to 6 with value of pos = 6 and prevPos = 3 in 
 * startScanFeaturesAt() call of FeatureGenerator.
 * You can refer to any of the token relative to current position by using the index below the token sequence.
 * Thus, you can create a pattern concat features for any token sequence in the neighbourhood of the current token, 
 * using relSegmentStart and relSegmentEnd.
 * For, example to create pattern for two tokens to the left of the current token, following is the parameters 
 * to be passed to the constructor of the class:
 * </p>
 * <pre>
 *  	new FeatureTypesEachLabel(model, new ConcreteConcatRegexFeatures(model,-2, -1, maxMemory, patternFile));
 * </pre>
 * 
 * @author 	Imran Mansuri
 * @since 1.2
 * @version 1.3
 */
 
public class ConcatRegexFeatures extends FeatureTypes {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4246100603296345601L;

	/**
	 *      Various patterns are defined here.
	 *      First dimension of this two dimensional array is feature name and second value is the
	 *      regular expression pattern to be matched against a token. You can add your own patterns
	 *      in this array.
	 */
	String patternString[][] = {
	    {"singleCapLetterWithDot",  "[A-Z]\\."  			},
		{"singleCapLetter",  		"[A-Z]"  				},
        {"isInitCapital",           "[A-Z][a-z]+"        },
        {"isAllCapital",            "[A-Z]+"                },
        {"isAllSmallCase",          "[a-z]+"                },
        
		{"singleDot", 		"[.]"			},
		{"singleComma", 		"[,]"			},
        {"singleQuote",         "[,]"           },
		{"isSpecialCharacter",		"[#;:\\-/<>'\"()&]"},
        
        //{"isWord",                  "[a-zA-Z][a-zA-Z]+"     },
		//{"isAlpha",           		"[a-zA-Z]+"             },
		//{"isAlphaNumeric",      	"[a-zA-Z0-9]+"          },

        {"singleDigit", 				"\\s*\\d\\s*"					},
		{"twoDigits", 				"\\s*\\d{2}\\s*"					},
		{"threeDigits", 				"\\s*\\d{3}\\s*"					},
		{"fourDigits", 				"\\s*\\(*\\d{4}\\)*\\s*"	},
        {"isDigits",                "\\d+"                  },
        {"containsDigit",           ".*\\d+.*"              },              
		{"isNumberRange", 			"\\d+\\s*([-]{1,2}\\s*\\d+)?"},
        
        {"endsWithDot",             "\\p{Alnum}+\\."        },
        {"endsWithComma",           "\\w+[,]"              },
        {"endsWithPunctuation",     "\\w+[;:,.?!]"          },
        {"singlePunctuation",       "\\p{Punct}"            },
        {"singleAmp",       "[&]"           },
        
		{"isDashSeparatedWords", 		"(\\w[-])+\\w"},
		{"isDashSeparatedSeq", 			"((\\p{Alpha}+|\\p{Digit}+)[-])+(\\p{Alpha}+|\\p{Digit}+)"},		
		{"isURL", 					"\\p{Alpha}+://(\\w+\\.)\\w+(:(\\d{2}|\\d{4}))?(/\\w+)*(/|(/\\w+\\.\\w+))?"	},
		{"isEmailId", 				"\\w+@(\\w+\\.)+\\w+"	},
		{"containsDashes",			".*--.*"},
        {"containsSpecialCharacters",".*[#;:\\-/<>'\"()&].*"},
	};

	Pattern p[];
	transient protected DataSequence data;
	protected int index, idbase, curId, window;
	protected int relSegmentStart, relSegmentEnd;
	protected int maxMemory;
	protected int left, right;
    /**
     * @param relSegmentStart2
     * @param relSegmentEnd2
     * @return
     */
    private int getWindowSize(int relSegmentStart, int relSegmentEnd) {
        if((sign(relSegmentEnd) == sign(relSegmentStart)) && relSegmentStart != 0)
            return relSegmentEnd - relSegmentStart + 1;
        else
            return relSegmentEnd - relSegmentStart + maxMemory;
    }

	/**
	 * Constructs an object of ConcatRegexFeatures to be used to generate features for the token 
	 * sequence as specified.
	 * You can specify the sequence of tokens on which the pattern has to be applied using relSegmentStart 
	 * and relSegmentEnd, which denotes segment boundries.
	 * The maxMemory denotes the maximum segment size, for normal CRF the value of maxMemory is 1.
	 * There are certain default patterns defined in the class. You can specify your own pattern in a file, and pass
	 * the name of the file in this constructor. The file should begin with integer value for number of pattern in the 
	 * file. This should be follwoed by one pattern definition on each line. The first word is the name of the pattern
	 * and second word is regular expression for the pattern.
	 *
	 * @param fgen			a {@link Model} object
	 * @param relSegmentStart	index of the reltive position for left boundary
	 * @param relSegmentEnd		index of the reltive position for right boundary
	 * @param maxMemory		maximum size of a segment
	 * @param patternFile		file which contains the pattern definition
	 */
	public ConcatRegexFeatures(FeatureGenImpl fgen, int relSegmentStart, int relSegmentEnd, int maxMemory, String patternFile){
		super(fgen);
        
		assert(relSegmentEnd >= relSegmentStart);
		this.relSegmentStart = relSegmentStart;
		this.relSegmentEnd = relSegmentEnd;
		this.maxMemory = maxMemory;
		
		window = getWindowSize(relSegmentStart, relSegmentEnd);		
		idbase = (int) Math.pow(2, window-1);
        if ((patternFile != null) && (patternFile.length()>0))
            patternString = getPatterns(patternFile);
		assert(patternString != null);
		p = new Pattern[patternString.length];
		for(int i = 0; i < patternString.length; i++){
			//System.out.println("i"+ i +" " + patternString[i][1]);
			p[i] = Pattern.compile(patternString[i][1]);

		}
		cache=true;
	}
	
    /**
	 * Constructs an object of ConcatRegexFeatures to be used to generate features for current token.
	 
	 * @param m		a {@link Model} object
	 * @param relSegmentStart	index of the reltive position for left boundary
	 * @param relSegmentEnd		index of the reltive position for right boundary
	 * @param maxMemory		maximum size of a segment
	 */
	public ConcatRegexFeatures(FeatureGenImpl m, int relSegmentStart, int relSegmentEnd, int maxMemory){	  
        this(m,relSegmentStart,relSegmentEnd,maxMemory,null);
	}

	/**
	 * Constructs an object of ConcatRegexFeatures to be used to generate features for current token.
	 
	 * @param m			a {@link Model} object
	 * @param relSegmentStart	index of the reltive position for left boundary
	 * @param relSegmentEnd		index of the reltive position for right boundary
	 */
	public ConcatRegexFeatures(FeatureGenImpl m, int relSegmentStart, int relSegmentEnd){
		this(m, relSegmentStart, relSegmentEnd, 1);
	}

    public ConcatRegexFeatures(FeatureGenImpl m){
        this(m, 0,0,1);
    }
    public ConcatRegexFeatures(FeatureGenImpl m, String patternFile){
        this(m, 0,0,1,patternFile);
    }
	/**
	 * Constructs an object of ConcatRegexFeatures to be used to generate features for current token.
	 
	 * @param m			a {@link Model} object
	 * @param relSegmentStart	index of the reltive position for left boundary
	 * @param relSegmentEnd		index of the reltive position for right boundary
	 * @param patternFile		file which contains the pattern definition
	 */
	public ConcatRegexFeatures(FeatureGenImpl m, int relSegmentStart, int relSegmentEnd, String patternFile){
		this(m, relSegmentStart, relSegmentEnd, 1, patternFile);
	}
	private int sign(int boundary){
		if(boundary == 0)
			return 0;
		else if(boundary < 0)
			return -1;
		else
			return 1;
	}

	/**
	 * Reads patterns to be matched from the file.
	 * The format of the file is as follows:
	 * The first line of the file is number of patterns, followed by a list of patterns one per line.
	 * Each line describes a pattern's name and pattern string itself.
	 *
	 * @param patternFile		name of the pattern file
	 */
	public static String[][] getPatterns(String patternFile){
		String line;
		String patterns[][];
		try {
			BufferedReader in = new BufferedReader(new FileReader(patternFile));
			int len = Integer.parseInt(in.readLine());
			patterns = new String[len][2];

			for(int k = 0; k < len; k++){
				StringTokenizer strTokenizer = new StringTokenizer(in.readLine());
				patterns[k][0] = strTokenizer.nextToken();
				patterns[k][1] = strTokenizer.nextToken();
				//System.out.println(patterns[k][0] + " " + patterns[k][1]);
			}
		}catch(IOException ioe){
			System.err.println("Could not read pattern file : " + patternFile);
			ioe.printStackTrace();
			return null;
		}

		return patterns;
	}

	/**
	 * Initaites scanning of features in a sequence at specified position. 
	 *
	 * @param data		a training sequence 
	 * @param prevPos	the previous label postion
	 * @param pos		Current token postion
	 */
	public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos){
		assert(patternString != null);
		this.data = data;
		index = 0;
		if (relSegmentStart <= 0) {
			left = prevPos + 1 + relSegmentStart;
		} else {
			left = pos + relSegmentStart;
		}

		if (relSegmentEnd < 0) {
			right = prevPos + 1 + relSegmentEnd;
		} else {
			right = pos + relSegmentEnd;
		}

		if(!(left >= 0 && left < data.length() && right >= 0 && right < data.length()))
			index = patternString.length;

		//System.out.println("DataLength:" + data.length() + " segment(" + (prevPos+1) + "," + pos + ") rs(" +relSegmentStart + "," + relSegmentEnd + ") window(" + left + "," + right + ") idbase:" + idbase);
		advance();

		return true;
	}
	
	/**
	 * Returns true if there are any more feature(s) for the current scan.
	 *
	 */
	public boolean hasNext() {
		return index < patternString.length;
	}

	/**
	 * Generates the next feature for the current scan.
	 *
	 * @param f	Copies the feature generated to the argument 
	 */
	public void next(FeatureImpl f) {

		if(featureCollectMode()){
			//This is a feature collection mode, so return id and name
			f.strId.name = "R_" + patternString[index][0];
			if ((window > 1) && (curId > 0)) {
			    f.strId.name =  f.strId.name + ("_" + window + "_" + Integer.toBinaryString(curId));
			}
		}
		
		/*//Return feature on token window
		int base = 1;
		f.strId.id = 0;
		for(int k = left; k <= right; k++){
			boolean match = p[index].matcher((String)data.x(k)).matches();	
			f.strId.id += base * (match? 1:0);
			base = base * 2;
		}
		f.val = (f.strId.id > 0) ? 1:0; //In case of no match return 0 as feature value 
		f.ystart = -1;
		f.strId.id += idbase * index++;*/

		f.val = 1;
		f.strId.id = curId + idbase * index++;
		f.ystart = -1;
		advance();
	}

	private void advance(){
		curId = 0;
		while(curId <= 0 && index < patternString.length){
			int base = 1;
			for(int k = left; k <= right; k++){
				boolean match = p[index].matcher(data.x(k).toString()).matches();	
				curId += base * (match? 1:0);
				base = base * 2;
			}
			if(curId > 0)
				break;				
			index++;
		}
	}

	public int maxFeatureId(){
	    return idbase * patternString.length; //(maximum base i.e. most significat bits + maximum offset)
	}
	int offsetLabelIndependentId(FeatureImpl f) {
	    return f.strId.id;
    }
};
