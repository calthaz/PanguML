package tools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class LabelGenerator {
	//private File rootDir;
	//private File labelDir;
	//private PrintWriter wr;
	public static final String LABEL_FILE_NAME = "tf-images-with-labels.txt";
	public static final String LABEL_TEXT_FILE_NAME = "tf-labels-to-text.txt";
	public static final String SEP = "\\";
	public static final String LABEL_SEP = "[-is-]";
	//private int counter = 0;
	private ArrayList<String> labels = new ArrayList<String>();
	
	public LabelGenerator(File rootDir, File labelDir){
		try {
			PrintWriter labelWr=new PrintWriter(new FileWriter(labelDir+SEP+LABEL_FILE_NAME,false));	
			PrintWriter textWr=new PrintWriter(new FileWriter(labelDir+SEP+LABEL_TEXT_FILE_NAME,false));	
			for(File dir: rootDir.listFiles()){
				if(dir.isDirectory()){
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
	
	/*
	 * 不想去寻找嵌套的文件夹了
	private void readRecursively(File dir, String currentClass){
		
	}
	*/
	public static ArrayList<String> readLabelsFromFile(String path){
		ArrayList<String> labels = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.indexOf(LABEL_SEP)!=-1){
					String[] val = line.split("\\"+LABEL_SEP);//+"\\"
					labels.add(Integer.parseInt(val[0]), val[1]);
				}
			}
			br.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return labels;
	}
	/**
	 * creates a label file 
	 * path\to\filename.jpg[-is-]label
	 * assume that root directory doesn't contain any image sample
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
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
			new LabelGenerator(rootDir, rootDir);
		}else{
			System.out.println("root directory must be a directory.");
		}
	}

}
