package zym.tensorflow.classifyfurnishing;

import zym.tensorflow.general.Classifier;
import zym.tensorflow.general.DevConstants;
import zym.tensorflow.tools.NativeUtils;

/**
 * <b class="en">Notice: Don't run it as it is because modelDir and libs are hardcoded here</b>
 * <b class="zh">注意: 不要直接运行因为modelDir和libs是编码在源文件里的</b>
 * <span class="en">
 * classifies only three categories, wall-paper, wooden-floor, floor-tile
 * </span>
 * <span class="zh">
 * 分类3类贴图：墙纸、墙砖、地板
 * </span>
 <pre>
 0|||floor-tile
1|||wall-paper
2|||wooden-floor
 </pre>
 */
public class FurnishingClassifier extends Classifier{
	private static final int IMG_SIZE = 128;
	private static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	/**
	 * <span class="en">
	 * classifies only three categories, wall-paper, wooden-floor, floor-tile <br>
	 *  In python the corresponding classifier is HardwareClassifier
	 * </span>
	 * <span class="zh">
	 * 分类3类贴图：墙纸、墙砖、地板， 对应python为HardwareClassifier
	 * </span>
	 * @param inputPaths  paths to all inputs. 
	 * <span class="en">if first element of this array is a dir, 
	 * the result file is saved there; if not, 
	 * the result file is saved alongside with this file</span>
	 * <span class="zh">如果该数组的第一个元素是文件夹，
	 * 结果列表储存在该文件夹中，否则储存在第一个文件所在的文件夹中</span>
	 */
	public FurnishingClassifier(String[] inputPaths) {
		
		modelPath =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-fur-no-text-1"+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-1/frozen_graph.pb");;
		batchModelPath =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb", 
				"/tf-models/model-fur-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		labelPath = NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"furpics/tf-labels-to-text.txt",
				"/labels/furpics/tf-labels-to-text.txt");
		
  	    loadAndRun(inputPaths); 
    
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
		new FurnishingClassifier(inputPaths);
  	    
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
