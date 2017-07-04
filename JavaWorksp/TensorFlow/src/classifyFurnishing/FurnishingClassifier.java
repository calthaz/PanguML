package classifyFurnishing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import general.GraphDriver;
import tools.LabelGenerator;

public class FurnishingClassifier {
	public static final String RESULT_FILE_NAME = "tf-inference-results.txt";
	public static final int IMG_SIZE = 32;
	public static final int BATCH_SIZE = 50;
	private static final String SEP = LabelGenerator.SEP;
	private static final String LABEL_SEP = LabelGenerator.LABEL_SEP;
	
	public static void main(String[] args){
		String rootPath = "";
		String mode = "file";
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
		}else if(args.length==1){
			rootPath = args[0];
		}else{
			mode = args[0];//JSON or file(default)
			rootPath = args[1];
		}
		System.out.println(rootPath);
		GraphDriver gd = new GraphDriver("D:\\TensorFlowDev\\PythonWorksp\\TensorFlow\\FurnitureClassifier\\model", 
  			  "input_tensor", "softmax_linear/softmax_linear",
  					  "D:\\TensorFlowDev\\PythonWorksp\\TensorFlow\\furniture\\bed\\tf-labels-to-text.txt",
  					  IMG_SIZE);
		GraphDriver gd1 = new GraphDriver("D:\\TensorFlowDev\\PythonWorksp\\TensorFlow\\FurnitureClassifier\\model"+BATCH_SIZE, 
	  			  "input_tensor", "softmax_linear/softmax_linear",
	  					  "D:\\TensorFlowDev\\PythonWorksp\\TensorFlow\\furniture\\bed\\tf-labels-to-text.txt",
	  					  IMG_SIZE, BATCH_SIZE);
  	    ArrayList<String> files = new ArrayList<String>();
  	    long time = System.currentTimeMillis();
  	    readImageFilesRecursively(new File(rootPath), files);
  	    System.out.println("Checked all files in "+ (System.currentTimeMillis()-time)+"ms");
  	    
  	    System.out.println(files.size());
  	    int nRounds = files.size()/BATCH_SIZE;
  	    System.out.println("Number of rounds: "+nRounds);
  	    System.out.println("batch size: "+BATCH_SIZE);
  	   
  	    //int counter=0;
  	    try {
  	    	PrintWriter wr = null;
			if(!"JSON".equals(mode)) {
				wr = new PrintWriter(new FileWriter(rootPath+SEP+RESULT_FILE_NAME,false));
			}
			
			String[]subset = new String[1];
			time = System.currentTimeMillis();
			for(int i=0; i<files.size(); i++){
				if(i%BATCH_SIZE==0&&i<BATCH_SIZE*nRounds){
					subset=new String[BATCH_SIZE];
					System.out.println("batch start: "+i);
				}
				if(i<BATCH_SIZE*nRounds){
					//add to batch
					subset[i%BATCH_SIZE]=files.get(i);
				}else{
					//infer once
					String result = gd.infer(files.get(i));
					wr.println(files.get(i)+LABEL_SEP+result);
				}
				if(i%BATCH_SIZE==BATCH_SIZE-1){
					//end of a batch
					String[] results = gd1.infer(subset);
					for(int j=0; j<BATCH_SIZE; j++){
						wr.println(subset[j]+LABEL_SEP+results[j]);
					}
					System.out.println("batch ends: "+i);
				}
	  	    }
			System.out.println("Finishing inferring in "+(System.currentTimeMillis()-time)+"ms");
			if(!"JSON".equals(mode)) {
				wr.flush();
				wr.close();
			}
			gd.close();
			gd1.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
  	    
	}

	private static void readImageFilesRecursively(File f, ArrayList<String> files) {
		if(f.isDirectory()){
			for(File entry : f.listFiles()){
				readImageFilesRecursively(entry, files);
			}	
		}else{
			try{
				//reads every image in case it were a bad one 
				BufferedImage img = ImageIO.read(f);
				if(img!= null&&img.getHeight()!=0){
					files.add(f.getAbsolutePath());
				}else{
					System.out.println("Probably not an image: "+f.getPath());
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+f.getPath());
			}
			
		}
	}
}
//when batch size = 20
//1200~16000
//5600~59000
//when batch size = 50
//1200~16000
//5600~58000
//好像实在也没多大区别