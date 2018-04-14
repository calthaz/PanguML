package zym.tensorflow.general;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import zym.tensorflow.tools.LabelGenerator;
import zym.tensorflow.tools.TFUtils;

/**
 *	<span class="en">contains methods shared among all classifiers</span>
 *  <span class="zh">所有分类器的公用方法</span>
 */
public abstract class Classifier {
	
	/**
	 * <span class="en">There can be a random prefix before this filename</span>
	 * <span class="zh">结果文件的基础名称，前面有随机前缀</span>
	 */
	public static final String RESULT_FILE_NAME = "tf-inference-results.txt";	
	/**
	 * <span class="en">Path to the int-to-text label file, {@code LabelGenerator.LABEL_TEXT_FILE_NAME}</span>
	 * <span class="zh">标签数字对应文字的文件, 名称为{@code LabelGenerator.LABEL_TEXT_FILE_NAME} </span>
	 */
	protected String labelPath;
	/**
	 * <span class="en">Path to the graph_def with {@code BATCH_SIZE = 1}.</span>
	 * <span class="zh">{@code BATCH_SIZE = 1} 的 graph_def 路径。</span>
	 */
	protected String modelPath;
	/**
	 * <span class="en">Path to the graph_def with {@code BATCH_SIZE > 1}</span>
	 * <span class="zh">{@code BATCH_SIZE > 1} 的 graph_def 路径。</span>
	 */
	protected String batchModelPath;
	/**
	 * <span class="en">GraphDriver with {@code BATCH_SIZE = 1}.</span>
	 * <span class="zh">{@code BATCH_SIZE = 1} 的GraphDriver。</span>
	 */
	protected GraphDriver gd;
	/**
	 * <span class="en">GraphDriver with {@code BATCH_SIZE > 1}.</span>
	 * <span class="zh">{@code BATCH_SIZE > 1} 的 GraphDriver。</span>
	 */
	protected GraphDriver bgd;
	
	/**
	 * <span class="en">Path to the last result file. Null if no inference has been made.</span>
	 * <span class="zh">到最后结果文件的路径。 如果没有计算，则为空。</span>
	 */
	protected String lastResultPath = null;
	
	private static final String SEP = LabelGenerator.SEP;
	
	/**
	 * 
	 * <span class="en">
	 * Create a Classifier with uninitialized params
	 * </span>
	 * <span class="zh">创建一个参数未初始化的分类器</span>
	 */
	public Classifier(){
		
	}
	
	/**
	 * 
	 * @return <span class="en">pic size required by the pre-trained model</span>
	 * <span class="zh">分类器要求的正方形图片边长</span>
	 */
	public abstract int getImageSize();
	
	/**
	 * 
	 * @return <span class="en">The supported batch size of a graph.pb when using batch processing</span>
	 * <span class="zh">graph批量处理时支持的BatchSize</span>
	 */
	public abstract int getBatchSize();
	/**
	 * 
	 * @return <span class="en">Image preprocessing method. "resize" or "crop"</span>
	 * <span class="zh">图片预处理方法。"resize" 或 "crop"</span>
	 */
	public abstract String getNormMethod();
	/**
	 * 
	 * @return <span class="en">Path to the int-to-text label file, {@code LabelGenerator.LABEL_TEXT_FILE_NAME}</span>
	 * <span class="zh">标签数字对应文字的文件, 名称为{@code LabelGenerator.LABEL_TEXT_FILE_NAME} </span>
	 */
	public String getLabelPath(){
		return this.labelPath;
	}
	/**
	 * 
	 * @return <span class="en">Path to the last result file. Null if no inference has been made.</span>
	 * <span class="zh">到最后结果文件的路径。 如果没有计算，则为空。</span>
	 */
	public String getResultPath(){
		return lastResultPath;
	}
	/**
	 * <span class="en">load images, run graphs, 
	 * print results as {@code System.out} 
	 * and save results to {@code RESULT_FILE_NAME} under rootDir</span>
	 * <span class="zh">加载图片, run graphs, 
	 * 用{@code System.out} 输出结果，
	 * 并把结果保存在rootDir下的{@code RESULT_FILE_NAME}文件里</span>
	 * @param inputPaths paths to all inputs<br>
	 * <span class="en">{@code rootPath} is the first element of this array. if first element of this array is a dir, 
	 * the result file is saved there; if not, 
	 * the result file is saved alongside with this file</span>
	 * <span class="zh">如果该数组的第一个元素{@code rootPath} 是文件夹，
	 * 结果列表储存在该文件夹中，否则储存在第一个文件所在的文件夹中</span>
	 *  
	 */
	public String loadAndRun(String[] inputPaths){
		long time = System.currentTimeMillis();
		if(modelPath==null||batchModelPath==null||labelPath==null){
			System.err.println("Resources failed to load");
			return null;
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
			this.lastResultPath = resultPath;
			return resultPath;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
  	    return null;
	}
	/**
	 * Execute Graph by Batch: 
	 * self-evident<br>
	 * <span class="en"></span><span class="zh">批量处理图片，余数单个处理</span>
	 * @param files list of paths to unchecked files
	 * @param wr <span class="en">writer to write the result file</span><span class="zh">写结果列表的{@code PrintWriter writer}</span>
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

			if(count%BATCH_SIZE==0&&subset[0]!=null){
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
