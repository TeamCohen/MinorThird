import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.OnlineClassifierLearner;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.util.StringUtil;

/**
 * Sample code for using an online learners.
 */

public class OnlineDemo 
{
    public static void main(String[] args) throws IOException, FileNotFoundException
    {
        if (args.length<1) {
            System.out.println("usage: inputFile");
            System.out.println();
            System.out.println("input file has one example per line");
            System.out.println("sample lines:");
            System.out.println("+1 bright red:2 sports car");
            System.out.println("-1 ugly:2 old car");
            System.out.println("+1 little red:3 ugly:-10 dinner dress:2");
        }

        // create a learner
        OnlineClassifierLearner learner = new VotedPerceptron();

        // forget any previous examples - this is not needed for a fresh learner
        learner.reset(); 
        
        // read the input file line by line
	LineNumberReader in = new LineNumberReader(new FileReader(new File(args[0])));
	String line;

	while ((line = in.readLine())!=null) {

            // split the line into white space
            String[] words = line.split("\\s+");
            if (words.length==0) continue;

            // the first word should be the class: +1 or -1
            // turn that word into a number
            double score = StringUtil.atof( words[0] );

            // turn the number into a ClassLabel object
            ClassLabel actualClass = ClassLabel.binaryLabel( score );
            
            // create an instance from the remaining words
            MutableInstance instance = new MutableInstance(words);
            for (int i=1; i<words.length; i++) {
                if (words[i].indexOf(':')>=0) {  // weighted feature eg "weight:180" 
                    String[] pair = words[i].split(":");
                    String featureName = pair[0];
                    double featureValue = StringUtil.atoi(pair[1]);
                    instance.addNumeric( new Feature(featureName), featureValue );
                } else {
                    String featureName = words[i]; // binary feature eg red - same as weight of 1.0
                    instance.addBinary( new Feature(featureName) );
                }
            }

            // get the current classifier of the learner
            Classifier classifier = learner.getClassifier();

            // predict using the current classifier on this instance
            ClassLabel predictedClass = classifier.classification( instance );
            double predictedClassAsNumber = predictedClass.numericLabel();

            // print the result
            String summary = predictedClass.isCorrect(actualClass) ? "GOOD " : "WRONG";
            System.out.println(summary+ " prediction: "+predictedClass+"="+predictedClassAsNumber+" on: "+instance); 

            // train on this example
            learner.addExample( new Example(instance, actualClass) );
        }
    }
}

