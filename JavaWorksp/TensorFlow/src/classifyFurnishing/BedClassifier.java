package classifyFurnishing;

import general.Classifier;
import general.DevConstants;
import tools.NativeUtils;

/**
 * deal with six categories of beds: <br>
 * single-bed, hammock, double-bed, baby-bed(cot), bunk-bed, round-bed
 *
 */
public class BedClassifier extends Classifier{
	public static final int IMG_SIZE = 32;
	public static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	
	public BedClassifier(String[] inputPaths){
		modelPath = NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-no-text-1/frozen_graph.pb",
				"/tf-models/model-no-text-1/frozen_graph.pb");
		//DevConstants.RES_ROOT+"tf-models/model";
		batchModelPath =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-no-text-"+BATCH_SIZE+"/frozen_graph.pb",
				"/tf-models/model-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		//DevConstants.RES_ROOT+"tf-models/model"+BATCH_SIZE;
		labelPath = NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"bed/tf-labels-to-text.txt","/labels/bed/tf-labels-to-text.txt");
		
		loadAndRun(inputPaths);
	}
	
	public static void main(String[] args){
		String rootPath = "";
		String[] inputPaths = new String[1];
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
			inputPaths[0] = System.getProperty("user.dir");
		}else {		
			rootPath = args[0];
			inputPaths = args;
		}
		System.out.println(rootPath);
		
		new BedClassifier(inputPaths);
  	    
	}
	
	@Override
	public int getImageSize() {
		return IMG_SIZE;
	}

	@Override
	public int getBatchSize() {
		return BATCH_SIZE;
	}
	
	@Override
	public String getNormMethod() {
		
		return IMG_NORMALIZE_METHOD;
	}
}
