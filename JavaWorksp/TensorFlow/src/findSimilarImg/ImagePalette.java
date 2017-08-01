package findSimilarImg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import colorPalette.ColorExtractor;
import colorPalette.ColorThief;
import colorPalette.OctTree;
import tools.LabelGenerator;

public class ImagePalette implements Comparable<ImagePalette>{
	public static int TOP_N = 6;
	private static ColorExtractor ce = new OctTree();//new ColorThief();
	private String path;
	private int[][] palette;
	public double score;
	public ImagePalette(String path){
		BufferedImage img;
		try {
			img = ImageIO.read(new File(path));
			if(img!=null&&img.getHeight()!=0){
				this.palette=ce.getRGBPalette(img, TOP_N);
				this.path = path;
				ce.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
			this.path = null;
		}
	}
	public ImagePalette(BufferedImage img) {
		this.palette=ce.getRGBPalette(img, TOP_N);
		ce.clear();
	}
	public String getPath() {
		return this.path;
	}
	public int[][] getPalette() {
		return palette;
	}
	@Override
	public int compareTo(ImagePalette rhs) {
		if (score<rhs.score)return -1;
		if (score>rhs.score)return 1;
		else return path.compareTo(rhs.getPath()); 
	}
	
	public String toString(){
		return path+LabelGenerator.LABEL_SEP+score;
	}
}
