package findSimilarImg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import colorPalette.ColorExtractor;
import colorPalette.ColorThief;
import colorPalette.OctTree;
import tools.LabelGenerator;

/**
 * <div class="en">Encapsulate an image, its main colors and its similarity to another image</div>
 * <div class="zh">封装一个图片，主要颜色和匹配程度的类</div>
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
	 * <div class="en">similarity</div><div class="zh">相似度</div>
	 */
	public double score;
	
	/**
	 * <div class="en">try to load the palette from a String path. <br>
	 * <b>If it fails, {@code this.path == null}!</b></div>
	 * <div class="zh">尝试通过String路径载入文件的主色调。<br>
	 * <b>如果失败, {@code this.path == null}!</b></div>
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
	 * <div class="en">load the palette from a String path. </div>
	 * <div class="zh">载入文件的主色调。</div>
	 * @param img
	 */
	public ImagePalette(BufferedImage img) {
		this.palette=ce.getRGBPalette(img, TOP_N);
		ce.clear();
	}
	
	/**
	 * <div class="en"><b>If it fails to read the maincolors, {@code this.path == null}!</b></div>
	 * <div class="zh"><b>如果载入文件的主色调失败, {@code this.path == null}!</b></div>
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
