/**
 * 
 */
package testers;

import static java.lang.Math.abs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import general.Utils;
import imprSearch.Impression;
import imprSearch.ColorConverter;
import imprSearch.ImprManager;

/**
 * this class shows how bad I extract main colors from an image
 * @author asus-pc
 *
 */
public class MainColorExtractor extends JFrame implements ActionListener{
	
	private JButton load, analyse;
	private JLabel currentFile;
	private BufferedImage chart, myPic;
	private static ColorConverter converter = new ColorConverter();
	private static int K=100;
	private static int CHART_WIDTH = 200;
	private static int CHART_HEIGHT = 600;
	Color[] colors;
	private Container con;
	private ChartRenderer crd;
	private Canvas can;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
                new MainColorExtractor();
               
            }
        });
	}
	
	public MainColorExtractor(){
		super("Main Color Extractor");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
	    this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		con = this.getContentPane();
	    con.setLayout(new BorderLayout());
	    
	    JPanel filePane = new JPanel();
	    filePane.setLayout(new FlowLayout());
	    load = new JButton("Load an Image");
	    analyse = new JButton("Analyse Image");
	    filePane.add(load);
	    filePane.add(analyse);
	    load.addActionListener(this);
	    analyse.addActionListener(this);
	    this.add(filePane,BorderLayout.SOUTH);
	    
	    currentFile=new JLabel("");
	    this.add(currentFile, BorderLayout.NORTH);
	    
	    chart = new BufferedImage(CHART_WIDTH,CHART_HEIGHT,BufferedImage.TYPE_INT_ARGB);
	    
	    crd = new ChartRenderer();
	    crd.setPreferredSize(new Dimension(CHART_WIDTH, CHART_HEIGHT));
		this.add(crd, BorderLayout.EAST);
		
		can = new Canvas();
		can.setPreferredSize(new Dimension(600,600));
		this.add(can);
		
	    this.pack();
	    this.setVisible(true);
		
	}
	
	@SuppressWarnings("serial")
	public class ChartRenderer extends JPanel{ 
		
		protected void paintComponent(Graphics g) { 
			super.paintComponent(g);//Why?
			if(chart!=null){
				Graphics2D g2 = (Graphics2D)g;    
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
				g2.drawImage(chart, 0, 0, chart.getWidth(), chart.getHeight(), null);
				
			}
			
		}
	}
	
	@SuppressWarnings("serial")
	public class Canvas extends JPanel{ 
		
		protected void paintComponent(Graphics g) { 
			super.paintComponent(g);//Why?
			if(myPic!=null){
				Graphics2D g2 = (Graphics2D)g;    
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
				g2.drawImage(myPic, 0, 0, this.getWidth(), this.getHeight(), null);
				
			}
			
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource()==load){
			
			JFileChooser chooser = new JFileChooser(Utils.DEFAULT_CANDI_FOLDER);
			
			FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif","png");//TODO really?
			chooser.setFileFilter(filter);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);//.DIRECTORIES_ONLY
			
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = chooser.getSelectedFile();
	            //File folder = chooser.getCurrentDirectory();
	            //fileNames.addElement(file.getPath());
	            
	            try {
	            	
					BufferedImage temp = (BufferedImage)ImageIO.read(file);
					Dimension d = Utils.scaleUniformFit(temp.getWidth(),temp.getHeight(),con.getWidth(),con.getHeight());
					myPic = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
					myPic.getGraphics().drawImage(temp, 0, 0, d.width, d.height, null);
					can.repaint();
					chart=null;
					System.out.println("successfully load an image.");
					currentFile.setText(file.getPath());
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	            
	        } else {
	        	System.out.println("Open command cancelled by user." );
	        }
			
		}else if(e.getSource()==analyse){
			if(myPic!=null){
				System.out.println("Analysing...");
				int[] cpPixels=new int[myPic.getHeight()*myPic.getWidth()];
				cpPixels=myPic.getRGB(0, 0, myPic.getWidth(), myPic.getHeight(), cpPixels, 0, myPic.getWidth());
				method1(cpPixels);
				drawColorChart();
			}
		}
	}

	private void method1(int[] cpPixels){
		//corresponding pixels' colors 
		float[] cpPairH = new float[cpPixels.length];
		float[] cpPairS = new float[cpPixels.length];
		float[] cpPairB = new float[cpPixels.length];
		//int imColor = cp.getColor();
		//int sumH=0;
		
		for(int k=0; k<cpPixels.length; k++){
			
			int c=cpPixels[k];
			
			float[] HSB = new float[3];
			
			HSB = Color.RGBtoHSB((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, HSB);
			
			cpPairH[k] = (float) (converter.toMyHue(HSB[0])*K);
			cpPairS[k] = HSB[1]*K;
			cpPairB[k] = HSB[2]*K;
		}
		
		int[] histogram= Utils.histogramFor(cpPairH, K);
		int[] typicalHues = Utils.findLocalMaximums(histogram,cpPixels.length/100, (int)7.0);
		
		ArrayList<int[]> typicalColors=new ArrayList<int[]>();
		
		
		
		for(int hue:typicalHues){
			ArrayList<Integer> coordinates = new ArrayList<Integer>();
			ArrayList<Integer> saturation = new ArrayList<Integer>();
			for(int s = 0; s<cpPixels.length; s++){
				if(abs(cpPairH[s]-hue)<3.5){
					saturation.add((int) cpPairS[s]);
					coordinates.add(s);
				
				}
			}
			
			int[] hSat= Utils.histogramFor(saturation, K);
			int[] typicalSaturations = Utils.findLocalMaximums(hSat,cpPixels.length/150, (int)10.0);
			
			for(int sat:typicalSaturations){
				ArrayList<Integer> brightness = new ArrayList<Integer>();
				for(int b=0; b<coordinates.size();b++){
					if(abs(cpPairS[b]-sat)<5.0){
						brightness.add((int) cpPairB[b]);
					}
				}
				
				int[] hBri= Utils.histogramFor(brightness, K);
				int[] typicalBrightness = Utils.findLocalMaximums(hBri, cpPixels.length/200, (int)10.0);
				
				for(int bri: typicalBrightness){
					int[] hsb = new int[3];
					hsb[0]=hue;
					hsb[1]=sat;
					hsb[2]=bri;
					typicalColors.add(hsb);
				}
			}

		}
		
		colors = new Color[typicalColors.size()];
		for(int i=0; i<typicalColors.size(); i++){
			colors[i] = new Color(Color.HSBtoRGB((float)typicalColors.get(i)[0]/100, (float)typicalColors.get(i)[1]/100, (float)typicalColors.get(i)[2]/100));
		}
	}
	
	private void method2(int[] cpPixels){
		for(int i=0;i<cpPixels.length; i++){
			int c = cpPixels[i];
			int translated = ((c>>16)&0xff)*256*256+( (c>>8)&0xff)*256+(c&0xff);
			cpPixels[i] = translated;
		}
		
		int[] histogram= Utils.histogramFor(cpPixels, 256*256*256);
		int[] temp = Utils.findLocalMaximums(histogram,cpPixels.length/50, (int)20.0);
		System.out.println("I am here...");
		colors = new Color[temp.length];
		for(int i=0; i<temp.length; i++){
			colors[i] = new Color(temp[i]/(256*256),(temp[i]%(256*256))/256, temp[i]%256);
		}
	}
	
	
	private void drawColorChart() {
		// TODO Auto-generated method stub
		chart = new BufferedImage(CHART_WIDTH,CHART_HEIGHT,BufferedImage.TYPE_INT_ARGB);
		
		int rectHeight=CHART_HEIGHT/colors.length;
		Graphics2D g = chart.createGraphics();
		for(int i=0; i<colors.length; i++){
			g.setColor(colors[i]);
			g.fillRect(0, i*rectHeight, CHART_WIDTH, rectHeight);
		}
		g.dispose();
		
		crd.repaint();
		System.out.println("color chart drawn");
	}

}
