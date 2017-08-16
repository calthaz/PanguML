package classifyFurnishing;

import general.Classifier;
import tools.NativeUtils;

/**
 * <b class="en">Notice: Don't run it as it is because modelDir and libs are hardcoded here</b>
 * <b class="zh">注意: 不要直接运行因为modelDir和libs是编码在源文件里的</b>
 * <div class="en">
 * classifies 4 categories of furnishing styles: <br>
 * western-luxurious, western-simple, Chinese, Japanese
 * </div>
 * <div class="zh">
 * 分类4类装修风格：西式奢华、西式简约、中式和日式
 * </div>
 * <pre>
0|||western-sim
1|||western-lux
2|||japanese
3|||chinese
</pre>
 */
public class StyleClassifier extends Classifier{
	private static final int IMG_SIZE = 224;//128;
	private static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	/**
	 * <div class="en">
	 * classifies 4 categories of furnishing styles: <br>
	 * western-luxurious, western-simple, Chinese, Japanese
	 * </div>
	 * <div class="zh">
	 * 分类4类装修风格：西式奢华、西式简约、中式和日式
	 * </div>
	 * @param inputPaths  paths to all inputs. 
	 * <span class="en">if first element of this array is a dir, 
	 * the result file is saved there; if not, 
	 * the result file is saved alongside with this file</span>
	 * <span class="zh">如果该数组的第一个元素是文件夹，
	 * 结果列表储存在该文件夹中，否则储存在第一个文件所在的文件夹中</span>
	 */
	public StyleClassifier(String[] inputPaths) {
		String modelDir = "F:/TensorFlowDev/PythonWorksp/TensorFlow/StyleClassifier/models/";
		modelPath =  NativeUtils.loadOrExtract(modelDir+"style224-4-style-7245-1"+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-1/frozen_graph.pb");;
		batchModelPath =  NativeUtils.loadOrExtract(modelDir+"style224-4-style-7245-"+BATCH_SIZE+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		labelPath = NativeUtils.loadOrExtract("F:/TensorFlowDev/training-materials/styles/style-only/eval/tf-labels-to-text.txt",
				"/labels/styles/tf-labels-to-text.txt");
		
  	    loadAndRun(inputPaths); 
  	    
  	    //F:\TensorFlowDev\JavaWorksp\TensorFlow\img\style-test
  	    //F:\TensorFlowDev\training-materials\styles\style-only
    
	}
	
	/**
	 * construct this classifier
	 * @param args input paths for constructor
	 */
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
