package general;

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

public class GraphDriver implements Closeable{
	
	private final int PIC_SIZE;
	private final int BATCH_SIZE;
	private String inputName;
	private String outputName;
	private ArrayList<String> labels;
	private Session sess;
	private Graph g;
	
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
	 * 
	 * @param modelPath
	 * @param inputName op_name in graph
	 * @param outputName op_name in graph
	 * @param labelPath
	 * @param picSize
	 * @param batchSize
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
	 * GraphDriver with batch_size = 1
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
	 * 
	 * @param imagePath
	 * @return text label
	 */
	public String infer(String imagePath){
		 String r = inferAndGetScore(imagePath);
		 return r.substring(0,r.indexOf(LabelGenerator.LABEL_SEP));
	}
	
	public String infer(BufferedImage img){
		 String r = inferAndGetScore(img);
		 return r.substring(0,r.indexOf(LabelGenerator.LABEL_SEP));
	}
	
	public String inferAndGetScore(String imagePath){
		 try (Tensor image = Tensor.create(toMatrix(imagePath))) {
			
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
	
	public String inferAndGetScore(BufferedImage img){
		 try (Tensor image = Tensor.create(toMatrix(img))) {
			
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
	 * 
	 * @param paths to images
	 * @return a list of text labels inferred 
	 */
	public String[] infer(String[] paths){
		String[] r = inferAndGetScore(paths);
		for(String s : r){
			s = s.substring(0, s.indexOf(LabelGenerator.LABEL_SEP));
		}
		return r;
	}
	public String[] infer(BufferedImage[] imgs){
		String[] r = inferAndGetScore(imgs);
		for(String s : r){
			s = s.substring(0, s.indexOf(LabelGenerator.LABEL_SEP));
		}
		return r;
	}
	
	public String[] inferAndGetScore(String[] paths){
		 try (Tensor image = Tensor.create(toMatrix(paths))) {
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
		    						"No.%d BEST MATCH: %s (%.2f%% likely)",
		    						i, labels.get(bestLabelIdx), probas[i][bestLabelIdx]));
		    		textResults[i] = labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[i][bestLabelIdx]);
	    		}
	    		return textResults;
	    	}
		 }
	}
	public String[] inferAndGetScore(BufferedImage[] imgs){
		 try (Tensor image = Tensor.create(toMatrix(imgs))) {
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
		    						"No.%d BEST MATCH: %s (%.2f%% likely)",
		    						i, labels.get(bestLabelIdx), probas[i][bestLabelIdx]));
		    		textResults[i] = labels.get(bestLabelIdx)+LabelGenerator.LABEL_SEP+String.format("%.2f", probas[i][bestLabelIdx]);
	    		}
	    		return textResults;
	    	}
		 }
	}
	/**
	 * Releases resources, 
	 * graph and session
	 */
	public void close(){
		sess.close();
		g.close();
		//Blocks until there are no active Session instances referring to this Graph. 
		//A Graph is not usable after close returns.
		//so call sess.close() first
		
	}
	
  /**
   * reads an image from file and returns a float array with color components
   * 
   * @param imageBytes
   * @return images = [height, width, channels]
   * use tf to do whatever extra processing
   */
	private float[][][] toMatrix(BufferedImage img){
		  float[][][] imgData = new float[PIC_SIZE][PIC_SIZE][3];
		  img = TFUtils.getScaledImage(img, PIC_SIZE, PIC_SIZE);
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
	
	private float[][][] toMatrix(String path){
		  
		  try {
			  BufferedImage img = ImageIO.read(new File(path));
			  return toMatrix(img);
		  } catch (IOException e) {
			  System.out.println("can't open image "+path);
			  return null;
		  }
	}
      
	private float[][][][] toMatrix(String[] paths) {
		float[][][][] data = new float[paths.length][PIC_SIZE][PIC_SIZE][3];
		for(int i=0; i<paths.length; i++){
			data[i] = toMatrix(paths[i]);
		}
		return data;
	}
	
	private float[][][][] toMatrix(BufferedImage[] imgs) {
		float[][][][] data = new float[imgs.length][PIC_SIZE][PIC_SIZE][3];
		for(int i=0; i<imgs.length; i++){
			data[i] = toMatrix(imgs[i]);
		}
		return data;
	}
	
    public static void main(String[] args) {  
    	GraphDriver gd = new GraphDriver(DevConstants.RES_ROOT+"tf-models\\model", 
    			  "input_tensor", 
    					  "softmax_linear/softmax_linear",
    					  DevConstants.RES_ROOT+"bed\\tf-labels-to-text.txt",
    					  32);
    	gd.infer("D:\\PythonWorksp\\TensorFlow\\furniture\\test\\hammock.jpg");
    	gd.close();
    }
}

