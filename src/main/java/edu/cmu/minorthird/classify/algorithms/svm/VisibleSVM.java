package edu.cmu.minorthird.classify.algorithms.svm;

import java.awt.Component;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;

import libsvm.svm_model;
import libsvm.svm_node;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.GUI;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * VisibleSVM processes the svm_model from LIBSVM into recognizabled 
 * formats, and visible GUI in MINORTHIRD.
 * 
 * For example, each svm_node of svm_model is converted to
 * Feature object in MINORTHIRD corresponding to the feature information
 * stored in FeatureIdFactory. Each support vector of svm_model is 
 * converted to Example object in MINORTHIRD.
 * 
 * @author chiachi
 *
 */
public class VisibleSVM implements Visible,Serializable{
	
	static final long serialVersionUID=20071130L;
	
	private svm_model model;

	private String[] m_classStrLabels;//Label class name

	private Example[] m_examples;//SVs info.

	private String[][] m_exampleWeightLabels;//example has different weight according to different hyperplane for multiclass svm, mxn,m=#SVs,n=#Hyperplane

	private Hyperplane[] m_hyperplanes;//hyperplane for each pair of C(n,2), n=# of class label

	private int m_posIndicator; //1, no reverse need; -1, reverse the sign of coef value

	private String[] m_hpLabels;//If it's not POS vs. NEG, store labelClassName1 vs. labelClassName2 as Hyperplane Tab Label

	/**
	 * Constructor for the svm_model returned from SVMClassifier LIBSVM training.
	 * 
	 * @param model svm_model returned from LIBSVM training
	 * @param factory FeatureFactory which has Feature's id and corresponding information before converted into svm_node in svm_problem for LIBSVM training
	 * 
	 */
	public VisibleSVM(svm_model model,FeatureFactory factory){
		this(model,factory,null);
	}

	/**
	 * Constructor for the svm_model returned from MultiClassSVMClassifier 
	 * LIBSVM training.
	 * 
	 * @param model svm_model returned from LIBSVM training
	 * @param factory FeatureFactory which has Feature's id and corresponding information before converted into svm_node in svm_problem for LIBSVM training
	 * @param schema ExampleSchema which has class label information
	 *
	 */
	public VisibleSVM(svm_model model,FeatureFactory factory,ExampleSchema schema){

		initialize(model,schema);

		setExamples(model,factory);

		if(m_hyperplanes.length>1){

			setHyperplanes();

		}else{

			setHyperplane();

		}
	}

	/**
	 * Get Support Vectors of svm_model in array of MINORTHIRD Example format.
	 * 
	 * @return Example[] array of MINORTHIRD Example consctructed from SVs of svm_model
	 * 
	 */
	public Example[] getExamples(){

		return m_examples;

	}

	/**
	 * Get Weight Labels of Example 
	 * 
	 * return m X n String array, m = # of examples, n = # of corresponding hyperplanes 
	 * 
	 */
	public String[][] getExampleWeightLabels(){

		return m_exampleWeightLabels;

	}

	/**
	 * Get Hyperplane from the Hyperplane array
	 * 
	 */
	public Hyperplane getHyperplane(int index){

		if(index<0||index>=m_hyperplanes.length){

			throw new IllegalArgumentException("out of range: "+index);

		}

		return m_hyperplanes[index];
	}

	/**
	 * Get Hyperplane label from the Hyperplane labels array
	 * 
	 */
	public String getHyperplaneLabel(int index){

		if(index<0||index>=m_hpLabels.length){

			throw new IllegalArgumentException("out of range: "+index);

		}

		return m_hpLabels[index];
	}

	/**
	 * Add Feature and it's weight to Hyperplane. In case of the input Feature 
	 * already existed in Hyperplane, reset weight of this existed Feature to be the 
	 * existing weight plus the input Feature weight.
	 * 
	 * @param exampleIndex    index in example array
	 * @param hyperplaneIndex index in hyperplane array
	 * 
	 */
	private void addInfoToHyperplane(int exampleIndex,int hyperplaneIndex){

		for(Iterator<Feature> flidx=m_examples[exampleIndex].featureIterator();flidx
				.hasNext();){

			Feature ftemp=flidx.next();

			double featureWeightTemp=m_examples[exampleIndex].getWeight(ftemp);

			featureWeightTemp*=m_examples[exampleIndex].getWeight();

			m_hyperplanes[hyperplaneIndex].increment(ftemp,featureWeightTemp);
		}
	}

	/**
	 * Initialize the member variables of the class according to the input
	 * svm_model and ExampleSchema.
	 * 
	 * @param model     svm_model returned from LIBSVM training
	 * @param schema    ExampleSchema which has class label information
	 * 
	 */
	private void initialize(svm_model model,ExampleSchema schema){
		
		this.model=model;
		
		int[] labelTypes=model.label;

		if(schema!=null){

			setClassStrLabels(schema);

		}else{

			setClassStrLabels();

		}

		m_posIndicator=
				((labelTypes.length==2&&m_classStrLabels[0]
						.equals(ExampleSchema.POS_CLASS_NAME))||labelTypes.length>2)?1:-1;

		// Construct Hyperplanes and the corresponding tab labels
		double[] rhos=model.rho;

		m_hyperplanes=new Hyperplane[rhos.length];

		m_hpLabels=new String[m_hyperplanes.length];

		for(int index=0;index<m_hyperplanes.length;++index){

			m_hyperplanes[index]=new Hyperplane();

			m_hyperplanes[index].setBias((-1.0)*rhos[index]*m_posIndicator);

		}
	}

	/**
	 * Set displayed labels for the example weight
	 * 
	 */
	private void initExampleWeightLabels(){

		if(m_hyperplanes.length==1){

			for(int index=0;index<m_exampleWeightLabels.length;++index){

				DecimalFormat df=new DecimalFormat("0.0000");

				m_exampleWeightLabels[index][0]=
						df.format(m_examples[index].getWeight());
			}
		}else{
			// For multiClass
			for(int index=0;index<m_exampleWeightLabels.length;++index){
				for(int idx=0;idx<m_exampleWeightLabels[0].length;++idx){

					m_exampleWeightLabels[index][idx]="null";

				}
			}
		}
	}

	/**
	 * Set Example array from the SVs of svm_model
	 * 
	 * @param model     svm_model returned from LIBSVM training
	 * @param idFactory FeatureIdFactory which has Feature's id and corresponding information
	 *                  before converted into svm_node in svm_problem for LIBSVM training
	 * 
	 */
	private void setExamples(svm_model model,FeatureFactory factory){
		
		svm_node[][] supportVectorsTemp=model.SV; // support vectors

		//int[] labelTypes=m_gate.getLabelForEachClass();//numeric value of ClassLabel

		int[] numSVs=model.nSV; //number of SVs for each different class label

		double[][] coef=model.sv_coef; //(numLabels-1) X (numSVs) coefficient array

		int labelIndex=0;//current label

		int upperBoundIndex=numSVs[0];//upper bound of current label index in SVs array

		int numOfSupportVectors=supportVectorsTemp.length;

		m_examples=new Example[numOfSupportVectors];

		m_exampleWeightLabels=new String[m_examples.length][m_hyperplanes.length];

		for(int index=0;index<numOfSupportVectors;++index){

			// construct class label
			ClassLabel clTemp=new ClassLabel(m_classStrLabels[labelIndex]);

			// get weight and set prefix info. of example
			double weightTemp=m_posIndicator*coef[0][index];//2 class labels, 1 raws of coef. matrix. Weight will be reset if it's MultiClass.

			// construct example
			Instance iTemp=
					SVMUtils.nodeArrayToInstance(supportVectorsTemp[index],factory);
			m_examples[index]=new Example(iTemp,clTemp,weightTemp);

			// check if next SV belongs to different class label
			if(index==(upperBoundIndex-1)&&index!=(numOfSupportVectors-1)){

				++labelIndex;

				upperBoundIndex+=numSVs[labelIndex];
			}
		}

		initExampleWeightLabels();

	}

	/**
	 * Set Hyperplane for Binary SVMClassifier
	 * 
	 */
	private void setHyperplane(){

		m_hpLabels[0]="";

		//set # of SVs
		int numOfSupportVectors=0;

		int[] numSVs=model.nSV;

		for(int idx=0;idx<numSVs.length;++idx){

			numOfSupportVectors+=numSVs[idx];
		}

		// loop through each SVs
		for(int index=0;index<numOfSupportVectors;++index){

			// loop through each features in current looped example
			for(Iterator<Feature> flidx=m_examples[index].featureIterator();flidx
					.hasNext();){

				Feature ftemp=flidx.next();

				double featureWeightTemp=m_examples[index].getWeight(ftemp);//get weight of feature

				featureWeightTemp*=m_examples[index].getWeight();//times weight of example

				m_hyperplanes[0].increment(ftemp,featureWeightTemp);//store feature and its weight info into hyperplane object
			}
		}
	}

	/**
	 * Set Hyperplane array for MultiClassSVMClassifier
	 * 
	 */
	private void setHyperplanes(){

		int[] labelTypes=model.label;

		double[][] coef=model.sv_coef;

		// Init started index for different class labels
		int[] numSVs=model.nSV;
		
		int[] startIdx=new int[labelTypes.length];

		startIdx[0]=0;

		for(int index=1;index<startIdx.length;++index){

			startIdx[index]=startIdx[index-1]+numSVs[index-1];

		}

		// Set Hyperplane features and tab information
		DecimalFormat df=new DecimalFormat("0.0000");

		int hpIndex=0;

		for(int iIdx=0;iIdx<labelTypes.length;++iIdx){
			for(int jIdx=iIdx+1;jIdx<labelTypes.length;++jIdx){

				m_hpLabels[hpIndex]=
						m_classStrLabels[iIdx]+" vs. "+m_classStrLabels[jIdx];

				int label0svsCount=numSVs[iIdx];

				int label1svsCount=numSVs[jIdx];

				int label0startedSVindex=startIdx[iIdx];

				int label1startedSVindex=startIdx[jIdx];

				for(int exampleIdx=label0startedSVindex;exampleIdx<(label0startedSVindex+label0svsCount);++exampleIdx){

					m_examples[exampleIdx].setWeight(coef[jIdx-1][exampleIdx]);

					m_exampleWeightLabels[exampleIdx][hpIndex]=
							df.format(coef[jIdx-1][exampleIdx]);

					addInfoToHyperplane(exampleIdx,hpIndex);
				}

				for(int exampleIdx=label1startedSVindex;exampleIdx<(label1startedSVindex+label1svsCount);++exampleIdx){

					m_examples[exampleIdx].setWeight(coef[iIdx][exampleIdx]);

					m_exampleWeightLabels[exampleIdx][hpIndex]=
							df.format(coef[iIdx][exampleIdx]);

					addInfoToHyperplane(exampleIdx,hpIndex);
				}
				++hpIndex;
			}
		}
	}

	/**
	 * Convert the numerical value of binary class labels from svm_model
	 * into the corresponding class label names.
	 * 
	 */
	private void setClassStrLabels(){

		int[] numericLabels=model.label;

		m_classStrLabels=new String[numericLabels.length];

		for(int index=0;index<m_classStrLabels.length;++index){

			m_classStrLabels[index]=

			(numericLabels[index]==1)?ExampleSchema.POS_CLASS_NAME:

			ExampleSchema.NEG_CLASS_NAME;
		}
	}

	/**
	 * Convert the numerical value of multi class labels from svm_model
	 * into the corresponding class label names.
	 * 
	 * @param schema ExampleSchema which has the label name and it's corresponding
	 *               numerical value.
	 */
	private void setClassStrLabels(ExampleSchema schema){

		int[] numericLabels=model.label;

		m_classStrLabels=new String[numericLabels.length];

		for(int index=0;index<m_classStrLabels.length;++index){

			m_classStrLabels[index]=schema.getClassName(numericLabels[index]);

		}
	}

	/************************************************************************
	 * GUI
	 *************************************************************************/
	@Override
	public Viewer toGUI(){

		ParallelViewer pv=new ParallelViewer();

		// construct Support Vecotrs Viewer
		Viewer svsGui=
				new ControlledViewer(new SVsViewer(),new SVsControls(m_hpLabels.length,
						m_hpLabels));

		svsGui.setContent(this);

		GUI.ExampleViewer svDetailGui=new GUI.ExampleViewer();

		pv.addSubView("Support Vectors",new ZoomedViewer(svsGui,svDetailGui));

		// construct Hyperplane Viewer
		for(int index=0;index<this.m_hyperplanes.length;++index){

			SVMHyperPlaneViewer hyperplaneViewer0=new SVMHyperPlaneViewer();

			hyperplaneViewer0.setHypid(index);

			hyperplaneViewer0.receiveContent(this);

			pv.addSubView(m_hpLabels[index]+" Hyperplane",hyperplaneViewer0);

		}

		return pv;
	}

	/*
	 * Controls over SVsViwer
	 * 
	 */
	static private class SVsControls extends ViewerControls{
		
		static final long serialVersionUID=20071130L;

		private int m_numButtons;

		private String[] m_buttonLabels;

		private JRadioButton[] weightTypeButton;

		public SVsControls(int numButtons,String[] buttonLabels){

			m_numButtons=numButtons;

			m_buttonLabels=new String[buttonLabels.length];

			for(int index=0;index<m_numButtons;++index){

				m_buttonLabels[index]=buttonLabels[index];
			}

			weightTypeButton=new JRadioButton[m_numButtons];

			ButtonGroup group=new ButtonGroup();
			;

			add(new JLabel("   Sort by"));

			// set first weight label
			if(m_buttonLabels[0].equals("")){

				weightTypeButton[0]=addButton("weight",group,true);

			}else{

				weightTypeButton[0]=
						addButton("weight("+m_buttonLabels[0]+")",group,true);

			}

			//set the rest weight labels if any
			for(int index=1;index<m_numButtons;++index){

				weightTypeButton[index]=
						addButton("weight("+m_buttonLabels[index]+")",group,false);

			}

		}

		@Override
		public void initialize(){
			//Init in construtor with two parameters, # of buttons, and button labels array
		}

		public int getSelectedButtonIndex(){

			for(int index=1;index<m_numButtons;++index){

				if(weightTypeButton[index].isSelected()){
					return index;
				}
			}

			return 0;
		}

		private JRadioButton addButton(String s,ButtonGroup group,boolean selected){

			JRadioButton button=new JRadioButton(s,selected);

			group.add(button);

			add(button);

			button.addActionListener(this);

			return button;
		}
	}

	/**
	 * Viewer for Support Vectors from LIBSVM svm_model
	 * 
	 */
	static private class SVsViewer extends ComponentViewer implements
			Controllable{
		
		static final long serialVersionUID=20071130L;

		private SVsControls controls=null;

		private VisibleSVM vsSVM=null;

		@Override
		public void applyControls(ViewerControls controls){
			this.controls=(SVsControls)controls;

			setContent(vsSVM,true);

			revalidate();
		}

		@Override
		public boolean canReceive(Object o){

			return o instanceof VisibleSVM;
		}

		@Override
		public JComponent componentFor(Object o){

			vsSVM=(VisibleSVM)o;

			final Example[] exampleTemp=vsSVM.getExamples();

			final String[][] exampleWeightsTemp=vsSVM.getExampleWeightLabels();

			final int numCols=1+exampleWeightsTemp[0].length;

			Object[][] tableData=new Object[exampleTemp.length][numCols];

			for(int index=0;index<exampleTemp.length;++index){

				for(int wIdx=0;wIdx<numCols-1;++wIdx){

					tableData[index][wIdx]=exampleWeightsTemp[index][wIdx];

				}

				tableData[index][numCols-1]=exampleTemp[index];

			}

			String[] columnNames=new String[numCols];

			if(numCols==2){

				columnNames[0]="Weight";

			}else{

				for(int index=0;index<numCols-1;++index){

					columnNames[index]="Weight ("+vsSVM.getHyperplaneLabel(index)+")";

				}
			}
			columnNames[numCols-1]="Example";

			if(controls!=null){

				sortTableData(tableData,controls.getSelectedButtonIndex());

			}else{

				sortTableData(tableData,0);

			}

			JTable table=new JTable(tableData,columnNames);

			table.setDefaultRenderer(Example.class,new TableCellRenderer(){

				@Override
				public Component getTableCellRendererComponent(JTable table,
						Object value,boolean isSelected,boolean hasFocus,int row,int column){
					return GUI.conciseExampleRendererComponent((Example)value,60,
							isSelected);
				}
			});

			monitorSelections(table,numCols-1);

			JScrollPane scrollpane=new JScrollPane(table);

			scrollpane
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			return scrollpane;

		}

		private void sortTableData(Object[][] tableData,int index){

			final int fidx=index;

			Arrays.sort(tableData,new Comparator<Object[]>(){

				@Override
				public int compare(Object[] ra,Object[] rb){

					if(rb[fidx].toString()=="null"){
						if(ra[fidx].toString()=="null"){
							return 0;
						}
						return -1;
					}else if(ra[fidx].toString()=="null"){
						return 1;
					}

					Double da=Double.valueOf(ra[fidx].toString());
					Double db=Double.valueOf(rb[fidx].toString());
					return MathUtil.sign(db.doubleValue()-da.doubleValue());
				}
			});
		}
	}

	/**
	 * Viewer for Hyperplane consctrued from LIBSVM svm_model 
	 *
	 */
	static private class SVMHyperPlaneViewer extends ComponentViewer{
		
		static final long serialVersionUID=20071130L;

		private int m_hyperplaneId;

		@Override
		public boolean canReceive(Object o){

			return o instanceof VisibleSVM;
		}

		@Override
		public JComponent componentFor(Object o){

			final VisibleSVM vsSVM=(VisibleSVM)o;

			return vsSVM.getHyperplane(m_hyperplaneId).toGUI();
		}

		public void setHypid(int id){

			m_hyperplaneId=id;

		}
	}
}