package zym.tensorflow.classifyfurnishing;

import zym.tensorflow.general.Classifier;
import zym.tensorflow.general.DevConstants;
import zym.tensorflow.tools.NativeUtils;

/**
 * <b class="en">Notice: Don't run it as it is because modelDir and libs are hardcoded here</b>
 * <b class="zh">注意: 不要直接运行因为modelDir和libs是编码在源文件里的</b>
 * <span class="en">
 * A Classifier dealing with six categories of beds: <br>
 * single-bed, hammock, double-bed, baby-bed(cot), bunk-bed, round-bed 
 * </span>
 * <span class="zh">
 * 分类6类床：单人床、吊床、双人床、宝宝床（有栏杆的）、双层床和圆床的分类器
 * </span>
 <pre>
 0|||baby-bed
1|||bunk-bed
2|||double-bed
3|||hammock
4|||round-bed
5|||single-bed
</pre>
 */
public class BedClassifier extends Classifier{
	private static final int IMG_SIZE = 224;//32;
	private static final int BATCH_SIZE = 20;
	private static final String IMG_NORMALIZE_METHOD = "resize";
	
	/**
	 * <span class="en">
	 * classify beds: <br>
	 * single-bed, hammock, double-bed, baby-bed(cot), bunk-bed, round-bed 
	 * </span>
	 * <span class="zh">
	 * 分类6类床：单人床、吊床、双人床、宝宝床（有栏杆的）、双层床和圆床
	 * </span>
	 * @param inputPaths  paths to all inputs. 
	 * <span class="en">if first element of this array is a dir, 
	 * the result file is saved there; if not, 
	 * the result file is saved alongside with this file</span>
	 * <span class="zh">如果该数组的第一个元素是文件夹，
	 * 结果列表储存在该文件夹中，否则储存在第一个文件所在的文件夹中</span>
	 */
	public BedClassifier(String[] inputPaths){
		String modelDir = "F:/TensorFlowDev/PythonWorksp/TensorFlow/FurnitureClassifier/models/";
		//modelPath = NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-no-text-1/frozen_graph.pb",
		modelPath = NativeUtils.loadOrExtract(modelDir+"model-bed-224-1/frozen_graph.pb",
				"/tf-models/model-no-text-1/frozen_graph.pb");
		//DevConstants.RES_ROOT+"tf-models/model";
		//batchModelPath =  NativeUtils.loadOrExtract(DevConstants.MOD_ROOT+"model-no-text-"+BATCH_SIZE+"/frozen_graph.pb",
		batchModelPath =  NativeUtils.loadOrExtract(modelDir+"model-bed-224-"+BATCH_SIZE+"/frozen_graph.pb",
				"/tf-models/model-no-text-"+BATCH_SIZE+"/frozen_graph.pb");
		//DevConstants.RES_ROOT+"tf-models/model"+BATCH_SIZE;
		labelPath = NativeUtils.loadOrExtract(DevConstants.RES_ROOT+"bed/tf-labels-to-text.txt","/labels/bed/tf-labels-to-text.txt");
		
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
