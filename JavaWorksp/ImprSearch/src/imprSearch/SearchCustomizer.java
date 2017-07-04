package imprSearch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Used in the first time the user uses this searching tool
 * or any time the user cleared memory
 * 
 * create a customized ColorConverter
 * Usage: 
 * scroll the spectrum,
 * enter the start values and end values of intervals in which you find color transition as subtle as negligible
 * make sure to write them in pairs
 * to preview the customized spectrum, click validate
 * 
 * Note: range like 98 to 5 is not supported! Write 0 to 5, 98 to 99. 
 * @author asus-pc
 *
 */
@SuppressWarnings("serial")
public class SearchCustomizer extends JFrame implements ActionListener, PropertyChangeListener{
	private JPanel settings;
	private BufferedImage spectrum;
	private ArrayList<Integer> seps;
	private JButton clear,validate;
	private ArrayList<JFormattedTextField> fields;
	private Canvas canvas;
	private ColorConverter converter;
	private static final int SPEC_HEIGHT = 500;
	private static final int SPEC_STEP = 50;
	private static final int K=100;
	private static final int PADDING = 30;
	private static final Dimension C_SIZE=new Dimension(1500,700);
	private static final int SPEC_BORDER = 20;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {           	
                new SearchCustomizer();
            }
        });
	}
	
	public SearchCustomizer(){
		super("Search Customizer");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		
		
	    this.setLayout(new BorderLayout());
	    converter=new ColorConverter();
	    spectrum=new BufferedImage(SPEC_STEP*(K+2*PADDING), SPEC_HEIGHT+SPEC_BORDER, BufferedImage.TYPE_INT_ARGB);
	    drawSpectrum();
	    
	    canvas = new Canvas();
	    JScrollPane scroller =  new JScrollPane(canvas);
	    scroller.setPreferredSize(C_SIZE);
	    this.add(scroller);
	    refresh();
	    
		seps=new ArrayList<Integer>();
		fields=new ArrayList<JFormattedTextField>();//[ColorConverter.NUM_SEPS];
		clear=new JButton("Clear Values");
		validate=new JButton("Validate");
		settings = new JPanel();
		settings.setLayout(new GridBagLayout());
		organizeSettings();
		this.add(settings, BorderLayout.SOUTH);
		this.setPreferredSize(new Dimension(1500,500));
		validate.addActionListener(this);
		
		int seps[] = new int[ColorConverter.NUM_SEPS];
	
		
		clear.addActionListener(this);
		this.pack();
		this.setVisible(true);
	}

	private void organizeSettings() {
		// TODO Auto-generated method stub
		GridBagConstraints c = new GridBagConstraints();
		c.weightx=0.5;
		c.weighty=0.5;	
		c.fill=GridBagConstraints.HORIZONTAL;
		
		c.gridx=0;
		c.gridy=0;
		c.gridwidth=2;
		c.gridheight=3;
		JTextField instructions=new JTextField("Instructions: type in the start index and the end index of the inteval you want to compress, \n"
				+ "according to the number that appears on the spectrum. Click the \"validate\" button to see new spectrum. ");
		instructions.setEditable(false);
		Rectangle r = new Rectangle(0,0,300,300);
		instructions.scrollRectToVisible(r);
		instructions.setPreferredSize(new Dimension(200,200));
		settings.add(instructions,c);
		
		c.anchor=GridBagConstraints.EAST;
		
		int i;
		for(i=0; i<ColorConverter.NUM_SEPS; i+=2){
			JFormattedTextField tf1 = new JFormattedTextField(NumberFormat.getNumberInstance());
			fields.add(tf1);
			JFormattedTextField tf2 = new JFormattedTextField(NumberFormat.getNumberInstance());
			fields.add(tf2);
			
			tf1.addPropertyChangeListener(this);
			tf2.addPropertyChangeListener(this);
			
			c.weightx=0.2;
			c.gridx=2+(i/6)*4;
			c.gridy=0+(i/2)%3;
			c.gridheight=1;
			c.gridwidth=1;
			settings.add(new JLabel("   From"), c);
			
			c.weightx=0.8;
			c.gridx=2+(i/6)*4+1;
			c.gridy=0+(i/2)%3;
			c.gridheight=1;
			c.gridwidth=1;
			settings.add(fields.get(i), c);
			
			c.weightx=0.2;
			c.gridx=2+(i/6)*4+2;
			c.gridheight=1;
			c.gridwidth=1;
			settings.add(new JLabel("to"), c);
			
			c.weightx=0.8;
			c.gridx=2+(i/6)*4+3;
			c.gridheight=1;
			c.gridwidth=1;
			settings.add(fields.get(i+1), c);
		}
		
		c.weightx=0.2;
		c.gridx=2+(i/6)*4+1;
		c.gridy=0+(i/2)%3;
		settings.add(clear, c);
		
		
		
		c.weightx=0.2;
		c.gridx=2+(i/6)*4+3;
		c.gridy=0+(i/2)%3;
		settings.add(validate, c);
		
	}
	
	public class Canvas extends JPanel{ 
		public Canvas(){
			super();
		}
		protected void paintComponent(Graphics g) { 
			super.paintComponent(g);	
			Graphics2D g2 = (Graphics2D)g;    
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
			g2.drawImage(spectrum, 0, 0, spectrum.getWidth(), spectrum.getHeight(), null);		
			//System.out.println("Paint canvas.");
			g2.dispose();
		}
	}
	
	/**
	 * invoked after the display object has been modified
	 * to tell the scroll pane to adjust to the new size
	 * and update the size information in the text fields
	 */
	private void refresh(){
		canvas.setPreferredSize(new Dimension(spectrum.getWidth(),SPEC_HEIGHT));
		//tells its parent component:"I wish I could have a space of that size!"
		//so the scroller says "OK, I can give you a pair of scrollbars."
		canvas.revalidate();
		//"REVALIDATE! Now your dream has come true." Says the scroller. 
		repaint();
	}
	
	private void drawSpectrum(){
		Graphics2D g=spectrum.createGraphics();
		double hue;
		for(int i=0;i<PADDING; i++){
			hue = converter.toStandardHue((float)(K-PADDING+i)/100);//(float)(K-PADDING+i)/100;//
			g.setColor(new Color(Color.HSBtoRGB((float)hue, (float)0.9, (float)0.9)));
			g.fillRect(i*SPEC_STEP, 0, SPEC_STEP, SPEC_HEIGHT);
			g.setColor(Color.white);
			g.drawString("\""+(float)(hue*100), i*SPEC_STEP, SPEC_HEIGHT);
		}
		
		for(int i=0;i<K; i++){
			hue = converter.toStandardHue((float)i/100);//(float)i/100;//
			g.setColor(new Color(Color.HSBtoRGB((float)hue, (float)0.9, (float)0.9)));
			g.fillRect(i*SPEC_STEP+PADDING*SPEC_STEP, 0, SPEC_STEP, SPEC_HEIGHT);
			g.setColor(Color.white);
			g.drawString("\""+(float)(hue*100), i*SPEC_STEP+PADDING*SPEC_STEP, SPEC_HEIGHT);
		}
		
		for(int i=0;i<PADDING; i++){
			hue = converter.toStandardHue((float)i/100);//(float)i/100;//
			g.setColor(new Color(Color.HSBtoRGB((float)hue, (float)0.9, (float)0.9)));
			g.fillRect((i+K+PADDING)*SPEC_STEP, 0, SPEC_STEP, SPEC_HEIGHT);
			g.setColor(Color.white);
			g.drawString("\""+(float)(hue*100), (i+K+PADDING)*SPEC_STEP, SPEC_HEIGHT);
		}
	}

	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		int index = fields.indexOf(e.getSource());
		System.out.println("Property Changed!  "+index);
		/* still throws exceptions
		try {
			fields.get(index).commitEdit();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}*/
		if(index!=-1 && fields.get(index).getValue()!=null){
			Graphics2D g=spectrum.createGraphics();
			g.setColor(Color.RED);
			int value =(int)(100*converter.toMyHue((float)((Number)fields.get(index).getValue()).intValue()/100));
			System.out.println(value);
			if(index%2==0){
				g.drawOval((value+ PADDING)*SPEC_STEP, SPEC_HEIGHT, SPEC_BORDER, SPEC_BORDER);
				g.drawString("s", (value+ PADDING)*SPEC_STEP, 0);
				g.drawString("s"+value, 0, 0);
			}else{
				g.fillOval((value+ PADDING)*SPEC_STEP, SPEC_HEIGHT, SPEC_BORDER, SPEC_BORDER);
				g.setColor(Color.white);
				g.drawString("e", (value+ PADDING)*SPEC_STEP, 0);
			}
			g.dispose();
			
			refresh();
		}
		
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource()==clear){
			for(int i=0; i<fields.size();i++){
				fields.get(i).setValue(null);
			}
		}else if(e.getSource()==validate){
			try {
				PrintWriter wr = new PrintWriter(new FileWriter("data.txt",true));//append!
				//wr.println("DATA FOR COLOR CONVERTER:");
				
				wr.print("SEP_VALUES: ");
				for (JFormattedTextField tf: fields){
					try {
						tf.commitEdit();
					} catch (ParseException e1) {
						System.out.println("Parse Exception!");
					}
					if(tf.getValue()!=null){
						double value =(double)((Number) tf.getValue()).intValue()/100;//(int)(100*converter.toMyHue((float)((Number)tf.getValue()).intValue()/100));
						wr.print(value+", ");
					}
					
					
				}
				wr.println("END. ");
				wr.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			converter=new ColorConverter();
			drawSpectrum();
			refresh();
		}
	}

}
