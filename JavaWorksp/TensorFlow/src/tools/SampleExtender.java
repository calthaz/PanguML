package tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * extend image samples by random crop<br>
 * jpgs and pngs only
 *
 */
public class SampleExtender {
	
	public static void main(String[] args) {
		String directoryPath = args[0];
		File directory = new File(directoryPath);
		if(directory.isDirectory()){
			//we are only going to crop jpgs and pngs
			for(File entry : directory.listFiles()){
				String ext = entry.getPath();
				System.out.println(ext);
				ext=ext.toLowerCase();
				if(ext.endsWith(".jpg")||ext.endsWith(".png")){
					double p = Math.random();
					if(p>0.6){
						try {
							BufferedImage img = ImageIO.read(entry);
							BufferedImage output = TFUtils.randomCropImage(img);
							ImageIO.write(output, "jpg", new File(directoryPath+"\\ext-"+(int)(Math.random()*1000000)+".jpg"));
						} catch (IOException e) {
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
