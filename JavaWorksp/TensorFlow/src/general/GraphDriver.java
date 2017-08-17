package general;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import inception5h.LabelImage;
import tools.LabelGenerator;
import tools.NativeUtils;
import tools.TFUtils;

/**
 * <div class="en">Read a TensorFlow Graph that is saved by 
 * {@code graph.as_graph_def()} and {@code graph_util.convert_variables_to_constants()}
 * for making predictions.</div>
 * <div class="zh">读取用 {@code graph.as_graph_def()}和{@code graph_util.convert_variables_to_constants()}保存的TensorFlow图
 * 用来出预测。</div>
 *
 */
public class GraphDriver implements Closeable{
	
	private final int PIC_SIZE;
	private String inputName;
	private String outputName;
	private ArrayList<String> labels;
	private Session sess;
	private Graph g;
	private int BATCH_SIZE;
	
	static {
		try {
			//System.load(DevConstants.RES_ROOT+"jni/libtensorflow_jni.so");
			System.load(DevConstants.RES_ROOT+"tensorflow_jni.dll");
		} catch (UnsatisfiedLinkError e) {
			try {    
				NativeUtils.loadLibraryFromJar("/tensorflow_jni.dll"); 
				//System.out.println(path);
			} catch (IOException e2) {    
				e2.printStackTrace(); // This is probably not the best way to handle exception :-)  
			}   
		}
	}
	
	/**
	 * <div class="en"> construct a GraphDriver with an active session and a graph</div>
	 * <div class="zh">构建一个有graph和活动session的GraphDriver</div>
	 * @param modelPath
	 * @param inputName op_name in graph
	 * @param outputName op_name in graph
	 * @param labelPath
	 * @param picSize
	 * @param batchSize 
	 * <span class="zh">batch size原先为了读取model文件用，在此处没有用</span>
	 * <span class="en">batch size was used to read the model, but is not used here anymore</span>
	 */
	public GraphDriver(String modelPath, String inputName, String outputName, String labelPath, int picSize, int batchSize){
		this.PIC_SIZE = picSize;
		this.BATCH_SIZE = batchSize;
		byte[] graphDef = LabelImage.readAllBytesOrExit(Paths.get(modelPath));
		this.g = new Graph();
		g.importGraphDef(graphDef);
		this.sess = new Session(g);
		this.inputName = inputName;
		this.outputName = outputName;
		labels = LabelGenerator.readLabelsFromFile(labelPath);
		//System.out.println(labels);
	}
	
	/**
	 * <div class="en"> construct a GraphDriver with an active session and a graph<br>
	 * actually just a GraphDriver with {@code BATCH_SIZE = 1}</div>
	 * <div class="zh">构建一个有graph和活动session的GraphDriver<br>
	 * 其实就是 {@code BATCH_SIZE = 1}的GraphDriver</div>
	 * @param modelPath
	 * @param inputName op_name in graph
	 * @param outputName op_name in graph
	 * @param labelPath
	 * @param picSize
	 */
	public GraphDriver(String modelPath, String inputName, String outputName, String labelPath, int picSize){
		this(modelPath, inputName, outputName, labelPath, picSize, 1);
	}
	
	/**
	 * <div class="en">predict the category of a single image, no score appended</div>
	 * <div class="zh">预测单个图像的类别，不附加分数</div>
	 * @param imagePath string path to the image
	 * @return  <span class="zh">文本标签</span><span class="en">text label</span>
	 */
	public String infer(String imagePath, String normMethod){
		 String r = inferAndGetScore(imagePath, normMethod);
		 return r.substring(0,r.indexOf(LabelGenerator.LABEL_SEP));
	}
	
	/**
	 * <div class="en">predict the category of a single image, no score appended</div>
	 * <div class="zh">预测单个图像的类别，不附加分数</div>
	 * @param img 
	 * @return <span class="zh">文本标签</span><span class="en">text label</span>
	 */
	public String infer(BufferedImage img, String normMethod){
		 String r = inferAndGetScore(img, normMethod);
		 return r.substring(0,r.indexOf(LabelGenerator.LABEL_SEP));
	}
	
	/**
	 *
	 * <div class="en"> predict the category of a single image, with score appended in format: label(%.2f)</div>
	 * <div class="zh">预测单个图像的类别，附加分数格式为: 标签(%.2f)</div>
	 * @param imagePath string path to the image
	 * @return <span class="zh">文本标签</span><span class="en">text label</span>
	 */
	public String inferAndGetScore(String imagePath, String normMethod){
		 try (Tensor image = Tensor.create(toMatrix(imagePath, normMethod))) {
			
			 try (//Session s = smb.session();//run() cannot be called on the Session after close()
	            Tensor result = sess.runner().feed(inputName, image).fetch(outputName).run().get(0)) {
	    		//I guess these are corresponding variable names
	    		//no, they are not
	    		//they are operation names... as in tf.someOp(arg1, arg2, name="...");
	      
	    		final long[] rshape = result.shape();
	    		
	    		
	    		if (result.numDimensions() != 2 || rshape[0] != 1) {
	    			throw new RuntimeException(
	    					String.format(
	    							"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
	    							Arrays.toString(rshape)));
	    		}
	    		int nlabels = (int) rshape[1];
	    		float[] probas = result.copyTo(new float[1][nlabels])[0];
	    		
	    		int bestLabelIdx = TFUtils.maxIndex(probas);
	    		System.out.println(
	    				String.format(
	    						"BEST MATCH: %s (%.2f%% likely)",
	    						labels.get(bestLabelIdx), probas[bestLabelIdx]));
	    		return labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[bestLabelIdx]);
	    	}
		 }
	}
	
	/**
	 * <div class="en"> predict the category of a single image, with score appended in format: label(%.2f)</div>
	 * <div class="zh">预测单个图像的类别，附加分数格式为: 标签(%.2f)</div>
	 * @param img
	 * @return <span class="zh">文本标签</span><span class="en">text label</span>
	 */
	public String inferAndGetScore(BufferedImage img, String normMethod){
		 try (Tensor image = Tensor.create(toMatrix(img, normMethod))) {
			
			 try (//Session s = smb.session();//run() cannot be called on the Session after close()
	            Tensor result = sess.runner().feed(inputName, image).fetch(outputName).run().get(0)) {
	    		//I guess these are corresponding variable names
	    		//no, they are not
	    		//they are operation names... as in tf.someOp(arg1, arg2, name="...");
	      
	    		final long[] rshape = result.shape();
	    		
	    		
	    		if (result.numDimensions() != 2 || rshape[0] != 1) {
	    			throw new RuntimeException(
	    					String.format(
	    							"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
	    							Arrays.toString(rshape)));
	    		}
	    		int nlabels = (int) rshape[1];
	    		float[] probas = result.copyTo(new float[1][nlabels])[0];
	    		
	    		int bestLabelIdx = TFUtils.maxIndex(probas);
	    		for(int i=0; i<probas.length;i++){
	    			System.out.print(labels.get(i)+": "+probas[i]+"; ");
	    		}
	    		System.out.println();
	    		System.out.println(
	    				String.format(
	    						"BEST MATCH: %s (%.2f)",
	    						labels.get(bestLabelIdx), probas[bestLabelIdx]));
	    		return labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[bestLabelIdx]);
	    	}
		 }
	}
	
	/**
	 * 
	 * <div class="en">predict the category of a list of images, no score appended</div>
	 * <div class="zh">预测一个列表图片的类别，不带分数</div>
	 * @param paths to images
	 * @return <span class="zh">一列文本标签</span><br><span class="en">a list of text labels inferred </span>
	 */
	public String[] infer(String[] paths, String normMethod){
		String[] r = inferAndGetScore(paths, normMethod);
		for(String s : r){
			s = s.substring(0, s.indexOf(LabelGenerator.LABEL_SEP));
		}
		return r;
	}
	/**
	 * <div class="en">predict the category of a list of images, no score appended</div>
	 * <div class="zh">预测一个列表图片的类别，不带分数</div>
	 * @param imgs
	 * @return <span class="zh">一列文本标签</span><br><span class="en">a list of text labels inferred </span>
	 */
	public String[] infer(BufferedImage[] imgs, String normMethod){
		String[] r = inferAndGetScore(imgs, normMethod);
		for(String s : r){
			s = s.substring(0, s.indexOf(LabelGenerator.LABEL_SEP));
		}
		return r;
	}
	/**	 
	 * <div class="en"> predict the category of a single image, with score appended in format: label(%.2f)</div>
	 * <div class="zh">预测单个图像的类别，附加分数格式为: 标签(%.2f)</div>
	 * @param paths paths to the images
	 * @return <span class="zh">一列文本标签</span><br><span class="en">a list of text labels inferred </span>
	 */
	public String[] inferAndGetScore(String[] paths, String normMethod){
		 try (Tensor image = Tensor.create(toMatrix(paths, normMethod))) {
			 //System.out.println("my image "+image);
			 try (Tensor result = sess.runner().feed(inputName, image).fetch(outputName).run().get(0)) {
	    		final long[] rshape = result.shape();
	    		//System.out.println(result);	
	    		if (result.numDimensions() != 2) {
	    			throw new RuntimeException(
	    					String.format(
	    							"Expected model to produce a [M N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
	    							Arrays.toString(rshape)));
	    		}
	    		int nlabels = (int) rshape[1];
	    		float[][] probas = result.copyTo(new float[(int) rshape[0]][nlabels]);
	    		String[] textResults = new String[probas.length];
	    		
	    		//System.out.print(probas.length);
	    		for(int i=0; i<probas.length; i++){
		    		int bestLabelIdx = TFUtils.maxIndex(probas[i]);
		    		System.out.println(
		    				String.format(
		    						"No.%d BEST MATCH: %s (%.2f)",
		    						i, labels.get(bestLabelIdx), probas[i][bestLabelIdx]));
		    		textResults[i] = labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[i][bestLabelIdx]);
	    		}
	    		return textResults;
	    	}
		 }
	}
	/**
	 * <div class="en"> predict the category of a single image, with score appended in format: label(%.2f)</div>
	 * <div class="zh">预测单个图像的类别，附加分数格式为: 标签(%.2f)</div>
	 * @param imgs
	 * @param normMethod
	 * @return <span class="zh">一列文本标签</span><br><span class="en">a list of text labels inferred </span>
	 */
	public String[] inferAndGetScore(BufferedImage[] imgs, String normMethod){
		 try (Tensor image = Tensor.create(toMatrix(imgs, normMethod))) {
			 //System.out.println("my image "+image);
			 try (Tensor result = sess.runner().feed(inputName, image).fetch(outputName).run().get(0)) {
	    		final long[] rshape = result.shape();
	    		//System.out.println(result);	
	    		if (result.numDimensions() != 2) {
	    			throw new RuntimeException(
	    					String.format(
	    							"Expected model to produce a [M N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
	    							Arrays.toString(rshape)));
	    		}
	    		int nlabels = (int) rshape[1];
	    		float[][] probas = result.copyTo(new float[(int) rshape[0]][nlabels]);
	    		String[] textResults = new String[probas.length];
	    		
	    		//System.out.print(probas.length);
	    		for(int i=0; i<probas.length; i++){
		    		int bestLabelIdx = TFUtils.maxIndex(probas[i]);
		    		System.out.println(
		    				String.format(
		    						"No.%d BEST MATCH: %s (%.2f)",
		    						i, labels.get(bestLabelIdx), probas[i][bestLabelIdx]));
		    		textResults[i] = labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[i][bestLabelIdx]);
	    		}
	    		return textResults;
	    	}
		 }
	}
	/**
	 * 
	 * <div class="en">Releases resources <br>
	 * i.e. graph and session</div>
	 * <div class="zh">释放资源，即session和graph</div>
	 */
	public void close(){
		sess.close();
		g.close();
		//Blocks until there are no active Session instances referring to this Graph. 
		//A Graph is not usable after close returns.
		//so call sess.close() first
		
	}
	
  /**
   * 
   * <div class="en">process the image and return a float array with rgb color components </div>
   * <div class="zh">处理图片，返回带有rgb颜色分量的浮点数组</div>
   * @param img
   * @param normMethod 
   * <span class="zh">用来把图片处理成一样大小的方法</span>
   * <span class="en">use normMethod to get images of the same size</span>
   * @return images = [height, width, channels]
   * <span class="zh">其他处理用TensorFlow处理</span><span class="en">use tf to do whatever extra processing</span>
   */
	private float[][][] toMatrix(BufferedImage img, String normMethod){
		  float[][][] imgData = new float[PIC_SIZE][PIC_SIZE][3];
		  if(normMethod.equals("crop")){
			  int h = img.getHeight();
			  int w = img.getWidth();
			  if(h<PIC_SIZE||w<PIC_SIZE){
				  Dimension d = TFUtils.scaleUniformFill(w, h, PIC_SIZE, PIC_SIZE);
				  img = TFUtils.getScaledImage(img, d.width, d.height);
			  }
			  img = TFUtils.centerCrop(img, PIC_SIZE, PIC_SIZE);
		  }else{
			  img = TFUtils.getScaledImage(img, PIC_SIZE, PIC_SIZE);
		  }
		  
		  //A new Image object is returned which will render the image at the specified width and height by default. 
	  
		  //ImageIO.write(img, "jpg", new File("img\\myImage.jpg"));
		  int[] raw = new int[PIC_SIZE*PIC_SIZE];
	
		  raw = img.getRGB(0, 0, PIC_SIZE, PIC_SIZE, raw, 0, PIC_SIZE);
	
		  int r,g,b;
		  for(int i=0; i<PIC_SIZE*PIC_SIZE;i++){
			  r = raw[i]>>16 & 0xff;
		  	  g = raw[i]>>8 & 0xff;
		  	  b = raw[i] & 0xff;
		  	  //i = y*scansize + x
		  	  imgData[i/PIC_SIZE][i%PIC_SIZE][0] =r;//pixel stored as [y][x]
		  	  imgData[i/PIC_SIZE][i%PIC_SIZE][1] =g;
		  	  imgData[i/PIC_SIZE][i%PIC_SIZE][2] =b;
		  }
		  return imgData;
	}
	
	  /**
	   * 
	   * <div class="en">reads an image from file， process it and returns a float array with rgb color components </div>
	   * <div class="zh">从文件读取图像处理后返回带有rgb颜色分量的浮点数组</div>
	   * @param path
	   * @param normMethod    
	   * <span class="zh">用来把图片处理成一样大小的方法</span>
       * <span class="en">use normMethod to get images of the same size</span>
	   * @return images = [height, width, channels]
	   * <span class="zh">其他处理用TensorFlow处理</span><span class="en">use tf to do whatever extra processing</span>
	   */
	private float[][][] toMatrix(String path, String normMethod){
		  
		  try {
			  BufferedImage img = ImageIO.read(new File(path));
			  return toMatrix(img, normMethod);
		  } catch (IOException e) {
			  System.out.println("can't open image "+path);
			  return null;
		  }
	}
      
	private float[][][][] toMatrix(String[] paths, String normMethod) {
		float[][][][] data = new float[paths.length][PIC_SIZE][PIC_SIZE][3];
		for(int i=0; i<paths.length; i++){
			data[i] = toMatrix(paths[i], normMethod);
		}
		return data;
	}
	
	private float[][][][] toMatrix(BufferedImage[] imgs, String normMethod) {
		float[][][][] data = new float[imgs.length][PIC_SIZE][PIC_SIZE][3];
		for(int i=0; i<imgs.length; i++){
			data[i] = toMatrix(imgs[i], normMethod);
		}
		return data;
	}
	
	/**
	 * <div class="en">Test</div>
	 * <div class="zh">测试</div>
	 * @param args 
	 */
    public static void main(String[] args) {  
    	GraphDriver gd = new GraphDriver(DevConstants.RES_ROOT+"tf-models\\model", 
    			  "input_tensor", 
    					  "softmax_linear/softmax_linear",
    					  DevConstants.RES_ROOT+"bed\\tf-labels-to-text.txt",
    					  32);
    	gd.infer("D:\\PythonWorksp\\TensorFlow\\furniture\\test\\hammock.jpg", "resize");
    	gd.close();
    }

	/**
	 * @return the BATCH_SIZE
	 */
	public int getBatchSize() {
		return BATCH_SIZE;
	}
	/**
	 * @return the PIC_SIZE <span class="en">pic size required by the pre-trained model</span>
	 * <span class="zh">分类器要求的正方形图片边长</span>
	 */
	public int getPicSize() {
		return PIC_SIZE;
	}
}

