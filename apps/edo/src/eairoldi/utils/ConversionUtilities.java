package eairoldi.utils;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Edoardo Airoldi
 * Date: Dec 20, 2004
 */

public class ConversionUtilities
{
    static private Logger log = Logger.getLogger(ConversionUtilities.class);

    /** Converts Dataset from Minorthird into LDA format */
    static public void saveLdaFile(Dataset data, String filename, boolean makeBinary)
    {
        if (makeBinary)
        {
            System.out.println("Transforming :: Counts to 0/1");
            MakeBinaryTransform filter = new MakeBinaryTransform();
            InstanceTransform t = filter.batchTrain(data);
            data = t.transform(data);
        }

        BasicFeatureIndex fidx = new BasicFeatureIndex( data );
        try
        {
            System.out.println("Printing :: Data to file");
            File outFILE = new File(filename+".data-lda.txt");
            File wordFILE = new File(filename+".words-lda.txt");
            PrintStream out = new PrintStream(new FileOutputStream(outFILE));
            PrintStream word = new PrintStream(new FileOutputStream(wordFILE));

            int len = fidx.featureIterator().estimatedSize();
            System.out.print("Total words :: "+len);
            ArrayList num2word = new ArrayList();
            for (Feature.Looper fl=fidx.featureIterator(); fl.hasNext();)
            {
                Feature ft = fl.nextFeature();
                num2word.add(ft);
                int idx = num2word.indexOf(ft);
                word.println( idx+" "+ft );
            }

            for (Example.Looper el=data.iterator(); el.hasNext();)
            {
                Example ex = el.nextExample();

                len = ex.featureIterator().estimatedSize();
                StringBuffer line = new StringBuffer(len+"");

                for (Feature.Looper fl=ex.featureIterator(); fl.hasNext(); )
                {
                    Feature ft = fl.nextFeature();
                    int idx = num2word.indexOf(ft);
                    int wgt = (int) ex.getWeight( ft );
                    line.append(" "+idx+":"+wgt);
                }
                out.println( line );
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
            System.exit(1);
        }
    }

    /** Saves train/test versions of a Dataset in tetrad format */
    static public void saveTetradFile(Dataset data, String filename, boolean makeBinary)
    {
        Dataset[] d = new Dataset[]{data};
        String[] fn = new String[]{filename};
        saveTetradFile(d,fn,makeBinary);
    }

    /** Merges Dataset[], and then saves train/test versions in tetrad format */
    static public void saveTetradFile(Dataset[] data, String[] filename, boolean makeBinary)
    {
        if (makeBinary)
        {
            MakeBinaryTransform filter = new MakeBinaryTransform();
            for (int i=0; i<data.length; i++)
            {
                InstanceTransform t = filter.batchTrain(data[i]);
                data[i] = t.transform(data[i]);
            }
            System.out.println("Transform :: Counts to 0/1");
        }

        Dataset allData = ConversionUtilities.composeDatasets(data);

        ExampleSchema schema = allData.getSchema();
        int numberOfClasses = schema.getNumberOfClasses();
        //String[] classLabels = new String[numberOfClasses];
        //for (int i=0; i<numberOfClasses; i++) { classLabels[i]=schema.getClassName(i); }

        // train version
        BasicFeatureIndex fidx = new BasicFeatureIndex( allData );
        try
        {
            for (int k=0; k<data.length; k++)
            {
                System.out.println(filename[k]+".train.ttd");
                File outFILE = new File(filename[k]+".train.ttd");
                PrintStream out = new PrintStream(new FileOutputStream(outFILE));

                out.println("/discretevars");

                StringBuffer secondLine = new StringBuffer("ClassificationLabel:");
                for (int i=0; i<(numberOfClasses-1); i++) { secondLine.append(i+"="+i+","); }
                secondLine.append((numberOfClasses-1)+"="+(numberOfClasses-1));
                out.println(secondLine);

                int ftCnt=0;
                for (Feature.Looper fl=fidx.featureIterator(); fl.hasNext();)
                {
                    fl.nextFeature();
                    ftCnt++;
                    if (makeBinary) { out.println( " V"+ftCnt+":0=0,1=1" ); }
                    else { System.out.println("Not implemented yet!"); System.exit(1);}
                }

                out.println("/discretedata");

                StringBuffer line = new StringBuffer("ClassificationLabel");
                ftCnt=0;
                for (Feature.Looper fl=fidx.featureIterator(); fl.hasNext();)
                {
                    fl.nextFeature();
                    ftCnt++;
                    line.append(" V"+ftCnt);
                    //line.append(" "+fl.nextFeature());
                }
                out.println(line);

                ExampleSchema s = allData.getSchema();
                for (Example.Looper el=data[k].iterator(); el.hasNext();)
                {
                    Example ex = el.nextExample();
                    int label = s.getClassIndex( ex.getLabel().bestClassName() );
                    //System.out.println( ex.getLabel().bestClassName()+" "+label+" "+s.getClassIndex( ex.getLabel().bestClassName() ));
                    //System.out.println( ex.getSource()+", "+ex.getSubpopulationId() );
                    StringBuffer lineBuffer = new StringBuffer(""+label);
                    for (Feature.Looper i=fidx.featureIterator(); i.hasNext(); )
                    {
                        int wgt = (int) ex.getWeight( i.nextFeature() );
                        lineBuffer.append(" "+wgt);
                    }
                    out.println( lineBuffer );
                }
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
            System.exit(1);
        }

        // test version
        try
        {
            for (int k=0; k<data.length; k++)
            {
                System.out.println(filename[k]+".test.ttd");
                File outFILE = new File(filename[k]+".test.ttd");
                PrintStream out = new PrintStream(new FileOutputStream(outFILE));

                out.println("/discretedata");

                StringBuffer line = new StringBuffer("ClassificationLabel");
                int ftCnt = 0;
                for (Feature.Looper fl=fidx.featureIterator(); fl.hasNext();)
                {
                    fl.nextFeature();
                    ftCnt++;
                    line.append(" V"+ftCnt);
                    //line.append(" "+fl.nextFeature());
                }
                out.println(line);

                ExampleSchema s = allData.getSchema();
                for (Example.Looper el=data[k].iterator(); el.hasNext();)
                {
                    Example ex = el.nextExample();
                    int label = s.getClassIndex( ex.getLabel().bestClassName() );
                    //System.out.println( ex.getSource()+", "+ex.getSubpopulationId() );
                    StringBuffer lineBuffer = new StringBuffer(""+label);
                    for (Feature.Looper i=fidx.featureIterator(); i.hasNext(); )
                    {
                        int wgt = (int) ex.getWeight( i.nextFeature() );
                        lineBuffer.append(" "+wgt);
                    }
                    out.println( lineBuffer );
                }
            }

            // print Vs to Features to screen
            int ftCnt = 0;
            for (Feature.Looper fl=fidx.featureIterator(); fl.hasNext();)
            {
                Feature ft = fl.nextFeature();
                ftCnt++;
                System.out.println(" V"+ftCnt+" = "+ft);
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
            System.exit(1);
        }
    }

    /** Makes a Dataset by unioning those passed in as arguments */
    private static Dataset composeDatasets(Dataset[] data)
    {
        Dataset d = new BasicDataset();
        for (int i=0; i<data.length; i++)
        {
            BasicFeatureIndex fidx = new BasicFeatureIndex(data[i]);
            System.out.println( "Dataset n."+i+" :: examples="+data[i].size()+", features="+fidx.numberOfFeatures() );

            for (Example.Looper j=data[i].iterator(); j.hasNext();)
            {
                d.add( j.nextExample() );
            }
        }

        BasicFeatureIndex fidx = new BasicFeatureIndex(d);
        System.out.println( "Dataset all :: examples="+d.size()+", features="+fidx.numberOfFeatures() );

        return d;
    }


    //
    // Test Conversion Utilities
    //

    static public void main(String[] args)
    {
        String PATH = "C:\\Archive-Projects\\PNAS\\pnas\\plaintext\\";
        File fin = new File(PATH+"pnas.abs.data.3rd");
        Dataset din = new BasicDataset();

        try
        {
            din = DatasetLoader.loadFile(fin);
        }
        catch (Exception x)
        {
            log.error(x,x);
            System.exit(1);
        }

        String filename = PATH+"pnas.abs";
        ConversionUtilities.saveLdaFile(din,filename,false);

        /*try {
        Dataset d = DatasetLoader.loadFile( new File("/Users/eairoldi/cmu.research/8.Text.Learning.Group/src.MISC/movie-data.3rd") );

        System.out.println("applying frequency filter ...");
        OrderBasedTransformLearner f1 = new OrderBasedTransformLearner( "document" );
        InstanceTransform t1 = f1.batchTrain( d );
        //((OrderBasedInstanceTransform)t1).setNumberOfFeatures( 100 ); // 100 is default value
        d = t1.transform( d );

        System.out.println("applying binary filter ...");
        MakeBinaryTransform f2 = new MakeBinaryTransform();
        InstanceTransform t2 = f2.batchTrain(d);
        d = t2.transform(d);

        //System.out.println("binary data ::\n"+d);

        System.out.println("applying tfidf filter ...");
        TFIDFTransformLearner f3 = new TFIDFTransformLearner();
        InstanceTransform t3 = f3.batchTrain(d);
        d = t3.transform(d);

        System.out.println("tfidf data ::\n"+d);

        BasicFeatureIndex fi = new BasicFeatureIndex(d);
        System.out.println("#ft = "+fi.numberOfFeatures() );
        } catch (Exception x) {;}
        System.exit(0); */

        /*String PATH = "/Users/eairoldi/cmu.research/Xue/xue.ROY/";
        File fin = new File(PATH+"roy-data-fin.3rd");
        File ma = new File(PATH+"roy-data-ma.3rd");
        File mix = new File(PATH+"roy-data-mix.3rd");

        Dataset[] data = new Dataset[3];

        try
        {
        data[0] = DatasetLoader.loadFile(fin);
        data[1] = DatasetLoader.loadFile(ma);
        data[2] = DatasetLoader.loadFile(mix);
        }
        catch (Exception x)
        {
        log.error(x,x);
        System.exit(1);
        }

        PATH = PATH + "roy-cross-topic/";
        String[] filenames = new String[]{PATH+"Vs.roy-fin",PATH+"Vs.roy-ma",PATH+"Vs.roy-mix"};
        ConversionUtilities.saveTetradFile(data,filenames,true);*/

        //Dataset allData = Utilities.composeDatasets(data);

        // DEBUG
        /*String PATH = "/Users/eairoldi/cmu.research/8.Text.Learning.Group/src.MISC/";
        File d1 = new File(PATH+"data1.m3rd");
        File d2 = new File(PATH+"data2.m3rd");
        File d3 = new File(PATH+"data3.m3rd");

        Dataset[] data = new Dataset[3];

        try
        {
        data[0] = DatasetLoader.loadFile(d1);
        data[1] = DatasetLoader.loadFile(d2);
        data[2] = DatasetLoader.loadFile(d3);
        }
        catch (Exception x)
        {
        log.error(x,x);
        System.exit(1);
        }

        String[] filenames = new String[]{PATH+"data1-out",PATH+"data2-out",PATH+"data3-out"};
        Utilities.saveTetradFile(data,filenames,true);*/

    }

}
