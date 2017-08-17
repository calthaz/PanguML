package zym.cv.findsimilarimg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import zym.cv.findmaincolor.ColorExtractor;
import zym.cv.findmaincolor.OctTree;
import zym.tensorflow.tools.LabelGenerator;

/**
 * <span class="en">Encapsulate an image, its main colors and its similarity to another image</span>
 * <span class="zh">封装一个图片，主要颜色和匹配程度的类</span>
 *
 */
public class ImagePalette implements Comparable<ImagePalette>{
	/**
	 * <span class="zh">用前{@code TOP_N}个颜色进行比较</span>
	 * <span class="en">use the first {@code TOP_N} colors for comparing</span>
	 */
	public static int TOP_N = 6;
	private static ColorExtractor ce = new OctTree();//new ColorThief();
	private String path;
	private int[][] palette;
	/**
	 * <span class="en">similarity</span><span class="zh">相似度</span>
	 */
	public double score;
	
	/**
	 * <span class="en">try to load the palette from a String path. <br>
	 * <b>If it fails, {@code this.path == null}!</b></span>
	 * <span class="zh">尝试通过String路径载入文件的主色调。<br>
	 * <b>如果失败, {@code this.path == null}!</b></span>
	 * @param path
	 */
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
	/**
	 * <span class="en">load the palette from a String path. </span>
	 * <span class="zh">载入文件的主色调。</span>
	 * @param img
	 */
	public ImagePalette(BufferedImage img) {
		this.palette=ce.getRGBPalette(img, TOP_N);
		ce.clear();
	}
	
	/**
	 * <span class="en"><b>If it fails to read the maincolors, {@code this.path == null}!</b></span>
	 * <span class="zh"><b>如果载入文件的主色调失败, {@code this.path == null}!</b></span>
	 * @return path
	 */
	public String getPath() {
		return this.path;
	}
	
	/**
	 * @return {@code int[TOP_N][3]}
	 */
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
