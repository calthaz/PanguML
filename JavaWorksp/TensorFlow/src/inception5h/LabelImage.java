package inception5h;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import classifyFurnishing.FurnishingClassifier;
import general.DevConstants;
import tools.LabelGenerator;

/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
public class LabelImage {
  public static final String LABEL_SEP = LabelGenerator.LABEL_SEP;
  static {
	  try {
	    System.load(DevConstants.RES_ROOT+"jni/libtensorflow_jni.so");
	    //java.lang.UnsatisfiedLinkError: Expecting an absolute path of the library: tensorflow_jni.dll ) 
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
    if (args.length != 1) {
      printUsage(System.err);
      System.exit(1);
    }
    
    String modelDir = DevConstants.RES_ROOT+"tf-models/inception5h"; //"D:\\TensorFlowDev\\JavaWorksp\\TensorFlow\\inception5h";
    String imageFile = args[0];
    
    ArrayList<String> files = new ArrayList<String>();
    long time = System.currentTimeMillis();
    File root = new File(imageFile);
    FurnishingClassifier.readImageFilesRecursively(root, files);
    long loadTime = (System.currentTimeMillis()-time);
    System.out.println("Checked all files in "+ loadTime +"ms");
	    
    String resultPath = "";
    if(root.isDirectory()){
    	resultPath = imageFile+"/"+FurnishingClassifier.RESULT_FILE_NAME;
    }else{
    	resultPath = root.getParent()+"/"+FurnishingClassifier.RESULT_FILE_NAME;
    }
    
    byte[] graphDef = readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"));
    List<String> labels =
        readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"));
    byte[] imageBytes = null;
               
    PrintWriter wr;
	try {
		wr = new PrintWriter(resultPath,"UTF-8");	
		time = System.currentTimeMillis();
	    for(int i=0; i<files.size(); i++){
	    	imageBytes = readAllBytesOrExit(Paths.get(files.get(i)));
		    
		    try (Tensor image = constructAndExecuteGraphToNormalizeImage(imageBytes)) {
		      
		      float[] labelProbabilities = executeInceptionGraph(graphDef, image);
		      int bestLabelIdx = maxIndex(labelProbabilities);
		      System.out.println(
		          String.format(
		              "BEST MATCH: %s (%.2f%% likely)",
		              labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx] * 100f));
		      wr.println(files.get(i)+LABEL_SEP+labels.get(bestLabelIdx)+LABEL_SEP+String.format("%.2f", labelProbabilities[bestLabelIdx]));
		      
		    } 
	    }
	    long runTime = (System.currentTimeMillis()-time);
		System.out.println("Finishing inferring in "+runTime+"ms");
		System.out.println(String.format("Loading %d files in %d ms. Inference has taken %d ms.", files.size(), loadTime, runTime));
		System.out.println(resultPath);
	    wr.flush();
		wr.close();
    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  private static Tensor constructAndExecuteGraphToNormalizeImage(byte[] imageBytes) {
    try (Graph g = new Graph()) {
      GraphBuilder b = new GraphBuilder(g);
      // Some constants specific to the pre-trained model at:
      // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
      //
      // - The model was trained with images scaled to 224x224 pixels.
      // - The colors, represented as R, G, B in 1-byte each were converted to
      //   float using (value - Mean)/Scale.
      final int H = 224;
      final int W = 224;
      final float mean = 117f;
      final float scale = 1f;

      // Since the graph is being constructed once per execution here, we can use a constant for the
      // input image. If the graph were to be re-used for multiple input images, a placeholder would
      // have been more appropriate.
      final Output input = b.constant("input", imageBytes);
      final Output output =
          b.div(
              b.sub(
                  b.resizeBilinear(
                      b.expandDims(
                          b.cast(b.decodeJpeg(input, 3), DataType.FLOAT),
                          b.constant("make_batch", 0)),
                      b.constant("size", new int[] {H, W})),
                  b.constant("mean", mean)),
              b.constant("scale", scale));
      try (Session s = new Session(g)) {
        return s.runner().fetch(output.op().name()).run().get(0);
      }
    }
  }

  private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
    try (Graph g = new Graph()) {
      g.importGraphDef(graphDef);
      try (Session s = new Session(g); 		  
          Tensor result = s.runner().feed("input", image).fetch("output").run().get(0)) {
    	  //System.out.println(image);//FLOAT tensor with shape [1, 224, 224, 3]
        final long[] rshape = result.shape();
        if (result.numDimensions() != 2 || rshape[0] != 1) {
          throw new RuntimeException(
              String.format(
                  "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                  Arrays.toString(rshape)));
        }
        int nlabels = (int) rshape[1];
        return result.copyTo(new float[1][nlabels])[0];
      }
    }
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

  // In the fullness of time, equivalents of the methods of this class should be auto-generated from
  // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
  // like Python, C++ and Go.
  static class GraphBuilder {
    GraphBuilder(Graph g) {
      this.g = g;
    }

    Output div(Output x, Output y) {
      return binaryOp("Div", x, y);
    }

    Output sub(Output x, Output y) {
      return binaryOp("Sub", x, y);
    }

    Output resizeBilinear(Output images, Output size) {
      return binaryOp("ResizeBilinear", images, size);
    }

    Output expandDims(Output input, Output dim) {
      return binaryOp("ExpandDims", input, dim);
    }

    Output cast(Output value, DataType dtype) {
      return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
    }

    Output decodeJpeg(Output contents, long channels) {
      return g.opBuilder("DecodeJpeg", "DecodeJpeg")
          .addInput(contents)
          .setAttr("channels", channels)
          .build()
          .output(0);
    }

    Output constant(String name, Object value) {
      try (Tensor t = Tensor.create(value)) {
        return g.opBuilder("Const", name)
            .setAttr("dtype", t.dataType())
            .setAttr("value", t)
            .build()
            .output(0);
      }
    }

    private Output binaryOp(String type, Output in1, Output in2) {
      return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
    }

    private Graph g;
  }
}