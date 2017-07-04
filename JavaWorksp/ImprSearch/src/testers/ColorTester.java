package testers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import imprSearch.ImprManager;

/**
 * Test your eyes and the manager's and see if they have similar sensitivity to colors
 * @author asus-pc
 *
 */
public class ColorTester extends JFrame implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Container con;
	private ArrayList<Integer> colorPool = new ArrayList<Integer>();
	private ArrayList<boolean[]> result;
	private JPanel pane;
	private JButton start, quit, yes, no;
	private static Thread testing = new Thread(new Testing());
	int color1,color2;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		javax.swing.SwingUtilities.invokeLater(testing);

	}
	
	private static class Testing implements Runnable{ //what is a static class?

		@Override
		public void run() {
			// TODO Auto-generated method stub
			 new ColorTester();
		}
		
	}
	
	public ColorTester(){
		super("Color Tester");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(1000, 800));
		this.pack();
		this.setVisible(true);
		con = this.getContentPane();
		generateColorCombinations();
		this.setLayout(new BorderLayout());
		start = new JButton("Start testing if you are not color blind.");
		this.add(start,BorderLayout.SOUTH);
		start.addActionListener(this);
	}
	
	private void generateColorCombinations() {
		// TODO Auto-generated method stub
		int step = 10;
		for(int i=0; i<100; i+=step){
			for(int j=0; j<100; j+=step){
				for(int k=0; k<100; k+=step){
					colorPool.add(Color.HSBtoRGB((float)i/100, (float)j/100, (float)k/100));
				}
					
			}
		
		}
		result = new ArrayList<boolean[]>();
	}

	private void showTestingImage(){
		if(colorPool.size()==1){
			showResult();
		}
		int index = (int)(Math.random()*colorPool.size());
		color2 = colorPool.remove(index); //TODO: no colors left!
		Color c2= new Color(color2);
		Graphics2D g = (Graphics2D) con.getGraphics();
		g.setColor(c2);
		g.fillRect(0, 0, con.getWidth(), con.getHeight());
		
		color1=color2;
		
		if(index%5!=0){
			index = (int)(Math.random()*colorPool.size());
			color1 = colorPool.remove(index);
			Color c1= new Color(color1);
			
			
			g.setColor(c1);
			int r = 20+(int)(Math.random()*(con.getWidth()+con.getHeight())/4);
			int x = (int)(Math.random()*(con.getWidth()/2-2*r));
			int y = (int)(Math.random()*(con.getHeight()/2-2*r));
			g.fillOval(x,y, 2*r, 2*r);
		}
		
		g.dispose();
		
	}
	
	private void erase(){
		Graphics2D g = (Graphics2D) con.getGraphics();
		Color tr = Color.LIGHT_GRAY;
		g.setColor(tr);
		g.fillRect(0, 0, con.getWidth(), con.getHeight());
		g.dispose();
	}
	
	private void aTest(){
		yes.setEnabled(false);
		no.setEnabled(false);
		
		showTestingImage();
		
		try {
			synchronized(testing) { //what's this???
			       testing.wait(300);
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		erase();
		
		try {
			synchronized(testing) {
		       testing.wait(1000);
		    }
			
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//java.lang.IllegalMonitorStateException
		
		yes.setEnabled(true);
		no.setEnabled(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource().equals(start)){
			start.setVisible(false);
			pane = new JPanel();
			
			pane.setLayout(new FlowLayout());
			con.add(pane,BorderLayout.SOUTH);
			
			yes = new JButton("I can remember there was a circle.");
			pane.add(yes);
			yes.addActionListener(this);
			
			no = new JButton("I didn't see a circle.");
			pane.add(no);
			no.addActionListener(this);
			
			quit = new JButton("Qiut and see the result.");
			pane.add(quit);
			quit.addActionListener(this);
			
			
			
			aTest();
			
		}else if(e.getSource().equals(yes)){
			boolean[] r = new boolean[2];
			if(color1==color2) {
				 r[0]=true;
				 r[1]=false;
			}else{
				 r[0]=ImprManager.areSimilarColors(color1,color2);
				 r[1]=false;
			}
			result.add(r);
			aTest();
			
		}else if(e.getSource().equals(no)){
			boolean[] r = new boolean[2];
			if(color1==color2) {
				 r[0]=true;
				 r[1]=true;
			}else{
				 r[0]=ImprManager.areSimilarColors(color1,color2);
				 r[1]=true;
			}
			result.add(r);
			aTest();
			
		}else if(e.getSource().equals(quit)){
			showResult();
		}
		
	}
	
	private void showResult(){
		JLabel table = new JLabel();
		String str = "The Testing results are: \n";
		int count =0;
		int TFcount=0; 
		int FTcount =0;
		for(boolean[] i: result){
			if(i[0]==i[1]) count++;
			else if(i[0]==true)TFcount++;
			else FTcount++;
			str+=i[0]+","+i[1]+"\n";
			
		}
		str+="The accuracy is "+ (double)count/result.size()+".\n";
		str+="The total number of tests: "+result.size()+".\n";
		str+="The times when I thought there were two colors but manager said no: "+TFcount+".\n";
		str+="The times when I thought there was no circle but manager said yes: "+FTcount+".\n";
		table.setText(str);
		con.add(table);
		yes.setEnabled(false);
		no.setEnabled(false);
		quit.setEnabled(false);
		System.out.println(str);
	}
	private void testFunctions(){
		for(int s=0; s<100; s++){
			System.out.println("x=y= "+s);
			System.out.println("Func2: "+ ImprManager.func2(s, s));
		}
	}
	
}
