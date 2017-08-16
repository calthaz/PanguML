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
 *	<span class="en">contains methods shared among all classifiers</span>
 *  <span class="zh">所有分类器的公用方法</span>
 */
public abstract class Classifier {
	
	/**
	 * <span class="en">There can be a random prefix before this filename</span>
	 * <span class="zh">结果文件的基础名称，前面有随机前缀</span>
	 */
	public static final String RESULT_FILE_NAME = "tf-inference-results.txt";	
	protected String labelPath;
	protected String modelPath;
	protected String batchModelPath;
	protected GraphDriver gd;
	protected GraphDriver bgd;
	private static final String SEP = LabelGenerator.SEP;
	
	/**
	 * 
	 * <div class="en">
	 * Create a Classifier with uninitialized params
	 * </div>
	 * <div class="zh">创建一个参数未初始化的分类器</div>
	 */
	public Classifier(){
		
	}
	
	/**
	 * 
	 * @return <span class="en"></span>
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
	 * @return <span class="en">Image preprocessing method</span>
	 * <span class="zh">图片预处理方法</span>
	 */
	public abstract String getNormMethod();
	
	/**
	 * <div class="en">load images, run graphs, 
	 * print results as {@code System.out} 
	 * and save results to {@code RESULT_FILE_NAME} under rootDir</div>
	 * <div class="zh">加载图片, run graphs, 
	 * 用{@code System.out} 输出结果，
	 * 并把结果保存在rootDir下的{@code RESULT_FILE_NAME}文件里</div>
	 * @param inputPaths paths to all inputs<br>
	 * <span class="en">{@code rootPath} is the first element of this array. if first element of this array is a dir, 
	 * the result file is saved there; if not, 
	 * the result file is saved alongside with this file</span>
	 * <span class="zh">如果该数组的第一个元素{@code rootPath} 是文件夹，
	 * 结果列表储存在该文件夹中，否则储存在第一个文件所在的文件夹中</span>
	 *  
	 */
	public void loadAndRun(String[] inputPaths){
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
