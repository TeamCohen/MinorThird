package edu.cmu.minorthird.util;

/** Stores the version number as: <br>
 * <br>
 * Version # Format: Version#.Year.Month.Day <br>
 * <br>
 * Version History:
 * <hr>
 * <table border="0">
 *   <tr valign=top><td align="right">1.5.7.15</td> 
 *     <td align="left">
 *       - First Version to be checked in. <br>
 *       - Recently added Mixup tokenization extras 
 *   </td></tr>
 *   <tr valign=top><td align="right">2.5.8.9</td>
 *     <td align="left">-  Added multi label classification to text package
 *   </td></tr> 
 *   <tr valign=top><td align="right">3.5.8.18</td>
 *     <td align="left">-  New Explanation Facilities
 *   </td></tr>
 *   <tr valign=top><td align="right">3.2.5.9.15</td>
 *     <td align="left">Fixed Bugs:<br>
 *                 - createSpanProp <br>
 *                 - MaxEntLearner(CRFLearner)log space option: trainer ll (for large german dataset) <br>
 *                 - CascadingBinaryLearner macKappa changed from 0.0 t0 -1.0 <br>
 *                 - MultiClassifierAnnotator outputs _predicted_spanProp <br>
 *   </td></tr> 
 *   <tr valign=top><td align="right">4.5.10.3</td>
 *     <td align="left">- NEW GUI<br>
 *                 -  other option for classifyCommandLineUtil<br>
 *                 -  Fixed saving bug in classify package<br>
 *   </td></tr>
 *   <tr valign=top><td align="right">5.5.10.17</td>
 *     <td align="left">-  Fixed bug in GUI (PE doesn't pop up when TextField is entered) <br>
 *                 -  Added TransformingMultiLearner and PredictedClassTransform for adding predicted class
 *                    names as features for multi label classification
 *   </td></tr>
 *   <tr valign=top><td align="right">5.2.5.10.27</td>
 *     <td align="left">- a few gui fixes: help button fixes, showTestDetails automatic, DebugMixup, 
 *                        and EditParams added to Selectable Types<br>
 *                 - Saving Extractor bug fixed<br>
 *                 - classify command line to gui -learner option consistency fixed<br>
 *   </td></tr>
 *   <tr valign=top><td align="right">6.5.11.30</td>
 *     <td align="left">- More gui updates:  shorted class names, links to tutorials, links to javadocs, more help buttons<br>
 *                 - more parameter bunches added to selectable types<br>
 *                 - Documentation added: Index and tutorial<br>
 *                 - Fixed cross option for TrainTestMultiClassifier - Tester.multiEvaluate, MultiCrossValidatedDataset<br>
 *   </td></tr>
 *   <tr valign=top><td align="right">6.2.5.12.8</td>
 *     <td align="left">- Fixed MultiClassifier cross testing implementation<br>
 *                 - Fixed MultiClassifier explanation facilities<br>
 *                 - Added and updated documentation<br>
 *                 - Fixed SpanTypeTextBase to extract more than one span Type per document</br>
 *    </td></tr>
 *    <tr valign=top><td align="right">7.6.1.11</td>
 *      <td align="left">- added demos/MyFE.java as an example<br>
 *                 - modified CommandLineUtil.newObjectFromBSH to allow bsh.source(file)<br>
 *                 - added PreprocessTextForClassifier, PreprocessTextForExtractor<br>
 *                 - added text.learn.BeginContinueOutsideReduction<br>
 *                 - updated selectableTypes as appropriate<br>
 *                 - fixed buggy implementation of span.getLoChar(), span.getHiChar()<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">7.6.1.31</td>
 *      <td align="left">- added main to SpanDifference<br>
 *                 - added mixup command 'annotateWith FILE'<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">8.6.2.13</td>
 *      <td align="left">- Changed createSpanProp to implement Mixup<br>
 *                 - fixed semiCRF saving bug<br>
 *                 - fixed DecisionTree Print<br>
 *                 - Updated MinorTagger and MinorTaggerClient to output labels format and run on velmak<br>
 *                 - Cleaned up commandLineUtil<br>
 *                 - Fixed bug in RefUtils<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">8.6.3.15</td>
 *      <td align="left">- cleaned up output in ProgressCounter<br>
 *                 - fixed some bugs in TextBaseLoader, for reading in text with XML labels<br>
 *                 - added ProgressCounter to TextBaseLoader for loading directories<br>
 *                 - made OneVsAllLearner work for Online learners<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">9.6.4.19</td>
 *      <td align="left">- NEW classify GUI<br>
 *                 - fixed bug in CRFLearner<br>
 *                 - New LevelManager and TexBaseMapper for handling multiple levels<br>
 *                 - importLabels taken out of TextBase<br>
 *                 - Encapsulated Annotator Loader fixed<br>
 *                 - Updated Documentation<br>
 *                 - Cammie's Last Day!<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">10.6.10.12</td>
 *      <td align="left">- Added confidence calculation capability to the SVM learner<br>
 *                 - Fixed encapsulated annotators so that they can require other encapsulated annotators.<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">10.6.10.23</td>
 *      <td align="left">- Fixed errors in MixupPrograms handling of regular expressions.
 *    </td></tr>
 *    <tr valign=top><td align="right">11.6.11.21</td>
 *      <td align="left">- Initial version of extractor-confidence computation
 *    </td></tr>
 *    <tr valign=top><td align="right">11.6.11.22</td>
 *      <td align="left"> - Better version of extractor-confidence computation, with BeamSearch modified appropriately
 *    </td></tr>
 *    <tr valign=top><td align="right">12.7.3.30</td>
 *      <td align="left"> - Added support for a hierarchy of text bases and text labels that can be used in code or mixup<br>
 *      <td align="left"> - Refactored the Tokenizer class to support multiple tokenization schemes<br>
 *      <td align="left"> - Refactored the TextBase hierarchy to make the TextBase interface immutable with a mutable abstract
 *                          implementation defining the mutable functionality. <br>
 *      <td align="left"> - Created a TextBaseManager class to manage text bases that are derived from each other.<br>
 *      <td align="left"> - Added keywords to the mixup language to provide access to the TextBaseManager functionality.<br>
 *    </td></tr>
 *    <tr valign=top><td align="right">12.7.5.31</td>
 *      <td align="left"> - Fixed bug where history size variable for certain learners was being ignored<br>
 *      <td align="left"> - Added new Random Forests learners<br>
 *      <td align="left"> - Fixed bugs that prevented compilation under java 1.6<br>
 *      <td align="left"> - Switched the build mode from java 1.4 to java 1.5 in the ant build scripts<br>
 *      <td align="left"> - Created new layout for documentation<br>
 *    </td></tr>
 * </table>
 */
public class Version {

    private static String version = "Version 12.7.5.31";

    public static String getVersion() {
        return version;
    }
}
