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
 * <span class="en">
 * An utility class to process training samples.<br>
 * </span>
 * <span class="zh">
 * 一个处理培训样本的工具类。
 * </span>
 *
 */
public class SampleHelper {
	
	/**
	 * <span class="en">Must implements {@code public BufferedImage process(BufferedImage img);} <br>
	 * Usage: just like FilenameFilter<br></span>
	 * <span class="zh">必须实现{@code public BufferedImage process(BufferedImage img);} <br>
	 * 用法：就像FilenameFilter一样</span>
	 * 
	 * <pre>
String[] myFiles = directory.list(new FilenameFilter() {
	public boolean accept(File directory, String fileName) {
		return fileName.endsWith(".txt");
	}
});
	 * </pre>
	 */
	public interface ImgProcessor{
		/** 
		 * <span class="zh">处理图片</span>
		 * <span class="en">Process an image</span>
		 * @param img
		 * @return <span class="zh">处理后的图片</span><span class="en">the processed image</span>
		 */
		public BufferedImage process(BufferedImage img); 
	}
	private static final String SEP = "/";
	
	private SampleHelper(){
		
	}
	/**
	 *  <span class="en">Extends the number of examples by random cropping.<br>
	 *  crop portion is specified by {@code TFUtils.cropRate}<br>
	 *  new images saved in the same dir but with new names starting with "ext-"<br>
	 *  <b>Note: not recursive</b><br></span>
	 *  <span class="zh">通过随机裁剪来扩展样本的数量
	 * 裁剪部分由{@code TFUtils.cropRate} <br>指定
	 * 新图像保存在同一个目录中，但以“ext-”开头的新名称<br>
	 * <b>注意：没有递归</b></span>
	 * @param directoryPath 
	 * <span class="zh">样本目录，只有该目录下文件会被扩充</span>
	 * <span class="en">Sample directory, only the files in the directory will be extended</span>
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
	 * <span class="en">Copy the entire structure of {@code inputDir} to {@code outputDir/inputDirName} 
	 * with images processed by a processor inside. <br>
	 * <b>Note:recursive</b></span>
	 * <span class="zh">将{@code inputDir}的整个结构复制到{@code outputDir / inputDirName}
	 * 由处理器内部处理的图像。<br>
	 * <b>注意：递归</b></span>
	 * @param inputDir
	 * @param outputDir
	 * @param prefix 
	 * <span class="zh">用"prefix"+randInt重命名文件</span>
	 * <span class="en">rename images with "prefix"+randInt<br></span>
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
	 * <span class="en">
	 * Copy dir structure and images in inputDir, 
	 * and output resized images to outputDir/inputDirname/ in corresponding structure. <br>
	 * Rename images with "scale-"+randInt<br>
	 * <b>Note:recursive</b><br>
	 * </span>
	 * <span class ="zh">
	 * 复制dir结构和图像在inputDir中，
	 * 并在相应的结构中输出调整大小的图像到outputDir/inputDirname/。
	 * 用"scale - "+ randInt <br>重命名图像
	 * <b>注意：递归</b>
	 * </span>
	 * @param inputDir
	 * @param outputDir
	 * @param maxSize <span class="zh">输出图像的最大尺寸，
	 * 图像的所有边都小于或等于{@code maxSize}</span>
	 * <span class="en">max size of the output image, 
	 * whose all sides are less than or equal to {@code maxSize}</span>
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
	 * <b class="en">Note:recursive</b>
	 * <b class="zh">注意:递归</b>
	 * @param dir
	 */
	public static void deleteEmptyDirs(File dir) {
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
	 * <span class="en">Print paths to images that does not meet any of the requirements
	 * <b>Note:recursive</b></span>
	 * <span class="zh">打印不符合任何要求的图像路径
	 * <b>注意：递归</b></span>
	 * 
	 * @param rootDir <span class="zh">检查的文件夹</span><span class="en">div to be checked</span>
	 * @param minW
	 * @param minH
	 * @param maxW <span class="zh">如果{@code maxW<=0}, 无视它</span><span class="en">If {@code maxW<=0}, it is ignored</span>
	 * @param maxH <span class="zh">如果{@code maxH<=0}, 无视它</span><span class="en">If {@code maxH<=0}, it is ignored</span>
	 * @param divs <span class="zh">如果不为null，则存储不合格图像的原始路径</span>
	 * <span class="en">if not null, original paths to the unqualified images are stored there</span>
	 * @param outputDir <span class="zh">如果不为null，则不合格的图像被移动到该目录</span>
	 * <span class="en">if not null, unqualified images are moved to this dir</span>
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
							if(add&&divs!=null){
								divs.add(entry.getName());
							}
							if(add&&outputDir!=null){
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
		checkDimensionsRec(root, minW, minH, maxW, maxH, divs, "/tmp");
		System.out.println("/tmp");
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
	 * <b>will become 会变成<br></b>
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
	 * <span class="en">Generate randomly selected files for training and evaluation from a processed dataset
	 * <b>Note:recursive</b></span>
	 * <span class="zh">从处理过的数据集生成随机选择的训练和评估文件
	 * <b>注意：递归</b></span>
	 * @param rootPath 
	 * <span class="zh">在根目录下创建文件夹"train"和"eval"
	 * 将根目录的结构保留在这些新的目录下。</span>
	 * <span class="en">Create folders "train" and "eval" under the root dir and
	 * Keep the structure of the root dir inside these new dirs. </span>
	 * @param percentForEval 
	 * <span class="zh">以percentForEval/1的概率，
	 * 根目录中的文件被复制到eval目录中，其余的将被复制到train目录中.</span>
	 * <span class="en">With a probability of percentForEval/1, 
	 * files in the root dir are copied to the eval dir, the rest are copied to the train dir.</span>
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
	/**
	 * <span class="en">Test</span>
	 * <span class="zh">测试</span>
	 * @param args 
	 */
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
