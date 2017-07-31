package classifyFurnishing;

import general.Classifier;
import general.DevConstants;
import tools.NativeUtils;

public class StyleClassifier extends Classifier{
	private static final int IMG_SIZE = 128;
	private static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	
	public StyleClassifier(String[] inputPaths) {
		
		modelPath =  NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"tf-models/style128-1"+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-1/frozen_graph.pb");;
		batchModelPath =  NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"tf-models/style128-"+BATCH_SIZE+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		labelPath = NativeUtils.loadOrExtract("F:/tmp/styles/tf-labels-to-text.txt",
				"/labels/styles/tf-labels-to-text.txt");
		
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
		new StyleClassifier(inputPaths);
  	    
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
