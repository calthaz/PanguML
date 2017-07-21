package findSimilarImg;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.imageio.ImageIO;

import findSimilarImg.ImageEntry;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import tools.TFUtils;

//import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
//import javax.js\

public class fastQuerying {
	private static float[][] w={
			{(float) 4.04,(float) 0.78,(float) 0.46,(float) 0.43,(float) 0.41,(float) 0.32},
			{(float) 15.14,(float) 0.92,(float) 0.53,(float) 0.26,(float) 0.14,(float) 0.07},//15.14
			{(float) 22.62,(float) 0.40,(float) 0.63,(float) 0.25,(float) 0.15,(float) 0.38},//22.62
			};
	/**channel, sign, i,j, candi lists(based  on index in candiList)*/
	private ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>> searchArray;
	//private BufferedImage imprDraft;
	
	private PrintWriter wr;
	private String progressFile = "fq-progress.txt";
	private String resultFile;
	private String dataFile = System.currentTimeMillis()+"fq-data.txt";
	private String rootPaths[];
	private ArrayList<String> candiPathList = new ArrayList<String>();
	private ArrayList<ImageEntry> candiList = new ArrayList<ImageEntry>();
	private String RESULT_FILE_NAME = "fq-result.txt";
	
	
	public fastQuerying(String draftPath, String[] inputPaths) {
		rootPaths = inputPaths;
		String rootPath = inputPaths[0];
		File root = new File(rootPath);
		String prefix = "fq-"+(int)(Math.random()*100000);
  	    if(root.isDirectory()){
  	    	resultFile = rootPath+"/"+prefix+RESULT_FILE_NAME;
  	    	progressFile  = rootPath+"/"+prefix+progressFile;
  	    	dataFile = rootPath+"/"+prefix+dataFile;
  	    }else{
  	    	resultFile = root.getParent()+"/"+prefix+RESULT_FILE_NAME;
  	    	progressFile  = root.getParent()+"/"+prefix+progressFile;
  	    	dataFile = root.getParent()+"/"+prefix+dataFile;
  	    }
  	    loadFiles();
  	    initSearchArray();
  	    //String savedMap = "F:/TensorFlowDev/JavaWorksp/TensorFlow/img/fq-231651500631273277fq-data.txt";
  	    //searchArray = parseFeatureMap(savedMap);
  	    
  	    try {
			addToDataBase();
		} catch (IOException e) {
			System.out.println("can't write progress file");
		}
  	    
    	try {
			BufferedImage img = ImageIO.read(new File(draftPath));
			if(img!= null&&img.getHeight()!=0){
				findCandidates(img);
			}else{
				System.out.println("Probably not an image: "+draftPath);
			}
		} catch (IOException e) {
			System.out.println("Error reading "+draftPath);
		}
    	
    	
//    	Gson g = new Gson();
    	PrintWriter jsonWriter;
    	ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<String>>>>> dataToSave 
    		= saveFeatureMap(searchArray);
		try {
			jsonWriter = new PrintWriter(new FileWriter(dataFile));
			jsonWriter.print(JSONArray.fromObject(dataToSave).toString());
//			jsonWriter.print(g.toJson(dataToSave));
			jsonWriter.flush();
			jsonWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
	}
	
	private ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>> parseFeatureMap(String savedMap) {
		//Gson g = new Gson();
		//g.fromJson("xxx",);
		
		return null;
	}

	private ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<String>>>>> saveFeatureMap(
			ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>> search) {
		ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<String>>>>> ret 
			= new ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<String>>>>>();
		for(int cc=0; cc<=3; cc++){
			ret.add(new ArrayList<ArrayList<ArrayList<ArrayList<String>>>>());
			for(int sign=0;sign<=1; sign++){
				ret.get(cc).add(new ArrayList<ArrayList<ArrayList<String>>>());
				for(int i=0; i<ImageEntry.H; i++){
					ret.get(cc).get(sign).add(new ArrayList<ArrayList<String>>());
					for(int j=0; j<ImageEntry.W;j++){
						ret.get(cc).get(sign).get(i).add(new ArrayList<String>());
						ArrayList<ImageEntry> list = searchArray.get(cc).get(sign).get(i).get(j);
						for(ImageEntry ime:list){
							ret.get(cc).get(sign).get(i).get(j).add(ime.getPath());
						}
					}
				}
			}
		}		 
		return ret;
	}

	private void initSearchArray(){
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
		time=(int) System.currentTimeMillis()-time;
		//System.out.println("Setting up database has taken "+time+"ms.");
	    //if(wr!=null)wr.println("Setting up database has taken "+time+"ms.");
	    wr.flush();
	}
	
	private void loadFiles(){
		for(String p : rootPaths){
			TFUtils.readImageFilesRecursively(new File(p), candiPathList);
		}
	    
	}
	
	/**
	 * 
	 * @throws IOException if progress file can't be written
	 */
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
	private int bin(int i, int j){
		return min(max(i,j),5);
	}
	
	private void findCandidates(BufferedImage draft) throws IOException {
		
		System.out.println("Processing image...");
		if(wr!=null)wr.println("\n------"+Calendar.getInstance().getTime()+"------");
		//Time processing time.
		int time = (int) System.currentTimeMillis();
		String imprID = "temp-id";
		ImageEntry imp = new ImageEntry(draft,imprID);
		if(wr!=null)wr.println("Current Impression: "+imp.getPath());
		
		time = (int)System.currentTimeMillis()-time;
		System.out.println("Processing has taken "+time+"ms.");
		if(wr!=null)wr.println("Processing has taken "+time+"ms.");
		
		System.out.println(imp);
		System.out.println("Starts comparing...");
		time = (int) System.currentTimeMillis();
		PrintWriter progress = new PrintWriter(new FileWriter(progressFile,true),true);
		progress.println("Progress: start real comparing");
		    
		for(ImageEntry candi: candiList){
			candi.setPairedImprID(imprID);
			candi.setScore(0);
		}
 	   
 	    for(int cChannel = 0; cChannel<3; cChannel++){
 	    	for(ImageEntry candi: candiList){
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

			
		PrintWriter result = new PrintWriter(new FileWriter(resultFile,true),true);
		System.out.println(resultFile);
		result.println(candiList);
		result.close();

	    //System.out.println(candiList);
		time = (int)System.currentTimeMillis()-time;
 	    System.out.println("Comparing has taken "+time+"ms.");
 	   progress.println("Finished: "+"Comparing has taken "+time+"ms.");
 	   progress.flush();
 	   progress.close();
 	    if(wr!=null)wr.println("Comparing has taken "+time+"ms.");
 	    wr.flush();
	}
	
	public static void main(String args[]){
		String rootPath = "";
		String draftPath = "";
		String[] inputPaths = new String[1];
		if(args.length<1){
			return;
		}else if(args.length<2){
			draftPath = args[0];
			rootPath = System.getProperty("user.dir");
			inputPaths[0] = System.getProperty("user.dir");
		}else {		
			draftPath = args[0];
			rootPath = args[1];
			inputPaths = Arrays.copyOfRange(args, 1, args.length);
		}
		System.out.println(rootPath);
		new fastQuerying(draftPath, inputPaths);
	}
}
