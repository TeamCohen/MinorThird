package eairoldi.utils;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.FeatureBuffer;
import edu.cmu.minorthird.text.learn.SpanFE;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * User: Edoardo M. Airoldi  (eairoldi@cs.cmu.edu)
 * Date: Feb 16, 2005
 */

public class MakeData
{
    static private Logger log = Logger.getLogger(MakeData.class);
    static private String[] PnasTopics = new String[] {"AppliedBiologicalSciences", "SocialSciences",
                                    "Pharmacology", "Geophysics", "Psychology",
                                    "Astronomy", "EconomicSciences","AppliedMathematics",
                                    "PoliticalSciences", "Engineering", "Anthropology-BS",
                                    "U.S.FrontiersOfScienceSymposium",
                                    "ChineseamericanFrontiersOfScienceSymposium",
                                    "MedicalSciences", "Mathematics", "Immunology",
                                    "Microbiology", "Ecology",
                                    "German-americanFrontiersOfScienceSymposium",
                                    "ComputerSciences", "ResearchArticles", "AgriculturalSciences",
                                    "Neurobiology", "AppliedPhysicalSciences",
                                    "FrontiersOfScienceSymposium", "Anthropology",
                                    "Psychology-PS", "Psychology-BS", "Evolution",
                                    "PlantBiology", "Physiology", "Physics",
                                    "PopulationBiology", "Statistics", "CellBiology",
                                    "JapaneseamericanFrontiersOfScienceSymposium",
                                    "Biochemistry", "Geophyics", "Chemistry", "Biophysics",
                                    "Genetics", "DevelopmentalBiology", "Introduction","Geology"};

    // create dataset
    public static Dataset make(File dirFile, File envFile, File dataFile, File wordFile)
    {
        Dataset d = new BasicDataset();

        try {
            // load the documents and labels
            System.out.println("Load Texts");
            TextBaseLoader loader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, false);
            TextBase base = loader.load( dirFile );
            MutableTextLabels labels = new BasicTextLabels(base);
            new TextLabelsLoader().importOps(labels, base, envFile);

            // set up a simple bag-of-words feature extractor
            SpanFeatureExtractor fe = new SpanFeatureExtractor()
            {
                public Instance extractInstance(TextLabels labels, Span s) {
                    FeatureBuffer buf = new FeatureBuffer(labels, s);
                    SpanFE.from(s,buf).tokens().eq().lc().punk().emit();
                    return buf.getInstance();
                }
                public Instance extractInstance(Span s) {
                    return extractInstance(null,s);
                }
            };

            // Extract features and create a dataset
            System.out.println("Extract Features");
            for (Span.Looper i = base.documentSpanIterator(); i.hasNext();) {
                Span s = i.nextSpan();

                boolean found = false;
                for (int t=0; t<PnasTopics.length; t++) {
                    if ( labels.hasType(s,PnasTopics[t]) ) {
                        d.add(new Example(fe.extractInstance(labels,s), new ClassLabel(PnasTopics[t])));
                        // for binary :: label = +1 or -1;
                        // data.add(new BinaryExample(fe.extractInstance(labels,s), label));
                        found = true;
                    }
                }

                if (!found) {
                    System.out.println("error: missing label!");
                    System.out.print(s.toString());
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error(e, e);
            System.exit(1);
        }

        return d;
    }

    private static Dataset filterDataset(Dataset data, String f)
    {
        if (f.equals("T1"))
        {
            System.out.println("Filter Features with T1");
            T1InstanceTransformLearner filter = new T1InstanceTransformLearner();
            filter.setREF_LENGTH(660.0);
            //filter.setPDF("Negative-Binomial");
            T1InstanceTransform t1stat = (T1InstanceTransform)filter.batchTrain( data );
            t1stat.setALPHA(0.05);
            t1stat.setMIN_WORDS(50);  //t1stat.setMAX_WORDS(10000);
            t1stat.setSAMPLE(2500);
            data = t1stat.transform( data );
        }
        else if (f.equals("Freq"))
        {
            int minFreq =3;  String model = "document"; // or "document"
            System.out.println("Filter Features by Frequency");
            FrequencyBasedTransformLearner filter = new FrequencyBasedTransformLearner( minFreq,model );
            AbstractInstanceTransform ait = (AbstractInstanceTransform)filter.batchTrain( data );
            data = ait.transform( data );
        }
        else if (f.equals("Info-Gain"))
        {
            int featureToKeep = 10;  String model = "document"; // or "word"
            System.out.println("Filter Features with Info-Gain");
            InfoGainTransformLearner filter = new InfoGainTransformLearner( model );
            InfoGainInstanceTransform infoGain = (InfoGainInstanceTransform)filter.batchTrain( data );
            infoGain.setNumberOfFeatures( featureToKeep );
            data = infoGain.transform( data );
        }
        else if (f.equals("Top"))
        {
            int featureToKeep = 50000;  String model = "word"; // or "word"
            System.out.println("Filter Features with Top="+featureToKeep);
            OrderBasedTransformLearner filter = new OrderBasedTransformLearner( model );
            OrderBasedInstanceTransform infoGain = (OrderBasedInstanceTransform)filter.batchTrain( data );
            infoGain.setNumberOfFeatures( featureToKeep );
            data = infoGain.transform( data );
        }
        else
        {
            System.out.println("No Filter was used");
        }
        return data;
    }

    public static double[][] computeDisctance(Dataset data)
    {
        double[][] d = new double[ data.size() ][ data.size() ];
        int ii=0;
        int jj=0;

        for ( Example.Looper i=data.iterator(); i.hasNext(); )
        {
            System.out.println("ex = " +ii);
            Example exi = i.nextExample();
            for ( Example.Looper j=data.iterator(); j.hasNext(); )
            {
                Example exj = j.nextExample();
                d[ii][jj] = distance(exi,exj);
                jj = (jj+1)%data.size();
            }
            ii = (ii+1)%data.size();
        }

        return d;
    }

    public static double distance(Example exi, Example exj)
    {
        Instance ini = exi.asInstance();
        Instance inj = exj.asInstance();

        double num = 0.0;
        double denomi = 0.0;
        double denomj = 0.0;

        Set set = new TreeSet();
        for (Feature.Looper i=ini.featureIterator(); i.hasNext();)
        {
            Feature f = i.nextFeature();
            set.add(f);
        }
        for (Feature.Looper j=inj.featureIterator(); j.hasNext();)
        {
            Feature f = j.nextFeature();
            set.add(f);
        }
        Feature.Looper looper = new Feature.Looper( set.iterator() );

        for ( Feature.Looper i=looper; i.hasNext(); )
        {
            Feature f = i.nextFeature();

            double wi = 0.0;
            double wj = 0.0;

            try {  wi = exi.getWeight(f); }
            catch (Exception x) { ; }
            try {  wj = exj.getWeight(f); }
            catch (Exception x) { ; }

            num = num + wi*wj;
            denomi = denomi + Math.pow( wi,2 );
            denomj = denomj + Math.pow( wj,2 );
        }
        return num / (Math.sqrt(denomi) * Math.sqrt(denomj));
    }

    static public void main(String[] argv) {

        try {
            String path = "C:\\Archive-Projects\\PNAS\\pnas\\plaintext\\";
            File dirFile = new File(path+"abstracts");
            File envFile = new File(path+"plaintext.abs.env");
            File dataFile = new File(path+"pnas.abs.data.3rd");
            File wordFile = new File(path+"pnas.abs.words.txt");

            Dataset d = MakeData.make(dirFile,envFile,dataFile,wordFile);
            d = filterDataset(d,""); // Filter ca be "T1", "Freq", "Info-Gain", or "Top"

            BasicFeatureIndex fidx = new BasicFeatureIndex(d);
            System.out.println( "Dataset:\n # examples = "+d.size() );
            System.out.println( " # features = "+fidx.numberOfFeatures() );
            DatasetLoader.save( d,dataFile );

            PrintStream out = new PrintStream(new FileOutputStream(wordFile));
            for (Feature.Looper i=fidx.featureIterator(); i.hasNext(); )
            {
                Feature f = i.nextFeature();
                out.println( f );
            }

        } catch (Exception x) {;}

        System.exit(0);
    }

}
