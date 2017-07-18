package classifyFurnishing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import general.Classifier;
import general.DevConstants;
import general.GraphDriver;
import tools.LabelGenerator;
import tools.NativeUtils;
import tools.TFUtils;

public class FurnishingClassifier extends Classifier{
	
	public static final int IMG_SIZE = 128;
	public static final int BATCH_SIZE = 20;
	private static final String SEP = LabelGenerator.SEP;
	
	public static void main(String[] args){
		String rootPath = "";
		String[] inputPaths = new String[1];
		String mode = "file";
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
			inputPaths[0] = System.getProperty("user.dir");
		}else {		
			rootPath = args[0];
			inputPaths = args;
		}
		System.out.println(rootPath);
		long time = System.currentTimeMillis();
		String modelPath =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-fur-no-text-1"+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-1/frozen_graph.pb");;
		String modelPath2 =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		String labelPath = NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"furpics/tf-labels-to-text.txt",
				"/labels/furpics/tf-labels-to-text.txt");
		if(modelPath==null||modelPath2==null||labelPath==null){
			System.err.println("Resources failed to load");
			return;
		}
		GraphDriver gd = new GraphDriver(modelPath, 
  			  "input_tensor", "softmax_linear/softmax_linear",
  			  		labelPath,
  					  IMG_SIZE);
		GraphDriver gd1 = new GraphDriver(modelPath2, 
	  			  "input_tensor", "softmax_linear/softmax_linear",
	  			  			labelPath,
	  					  IMG_SIZE, BATCH_SIZE);
  	    ArrayList<String> files = new ArrayList<String>();
  	    
  	    File root = new File(rootPath);
  	    TFUtils.readFilesRecursively(root, files);
  	    for(int i = 1; i< inputPaths.length; i++){
	    	TFUtils.readFilesRecursively(new File(inputPaths[i]), files);
	    }
  	    long loadTime = (System.currentTimeMillis()-time);
  	    System.out.println("Load files in "+ loadTime +"ms");
  	    
  	    System.out.println(files.size());
  	    System.out.println("batch size: "+BATCH_SIZE);
  	   
  	    String resultPath = "";
  	    String prefix = "furn-"+(int)(Math.random()*100000);
  	    if(root.isDirectory()){
  	    	resultPath = rootPath+SEP+prefix+RESULT_FILE_NAME;
  	    }else{
  	    	resultPath = root.getParent()+SEP+prefix+RESULT_FILE_NAME;
  	    }
  	    //int counter=0;
  	    try {
  	    	PrintWriter wr = null;			
			wr = new PrintWriter(resultPath,"UTF-8");			
			time = System.currentTimeMillis();
			executeGraphByBatch(files, BATCH_SIZE, wr, gd, gd1);
			long runTime = (System.currentTimeMillis()-time);
			System.out.println("Finishing inferring in "+runTime+"ms");
			System.out.println(String.format("Loading %d files in %d ms. Inference has taken %d ms.", files.size(), loadTime, runTime));

			System.out.println(resultPath);

			wr.flush();
			wr.close();
			
			gd.close();
			gd1.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
  	    
	}
	
}
