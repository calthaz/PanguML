package testers;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import fastQuerying.ImageEntry;
import general.CandiWindow;
import general.Manager;
import general.Utils;
import imprSearch.Impression;
import imprSearch.ColorConverter;
import imprSearch.ImprManager;

public class GeneralTester {
	private static Scanner sc = new Scanner(System.in);
	private static BufferedImage display = new BufferedImage(800,800,1);
	private static int[] indiColor = {-1,255,0xFF<<8, 0xFF<<16};
	private static JFrame frame = new JFrame("Tester");
	private static JPanel con = (JPanel) frame.getContentPane();

	public static void main(String[] args) {
		//SketchPad pad = new SketchPad();
		
		//frame.add(pad);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(800, 1000));
		frame.pack();
		frame.setVisible(true);
		
		countWindows();
		//int count = 0;
		//showBFS();
		/*
		while (true){
			sc.nextLine();
			String fileName = "d:\\zym\\FRIT\\IMG_070"+(count%10)+".jpg";
			count++;
			try {
				BufferedImage temp = (BufferedImage)ImageIO.read(new File(fileName));
				//display = temp;
				Graphics2D g = display.createGraphics();
				g.drawImage(temp, 0, 0, 600, 600, null);
				Graphics g2 =  con.getGraphics();
				g2.drawImage(display, 0, 0, 1000, 1000, null);
				
				g.dispose();
				g2.dispose();
				
				display.flush();//No difference at all
				
				System.out.println(count+": "+fileName);

			} catch (IOException e) {
				e.printStackTrace();
			}
		The dimensions of the candidate are: 455, 768
The dimensions of the draft are: 100, 62
Sliding step length: 4; Amplication rate step: 1.2; Max fold times: 2
Windows count: 1696!!!!!!!!!!!!!!!!!!!!!
Therefore, there are 1696 windows generated.
Origins: (0, 0), Ratio: 3.79
			}*/
		//for(int i = 0; i<=100;i+=10){
			//getHSBMap("Saturation",i);
			//getHSBMap("Hue",i);
			//getHSBMap("Brightness",i);
		//}
		
		//testMyHue();
		//testConvertBack();
		
		//int x;
		//for(x=0;x<100;x+=5){
		
			//System.out.println(x);
			//System.out.println(SearchManager.func1((double)x));
		//}
		visualizeWavelet();
		
		
		String imageName = "d:\\zym\\AP Java Programming\\Testing Images\\cousin.jpg";
		
		try{
			BufferedImage draft = (BufferedImage)ImageIO.read(new File(imageName));
			int[] pixels = new int [draft.getHeight()*draft.getWidth()];
			draft.getRGB(0, 0, draft.getWidth(), draft.getHeight(), pixels, 0, draft.getWidth());
			int[] hue = new int[pixels.length];
			for (int i = 0; i<pixels.length; i++){
				int c=pixels[i];
				float[] HSB = new float[3];
				HSB = Color.RGBtoHSB((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, HSB);
				hue[i] = (int) (HSB[0]*100);
			}
			
			int[] frequency = Utils.histogramFor(hue,100);
			System.out.println("I am here!");
			printArray(frequency);
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	private static void showBFS(){
		String imprName = "d:\\zym\\AP Java Programming\\Imprs\\sketches\\kong-im.jpg";//debug04172.png
		try{
			BufferedImage draft = (BufferedImage)ImageIO.read(new File(imprName));
			Graphics g2 =  con.getGraphics();
			g2.drawImage(draft, 0, 0, null);
			sc.nextLine();
			Impression imp = new Impression(draft,5,"NA");
			g2.drawImage(imp.getDraft(), 0, 0, null);
			sc.nextLine();
			System.out.println(imp);
			System.out.println("It has "+imp.getColorPatchesCount());
			
			//g2.drawImage(imp.display(), 0, 0, null);
			g2.dispose();
			while (true){
				int index = sc.nextInt();
				Graphics g =  con.getGraphics();
				g.drawImage(imp.visualizedBFS.get(index), 0, 0, null);
				g.dispose();
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private static BufferedImage copy(BufferedImage old){
		//TODO: improve efficiency
		BufferedImage img = new BufferedImage(old.getWidth(),old.getHeight(),Impression.IMAGE_TYPE);
		for (int i=0; i<old.getHeight(); i++){
			for(int j=0; j<old.getWidth(); j++){
				img.setRGB(j, i, old.getRGB(j,i));
			}
		}
		return img;
	}
	
	
		/*
		File f = new File("d:\\zym\\AP Java Programming\\Testing images\\Frankfurt.jpg");
		
		try {
			BufferedImage temp = (BufferedImage)ImageIO.read(f);
			Dimension d = ImprUtils.scaleUniformFitScreen(temp.getWidth(),temp.getHeight(),SketchPad.CANVAS_WIDTH,SketchPad.PAD_HEIGHT);	
			
			BufferedImage display = new BufferedImage(d.width,d.height,Impression.IMAGE_TYPE);
		
			display.getGraphics().drawImage(temp, 0, 0, d.width, d.height, null);
			
			int[] oldPixels = new int[d.width*d.height];
			
			display.getRGB(0, 0, d.width, d.height, oldPixels, 0, d.width);
			
			int[] pixels = ImprUtils.applyHistogramEqualization(oldPixels);
			
			display.getRaster().setDataElements(0, 0, d.width, d.height, pixels);
			
			pad.displayImage(display);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
		
	
	public static BufferedImage visualizeBFS(BufferedImage draft, int[] coordinates){
		int w = draft.getWidth();
		BufferedImage result = copy(draft);
		for(int i=0; i<coordinates.length; i++){
			result.setRGB(coordinates[i]%w, coordinates[i]/w,indiColor[1]);
		}
		return result;
	}
	
	public static void printArray(int[] arr){
		for(int i: arr){
			System.out.println(i);
		}
	}
	
	public static BufferedImage getHSBMap(String mode, int value){
		BufferedImage map = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		for(int y=0; y<map.getHeight();y++){
			for(int x=0; x<100; x++){
				switch(mode){
				case "Hue":
					int color = Color.HSBtoRGB((float)value/100, (float)x/100,(float)y/100);
					map.setRGB(x, y, color);
					break;
				case "Saturation":
					int color1 = Color.HSBtoRGB((float)x/100,(float)value/100,(float)y/100);
					map.setRGB(x, y, color1);
					break;
				case "Brightness":
					int color2 = Color.HSBtoRGB((float)x/100,(float)y/100,(float)value/100);
					map.setRGB(x, y, color2);
					break;
				default:
				}
			}
		}
		File out = new File("d:\\zym\\AP Java Programming\\Testing Images\\"+mode+value+".png");
		try {  
			ImageIO.write(map, "png", out);  
			System.out.println("Saved as:"+out.getName());
		} catch (IOException e1) {  
			e1.printStackTrace();  
		}  
		return map;
	}
	
	private static void testMyHue(){
		File f = new File("d:\\zym\\AP Java Programming\\HBS test\\saturation100.png");
		ColorConverter converter = new ColorConverter();
		try {
			BufferedImage img = ImageIO.read(f);
			for(int x = 0; x< 100; x++){
				int color = img.getRGB(x, 99);
				float[] hsb = new float[3];
				hsb = Color.RGBtoHSB((color>>16)&0xff, (color>>8)&0xff, color&0xff, hsb);
				System.out.println((int)(hsb[0]*100)+"is converted to"+(int)(converter.toMyHue(hsb[0])*100) );
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void testConvertBack(){
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		ColorConverter converter = new ColorConverter();
		for(int y=0; y<img.getHeight();y++){
			for(int x=0; x<100; x++){
				
					int color1 = Color.HSBtoRGB(converter.toStandardHue((double)x/100),1,(float)y/100);
					img.setRGB(x, y, color1);

				
			}
		}
		File out = new File("d:\\zym\\AP Java Programming\\Testing Images\\"+"convert back 1"+".png");
		
		try {  
			ImageIO.write(img, "png", out);  
			System.out.println("Saved as:"+out.getName());
		} catch (IOException e1) {  
			e1.printStackTrace();  
		}  
	}
	
	private static void visualizeWavelet(){
		ImageEntry entry = new ImageEntry("d:\\zym\\Java Programming\\Imprs\\sketches\\berlin-dom.jpg","berlin-dom");
		float[][] mat1=Utils.normalize1(entry.getMatrixH());
		float[][] mat2=Utils.normalize1(entry.getMatrixS());
		float[][] mat3=Utils.normalize1(entry.getMatrixB());
		int w = ImageEntry.W;
		int[] arr = new int[w*ImageEntry.H];
		for(int i=0; i<arr.length; i++){
			int c = (int)(mat1[i/w][i%w]*255);
			arr[i]=((0xFF<<24)|(c<<16)|(c<<8)|(c));
		}
		BufferedImage fig=new BufferedImage(w,ImageEntry.H,BufferedImage.TYPE_INT_RGB);
		fig.setRGB(0, 0, w, ImageEntry.H, arr, 0, w);
		File out = new File("d:\\zym\\Java Programming\\"+"ber"+"1"+".png");
		try {  
			ImageIO.write(fig, "png", out);  
			System.out.println("Saved as:"+out.getName());
		} catch (IOException e1) {  
			e1.printStackTrace();  
		}  
		
		for(int i=0; i<arr.length; i++){
			int c = (int)(mat2[i/w][i%w]*255);
			arr[i]=((0xFF<<24)|(c<<16)|(c<<8)|(c));
		}
		fig.setRGB(0, 0, w, ImageEntry.H, arr, 0, w);
		out = new File("d:\\zym\\Java Programming\\"+"ber"+"2"+".png");
		try {  
			ImageIO.write(fig, "png", out);  
			System.out.println("Saved as:"+out.getName());
		} catch (IOException e1) {  
			e1.printStackTrace();  
		} 
		
		for(int i=0; i<arr.length; i++){
			int c = (int)(mat3[i/w][i%w]*255);
			arr[i]=((0xFF<<24)|(c<<16)|(c<<8)|(c));
		}
		
		fig.setRGB(0, 0, w, ImageEntry.H, arr, 0, w);
		out = new File("d:\\zym\\Java Programming\\"+"ber"+"3"+".png");
		try {  
			ImageIO.write(fig, "png", out);  
			System.out.println("Saved as:"+out.getName());
		} catch (IOException e1) {  
			e1.printStackTrace();  
		}
	}
	
	private static void countWindows(){
		
		BufferedImage candi, draft;
		try {
			Manager man = new ImprManager("gui");
			BufferedImage origCandi = ImageIO.read(new File("D:\\zym\\Java Programming\\Testing Images\\le fifre.jpg"));
			
			Dimension d = Utils.scaleUniformFit(origCandi.getWidth(), origCandi.getHeight(), 150, 150);
			BufferedImage small = new BufferedImage(d.width,d.height,Utils.IMAGE_TYPE);
			Graphics2D g2d = small.createGraphics();
			g2d.drawImage(origCandi, 0, 0, d.width, d.height, null);
			g2d.dispose();
			Graphics2D g2 = origCandi.createGraphics();
			g2.drawImage(small, 0, 0, origCandi.getWidth(), origCandi.getHeight(), null);
			g2.dispose();
			
			candi = new BufferedImage(900,900,BufferedImage.TYPE_INT_RGB);
			Graphics2D g1 = candi.createGraphics();
			g1.drawImage(origCandi, 0, 0, null);
			g1.dispose();
			
			draft = ImageIO.read(new File("D:\\zym\\Java Programming\\Imprs\\sketches\\draft-0My Impression-imp.png"));
			//draft = ImageIO.read(new File("D:\\zym\\Java Programming\\Imprs\\sketches\\sketchessunflower-im-ul2.png"));
			System.out.println(String.format("The dimensions of the candidate are: %s, %s", candi.getWidth(), candi.getHeight()));
			System.out.println(String.format("The dimensions of the draft are: %s, %s", draft.getWidth(), draft.getHeight()));
			
			ArrayList<CandiWindow> winds = man.getEmptyWindows(origCandi, draft);
			System.out.println(man.printSettings());
			 
			
			
			System.out.println(String.format("Therefore, there are %s windows generated.",winds.size()));
			
			Graphics2D g =(Graphics2D) con.getGraphics();
			g.drawImage(candi, 0, 0, null);
			g.setColor(Color.yellow);
			g.fillArc((int)(man.getCenterX()*origCandi.getWidth()), (int)(man.getCenterY()*origCandi.getHeight()), 5, 5, 0,360);
			g.setColor(Color.white);
			for(int i=0; i<winds.size(); i+=1){
				CandiWindow cw = winds.get(i);
				double ratio = cw.getAmplificationRatio();
				int x1=cw.startX();
				int y1=cw.startY();
				//g.drawRect(x1, y1, (int)(draft.getWidth()*ratio), (int)(draft.getHeight()*ratio));
				g.fillArc((int)(x1+(draft.getWidth()*ratio)/2), (int)(y1+(draft.getHeight()*ratio)/2), 5, 5, 0,360);
				//System.out.println(String.format("Origins: (%s, %s), Ratio: %s" , cw.startX(),cw.startY(), cw.getAmplificationRatio()));
				if(cw.endX()>=candi.getWidth()||cw.endY()>=candi.getHeight()){
					System.out.println(String.format("Dimensions of the source: %s * %s; Clip end point: (%s,%s)", 
					candi.getWidth(), candi.getHeight(),cw.endX(),cw.endY()));
					
				}
					sc.nextLine();
			}
			g.dispose();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

}
