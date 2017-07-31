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

public class LabelGenerator {
	//private File rootDir;
	//private File labelDir;
	//private PrintWriter wr;
	public static final String LABEL_FILE_NAME = "tf-images-with-labels.txt";
	public static final String LABEL_TEXT_FILE_NAME = "tf-labels-to-text.txt";
	public static final String SEP = "/";
	public static final String LABEL_SEP = "|||";
	//private int counter = 0;
	private ArrayList<String> labels = new ArrayList<String>();
	
	/**
	 * assumes that imgs are stored this way: 
	 * root/catagory1; root/catagory2; root/catagory3; ... no nesting
	 * acts like {@code LabelGenerator(rootDir, labelDir, 1)}
	 * @param rootDir
	 * @param labelDir where you can find the label files
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * assumes that imgs are stored this way: <br>
	 * root/nest1/nest2/xxx.img
	 * label for "xxx.img" is then "nest1/nest2"
	 * @param rootDir
	 * @param labelDir where you can find the label files
	 */
	public LabelGenerator(File rootDir, File labelDir, int depth){
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
											textWr.println((labels.size()-1)+LABEL_SEP+label);
										}
										labelWr.println(String.format("%s"+LABEL_SEP+"%s", child.getAbsolutePath(), labels.size()-1));
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
			labelWr.flush();
			labelWr.close();
			textWr.flush();
			textWr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//private void generateNestedLabels(PrintWriter labelWr, PrintWriter textWr, )
	/**
	 * 
	 * @param path path to the label file, utf-8 encoded
	 * @return an ArrayList of labels read from the label file as separated by LABEL_SEP<br> 
	 * for example "0LABEL_SEPtree" => labels.get(0)==tree
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return labels;
	}
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
	/**
	 * creates a label file 
	 * path\to\filename.jpg[-is-]label
	 * assume that root directory doesn't contain any image sample
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
			new LabelGenerator(rootDir, rootDir, 2);
		}else{
			System.out.println("root directory must be a directory.");
		}
		System.out.println("Finished.");
	}

}
