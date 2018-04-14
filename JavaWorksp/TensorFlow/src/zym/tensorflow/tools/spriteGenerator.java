package zym.tensorflow.tools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

/**
 * <span class="zh">生成固定大小的sprite图片</span>
 * <span class="en">Generate a sprite image of fixed size.</span>
 * 
 * @see <a href="https://www.tensorflow.org/versions/master/get_started/embedding_viz">TensorBoard: Embedding Visualization</a>
 *
 */
public class spriteGenerator {
	//public static final int MAX_SPRITE_SIZE = 8192;//probably too big
	/**
	 *  <span class="zh">sprite图像的最大尺寸小于最大支持尺寸</span>
	 *  <span class="en">Maximum size of the sprite image, smaller than the maximun supported size on </span>
	 *  <a href="https://www.tensorflow.org/versions/master/get_started/embedding_viz">TensorBoard: Embedding Visualization</a>
	 */
	public static int MAX_SPRITE_SIZE = 3000;
	/**
	 *  <span class="zh">sprite图像里单张图片的尺寸</span>
	 *  <span class="en">Size of a thumbnail in the sprite image </span>
	 */
	public static int THUMB_SIZE = 40;
	
	/**
	 *  <span class="zh">运行</span>
	 *  <span class="en">Run</span>
	 *  spriteGenerator.
	 *  @param args <span class="zh">数组中的第一个元素是标准的图像与标签文件。</span>
	 *  <span class="en">The first element in the array is a standard images-with-labels file.</span>
	 */
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
			System.err.println("tf-images-with-labels.txt file can't be found at "+args[0]);
		}
	}

}
