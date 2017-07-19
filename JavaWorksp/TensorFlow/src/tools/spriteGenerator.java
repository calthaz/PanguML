package tools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class spriteGenerator {
	/**
	 * 标准参照
	 * https://www.tensorflow.org/versions/master/get_started/embedding_viz
	 */
	//public static final int MAX_SPRITE_SIZE = 8192;//probably too big
	public static final int MAX_SPRITE_SIZE = 3000;
	public static final int THUMB_SIZE = 40;

	public static void main(String[] args) {
		if(args.length!=1){
			System.out.println("supply one argument as the tf-images-with-labels.txt file");
			return;
		}
		File labels = new File(args[0]);
		String rootPath = labels.getParent();
		System.out.println("Sprite image will be saved under "+rootPath);
		int picPerLine = MAX_SPRITE_SIZE/THUMB_SIZE;
		int spriteWidth = picPerLine*THUMB_SIZE;
		BufferedImage sprite = new BufferedImage(spriteWidth, spriteWidth, BufferedImage.TYPE_INT_ARGB);
	  	Graphics2D g = sprite.createGraphics();
	  	  
		try {
			Scanner sc = new Scanner(labels);
			int count = 0;
			while(sc.hasNextLine()){
				String line = sc.nextLine();
				line = line.trim();
				String imagePath = line.substring(0,line.indexOf(LabelGenerator.LABEL_SEP));
				if(imagePath!=""){
						
						BufferedImage img = null;
						try {
							 img = ImageIO.read(new File(imagePath));
							 img = TFUtils.getScaledImage(img, THUMB_SIZE, THUMB_SIZE);
							 //System.out.println("get thumb");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("Failed to read "+imagePath);
							System.out.println("Use blank img instead");
							img = TFUtils.getBlankImage(THUMB_SIZE, THUMB_SIZE);
						}
						g.drawImage(img, count%picPerLine*THUMB_SIZE, count/picPerLine*THUMB_SIZE, null);
						//drawing starts from zero, so add one to count here.
						count++;	
				}
				
			}
			g.dispose();
			BufferedImage dest = sprite.getSubimage(0, 0, spriteWidth, (count/picPerLine+1)*THUMB_SIZE);
			try {
				ImageIO.write(dest, "png", new File(rootPath+"/sprite.png"));
				//image format is a question. the images in the set are of different format, but they are converted...?
			} catch (IOException e) {
				System.err.println("Sprite image failed to save");
			}
			sc.close();
			System.out.println("Sprite image saved");
			System.out.println(rootPath+"/sprite.png");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("tf-images-with-labels.txt file can't be found at "+args[0]);
		}
	}

}
