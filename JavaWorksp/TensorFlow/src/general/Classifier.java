package general;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import tools.LabelGenerator;
import tools.TFUtils;

/**
 * 
 * contains methods shared among all classifiers
 *
 */
public abstract class Classifier {

	public static final String RESULT_FILE_NAME = "tf-inference-results.txt";	
	protected String labelPath;
	protected String modelPath;
	protected String batchModelPath;
	protected GraphDriver gd;
	protected GraphDriver bgd;
	private static final String SEP = LabelGenerator.SEP;
	
	
	public Classifier(){
		
	}
	
	public abstract int getImageSize();
	public abstract int getBatchSize();
	public abstract String getNormMethod();
	
	/**
	 * load images, run graphs, print results as {@code System.out} and save results to {@code RESULT_FILE_NAME} under rootDir
	 * 
	 * @param inputPaths paths to all inputs<br>
	 *  {@code rootPath} is the first element of this array. if it is a dir, the result file is saved there;
	 *  if not, the result file is saved alongside with this file<br>
	 *  
	 */
	protected void loadAndRun(String[] inputPaths){
		long time = System.currentTimeMillis();
		if(modelPath==null||batchModelPath==null||labelPath==null){
			System.err.println("Resources failed to load");
			return;
		}
		gd = new GraphDriver(modelPath, 
  			  "input_tensor", "softmax_linear/softmax_linear",
  			  		labelPath,
  			  	getImageSize());
		bgd = new GraphDriver(batchModelPath, 
	  			  "input_tensor", "softmax_linear/softmax_linear",
	  			  			labelPath,
	  			  		getImageSize(), getBatchSize());
		String rootPath = inputPaths[0];
  	    ArrayList<String> files = new ArrayList<String>();
  	    File root = new File(rootPath);
  	    for(int i = 0; i< inputPaths.length; i++){
	    	TFUtils.readFilesRecursively(new File(inputPaths[i]), files);
	    }
  	    long loadTime = (System.currentTimeMillis()-time);
  	    System.out.println("Load files in "+ loadTime +"ms");
	    System.out.println("file count: "+files.size());
	    System.out.println("batch size: "+getBatchSize());
	    
	    String resultPath = "";
  	    String prefix = "furn-"+(int)(Math.random()*100000);
  	    if(root.isDirectory()){
  	    	resultPath = rootPath+SEP+prefix+RESULT_FILE_NAME;
  	    }else{
  	    	resultPath = root.getParent()+SEP+prefix+RESULT_FILE_NAME;
  	    }

  	    try {
  	    	PrintWriter wr = null;			
			wr = new PrintWriter(resultPath,"UTF-8");			
			time = System.currentTimeMillis();
			executeGraphByBatch(files, wr);
			long runTime = (System.currentTimeMillis()-time);
			System.out.println("Finishing inferring in "+runTime+"ms");
			System.out.println(String.format("Loading %d files in %d ms. Inference has taken %d ms.", files.size(), loadTime, runTime));

			System.out.println(resultPath);

			wr.flush();
			wr.close();
			
			gd.close();
			bgd.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * Execute Graph by Batch: 
	 * self-evident
	 * @param files list of paths to unchecked files
	 * @param BATCH_SIZE
	 * @param wr writer to write the result file
	 * @param gd Graph with batch size = 1
	 * @param gd1 Graph with batch size = BATCH_SIZE
	 */
	private void executeGraphByBatch(ArrayList<String> files, PrintWriter wr) {
		int BATCH_SIZE = getBatchSize();
		BufferedImage[] subset = null;
		int[] subsetIndex = null;
		int count=0;
		for(int i=0; i<files.size(); i++){
			if(count%BATCH_SIZE==0){
				subset=new BufferedImage[BATCH_SIZE];
				subsetIndex = new int[BATCH_SIZE];
				System.out.println("batch start: "+i);
			}
			File f = new File(files.get(i));
			try{
				//reads every image in case it were a bad one 
				BufferedImage img = ImageIO.read(f);
				if(img!= null&&img.getHeight()!=0){
					subset[count%BATCH_SIZE]=img;
					subsetIndex[count%BATCH_SIZE]=i;
					count++;
				}else{
					System.out.println("Probably not an image: "+f.getPath());
					continue;
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+f.getPath());
				continue;
			}					

			if(count%BATCH_SIZE==0){
				//end of a batch
				String[] results = bgd.inferAndGetScore(subset, getNormMethod());
				for(int j=0; j<BATCH_SIZE; j++){
					//wr.println(subset[j]+LABEL_SEP+results[j]);
					wr.println(files.get(subsetIndex[j])+LabelGenerator.LABEL_SEP+results[j]);
				}
				System.out.println("batch ends: "+i);
				subset = null;
				subsetIndex = null;
			}
  	    }
		if(subset!=null){
			for(int j=0; j<BATCH_SIZE; j++){
				if(subset[j]!=null){
					String result = gd.inferAndGetScore(subset[j], getNormMethod());
					wr.println(files.get(subsetIndex[j])+LabelGenerator.LABEL_SEP+result);
				}
			}
		}

	}
}
