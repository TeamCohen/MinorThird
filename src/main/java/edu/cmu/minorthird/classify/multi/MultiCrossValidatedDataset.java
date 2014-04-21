package edu.cmu.minorthird.classify.multi;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.CrossValidatedDataset;
import edu.cmu.minorthird.classify.transform.AbstractInstanceTransform;
import edu.cmu.minorthird.classify.transform.PredictedClassTransform;
import edu.cmu.minorthird.classify.transform.TransformingMultiClassifier;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/** 
 * View result of some sort of train/test experiment for Data with Multiple Labels.
 *
 * @author Cameron Williams
 */

public class MultiCrossValidatedDataset implements Visible{

	static private Logger log=Logger.getLogger(CrossValidatedDataset.class);

	private MultiClassifiedDataset[] cds;

	private MultiClassifiedDataset[] trainCds;

	private MultiEvaluation v;

	public MultiCrossValidatedDataset(ClassifierLearner learner,MultiDataset d,
			Splitter<MultiExample> splitter){
		this(learner,d,splitter,false,false);
	}

	public MultiCrossValidatedDataset(ClassifierLearner learner,MultiDataset d,
			Splitter<MultiExample> splitter,boolean saveTrainPartitions){
		this(learner,d,splitter,saveTrainPartitions,false);
	}

	public MultiCrossValidatedDataset(ClassifierLearner learner,MultiDataset d,
			Splitter<MultiExample> splitter,boolean saveTrainPartitions,boolean cross){
		MultiDataset.MultiSplit s=d.MultiSplit(splitter);
		cds=new MultiClassifiedDataset[s.getNumPartitions()];
		trainCds=
				saveTrainPartitions?new MultiClassifiedDataset[s.getNumPartitions()]
						:null;
		v=new MultiEvaluation(d.getMultiSchema());
		ProgressCounter pc=
				new ProgressCounter("train/test","fold",s.getNumPartitions());
		for(int k=0;k<s.getNumPartitions();k++){
			MultiDataset trainData=s.getTrain(k);
			if(cross)
				trainData=trainData.annotateData();
			MultiDataset testData=s.getTest(k);
			log.info("splitting with "+splitter+", preparing to train on "+
					trainData.size()+" and test on "+testData.size());
			MultiClassifier c=
					new MultiDatasetClassifierTeacher(trainData).train(learner);
			//if(cross) testData=testData.annotateData(c);
			if(cross){
				AbstractInstanceTransform transformer=new PredictedClassTransform(c);
				c=new TransformingMultiClassifier(c,transformer);
			}
			MultiDatasetIndex testIndex=new MultiDatasetIndex(testData);
			cds[k]=new MultiClassifiedDataset(c,testData,testIndex);
			if(trainCds!=null)
				trainCds[k]=new MultiClassifiedDataset(c,trainData,testIndex);
			v.extend(c,testData);
			//v.setProperty("classesInFold"+(k+1), "train: "+classDistributionString(trainData.getSchema(),new MultiDatasetIndex(trainData))
			//							+"     test: "+classDistributionString(testData.getSchema(),testIndex));
			log.info("splitting with "+splitter+", stored classified dataset");
			pc.progress();
		}
		pc.finished();
	}

//	private String classDistributionString(MultiExampleSchema multiSchema,
//			MultiDatasetIndex index){
//		StringBuffer buf=new StringBuffer("");
//		java.text.DecimalFormat fmt=new java.text.DecimalFormat("#####");
//		ExampleSchema[] schemas=multiSchema.getSchemas();
//		for(int x=0;x<schemas.length;x++){
//			ExampleSchema schema=schemas[x];
//			for(int i=0;i<schema.getNumberOfClasses();i++){
//				if(buf.length()>0)
//					buf.append("; ");
//				String label=schema.getClassName(i);
//				buf.append(fmt.format(index.size(label))+" "+label);
//			}
//		}
//		return buf.toString();
//	}

	@Override
	public Viewer toGUI(){
		ParallelViewer main=new ParallelViewer();
		for(int i=0;i<cds.length;i++){
			final int k=i;
			System.out.println(i);
			main.addSubView("Test Partition "+(i+1),new TransformedViewer(cds[0]
					.toGUI()){
				static final long serialVersionUID=20080130L;
				@Override
				public Object transform(Object o){
//					MultiCrossValidatedDataset cvd=(MultiCrossValidatedDataset)o;
					return cds[k];
				}
			});
		}
		if(trainCds!=null){
			for(int i=0;i<trainCds.length;i++){
				final int k=i;
				main.addSubView("Train Partition "+(i+1),new TransformedViewer(cds[0]
						.toGUI()){
					static final long serialVersionUID=20080130L;
					@Override
					public Object transform(Object o){
//						MultiCrossValidatedDataset cvd=(MultiCrossValidatedDataset)o;
						return trainCds[k];
					}
				});
			}
		}
		main.addSubView("Overall Evaluation",new TransformedViewer(v.toGUI()){
			static final long serialVersionUID=20080130L;
			@Override
			public Object transform(Object o){
				MultiCrossValidatedDataset cvd=(MultiCrossValidatedDataset)o;
				return cvd.v;
			}
		});
		main.setContent(this);
		return main;
	}

	public MultiEvaluation getEvaluation(){
		return v;
	}

	public static void main(String[] args){
		System.out.println("CrossValidatedDataset");
	}

}
