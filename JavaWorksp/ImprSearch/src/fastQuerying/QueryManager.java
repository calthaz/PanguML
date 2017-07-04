package fastQuerying;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import general.Candidate;
import javax.imageio.ImageIO;
import general.Manager;
import general.Query;

import static java.lang.Math.*;

/**
 * This class carries out the algorithm described in http://grail.cs.washington.edu/projects/query/mrquery.pdf 
 * I am not so sure whether I have succeeded or not.
 * @author asus-pc
 *
 */
public class QueryManager extends Manager{
	//public String myStr = "blablabla I am instantiated.";
	//private ArrayList<ImageEntry> candiList = new ArrayList<ImageEntry>();
	//public AbstractTableModel tableModel = new MyTableModel();
	
	/**channel, sign, i,j, candi lists(based  on index in candiList)*/
	private ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>> searchArray;
	//private BufferedImage imprDraft;
	
	private PrintWriter wr;
	private ArrayList<String> candiPathList = new ArrayList<String>();
	
	private static float[][] w={
			{(float) 4.04,(float) 0.78,(float) 0.46,(float) 0.43,(float) 0.41,(float) 0.32},
			{(float) 15.14,(float) 0.92,(float) 0.53,(float) 0.26,(float) 0.14,(float) 0.07},//15.14
			{(float) 22.62,(float) 0.40,(float) 0.63,(float) 0.25,(float) 0.15,(float) 0.38},//22.62
			};//4.04
	
	public QueryManager(){
		//myStr = "blablabla I am instantiated.";
		this.mode="gui";
		candiList = new ArrayList<Candidate>();
		imprList = new ArrayList<String>();
		imprStock =  new HashMap<String, Query>();
		tableModel = new MyTableModel();
		cbModel= new MyComboBoxModel();
		try {
			wr=new PrintWriter(new FileWriter("fastQuerying log.txt",true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int time = (int) System.currentTimeMillis();
		if(wr!=null)wr.println("\n------"+Calendar.getInstance().getTime()+"------");
		
		searchArray=new ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>>();
		for(int cc=0; cc<=3; cc++){
			searchArray.add(new ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>());
			for(int sign=0;sign<=1; sign++){
				searchArray.get(cc).add(new ArrayList<ArrayList<ArrayList<ImageEntry>>>());
				for(int i=0; i<ImageEntry.H; i++){
					searchArray.get(cc).get(sign).add(new ArrayList<ArrayList<ImageEntry>>());
					for(int j=0; j<ImageEntry.W;j++){
						searchArray.get(cc).get(sign).get(i).add(new ArrayList<ImageEntry>());

					}
				}
			}
		}
		
		addExamples();
		 tableModel.fireTableDataChanged();
		 
		time=(int) System.currentTimeMillis()-time;
		//System.out.println("Setting up database has taken "+time+"ms.");
	    //if(wr!=null)wr.println("Setting up database has taken "+time+"ms.");
	    wr.flush();
	}
	
	public QueryManager(String mode){
		if(mode.equals("text")){
			this.mode="text";
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
		}
		try {
			wr=new PrintWriter(new FileWriter("fastQuerying log.txt",true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int time = (int) System.currentTimeMillis();
		if(wr!=null)wr.println("\n------"+Calendar.getInstance().getTime()+"------");
		
		searchArray=new ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>>();
		for(int cc=0; cc<=3; cc++){
			searchArray.add(new ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>());
			for(int sign=0;sign<=1; sign++){
				searchArray.get(cc).add(new ArrayList<ArrayList<ArrayList<ImageEntry>>>());
				for(int i=0; i<ImageEntry.H; i++){
					searchArray.get(cc).get(sign).add(new ArrayList<ArrayList<ImageEntry>>());
					for(int j=0; j<ImageEntry.W;j++){
						searchArray.get(cc).get(sign).get(i).add(new ArrayList<ImageEntry>());

					}
				}
			}
		}
		if(this.mode=="gui")tableModel.fireTableDataChanged();
		time=(int) System.currentTimeMillis()-time;
		//System.out.println("Setting up database has taken "+time+"ms.");
	    //if(wr!=null)wr.println("Setting up database has taken "+time+"ms.");
	    wr.flush();
	}
	
	private void addExamples(){
		//addToDataBase("berlin.jpg");		
		//addToDataBase("river.jpg");		
		candiPathList.add("berlin.jpg");
		candiPathList.add("tower.jpg");
		candiPathList.add("fruits.jpg");
		candiPathList.add("statue.jpg");
		candiPathList.add("house.jpg");
		candiPathList.add("impression example 1.jpg");
		candiPathList.add("impression example 2.png");
		candiPathList.add("impression example 3.png");
		try {
			addToDataBase();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//loadFiles(new File("d:\\zym\\Java Programming\\Testing Images"));
	}
	
	public void loadFiles(File inFile) {
		//candiPathList.clear();
		loadRecursively(inFile);
		/*
		 * Not now
		 * try {
			addToDataBase();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
	
	@Override
	public boolean loadFiles(String path) {
		File f = new File(path);
		loadFiles(f);
		return true; //TODO really true?
	}
	
	private void loadRecursively(File inFile){
		//System.out.println("recursive load"+inFile.getPath());
		if(inFile.isDirectory()){
			//FileFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif","png");
			for(File entry : inFile.listFiles()){//filter
				loadRecursively(entry);
			}
		}else{
			String ext = inFile.getPath();
			//ext=ext.substring(ext.length()-4);
			ext=ext.toLowerCase();
			if(ext.endsWith(".jpg")||ext.endsWith(".png")||ext.endsWith(".gif")||ext.endsWith(".jpeg")){
				candiPathList.add(inFile.getPath());				
			}
		//addToDataBase(inFile.getPath());
	    //if(this.mode=="gui")tableModel.fireTableRowsInserted(candiList.size()-1, candiList.size()-1);
		}
	}

	private void addToDataBase() throws IOException{
		int time = (int) System.currentTimeMillis();
		PrintWriter progress = new PrintWriter(new FileWriter(progressFile,true),true);
		System.out.println(progressFile);
	    progress.println("Progress: Start comparing");
		int total = candiPathList.size();
		for(int n=0; n<total; n++){
			String path = candiPathList.get(n);
			try{				
				ImageEntry entry = new ImageEntry(path,"");
				//System.out.println("forming search array");
				System.out.println(entry.getPath());
				candiList.add(entry);
				for(int cc=0; cc<3; cc++){
					for(int sign=0;sign<=1; sign++){
						for(int i=0; i<ImageEntry.H; i++){
							for(int j=0; j<ImageEntry.W;j++){
								int[] o ={i,j};
								//int index = entry.coefficients.get(cc).get(sign).indexOf(o);
								//sadly, they never refer to the same object, so they can never be equal. 
								for(int[] co:entry.coefficients.get(cc).get(sign)){
									if(co[0]==o[0]&&co[1]==o[1]){
										searchArray.get(cc).get(sign).get(i).get(j).add(entry);
									}
								}	
							}
						}
					}
				}
				System.out.println(String.format("Processing Candidate %s out of %s", n,total));
				if(this.mode=="gui")tableModel.fireTableRowsInserted(candiList.size()-1, candiList.size()-1);
				progress.println("Progress: processing file No."+(n+1)+" out of "+total);
			}catch(IOException e){
				System.out.println(String.format("Error: while Processing Candidate %s out of %s, path: %s", n,total,path));
				e.printStackTrace();
			}
		}
		time = (int)System.currentTimeMillis()-time;
		progress.println("Progress: Loading Candidates finished in "+time+" seconds");
		System.out.println("Progress: Loading Candidates finished in "+time+" seconds");
 	    progress.close();
	}
	
	
	@Override
	public void run() {
		try {
			addToDataBase();
			findCandidates();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void findCandidates() throws IOException {
		
		System.out.println("Processing image...");
		if(wr!=null)wr.println("\n------"+Calendar.getInstance().getTime()+"------");
		//Time processing time.
		int time = (int) System.currentTimeMillis();
		ImageEntry imp = new ImageEntry(imprDraft,imprID);
		if(wr!=null)wr.println("Current Impression: "+imp.getPath());
		
		time = (int)System.currentTimeMillis()-time;
	    System.out.println("Processing has taken "+time+"ms.");
	    if(wr!=null)wr.println("Processing has taken "+time+"ms.");
	    
	    imprStock.put(imprID, imp);
	    if(this.mode=="gui")cbModel.addElement(imprID);
	    
	    System.out.println(imp);
	    System.out.println("Starts comparing...");
 	    time = (int) System.currentTimeMillis();
 	   PrintWriter progress = new PrintWriter(new FileWriter(progressFile,true),true);
 	   progress.println("Progress: start real comparing");
 	    
 	   for(Candidate candi: candiList){
    		candi.setPairedImprID(imprID);
    		candi.setScore(0);
 	   }
 	   
 	    for(int cChannel = 0; cChannel<3; cChannel++){
 	    	for(Candidate candi: candiList){
 	    		candi.setScore(candi.getScore()+w[cChannel][0]*abs(((ImageEntry)candi).average.get(cChannel)-imp.average.get(cChannel)));
 	    		//candi.score+=w[cChannel][0]*abs(candi.average.get(cChannel)-imp.average.get(cChannel));
 	    	}
 	    	
 	    	for(int[] coordinate: imp.coefficients.get(cChannel).get(0)){
 	    		//System.out.print("D+");
 	    		//list.addAll(searchArray.get(cChannel).get(0).get(coordinate[0]).get(coordinate[1]));
 	    		//System.out.println(searchArray.get(cChannel).get(0).get(coordinate[0]).get(coordinate[1]).size());
 	    		for(ImageEntry entry: searchArray.get(cChannel).get(0).get(coordinate[0]).get(coordinate[1])){
 	    			//candiList.get(index).score-=w[cChannel][bin(coordinate[0],coordinate[1])];
 	    			entry.setScore(entry.getScore()-w[cChannel][bin(coordinate[0],coordinate[1])]);
 	    			//entry.score-=w[cChannel][bin(coordinate[0],coordinate[1])];
 	    			//System.out.println(" "+entry.getPath()+" "+entry.score);
 	    		}
 	    	}
 	    	
 	    	for(int[] coordinate: imp.coefficients.get(cChannel).get(1)){
 	    		//list.addAll(searchArray.get(cChannel).get(1).get(coordinate[0]).get(coordinate[1]));
 	    		for(ImageEntry entry: searchArray.get(cChannel).get(1).get(coordinate[0]).get(coordinate[1])){
 	    			entry.setScore(entry.getScore()-w[cChannel][bin(coordinate[0],coordinate[1])]);
 	    			//entry.score-=w[cChannel][bin(coordinate[0],coordinate[1])];
 	    		}
 	    	}
 	    }
 	    
 	    System.out.println("Sorting results...");
	    Collections.sort(candiList);
	    if(this.mode.equals("gui")){
			tableModel.fireTableDataChanged();
		}
		else{
			//System.out.println(candiList);
			
			PrintWriter result = new PrintWriter(new FileWriter(resultFile,true),true);
			System.out.println(resultFile);
			result.println(candiList);
			result.close();
		}
	    //System.out.println(candiList);
		time = (int)System.currentTimeMillis()-time;
 	    System.out.println("Comparing has taken "+time+"ms.");
 	   progress.println("Finished: "+"Comparing has taken "+time+"ms.");
 	   progress.flush();
 	   progress.close();
 	    if(wr!=null)wr.println("Comparing has taken "+time+"ms.");
 	    wr.flush();
	}
	
	/**
	 * what should i and j be? of course they will be larger than 5 in most cases!
	 */
	private int bin(int i, int j){
		return min(max(i,j),5);
	}

	@Override
	public BufferedImage showComparision(int candiIndex) {
		// TODO Auto-generated method stub
		try{
			BufferedImage img = ImageIO.read(new File(candiList.get(candiIndex).getPath()));
			return img;
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int[] showPreference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyPreference(int[] pref) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String printSettings() {
		// TODO Auto-generated method stub
		return w.toString();
	}
}
