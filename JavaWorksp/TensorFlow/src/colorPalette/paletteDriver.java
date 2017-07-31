package colorPalette;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class paletteDriver {
	private static void printColorBlocks(int[][] colors){
		for(int[] i : colors){
				System.out.println(
					String.format("<div class=\"swatch\" style=\"width=65px; height=50px; background-color: rgb(%d, %d, %d)\"></div>", 
							i[0], i[1], i[2]));
		}
	}
	public static void main(String[] args) {
		try {
			//BufferedImage img = ImageIO.read(new File("F:/tmp/styles/mediterranean/bedroom/pipi059126993.png"));
			//BufferedImage img = ImageIO.read(new File("F:/tmp/styles/mediterranean/bedroom/pipi67132787.png"));
			BufferedImage img = ImageIO.read(new File("F:/TensorFlowDev/JavaWorksp/TensorFlow/img/dingo.jpg"));
			int[][] colors = ColorThief.getPalette(img, 6);
			printColorBlocks(colors);
			OctTree ot = new OctTree();
			System.out.println("-----------------Octree palette------------");
			colors = ot.getPalatte(img, 10);
			printColorBlocks(colors);
			int num = 1000;
			long time = System.currentTimeMillis();
			for(int i=0; i<num; i++){
				colors = ColorThief.getPalette(img, 10);
			}
			System.out.println("Time for lok: "+ (System.currentTimeMillis()-time));
			time = System.currentTimeMillis();
			for(int i=0; i<num; i++){
				ot.clear();
				colors = ot.getPalatte(img, 10);
			}
			System.out.println("Time for oct: "+ (System.currentTimeMillis()-time));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
