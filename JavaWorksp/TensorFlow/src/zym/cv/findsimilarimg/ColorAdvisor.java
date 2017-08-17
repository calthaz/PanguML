package zym.cv.findsimilarimg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

import zym.tensorflow.tools.TFUtils;

/**
 * <span class="en">Get similar images based on main colors</span>
 * <span class="zh">根据主色调匹配相似图片</span>
 *
 */
public class ColorAdvisor {
	
	/**
	 * <span class="en">Get similar images based on main colors</span>
	 * <span class="zh">根据主色调匹配相似图片</span>
	 * @return
	 */
	public static ArrayList<ImagePalette> getSimilarImages(BufferedImage img, String database){
		//int[][] colors = ce.getPalette(img, TOP_N);
		ImagePalette input = new ImagePalette(img);
		float[][] inputHSB = new float[input.getPalette().length][3];
		for(int i=0; i<input.getPalette().length; i++){
			int[] rgb = input.getPalette()[i];
			inputHSB[i] = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);
		}
		ArrayList<String> files = new ArrayList<String>();
		ArrayList<ImagePalette> pals = new ArrayList<ImagePalette>();
		TFUtils.readFilesRecursively(new File(database), files);
		for(String p : files){
			ImagePalette imp = new ImagePalette(p);
			if(null!=imp.getPath()&&null!=imp.getPalette()){
				double distance=0;
				//System.out.println(imp.getPath());
				for(int i=0; i<imp.getPalette().length; i++){
					int[] rgb = imp.getPalette()[i];
					float[] hsb = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);
					distance+=Math.abs(hsb[0]-inputHSB[i][0])+Math.abs(hsb[1]-inputHSB[i][1])+Math.abs(hsb[2]-inputHSB[i][2]);
					//System.out.println(String.format("[%.2f, %.2f][%.2f, %.2f][%.2f, %.2f]", 
							//hsb[0],inputHSB[i][0],hsb[1],inputHSB[i][1],hsb[2],inputHSB[i][2]));
				}			
			imp.score = distance;	
			pals.add(imp);
			}
			
		}
		Collections.sort(pals);
		return pals;
	}
	//public static String getSimilarImages(BufferedImage img, String database, ArrayList<int[]> palette){
		
	//}
	/**
	 * <span class="en">Test</span>
	 * <span class="zh">测试</span>
	 * @param args 
	 */
	public static void main(String[] args) {
		
		BufferedImage input;
		try {
			input = ImageIO.read(new File("training-materials/hardware/p-wall-paper/sc39.png"));
			long time = System.currentTimeMillis();
			ArrayList<ImagePalette> results = getSimilarImages(input, "training-materials/hardware");
			int count = 0;
			for(ImagePalette imp : results){
				System.out.println(String.format("<img class=\"target-image\" src=\"%s\">", imp.getPath()));
				if(count==45)break;
				count++;
			}
			System.out.println(System.currentTimeMillis()-time+"ms");
			//1931 9550 vs 8237
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
