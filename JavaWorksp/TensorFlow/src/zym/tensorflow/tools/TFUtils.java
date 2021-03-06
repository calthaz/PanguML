package zym.tensorflow.tools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;
/**
 * <span class="en">
 * An utility class<br>
 * </span>
 * <span class="zh">
 * 一个工具类。
 * </span>
 *
 */
public class TFUtils {
	private TFUtils(){
		
	}
	public static double cropRate = 0.7;
	public static final String SEP = "/";
	
	/**
	 * 
	 * <span class="zh">随机裁剪，因为正常裁剪并不复杂。
	 * 裁剪部分大小由{@code TFUtils.cropRate}指定</span>
	 * <span class="en">random crop, since normal cropping is not complicated. 
	 * crop portion is specified by {@code TFUtils.cropRate}</span>
	 * @return <span class="zh">来自{@code img}的随机选择区域</span>
	 * <span class="en">a randomly selected region from {@code img}</span>
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
	 * <span class="zh">中心裁剪</span>
	 * @param img
	 * @param width
	 * @param height
	 * @return cropped img
	 * @throws IllegalArgumentException <span class="zh">如果图片小于目标尺寸</span>
	 * <span class="en">if img is smaller than the desired size</span>
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
	 * <div class="zh">创建一个新的BufferedImage，并按照
	 * <a href="https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage">此stackoverflow页面</a>上的建议，
	 * 绘制缩放的图片 </div>
	 * <div class="en">Create a new BufferedImage and and draw a scaled version of the original on the new one, as suggested
     * by <a href="https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage">this stackoverflow page</a></div>
     * @param original
     * @param newWidth
     * @param newHeight
     * @return <span class="zh">缩放后的图片</span><span class="en">a scaled img</span>
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
     * <span class="zh">和{@code tf.argmax(tensor, axis = 0)}相似</span>
     * <span class="en">similar to {@code tf.argmax(tensor, axis = 0)}</span>
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
     * <span class="zh">递归读取文件并把路径放在ArrayList里面</span>
     * <span class="en">read files and put paths to the files in ArrayList files</span>
     * 
     * @param f
     * @param files <span class="zh">储存路径</span><span class="en">store the paths</span>
     * 
     */
	public static void readFilesRecursively(File f, ArrayList<String> files) {
		if(null==files){
			return;
		}
		if(f.isDirectory()){
			for(File entry : f.listFiles()){
				readFilesRecursively(entry, files);
			}	
		}else{
			files.add(f.getPath());
		}
	}
	
    /**
     * <span class="zh">递归读取文件，检查它们是否是可读取的图像，
     * 并将文件的路径放入传入的ArrayList中</span>
     * <span class="en">Read files, check if they are readable images, 
     * and put paths to the files in to the passed-in ArrayList</span>
     * 
     * @param f
     * @param files <span class="zh">储存路径</span><span class="en">store the paths</span>
     * 
     */
	public static void readImageFilesRecursively(File f, ArrayList<String> files) {
		if(null==files){
			return;
		}
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
	 * <span class="zh">获取给定尺寸的{@code TYPE_INT_ARGB}的透明图像</span>
	 * <span class="en">Get a transparent image of {@code TYPE_INT_ARGB} of given size</span>
	 * @param w
	 * @param h
	 * @return image
	 */
	public static BufferedImage getBlankImage(int w, int h) {
		BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		return ret;
	}
	
	/**
	 * <span class="en">make at least one side equal to destination size, 
	 * and ensure the whole returned dimension can be contained in the destination area<br> 
	 * <b>Keep aspect ratio.</b></span>
	 * <span class="zh">使至少一边等于目的大小，
	 * 并确保整个返回的尺寸可以包含在目的区域内。<br> 
	 * <b>保持纵横比。</b></span>
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
	 * <span class="zh">使至少一边等于目的地大小，
	 * 填满目的地区域，并有可能有额外区域<br>
	 * <b>保持纵横比。</b></span>
	 * <span class="en">make at least one side equal to destination size, 
	 * fill the destination region and leave out some extra area of the origin object<br> 
	 * <b>Keep aspect ratio. </b>  </span>
	 * 
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
	 * <span class="zh">以原始的方式检查预测准确率。</span>
	 * <span class="en">Check accuracy in a foolproof way.</span>
	 * <pre>
	 * if(path.indexOf(label)!=-1)correct[index]++;
	 * </pre>
	 * 
	 * @param resultFile
	 * @param labelFile
	 * @return double[classIndex]=accuracy<br><div class="zh">
	 * 精度=（机器判断正确）/（机器认为是在这个类）<br>
	 * 即1-精度=假阳性率(?)</div>
	 * <div class="en">accuracy = (machine got it right)/(machine thought it to be in this class)<br>
	 * i.e. 1-accuracy = false positive rate</div>
	 * <br>
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
					/*
					if(label.indexOf("\\")!=-1){
						//label = label.substring(0,label.indexOf("\\"));
						label = label.substring(label.indexOf("\\")+1);
					}*/
					
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
	
	/**
	 * <span class="zh">根据{@code resultFile}中的机器的分类结果将文件复制到{@code outputPath}下单独的文件夹中</span>
	 * <span class="en">Copy files into separate folders in {@code outputPath} according to machine's classification in {@code resultFile}</span>
	 * @param resultFile
	 * @param labelFile
	 * @param outputPath
	 */
	public static void sortByClass(String resultFile, String labelFile, String outputPath){
		
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile(labelFile);
		File outputDir = new File(outputPath);
		if(!outputDir.isDirectory()){
			System.out.println("output path "+outputPath+" should be a directory.");
		}
		if(labels.size()>0){
			for(String l:labels){
				new File(outputPath+SEP+l).mkdirs();
			}
			
			try {
				Scanner sc = new Scanner(new File(resultFile));
				FileInputStream fis; 
				FileOutputStream fos; 
				byte[] b = new byte[1024]; 
				int a; 
				while(sc.hasNextLine()){
					String str = sc.nextLine();
					//System.out.println(str);
					String[] line = LabelGenerator.parseResultLine(str, 2);
					if(null!=line[0]&&null!=line[1]){
						File sub = new File(line[0]);
						String label = line[1];
						if(labels.indexOf(label)==-1){
							System.out.println(label+" is not in the label list.");
						}
						try{
							fis = new FileInputStream(sub); 
							String target = outputPath+SEP+label+SEP+sub.getName();
					        fos = new FileOutputStream(new File(target)); 
					        while ((a = fis.read(b)) != -1) { 
					          fos.write(b, 0, a); 
					        } 
						}catch(IOException  e){
							System.out.println("Cannot copy file: "+sub.getAbsolutePath());
						} 
					}
					
				}
				sc.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

		}
	}
	/**
	 * <span class="en">Test</span>
	 * <span class="zh">测试</span>
	 * @param args 
	 */
	public static void main(String args[]){
		//deleteEmptyDirs(new File("c:/tmp/test"));
		
		String rootPath = "C:/tmp/风格";
		
		//if(args.length<1){
			//rootPath = System.getProperty("user.dir");
		//}else{
			//rootPath = args[0];
		//}
		
		/*System.out.println(rootPath);
		sortByClass("F:/TensorFlowDev/training-materials/styles/style-only/eval/furn-224-tf-inference-results.txt",
				"F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt",
				"F:\\TensorFlowDev\\training-materials\\styles\\tmp");*/
		
		System.out.println("-----------crop----------------");
		double[] result = checkAccuracy("F:/TensorFlowDev/training-materials/hardware/crop-tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/hardware/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		System.out.println("-----------resize----------------");
		result = checkAccuracy("F:/TensorFlowDev/training-materials/hardware/resize-tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/hardware/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		System.out.println("-----------beds----------------");
		//F:\TensorFlowDev\PythonWorksp\TensorFlow\furniture\bed/furn-47010tf-inference-results.txt91126
		result = checkAccuracy("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/furn-224-tf-inference-results.txt", 
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/tf-labels-to-text.txt");
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/tf-labels-to-text.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		/*
		 * 32
		baby-bed:0.9955947136563876
		bunk-bed:0.9873873873873874
		double-bed:0.9588744588744589
		hammock:0.9858407079646018
		round-bed:0.9743589743589743
		single-bed:0.9563758389261745
		total:0.9361170592433976
		 * 224
		baby-bed:0.9420935412026726
		bunk-bed:0.9229390681003584
		double-bed:0.904862579281184
		hammock:0.9507908611599297
		round-bed:0.9572649572649573
		single-bed:0.9403508771929825
		total:0.9782298358315489
		 * 
		 * 
		 * */
		System.out.println("-----------3 furn----------------");
		//
		result = checkAccuracy("F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/furpics/furn-5955tf-inference-results.txt", 
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/furpics/tf-labels-to-text.txt");
		for(double s : result){
			System.out.println(s);
		}
		
		System.out.println("----------- styles and rooms ----------------");
		/*result = checkAccuracy("F:/TensorFlowDev/training-materials/styles/furn-55795tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/styles/tf-labels-to-text.txt");
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/training-materials/styles/tf-labels-to-text.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		System.out.println("----------- BY STYLE ----------------");
		result = checkAccuracy("F:/TensorFlowDev/training-materials/styles/furn-55795tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/styles/tf-labels-to-text-collapse-by-style.txt");
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/training-materials/styles/tf-labels-to-text-collapse-by-style.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		System.out.println("----------- BY ROOM ----------------");
		result = checkAccuracy("F:/TensorFlowDev/training-materials/styles/furn-55795tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/styles/tf-labels-to-text-collapse-by-room.txt");
		labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/training-materials/styles/tf-labels-to-text-collapse-by-room.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		F:/TensorFlowDev/training-materials/styles/style-only/eval/furn-35858tf-inference-results.txt*/
		result = checkAccuracy("F:/TensorFlowDev/training-materials/styles/style-only/train/furn-128-tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt");
		labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		result = checkAccuracy("F:/TensorFlowDev/training-materials/styles/style-only/eval/furn-224-7245-tf-inference-results.txt", 
				"F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt");
		labels = LabelGenerator.readLabelsFromFile("F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt");
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		/*
		128 train
		western-sim:0.9964912280701754
		western-lux:0.99822695035461
		japanese:1.0
		chinese:1.0
		total:0.9986498649864987
		128 eval
		western-sim:0.41509433962264153
		western-lux:0.4122448979591837
		japanese:0.46875
		chinese:0.5037878787878788
		total:0.4498997995991984
		224 train
		western-sim:0.9185441941074524
		western-lux:0.9290780141843972
		japanese:0.9583333333333334
		chinese:0.9376083188908145
		total:0.9351935193519352
		224 eval
		western-sim:0.5225563909774437
		western-lux:0.5311203319502075
		japanese:0.6220095693779905
		chinese:0.5957446808510638
		total:0.5661322645290581
		224 eval global_step=7245
		western-sim:0.5263157894736842
		western-lux:0.5308641975308642
		japanese:0.625
		chinese:0.594306049822064
		total:0.5671342685370742
		 */
	}
}
