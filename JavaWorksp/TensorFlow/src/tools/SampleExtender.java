package tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import general.Transformer;

public class SampleExtender {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String directoryPath = args[0];
		File directory = new File(directoryPath);
		if(directory.isDirectory()){
			//FileFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif","png");
			for(File entry : directory.listFiles()){//filter
				String ext = entry.getPath();
				//ext=ext.substring(ext.length()-4);
				System.out.println(ext);
				ext=ext.toLowerCase();
				if(ext.endsWith(".jpg")){
					double p = Math.random();
					if(p>0.9){
						
						try {
							BufferedImage img = ImageIO.read(entry);
							BufferedImage output = Transformer.cropImage(img);
							ImageIO.write(output, "jpg", new File(directoryPath+"\\ext-"+(int)(Math.random()*1000000)+".jpg"));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}else{
			System.out.println("not a dir");
		}

	}
}
