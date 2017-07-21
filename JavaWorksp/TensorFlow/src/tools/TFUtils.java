package tools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class TFUtils {
	public static double cropRate = 0.7;
	public static final String SEP = "/";
	
	/**
	 * random crop, since normal cropping is not complicated. 
	 * crop portion is specified by {@code TFUtils.cropRate}
	 * @return a randomly selected region from {@code img}
	 */
	public static BufferedImage randomCropImage(BufferedImage img){
		int w = (int) (img.getWidth()*cropRate);
		int h = (int)(img.getHeight()*cropRate);
		int startX = (int) (Math.random()*(img.getWidth()-w));
		int startY = (int) (Math.random()*(img.getHeight()-h));
		int[] raw = new int[w*h];
		//img.getRaster().getPixels(0, 0, PIC_SIZE, PIC_SIZE, raw);java.lang.ArrayIndexOutOfBoundsException: 784
		raw = img.getRGB(startX, startY, w, h, raw, 0, w);
		BufferedImage output = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);			  
		output.setRGB(0, 0, w, h, raw, 0, w);
		return output;
	}
	/**
	 * 
	 * @param img
	 * @param width
	 * @param height
	 * @return cropped img
	 * @throws Exception if img is smaller than the desired size
	 */
	public static BufferedImage centerCrop(BufferedImage img, int width, int height) throws IllegalArgumentException{
		int w = img.getWidth();
		int h = img.getHeight();
		int startX = (w-width)/2;
		int startY = (h-height)/2;
		if(startX<0||startY<0){
			throw new IllegalArgumentException("img is smaller than the desired size "+width+"x"+height);
		}
		int[] raw = new int[width*height];
		raw = img.getRGB(startX, startY, width, height, raw, 0, width);
		BufferedImage output = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);			  
		output.setRGB(0, 0, width, height, raw, 0, width);
		return output;
	}
	/**
     * create a new BufferedImage and and draw a scaled version of the original on the new one.<br>
     * 方法来自<a href="https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage">this stackoverflow page</a>
     * @param original
     * @param newWidth
     * @param newHeight
     * @return a scaled img
     */
    public static BufferedImage getScaledImage(BufferedImage original, int newWidth, int newHeight){
  	  BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
  	  Graphics2D g = resized.createGraphics();
  	  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
  	      RenderingHints.VALUE_INTERPOLATION_BILINEAR);
  	  g.drawImage(original, 0, 0, newWidth, newHeight, 0, 0, original.getWidth(),
  	      original.getHeight(), null);
  	  g.dispose();
  	  return resized;
    }
    
    /**
     * similar to {@code tf.argmax(tensor, axis = 0)}
     * @param probabilities
     * @return
     */
    public static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
          if (probabilities[i] > probabilities[best]) {
            best = i;
          }
        }
        return best;
    } 
    
    /**
     * read files and put paths to the files in 
     * @param f
     * @param files 
     * @return
     */
	public static void readFilesRecursively(File f, ArrayList<String> files) {
		if(f.isDirectory()){
			for(File entry : f.listFiles()){
				readFilesRecursively(entry, files);
			}	
		}else{
			files.add(f.getPath());
		}
	}
	
	 /**
     * read files, check if they are readable images, 
     * and put paths to the files in to the passed-in arraylist
     * @param f
     * @param files 
     * @return
     */
	public static void readImageFilesRecursively(File f, ArrayList<String> files) {
		if(f.isDirectory()){
			for(File entry : f.listFiles()){
				readImageFilesRecursively(entry, files);
			}	
		}else{
			try{
				//reads every image in case it were a bad one 
				BufferedImage img = ImageIO.read(f);
				if(img!= null&&img.getHeight()!=0){
					files.add(f.getPath());
				}else{
					System.out.println("Probably not an image: "+f.getPath());
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+f.getPath());
			}
		}
	}
	
	/**
	 * get a transparent image of {@code TYPE_INT_ARGB} of given size
	 * @param w
	 * @param h
	 * @return image
	 */
	public static BufferedImage getBlankImage(int w, int h) {
		BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		return ret;
	}
	
	/**
	 * make at least one side equal to destination size, 
	 * and ensure the whole returned dimension can be contained in the destination area,
	 * leaving empty space if necessary.<br> 
	 * <b>Keep aspect ratio. </b>
	 * @param origW
	 * @param origH
	 * @param destiW
	 * @param destiH
	 * @return Dimension (width,height)
	 */
	public static Dimension scaleUniformFit(int origW, int origH, int destiW, int destiH){
		double ratio = (double)origW/origH;
		Dimension d = new Dimension();
		if ((double)destiW/destiH<ratio){
			d.width=destiW;
			d.height=(int)(destiW/ratio);
		}else{
			d.height=destiH;
			d.width=(int) (destiH*ratio);
		}
		return d;
	}
	/**
	 * make at least one side equal to destination size, 
	 * fill the destination region and leave out some extra area of the origin object<br> 
	 * <b>Keep aspect ratio. </b>  
	 * @param origW
	 * @param origH
	 * @param destiW
	 * @param destiH
	 * @return Dimension (width,height)
	 */
	public static Dimension scaleUniformFill(int origW, int origH, int destiW, int destiH){
		double ratio = (double)origW/origH;
		Dimension d = new Dimension();
		if ((double)destiW/destiH<ratio){
			d.height=destiH; //TODO: it seems that I've got it wrong here? 
			d.width=(int) (destiH*ratio);
		}else{
			d.width=destiW;
			d.height=(int)(destiW/ratio);
		}
		return d;
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
				scaleImageDirRecursive(inputRoot, realOutputRoot, maxSize, count);
			}else{
				System.err.println("Create dir failed "+realOutputRoot.getAbsolutePath());
			}
		}else{
			try{
				BufferedImage img = ImageIO.read(inputRoot);
				if(img!= null&&img.getHeight()!=0){
					BufferedImage st = null;
					if(img.getWidth()<=maxSize&&img.getHeight()<=maxSize){
						st = img;
					}else{
						Dimension des = scaleUniformFit(img.getWidth(), img.getHeight(), maxSize, maxSize);
						st = getScaledImage(img, des.width, des.height);
					}
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
	/**
	 * 
	 * @param inputRoot must be a dir
	 * @param outputRoot must also be a dir
	 * @param maxSize maxSize of an image
	 */
	private static void scaleImageDirRecursive(File inputRoot, File outputRoot, int maxSize, int count) {
		// TODO Auto-generated method stub
		for(File f : inputRoot.listFiles()){
			if(f.isDirectory()){
				File nextOut = new File(outputRoot+SEP+f.getName());
				if(nextOut.mkdirs()){
					scaleImageDirRecursive(f, nextOut, maxSize, count);
				}else{
					System.err.println("Create dir failed "+nextOut.getAbsolutePath());
				}
				deleteEmptyDirs(nextOut);
			}else{
				try{
					BufferedImage img = ImageIO.read(f);
					if(img!= null&&img.getHeight()!=0){
						BufferedImage st = null;
						if(img.getWidth()<=maxSize&&img.getHeight()<=maxSize){
							st = img;
						}else{
							Dimension des = scaleUniformFit(img.getWidth(), img.getHeight(), maxSize, maxSize);
							st = getScaledImage(img, des.width, des.height);
						}
						String ext = "png";
						if(st.getType()==BufferedImage.TYPE_INT_RGB){
							ext = "jpg";
						}
						int rand = (int)(Math.random()*100000000);
						ImageIO.write(st, ext, new File(outputRoot.getAbsolutePath()+SEP+("bai"+count)+rand+"."+ext));
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
	/**
	 * Check accuracy in a fool-proof way
	 * @param resultFile
	 * @param labelFile
	 * @return accuracy by category
	 */
	public static double[] checkAccuracy(String resultFile, String labelFile){
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile(labelFile);
		int[] sum = new int[labels.size()+1];
		int[] correct = new int[labels.size()+1];
		double[] ret = new double[labels.size()+1];
		try {
			Scanner sc  = new Scanner(new File(resultFile));
			while(sc.hasNextLine()){
				String line = sc.nextLine();
				if(line.indexOf(LabelGenerator.LABEL_SEP)!=-1){
					line = line.trim();
					//System.out.println(line);
					String path = line.substring(0,line.indexOf(LabelGenerator.LABEL_SEP));
					String m = line.substring(line.indexOf(LabelGenerator.LABEL_SEP)+LabelGenerator.LABEL_SEP.length());
					String label = m.substring(0,m.indexOf(LabelGenerator.LABEL_SEP));
					int index = labels.indexOf(label);
					sum[index]++;
					if(path.indexOf(label)!=-1){
						correct[index]++;
					}
				}
			}
			sc.close();
			for(int i=0; i<labels.size(); i++){
				ret[i] = (double)correct[i]/sum[i];
				correct[labels.size()]+=correct[i];
				sum[labels.size()]+=sum[i];
			}
			ret[labels.size()] = (double)correct[labels.size()]/sum[labels.size()];
		} catch (FileNotFoundException e) {
			System.err.println("Result file not found");
		}
		return ret;
	}
	public static void main(String args[]){
		//deleteEmptyDirs(new File("c:/tmp/test"));
		
		//String rootPath = "C:/tmp/hardware";
		/*
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
		}else{
			rootPath = args[0];
		}*/
		//System.out.println(rootPath);
		//scaleImageDir(rootPath, "c:/tmp/新硬装贴图", 512);
		//File root = new File(rootPath);
		//if(root.isDirectory()) checkDimensions(root, 128, 128, -1, -1);
		System.out.println("-----------crop----------------");
		double[] result = checkAccuracy("C:/tmp/hardware/crop-tf-inference-results.txt", "C:/tmp/hardware/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		System.out.println("-----------resize----------------");
		result = checkAccuracy("C:/tmp/hardware/resize-tf-inference-results.txt", "C:/tmp/hardware/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		System.out.println("-----------beds----------------");
		//
		result = checkAccuracy("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/furn-91126tf-inference-results.txt", 
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		System.out.println("-----------3 furn----------------");
		//
		result = checkAccuracy("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/furpics/furn-5955tf-inference-results.txt", 
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/furpics/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}

	}
}
