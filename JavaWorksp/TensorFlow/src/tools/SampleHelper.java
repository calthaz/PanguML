package tools;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import general.DevConstants;

/**
 * extend image samples by random crop<br>
 * jpgs and pngs only
 *
 */
public class SampleHelper {
	
	/**
	 * Must implements {@code public BufferedImage process(BufferedImage img);} <br>
	 * usage: just like FilenameFilter
	 * <div style="font-family: monospace">
	 * String[] myFiles = directory.list(new FilenameFilter() {<br>
		    public boolean accept(File directory, String fileName) {<br>
		        return fileName.endsWith(".txt");<br>
		    }<br>
		});<br>
	 * </div>
	 */
	private interface ImgProcessor{
		public BufferedImage process(BufferedImage img); 
	}
	private static final String SEP = "/";
	
	/**
	 * Extends the number of examples by random cropping.<br>
	 *  crop portion is specified by {@code TFUtils.cropRate}<br>
	 *  new images saved in the same dir but with new names starting with "ext-"<br>
	 *  <b>Note: not recursive</b>
	 * @param directoryPath
	 */
	public static void extendSamples(String directoryPath){
		File directory = new File(directoryPath);
		if(directory.isDirectory()){
			//we are only going to crop jpgs and pngs
			for(File entry : directory.listFiles()){
				String ext = entry.getPath();
				System.out.println(ext);
				ext=ext.toLowerCase();
				if(ext.endsWith(".jpg")||ext.endsWith(".png")){
					double p = Math.random();
					if(p>0.6){
						try {
							BufferedImage img = ImageIO.read(entry);
							BufferedImage output = TFUtils.randomCropImage(img);
							ImageIO.write(output, "jpg", new File(directoryPath+"\\ext-"+(int)(Math.random()*1000000)+".jpg"));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}else{
			System.out.println("not a dir");
		}
	}
	
	/**
	 * Copy the entire structure of {@code inputDir} to {@code outputDir/inputDirName} 
	 * with images processed by processor inside. <br>
	 * <b>Note:recursive</b>
	 * @param inputDir
	 * @param outputDir
	 * @param prefix rename images with "prefix"+randInt<br>
	 * @param processor {@code outputImg = processor.process(inputImg);}
	 */
	public static void batchEditImages(String inputDir, String outputDir, String prefix, ImgProcessor processor){
		File inputRoot = new File(inputDir);
		File outputRoot = new File(outputDir);
		if(!inputRoot.exists()||!outputRoot.exists()){
			System.out.println("Input dir or output dir doesn't exist");
			return;
		}
		if(inputRoot.isDirectory()){
			String r = inputRoot.getName();
			File realOutputRoot = new File(outputDir+SEP+r);
			int count = 0; 
			if(realOutputRoot.mkdirs()){
				batchEditImagesRecursive(inputRoot, realOutputRoot, prefix, processor, count);
			}else{
				System.err.println("Create dir failed "+realOutputRoot.getAbsolutePath());
			}
		}else{
			try{
				BufferedImage img = ImageIO.read(inputRoot);
				if(img!= null&&img.getHeight()!=0){
					BufferedImage st = processor.process(img);
					String ext = "png";
					if(st.getType()==BufferedImage.TYPE_INT_RGB){
						ext = "jpg";
					}
					ImageIO.write(st, ext, new File(outputDir+SEP+inputRoot.getName()));
				}else{
					System.out.println("Probably not an image: "+inputRoot.getPath());
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+inputRoot.getPath());
			}
		}
		System.out.println();
		System.out.println("Finished.");
	}

	private static void batchEditImagesRecursive(File inputRoot, File outputRoot, String prefix, ImgProcessor processor, int count) {
		for(File f : inputRoot.listFiles()){
			if(f.isDirectory()){
				File nextOut = new File(outputRoot+SEP+f.getName());
				if(nextOut.mkdirs()){
					batchEditImagesRecursive(f, nextOut, prefix, processor, count);
				}else{
					System.err.println("Create dir failed "+nextOut.getAbsolutePath());
				}
				deleteEmptyDirs(nextOut);
			}else{
				try{
					BufferedImage img = ImageIO.read(f);
					if(img!= null&&img.getHeight()!=0){
						BufferedImage st = processor.process(img);
						String ext = "png";
						if(st.getType()==BufferedImage.TYPE_INT_RGB){
							ext = "jpg";
						}
						int rand = (int)(Math.random()*100000000);
						ImageIO.write(st, ext, new File(outputRoot.getAbsolutePath()+SEP+(prefix+count)+rand+"."+ext));
						System.out.print(".");
						if(count%100==0){
							System.out.println();
						}
						count++;
					}else{
						System.out.println("Probably not an image: "+f.getPath());
					}
				}catch  (IOException e){
					System.out.println("Error while reading "+f.getPath());
				}
			}
		}
	}
	/**
	 * copy dir structure and images in inputDir, 
	 * and output resized images to outputDir/inputDirname/ in corresponding structure. 
	 * rename images with "scale-"+randInt<br>
	 * <b>Note:recursive</b>
	 * @param inputDir
	 * @param outputDir
	 * @param maxSize max size of the output image, 
	 * whose all sides are less than or equal to {@code maxSize}
	 */
	public static void scaleImageDir(String inputDir, String outputDir, int maxSize){
		batchEditImages(inputDir, outputDir, "scale-", new ImgProcessor(){
			@Override
			public BufferedImage process(BufferedImage img) {
				BufferedImage st = null;
				if(img.getWidth()<=maxSize&&img.getHeight()<=maxSize){
					st = img;
				}else{
					Dimension des = TFUtils.scaleUniformFit(img.getWidth(), img.getHeight(), maxSize, maxSize);
					st = TFUtils.getScaledImage(img, des.width, des.height);
				}
				return st;
			}		
		});
	}
	/**
	 * <b>Note:recursive</b>
	 * @param dir
	 */
	private static void deleteEmptyDirs(File dir) {
		if(dir.list().length==0){
			//dir.delete();
			System.out.println("Delete"+dir.getPath());
			try {
				Files.delete(Paths.get(dir.getAbsolutePath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		for(File f: dir.listFiles()){
			if(f.isDirectory()){
				deleteEmptyDirs(f);			
			}
		}
		if(dir.list().length==0){
			//dir.delete();
			System.out.println("Delete "+dir.getPath());
			try {
				Files.delete(Paths.get(dir.getAbsolutePath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
	}
	/*
	public static void addPrefixFolder(String root, String prefix){
		File   file = new   File("D:/gai.jpg");   //指定文件名及路径  
		String name="123";     
		String filename = file.getAbsolutePath();     
		if(filename.indexOf(".")>=0){     
	       filename   =   filename.substring(0,filename.lastIndexOf("."));     
	    }     
	    file.renameTo(new   File(name+".jpg"));   //改名 
	}*/
	/**
	 * Print paths to images that does not meet any of the requirements
	 * <b>Note:recursive</b>
	 * @param rootDir div to be checked
	 * @param minW
	 * @param minH
	 * @param maxW if maxW<=0, it is ignored
	 * @param maxH if maxH<=0, it is ignored
	 * @param divs if not null, paths to the unqualified images are stored there
	 * @param outputDir if not null, unqualified images are moved to this dir
	 */
	public static void checkDimensionsRec(File rootDir, int minW, int minH, int maxW, int maxH, ArrayList<String> divs, String outputDir){
			for(File entry : rootDir.listFiles()){
				if(entry.isDirectory()){
					checkDimensionsRec(entry, minW, minH, maxW, maxH, divs, outputDir);
				}else{
					try{
						BufferedImage img = ImageIO.read(entry);
						if(img!= null&&img.getHeight()!=0){
							int w = img.getWidth();
							int h = img.getHeight();
							boolean add = false;
							if(w<minW){
								System.out.println(entry.getPath()+"'s width is smaller than "+minW);
								add=true;
							}
							if(h<minH){
								System.out.println(entry.getPath()+"'s height is smaller than "+minW);
								add=true;
							}
							if(maxW>0&&w>maxW){
								System.out.println(entry.getPath()+"'s height is larger than "+maxW);
								add=true;
							}
							if(maxH>0&&h>maxH){
								System.out.println(entry.getPath()+"'s height is larger than "+maxW);
								add=true;
							}
							if(add&&divs!=null&&outputDir!=null){
								divs.add(entry.getName());
								entry.delete();
								ImageIO.write(img, "jpg", new File(outputDir+SEP+entry.getName()));
							}
						}else{
							System.out.println("Probably not an image: "+entry.getPath());
						}
					}catch  (IOException e){
						System.out.println("Error while reading "+entry.getPath());
					}
				}
			}		
	}
	/**
	 * handy helper function to move inappropriate images to a dir 
	 * and print filenames in html divs so that these images can be captured in a web page
	 * <b>Note:recursive</b>
	 * @param root dir to be checked
	 * @param minW
	 * @param minH
	 * @param maxW if maxW<=0, it is ignored
	 * @param maxH if maxH<=0, it is ignored
	 */
	private static void checkDimensions(File root, int minW, int minH, int maxW, int maxH) {
		ArrayList<String> divs = new ArrayList<String>();
		String webPath = "img/tietu/";
		checkDimensionsRec(root, minW, minH, maxW, maxH, divs, "F:/tmp");
		System.out.println("F:/tmp");
		for(String name : divs){
			System.out.println(String.format("<div style=\"background-image: url('%s%s');"
					, webPath, name)+ "height: 536px; width: 100%; border: 2px solid green;\">"
					+"</div>");
		}
		
	}
	/**
	 * For styleClassifier data preparation.<b>For example:<br></b>
	 * with rooms = {"living-room", "dining-room"}<br>
	 * and dirPath = "chinese";
	 * chinese/suit1/living-room<br>
	 * chinese/suit1/dining-room<br>
	 * chinese/suit2/living-room<br>
	 * chinese/suit2/dining-room<br>
	 * chinese/suit2/foo<br>
	 * <b>will become<br></b>
	 * chinese/living-room<br>
	 * chinese/dining-room<br>
	 * <b>Note:not recursive</b>
	 * @param dirPath
	 * @param rooms
	 */
	private static void putRoomsTogetherWithinAStyle(String dirPath, String[] rooms){
		File f = new File(dirPath);
		if(f.isDirectory()){
			File[] suits = f.listFiles();
			if(suits.length==0)return;
			for(String r: rooms){
				File roomDir = new File(dirPath+SEP+r);
				if(!roomDir.mkdirs()){
					System.err.println("Failed to creat dir for room: "+ r);
					return;
				}
			}
			System.out.println("Copying files");
			FileInputStream fis; 
		    FileOutputStream fos; 
		    byte[] b = new byte[1024]; 
		    int a; 
			for(File su: suits){
				if(su.getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)!=-1)continue;
				for(File room: su.listFiles()){
					if(Arrays.asList(rooms).contains(room.getName())){
						for(File img: room.listFiles()){
							if(img.getParentFile().getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)!=-1)continue;
							try{
								fis = new FileInputStream(img); 
						        fos = new FileOutputStream(new File(dirPath+SEP+room.getName()+SEP+img.getName())); 
						        while ((a = fis.read(b)) != -1) { 
						          fos.write(b, 0, a); 
						        } 
							}catch(IOException  e){
								e.printStackTrace();
								System.out.println("Cannot move file: "+img.getAbsolutePath());
							}
							
							//if(!img.renameTo(new File(dirPath+SEP+room.getName()+SEP+img.getName()))){
								
							//}
						}
					}
				}
			}
		}
	}
	/**
	 * generate randomly selected files for training and evaluation from a processed dataset
	 * <b>Note:recursive</b>
	 * @param rootPath create folders "train" and "eval" under the root dir and
	 * keep the structure of the root dir inside these new dirs. 
	 * @param percentForEval with a probability of percentForEval/1, 
	 * files in the root dir are copied to the eval dir, the rest are copied to the train dir
	 */
	public static void generateTrainAndEvalSets(String rootPath, double percentForEval){
		File f = new File(rootPath);
		if(f.isDirectory()){
			File[] children = f.listFiles();
			if(children.length==0)return;
			
			File trainDir = new File(rootPath+SEP+"train");
			if(!trainDir.mkdirs()){
				System.err.println("Failed to creat dir for training.");
				return;
			}
			
			File evalDir = new File(rootPath+SEP+"eval");
			if(!evalDir.mkdirs()){
				System.err.println("Failed to creat dir for evaluation.");
				return;
			}
			System.out.println("Copying files");
			FileInputStream fis; 
		    FileOutputStream fos; 
		    byte[] b = new byte[1024]; 
		    int a; 
			for(File sub: children){//NEVER USE list() HERE! causes infinite loops
				if(sub.isDirectory()){
					if(sub.getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)==-1){
						generateTrainAndEvalSetsRec(sub, trainDir, evalDir, percentForEval);
					}
				}else{
					if(sub.getParentFile().getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)!=-1)continue;
					try{
						fis = new FileInputStream(sub); 
						String target;
						if(Math.random()>percentForEval){
							target = trainDir.getAbsolutePath()+SEP+sub.getName();
						}else{
							target = evalDir.getAbsolutePath()+SEP+sub.getName();
						}
				        fos = new FileOutputStream(new File(target)); 
				        while ((a = fis.read(b)) != -1) { 
				          fos.write(b, 0, a); 
				        } 
					}catch(IOException  e){
						e.printStackTrace();
						System.out.println("Cannot copy file: "+sub.getAbsolutePath());
					}
				}
			}
		}else{
			System.err.println("Must supply a root directory.");
		}
	}
	private static void generateTrainAndEvalSetsRec(File dir, File trainDir, File evalDir, double percentForEval) {
		File[] children = dir.listFiles();
		if(children.length==0)return;
		trainDir = new File(trainDir+SEP+dir.getName());
		if(!trainDir.mkdirs()){
			System.err.println("Failed to creat dir for training.");
			return;
		}
		evalDir = new File(evalDir+SEP+dir.getName());
		if(!evalDir.mkdirs()){
			System.err.println("Failed to creat dir for evaluation.");
			return;
		}
		FileInputStream fis; 
	    FileOutputStream fos; 
	    byte[] b = new byte[1024]; 
	    int a; 
		for(File sub: children){
			if(sub.isDirectory()){
				if(sub.getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)==-1){
					generateTrainAndEvalSetsRec(sub, trainDir, evalDir, percentForEval);
				}
			}else{
				if(sub.getParentFile().getAbsolutePath().indexOf(DevConstants.IGNORE_PREFIX)!=-1)continue;
				try{
					fis = new FileInputStream(sub); 
					String target;
					if(Math.random()>percentForEval){
						target = trainDir.getAbsolutePath()+SEP+sub.getName();
					}else{
						target = evalDir.getAbsolutePath()+SEP+sub.getName();
					}
			        fos = new FileOutputStream(new File(target)); 
			        while ((a = fis.read(b)) != -1) { 
			          fos.write(b, 0, a); 
			        } 
				}catch(IOException  e){
					e.printStackTrace();
					System.out.println("Cannot copy file: "+sub.getAbsolutePath());
				}
			}
		}
		
	}

	public static void main(String[] args) {
		//deleteEmptyDirs(new File("));

		//generateTrainAndEvalSets("F:/TensorFlowDev/training-materials/styles/style-only", 0.3);
		//String[] roomNames = {"餐厅","厨房","客厅","卧室","书房","卫生间"};
		//putRoomsTogetherWithinAStyle("F:\\TensorFlowDev\\training-materials\\styles\\ready\\eastern-luxurious", roomNames);
		//System.out.println("Done");
		//String rootPath = "F:\\TensorFlowDev\\training-materials\\styles\\ready\\w-lux";
		//scaleImageDir(rootPath, "F:\\TensorFlowDev\\training-materials\\styles\\style-only", 512);
		//rootPath = "F:\\TensorFlowDev\\training-materials\\styles\\ready\\w-sim";
		//scaleImageDir(rootPath, "F:\\TensorFlowDev\\training-materials\\styles\\style-only", 512);
		
		//System.out.println(rootPath);
		//scaleImageDir(rootPath, "F:/tmp", 512);
		//File root = new File(rootPath);
		//if(root.isDirectory()) checkDimensions(root, 433, 251, -1, -1);
		/*batchEditImages("F:\\TensorFlowDev\\网页\\北欧风格装修_百度图片搜索_files", 
				"F:/TensorFlowDev/网页上的图", "baidu", new ImgProcessor(){
			@Override
			public BufferedImage process(BufferedImage img) {
				int h = img.getHeight();
				int w = img.getWidth();
				if(h<300||w<300){
					Dimension d = TFUtils.scaleUniformFill(w, h, 300, 300);
					BufferedImage ret = TFUtils.getScaledImage(img, d.width, d.height);
					return ret;
				}else{
					return img;
				}
			}		
		});
		batchEditImages(rootPath, "F:/TensorFlowDev/training-materials/styles", new ImgProcessor(){
			@Override
			public BufferedImage process(BufferedImage img) {
				//mixed return img.getSubimage(37, 35, 340, 226);
				//american return img.getSubimage(29, 12, 396, 249);
				//american-simple return img.getSubimage(47, 14, 360, 244);
				//european return TFUtils.centerCrop(img,(int)(img.getHeight()*1.5), img.getHeight());
				//japanese return img.getSubimage(35, 11, 385, 250);
				//mediterranean return img.getSubimage(37, 0, 395, 256);
				//modern chinese return img.getSubimage(29, 4, 395, 257);
				//modern simple return img.getSubimage(37, 2, 395, 255);
				//northern european return img.getSubimage(43, 1, 390, 250);
				return img.getSubimage(38, 1, 395, 256);//pastoral
			}		
		});
		
		
		
		batchEditImages(rootPath, "F:/TensorFlowDev/training-materials/styles/tmp", new ImgProcessor(){
			@Override
			public BufferedImage process(BufferedImage img) {
				int X = 176;
				int Y = 20;
				int W = 2575;//picture width
				int H = 1440;//picture height
				double xMin = (double)img.getWidth()/W*X; 
				double yMin = (double)img.getHeight()/H*Y; 
				double wMax = (double)img.getWidth()/W*2290;//window width
				double hMax = (double)img.getHeight()/H*1300;//window height
				double w = wMax*(100-Math.random()*15)/100;
				double h = hMax*(100-Math.random()*15)/100;
				double xMax = xMin+wMax-w;
				double yMax = yMin+hMax-h;
				return img.getSubimage((int)(xMin+Math.random()*(xMax-xMin)), (int)(yMin+Math.random()*(yMax-yMin)), 
						(int)w, (int)h);
			}		
		});	*/
	}
}
