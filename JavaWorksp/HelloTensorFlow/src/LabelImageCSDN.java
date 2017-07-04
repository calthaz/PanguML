
/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;


/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
public class LabelImageCSDN {
	public static final int PIC_SIZE=24;
  static {
	  try {
	    System.load("D:\\javaworksp\\tensorflow_jni.dll");
	  } catch (UnsatisfiedLinkError e) {
	    System.err.println("Native code library failed to load.\n" + e);
	    System.exit(1);
	  }
  }
  
  private static void printUsage(PrintStream s) {
    final String url =
        "https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip";
    s.println(
        "Java program that uses a pre-trained Inception model (http://arxiv.org/abs/1512.00567)");
    s.println("to label JPEG images.");
    s.println("TensorFlow version: " + TensorFlow.version());
    s.println();
    s.println("Usage: label_image <model dir> <image file>");
    s.println();
    s.println("Where:");
    s.println("<model dir> is a directory containing the unzipped contents of the inception model");
    s.println("            (from " + url + ")");
    s.println("<image file> is the path to a JPEG image file");
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      printUsage(System.err);
      System.exit(1);
    }
    String modelDir = args[0];
    String imageFile = args[1];

    //byte[] graphDef = readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"));
    SavedModelBundle smb = SavedModelBundle.load("D:\\PythonWorksp\\TensorFlow\\CTFAR-10\\model", "serve");
    List<String> labels =
        readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"));
    byte[] imageBytes = readAllBytesOrExit(Paths.get(imageFile));

    try (Tensor image = Tensor.create(toCIFAR(imageFile))) {
      float[] labelProbabilities = executeInceptionGraph(smb, image);
      int bestLabelIdx = maxIndex(labelProbabilities);
      System.out.println(
          String.format(
              "BEST MATCH: %s (%.2f%% likely)",
              labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx]));
      for(int i=0; i<10; i++){
			System.out.print(
  				String.format(
  						"Probability for:: %s: %.2f%%; ",
  						labels.get(i), labelProbabilities[i]));
      }
    }
  }

  private static float[] executeInceptionGraph( SavedModelBundle smb, Tensor image) {
    //try (Graph g = new Graph()) {
      //g.importGraphDef(graphDef);
	  System.out.println(image);
      try (Session s = smb.session(); 		  
          Tensor result = s.runner().feed("image-holder", image).fetch("fc_output/output-tensor").run().get(0)) {
    	  System.out.println(result);
        final long[] rshape = result.shape();
        //if (result.numDimensions() != 2 || rshape[0] != 1) {
          //throw new RuntimeException(
             // String.format(
                 // "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                 // Arrays.toString(rshape)));
       // }
        int nlabels = (int) rshape[1];
        return result.copyTo(new float[128][nlabels])[0];
      }
    //}
  }

  private static int maxIndex(float[] probabilities) {
    int best = 0;
    for (int i = 1; i < probabilities.length; ++i) {
      if (probabilities[i] > probabilities[best]) {
        best = i;
      }
    }
    return best;
  }

  private static byte[] readAllBytesOrExit(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      System.err.println("Failed to read [" + path + "]: " + e.getMessage());
      System.exit(1);
    }
    return null;
  }

  private static List<String> readAllLinesOrExit(Path path) {
    try {
      return Files.readAllLines(path, Charset.forName("UTF-8"));
    } catch (IOException e) {
      System.err.println("Failed to read [" + path + "]: " + e.getMessage());
      System.exit(0);
    }
    return null;
  }

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
  private static float[][][][] toCIFAR(String path){
	  float[][][][] output = new float[128][PIC_SIZE][PIC_SIZE][3];
	  float [][][] image = new float[PIC_SIZE][PIC_SIZE][3];
	  try {
		  BufferedImage img = ImageIO.read(new File(path));
		  img = getScaledImage(img, PIC_SIZE, PIC_SIZE);
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
		  	  //output[0][i%PIC_SIZE][i/PIC_SIZE][0] = 1-(r+g+b)/(255f*3);MNIST不是这个, 不过统一应该就好啦
		  	  //i = y*scansize + x
		  	  image[i/PIC_SIZE][i%PIC_SIZE][0] = r;//注意 似乎是这种朝向[y][x]
		  	image[i/PIC_SIZE][i%PIC_SIZE][1] = g;
		  	image[i/PIC_SIZE][i%PIC_SIZE][2] = b;
		  }
		  Arrays.fill(output, image);
		  return output;
	  } catch (IOException e) {
		  System.out.println("can't open image "+path);
		  Arrays.fill(output, 1f);
		  return output;
	  }
  }
}