package findSimilarImg;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import javax.imageio.ImageIO;

import findSimilarImg.ImageEntry;
import general.DevConstants;
import tools.LabelGenerator;


/**
 * <div class="en">This class carries out the algorithm described in
 * <a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">this paper</a></div>
 * <div class="zh">这个类执行的是<a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">这个论文</a>中的算法</div>
 * 
 */
public class fastQuerying {
	private static final String DATA_EXT = ".fqdata";
	private static float[][] w={
			{(float) 4.04,(float) 0.78,(float) 0.46,(float) 0.43,(float) 0.41,(float) 0.32},
			{(float) 15.14,(float) 0.92,(float) 0.53,(float) 0.26,(float) 0.14,(float) 0.07},//15.14
			{(float) 22.62,(float) 0.40,(float) 0.63,(float) 0.25,(float) 0.15,(float) 0.38},//22.62
			};
	/**channel*2+sign, y, x, candi lists (share instances with candiList)*/
	private ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>> searchArray;

	private String resultFile;
	/**
	 * <div class="en">stores the feature map of a collection of files
	 * follows this pattern: {@code [random prefix]-[currentTimeMillis][DATA_EXT]}
	 * therefore next time the program loads this dir it will look for the latest map and loads it</div>
	 * <div class="zh">按照这个格式：{@code [random prefix]-[currentTimeMillis][DATA_EXT]}
	 * 储存特征数据表，下次加载时会去寻找</div>
	 * 
	 */
	private String dataFile = "-"+System.currentTimeMillis()+DATA_EXT;
	
	private ArrayList<String> rootPaths;
	private ArrayList<String> candiPathList = new ArrayList<String>();
	private ArrayList<String> pathsRead = new ArrayList<String>();
	private ArrayList<ImageEntry> candiList = new ArrayList<ImageEntry>();
	private String RESULT_FILE_NAME = "-fq-result.txt";
	
	/**
	 * 
	 * <div class="zh">最强大的构造函数</div><div class="en">The most powerful constructor</div>
	 * @param draftPath <span class="zh">跟别人比较的图片</span><span class="en"></span>
	 * @param inputPaths <span class="zh">用来和draft比较的图片路径/目录路径</span><span class="en"></span>
	 */
	public fastQuerying(String draftPath, String[] inputPaths) {
		rootPaths = trimFiles(inputPaths);
		String rootPath = rootPaths.get(0);//inputPaths[0];
		File root = new File(rootPath);
		String prefix = "fq"+(int)(Math.random()*100000);
  	    if(root.isDirectory()){
  	    	resultFile = rootPath+"/"+prefix+RESULT_FILE_NAME;
  	    	dataFile = rootPath+"/"+prefix+dataFile;
  	    	System.out.println("Root dir is "+rootPath);
  	    }else{
  	    	resultFile = root.getParent()+"/"+prefix+RESULT_FILE_NAME;
  	    	dataFile = root.getParent()+"/"+prefix+dataFile;
  	    	root = root.getParentFile();
  	    	System.out.println("Root dir is "+root.getAbsolutePath());
  	    }
  	    long loadTime = System.currentTimeMillis();
  	    initSearchArray();
  	    loadFiles();
  	    //System.out.println(candiPathList);
  	    readSavedData();
  	    candiPathList.removeAll(pathsRead);
  	    //System.out.println(candiPathList);
		addToDataBase();
		System.out.println("Loading Candidates finished in "+(System.currentTimeMillis()-loadTime)+" miliseconds");
		//saveFeatureMap(dataFile);
		/*
  	    System.out.println(searchArray.size());
		System.out.println(searchArray.get(0).size());
		System.out.println(searchArray.get(1).size());
		System.out.println(searchArray.get(2).size());
		System.out.println(searchArray.get(3).size());
		System.out.println(searchArray.get(4).size());
		System.out.println(searchArray.get(5).size());
		System.out.println(candiList.size());
	    */
    	try {
			BufferedImage img = ImageIO.read(new File(draftPath));
			if(img!= null&&img.getHeight()!=0){
				findCandidates(img);
			}else{
				System.out.println("Probably not an image: "+draftPath);
			}
		} catch (IOException e) {
			System.out.println("Can't read draft "+draftPath);
		}
    	
    	
	}
	/**
	 * <div class="en">reads all the files under current dir and generate fqdata file</div>
	 * <div class="zh">读该目录下所有文件并且在此处生成fqdata文件</div>
	 */
	public fastQuerying(){
		rootPaths = new ArrayList<String>();
		rootPaths.add(System.getProperty("user.dir"));
		String rootPath = rootPaths.get(0);//inputPaths[0];
		File root = new File(rootPath);
		String prefix = "fq"+(int)(Math.random()*100000);
	    if(root.isDirectory()){
	    	dataFile = rootPath+"/"+prefix+dataFile;
	    	System.out.println("Root dir is "+rootPath);
	    }else{
	    	dataFile = root.getParent()+"/"+prefix+dataFile;
	    	root = root.getParentFile();
	    	System.out.println("Root dir is "+root.getAbsolutePath());
	    }
	    
	    initSearchArray();
	    for(File f:root.listFiles()){
	    	if(f.getName().endsWith(DATA_EXT)){
	    		f.delete();
	    	}
	    }
	    loadFiles();  
		addToDataBase();
		saveFeatureMap(dataFile);
	}
	
	/**
	 * <div class="zh">传入图片路径在当前目录下寻找保存过的特征图然后比较计算</div>
	 * <div class="en">Use a locally stored fqdata file to find similar images for the draft </div>
	 * @param draftPath <span class="zh">跟别人比较的图片</span><span class="en"></span>
	 */
	public fastQuerying(String draftPath){
		rootPaths = new ArrayList<String>();
		rootPaths.add(System.getProperty("user.dir"));
		//rootPaths.add("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/furpics");
		String rootPath = rootPaths.get(0);//inputPaths[0];
		File root = new File(rootPath);
		String prefix = "fq"+(int)(Math.random()*100000);
	    if(root.isDirectory()){
	    	resultFile = rootPath+"/"+prefix+RESULT_FILE_NAME;
	    	dataFile = rootPath+"/"+prefix+dataFile;
	    	System.out.println("Root dir is "+rootPath);
	    }else{
	    	System.err.println("Root dir is not a dir: "+root.getAbsolutePath());
	    	return;
	    }
	    long loadTime = System.currentTimeMillis();
	    initSearchArray();
	    for(File f:root.listFiles()){
	    	if(f.getName().endsWith(DATA_EXT)){
	    		System.out.println("Try to parse "+f.getName());
	    		try {
					parseFeatureMapOnly(f.getAbsolutePath());
				} catch (ClassNotFoundException | IOException e) {
					System.err.println("Can't parse feature map "+f.getAbsolutePath());
					return;
				}
	    	}
	    }
	    if(candiList.size()==0){
	    	System.out.println("No images to compare with. ");
			return;
	    }
	    System.out.println("Loading Candidates finished in "+(System.currentTimeMillis()-loadTime)+" miliseconds");
    	try {
			BufferedImage img = ImageIO.read(new File(draftPath));
			if(img!= null&&img.getHeight()!=0){
				findCandidates(img);
			}else{
				System.out.println("Probably not an image: "+draftPath);
			}
		} catch (IOException e) {
			System.out.println("Can't read draft "+draftPath);
		}
	    
	}
	private void initSearchArray(){
		
		searchArray=new ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>();
		for(int cs=0; cs<3*2; cs++){
			searchArray.add(new ArrayList<ArrayList<ArrayList<ImageEntry>>>());
			for(int i=0; i<ImageEntry.H; i++){
				searchArray.get(cs).add(new ArrayList<ArrayList<ImageEntry>>());
				for(int j=0; j<ImageEntry.W;j++){
					searchArray.get(cs).get(i).add(new ArrayList<ImageEntry>());
	
				}
			}
		}		 
	    
	}

	/**
	 * <div class="en">delete redundant paths and nonexistent paths.</div>
	 * <div class="zh">删除多余和不存在的路径</div> 
	 * <span class="zh">示例:</span><span class="en">For example:</span>
	 * <pre>
inputPaths: [F:/tmp/foo, F:/tmp/root2, F:/tmp/root/ignore-l3, F:/tmp/root/l2/ignore-l2-2/l2-2-1, F:/tmp/root/l2/l2-1, 
		F:/tmp/root/l2/ignore-l2-2/sc9.png, F:/tmp/root/l1/sc0.png, F:/tmp/root/l2/l2-1/sc0.png]
return: [F:/tmp/root2, F:/tmp/root/l2/l2-1, F:/tmp/root/l1/sc0.png]
</pre>
	 * @param inputPaths <span class="zh">需要修剪的列表</span><span class="en">paths to trim</span>
	 * @return trimed paths
	 */
	public static ArrayList<String> trimFiles(String[] inputPaths) {
		//candiPathList is initialized
		ArrayList<String> paths = new ArrayList<String>();
		for(String str : inputPaths){
			Path p = Paths.get(str).normalize().toAbsolutePath();//normalize or not?
			File f = new File(str);
			if(!f.exists()){
				System.out.println("Input path: "+str+" doesn't exist.");
				continue;
			}
			if(f.isDirectory()){
				if(str.indexOf(DevConstants.IGNORE_PREFIX)!=-1) continue;
			}else{
				if(f.getParent().indexOf(DevConstants.IGNORE_PREFIX)!=-1) continue;
			}
			//now this file or folder shall not be ignored
			//check if it's redundant
			boolean isRedundant = false;
			for(String old : paths){
				Path parent = Paths.get(old).toAbsolutePath();
				if(p.startsWith(parent)){
					//yes, it is redundant
					System.out.println(p+" is the children of "+parent);
					isRedundant = true;
					break;
				}
			}
			if(!isRedundant)paths.add(p.toString());
		}
		System.out.println(paths);
		return paths;
	}
	
	private void loadFiles(){
		for(String p : rootPaths){
			loadImageFilesRecursively(new File(p), candiPathList);
		}
	    
	}
	
	/**
	 * 找所有目录下的保存的特征文件并读取
	 */
	private void readSavedData(){
		for(String str : rootPaths){
  	    	File f = new File(str);
  	    	if(!f.isDirectory()){
  	    		f = f.getParentFile();
  	    	}
  	    	long time = 0l;
    		String dataFile = null;
    		for(File c : f.listFiles()){
    			if(c.getName().endsWith(DATA_EXT)){
    				String name = c.getName();
    				try{
    					long t = Long.parseLong(name.substring(name.lastIndexOf("-")+1, name.indexOf(DATA_EXT)));
    					if(t>time){
    						dataFile = c.getAbsolutePath();
    						time = t;
    					}
    				}catch(NumberFormatException e){
    					System.out.println(c.getAbsolutePath()+"'s time can't be inferred");
    				}
    				
    			}
    		}
    		if(dataFile!=null){
    			try {
					parseFeatureMap(dataFile);
					System.out.println("Parsed feature map "+dataFile);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
    		}
    		
  	    }
	}
	
	private void loadImageFilesRecursively(File f, ArrayList<String> pathList) {
		if(f.isDirectory()){
			if(f.getName().indexOf(DevConstants.IGNORE_PREFIX)==-1){
				for(File entry : f.listFiles()){
					loadImageFilesRecursively(entry, pathList);
				}
			}	
		}else{
			try{
				//reads every image in case it were a bad one 
				BufferedImage img = ImageIO.read(f);
				if(img!= null&&img.getHeight()!=0){
					pathList.add(f.getPath());
				}else{
					System.out.println("Probably not an image: "+f.getPath());
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+f.getPath());
			}
		}
	}
	
	/**
	 * <div class="en">
	 * read entries from saved data if it is listed on {@code candiPathList}
	 * searchArray must be initialized here.
	 * <b>Note: if multiple ImageEntries that have the same path are parsed, 
	 * only the first instance is saved into the {@code candiList}, 
	 * but all are saved into {@code searchArray}</b>, 
	 * because the cost to remove all the occurrences of the other ImageEntries 
	 * in {@code searchArray} is simply not worth it. 
	 * </div>
	 * <br>
	 * <div class="zh">
	 * 从{@code candiPathList}中列出已保存数据的条目
	 * searchArray必须在这里初始化
	 * <b>注意：如果具有相同路径的多个ImageEntries被解析，
	 * 只有第一个对象被保存到{@code candiList}中，
	 * 但所有都保存到{@code searchArray} </ b>中，
	 * 因为在{@code searchArray}删除所有其他ImageEntries的成本太大
	 * 根本不值得。</div>
	 * @param savedMap <span class="zh">已保存的特征数据路径</span><span class="en">path to saved map</span>
	 * @throws ClassNotFoundException 
	 * <span class="zh">保存的数据不可读成fq特征图</span>
	 * <span class="en">The stored map is not a fq feature map</span>
	 * @throws IOException <span class="zh">读文件出错</span><span class="en">if an I/O error occurs while reading stream header</span>
	 */
	private void parseFeatureMap(String savedMap) throws ClassNotFoundException, IOException{		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(savedMap)));
		@SuppressWarnings("unchecked")
		ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>> map =  (ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>) in.readObject();
		in.close();		
		//ArrayList<String> localPaths = new ArrayList<String>();
		//ArrayList<ImageEntry> localCandis = new ArrayList<ImageEntry>();
		for(int cs=0; cs<3*2; cs++){
			for(int i=0; i<ImageEntry.H; i++){
				for(int j=0; j<ImageEntry.W;j++){
					for(int c = 0; c<map.get(cs).get(i).get(j).size(); c++){
						ImageEntry entry = map.get(cs).get(i).get(j).get(c);
						if(candiPathList.indexOf(entry.getPath())==-1){
							//this file is not in the scope
							continue;
						}
						if(pathsRead.indexOf(entry.getPath())==-1){
							pathsRead.add(entry.getPath());
							candiList.add(entry);
						}
						searchArray.get(cs).get(i).get(j).add(entry);
					}
				}
			}
		}
	}

	/**
	 * <div class="en">
	 * <b>ignore {@code candiPathList}</b><br>
	 * read entries from saved data if it is listed on {@code candiPathList}
	 * searchArray must be initialized here.
	 * <b>Note: if multiple ImageEntries that have the same path are parsed, 
	 * only the first instance is saved into the {@code candiList}, 
	 * but all are saved into {@code searchArray}</b>, 
	 * because the cost to remove all the occurrences of the other ImageEntries 
	 * in {@code searchArray} is simply not worth it. 
	 * </div>
	 * <br>
	 * <div class="zh">
	 *  <b>无视{@code candiPathList}</b><br>
	 * 从{@code candiPathList}中列出已保存数据的条目
	 * searchArray必须在这里初始化
	 * <b>注意：如果具有相同路径的多个ImageEntries被解析，
	 * 只有第一个对象被保存到{@code candiList}中，
	 * 但所有都保存到{@code searchArray} </ b>中，
	 * 因为在{@code searchArray}删除所有其他ImageEntries的成本太大
	 * 根本不值得。</div>
	 * @param savedMap
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void parseFeatureMapOnly(String savedMap) throws ClassNotFoundException, IOException{		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(savedMap)));
		@SuppressWarnings("unchecked")
		ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>> map =  (ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>) in.readObject();
		in.close();		
		for(int cs=0; cs<3*2; cs++){
			for(int i=0; i<ImageEntry.H; i++){
				for(int j=0; j<ImageEntry.W;j++){
					for(int c = 0; c<map.get(cs).get(i).get(j).size(); c++){
						ImageEntry entry = map.get(cs).get(i).get(j).get(c);
						if(pathsRead.indexOf(entry.getPath())==-1){
							pathsRead.add(entry.getPath());
							candiList.add(entry);
						}
						searchArray.get(cs).get(i).get(j).add(entry);
					}
				}
			}
		}
	}
	
	private void saveFeatureMap(String path) {
		ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>> ret 
			= new ArrayList<ArrayList<ArrayList<ArrayList<ImageEntry>>>>();
		for(int cs=0; cs<3*2; cs++){
			ret.add(new ArrayList<ArrayList<ArrayList<ImageEntry>>>());
				for(int i=0; i<ImageEntry.H; i++){
					ret.get(cs).add(new ArrayList<ArrayList<ImageEntry>>());
					for(int j=0; j<ImageEntry.W;j++){
						ret.get(cs).get(i).add(new ArrayList<ImageEntry>());
						ArrayList<ImageEntry> list = searchArray.get(cs).get(i).get(j);
						for(ImageEntry ime:list){
							ret.get(cs).get(i).get(j).add(ime.strip());
						}
					}
				}
			
		}		 
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(path)));
			out.writeObject(ret);
			out.close();
			
			
		} catch (IOException  e) {
			e.printStackTrace();
		}
		System.out.println("Feature Map saved at "+path);
	}

	/**
	 * based on candiPathList, add ImageEntry to both candiList and searchArray 
	 * so that they <b>share the same set of candis</b>
	 * @throws IOException if progress file can't be written
	 */
	private void addToDataBase(){

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
								for(int[] co:entry.coefficients.get(cc*2+sign)){
									if(co[0]==o[0]&&co[1]==o[1]){
										searchArray.get(cc*2+sign).get(i).get(j).add(entry);
									}
								}	
							}
						}
					}
				}
				System.out.println(String.format("Processing Candidate %s out of %s", n,total));
			}catch(IOException e){
				System.out.println(String.format("Error: while Processing Candidate %s out of %s, path: %s", n,total,path));
				e.printStackTrace();
			}
		}
		
 	    
	}
	
	private int bin(int i, int j){
		return min(max(i,j),5);
	}
	
	/**
	 * 主要的搜索过程
	 * @param draft
	 * @throws IOException
	 */
	private void findCandidates(BufferedImage draft) throws IOException {
		
		System.out.println("Processing image...");
		
		//Time processing time.
		int time = (int) System.currentTimeMillis();
		String imprID = "temp-id";
		ImageEntry imp = new ImageEntry(draft,imprID);
		time = (int)System.currentTimeMillis()-time;
		System.out.println("Processing has taken "+time+"ms.");
		System.out.println("Starts comparing...");
		time = (int) System.currentTimeMillis();
		    
		for(ImageEntry candi: candiList){
			candi.setPairedImprID(imprID);
			candi.setScore(0);
		}
 	   
 	    for(int cChannel = 0; cChannel<3; cChannel++){
 	    	for(ImageEntry candi: candiList){
 	    		candi.setScore(candi.getScore()+w[cChannel][0]*abs(((ImageEntry)candi).average.get(cChannel)-imp.average.get(cChannel)));
 	    	}
 	    	
 	    	for(int[] coordinate: imp.coefficients.get(cChannel*2)){
 	    		for(ImageEntry entry: searchArray.get(cChannel*2).get(coordinate[0]).get(coordinate[1])){
 	    			entry.setScore(entry.getScore()-w[cChannel][bin(coordinate[0],coordinate[1])]);
 	    		}
 	    	}
 	    	
 	    	for(int[] coordinate: imp.coefficients.get(cChannel*2+1)){
 	    		for(ImageEntry entry: searchArray.get(cChannel*2+1).get(coordinate[0]).get(coordinate[1])){
 	    			entry.setScore(entry.getScore()-w[cChannel][bin(coordinate[0],coordinate[1])]);
 	    		}
 	    	}
 	    }
 	    
 	    System.out.println("Sorting results...");
	    Collections.sort(candiList);
	    //System.out.println(candiList);
		time = (int)System.currentTimeMillis()-time;
 	    System.out.println("Comparing has taken "+time+"ms.");
 	    PrintWriter result = new PrintWriter(new FileWriter(resultFile,true),true);
 	    for(ImageEntry e : candiList){
 	    	result.println(e.getPath()+LabelGenerator.LABEL_SEP+e.getScore());
 	    }
		System.out.println(resultFile);
		result.close();
 	   
	}
	
	/**
	 * <span class="zh">读已有fqdata和args[0]比较</span>
	 * <span class="en">read existing fqdata and compare with args[0]</span>
	 * @param args
	 */
	public static void main(String args[]){
		//String rootPath = "";
		//String draftPath = "";
		//String[] inputPaths = new String[1];
		if(args.length<1){
			System.out.println("1st argument should be the path to an image");
			return;
		}/*else if(args.length<2){
			//draftPath = args[0];
			//rootPath = System.getProperty("user.dir");
			inputPaths[0] = System.getProperty("user.dir");
		}else {		
			draftPath = args[0];
			//rootPath = args[1];
			inputPaths = Arrays.copyOfRange(args, 1, args.length);
		}
		//System.out.println(rootPath);
	    for(String p : inputPaths){
			System.out.println(p);
		}
		//trimFiles(inputPaths);
		//new fastQuerying(draftPath, inputPaths);
		//new fastQuerying();*/
		new fastQuerying(args[0]);
	}
}
