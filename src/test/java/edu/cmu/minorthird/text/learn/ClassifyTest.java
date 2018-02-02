package edu.cmu.minorthird.text.learn;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Level;

import edu.cmu.minorthird.classify.AbstractClassificationChecks;
import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;

/**
 * @author ksteppe
 */
public abstract class ClassifyTest extends AbstractClassificationChecks{

	/** file loading of data */
	protected String dataFile;

	protected String labelsFile;

	/** text base of training data */
	protected TextBase base;

	protected TextLabels labels;

	/** testing data */
	protected TextBase testBase;

	protected TextLabels testLabels;

	/** span checking */
	String documentId;

	protected String labelString;

	/** defaults for testing */
	protected final static SpanFeatureExtractor DEFAULT_SFE=
			edu.cmu.minorthird.text.learn.SampleFE.BAG_OF_WORDS;

	public ClassifyTest(String s){
		super(s);
		log.setLevel(Level.DEBUG);
	}

	/**
	 * classify with default features, learner, splitter check against given data
	 */
	public void classify(double[] referenceData){
		checkClassifyText(DEFAULT_SFE,DEFAULT_LEARNER,referenceData);
	}

	/** run default classification but output evaluation with no check */
	public void benchMarkClassify(){
		classify(null);
	}

	/**
	 * Base test for classification send null referenceData to get a print out
	 */
	public void checkClassifyText(SpanFeatureExtractor fe,
			ClassifierLearner learner,double[] referenceStats){
		try{
			Dataset trainData=createDataSet(base,labels,fe);
			Dataset testData=createDataSet(testBase,testLabels,fe);

			checkClassify(learner,trainData,testData,referenceStats);

		}catch(Exception e){
			log.fatal(e,e);
			fail();
		}
	}

	/**
	 * takes the text base, labels and a feature extractor produces a dataset
	 * 
	 * @param fe
	 * @return Dataset
	 */
	private Dataset createDataSet(TextBase base,TextLabels labels,
			edu.cmu.minorthird.text.learn.SpanFeatureExtractor fe){
		Dataset data=new BasicDataset();
		for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
			Span s=i.next();
			// System.out.println( labels );
			double label=getLabel(labels,s);
// log.info("label: " + s.getDocumentId() + " : is : " + label);
			data.add(new Example(fe.extractInstance(labels,s),ClassLabel
					.binaryLabel(label)));
		}
		return data;
	}

	/** extract labeling for the given span */
	protected double getLabel(TextLabels labels,Span s){
		double label=labels.hasType(s,labelString)?+1:-1;
		return label;
	}

	/** load labels from file */
	private void loadLabels() throws IOException{
// set up the labels
		labels=new TestTextLabels(base);
		new TextLabelsLoader().importOps((BasicTextLabels)labels,base,new File(
				labelsFile));
	}

	/**
	 * check the spans for the loaded labels The test is to ensure that the spans
	 * in the labels and the spans in the text base are the same, with no
	 * 'off-by-one' errors.
	 * 
	 * @throws java.io.IOException
	 */
	void checkSpans() throws IOException{
		loadLabels();

		Span baseSpan=base.documentSpan(documentId);
// log.info("baseSpan for " + documentId + " is " + baseSpan);
		log.info("span from "+baseSpan.getDocumentId()+" of size "+baseSpan.size());

		Set<String> typeSet=labels.getTypes();
		log.info(typeSet.toString());

		Span checkSpan=null;
		for(Iterator<String> iterator=typeSet.iterator();iterator.hasNext();){
			String typeName=iterator.next();

// log.info("**************** TYPES: " + typeName + " ********************");
			// now get all the stuff with that type
			for(Iterator<Span> it=base.documentSpanIterator();it.hasNext();){
				String id=it.next().getDocumentId();
				Set<Span> spanSet=((TestTextLabels)labels).getTypeSet(typeName,id);
				for(Iterator<Span> spanIt=spanSet.iterator();spanIt.hasNext();){
					Span span=spanIt.next();
					if(id.equals(documentId)){
						log.info("    Document ID: "+id);
						log.info("        span: "+span.getTextToken(0).asString()+":"+
								span.getTextToken(span.size()-1)+" size: "+span.size());
						checkSpan=span;
					}
				} // spanIt
			} // it
		} // iterator

		for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
			Span s=i.next();
			if(s.getDocumentId().equals(documentId)){
				log.info("        span: "+s.getTextToken(0).asString()+":"+
						s.getTextToken(s.size()-1)+" size: "+s.size());
				log.info("        checkSpan: "+checkSpan.getTextToken(0).asString()+
						":"+checkSpan.getTextToken(checkSpan.size()-1)+" size: "+
						checkSpan.size());
				log.info(new Boolean(checkSpan.equals(s)));
				assertEquals(checkSpan.size(),s.size());
				assertEquals(checkSpan,s);
			}
		}
	}

}
