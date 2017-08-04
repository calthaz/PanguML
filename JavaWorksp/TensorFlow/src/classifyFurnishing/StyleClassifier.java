package classifyFurnishing;

import general.Classifier;
import tools.NativeUtils;

public class StyleClassifier extends Classifier{
	private static final int IMG_SIZE = 128;//128;
	private static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	
	public StyleClassifier(String[] inputPaths) {
		String modelDir = "F:/TensorFlowDev/PythonWorksp/TensorFlow/StyleClassifier/models/";
		modelPath =  NativeUtils.loadOrExtract(modelDir+"style128-4-style-1"+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-1/frozen_graph.pb");;
		batchModelPath =  NativeUtils.loadOrExtract(modelDir+"style128-4-style-"+BATCH_SIZE+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		labelPath = NativeUtils.loadOrExtract("F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt",
				"/labels/styles/tf-labels-to-text.txt");
		
  	    loadAndRun(inputPaths); 
  	    
  	    //F:\TensorFlowDev\JavaWorksp\TensorFlow\img\style-test
  	    //F:\TensorFlowDev\training-materials\styles\style-only
    
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
