package tools;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * extend image samples by random crop<br>
 * jpgs and pngs only
 *
 */
public class SampleHelper {
	
	/**
	 * Must implements {@code public BufferedImage process(BufferedImage img);} 
	 */
	private interface ImgProcessor{
		public BufferedImage process(BufferedImage img); 
		/*
		  String[] myFiles = directory.list(new FilenameFilter() {
			    public boolean accept(File directory, String fileName) {
			        return fileName.endsWith(".txt");
			    }
			});
		 
		  */
	}
	private static final String SEP = "/";
	private static String outputPrefix = "dfgj";
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

	public static void batchEditImages(String inputDir, String outputDir, ImgProcessor processor){
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
				batchEditImagesRecursive(inputRoot, realOutputRoot, processor, count);
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

	private static void batchEditImagesRecursive(File inputRoot, File outputRoot, ImgProcessor processor, int count) {
		for(File f : inputRoot.listFiles()){
			if(f.isDirectory()){
				File nextOut = new File(outputRoot+SEP+f.getName());
				if(nextOut.mkdirs()){
					batchEditImagesRecursive(f, nextOut, processor, count);
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
						ImageIO.write(st, ext, new File(outputRoot.getAbsolutePath()+SEP+(outputPrefix+count)+rand+"."+ext));
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
	 * rename images with /[a-z][0-9]/ (?)
	 * @param inputDir
	 * @param outputDir
	 * @param maxSize max size of the output image, all sides are less than or equal to {@code maxSize}
	 */
	public static void scaleImageDir(String inputDir, String outputDir, int maxSize){
		batchEditImages(inputDir, outputDir, new ImgProcessor(){
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
	 * 好像不能全删掉，为什么呐？
	 * @param dir
	 */
	private static void deleteEmptyDirs(File dir) {
		if(dir.list().length==0){
			dir.delete();
			return;
		}
		for(File f: dir.listFiles()){
			if(f.isDirectory()){
				deleteEmptyDirs(f);			
				//Deletes the file or directory denoted by this abstract pathname. 
				//If this pathname denotes a directory, then the directory must be empty in order to be deleted. 
			}
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
	private static void putRoomsTogetherWithinAStyle(String dirPath, String[] rooms){
		File f = new File(dirPath);
		if(f.isDirectory()){
			File[] suits = f.listFiles();
			if(suits.length==0)return;
			for(String r: rooms){
				File roomDir = new File(dirPath+SEP+r);
				if(!roomDir.mkdirs()){
					System.err.println("Failed to creat dir for room: "+ r);
				}
			}
			for(File su: suits){
				for(File room: su.listFiles()){
					if(Arrays.asList(rooms).contains(room.getName())){
						for(File img: room.listFiles()){
							if(!img.renameTo(new File(dirPath+SEP+room.getName()+SEP+img.getName()))){
								System.out.println("Cannot move file: "+img.getAbsolutePath());
							}
						}
					}
				}
			}
		}
	}
	public static void main(String[] args) {
		//String[] roomNames = {"餐厅","厨房","客厅","卧室","书房","卫生间"};
		//putRoomsTogetherWithinAStyle("F:/tmp/风格/pastoral", roomNames);
		
		
		
		String rootPath = "F:/tmp/styles/pastoral";
		
		//if(args.length<1){
			//rootPath = System.getProperty("user.dir");
		//}else{
			//rootPath = args[0];
		//}
		
		//System.out.println(rootPath);
		//scaleImageDir(rootPath, "F:/tmp", 512);
		//File root = new File(rootPath);
		//if(root.isDirectory()) checkDimensions(root, 433, 251, -1, -1);
		
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
		
		
		
	}
}
