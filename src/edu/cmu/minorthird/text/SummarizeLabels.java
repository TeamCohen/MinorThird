package edu.cmu.minorthird.text;

/** Main routine that loads a TextLabels object and summarizes its properties.
 *
 * @author William Cohen
 */

public class SummarizeLabels
{
	public static void main(String[] argv)
	{
		try {
			TextLabels labels = FancyLoader.loadTextLabels(argv[0]);
			System.out.println("========== Summary of "+argv[0]+" ==========");
			System.out.println("Documents:        "+labels.getTextBase().size());
			System.out.println("Token Properties: "+labels.getTokenProperties());
			System.out.println("Span Properties:  "+labels.getSpanProperties());
			System.out.println("Span Types:       "+labels.getTypes());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: labelKey");
		}
	}
}
