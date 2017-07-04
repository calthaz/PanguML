package imprSearch;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import javax.imageio.ImageIO;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.filechooser.FileFilter;
//import javax.swing.filechooser.FileNameExtensionFilter;
//import javax.swing.table.AbstractTableModel;

import general.Candidate;
import general.Utils;
import general.CandiWindow;
import general.Manager;
import general.Query;
import general.Manager.MyComboBoxModel;
import general.Manager.MyTableModel;

/**
 * This guy uses the coarse method I write on my own to do the searching task.
 * resource link: 
 * Implement combo box model: http://stackoverflow.com/questions/2305348/use-string-list-as-source-of-combo-box/2305474#2305474
 */
public class ImprManager extends Manager {
	
	
	//private ArrayList<ImprCandidate> candiList = new ArrayList<ImprCandidate>();
	//public AbstractTableModel tableModel = new ImprTableModel();//
	//private ArrayList<String> imprList = new ArrayList<String>();
	//public ImprComboBoxModel cbModel = new ImprComboBoxModel();
	
	//private HashMap<String, Impression> imprStock = new HashMap<String, Impression>();
	//private BufferedImage imprDraft;
	private int rly = 5;
	//private String imprID;
	
	private static final int CANDI_AVERAGE_THRESHOLD=80;
	private static final double CANDI_SD_THRESHOLD=0.5;//0.5 when image is 1000x800, 0.7 when the image is 500x500
	private static final double CANDI_H_THRESHOLD=7;
	private static final double CANDI_S_THRESHOLD=30;
	private static final double CANDI_B_THRESHOLD=35; //thresholds
	private static final int K = 100;
	public static int bLimit = 30; 
	public static int sLimit = 30;
	
	//public int searchMethod;
	public static final int RGB_SEARCH = 0;
	public static final int HSB_SEARCH = 1;
	//public int preprocessing;
	public static final int NONE = 0;
	public static final int EQUALIZE = 1;
	public static final int SMOOTH =2;//TODO implement later
	public static final int COMPRESS=3;
	private static final int COMPRESS_DIMEN = 150;
	
	private static ColorConverter converter = new ColorConverter();
	private PrintWriter wr=null;
	
	
	
	//private String settings; 
	
	
	public ImprManager(){
		this.mode="gui";
		candiList = new ArrayList<Candidate>();
		imprList = new ArrayList<String>();
		imprStock =  new HashMap<String, Query>();
		tableModel = new MyTableModel();
		cbModel= new MyComboBoxModel();
		
		addExamples();
		
		searchMethod = HSB_SEARCH;
		preprocessing = COMPRESS;
		
	    tableModel.fireTableDataChanged();
	    
	   //settings = String.format("Searching Method: %s; Pre-processing: %s; Maximum patch size: %s.",
				//searchMethod,preprocessing, Impression.MAX_PATCH_SIZE);
	}
	
	public ImprManager(String mode){
		if(mode.equals("text")){
			this.mode=mode;
			candiList = new ArrayList<Candidate>();
			imprList = new ArrayList<String>();
			imprStock =  new HashMap<String, Query>();
		}else{
			this.mode="gui";
			candiList = new ArrayList<Candidate>();
			imprList = new ArrayList<String>();
			imprStock =  new HashMap<String, Query>();
			tableModel = new MyTableModel();
			cbModel= new MyComboBoxModel();
			addExamples();
			tableModel.fireTableDataChanged();
		}
		
		searchMethod = HSB_SEARCH;
		preprocessing = COMPRESS;
			    	    
		
	}
	
	private void addExamples(){
		candiList.add(new ImprCandidate("berlin.jpg"));
		candiList.add(new ImprCandidate("tower.jpg"));
		candiList.add(new ImprCandidate("fruits.jpg"));
		candiList.add(new ImprCandidate("river.jpg"));
		candiList.add(new ImprCandidate("statue.jpg"));
		candiList.add(new ImprCandidate("house.jpg"));
		candiList.add(new ImprCandidate("impression example 1.jpg"));
		candiList.add(new ImprCandidate("impression example 2.png"));
		candiList.add(new ImprCandidate("impression example 3.png"));
		//loadFiles(new File("d:\\zym\\Java Programming\\Testing Images"));
		
		/*
		try{
			String tempID = "Impression example";
			imprDraft = (BufferedImage)ImageIO.read(new File("berlin-im.jpg"));
			imprStock.put(tempID, new Impression(ImprUtils.IMAGE_WIDTH, ImprUtils.IMAGE_HEIGHT,imprDraft,rly,imprID));
			imprList.add(tempID);
		} catch (IOException e){
			e.printStackTrace();
		}*/
		
	}
	
	
	
	@Override
	public void run() {
		
		try {
			findCandidates();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void findCandidates() throws IOException {
		 try {
				wr=new PrintWriter(new FileWriter("searching log.txt",true));
				
		} catch (IOException e) {
				e.printStackTrace();
		}
		
		PrintWriter progress = new PrintWriter(new FileWriter(progressFile,true),true);
		
		System.out.println("Processing image...");
		if(wr!=null)wr.println("\n------"+Calendar.getInstance().getTime()+"------");
		//Time processing time.
		int time = (int) System.currentTimeMillis();
		
		Impression imp = new Impression(imprDraft,rly,imprID,progressFile);
		imprID = imp.getID();
		System.out.println("Current Impression: "+imp);
		
		if(wr!=null)wr.println("Current Impression: "+imp);
		if(imp.getColorPatchesCount()==0){
			if(wr!=null)wr.println("Empty imp. Searching Ends.");
			System.out.println("Empty imp. Searching Ends.");
			progress.println("Fatal Error: Empty Draft");
			progress.flush();
			progress.close();
			return; 
		}
		
		time = (int)System.currentTimeMillis()-time;
	    System.out.println("Processing has taken "+time+"ms.");
	    if(wr!=null)wr.println("Processing has taken "+time+"ms.");
	    
	    imprStock.put(imprID, imp);
	    if(mode.equals("gui")){
	    	cbModel.addElement(imprID);
	    }
	    
	    
	    progress.println("Progress: Start comparing");
	    
	    if(wr!=null)wr.println(printSettings());
	    int windowCount=0;
	    int fileCount = 0;
	    
		System.out.println("Starts comparing...");
 	    time = (int) System.currentTimeMillis();
 	    
		int patchCount = imp.getColorPatchesCount();
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		for (int i=0; i<candiList.size(); i++){ //for each candidate
			
			ImprCandidate curCandi = (ImprCandidate) candiList.get(i);
		
			File f = new File(curCandi.getPath());
			curCandi.setPairedImprID(imp.getID());
			System.out.println("Current File: "+f.getPath());
			fileCount++;
			
			try {
				
				BufferedImage origCandi = (BufferedImage)ImageIO.read(f);
				if(preprocessing == COMPRESS){
					Dimension d = Utils.scaleUniformFit(origCandi.getWidth(), origCandi.getHeight(), COMPRESS_DIMEN, COMPRESS_DIMEN);
					BufferedImage small = new BufferedImage(d.width,d.height,Utils.IMAGE_TYPE);
					System.out.println(String.format("Small Candi dimension: %s, %s", small.getWidth(), small.getHeight()));
					Graphics2D g2d = small.createGraphics();
					g2d.drawImage(origCandi, 0, 0, d.width, d.height, null);
					g2d.dispose();
					Graphics2D g2 = origCandi.createGraphics();
					g2.drawImage(small, 0, 0, origCandi.getWidth(), origCandi.getHeight(), null);
					g2.dispose();
					System.out.println(String.format("Candi dimension: %s, %s", origCandi.getWidth(), origCandi.getHeight()));
					/*try {  
						ImageIO.write(origCandi, "png", new File("over-compressed-"+f.getName()+"-1.png"));  
						System.out.println("compressed candi Saved .");
					} catch (IOException e1) {  
						e1.printStackTrace();  
					} */
				}
				
				ArrayList<CandiWindow> winds = getEmptyWindows(origCandi,imp.getDraft());
				
				for(CandiWindow cw:winds){
					int simPixelCount1 = 0;
					int simPixelCount2 = 0;
					int validArea=0;
					windowCount++;
					
					BufferedImage candiImage = cw.getClip(origCandi);//new BufferedImage(width,height,Impression.IMAGE_TYPE);
		
					//candiImage.getGraphics().drawImage(origCandi, 0, 0, width, height, null);
					int[] candiPixels = new int[width*height];
					candiImage.getRGB(0, 0, width, height, candiPixels, 0, width);
					if(preprocessing==EQUALIZE)candiPixels = Utils.applyHistogramEqualization(candiPixels);//TODO: preprocessing
				
					int[] tempIndices = new int[patchCount];
					
					//System.in.read();
					
					switch (searchMethod){
				
						case RGB_SEARCH:
					
							//System.out.println("RGB Search--------------------");
						
							for (int j=0; j<patchCount; j++){ 
								//for each color patch in the impression
								ColorPatch cp = imp.getColorPatch(j);
								int[] cpPixels = cp.getPatchPixels();
							
								double RGBinfo[] = analysePatchRGB(cpPixels, candiPixels);
					
								System.out.println("Patch No."+j+" "+cp+
										"And the color in candi in RGB is "+RGBinfo[0]+", "+RGBinfo[2]+", "+RGBinfo[4]+". And the standard deviation is "+RGBinfo[1]+", "+RGBinfo[3]+", "+RGBinfo[5]+".");
								if(isQualified(RGBinfo[0], RGBinfo[2], RGBinfo[4],cp.getColor(),RGBinfo[1], RGBinfo[3], RGBinfo[5])){
									tempIndices[j]= j;
									System.out.println("Win this patch!");
									simPixelCount1+=cpPixels.length;
								}else {
									System.out.println("Lose this patch!");
									tempIndices[j]=-1;
								}			

								validArea+=cpPixels.length;
							}
							break;
						
						case HSB_SEARCH:
							//System.out.println("HSB Search--------------------");
					
							for (int j=0; j<patchCount; j++){ 
								//for each color patch in the impression
								ColorPatch cp = imp.getColorPatch(j);
								int[] cpPixels = cp.getPatchPixels();
					
								//double[] HSBinfo = analysePatchHSB(cpPixels, candiPixels);
								int[][] typicalColors = analysePatchHSB(cpPixels, candiPixels);
					
								//System.out.println("Patch No."+j+" "+cp+
									//"And the color in candi in HSB is "+HSBinfo[0]+", "+HSBinfo[1]+", "+HSBinfo[2]+".");
								//System.out.print("Patch No."+j+" "+cp+" and the typical colors in candi are ");
								for(int[] value : typicalColors) {
									//System.out.print("("+value[0]+", "+value[1]+", "+value[2]+")");
								}
								if(isQualified(cp.getColor(),typicalColors)){//(HSBinfo[0],HSBinfo[1], HSBinfo[2], cp.getColor())
									tempIndices[j]= j;
									//System.out.println("Win this patch!");
									simPixelCount2+=cpPixels.length;
								}else {
									//System.out.println("Lose this patch!");
									tempIndices[j]=-1;
								}
					
								validArea+=cpPixels.length;
							}
							break;
						
						default:
					}
					//Double sim = new Double((double)similarity/validArea);
					double sim = (double)(simPixelCount1+simPixelCount2)/validArea;
					//System.out.println("Similarity: "+sim);
					cw.score=sim;
					if(sim>=curCandi.getWinner().score){
						//if(sim!=0&&sim!=curCandi.getWinner().score)System.in.read();
						curCandi.SetWinner(cw);
						curCandi.setPatchesWined(packedArray(tempIndices,-1));
						System.out.println("Similarity: "+sim);
					}
					
				}
				curCandi.setScore(curCandi.getWinner().score);
				if(this.mode.equals("gui")){
					tableModel.fireTableCellUpdated(i, 1);
					tableModel.fireTableCellUpdated(i, 2);
				}
				System.out.println(curCandi);
				if(wr!=null)wr.println(curCandi);
			} catch (IOException e) {
				e.printStackTrace();
			}
			progress.println("Progress: processing file No."+(i+1)+" out of "+candiList.size());
		}
		System.out.println("Sorting results...");
		Collections.sort(candiList);
		if(this.mode.equals("gui")){
			tableModel.fireTableDataChanged();
		}
		else{
			System.out.println(candiList);
			 PrintWriter result = new PrintWriter(new FileWriter(resultFile,true),true);
			 result.println(candiList);
			 result.close();
		}
		
		/*
		 * sort Javadoc
		public void sort(Comparator<? super E> c)

		Description copied from interface: List

		Sorts this list according to the order induced by the specified Comparator. 
		All elements in this list must be mutually comparable using the specified comparator 
		(that is, c.compare(e1, e2) must not throw a ClassCastException for any elements e1 and e2 in the list). 

		If the specified comparator is null 
		then all elements in this list must implement the Comparable interface 
		and the elements' natural ordering should be used. 
		 */
		time = (int)System.currentTimeMillis()-time;
 	    System.out.println("Total file count: "+fileCount+". Total window count: "+windowCount+". Comparing has taken "+time+"ms.");
 	    if(wr!=null)wr.println("Total file count: "+fileCount+". Total window count: "+windowCount+". Comparing has taken "+time+"ms.");
 	    wr.close();
 	    progress.println("Finished: Comparing finished in "+time+" seconds");
 	    progress.close();
	}
	
	public int getPreprocessing() {
		return preprocessing;
	}

	public void setPreprocessing(int preprocessing) {
		this.preprocessing = preprocessing;
	}

	/**
	 * Analyze the color basing on RGB component values
	 * @param cpPixels stores the coordinates of pixels in a color patch in the impression
	 * @param candiPixels stores all the pixels' colors in an array as long as width*height of the candidate
	 * @return averaged component values and standard deviation respectively
	 */
	private double[] analysePatchRGB(int[] cpPixels, int[] candiPixels){
		//corresponding pixels' colors 
		int[] cpPairR = new int[cpPixels.length];
		int[] cpPairG = new int[cpPixels.length];
		int[] cpPairB = new int[cpPixels.length];
		//int imColor = cp.getColor();
		int sumR=0;
		int sumG=0;
		int sumB=0;
		for(int k=0; k<cpPixels.length; k++){
			
			int c=candiPixels[cpPixels[k]];
			
			cpPairR[k] = (c>>16)&0xFF;
			cpPairG[k] = (c>>8)&0xFF;
			cpPairB[k] = c&0xFF;
			
			sumR+=(c>>16)&0xFF;
			sumG+=(c>>8)&0xFF;
			sumB+=c&0xFF;
			
		}
		sumR=sumR/cpPixels.length;
		sumG=sumG/cpPixels.length;
		sumB=sumB/cpPixels.length;
		//int imColorR=cp.getRed();
		//int imColorG=cp.getGreen();
		//int imColorB= cp.getBlue();
		
		double stdR = Utils.computeStandardDeviation(cpPairR);
		double stdG = Utils.computeStandardDeviation(cpPairG);
		double stdB = Utils.computeStandardDeviation(cpPairB);
		
		double[] result={sumR,stdR,sumG,stdG,sumB,stdB};
	
		return result;
	}
	
	/**
	 * Analyze the color basing on HSB component values
	 * @param cpPixels stores the coordinates of pixels in a color patch in the impression
	 * @param candiPixels stores all the pixels' colors in an array as long as width*height of the candidate
	 * @return averaged component values
	 */
	private int[][] analysePatchHSB(int[] cpPixels, int[] candiPixels){
		//corresponding pixels' colors 
		float[] cpPairH = new float[cpPixels.length];
		float[] cpPairS = new float[cpPixels.length];
		float[] cpPairB = new float[cpPixels.length];
		//int imColor = cp.getColor();
		//int sumH=0;
		
		for(int k=0; k<cpPixels.length; k++){
			
			int c=candiPixels[cpPixels[k]];
			
			float[] HSB = new float[3];
			
			HSB = Color.RGBtoHSB((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, HSB);
			
			cpPairH[k] = (float) (converter.toMyHue(HSB[0])*K);
			if((converter.toMyHue(HSB[0])*K)>K||(converter.toMyHue(HSB[0])*K)<0) System.out.println(HSB[0]+"  --  "+ (float) (converter.toMyHue(HSB[0])*K) );
			cpPairS[k] = HSB[1]*K;
			cpPairB[k] = HSB[2]*K;
			
			//sumH+=HSB[0]*K;
			//sumS+=HSB[1]*K;
			//sumB+=HSB[2]*K;
			
		}
		
		int[] histogram= Utils.histogramFor(cpPairH, K);
		int[] typicalHues = Utils.findLocalMaximums(histogram,cpPixels.length/4, (int)CANDI_H_THRESHOLD);
		int[][] typicalColors=new int[typicalHues.length][3];
		
		//find the average brightness and saturation of those pixels that have hues near typical values
		for(int j = 0; j<typicalHues.length;j++){
			int sumS=0;
			int sumB=0;
			int count =0;
			for(int i = 0; i<cpPixels.length; i++){
				if(abs(cpPairH[i]-typicalHues[j])<CANDI_H_THRESHOLD){
					sumS+=cpPairS[i];
					sumB+=cpPairB[i];
					count++;
				}
				
			}
			typicalColors[j][0]=typicalHues[j];
			typicalColors[j][1]=sumS/count;
			typicalColors[j][2]=sumB/count;
		}
		
		
		//double[] result={sumH,sumS,sumB};
		return typicalColors;
	}
	
	private static int[] packedArray(int[] temp, int sentinel) {
		int count=0;
		for(int i:temp){
			if(i!=sentinel)count++;
		}
		
		int[] packed = new int[count];
		count = 0; 
		for(int i:temp){
			if(i!=sentinel){
				packed[count]=i;
				count++;
			}
		}	
		return packed;
	}

	/**
	 * the method that tests the validity of the color patch
	 * @param candiR averaged red component value in the corresponding area of the candidate
	 * @param candiG averaged green component value in the corresponding area of the candidate
	 * @param candiB averaged blue component value in the corresponding area of the candidate
	 * @param imColor averaged color in the color patch in impression
	 * @param stdR standard deviation value of red component in the corresponding area of the candidate
	 * @param stdG standard deviation value of green component in the corresponding area of the candidate
	 * @param stdB standard deviation value of blue component in the corresponding area of the candidate
	 * @return whether this patch is qualified
	 */
	private boolean isQualified(double candiR, double candiG, double candiB, int imColor, double stdR, double stdG, double stdB){
		return abs(candiR-((imColor>>16)&0xFF))<CANDI_AVERAGE_THRESHOLD&&abs(candiG-((imColor>>8)&0xFF))<CANDI_AVERAGE_THRESHOLD
				&&abs(candiB-(imColor&0xFF))<CANDI_AVERAGE_THRESHOLD&&(stdR+stdG+stdB)/3<CANDI_SD_THRESHOLD;
	}
	
	private boolean isQualified(double candiH, double candiS, double candiB,int imColor){
		float[] hsb = new float[3];
		hsb = Color.RGBtoHSB((imColor>>16)&0xFF, (imColor>>8)&0xFF, imColor&0xFF, hsb);
		//System.out.println("The HSB values in Imp are: "+hsb[0]*K+", "+hsb[1]*K+", "+hsb[2]*K+".");
		return abs(candiH-hsb[0]*K)<CANDI_H_THRESHOLD&&abs(candiS-hsb[1]*K)<CANDI_S_THRESHOLD
				&&abs(candiB-hsb[2]*K)<CANDI_B_THRESHOLD;
	}

	private boolean isQualified(int imColor, int[][] typicalColors){
		float[] hsb = new float[3];
		hsb = Color.RGBtoHSB((imColor>>16)&0xFF, (imColor>>8)&0xFF, imColor&0xFF, hsb);
		//System.out.println("The HSB values in Imp are: "+hsb[0]*K+", "+hsb[1]*K+", "+hsb[2]*K+".");
		for(int[] value: typicalColors){
			/*if(abs(converter.toMyHue(hsb[0])*K-value[0])<CANDI_H_THRESHOLD && abs(hsb[1]*K-value[1])<CANDI_S_THRESHOLD 
					&& abs(hsb[2]*K-value[2])<CANDI_B_THRESHOLD) return true;*/
			int candiC = Color.HSBtoRGB((float)value[0]/K, (float)value[1]/K, (float)value[2]/K);
			if(areSimilarColors(imColor, candiC)) return true;
		}
		return false;
	}

	/**
	 * 
	 * @param c1 color that you have at hand
	 * @param c2 base that you compare c1 with (candi)
	 * @return
	 */
	public static boolean areSimilarColors(int c1, int c2){
		float[] hsb1 = new float[3];
		hsb1 = Color.RGBtoHSB((c1>>16)&0xFF, (c1>>8)&0xFF, c1&0xFF, hsb1);
		float[] hsb2 = new float[3];
		hsb2 = Color.RGBtoHSB((c2>>16)&0xFF, (c2>>8)&0xFF, c2&0xFF, hsb2);
		if(abs(hsb1[1]-hsb2[1])*K>CANDI_S_THRESHOLD || abs(hsb1[2]-hsb2[2])*K>CANDI_B_THRESHOLD )return false;
		double curThreshold=func2(hsb2[1]*K, hsb2[2]*K);
		//System.out.println("this threshold is " + curThreshold+".");
		if(abs(hsb1[0]*K-hsb2[0]*K)<func2(hsb2[1]*K, hsb2[2]*K)) return true;
		else return false;
	}
	
	/**
	 * 
	 * @param x 0<x<100
	 * @return
	 */
	public static double func1(double x){
		//double value = -(double)23/147000*x*x*x+(double)601/14700*x*x-(double)719/210*x+K;// min = 7 at 70
		double value = (double)7163/771120000*x*x*x*x-(double)429767/192780000*x*x*x+(double)7319393/38556000*x*x-(double)2652557/385560*x+K; //min = 2 at 87
		//double value = -3*x+110; if not cast first
		return value;
	}
	/**
	 * 
	 * @param x 0<x<100 saturation
	 * @param y 0<y<100 brightness
	 * @return hue threshold
	 */
	public static double func2(double x, double y){
		//l: y-100=(100-py)/(100-px)*(x-100)
		//c: y=900/x
		//solve:
		//System.out.println("s,b="+x+", "+y);
		//double x1 = -(sqrt(4*bLimit*sLimit*(K-y)*(K-x)+K*K*(x-y)*(x-y))-K*(x-y))/(2*(K-y));
		double x1,y1;
		if(y!=K){
			x1 = (sqrt(4*bLimit*sLimit*(K-y)*(K-x)+K*K*(x-y)*(x-y))+K*(x-y))/(2*(K-y));
			y1 = bLimit*sLimit/x1;
		} else{
			x1= bLimit*sLimit/K;
			y1=K;
		}
		//double x1= 10*(sqrt(25*y*y-y*(41*x+900)+25*(x*x-36*x+3600))+5*(y-x))/(100-y);//wrong: a=y;1; b=x1;
		
		//System.out.println("x1,y1="+x1+", "+y1);
		
		double s1= sqrt((K-x1)*(K-x1)+(K-y1)*(K-y1));
		double s2 = sqrt((K-x)*(K-x)+(K-y)*(K-y));
		return func1((s1-s2)/s1*K);//
	}
	
	public void loadFiles(File inFile) {
		if(inFile.isDirectory()){
			//FileFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif","png");
			for(File entry : inFile.listFiles()){//filter
				loadFiles(entry);
			}
		}else{
		//TODO¡¡Filter!
			String ext = inFile.getPath();
			//ext=ext.substring(ext.length()-4);
			ext=ext.toLowerCase();
			if(ext.endsWith(".jpg")||ext.endsWith(".png")||ext.endsWith(".gif")||ext.endsWith(".jpeg")){
				candiList.add(new ImprCandidate(inFile.getPath()));
				if(this.mode.equals("gui")){
					tableModel.fireTableRowsInserted(candiList.size()-1, candiList.size()-1);
				}
			}
		}
		
	}
	
	public boolean loadFiles(String path){
		File f = new File(path);
		loadFiles(f);
		return true; //TODO really true?
	}

	public BufferedImage showComparision(int candiIndex){
		
		
		ImprCandidate can = (ImprCandidate) candiList.get(candiIndex);
		Impression imp = (Impression) imprStock.get(can.getPairedImprID());
		System.out.println("Show comparision between "+can.getPath()+" and "+imp);
		
		if(imp!= null){
			BufferedImage display = null ; 
		    //TODO WAIT, Unfinished Aug 13    
			try{
				BufferedImage base = ImageIO.read(new File(can.getPath()));//new BufferedImage(w,h,Utils.IMAGE_TYPE);
				//base.getGraphics().drawImage((BufferedImage)ImageIO.read(new File(can.getPath())), 0, 0, w, h, null);
				int h = base.getHeight();
				int w = base.getWidth();
				display = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
				
				CandiWindow cw = can.getWinner();
				double ratio = cw.getAmplificationRatio();
				int x1=cw.startX();
				int y1=cw.startY();
				
				Graphics2D g = (Graphics2D) display.getGraphics();
				g.setColor(Color.GREEN);
				g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				g.drawRect(x1, y1, (int)(imp.getWidth()*ratio), (int)(imp.getHeight()*ratio));
				
				/*
				int[] pixels = new int[w*h];
				//for(int i=0;i<pixels.length;i++){
					//pixels[i]=Color.GRAY.getRGB();
				//}
				
				for(int i=0;i<imp.getColorPatchesCount();i++){
					ColorPatch cp = imp.getColorPatch(i);
					int[] p = cp.getPatchPixels();
					for(int j = 0; j<p.length; j++){
						int x =Utils.mapCoordinate(ratio, p[j]%Utils.DRAFT_WIDTH, x1);
						int y =Utils.mapCoordinate(ratio, p[j]/Utils.DRAFT_WIDTH, y1);
						pixels[x*w+y]=cp.getColor();
					}
				}
				int[] cpIndices = can.getPatchesWined();
		
				for (int i = 0; i < cpIndices.length; i++){
					ColorPatch cp = imp.getColorPatch(cpIndices[i]);
					int[] p = cp.getPatchPixels();
					for(int j = 0; j<p.length; j++){
						int x =Utils.mapCoordinate(ratio, p[j]%Utils.DRAFT_WIDTH, x1);
						int y =Utils.mapCoordinate(ratio, p[j]/Utils.DRAFT_WIDTH, y1);
						pixels[x*w+y]=0;//TODO 0|0|0|0?
					}
				}
				display.getRaster().setDataElements(0, 0, w, h, pixels);
				
				try {  
					ImageIO.write(display, "jpg", new File("mask-"+imp.getID()+".jpg"));  
					System.out.println("mask Saved .");
				} catch (IOException e1) {  
					e1.printStackTrace();  
				} */
			}catch (IOException e){
				e.printStackTrace();
			}
			
		return display;
		
		}else return null;
	}
	
	/**
	 * show the current search preference to a client
	 * @return pref[0] searching method
	 * 		   pref[1] preprocessing method
	 */
	public int[] showPreference(){
		int[] pref = {searchMethod,preprocessing};
		return pref;		
	}
	
	public void modifyPreference(int[] pref){
		searchMethod = pref[0];
		preprocessing = pref[1];
	}
	
	public String printSettings(){
		return String.format("Gerneral settings: \n Searching Method: %s; Pre-processing: %s; Maximum patch size: %s.\n"
				+ "Slide Window settings: \n AM_RATE_STEP = %s; MAX_AM_RATE = %s; MAX_FOLDS = %s; CENTER_X = %s; CENTER_Y= %s; \n SEARCH_W = %s; SEARCH_H = %s; SLIDING_STEP = %s  Probability function: %s",
				searchMethod,preprocessing, Impression.MAX_PATCH_SIZE, AM_RATE_STEP,MAX_AM_RATE,MAX_FOLDS,CENTER_X,CENTER_Y,SEARCH_W,SEARCH_H,SLIDING_STEP,probFunc);
	}



}
