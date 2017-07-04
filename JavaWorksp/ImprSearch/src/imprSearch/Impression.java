package imprSearch;


import java.awt.Color;
import java.awt.Dimension;
//import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import general.Utils;
import general.Query;

import static java.lang.Math.*;

public class Impression extends Utils implements Query{
	
	private String myID;
	private static int imprCount=0;
	private BufferedImage draft;
	private int[] draftPixel;
	private BufferedImage base;
	private int[] basePixel;
	private int rly;
	//discard the idea of error margin
	/*
	private int[] errorMarginR;
	private int[] errorMarginG;
	private int[] errorMarginB;
	*/
	private boolean[] isTypical;
	private ArrayList<ColorPatch> colorPatches;
	private int height;
	private int width;
	private static final double SD_THRESHOLD = 0.5;
	private static final int SD_AREA_R = 7;
	private static final int PATCH_SIZE_THRESHOLD = 5;
	public static int MAX_PATCH_SIZE = 10000/8;
	public ArrayList<BufferedImage> visualizedBFS = new ArrayList<BufferedImage>();
	
	public Impression(BufferedImage draft, int reliability){
		this.draft = draft;
		rly = reliability;
		height = draft.getHeight();
		width = draft.getWidth();
		draftPixel = new int[width*height];
		draftPixel = draft.getRGB(0, 0, width, height, draftPixel, 0, width);
		myID=imprCount+"Impression-imp";
		imprCount++;
		//getBasicImpr();
		//getErrorMargin();
		getTypicalMap();

		isolateColorPatches();
	}
	
	/**
	 * generates an scaled impression 
	 * Note: should get the isTypical array before scaling because scaling affect 
	 * Since you can not scale an image with "noise" drawn in it, discard width, height params
	 * 
	 *
	 * @param rawDraft
	 * @param reliability
	 * @param name
	 * @throws IOException 
	 */
	public Impression(BufferedImage rawDraft, int reliability, String name, String progressFile) throws IOException{
		/*
		//maintain the ratio
		Dimension d = scaleUniformFitScreen(rawDraft.getWidth(),rawDraft.getHeight(),width,height);		
		//scaledDr = (BufferedImage)draft.getScaledInstance(width,height,SCALE_HINTS);//wrong
		draft = new BufferedImage(d.width,d.height,IMAGE_TYPE);
		draft.getGraphics().drawImage(rawDraft, 0, 0, d.width, d.height, null);
		*/
		PrintWriter wr = new PrintWriter(new FileWriter(progressFile,true),true);
		draft=rawDraft;
		rly = reliability;
		draftPixel = new int[draft.getWidth()*draft.getHeight()];
		draftPixel = draft.getRGB(0, 0, draft.getWidth(), draft.getHeight(), draftPixel, 0, draft.getWidth());
		Dimension comp = Utils.scaleUniformFit(draft.getWidth(), draft.getHeight(), Utils.DRAFT_WIDTH, draft.getHeight());
		this.height = comp.height;
		this.width = comp.width;
		getTypicalMap();//before compressing the raw draft
		
		wr.println("Progress: typical map gotten");
		
		draft = new BufferedImage(width,height,Utils.IMAGE_TYPE);
		draft.getGraphics().drawImage(rawDraft, 0, 0, width,height,null);
		draftPixel = new int[width*height];
		draftPixel = draft.getRGB(0, 0, draft.getWidth(), draft.getHeight(), draftPixel, 0, draft.getWidth());
		
		myID=imprCount+name+"-imp";
		/*
		try {  
			ImageIO.write(draft, "png", new File("draft-"+this.myID+".png"));  
			System.out.println("Draft Saved .");
		} catch (IOException e1) {  
			e1.printStackTrace();  
		} */
		
		imprCount++;
		//getBasicImpr();
		//getErrorMargin(); //Too slow.
		isolateColorPatches();
		
		wr.println("Progress: draft processing finished");
		wr.flush();
		wr.close();
	}
	
	public BufferedImage getDraft(){
		return draft;
	}
	
	/*
	public Impression scale(int width, int height){
		//maintain the ratio
		double ratio = (double)this.height/this.width;
		if((double)height/width<ratio) width=(int) (height/ratio);
		else height = (int)(width*ratio);
		// how can they cast this?
		//scaledDr = (BufferedImage)draft.getScaledInstance(width,height,SCALE_HINTS);
		BufferedImage scaledDr = new BufferedImage(width,height,IMAGE_TYPE);
		scaledDr.getGraphics().drawImage(draft, 0, 0, width, height, null);
		
	
		return new Impression(scaledDr,rly);
	}*/

	public BufferedImage display(){
		//BufferedImage dis = copy(base);
		if(base == null){
			BufferedImage dis = new BufferedImage(width, height, IMAGE_TYPE);
		
			int[] impr = new int[height*width];
		
			for (int i = 0; i < colorPatches.size(); i++){
				ColorPatch cp = colorPatches.get(i);
				int color=0;
				try{
					color= cp.getColor();
				}catch(NullPointerException e){
					
					e.printStackTrace();
					System.out.println("Too small?"+cp.getPatchPixels().length);
				}
				int[] pixels = cp.getPatchPixels();
				for(int j = 0; j<pixels.length; j++){
					impr[pixels[j]]=color;
				}
					//Standard deviation test
					//packRGB((float)(getRed(origin)+(0.5-random())*errorMarginR[i]*50),
					//(float)(getGreen(origin)+(0.5-random())*errorMarginG[i]*50),(float)(getBlue(origin)+(0.5-random())*errorMarginB[i]*50));
			}
		/*
		for (int i=0;i<impr.length;i++){
			if(!isTypical[i])impr[i]=0;
			else impr[i]=draftPixel[i];
		}*/
	
			dis.getRaster().setDataElements(0, 0, width, height, impr);
			base = dis;
		}//why using setPixels sometimes throws ArrayIndexOutOfBound Exception?
		System.out.println("Display impression");
		return base;
	}
	
	public int getColorPatchesCount(){
		return colorPatches.size();
	}
	
	public ColorPatch getColorPatch(int index){
		return colorPatches.get(index);
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	public String getID(){
		return myID;
	}
	
	public double getTypicalRate(){
		int typicalCount=0;
		for(ColorPatch cp: colorPatches){
			try{
				typicalCount+=cp.getPatchPixels().length;
			}catch(NullPointerException e){
				try {
					System.out.println("Null pointer!!!--no cp."+e.getMessage());
					System.in.read();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
			}
		}
		return (double)typicalCount/width/height;
	}
	
	public String toString(){
		//= "Impression: "+myID+" Typical rate£º "+getTypicalRate()
				//+" and "+colorPatches.size()+" color patches.";
		String str=String.format("Impression ID: %s, Draft size: %s*%s, Typical rate: %s, Color patch count: %s. ", 
				myID,draft.getWidth(),draft.getHeight(),getTypicalRate(),colorPatches.size());
		return str;
	}
	
	/**
	 * not used now. Earlier attempt. 
	 */
	private void getBasicImpr(){//should be in the constructor?
		//TODO
		//base = new BufferedImage(width,height,IMAGE_TYPE);
		//base = copy(draft);
		basePixel = copy(draftPixel);
		//void setRGB(int x, int y, int rgb) 
		//Sets a pixel in this BufferedImage to the specified RGB value. 
		//for(int i = rly; i<10;i++){
			// Shall I increase the area to which to apply this method, not the times to apply it?
			applyAveragingFilter(10-rly);
		//}
	}
	
	
	/**
	 * 
	 * @param r the radius in which the colors are averaged, including the central pixel
	 */
	private void applyAveragingFilter(int r) {
		for (int j=0; j<height; j++){
			for(int i=0; i<width; i++){
				//base.setRGB(j, i, averageNeighbors(base,j,i,r));
				basePixel[j*width+i]=averageNeighbors(basePixel,i,j,r);
			}
		}
	}
	
	
	/**
	 * This method is very slow. Early attempt.
	 * @param img the image to which to apply this method
	 * @param x	the x of the central pixel
	 * @param y the y of the central pixel
	 * @param r the radius in which the colors are averaged, including the central pixel
	 * @return the average RGB value near this pixel
	 */
	private int averageNeighbors(BufferedImage img, int x, int y, int r) {
		// TODO average more?
		int rgb = img.getRGB(x, y);
		int sumR= getRed(rgb);
		int sumG = getGreen(rgb);
		int sumB = getBlue(rgb);
		int count = 1;
		for (int i = 1; i<r; i++){
			if(x>i-1){
				rgb = img.getRGB(x-i, y);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(x<width-i-1){
				rgb = img.getRGB(x+i, y);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(y>i-1){
				rgb = img.getRGB(x, y-i);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(y<height-i-1){
				rgb = img.getRGB(x, y+1);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
		}
		return packRGB((float)sumR/count,(float)sumG/count,(float)sumB/count);
	}
	
	
	/**
	 * 
	 * @param pixels the image's RGB pixel to which to apply this method
	 * @param x	the x of the central pixel
	 * @param y the y of the central pixel
	 * @param r the radius in which the colors are averaged, including the central pixel
	 * @return the average RGB value near this pixel
	 */
	private int averageNeighbors(int[] pixels, int x, int y, int r) {
		// TODO average more?
		int rgb = pixels[y*width+x];
		int sumR= getRed(rgb);
		int sumG = getGreen(rgb);
		int sumB = getBlue(rgb);
		int count = 1;
		for (int i = 1; i<r; i++){
			if(x>i-1){
				rgb = pixels[y*width+x-i];//img.getRGB(x-i, y);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(x<width-i-1){
				rgb = pixels[y*width+x+i];//img.getRGB(x+i, y);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(y>i-1){
				rgb = pixels[(y-i)*width+x];//img.getRGB(x, y-i);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
			if(y<height-i-1){
				rgb = pixels[(y+1)*width+x];//img.getRGB(x, y+1);
				sumR+=getRed(rgb);
				sumG+= getGreen(rgb);
				sumB+= getBlue(rgb);
				count++;
			}
		}
		return packRGB((float)sumR/count,(float)sumG/count,(float)sumB/count);
	}
	
	private void getTypicalMap(){
		isTypical=new boolean[width*height];
		int [] original = new int[draft.getWidth()*draft.getHeight()]; 
		for (int j=0; j<draft.getHeight(); j++){
			for(int i=0; i<draft.getWidth(); i++){
				if(((draftPixel[j*draft.getWidth()+i]>>24)&0xff)==0) {
					original[i+j*draft.getWidth()]=Color.GRAY.getRGB();
				}else{
					int[] pixels = getPixelsWithin(i,j,SD_AREA_R);
					int[]pixelsR = new int[pixels.length];
					int[]pixelsG = new int[pixels.length];
					int[]pixelsB = new int[pixels.length];
				
					for (int k = 0; k <pixels.length; k++){
						pixelsR[k]=getRed(pixels[k]);
						pixelsG[k]=getGreen(pixels[k]);
						pixelsB[k]=getBlue(pixels[k]);
					}
				
					double stdR = computeStandardDeviation(pixelsR);
					double stdG = computeStandardDeviation(pixelsG);
					double stdB = computeStandardDeviation(pixelsB);
				
					if((stdR+stdG+stdB)/3.0>SD_THRESHOLD)original[i+j*draft.getWidth()]=Color.GRAY.getRGB();
					else original[i+j*draft.getWidth()]=Color.GREEN.getRGB();
				}
				
			}
		}
		BufferedImage tyMap = new BufferedImage(draft.getWidth(),draft.getHeight(),BufferedImage.TYPE_INT_RGB);
		tyMap.getRaster().setDataElements(0, 0, draft.getWidth(), draft.getHeight(), original);
		/*try {  
			ImageIO.write(tyMap, "png", new File("origTy-"+this.myID+".png"));  
			System.out.println("Saved compt typical map.");
		} catch (IOException e1) {  
			e1.printStackTrace();  
		} */
		BufferedImage compressed = new BufferedImage(width,height,Utils.IMAGE_TYPE);
		compressed.getGraphics().drawImage(tyMap, 0, 0, width,height,null);
		/*try {  
			ImageIO.write(compressed, "png", new File("compTy-"+this.myID+".png"));  
			System.out.println("Saved compt typical map.");
		} catch (IOException e1) {  
			e1.printStackTrace();  
		}  */
		int[] pixels = new int[width*height];
		pixels = compressed.getRGB(0, 0, width, height, pixels, 0, width);
		for(int i=0;i<height*width;i++){
			isTypical[i] = pixels[i]==Color.GREEN.getRGB();
		}
	}
	
	private void isolateColorPatches() {
	
		System.out.println("Start BFS.");
		colorPatches = new ArrayList<ColorPatch>();
		for(int i=0;i<isTypical.length;i++){
			if(isTypical[i]){
				System.out.println("This Pixel at ("+i%width+","+i/width+") is Typical. ");
				ColorPatch cp = findConnectedPixels(i);
				//if(cp!=null) colorPatches.add(cp);
				
				if(cp!=null){
					if(cp.getPatchPixels().length>MAX_PATCH_SIZE){
						ColorPatch[] cps = cp.getSubPatches(MAX_PATCH_SIZE);
						for(ColorPatch subcp :cps){
							colorPatches.add(subcp);
						}
					}else{
						colorPatches.add(cp);
					}
					
				}
				
			}
		}
		
	}


	private ColorPatch findConnectedPixels(int i) {
		ArrayList<Integer> waitList = new ArrayList<Integer>();
		
		waitList.add(i);
		isTypical[i]=false;
		int[] temp = new int[height*width];
		int count =0;
		
		while(true){
			int c = waitList.get(0);
			temp[count]=c;
			count++;
			int x=c%width;
			int y=c/width;
			waitList.remove(0);
		
			if(x+1<width&&isTypical[y*width+x+1]){
				waitList.add(y*width+x+1);
				isTypical[y*width+x+1]=false;
				//count++;//a strange, inconspicuous error will occur if I write count++ here!!! 
			}
			if(x-1>0&&isTypical[y*width+x-1]){
				waitList.add(y*width+x-1);
				isTypical[y*width+x-1]=false;
				//count++;
			}
			if(y+1<height&&isTypical[(y+1)*width+x]){
				waitList.add((y+1)*width+x);
				isTypical[(y+1)*width+x]=false;
				//count++;
			}
			if(y-1>0&&isTypical[(y-1)*width+x]){
				waitList.add((y-1)*width+x);
				isTypical[(y-1)*width+x]=false;
				//count++;
			}
			
			if(waitList.isEmpty())break;
			
		}
		int[] pixels = new int[count];
		int sumR=0;
		int sumG=0;
		int sumB=0;
		for(int i1 = 0; i1<count; i1++){
			pixels[i1] = temp[i1];
			int c=draftPixel[pixels[i1]];
			sumR+=getRed(c);
			sumG+=getGreen(c);
			sumB+=getBlue(c);
		}
		
		//visualizedBFS.add(Tester.visualizeBFS(draft, pixels));
		
		if (pixels.length < PATCH_SIZE_THRESHOLD) return null;//TODO how dare you return null here?
		
		int color = packRGB((float)sumR/count,(float)sumG/count,(float)sumB/count);
		return new ColorPatch(width,height,pixels,color);
	}

	
	
	/*early attempt
	private void getErrorMargin(){
		errorMarginR = new int[width*height];
		errorMarginG = new int[width*height];
		errorMarginB = new int[width*height];
		int r = SD_AREA_R;//TODO
		for (int j=0; j<height; j++){
			for(int i=0; i<width; i++){
				int[] pixels = getPixelsWithin(i,j,r);
				int[]pixelsR = new int[pixels.length];
				int[]pixelsG = new int[pixels.length];
				int[]pixelsB = new int[pixels.length];
				
				for (int k = 0; k <pixels.length; k++){
					pixelsR[k]=getRed(pixels[k]);
					pixelsG[k]=getGreen(pixels[k]);
					pixelsB[k]=getBlue(pixels[k]);
				}
				
				double stdR = computeStandardDeviation(pixelsR);
				double stdG = computeStandardDeviation(pixelsG);
				double stdB = computeStandardDeviation(pixelsB);
				
				errorMarginR[j*width+i] = (int)stdR;//TODO
				errorMarginG[j*width+i] = (int)stdG;
				errorMarginB[j*width+i] = (int)stdB;
			}
		}
		
	}*/
	
	
	private int[] getPixelsWithin(int x, int y, int r) {
		
		int count = 4*r*r-4;
		int[] temp = new int[count];
		count = 0; 
		for(int j = y-r; j < y+r+1; j++){
			if (!(j<0)&&j<draft.getHeight()){
				for(int i = x-r; i<x+r+1; i++){
					if(!(i<0)&&i<draft.getWidth() && !(sqrt((i-x)*(i-x)+(j-y)*(j-y))>r)){
						temp[count]= draftPixel[j*draft.getWidth()+i];
						count++;
					}
				}
			}
		}
		int [] pixels = new int [count];
		for(int i = 0; i<count; i++){
			pixels[i] = temp[i];
		}
		return pixels;
	}

	private int [] copy(int[] a){
		int [] b = new int [a.length];
		int i = 0;
		for(int x :a){
			b[i] = x;
			i++;
		}
		return b;
	}

	private int packRGB(float red, float green, float blue) {
		
		if(red<0)red = 0;
		if(red>0xFF)red = 0xFF;
		if(green<0)green = 0;
		if(green>0xFF)green = 0xFF;
		if(blue<0)blue = 0;
		if(blue>0xFF)blue = 0xFF;
		return (0xFF<<24)|(Math.round(red)<<16)|(Math.round(green)<<8)|(Math.round(blue));
		
	}

	private int getRed(int RGB){
		return (RGB>>16)&0xFF;
	}

	private int getGreen(int RGB){
		return (RGB>>8)&0xFF;
	}

	private int getBlue(int RGB){
		return RGB&0xFF;
	}
	
	//public static final int IMAGE_TYPE=BufferedImage.TYPE_INT_RGB;
	//private static final int SCALE_HINTS=Image.SCALE_DEFAULT;
	
}

