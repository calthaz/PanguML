package MNIST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import general.DevConstants;
import tools.TFUtils;

/**
 * 
 *  <div class="en">Use TensorFlow Java API to label hand-written digits using a pre-trained model. </div>
 *  <div class="zh">使用TensorFlow Java API和预先训练的模型来分类手写数字图片。</div>
 *
 */
public class MNISTGraph {
	/** <span class="en">pic size required by the pre-trained model</span>
	 * <span class="zh">分类器要求的正方形图片边长</span>*/
	public static final int PIC_SIZE = 28;
	
	static {
		  try {
		    System.load(DevConstants.RES_ROOT+"tensorflow_jni.dll");
		  } catch (UnsatisfiedLinkError e) {
		    System.err.println("Native code library failed to load.\n" + e);
		    System.exit(1);
		  }
	}
	
	  /**
	   * <div class="en">Give classes for paths in args</div>
	   * <div class="zh">给args里的文件做出分类</div>
	   * @param args
	   */
	public static void main(String[] args) {  
        String labels = "0123456789";  
        String[] files = args;
        //String imageFile = "F:\\TensorFlowDev\\PythonWorksp\\objectDetector\\img\\crop26.png";
        
        //TensorFlowInferenceInterface tfi = new TensorFlowInferenceInterface("D:/tf_mode/output_graph.pb","imageType");
        SavedModelBundle smb = SavedModelBundle.load("F:\\TensorFlowDev\\PythonWorksp\\TensorFlow\\MNIST\\model", "serve");//the serve tag 
        String retstr = "";
	    try(Session s = smb.session()) {  	
	        for(String imageFile:files){
	        	//Tensor image2 = constructAndExecuteGraphToNormalizeImage(imageBytes);
        		//FLOAT tensor with shape [1, 28, 28, 1] well...
        		Tensor image = Tensor.create(toMNIST(imageFile));
	        	System.out.println("my image "+image);
	        	float[][][][] arr = new float[1][PIC_SIZE][PIC_SIZE][1];
	        	image.copyTo(arr);
	        	for(int i=0; i<PIC_SIZE*PIC_SIZE;i++){
	        		if (i%PIC_SIZE == 0)System.out.println();
	        		if(arr[0][i/PIC_SIZE][i%PIC_SIZE][0]>0){
	        			System.err.print(String.format("%.1f ", arr[0][i/PIC_SIZE][i%PIC_SIZE][0]));
	        		}else{
	        			System.out.print(String.format("%.1f ", arr[0][i/PIC_SIZE][i%PIC_SIZE][0]));
	        		}         		
	        	}
	        	System.out.println();
	        	System.err.println();
	        	
	        	float[] keepProbArr = new float[1024];
	        	Arrays.fill(keepProbArr, 1f);//<--this usage of f is only in my AP book!
	        	Tensor keepProb = Tensor.create(new long[]{1,1024}, FloatBuffer.wrap(keepProbArr));
	
	        	try (Tensor result = s.runner().feed("input_tensor", image).feed("keep-prop",keepProb).fetch("output_tensor").run().get(0)) {
	        		//I guess these are corresponding variable names
	        		//no, they are not
	        		//they are operation names... as in tf.someOp(arg1, arg2, name="...");
	          
	        		final long[] rshape = result.shape();
	        		System.out.println(result.toString());
	        		if (result.numDimensions() != 2 || rshape[0] != 1) {
	        			throw new RuntimeException(
	        					String.format(
	        							"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
	        							Arrays.toString(rshape)));
	        		}
	        		int nlabels = (int) rshape[1];
	        		float[] probas = result.copyTo(new float[1][nlabels])[0];
	        		//System.out.print(probas.length);
	        		for(int i=0; i<10; i++){
	        			System.out.print(
	            				String.format(
	            						"Probability for number %s: %.2f%%; ",
	            						labels.charAt(i), probas[i]));
	        		}
	        		System.out.println();
	        		int bestLabelIdx = TFUtils.maxIndex(probas);
	        		System.out.println(
	        				String.format(
	        						"BEST MATCH: %s (%.2f%% likely)",
	        						labels.charAt(bestLabelIdx), probas[bestLabelIdx]));//wtf? larger than zero?
	        		retstr+=labels.charAt(bestLabelIdx);
	        	}
	        	image.close();
	        	System.out.println(retstr);
	        }
	    }catch(IOException e){
	        	e.printStackTrace();
	    }
          
	}
      
      /**
       * Let me try to use Java to normalize the image
       * x = tf.placeholder(tf.float32, [None, 784], name="input_tensor") 
       * [784][1,28,28,1], LabelImage
       * @param imageBytes
       * @return 
		output = (255-value)/255<br>
		because in MNIST data set, 1 looks like
<pre>
0   0   0   0   0   0   0 
0   0   0   1   0   0   0
0   0   0.2 1   0   0   0
0   0   0.1 1   0   0   0
0   0   0   1   0.3 0   0
0   0   0.2 1   0   0   0
0   0   0.1 1   0   0   0
0   0   0   0.2 0   0   0
0   0   0   0   0   0   0 
</pre>
       */
      private static float[][][][] toMNIST(String path) throws IOException{
    	  float[][][][] output = new float[1][PIC_SIZE][PIC_SIZE][1];
    	  
		  BufferedImage img = ImageIO.read(new File(path));
		  img = TFUtils.getScaledImage(img, PIC_SIZE, PIC_SIZE);
		  //A new Image object is returned which will render the image at the specified width and height by default. 
		  
		  //ImageIO.write(img, "jpg", new File("img\\myImage.jpg"));
		  int[] raw = new int[PIC_SIZE*PIC_SIZE];
		  //img.getRaster().getPixels(0, 0, PIC_SIZE, PIC_SIZE, raw);java.lang.ArrayIndexOutOfBoundsException: 784
		  raw = img.getRGB(0, 0, PIC_SIZE, PIC_SIZE, raw, 0, PIC_SIZE);
		  /*
		  BufferedImage testimage = new BufferedImage(PIC_SIZE,PIC_SIZE,BufferedImage.TYPE_INT_RGB);			  
		  testimage.setRGB(0, 0, PIC_SIZE, PIC_SIZE, raw, 0, PIC_SIZE);
		  ImageIO.write(testimage, "jpg", new File("img\\TestImage.jpg"));
		  */
		  int r,g,b;
		  for(int i=0; i<PIC_SIZE*PIC_SIZE;i++){
			  r = raw[i]>>16 & 0xff;
		  	  g = raw[i]>>8 & 0xff;
		  	  b = raw[i] & 0xff;
		  	  //if(i%100==0) System.out.println(r+"  "+g+"  "+b+"  "+(1-(r+g+b)/(255f*3)));
		  	  //output[0][i%PIC_SIZE][i/PIC_SIZE][0] = 1-(r+g+b)/(255f*3);MNIST
		  	  //i = y*scansize + x
		  	  output[0][i/PIC_SIZE][i%PIC_SIZE][0] = 1-(r+g+b)/(255f*3);//[y][x]
		  }
		  return output;
		  
      }
      
}
