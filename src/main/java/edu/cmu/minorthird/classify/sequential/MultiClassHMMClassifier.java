package edu.cmu.minorthird.classify.sequential;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;


//// so here when you call the 		MultiClassHMMClassifier, it's like return MultiClassHMMClassifier( dataset)	
	public class MultiClassHMMClassifier implements SequenceClassifier,SequenceConstants,Visible,Serializable
	{
		
		static final long serialVersionUID=20080207L;
		
		private ExampleSchema schema;
		public HMM hmmModel;
		private int numStates; 
		private int numEmissions;
		String[] state;
    double[][] aprob;
    double[][] eprob;
		ArrayList<String[]> training_seq;
		private Hashtable<String,String> dict_tok;
//		private Hashtable<String,String> dict_tok2idx;
//		private Hashtable<String,String> dict_idx2tok;
						
		/* HMM needs the dataset, to build the Hashtables and init all the matrix*/
		public MultiClassHMMClassifier(SequenceDataset dataset) 
		{
			this.schema = dataset.getSchema();
			
		/*		schema.validClassNameSet has private access so we can't change it here, 
		but we'll add the state 'start' to it conceptually*/
						
		// adding 1 state corresponding to 'start of sentence' will be done in hmm
			this.numStates = schema.getNumberOfClasses();

			this.state = new String[numStates];
			for(int i=0;i<schema.getNumberOfClasses();i++){
				state[i] = schema.getClassName(i);
			}

			this.dict_tok = new Hashtable<String,String>();
			training_seq = new ArrayList<String[]>();
			
			for (Iterator<Example[]> i=dataset.sequenceIterator(); i.hasNext(); ) {
				Example[] sequence = i.next();
				String[] tok = new String[sequence.length];			
//				int labels[] = new int[sequence.length];
				int size;
				String token;
				for (int j=0; j<sequence.length; j++) {
//						ClassLabel label = sequence[j].getLabel();
						size = sequence[j].numericFeatureIterator().next().size();
						token = sequence[j].numericFeatureIterator().next().getPart(size-1);
						tok[j] = token;
						if ( dict_tok.containsKey(token) ){
							int cnt = Integer.parseInt(dict_tok.get(token));
							cnt++;
							dict_tok.put(token,String.valueOf(cnt));
						}else{
							dict_tok.put(token,"1");
						}
// 	          labels[j] = schema.getClassIndex(sequence[j].getLabel().bestClassName());

	      }
				training_seq.add( tok);
			}
			
			dict_tok.put("UNSEEN","1");
			this.numEmissions=dict_tok.size();				
					
/* initHMM, could be estimate AB based on the dataset*/			
			aprob = new double[numStates][numStates];
			eprob = new double[numStates][numEmissions];
			hmmModel = new HMM(state, aprob, dict_tok, eprob);
		}

/*baum welch for hmm*/
		public void  baumwelch( final double threshold) {
			ArrayList<String[]> training_data = new ArrayList<String[]>( this.training_seq.size());
			for( int i=0; i<training_seq.size();i++){
				training_data.add( hmmModel.convert_Ob_seq( training_seq.get(i) ) );
			}
			hmmModel = HMM.baumwelch( training_data, this.state, this.dict_tok, threshold);    
			return;                          	
    }

// I think here you need the viterbi to get the ClassLabel[] for the instance[], something to
//take the place of h[i].score(instance)
		@Override
		public ClassLabel[] classification(Instance[] sequence)
		{
			ClassLabel[] label = new ClassLabel[sequence.length];
			String[] ob_seq = new String[sequence.length];
			
			for (int i=0; i<sequence.length; i++) {		
				int size = sequence[i].numericFeatureIterator().next().size();
				ob_seq[i] = sequence[i].numericFeatureIterator().next().getPart(size-1);
System.out.println("ob_seq["+i+"] is "+ob_seq[i]);				
			}
//			System.out.println("End of one call ");

		    String[] seq;
		    seq = hmmModel.convert_Ob_seq( ob_seq );
		    
    Viterbi vit = new Viterbi(hmmModel, seq);
    //vit.print(new SystemOut());
    
			String[] tag_seq = vit.getPath();		    
			for (int i=0; i<tag_seq.length; i++) {		
				label[i]= new ClassLabel(tag_seq[i]);
System.out.println("tag_seq["+i+"] is "+tag_seq[i]);					
			}		    
//				label[i]= new ClassLabel("NEG");
//				label.add( schema.getClassName(i), h[i].score(instance) );
//				System.out.println("Name of Classes is "+schema.getClassName(i));


			return label; 
		}

/** Return some string that 'explains' the classification, 
this function is also required to be re-written*/
		@Override
		public String explain(Instance[] instance)
		{
			StringBuffer buf = new StringBuffer("");
			for (int i=0; i<numStates; i++) {			
				buf.append("Hyperplane for class "+schema.getClassName(i)+":\n");
//				buf.append( h[i].explain(instance) );
				buf.append("\n");
			}
			return buf.toString();
		}

	    @Override
			public Explanation getExplanation(Instance[] instance) {
		Explanation.Node top = new Explanation.Node("MultiClassHMM Explanation");
		for (int i=0; i<numStates; i++) {			
		    Explanation.Node classEx = new Explanation.Node("Hyperplane for class "+schema.getClassName(i)+":\n");
		    top.add(classEx);
		}
		Explanation ex = new Explanation(top);
		return ex;
	    }

/*This one is for visualization*/
		@Override
		public Viewer toGUI()
		{
			Viewer gui = new ComponentViewer() {
				static final long serialVersionUID=20080207L;
					@Override
					public JComponent componentFor(Object o) {
						MultiClassHMMClassifier c = (MultiClassHMMClassifier)o;
						JPanel main = new JPanel();
						for (int i=0; i<numStates; i++) {
							JPanel classPanel = new JPanel();
							classPanel.setBorder(new TitledBorder("Class "+c.schema.getClassName(i)));
//							Viewer subviewer = voteMode ? s_t[i].toGUI() : w_t[i].toGUI();
//							subviewer.setSuperView( this );
//							classPanel.add( subviewer );
							main.add(classPanel);
						}
						return new JScrollPane(main);
					}
				};
			gui.setContent(this);
			return gui;
		}


/*This one could be used to output the model, say all the matrix*/
		@Override
		public String toString() 
		{
			return "[MultiClassHMMClassifier:";
		}
	}
