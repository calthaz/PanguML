package tools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import general.DevConstants;

/**
 * <div class="en">Generate or parse</div>
 * <div class="zh"></div>
 */
public class LabelGenerator {
	//private File rootDir;
	//private File labelDir;
	//private PrintWriter wr;
	/** <div class="zh">图片标签文件的名称
	   * <b>注意：要和Python文件里一致</b></div>
	   * <div class="en">a file with images and assigned labels
	   * <b>Note: must be in sync with that in Pthon files</b></div> */
	public static final String LABEL_FILE_NAME = "tf-images-with-labels.txt";
	/** <div class="zh">标签数字对应文字的文件的名称
	   * <b>注意：要和Python文件里一致</b></div>
	   * <div class="en">a file with tha translation of the integers to text labels
	   * <b>Note: must be in sync with that in Pthon files</b></div> */
	public static final String LABEL_TEXT_FILE_NAME = "tf-labels-to-text.txt";
	 /** 
	  * <div class="zh">系统文件路径分隔符</div>
	  * <div class="en">path separator in the system</div> */
	public static final String SEP = "/";
	  /** <div class="zh">用来读取和打印标签文件、结果文件的分隔符
	   * <b>注意：要和Python文件里一致</b></div>
	   * <div class="en">separator used to read and write label files and result files
	   * <b>Note: must be in sync with that in Pthon files</b></div> */
	public static final String LABEL_SEP = "|||";
	//private int counter = 0;
	/**
	 * <div class="en">text labels with indices of the corresponding integer labels</div>
	 * <div class="zh">具有相应整数标签索引的文本标签列表</div>
	 */
	public ArrayList<String> labels = new ArrayList<String>();
	private File rootDir;
	private File labelDir;
	private int depth;
	private boolean withSeparateEval;
	
	/**
	 * <div class="en">
	 * creates a label file like: <br>
	 * path\to\filename.jpg[{@code LabelGenerator.LABEL_SEP}]label<br>
	 * and an int-to-text file like: <br>
	 * 0[{@code LabelGenerator.LABEL_SEP}]label1<br>
	 * <br>
	 * Assumes that imgs are stored this way: 
	 * root/catagory1; root/catagory2; root/catagory3; ... no nesting<br>
	 * acts like {@code LabelGenerator(rootDir, labelDir, 1)}
	 * <b>but do not call run() afterwards. Thus you can't specify the order of the labels</b>
	 * </div>
	 * <br>
	 * <div class="zh">
	 * 创建一个标签文件，如：<br>
	 * path\to\filename.jpg [{@code LabelGenerator.LABEL_SEP}]标签<br>
	 * 和一个int-to-text文件，如：<br>
	 * 0 [{@code LabelGenerator.LABEL_SEP}] 标签1 <br>
	 * <br>
	 * 假设图片以这种方式存储：
	 * 根/类别1; 根/类别2; 根/类别3; ...没有嵌套<br>
	 * 像{@code LabelGenerator(rootDir，labelDir，1)}的行为，
	 * <b>但不需要在之后调用run()，因此也不能自定义标签顺序</b>
	 * </div>
	 * @param rootDir
	 * @param labelDir 
	 * <span class="zh">两个标签文件的存储位置</span>
	 * <span class="en">where you can find the 2 label files</span>
	 */
	public LabelGenerator(File rootDir, File labelDir){
		try {
			PrintWriter labelWr=new PrintWriter(labelDir+SEP+LABEL_FILE_NAME,"UTF-8");	
			PrintWriter textWr=new PrintWriter(labelDir+SEP+LABEL_TEXT_FILE_NAME,"UTF-8");	
			for(File dir: rootDir.listFiles()){
				if(dir.isDirectory()&&!dir.getName().startsWith(DevConstants.IGNORE_PREFIX)){
					for(File entry: dir.listFiles()){
						if(!entry.isDirectory()){
							//String ext = entry.getPath();
							//ext=ext.toLowerCase();
							
							try{
								//reads every image in case it were a bad one that could cause the training to fail
								BufferedImage img = ImageIO.read(entry);
								if(img!= null&&img.getHeight()!=0){
									if(!labels.contains(dir.getName())){
										labels.add(dir.getName());
										textWr.println((labels.size()-1)+LABEL_SEP+dir.getName());
									}
									labelWr.println(String.format("%s"+LABEL_SEP+"%s", entry.getAbsolutePath(), labels.size()-1));
								}else{
									System.out.println("Illegal image skipped: "+entry.getPath());
								}
							}catch  (IOException e){
								System.out.println("Illegal image skipped: "+entry.getPath());
							}
								
						}
						
					}
				}
			}
			labelWr.flush();
			labelWr.close();
			textWr.flush();
			textWr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * <div class="en">
	 * creates a label file like: <br>
	 * path\to\filename.jpg[{@code LabelGenerator.LABEL_SEP}]label<br>
	 * and an int-to-text file like: <br>
	 * 0[{@code LabelGenerator.LABEL_SEP}]label1<br>
	 * <br>
	 * assumes that imgs are stored this way: <br>
	 * root/nest1/nest2/xxx.img
	 * label for "xxx.img" is then "nest1/nest2"<br>
	 * <br>
	 * <b>
	 * must call run() to actually generate the files, </b>
	 * before calling run(), {@code this.labels} can be manually filled with 
	 * specific labels and their indices will be used 
	 * instead of the order read by {@code dir.listFiles()}
	 * </div>
	 * <div class="zh">
	 * 创建一个标签文件，如：<br>
	 * path\to\filename.jpg [{@code LabelGenerator.LABEL_SEP}]标签<br>
	 * 和一个int-to-text文件，如：<br>
	 * 0 [{@code LabelGenerator.LABEL_SEP}] 标签1 <br>
	 * <br>
	 * 假设imgs以这种方式存储：<br>
	 * 根/嵌套1/嵌套2/xxx.img
	 * 则“xxx.img”的标签是“嵌套1/嵌套2”<br>
	 * <br>
	 * <b>必须调用run()来实际生成文件，</b>
	 * 在调用run()之前，{@code this.labels}可以手动填充
	 * 将使用特定标签及其索引，而不是按{@code dir.listFiles()}读取的顺序作为索引
	 * </div>
	 * @param rootDir <span class="zh">根文件</span><span class="en">root directory</span>
	 * @param labelDir <span class="zh">两个标签文件的存储位置</span><span class="en">where you can find the 2 label files</span>
	 * @param depth 
	 * <div class="en">
	 * the number of parents an image have
	 * to reach the rootDir. 
	 * For example: <br>
	 * root/nest1/nest2/xxx.png has depth=2<br>
	 * If the specified depth = 2, root/nest1/foo.jpg <b>will not appear</b> in the label files.
	 * </div>
	 * <div class="zh">
	 * 图像达到rootDir经过的父文件夹数目。
     * 例如：<br>
     * 根/嵌套1/嵌套2/xxx.img的depth = 2 <br>
     * 如果指定的depth = 2，则根/嵌套1/foo.jpg<b>不会</b>出现在标签文件中。
     * </div>
	 */
	public LabelGenerator(File rootDir, File labelDir, int depth){
		this.withSeparateEval = false;
		this.rootDir = rootDir;
		this.labelDir = labelDir;
		this.depth = depth;
		//generateNestedLabels(rootDir, labelDir, depth);
	}
	/**
	 * <div class="en">
	 * creates a label file like: <br>
	 * path\to\filename.jpg[{@code LabelGenerator.LABEL_SEP}]label<br>
	 * and an int-to-text file like: <br>
	 * 0[{@code LabelGenerator.LABEL_SEP}]label1<br>
	 * <br>
	 * assumes that imgs are stored this way: <br>
	 * root/nest1/nest2/xxx.img
	 * label for "xxx.img" is then "nest1/nest2"<br>
	 * <br>
	 * <b>
	 * must call run() to actually generate the files, </b>
	 * before calling run(), {@code this.labels} can be manually filled with 
	 * specific labels and their indices will be used 
	 * instead of the order read by {@code dir.listFiles()}
	 * </div>
	 * <div class="zh">
	 * 创建一个标签文件，如：<br>
	 * path\to\filename.jpg [{@code LabelGenerator.LABEL_SEP}]标签<br>
	 * 和一个int-to-text文件，如：<br>
	 * 0 [{@code LabelGenerator.LABEL_SEP}] 标签1 <br>
	 * <br>
	 * 假设imgs以这种方式存储：<br>
	 * 根/嵌套1/嵌套2/xxx.img
	 * 则“xxx.img”的标签是“嵌套1/嵌套2”<br>
	 * <br>
	 * <b>必须调用run()来实际生成文件，</b>
	 * 在调用run()之前，{@code this.labels}可以手动填充
	 * 将使用特定标签及其索引，而不是按{@code dir.listFiles()}读取的顺序作为索引
	 * </div>
	 * @param rootDir <span class="zh">根文件</span><span class="en">root directory</span>
	 * @param labelDir <span class="zh">两个标签文件的存储位置</span><span class="en">where you can find the 2 label files</span>
	 * @param depth 
	 * <div class="en">
	 * the number of parents an image have
	 * to reach the rootDir. 
	 * For example: <br>
	 * root/nest1/nest2/xxx.png has depth=2<br>
	 * If the specified depth = 2, root/nest1/foo.jpg <b>will not appear</b> in the label files.
	 * </div>
	 * <div class="zh">
	 * 图像达到rootDir经过的父文件夹数目。
     * 例如：<br>
     * 根/嵌套1/嵌套2/xxx.img的depth = 2 <br>
     * 如果指定的depth = 2，则根/嵌套1/foo.jpg<b>不会</b>出现在标签文件中。
     * </div>
	 * @param withSeparateEval 
	 * <div class="en">If true, it looks for rootDir/train and rootDir/eval 
	 * and generate label files in these two folders. The index of the labels are the same.</div>
	 * <div class ="zh">如果为true，则查找rootDir/train和rootDir/eval
	 * 并在这两个文件夹中生成标签文件。 标签的索引是相同的</div>
	 */
	public LabelGenerator(File rootDir, File labelDir, int depth, boolean withSeparateEval){
		this.withSeparateEval = withSeparateEval;
		this.rootDir = rootDir;
		this.labelDir = labelDir;
		this.depth = depth;
	}
	/**
	 * <span class="zh">按照预设生成标签</span>
	 * <span class="en">generate the label files by preset params</span>
	 */
	public void run(){
		if(!withSeparateEval){
			generateNestedLabels(rootDir, labelDir, depth);
		}else{
			File trainDir = new File(rootDir+"/train");
			if(trainDir.exists()){
				generateNestedLabels(trainDir, new File(labelDir+"/train"), depth);
			}else{
				System.out.println("Can't find train dir.");
				return;
			}
			File evalDir = new File(rootDir+"/eval");
			if(evalDir.exists()){
				generateNestedLabels(evalDir, new File(labelDir+"/eval"), depth);
			}else{
				System.out.println("Can't find eval dir.");
				return;
			}
		}
	}
	private void generateNestedLabels(File rootDir, File labelDir, int depth){
		try {
			PrintWriter labelWr=new PrintWriter(labelDir+SEP+LABEL_FILE_NAME,"UTF-8");	
			PrintWriter textWr=new PrintWriter(labelDir+SEP+LABEL_TEXT_FILE_NAME,"UTF-8");	
			ArrayList<File> dirs = new ArrayList<File>();
			dirs.add(rootDir);
			Path rootPath = Paths.get(rootDir.getAbsolutePath());
			for(int round=0; round<=depth; round++){
				ArrayList<File> tmp = new ArrayList<File>();
				while(!dirs.isEmpty()){
					File father = dirs.remove(dirs.size()-1);
					for(File child : father.listFiles()){
						if(child.isDirectory()&&!child.getName().startsWith(DevConstants.IGNORE_PREFIX)){
							tmp.add(child);
						}
						if(round==depth){
							if(!child.isDirectory()){
								try{
									//reads every image in case it were a bad one that could cause the training to fail
									BufferedImage img = ImageIO.read(child);
									if(img!= null&&img.getHeight()!=0){
										//children are image files
										Path childPath = Paths.get(father.getAbsolutePath()); 
										Path relative = rootPath.relativize(childPath);
										String label = relative.toString();
										if(!labels.contains(label)){
											labels.add(label);
											//textWr.println((labels.size()-1)+LABEL_SEP+label);
											labelWr.println(String.format("%s"+LABEL_SEP+"%s", child.getAbsolutePath(), labels.size()-1));
										}else{
											labelWr.println(String.format("%s"+LABEL_SEP+"%s", child.getAbsolutePath(), labels.indexOf(label)));
										}
										
									}else{
											System.out.println("Illegal image skipped: "+child.getPath());
									}
								}catch  (IOException e){
									System.out.println("Illegal image skipped: "+child.getPath());
								}
								
							}
						}
					}
				}
				dirs = tmp; 
			}
			for(int i=0; i<labels.size(); i++){
				textWr.println(i+LABEL_SEP+labels.get(i));
			}
			labelWr.flush();
			labelWr.close();
			textWr.flush();
			textWr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param path <span class="zh">utf-8编码的标签文件的路径</span><span class="en">path to the label file, utf-8 encoded</span>
	 * @return <div class = "en">an ArrayList of labels read from the label file as separated by LABEL_SEP<br> 
	 * for example {@code "0LABEL_SEPtree" => labels.get(0)==tree}</div>
	 * <div class="zh">从标签文件读取由LABEL_SEP分隔的标签到ArrayList<br>
	 * 例如{@code "0LABEL_SEPtree" => labels.get(0)==tree}</div>
	 */
	public static ArrayList<String> readLabelsFromFile(String path){
		ArrayList<String> labels = new ArrayList<String>();
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(new File(path)),"UTF-8");                 
			BufferedReader br = new BufferedReader(read);
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.indexOf(LABEL_SEP)!=-1){
					//String[] val = line.split("\\"+LABEL_SEP+"");
					line = line.trim();
					//System.out.println(line);
					String index = line.substring(0,line.indexOf(LABEL_SEP));
					String name = line.substring(line.indexOf(LABEL_SEP)+LABEL_SEP.length());
					labels.add(Integer.parseInt(index), name);
				}
			}
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return labels;
	}
	/**
	 *
	 * <div class="en"> Fill the return array with strings split up by {@code LabelGenerator.LABEL_SEP}</div>
	 * <div class="zh">用{@code LabelGenerator.LABEL_SEP}分割字符串，填充返回数组</div>
	 * @param line <span class="zh">用于解析的字符串</span><span class="en">String to be parsed</span>
	 * @param fieldCount
	 * @return <div class="en">array of length {@code fieldCount}. 
	 * if {@code fieldCount} is larger than the actual number of fields in {@code line}, 
	 * the extra parts of the array are {@code null}; if {@code fieldCount} is less than the actual number of fields, 
	 * only the first {@code fieldCount} fields are returned.</div>
	 * <div class="zh">长度为{@code fieldCount}的数组。
	 * 如果{@code fieldCount}大于{@code line}中实际的字段数，
	 * 数组的额外部分是{@code null}; 如果{@code fieldCount}小于实际的字段数，
	 * 只返回前{@code fieldCount}个字段。</div>
	 */
	public static String[] parseResultLine(String line, int fieldCount){
		String[] result = new String[fieldCount];
		for(int i=0; i<fieldCount; i++){
			
			int index = line.indexOf(LabelGenerator.LABEL_SEP);
			if(index!=-1){
				result[i] = line.substring(0,index);
				line = line.substring(index+LabelGenerator.LABEL_SEP.length());
			}else{
				result[i]=line;
				break;
			}
			
		}
		for(String s: result){
			System.out.println(s);
		}
		return result;
	}
	public String toString(){
		String str = String.format("LabelGenerator: \n rootDir:%s; labelDir:%s; \n depth:%d; withSeparateEval:%s\n", 
				rootDir, labelDir,depth,withSeparateEval);
		for(String label: labels){
			str+=label+"\n";
		}
		return str;
	}
	
	/**
	 * <span class="zh">样例</span><span class="en">example</span>
	 * @param args
	 */
	public static void main(String[] args) {
		String rootPath = "";
		if(args.length<1){
			rootPath = System.getProperty("user.dir");
		}else{
			rootPath = args[0];
		}
		System.out.println(rootPath);
		File rootDir = new File(rootPath);
		if(rootDir.isDirectory()){
			File labelFile = new File(rootPath+SEP+LABEL_FILE_NAME);
			
			if(labelFile.exists()){
				System.out.println("label file already exists. Overwriting that file");
				System.out.println(labelFile.getAbsolutePath());
			}
			LabelGenerator lg = new LabelGenerator(rootDir, rootDir, 1, true);
			//String[] labels = {"baby-bed","bunk-bed","double-bed","hammock","round-bed","single-bed"};
			//ArrayList<String> arr = new ArrayList<String>();
			//for(String l : labels){
				//arr.add(l);
			//}
			//lg.labels = arr;
			lg.run();
		}else{
			System.out.println("root directory must be a directory.");
		}
		System.out.println("Finished.");
	}

}
