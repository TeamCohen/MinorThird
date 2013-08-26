package iitb.MaxentClassifier;
import iitb.CRF.CRF;
import iitb.Utils.Options;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
/**
 *
 * This class shows how to use the CRF package iitb.CRF for basic maxent 
 * classification where the features are provided as attributes of the 
 * instances to be classified. The number of classes can be more than two.
 *
 * @author Sunita Sarawagi
 *
 */ 


public class MaxentClassifier {
    protected FeatureGenRecord featureGen;
    protected CRF crfModel;
    protected DataDesc dataDesc;
    protected Options opts;
    public MaxentClassifier(Options opts) throws Exception {
	dataDesc = new DataDesc(opts);
    this.opts = opts;
	// read all parameters
	featureGen = new FeatureGenRecord(dataDesc.numColumns, dataDesc.numLabels);
    featureGen.addBias=1;
    if (opts.getProperty("class-prior")!=null)
        featureGen.addBias = opts.getInt("class-prior");
    }
    protected void train(String trainFile) throws IOException {
        train(FileData.read(trainFile,dataDesc));
    }
    public void train(Vector trainRecs) {
        crfModel = new CRF(dataDesc.numLabels,featureGen,opts);
        // read training data from the  given file.
            double params[] = crfModel.train(new DataSet(trainRecs));
            System.out.println("Trained model");
            for (int i = 0; i < params.length; i++)
                System.out.println(featureGen.featureName(i) + " " + params[i]);
    }
    void test(String testFile)  throws IOException {
        FileData fData = new FileData();
        fData.openForRead(testFile,dataDesc);
        test(fData.iterator(),false);
    }
    public void test(Iterator<DataRecord> dataIter, boolean testOnly)  throws IOException {
	int confMat[][] = new int[dataDesc.numLabels][dataDesc.numLabels];
	while (dataIter.hasNext()) {
        DataRecord dataRecord = (DataRecord) dataIter.next();
	    int trueLabel = dataRecord.y();
	    crfModel.apply(dataRecord);
	    //	    System.out.println(trueLabel + " true:pred " + dataRecord.y());
	    confMat[trueLabel][dataRecord.y()]++;
        if (testOnly) dataRecord.set_y(0, trueLabel);
	}
	// output confusion matrix etc directly.
	System.out.println("Confusion matrix ");
	for(int i=0 ; i<dataDesc.numLabels ; i++) {
	    System.out.print(i);
	    for(int j=0 ; j<dataDesc.numLabels ; j++) {
		System.out.print("\t"+confMat[i][j]);
	    }
	    System.out.println();
	}
    }
    public static void main(String args[]) {
	try {
	    Options opts = new Options(args);
	    MaxentClassifier maxent = new MaxentClassifier(opts);
	    maxent.train(opts.getMandatoryProperty("trainFile"));
	    System.out.println("Finished training...Starting test");
	    maxent.test(opts.getMandatoryProperty("testFile"));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
};
