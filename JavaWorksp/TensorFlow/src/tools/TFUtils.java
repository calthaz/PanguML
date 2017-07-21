package tools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class TFUtils {
	public static double cropRate = 0.7;
	public static final String SEP = "/";
	public static BufferedImage cropImage(BufferedImage img){
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
     * https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
     * create a new BufferedImage and and draw a scaled version of the original on the new one.
     * @param original
     * @param newWidth
     * @param newHeight
     * @return
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
    
    public static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
          if (probabilities[i] > probabilities[best]) {
            best = i;
          }
        }
        return best;
    } 
    
	public static void readFilesRecursively(File f, ArrayList<String> files) {
		if(f.isDirectory()){
			for(File entry : f.listFiles()){
				readFilesRecursively(entry, files);
			}	
		}else{
			files.add(f.getPath());
		}
	}
	
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
	public static BufferedImage getBlankImage(int w, int h) {
		// TODO Auto-generated method stub
		BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		return ret;
	}
	
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
					Dimension des = scaleUniformFit(img.getWidth(), img.getHeight(), maxSize, maxSize);
					BufferedImage st = getScaledImage(img, des.width, des.height);
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
						if(img.getWidth()<maxSize&&img.getHeight()<maxSize){
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
						ImageIO.write(st, ext, new File(outputRoot.getAbsolutePath()+SEP+"bai"+count+rand+"."+ext));
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
	
	public static void main(String args[]){
		//deleteEmptyDirs(new File("c:/tmp/test"));
		
		String rootPath = "C:/tmp/涂料下载";
		/*
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
		}else{
			rootPath = args[0];
		}*/
		System.out.println(rootPath);
		scaleImageDir(rootPath, "c:/tmp/新硬装贴图", 512);
	}
}
