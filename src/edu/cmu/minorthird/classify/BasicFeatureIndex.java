package edu.cmu.minorthird.classify;



/**
 * @author Edoardo Airoldi
 * Date: Nov 24, 2003
 */

public class BasicFeatureIndex extends DatasetIndex implements FeatureIndex {

   public BasicFeatureIndex()
   {
      super();
   }

   public BasicFeatureIndex(Dataset data)
   {
      this();
      for (Example.Looper i=data.iterator(); i.hasNext(); ) {
         addExample(i.nextExample());
      }
   }


   /** Number of examples with label l containing non-zero values for feature f. */
   public int size(Feature f,String label)
   {
      int size = 0;
      for (int j=0; j<size(f); j++) {
         if ( label.equals( getExample(f,j).getLabel().bestClassName() ) ) {
            size += 1;
         }
      }
      return size;
   }

   /** Get counts of feature f in i-th example containing feature f */
   public double getCounts(Feature f,int i)
   {
      return ((Example)featureIndex(f).get(i)).getWeight(f);
   }

   /** Get counts of feature f in examples with label l */
   public double getCounts(Feature f,String label)
   {
      double total=0.0;
      for (int j=0; j<size(f); j++) {
         //System.out.println( getExample(f,j).getLabel().bestClassName() );
         if ( label.equals( getExample(f,j).getLabel().bestClassName() ) ) {
            total += ((Example)featureIndex(f).get(j)).getWeight(f);
         }
      }
      return total;
   }

   /** Get number of documents which contain feature f with label l */
   public double getDocCounts(Feature f,String label)
   {
      double total=0.0;
      for (int j=0; j<size(f); j++) {
         //System.out.println( getExample(f,j).getLabel().bestClassName() );
         if ( label.equals( getExample(f,j).getLabel().bestClassName() ) ) {
            total += 1.0;
         }
      }
      return total;
   }


   public String toString()
   {
      StringBuffer buf = new StringBuffer("[index");
      for (Feature.Looper i=featureIterator(); i.hasNext(); ) {
         Feature f = i.nextFeature();
         buf.append("\n"+f+":");
         for (int j=0; j<size(f); j++) {
            buf.append("\n\t"+getExample(f,j).toString());
            //buf.append("\n\t"+"feature:"+f+" counts:"+getCounts(f,j));
            //buf.append(" label:"+getExample(f,j).getLabel().bestClassName());
         }
         buf.append("\n\t"+"feature:"+f+" posCounts:"+getCounts(f,"POS")+" negCouns:"+getCounts(f,"NEG"));
      }
      buf.append("\nindex]");
      return buf.toString();
   }

   static public void main(String[] args)
   {
      System.out.println(new BasicFeatureIndex(SampleDatasets.sampleData("bayes",false)));
   }


}
