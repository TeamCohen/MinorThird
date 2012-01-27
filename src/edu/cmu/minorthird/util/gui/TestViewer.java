package edu.cmu.minorthird.util.gui;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Some test cases for viewers.
 * 
 * @author William cohen
 *
 */

public class TestViewer{

	static private Viewer parallelStringViewer(){
		Viewer lc=new ComponentViewer(){

			static final long serialVersionUID=20080517L;

			@Override
			public boolean canReceive(Object o){
				return o instanceof String;
			}

			@Override
			public JComponent componentFor(Object o){
				String s=(String)o;
				System.out.println("converting "+s+" to lower case!");
				return new JTextArea(s.toLowerCase());
			}
		};
		Viewer uc=new ComponentViewer(){

			static final long serialVersionUID=20080517L;

			@Override
			public boolean canReceive(Object o){
				return o instanceof String;
			}

			@Override
			public JComponent componentFor(Object o){
				String s=(String)o;
				System.out.println("converting "+s+" to upper case!");
				return new JTextArea(s.toUpperCase());
			}
		};
		Viewer van=new VanillaViewer();
		ParallelViewer par=new ParallelViewer();
		par.addSubView("Original",van);
		par.addSubView("LowerCase",lc);
		par.addSubView("UpperCase",uc);
		return par;
	}

	static private Viewer indexViewer(){
		final Map<String,String[]> m=new HashMap<String,String[]>();
		m.put("Hurst",new String[]{"Matthew","Wakako","Hannah"});
		m.put("Cohen",new String[]{"William","Susan","Joshua","Charlie"});
		m.put("Tomikoyo",new String[]{"Takashi","Laura","Makoto","TBA"});
		Viewer indexViewer=new IndexedViewer(){
			static final long serialVersionUID=20080517L;
			@Override
			public Object[] indexFor(Object o){
				sendSignal(TEXT_MESSAGE,"displaying the "+o+" family");
				return m.get(o);
			}
		};
		Viewer lnameListViewer=new ComponentViewer(){
			static final long serialVersionUID=20080517L;
			@Override
			public JComponent componentFor(Object o){
				Object[] array=(Object[])o;
				JList jlist=new JList(array);
				monitorSelections(jlist);
				return jlist;
			}
		};
		lnameListViewer.setContent(m.keySet().toArray());
		ZoomedViewer familyViewer=
				new ZoomedViewer(indexViewer,parallelStringViewer());
		familyViewer.setHorizontal();
		return new MessageViewer(new ZoomedViewer(lnameListViewer,familyViewer));
	}

	public static void main(String[] argv){
		try{
			Viewer v=indexViewer();
			/*
			TransformedViewer v = new TransformedViewer() {
					public Object transform(Object o) { 
						System.out.println("transforming "+o);
						return "hello, "+o; 
					}
				};
			v.setSubView( parallelStringViewer() );
			v.receiveContent( "world" );
			 */
			new ViewerFrame("Test",v);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
